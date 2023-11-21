package zone.ien.shampoo.fragment

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import zone.ien.shampoo.R
import zone.ien.shampoo.activity.DeviceAddActivity
import zone.ien.shampoo.activity.TAG
import zone.ien.shampoo.adapter.DeviceAddScanAdapter
import zone.ien.shampoo.callback.DeviceAddActivityCallback
import zone.ien.shampoo.callback.DeviceAddCallback
import zone.ien.shampoo.constant.DeviceInfo
import zone.ien.shampoo.constant.IntentID
import zone.ien.shampoo.constant.IntentKey
import zone.ien.shampoo.databinding.FragmentDeviceAddScanBinding
import zone.ien.shampoo.receiver.BluetoothDeviceReceiver
import zone.ien.shampoo.utils.Dlog
import zone.ien.shampoo.utils.MyUtils.getBluetoothGattProperty
import zone.ien.shampoo.utils.MyUtils.toInt
import java.util.UUID

class DeviceAddScanFragment : Fragment() {

    private lateinit var binding: FragmentDeviceAddScanBinding

    private var mListener: OnFragmentInteractionListener? = null
    private var callbackListener: DeviceAddActivityCallback? = null

    private lateinit var bm: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var isScanning = false
    private var hashDeviceMap = HashMap<String, BluetoothDevice>()
    private lateinit var adapter: DeviceAddScanAdapter

    private var gatt: BluetoothGatt? = null
    private var readCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null

    private lateinit var barcodeLauncher: ActivityResultLauncher<ScanOptions>
    private val deviceAddCallback = object: DeviceAddCallback {
        override fun add(device: BluetoothDevice) {
            if (requireContext().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
            gatt = device.connectGatt(requireContext(), true, gattCallback)
        }
    }

    private fun hasProperty(characteristic: BluetoothGattCharacteristic, property: Int): Boolean = (characteristic.properties and property) == property

    private val gattCallback = object: BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (requireContext().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
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
//                            DeviceAddActivity.deviceName = device.name
                            DeviceAddActivity.deviceAddress = device.address
                        }

                        barcodeLauncher.launch(ScanOptions().apply {
                            setOrientationLocked(true)
                            setCameraId(1)
                        })
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (requireContext().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val services = gatt?.services ?: return
                for (service in services) {
                    Dlog.d(TAG, "service <${service.uuid}>")
                    for (characteristic in service.characteristics) {
                        Dlog.d(TAG, "(${getBluetoothGattProperty(characteristic.properties)}) ${characteristic.uuid}")
                        if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_READ)) {
                            gatt.readCharacteristic(characteristic)
                            readCharacteristic = characteristic
                        }
                        if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_NOTIFY)) {
                            gatt.setCharacteristicNotification(characteristic, true)
                            notifyCharacteristic = characteristic
                        }
                        Dlog.d(TAG, "success")
                    }
                }
            }
            Dlog.d(TAG, "onServicesDiscovered")
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            super.onCharacteristicChanged(gatt, characteristic, value)
//            Dlog.d(TAG, "onCharacteristicChanged ${characteristic.uuid} ${characteristic.value} $value")

            val intent = Intent(IntentID.BLE_DEVICE)
//            val intent = Intent(requireContext(), BluetoothDeviceReceiver::class.java)
            intent.putExtra(IntentKey.BLE_CHAR_CHANGED, value.toInt())
            requireContext().sendBroadcast(intent)
//            Dlog.d(TAG, value.toInt().toString())
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, value, status)

            Dlog.d(TAG, "onCharacteristicRead ${characteristic.uuid}")

            if (status == BluetoothGatt.GATT_SUCCESS) {

                Dlog.d(TAG, value.joinToString {
                    it.toInt().toString()
                })
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)

            Dlog.d(TAG, "onCharacteristicWrite")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_device_add_scan, container, false)

        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = null

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(object: MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_device_add_scan, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_search -> {
                        if (isScanning) {
                            menuItem.setIcon(R.drawable.ic_search)
                            stopScan()
                        } else {
                            menuItem.setIcon(R.drawable.ic_search_off)
                            scanDevices()
                        }
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        bm = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bm.adapter


        adapter = DeviceAddScanAdapter(arrayListOf()).apply {
            setClickCallback(deviceAddCallback)
        }
        binding.list.adapter = adapter

        // barcode
        barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
            if (requireContext().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return@registerForActivityResult
            gatt?.let {
                notifyCharacteristic?.let { characteristic ->
                    val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    Dlog.d(TAG, "descriptor: ${getBluetoothGattProperty(descriptor.characteristic.properties)} ${descriptor.permissions} ${descriptor.value}")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Dlog.d(TAG, "enable notification 1 ${it.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)}")
                    } else {
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        Dlog.d(TAG, "enable notification 2 ${it.writeDescriptor(descriptor)}")
                    }
                }
            }
            if (result.contents == null) {
                Dlog.d(TAG, "result null")
            } else {
                Dlog.d(TAG, "Scanned ${result.contents}")
                DeviceAddActivity.deviceBarcode = result.contents
                callbackListener?.scrollTo(DeviceAddActivity.DEVICE_ADD_PAGE_INFO)
            }
        }

        scanDevices()

    }

    private fun scanDevices() {
        if (requireContext().checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) return
        if (!bluetoothAdapter.isEnabled) return
        if (isScanning) return

        val scanner = bluetoothAdapter.bluetoothLeScanner
        Handler(Looper.getMainLooper()).postDelayed({
            isScanning = false
            scanner.stopScan(callback)
        }, 2 * 60 * 1000)

        isScanning = true
        scanner.startScan(callback)
    }

    private fun stopScan() {
        if (requireContext().checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) return
        if (!bluetoothAdapter.isEnabled) return
        if (!isScanning) return

        val scanner = bluetoothAdapter.bluetoothLeScanner
        isScanning = false
        scanner.stopScan(callback)
    }

    private val callback = object: ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            if (requireContext().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
//            if (result.device != null && result.device.name != null && hashDeviceMap[result.device.address] == null) {
            if (result.device != null && result.device.name == DeviceInfo.DEVICE_NAME && hashDeviceMap[result.device.address] == null) {
                hashDeviceMap[result.device.address] = result.device
                adapter.add(result.device)
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            super.onBatchScanResults(results)
            for (result in results) {
                Dlog.d(TAG, "BLE: $result")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
        }
    }

    fun setCallbackListener(callbackListener: DeviceAddActivityCallback?) {
        this.callbackListener = callbackListener
    }

    override fun onResume() {
        super.onResume()
        callbackListener?.setButtonEnabled(isPrev = true, isEnabled = false)
        callbackListener?.setButtonEnabled(isPrev = false, isEnabled = false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        @JvmStatic
        fun newInstance() = DeviceAddScanFragment().apply {
            val args = Bundle()
            arguments = args
        }
    }
}