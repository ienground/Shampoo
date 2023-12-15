package zone.ien.shampoo.receiver

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothStatusCodes
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings.Global
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import zone.ien.shampoo.R
import zone.ien.shampoo.activity.DeviceAddActivity
import zone.ien.shampoo.activity.TAG
import zone.ien.shampoo.constant.ActionID
import zone.ien.shampoo.constant.ChannelID
import zone.ien.shampoo.constant.IntentID
import zone.ien.shampoo.constant.IntentKey
import zone.ien.shampoo.constant.MessageType
import zone.ien.shampoo.constant.NotificationID
import zone.ien.shampoo.room.DeviceDatabase
import zone.ien.shampoo.room.DeviceLogDao
import zone.ien.shampoo.room.DeviceLogDatabase
import zone.ien.shampoo.room.DeviceLogEntity
import zone.ien.shampoo.room.NotificationsDatabase
import zone.ien.shampoo.room.NotificationsEntity
import zone.ien.shampoo.utils.Dlog
import zone.ien.shampoo.utils.MyUtils.getBluetoothGattProperty
import zone.ien.shampoo.utils.MyUtils.hasProperty
import java.util.UUID
import kotlin.reflect.typeOf

class BluetoothDeviceReceiver : BroadcastReceiver() {

    private var gatts: MutableMap<String, BluetoothGatt> = mutableMapOf()
    private var context: Context? = null
    private var writeCharacteristics: MutableMap<String, BluetoothGattCharacteristic> = mutableMapOf()
    private var readCharacteristics: MutableMap<String, BluetoothGattCharacteristic> = mutableMapOf()
    private var notifyCharacteristics: MutableMap<String, BluetoothGattCharacteristic> = mutableMapOf()

    private var deviceDatabase: DeviceDatabase? = null
    private var deviceLogDatabase: DeviceLogDatabase? = null
    private var notificationsDatabase: NotificationsDatabase? = null

    private var deviceIds: MutableMap<String, Long> = mutableMapOf()

    @OptIn(DelicateCoroutinesApi::class)
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (context?.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
            when (status) {
                BluetoothGatt.GATT_FAILURE, 133 -> { // unknown code 133
                    gatt?.disconnect()
                    gatt?.close()
                }
                BluetoothGatt.GATT_SUCCESS -> {
                    if (newState == BluetoothGatt.STATE_CONNECTED) {
                        gatt?.discoverServices()

                        Dlog.d(TAG, "Connected to ${gatt?.device?.name}  ${gatt?.device?.address} ${gatt?.device == null}")

                        gatt?.device?.let { device ->
                            DeviceAddActivity.deviceName = device.name ?: ""
                            DeviceAddActivity.deviceAddress = device.address ?: ""

                            GlobalScope.launch(Dispatchers.IO) {
                                val entity = deviceDatabase?.getDao()?.get(device.address)
                                entity?.let {
                                    deviceIds[device.address] = it.id ?: -1
                                }
                            }

                            Handler(Looper.getMainLooper()).postDelayed({
                                context?.let { Dlog.d(TAG, setNotifyDescriptor(it, device.address).toString()) }
                            }, 5 * 1000)
                        }
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (context?.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val services = gatt?.services ?: return
                gatt.device?.let { device ->
                    for (service in services) {
                        for (characteristic in service.characteristics) {
                            if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_WRITE)) {
                                writeCharacteristics[device.address] = characteristic
                            }
                            if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_READ)) {
                                gatt.readCharacteristic(characteristic)
                                readCharacteristics[device.address] = characteristic
                            }
                            if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_NOTIFY)) {
                                gatt.setCharacteristicNotification(characteristic, true)
                                notifyCharacteristics[device.address] = characteristic
                            }
                        }
                    }
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, byte: ByteArray) {
            super.onCharacteristicChanged(gatt, characteristic, byte)
            Dlog.d(TAG, "onCharacteristicChanged ${gatts.size} ${gatt.device.address}")
            val data = String(byte).split(":")
            val type = data[0].toInt()
            val value = data[1].toInt()

            context?.let { context ->
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannel(NotificationChannel(ChannelID.BATTERY_LOW_ID, context.getString(R.string.battery_warning), NotificationManager.IMPORTANCE_DEFAULT))
                nm.createNotificationChannel(NotificationChannel(ChannelID.LIQUID_LOW_ID, context.getString(R.string.liquid_warning), NotificationManager.IMPORTANCE_DEFAULT))

                context.sendBroadcast(Intent(IntentID.CALLBACK_DEVICE_VALUE).apply {
                    putExtra(IntentKey.MESSAGE_TYPE, type)
                    putExtra(IntentKey.MESSAGE_VALUE, value)
                })

                GlobalScope.launch(Dispatchers.IO) {
                    gatt.device?.let {
                        if (deviceIds[it.address] != -1L) {
                            val device = deviceDatabase?.getDao()?.get(it.address)
                            val entity = DeviceLogEntity(deviceIds[it.address] ?: -1, System.currentTimeMillis(), -1, -1)
                            when (type) {
                                MessageType.TYPE_BATTERY -> entity.battery = value
                                MessageType.TYPE_BATTERY_LOW -> {
                                    entity.battery = value
                                    val title = context.getString(R.string.battery_low_title, device?.title)
                                    val content = context.getString(R.string.battery_low_content, value)
                                    val notiEntity = NotificationsEntity(MessageType.TYPE_BATTERY_LOW, System.currentTimeMillis(), title, content, entity.parentId)
                                    notificationsDatabase?.getDao()?.add(notiEntity)

                                    NotificationCompat.Builder(context, ChannelID.BATTERY_LOW_ID).apply {
                                        setContentTitle(title)
                                        setContentText(content)
                                        setSmallIcon(R.drawable.ic_battery_alert)
                                        color = ContextCompat.getColor(context, R.color.md_theme_light_primary)

                                        nm.notify(NotificationID.SHAMPOO_BATTERY_LOW + (entity.id ?: -1).toInt(), build())
                                    }
                                }

                                MessageType.TYPE_LEVEL -> entity.capacity = value
                                MessageType.TYPE_LEVEL_LOW -> {
                                    entity.capacity = value
                                    val title = context.getString(R.string.liquid_low_title, device?.title)
                                    val content = context.getString(R.string.liquid_low_content, value)
                                    val notiEntity = NotificationsEntity(MessageType.TYPE_LEVEL_LOW, System.currentTimeMillis(), title, content, entity.parentId)
                                    notificationsDatabase?.getDao()?.add(notiEntity)

                                    NotificationCompat.Builder(context, ChannelID.LIQUID_LOW_ID).apply {
                                        setContentTitle(title)
                                        setContentText(content)
                                        setSmallIcon(R.drawable.ic_shampoo_alert)
                                        color = ContextCompat.getColor(context, R.color.md_theme_light_primary)

                                        nm.notify(NotificationID.SHAMPOO_LIQUID_LOW + (entity.id ?: -1).toInt(), build())
                                    }
                                }
                            }
                            if (entity.battery != -1 || entity.capacity != -1) deviceLogDatabase?.getDao()?.add(entity)
                        }
                    }
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            Dlog.d(TAG, "onCharacteristicWrite ${gatt?.device?.address}")
        }
    }

    private fun setNotifyDescriptor(context: Context, address: String): Boolean {
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return false

        var resultCode = -1
        val result = gatts[address]?.let {
            notifyCharacteristics[address]?.let { characteristic ->
                val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    resultCode = it.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    resultCode == BluetoothStatusCodes.SUCCESS
                } else {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    it.writeDescriptor(descriptor)
                }
            } ?: false
        } ?: false

        Dlog.d(TAG, "setNotifyDescriptor $result gatts ${gatts[address]} / char ${notifyCharacteristics[address]} ${resultCode}")
        return result
    }

    private fun sendMessage(context: Context, address: String, type: Int, message: String) {
        Dlog.d(TAG, "sendMessage")
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return

        gatts[address]?.let {
            writeCharacteristics[address]?.let { characteristic ->
                Log.d(TAG, "$type $message")
                val value = "${type}:${message}".toByteArray()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    it.writeCharacteristic(characteristic, value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                } else {
                    characteristic.value = value
                    characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    it.writeCharacteristic(characteristic)
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        Dlog.d(TAG, "onReceive")

        this.context = context
        deviceDatabase = DeviceDatabase.getInstance(context)
        deviceLogDatabase = DeviceLogDatabase.getInstance(context)
        notificationsDatabase = NotificationsDatabase.getInstance(context)

        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return

        when (intent.action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                Dlog.d(TAG, "ACTION_STATE_CHANGED")
            }

            BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> {
                Dlog.d(TAG, "ACTION_CONNECTION_STATE_CHANGED")
            }

            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice?
                }
                Dlog.d(TAG, "ACTION_ACL_CONNECTED ${device?.name}")

                GlobalScope.launch(Dispatchers.IO) {
                    device?.let {
                        val entity = deviceDatabase?.getDao()?.get(it.address)
                        Dlog.d(TAG, "entity: ${entity}")
                    }
                }

                device?.let {
                    if (gatts[it.address] == null) {
                        gatts[it.address] = it.connectGatt(context, true, gattCallback)
                    }
                }
            }

            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice?
                }
                Dlog.d(TAG, "ACTION_ACL_DISCONNECTED ${device?.name}")
            }

            ActionID.ACTION_CONNECT_DEVICE -> {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice?
                }
                Dlog.d(TAG, "Request new connect ${device?.name} ${device?.address}")

                device?.let {
                    if (gatts[it.address] == null) {
                        gatts[it.address] = it.connectGatt(context, true, gattCallback)
                    }
                }
            }
            ActionID.ACTION_DISCONNECT_DEVICE -> {
                val address = intent.getStringExtra(IntentKey.DEVICE_ADDRESS)
                Dlog.d(TAG, "Request disconnect ${address} ${gatts[address]}")
                gatts[address]?.close()
                gatts.remove(address)
                writeCharacteristics.remove(address)
                notifyCharacteristics.remove(address)
                readCharacteristics.remove(address)
            }
            ActionID.ACTION_NOTIFY_DESCRIPTOR -> {
                val address = intent.getStringExtra(IntentKey.DEVICE_ADDRESS) ?: ""
                setNotifyDescriptor(context, address)
            }
            ActionID.ACTION_SEND_DEVICE -> {
                val address = intent.getStringExtra(IntentKey.DEVICE_ADDRESS) ?: ""
                val type = intent.getIntExtra(IntentKey.MESSAGE_TYPE, -1)
                val message = intent.getStringExtra(IntentKey.MESSAGE_VALUE) ?: ""
                sendMessage(context, address, type, message)
            }
            ActionID.ACTION_REQUEST_CONNECT_STATE -> {
                val address = intent.getStringExtra(IntentKey.DEVICE_ADDRESS) ?: ""
                context.sendBroadcast(Intent(IntentID.ACTION_RESPONSE_CONNECT_STATE).apply {
                    putExtra(IntentKey.DEVICE_ADDRESS, address)
                    putExtra(IntentKey.CONNECT_STATE, gatts[address] != null)
                })
            }
        }
    }
}