package zone.ien.shampoo.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.AlarmManager
import android.content.*
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import kotlinx.coroutines.*
import zone.ien.shampoo.R
import zone.ien.shampoo.adapter.DeviceLogAdapter
import zone.ien.shampoo.constant.MessageType
import zone.ien.shampoo.databinding.ActivityDevModeBinding
import zone.ien.shampoo.room.DeviceLogDatabase
import zone.ien.shampoo.room.DeviceLogEntity
import zone.ien.shampoo.utils.MyUtils
import zone.ien.shampoo.utils.MyUtils.timeZero
import java.util.*

class DevModeActivity : AppCompatActivity() {

    lateinit var binding: ActivityDevModeBinding
    lateinit var sharedPreferences: SharedPreferences

    interface Callback {
        fun delete(id: Long)
    }

    private var logDatabase: DeviceLogDatabase? = null
    @OptIn(DelicateCoroutinesApi::class)
    private val logListCallback = object: Callback {
        override fun delete(id: Long) {
            GlobalScope.launch(Dispatchers.IO) {
                logDatabase?.getDao()?.delete(id)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_dev_mode)

        logDatabase = DeviceLogDatabase.getInstance(this)

        binding.btnAdd.setOnClickListener {
            startActivity(Intent(this, DevEditActivity::class.java))
        }

        GlobalScope.launch(Dispatchers.IO) {
            val battery = logDatabase?.getDao()?.getAll()?.sortedByDescending { it.timestamp }?.filter { it.battery != -1 } ?: listOf()
            val capacity = logDatabase?.getDao()?.getAll()?.sortedByDescending { it.timestamp }?.filter { it.capacity != -1 } ?: listOf()

            val itemsBattery = ArrayList<DeviceLogEntity>()
            val itemsCapacity = ArrayList<DeviceLogEntity>()
            var date = -1L

            for (item in battery) {
                if (!MyUtils.compareDate(date, item.timestamp)) {
                    date = item.timestamp
                    itemsBattery.add(DeviceLogEntity(item.parentId, item.timestamp, -1, -1))
                }
                itemsBattery.add(item)
            }

            date = -1L

            for (item in capacity) {
                if (!MyUtils.compareDate(date, item.timestamp)) {
                    date = item.timestamp
                    itemsCapacity.add(DeviceLogEntity(item.parentId, item.timestamp, -1, -1))
                }
                itemsCapacity.add(item)
            }

            val adapterBat = DeviceLogAdapter(itemsBattery, MessageType.TYPE_BATTERY).apply { setCallbackListener(logListCallback) }
            val adapterCap = DeviceLogAdapter(itemsCapacity, MessageType.TYPE_LEVEL).apply { setCallbackListener(logListCallback) }
            withContext(Dispatchers.Main) {
                binding.recyclerView.adapter = adapterBat
                binding.recyclerView2.adapter = adapterCap
            }
        }
    }
}