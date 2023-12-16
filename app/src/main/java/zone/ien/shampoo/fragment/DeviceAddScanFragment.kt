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
import android.content.res.ColorStateList
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
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.MenuProvider
import androidx.core.view.iterator
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import zone.ien.shampoo.R
import zone.ien.shampoo.activity.DeviceAddActivity
import zone.ien.shampoo.activity.TAG
import zone.ien.shampoo.adapter.DeviceAddScanAdapter
import zone.ien.shampoo.callback.DeviceAddActivityCallback
import zone.ien.shampoo.callback.DeviceAddCallback
import zone.ien.shampoo.constant.ActionID
import zone.ien.shampoo.constant.DeviceInfo
import zone.ien.shampoo.constant.IntentID
import zone.ien.shampoo.constant.IntentKey
import zone.ien.shampoo.databinding.FragmentDeviceAddScanBinding
import zone.ien.shampoo.receiver.BluetoothDeviceReceiver
import zone.ien.shampoo.utils.ColorUtils.getAttrColor
import zone.ien.shampoo.utils.Colors
import zone.ien.shampoo.utils.Dlog
import zone.ien.shampoo.utils.MyUtils
import zone.ien.shampoo.utils.MyUtils.getBluetoothGattProperty
import zone.ien.shampoo.utils.MyUtils.toDouble
import java.util.UUID

class DeviceAddScanFragment : Fragment() {

    private lateinit var binding: FragmentDeviceAddScanBinding

    private var mListener: OnFragmentInteractionListener? = null
    private var callbackListener: DeviceAddActivityCallback? = null

    private lateinit var bm: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var isScanning = false
    private var hashDeviceMap = HashMap<String, BluetoothDevice>()
    private var menuSearch: MenuItem? = null
    private lateinit var adapter: DeviceAddScanAdapter

    private lateinit var barcodeLauncher: ActivityResultLauncher<ScanOptions>
    private val deviceAddCallback = object: DeviceAddCallback {
        override fun add(device: BluetoothDevice) {
            requireContext().sendBroadcast(Intent(ActionID.ACTION_CONNECT_DEVICE).apply {
                putExtra(BluetoothDevice.EXTRA_DEVICE, device)
            })
            barcodeLauncher.launch(ScanOptions().apply {
                setOrientationLocked(true)
                setCameraId(1)
            })
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_device_add_scan, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(object: MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_device_add_scan, menu)
                for (menuItem in menu.iterator()) {
                    menuItem.iconTintList = ColorStateList.valueOf(getAttrColor(requireContext().theme, Colors.colorOnSecondaryContainer))
                    if (menuItem.itemId == R.id.menu_search) menuSearch = menuItem
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    android.R.id.home -> {
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                        true
                    }
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
            requireContext().sendBroadcast(Intent(ActionID.ACTION_NOTIFY_DESCRIPTOR).apply {
                putExtra(IntentKey.DEVICE_ADDRESS, DeviceAddActivity.deviceAddress)
            })
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
            menuSearch?.setIcon(R.drawable.ic_search)
        }, 10 * 1000)

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

            if (context != null) {
                if (requireContext().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
                Dlog.d(TAG, "${result.device.address} ${result.device.name}")
                if (result.device != null && result.device.name.equals(DeviceInfo.DEVICE_NAME, ignoreCase = true) && hashDeviceMap[result.device.address] == null) {
                    hashDeviceMap[result.device.address] = result.device
                    adapter.add(result.device)
                }
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