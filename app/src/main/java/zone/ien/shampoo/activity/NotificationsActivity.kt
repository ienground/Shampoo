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
import android.widget.AdapterView.OnItemClickListener
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
import zone.ien.shampoo.adapter.NotificationsAdapter
import zone.ien.shampoo.databinding.ActivityNotificationsBinding
import zone.ien.shampoo.room.DeviceDatabase
import zone.ien.shampoo.room.DeviceLogEntity
import zone.ien.shampoo.room.NotificationsDatabase
import zone.ien.shampoo.room.NotificationsEntity
import zone.ien.shampoo.utils.ColorUtils.getAttrColor
import zone.ien.shampoo.utils.Colors
import zone.ien.shampoo.utils.Dlog
import zone.ien.shampoo.utils.MyUtils
import zone.ien.shampoo.utils.MyUtils.timeZero
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class NotificationsActivity : AppCompatActivity() {

    lateinit var binding: ActivityNotificationsBinding

    private var deviceDatabase: DeviceDatabase? = null
    private var notificationsDatabase: NotificationsDatabase? = null
    private val startCalendar = Calendar.getInstance()
    private val endCalendar = Calendar.getInstance()
    private var selectionDeviceId = -1L

    private val timeZone = TimeZone.getDefault()
    private lateinit var dateFormat: SimpleDateFormat

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_notifications)

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
        notificationsDatabase = NotificationsDatabase.getInstance(this)
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
                    if (binding.icNoNotifications.visibility == View.VISIBLE) binding.icNoNotifications.alpha = (1f - it.animatedValue as Float) * 0.4f
                    if (binding.tvNoNotifications.visibility == View.VISIBLE) binding.tvNoNotifications.alpha = (1f - it.animatedValue as Float) * 0.4f
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
                val notifications = notificationsDatabase?.getDao()?.getByDeviceId(selectionDeviceId, startCalendar.timeZero().timeInMillis, endCalendar.timeZero().timeInMillis + AlarmManager.INTERVAL_DAY) as ArrayList
                val items = ArrayList<NotificationsEntity>()
                var date = -1L
                for (item in notifications) {
                    if (!MyUtils.compareDate(date, item.timestamp)) {
                        date = item.timestamp
                        items.add(NotificationsEntity(-1, date, "", "", -1L))
                    }
                    items.add(item)
                }

                inflateData(items)
            }
        }

        binding.shimmerFrame.startShimmer()
        binding.shimmerFrame.visibility = View.VISIBLE
        binding.list.visibility = View.INVISIBLE
        binding.list.alpha = 0f

        GlobalScope.launch(Dispatchers.IO) {
            val data = deviceDatabase?.getDao()?.getAll()
            data?.let {
                val maps: MutableMap<String, Pair<Long, String>> = mutableMapOf()
                for (device in data) {
                    maps[device.address] = Pair(device.id ?: -1, device.title)
                }
                if (it.isNotEmpty()) {
                    selectionDeviceId = it[0].id ?: -1
                }
                val notifications = notificationsDatabase?.getDao()?.getByDeviceId(selectionDeviceId, startCalendar.timeZero().timeInMillis, endCalendar.timeZero().timeInMillis + AlarmManager.INTERVAL_DAY) as ArrayList
                val items = ArrayList<NotificationsEntity>()
                var date = -1L
                for (item in notifications) {
                    if (!MyUtils.compareDate(date, item.timestamp)) {
                        date = item.timestamp
                        items.add(NotificationsEntity(-1, date, "", "", -1L))
                    }
                    items.add(item)
                }

                withContext(Dispatchers.Main) {
                    val arrayAdapter = ArrayAdapter(applicationContext, android.R.layout.simple_spinner_dropdown_item, maps.map { it.value.second })
                    binding.dropdownDeviceAuto.setAdapter(arrayAdapter)
                    binding.dropdownDeviceAuto.onItemClickListener = OnItemClickListener { parentView, selectedItemView, position, id ->
                        selectionDeviceId = maps.toList()[position].second.first
                    }
                    if (it.isNotEmpty()) {
                        binding.dropdownDeviceAuto.setText(it[0].title, false)
                    }
                }

                inflateData(items)
            }
        }
    }

    private suspend fun inflateData(items: ArrayList<NotificationsEntity>) {
        val adapter = NotificationsAdapter(items)

        withContext(Dispatchers.Main) {
            binding.list.adapter = adapter
            binding.shimmerFrame.stopShimmer()
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 300
                addUpdateListener {
                    binding.list.alpha = it.animatedValue as Float
                    binding.shimmerFrame.alpha = 1f - it.animatedValue as Float

                    if (items.isEmpty()) {
                        binding.icNoNotifications.alpha = (it.animatedValue as Float) * 0.4f
                        binding.tvNoNotifications.alpha = (it.animatedValue as Float) * 0.4f
                    }
                }
                addListener(object: AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        super.onAnimationStart(animation)
                        binding.list.visibility = View.VISIBLE
                        if (items.isEmpty()) {
                            binding.icNoNotifications.visibility = View.VISIBLE
                            binding.tvNoNotifications.visibility = View.VISIBLE
                        } else {
                            binding.icNoNotifications.visibility = View.GONE
                            binding.tvNoNotifications.visibility = View.GONE
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