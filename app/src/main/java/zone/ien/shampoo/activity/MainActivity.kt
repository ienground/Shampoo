package zone.ien.shampoo.activity

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.CycleInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import zone.ien.shampoo.R
import zone.ien.shampoo.constant.ActionID
import zone.ien.shampoo.databinding.ActivityMainBinding
import zone.ien.shampoo.databinding.DialogPermissionBinding
import zone.ien.shampoo.fragment.MainDashboardFragment
import zone.ien.shampoo.fragment.MainStoreFragment
import zone.ien.shampoo.receiver.BluetoothDeviceReceiver
import zone.ien.shampoo.room.DeviceDatabase
import zone.ien.shampoo.utils.Dlog
import zone.ien.shampoo.utils.MyUtils

const val TAG = "ShampooTAG"

class MainActivity : AppCompatActivity(),
        MainDashboardFragment.OnFragmentInteractionListener,
        MainStoreFragment.OnFragmentInteractionListener
{

    lateinit var binding: ActivityMainBinding

    private var deviceDatabase: DeviceDatabase? = null
    private lateinit var bm: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.activity = this

        loadFragment(MainDashboardFragment.newInstance())

        binding.navView.setOnItemSelectedListener {
            loadFragment(when (it.itemId) {
                R.id.navigation_dashboard -> MainDashboardFragment.newInstance()
                R.id.navigation_store -> MainStoreFragment.newInstance()
                else -> MainDashboardFragment.newInstance()
            })
        }

        deviceDatabase = DeviceDatabase.getInstance(this)
        bm = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bm.adapter

        val intentFilter = IntentFilter()
        intentFilter.let {
            it.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            it.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            it.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            it.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            it.addAction(ActionID.ACTION_CONNECT_DEVICE)
            it.addAction(ActionID.ACTION_DISCONNECT_DEVICE)
            it.addAction(ActionID.ACTION_NOTIFY_DESCRIPTOR)
            it.addAction(ActionID.ACTION_SEND_DEVICE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(BluetoothDeviceReceiver(), intentFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(BluetoothDeviceReceiver(), intentFilter)
        }

        // First Visit
        if ((checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            || (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)) {
            val colorOnSecondaryContainer = MyUtils.getAttrColor(theme, com.google.android.material.R.attr.colorOnSecondaryContainer)
            val dialogBinding: DialogPermissionBinding = DataBindingUtil.inflate(layoutInflater, R.layout.dialog_permission, null, false)
            val dialog = MaterialAlertDialogBuilder(this, R.style.Theme_Shampoo_MaterialAlertDialog).apply {
                setIcon(R.drawable.ic_security)
                setTitle(R.string.permission_title)

                val nearbyDeviceLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                    if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                        dialogBinding.cardNearbyDevices.isChecked = true
                        dialogBinding.icNearbyDevices.setImageResource(R.drawable.ic_check_circle)
                        dialogBinding.tvNearbyDevices.text = getString(R.string.allowed)
                        dialogBinding.cardNearbyDevices.isEnabled = false
                        dialogBinding.tvNearbyDevices.setTextColor(colorOnSecondaryContainer)
                        dialogBinding.tvNearbyDevicesSub.setTextColor(colorOnSecondaryContainer)
                        dialogBinding.icNearbyDevices.imageTintList = ColorStateList.valueOf(colorOnSecondaryContainer)

                        connectDevices()
                    }
                }

                val postNotificationLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        dialogBinding.cardPostNotification.isChecked = true
                        dialogBinding.icPostNotification.setImageResource(R.drawable.ic_check_circle)
                        dialogBinding.tvPostNotification.text = getString(R.string.allowed)
                        dialogBinding.cardPostNotification.isEnabled = false
                        dialogBinding.tvPostNotification.setTextColor(colorOnSecondaryContainer)
                        dialogBinding.tvPostNotificationSub.setTextColor(colorOnSecondaryContainer)
                        dialogBinding.icPostNotification.imageTintList = ColorStateList.valueOf(colorOnSecondaryContainer)
                    }
                }

                val locationLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        dialogBinding.cardLocation.isChecked = true
                        dialogBinding.icLocation.setImageResource(R.drawable.ic_check_circle)
                        dialogBinding.tvLocation.text = getString(R.string.allowed)
                        dialogBinding.cardLocation.isEnabled = false
                        dialogBinding.tvLocation.setTextColor(colorOnSecondaryContainer)
                        dialogBinding.tvLocationSub.setTextColor(colorOnSecondaryContainer)
                        dialogBinding.icLocation.imageTintList = ColorStateList.valueOf(colorOnSecondaryContainer)
                    }
                }

                if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    dialogBinding.cardNearbyDevices.isChecked = true
                    dialogBinding.icNearbyDevices.setImageResource(R.drawable.ic_check_circle)
                    dialogBinding.tvNearbyDevices.text = getString(R.string.allowed)
                    dialogBinding.cardNearbyDevices.isEnabled = false
                }

                if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    dialogBinding.cardPostNotification.isChecked = true
                    dialogBinding.icPostNotification.setImageResource(R.drawable.ic_check_circle)
                    dialogBinding.tvPostNotification.text = getString(R.string.allowed)
                    dialogBinding.cardPostNotification.isEnabled = false
                }

                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    dialogBinding.cardLocation.isChecked = true
                    dialogBinding.icLocation.setImageResource(R.drawable.ic_check_circle)
                    dialogBinding.tvLocation.text = getString(R.string.allowed)
                    dialogBinding.cardLocation.isEnabled = false
                }

                dialogBinding.cardNearbyDevices.setOnClickListener {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        nearbyDeviceLauncher.launch(Manifest.permission.BLUETOOTH_SCAN)
                    }
                }

                dialogBinding.cardPostNotification.setOnClickListener {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        postNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                dialogBinding.cardLocation.setOnClickListener {
                    locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }

                setPositiveButton(R.string.close) { dialog, id ->
                    dialog.dismiss()
                }
                setCancelable(false)
                setView(dialogBinding.root)
            }.create()
            dialog.show()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if ((checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)) {
                    val colorError = MyUtils.getAttrColor(theme, com.google.android.material.R.attr.colorError)
                    dialog.window?.decorView?.animate()?.translationX(16f)?.interpolator = CycleInterpolator(7f)
                    if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        dialogBinding.tvNearbyDevices.setTextColor(colorError)
                        dialogBinding.tvNearbyDevicesSub.setTextColor(colorError)
                        dialogBinding.icNearbyDevices.imageTintList = ColorStateList.valueOf(colorError)
                    }
                    if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        dialogBinding.tvLocation.setTextColor(colorError)
                        dialogBinding.tvLocationSub.setTextColor(colorError)
                        dialogBinding.icLocation.imageTintList = ColorStateList.valueOf(colorError)
                    }
                    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        dialogBinding.tvPostNotification.setTextColor(colorError)
                        dialogBinding.tvPostNotificationSub.setTextColor(colorError)
                        dialogBinding.icPostNotification.imageTintList = ColorStateList.valueOf(colorError)
                    }
                } else {
                    dialog.dismiss()
                }
            }
        }

        if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            connectDevices()
        }
    }

    private fun loadFragment(fragment: Fragment?): Boolean {
        if (fragment != null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, fragment)
                .commit()
            return true
        }
        return false
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun connectDevices() {
        GlobalScope.launch(Dispatchers.IO) {
            val data = deviceDatabase?.getDao()?.getAll()
            data?.let {
                for (entity in it) {
                    Dlog.d(TAG, "try to connect ${entity.title}")
                    val device = bluetoothAdapter.getRemoteDevice(entity.address)
                    sendBroadcast(Intent(ActionID.ACTION_CONNECT_DEVICE).apply {
                        putExtra(BluetoothDevice.EXTRA_DEVICE, device)
                    })
                }
            }
        }
    }

    override fun onFragmentInteraction(uri: Uri) {}
}