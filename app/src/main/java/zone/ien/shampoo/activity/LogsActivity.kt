package zone.ien.shampoo.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.AlarmManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import com.github.mikephil.charting.data.Entry
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zone.ien.shampoo.R
import zone.ien.shampoo.adapter.DeviceLogAdapter
import zone.ien.shampoo.constant.IntentKey
import zone.ien.shampoo.constant.MessageType
import zone.ien.shampoo.databinding.ActivityLogsBinding
import zone.ien.shampoo.room.DeviceDatabase
import zone.ien.shampoo.room.DeviceEntity
import zone.ien.shampoo.room.DeviceLogDatabase
import zone.ien.shampoo.room.DeviceLogEntity
import zone.ien.shampoo.room.NotificationsEntity
import zone.ien.shampoo.utils.ColorUtils.getAttrColor
import zone.ien.shampoo.utils.Colors
import zone.ien.shampoo.utils.MyUtils
import zone.ien.shampoo.utils.MyUtils.timeZero
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class LogsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogsBinding

    private var deviceDatabase: DeviceDatabase? = null
    private var deviceLogDatabase: DeviceLogDatabase? = null
    private val startCalendar = Calendar.getInstance()
    private val endCalendar = Calendar.getInstance()

    private val timeZone = TimeZone.getDefault()
    private lateinit var dateFormat: SimpleDateFormat
    private var device: DeviceEntity? = null

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_logs)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(ContextCompat.getDrawable(this, R.drawable.ic_arrow_back)?.apply { DrawableCompat.setTint(this, getAttrColor(theme, Colors.colorOnSecondaryContainer)) })

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
        deviceLogDatabase = DeviceLogDatabase.getInstance(this)

        val address = intent.getStringExtra(IntentKey.DEVICE_ADDRESS)
        val type = intent.getIntExtra(IntentKey.LOG_TYPE, -1)

        dateFormat = SimpleDateFormat(getString(R.string.dateShortFormat), Locale.getDefault())

        binding.shimmerFirst.divider.visibility = View.GONE
        binding.tvStartDate.text = dateFormat.format(startCalendar.time)
        binding.tvEndDate.text = dateFormat.format(endCalendar.time)
        binding.tvStartDate.setOnClickListener {
            val datePicker = getDatePickerDialog(R.string.select_start_date, startCalendar, it as Chip)
            datePicker.show(supportFragmentManager, "START_DATE_PICKER")
        }
        binding.tvEndDate.setOnClickListener {
            val datePicker = getDatePickerDialog(R.string.select_end_date, endCalendar, it as Chip)
            datePicker.show(supportFragmentManager, "END_DATE_PICKER")
        }
        binding.btnSearch.setOnClickListener {
            binding.shimmerFrame.startShimmer()
            binding.shimmerFrame.visibility = View.VISIBLE

            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 300
                addUpdateListener {
                    binding.list.alpha = 1f - it.animatedValue as Float
                    binding.shimmerFrame.alpha = it.animatedValue as Float
                    if (binding.icNoHistory.visibility == View.VISIBLE) binding.icNoHistory.alpha = (1f - it.animatedValue as Float) * 0.4f
                    if (binding.tvNoHistory.visibility == View.VISIBLE) binding.tvNoHistory.alpha = (1f - it.animatedValue as Float) * 0.4f
                }
                addListener(object: AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        super.onAnimationStart(animation)
                        binding.list.visibility = View.INVISIBLE
                    }
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        binding.shimmerFrame.visibility = View.INVISIBLE
                    }
                })
            }.start()

            GlobalScope.launch(Dispatchers.IO) {
                device?.let { inflateData(it, type) }
            }
        }

        binding.shimmerFrame.startShimmer()
        binding.shimmerFrame.visibility = View.VISIBLE
        binding.list.visibility = View.INVISIBLE
        binding.list.alpha = 0f

        GlobalScope.launch(Dispatchers.IO) {
            device = deviceDatabase?.getDao()?.get(address ?: "")
            device?.let { inflateData(it, type) }
        }

    }

    private suspend fun inflateData(device: DeviceEntity, type: Int) {
        val data = (
                if (type == MessageType.TYPE_LEVEL) deviceLogDatabase?.getDao()?.getCapacity(device.id ?: -1, startCalendar.timeZero().timeInMillis, endCalendar.timeZero().timeInMillis + AlarmManager.INTERVAL_DAY)
                else deviceLogDatabase?.getDao()?.getBattery(device.id ?: -1, startCalendar.timeZero().timeInMillis, endCalendar.timeZero().timeInMillis + AlarmManager.INTERVAL_DAY)) as ArrayList

        val items = ArrayList<DeviceLogEntity>()
        var date = -1L
        for (item in data) {
            if (!MyUtils.compareDate(date, item.timestamp)) {
                date = item.timestamp
                items.add(DeviceLogEntity(device.id ?: -1, item.timestamp, -1, -1))
            }
            items.add(item)
        }

        val adapter = DeviceLogAdapter(items, type)
        withContext(Dispatchers.Main) {
            binding.list.adapter = adapter
            binding.shimmerFrame.stopShimmer()
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 300
                addUpdateListener {
                    binding.list.alpha = it.animatedValue as Float
                    binding.shimmerFrame.alpha = 1f - it.animatedValue as Float

                    if (items.isEmpty()) {
                        binding.icNoHistory.alpha = (it.animatedValue as Float) * 0.4f
                        binding.tvNoHistory.alpha = (it.animatedValue as Float) * 0.4f
                    }
                }
                addListener(object: AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        super.onAnimationStart(animation)
                        binding.list.visibility = View.VISIBLE
                        if (items.isEmpty()) {
                            binding.icNoHistory.visibility = View.VISIBLE
                            binding.tvNoHistory.visibility = View.VISIBLE
                        } else {
                            binding.icNoHistory.visibility = View.GONE
                            binding.tvNoHistory.visibility = View.GONE
                        }
                    }
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        binding.shimmerFrame.visibility = View.INVISIBLE
                    }
                })
            }.start()
        }
    }

    private fun getDatePickerDialog(@StringRes title: Int, calendar: Calendar, chip: Chip): MaterialDatePicker<Long> {
        val constraintsBuilder= CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(title)
            .setPositiveButtonText(android.R.string.ok)
            .setNegativeButtonText(android.R.string.cancel)
            .setSelection(calendar.timeInMillis.let { it + timeZone.getOffset(it) })
            .setCalendarConstraints(constraintsBuilder.build())
            .build()
        datePicker.addOnPositiveButtonClickListener {
            calendar.timeInMillis = it
            chip.text = dateFormat.format(calendar.time)
        }
        datePicker.addOnNegativeButtonClickListener { }

        return datePicker
    }
}