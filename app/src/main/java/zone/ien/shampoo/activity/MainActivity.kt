package zone.ien.shampoo.activity

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import zone.ien.shampoo.R
import zone.ien.shampoo.constant.ActionID
import zone.ien.shampoo.databinding.ActivityMainBinding
import zone.ien.shampoo.fragment.MainDashboardFragment
import zone.ien.shampoo.fragment.MainStoreFragment
import zone.ien.shampoo.receiver.BluetoothDeviceReceiver
import zone.ien.shampoo.room.DeviceDatabase
import zone.ien.shampoo.utils.Dlog
import zone.ien.shampoo.utils.MyUtils.toInt

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
            it.addAction(ActionID.ACTION_NOTIFY_DESCRIPTOR)
            it.addAction(ActionID.ACTION_SEND_DEVICE)
        }

        registerReceiver(BluetoothDeviceReceiver(), intentFilter, RECEIVER_EXPORTED)

        GlobalScope.launch(Dispatchers.IO) {
            val data = deviceDatabase?.getDao()?.getAll()
            data?.let {
                for (entity in it) {
                    val device = bluetoothAdapter.getRemoteDevice(entity.address)
                    sendBroadcast(Intent(ActionID.ACTION_CONNECT_DEVICE).apply {
                        putExtra(BluetoothDevice.EXTRA_DEVICE, device)
                    })
                }
            }
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

    override fun onFragmentInteraction(uri: Uri) {}
}