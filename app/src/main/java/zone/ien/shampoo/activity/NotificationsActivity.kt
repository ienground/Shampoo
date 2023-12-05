package zone.ien.shampoo.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zone.ien.shampoo.R
import zone.ien.shampoo.adapter.NotificationsAdapter
import zone.ien.shampoo.databinding.ActivityNotificationsBinding
import zone.ien.shampoo.room.DeviceDatabase
import zone.ien.shampoo.room.NotificationsDatabase
import zone.ien.shampoo.utils.Dlog
import zone.ien.shampoo.utils.MyUtils

class NotificationsActivity : AppCompatActivity() {

    lateinit var binding: ActivityNotificationsBinding

    private var deviceDatabase: DeviceDatabase? = null
    private var notificationsDatabase: NotificationsDatabase? = null

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_notifications)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(ContextCompat.getDrawable(this, R.drawable.ic_arrow_back)?.apply { DrawableCompat.setTint(this, MyUtils.getAttrColor(theme, com.google.android.material.R.attr.colorOnSecondaryContainer)) })

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    android.R.id.home -> {
                        onBackPressedDispatcher.onBackPressed()
                        true
                    }

                    else -> false
                }
            }
        })

        deviceDatabase = DeviceDatabase.getInstance(this)
        notificationsDatabase = NotificationsDatabase.getInstance(this)

        GlobalScope.launch(Dispatchers.IO) {
            val data = deviceDatabase?.getDao()?.getAll()
            val notifications = notificationsDatabase?.getDao()?.getAll() as ArrayList
            data?.let {
                val maps: MutableMap<String, Pair<Long, String>> = mutableMapOf()
                for (device in data) {
                    maps[device.address] = Pair(device.id ?: -1, device.title)
                }
                val adapter = NotificationsAdapter(notifications)

                withContext(Dispatchers.Main) {
                    binding.list.adapter = adapter
//                    val adapter = ArrayAdapter(applicationContext, android.R.layout.simple_spinner_dropdown_item, maps.values.)
//                    binding.dropdownDeviceAuto.setAdapter(adapter)
//                    binding.dropdownDeviceAuto.onItemClickListener = OnItemClickListener { parentView, selectedItemView, position, id ->
//                        Dlog.d(TAG, "adapter ${maps.keys.toList()[position]}")
//                    }
                }
            }
        }


    }
}