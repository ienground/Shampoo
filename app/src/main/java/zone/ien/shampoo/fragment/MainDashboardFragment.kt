package zone.ien.shampoo.fragment

import android.Manifest
import android.app.Activity
import android.app.Activity.RECEIVER_EXPORTED
import android.app.Activity.RESULT_OK
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.iterator
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zone.ien.shampoo.R
import zone.ien.shampoo.activity.DetailActivity
import zone.ien.shampoo.activity.DeviceAddActivity
import zone.ien.shampoo.activity.LogsActivity
import zone.ien.shampoo.activity.NotificationsActivity
import zone.ien.shampoo.activity.SettingsActivity
import zone.ien.shampoo.activity.TAG
import zone.ien.shampoo.adapter.DashboardAdapter
import zone.ien.shampoo.callback.DashboardCallback
import zone.ien.shampoo.constant.ActionID
import zone.ien.shampoo.constant.IntentID
import zone.ien.shampoo.constant.IntentKey
import zone.ien.shampoo.constant.IntentValue
import zone.ien.shampoo.room.DeviceEntity
import zone.ien.shampoo.databinding.FragmentMainDashboardBinding
import zone.ien.shampoo.receiver.BluetoothDeviceReceiver
import zone.ien.shampoo.room.DeviceDatabase
import zone.ien.shampoo.room.DeviceLogDatabase
import zone.ien.shampoo.utils.ColorUtils.getAttrColor
import zone.ien.shampoo.utils.Colors
import zone.ien.shampoo.utils.Dlog
import zone.ien.shampoo.utils.MyUtils
import java.util.Calendar

class MainDashboardFragment : Fragment() {

    private lateinit var binding: FragmentMainDashboardBinding

    private var mListener: OnFragmentInteractionListener? = null
    private var deviceDatabase: DeviceDatabase? = null
    private var deviceLogDatabase: DeviceLogDatabase? = null

    lateinit var deviceAddActivityLauncher: ActivityResultLauncher<Intent>
    lateinit var detailActivityLauncher: ActivityResultLauncher<Intent>

    private var dashboardCallback = object: DashboardCallback {
        override fun callback(position: Int, id: Long) {
            detailActivityLauncher.launch(Intent(requireContext(), DetailActivity::class.java).apply {
                putExtra(IntentKey.DATA_ID, id)
            })
        }
    }

    private var bluetoothConnectReceiver: BroadcastReceiver? = null
    private var bluetoothConnectStateReceiver: BroadcastReceiver? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main_dashboard, container, false)

        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = null

        return binding.root
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(object: MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)
                for (menuItem in menu.iterator()) {
                    menuItem.iconTintList = ColorStateList.valueOf(getAttrColor(requireContext().theme, Colors.colorOnSecondaryContainer))
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_add -> {
                        deviceAddActivityLauncher.launch(Intent(requireContext(), DeviceAddActivity::class.java))
                        true
                    }
                    R.id.menu_notifications -> {
                        startActivity(Intent(requireContext(), LogsActivity::class.java))
                        true
                    }
                    R.id.menu_settings -> {
                        startActivity(Intent(requireContext(), SettingsActivity::class.java))
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        deviceDatabase = DeviceDatabase.getInstance(requireContext())
        deviceLogDatabase = DeviceLogDatabase.getInstance(requireContext())

        binding.subTitle.text = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 6..10 -> getString(R.string.user_hello_morning)
            in 11..16 -> getString(R.string.user_hello_afternoon)
            in 17..20 -> getString(R.string.user_hello_evening)
            else -> getString(R.string.user_hello_night)
        }

        GlobalScope.launch(Dispatchers.IO) {
            val data = deviceDatabase?.getDao()?.getAll() as ArrayList?
            data?.let {
                for (entity in it) {
                    val battery = deviceLogDatabase?.getDao()?.getBattery(entity.id ?: -1, 5)
                    val capacity = deviceLogDatabase?.getDao()?.getCapacity(entity.id ?: -1, 5)

                    battery?.let {
                        if (it.isNotEmpty()) entity.battery = it[0].battery
                    }
                    capacity?.let {
                        if (it.isNotEmpty()) entity.capacity = it[0].capacity
                    }
                    requireContext().sendBroadcast(Intent(ActionID.ACTION_REQUEST_CONNECT_STATE).apply {
                        putExtra(IntentKey.DEVICE_ADDRESS, entity.address)
                    })
                }
                withContext(Dispatchers.Main) {
                    val adapter = DashboardAdapter(it).apply {
                        setClickCallback(dashboardCallback)
                    }
                    binding.list.adapter = adapter

                    if (data.isEmpty()) {
                        binding.icNoDevices.visibility = View.VISIBLE
                        binding.tvNoDevices.visibility = View.VISIBLE
                    }
                }
            }
        }

        deviceAddActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val id = result.data?.getLongExtra(IntentKey.DATA_ID, -1) ?: -1
                if (id != -1L) {
                    GlobalScope.launch(Dispatchers.IO) {
                        val entity = deviceDatabase?.getDao()?.get(id)

                        withContext(Dispatchers.Main) {
                            entity?.let {
                                (binding.list.adapter as DashboardAdapter).add(it)
                                binding.icNoDevices.visibility = if (binding.list.adapter?.itemCount == 0) View.VISIBLE else View.GONE
                                binding.tvNoDevices.visibility = if (binding.list.adapter?.itemCount == 0) View.VISIBLE else View.GONE
                            }
                        }
                    }
                }
            }
        }

        detailActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val id = result.data?.getLongExtra(IntentKey.DATA_ID, -1) ?: -1
                val actionType = result.data?.getIntExtra(IntentKey.ACTION_TYPE, -1) ?: -1
                if (id != -1L) {
                    when (actionType) {
                        IntentValue.ACTION_DELETE -> {
                            (binding.list.adapter as DashboardAdapter).delete(id)
                            binding.icNoDevices.visibility = if (binding.list.adapter?.itemCount == 0) View.VISIBLE else View.GONE
                            binding.tvNoDevices.visibility = if (binding.list.adapter?.itemCount == 0) View.VISIBLE else View.GONE
                        }
                    }
                }
            }
        }

        bluetoothConnectStateReceiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val address = intent.getStringExtra(IntentKey.DEVICE_ADDRESS) ?: ""
                val isConnected = intent.getBooleanExtra(IntentKey.CONNECT_STATE, false) ?: false

                (binding.list.adapter as DashboardAdapter).updateConnectionState(address, isConnected)
            }
        }
        bluetoothConnectReceiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return

                when (intent.action) {
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice?
                        }
                        Dlog.d(TAG, "ACTION_ACL_CONNECTED ${device?.name}")
                        (binding.list.adapter as DashboardAdapter).updateConnectionState(device?.address ?: "", true)
                    }

                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice?
                        }
                        Dlog.d(TAG, "ACTION_ACL_DISCONNECTED ${device?.name}")
                        (binding.list.adapter as DashboardAdapter).updateConnectionState(device?.address ?: "", false)
                    }
                }
            }
        }
        val intentFilter = IntentFilter()
        intentFilter.let {
            it.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            it.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            it.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            it.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(bluetoothConnectReceiver, intentFilter, RECEIVER_EXPORTED)
            requireContext().registerReceiver(bluetoothConnectStateReceiver, IntentFilter(IntentID.ACTION_RESPONSE_CONNECT_STATE), RECEIVER_EXPORTED)
        } else {
            requireContext().registerReceiver(bluetoothConnectReceiver, intentFilter)
            requireContext().registerReceiver(bluetoothConnectStateReceiver, IntentFilter(IntentID.ACTION_RESPONSE_CONNECT_STATE))
        }
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
        requireContext().unregisterReceiver(bluetoothConnectReceiver)
        requireContext().unregisterReceiver(bluetoothConnectStateReceiver)
    }

    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        @JvmStatic
        fun newInstance() = MainDashboardFragment().apply {
            val args = Bundle()
            arguments = args
        }
    }
}