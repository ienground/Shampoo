package zone.ien.shampoo.activity

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.content.Context
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
import zone.ien.shampoo.databinding.ActivityMainBinding
import zone.ien.shampoo.fragment.MainDashboardFragment
import zone.ien.shampoo.room.DeviceDatabase
import zone.ien.shampoo.utils.Dlog
import zone.ien.shampoo.utils.MyUtils.toInt

const val TAG = "ShampooTAG"

class MainActivity : AppCompatActivity(),
        MainDashboardFragment.OnFragmentInteractionListener
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
                R.id.navigation_store -> MainDashboardFragment.newInstance()
                else -> MainDashboardFragment.newInstance()
            })
        }

        deviceDatabase = DeviceDatabase.getInstance(this)
        bm = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bm.adapter

        GlobalScope.launch(Dispatchers.IO) {
            val data = deviceDatabase?.getDao()?.getAll()
            data?.let {
                for (entity in it) {
                    val device = bluetoothAdapter.getRemoteDevice(entity.address)
                    if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return@let
                    device.connectGatt(applicationContext, true, object: BluetoothGattCallback() {
                        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                            super.onConnectionStateChange(gatt, status, newState)
                            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
                            when (status) {
                                BluetoothGatt.GATT_FAILURE, 133 -> { // unknown code 133
                                    gatt?.disconnect()
                                    gatt?.close()
                                }
                                BluetoothGatt.GATT_SUCCESS -> {
                                    if (newState == BluetoothGatt.STATE_CONNECTED) {
                                        Dlog.d(TAG, "Connected to ${gatt?.device?.name}")
                                    }
                                }
                            }
                        }

                        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
                            super.onCharacteristicRead(gatt, characteristic, value, status)
                            Dlog.d(TAG, "value: $value")
                        }
                        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                            super.onCharacteristicChanged(gatt, characteristic, value)
                            Dlog.d(TAG, "change: ${value.toInt()}")
                        }
                    })
                    Dlog.d(TAG, "device: ${device.name} ${device.address} ${device}")
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