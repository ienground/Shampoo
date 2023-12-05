package zone.ien.shampoo.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zone.ien.shampoo.R
import zone.ien.shampoo.adapter.DeviceLogAdapter
import zone.ien.shampoo.constant.ActionID
import zone.ien.shampoo.constant.IntentKey
import zone.ien.shampoo.constant.MessageType
import zone.ien.shampoo.databinding.ActivityDetailBinding
import zone.ien.shampoo.room.DeviceDatabase
import zone.ien.shampoo.room.DeviceLogDatabase
import zone.ien.shampoo.room.DeviceLogEntity
import zone.ien.shampoo.utils.Dlog

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding

    private var deviceDatabase: DeviceDatabase? = null
    private var deviceLogDatabase: DeviceLogDatabase? = null
    private var id: Long = -1
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_detail)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = null

        addMenuProvider(object: MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_detail, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_delete -> {
                        MaterialAlertDialogBuilder(this@DetailActivity, R.style.Theme_Shampoo_MaterialAlertDialog).apply {
                            setIcon(R.drawable.ic_delete)
                            setTitle(R.string.delete_title)
                            setMessage(R.string.delete_content)
                            setPositiveButton(android.R.string.ok) { dialog, _ ->
                                GlobalScope.launch(Dispatchers.IO) {
                                    deviceDatabase?.getDao()?.delete(id)
                                }
                                finish()
                            }
                            setNegativeButton(android.R.string.cancel) { dialog, _ ->

                            }
                        }.show()
                        true
                    }
                    else -> false
                }
            }
        })


        deviceDatabase = DeviceDatabase.getInstance(this)
        deviceLogDatabase = DeviceLogDatabase.getInstance(this)

        id = intent.getLongExtra(IntentKey.DATA_ID, -1)
        if (id != -1L) {
            GlobalScope.launch(Dispatchers.IO) {
                val data = deviceDatabase?.getDao()?.get(id)
                val logs = deviceLogDatabase?.getDao()?.getByParentId(id)?.sortedByDescending { it.timestamp }

                withContext(Dispatchers.Main) {
                    data?.let {
                        binding.tvContentLocation.text = it.room.toString()
                        binding.tvContentType.text = it.product
                        binding.tvContentCapacity.text = it.max.toString()
                    }

                    logs?.let { list ->
                        binding.tabs.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
                            override fun onTabSelected(tab: TabLayout.Tab) {
                                val items: ArrayList<DeviceLogEntity>
                                val type: Int

                                when (tab.position) {
                                    0 -> {
                                        items = list.filter { it.capacity != -1 } as ArrayList
                                        type = MessageType.TYPE_LEVEL
                                    }
                                    else -> {items = list.filter { it.battery != -1 } as ArrayList
                                        type = MessageType.TYPE_BATTERY
                                    }
                                }

                                val adapter = DeviceLogAdapter(items, type)
                                binding.list.adapter = adapter
                            }

                            override fun onTabUnselected(tab: TabLayout.Tab?) {}

                            override fun onTabReselected(tab: TabLayout.Tab?) {}
                        })

                        val adapter = DeviceLogAdapter(list.filter { it.capacity != -1 } as ArrayList, MessageType.TYPE_LEVEL)
                        binding.list.adapter = adapter
                    }
                }
            }
        }

        binding.cardImage.setOnClickListener {
            sendBroadcast(Intent(ActionID.ACTION_SEND_DEVICE).apply {
                putExtra(IntentKey.MESSAGE_TYPE, MessageType.TYPE_BATTERY)
                putExtra(IntentKey.MESSAGE_VALUE, "Request")
            })
        }

        binding.btnBattery.setOnClickListener {
            sendBroadcast(Intent(ActionID.ACTION_SEND_DEVICE).apply {
                putExtra(IntentKey.MESSAGE_TYPE, MessageType.TYPE_BATTERY)
                putExtra(IntentKey.MESSAGE_VALUE, "Request")
            })
        }
        binding.btnBattery.setOnLongClickListener {
            sendBroadcast(Intent(ActionID.ACTION_SEND_DEVICE).apply {
                putExtra(IntentKey.MESSAGE_TYPE, MessageType.TYPE_BATTERY_LOW)
                putExtra(IntentKey.MESSAGE_VALUE, "Request")
            })
            true
        }
        binding.btnLiquid.setOnClickListener {
            sendBroadcast(Intent(ActionID.ACTION_SEND_DEVICE).apply {
                putExtra(IntentKey.MESSAGE_TYPE, MessageType.TYPE_LEVEL)
                putExtra(IntentKey.MESSAGE_VALUE, "Request")
            })
        }
        binding.btnLiquid.setOnLongClickListener {
            sendBroadcast(Intent(ActionID.ACTION_SEND_DEVICE).apply {
                putExtra(IntentKey.MESSAGE_TYPE, MessageType.TYPE_LEVEL_LOW)
                putExtra(IntentKey.MESSAGE_VALUE, "Request")
            })
            true
        }
        binding.btnMeasure.setOnClickListener {
            sendBroadcast(Intent(ActionID.ACTION_SEND_DEVICE).apply {
                putExtra(IntentKey.MESSAGE_TYPE, MessageType.TYPE_MEASURE_INIT)
                putExtra(IntentKey.MESSAGE_VALUE, "Request")
            })
        }
        binding.btnMeasure.setOnLongClickListener {
            sendBroadcast(Intent(ActionID.ACTION_NOTIFY_DESCRIPTOR))
            true
        }
    }
}