package zone.ien.shampoo.activity

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zone.ien.shampoo.R
import zone.ien.shampoo.constant.ActionID
import zone.ien.shampoo.constant.ChannelID
import zone.ien.shampoo.constant.MessageType
import zone.ien.shampoo.constant.NotificationID
import zone.ien.shampoo.constant.PendingIntentReqCode
import zone.ien.shampoo.databinding.ActivityMainBinding
import zone.ien.shampoo.databinding.DialogPermissionBinding
import zone.ien.shampoo.fragment.MainDashboardFragment
import zone.ien.shampoo.fragment.MainStoreFragment
import zone.ien.shampoo.receiver.BTSyncReceiver
import zone.ien.shampoo.receiver.BluetoothDeviceReceiver
import zone.ien.shampoo.room.DeviceDatabase
import zone.ien.shampoo.room.NotificationsDatabase
import zone.ien.shampoo.room.NotificationsEntity
import zone.ien.shampoo.utils.ColorUtils.getAttrColor
import zone.ien.shampoo.utils.Colors
import zone.ien.shampoo.utils.Dlog
import zone.ien.shampoo.utils.MyUtils
import java.util.Calendar
import java.util.concurrent.TimeUnit

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
            val colorOnSecondaryContainer = getAttrColor(theme, Colors.colorOnSecondaryContainer)
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
                    val colorError = getAttrColor(theme, Colors.colorError)
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 0)
        }

        if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            connectDevices()
        }

        val context = applicationContext
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance()
        val pendingIntent = PendingIntent.getBroadcast(context, PendingIntentReqCode.BUBBLE_SCHEDULE_DAY, Intent(context, BTSyncReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, 60 * 1000L, pendingIntent)

//        GlobalScope.launch(Dispatchers.IO) {
//            val notificationsDatabase = NotificationsDatabase.getInstance(applicationContext)
//            var title = context.getString(R.string.battery_low_title, "1번 부스")
//            var content = context.getString(R.string.battery_low_content, 15)
//            var calendar = Calendar.getInstance().apply {
//                set(Calendar.DAY_OF_MONTH, 17)
//            }
//            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            nm.createNotificationChannel(NotificationChannel(ChannelID.LIQUID_LOW_ID, "low", NotificationManager.IMPORTANCE_HIGH))
//            nm.createNotificationChannel(NotificationChannel(ChannelID.BATTERY_LOW_ID, "low", NotificationManager.IMPORTANCE_HIGH))
//
//            NotificationCompat.Builder(context, ChannelID.BATTERY_LOW_ID).apply {
//                setContentTitle(title)
//                setContentText(content)
//                setSmallIcon(R.drawable.ic_battery_alert)
//                color = ContextCompat.getColor(context, R.color.md_theme_light_primary)
//
//                nm.notify(NotificationID.SHAMPOO_BATTERY_LOW + 1, build())
//            }
//
//            var notiEntity = NotificationsEntity(MessageType.TYPE_BATTERY_LOW, calendar.timeInMillis, title, content, 10)
//                        notificationsDatabase?.getDao()?.add(notiEntity)
//
//            title = context.getString(R.string.liquid_low_title, "1번 부스")
//            content = context.getString(R.string.liquid_low_content, 80)
//            notiEntity = NotificationsEntity(MessageType.TYPE_LEVEL_LOW, calendar.timeInMillis + 132 * 60 * 1000, title, content, 10)
//                        notificationsDatabase?.getDao()?.add(notiEntity)
//
//            NotificationCompat.Builder(context, ChannelID.LIQUID_LOW_ID).apply {
//                setContentTitle(title)
//                setContentText(content)
//                setSmallIcon(R.drawable.ic_shampoo_alert)
//                color = ContextCompat.getColor(context, R.color.md_theme_light_primary)
//
//                nm.notify(NotificationID.SHAMPOO_LIQUID_LOW + 1, build())
//            }
//
//
//        }
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