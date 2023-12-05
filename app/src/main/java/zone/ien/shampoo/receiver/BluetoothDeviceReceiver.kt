package zone.ien.shampoo.receiver

import android.Manifest
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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import zone.ien.shampoo.activity.DeviceAddActivity
import zone.ien.shampoo.activity.TAG
import zone.ien.shampoo.constant.ActionID
import zone.ien.shampoo.constant.IntentID
import zone.ien.shampoo.constant.IntentKey
import zone.ien.shampoo.constant.MessageType
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

    private var gatt: BluetoothGatt? = null
    private var context: Context? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var readCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null

    private var deviceDatabase: DeviceDatabase? = null
    private var deviceLogDatabase: DeviceLogDatabase? = null
    private var notificationsDatabase: NotificationsDatabase? = null

    private var deviceId: Long = -1

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
                        Dlog.d(TAG, "Connected to ${gatt?.device?.name}")
                        gatt?.discoverServices()

                        gatt?.device?.let { device ->
                            DeviceAddActivity.deviceName = device.name
                            DeviceAddActivity.deviceAddress = device.address

                            GlobalScope.launch(Dispatchers.IO) {
                                val entity = deviceDatabase?.getDao()?.get(device.address)
                                entity?.let {
                                    deviceId = it.id ?: -1
                                }
                            }
                        }

                        Handler(Looper.getMainLooper()).postDelayed({
                            context?.let { Dlog.d(TAG, setNotifyDescriptor(it).toString()) }
                        }, 10 * 1000)
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (context?.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val services = gatt?.services ?: return
                for (service in services) {
                    for (characteristic in service.characteristics) {
                        if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_WRITE)) {
                            writeCharacteristic = characteristic
                        }
                        if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_READ)) {
                            gatt.readCharacteristic(characteristic)
                            readCharacteristic = characteristic
                        }
                        if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_NOTIFY)) {
                            gatt.setCharacteristicNotification(characteristic, true)
                            notifyCharacteristic = characteristic
                        }
                    }
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, byte: ByteArray) {
            super.onCharacteristicChanged(gatt, characteristic, byte)
            Dlog.d(TAG, "onCharacteristicChanged")
            val data = String(byte).split(":")
            val type = data[0].toInt()
            val value = data[1].toInt()

            context?.sendBroadcast(Intent(IntentID.CALLBACK_DEVICE_VALUE).apply {
                putExtra(IntentKey.MESSAGE_TYPE, type)
                putExtra(IntentKey.MESSAGE_VALUE, value)
            })

            GlobalScope.launch(Dispatchers.IO) {
                if (deviceId != -1L) {
                    val entity = DeviceLogEntity(deviceId, System.currentTimeMillis(), -1, -1)
                    when (type) {
                        MessageType.TYPE_BATTERY -> entity.battery = value
                        MessageType.TYPE_BATTERY_LOW -> {
                            entity.battery = value
                            val notiEntity = NotificationsEntity(MessageType.TYPE_BATTERY_LOW, System.currentTimeMillis(), "배터리 없음 ${value}", "배터리가 없어요", entity.id ?: -1)
                            notificationsDatabase?.getDao()?.add(notiEntity)

                        }
                        MessageType.TYPE_LEVEL -> entity.capacity = value
                        MessageType.TYPE_LEVEL_LOW -> {
                            entity.capacity = value
                            val notiEntity = NotificationsEntity(MessageType.TYPE_LEVEL_LOW, System.currentTimeMillis(), "샴푸 없음 ${value}", "샴푸가 없어요", entity.id ?: -1)
                            notificationsDatabase?.getDao()?.add(notiEntity)
                        }
                    }
                    if (entity.battery != -1 || entity.capacity != -1) deviceLogDatabase?.getDao()?.add(entity)
                }
            }


        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            Dlog.d(TAG, "onCharacteristicWrite")
        }
    }

    private fun setNotifyDescriptor(context: Context): Boolean {
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return false

        Dlog.d(TAG, "setNotifyDescriptor null ${notifyCharacteristic == null}")
        return gatt?.let {
            notifyCharacteristic?.let { characteristic ->
                val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    it.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == BluetoothStatusCodes.SUCCESS
                } else {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    it.writeDescriptor(descriptor)
                }
            } ?: false
        } ?: false
    }

    private fun sendMessage(context: Context, type: Int, message: String) {
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return

        gatt?.let {
            writeCharacteristic?.let { characteristic ->
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

                if (gatt?.device?.name != (device?.name ?: "")) {
                    gatt = device?.connectGatt(context, true, gattCallback)
                }


//                gatt = device?.connectGatt(context, true, gattCallback)
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
                Dlog.d(TAG, "Request new connect ${device?.name}")

                gatt = device?.connectGatt(context, true, gattCallback)
            }
            ActionID.ACTION_NOTIFY_DESCRIPTOR -> {
                setNotifyDescriptor(context)
            }
            ActionID.ACTION_SEND_DEVICE -> {
                val type = intent.getIntExtra(IntentKey.MESSAGE_TYPE, -1)
                val message = intent.getStringExtra(IntentKey.MESSAGE_VALUE) ?: ""
                sendMessage(context, type, message)
            }
        }
    }
}