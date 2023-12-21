package zone.ien.shampoo.activity

import android.content.*
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import kotlinx.coroutines.*
import zone.ien.shampoo.R
import zone.ien.shampoo.databinding.ActivityDevEditBinding
import zone.ien.shampoo.room.DeviceDatabase
import zone.ien.shampoo.room.DeviceLogDatabase
import zone.ien.shampoo.room.DeviceLogEntity
import zone.ien.shampoo.utils.MyUtils
import java.text.SimpleDateFormat
import java.util.*

class DevEditActivity : AppCompatActivity() {

    lateinit var binding: ActivityDevEditBinding
    private val calendar = Calendar.getInstance()
    private val timeZone = TimeZone.getDefault()
    private lateinit var dateFormat: SimpleDateFormat

    private var deviceDatabase: DeviceDatabase? = null
    private var deviceLogDatabase: DeviceLogDatabase? = null
    private var selectionDeviceId = -1L

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_dev_edit)
        dateFormat = SimpleDateFormat("${getString(R.string.dateFormat)} ${getString(R.string.timeFormat)}", Locale.getDefault())

        deviceDatabase = DeviceDatabase.getInstance(this)
        deviceLogDatabase = DeviceLogDatabase.getInstance(this)

        binding.btnTime.setOnClickListener {
            val datePicker = getDatePickerDialog(R.string.select_start_date, calendar, it as MaterialButton)
            datePicker.show(supportFragmentManager, "START_DATE_PICKER")
        }

        binding.btnSave.setOnClickListener {
            val entity = DeviceLogEntity(selectionDeviceId, calendar.timeInMillis, binding.inputBattery.text.toString().let { if (it != "") it.toInt() else -1 }, binding.inputCapacity.text.toString().let { if (it != "") it.toInt() else -1 })
            GlobalScope.launch(Dispatchers.IO) {
                deviceLogDatabase?.getDao()?.add(entity)
            }
        }

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

                withContext(Dispatchers.Main) {
                    val arrayAdapter = ArrayAdapter(applicationContext, android.R.layout.simple_spinner_dropdown_item, maps.map { it.value.second })
                    binding.spinner.adapter = arrayAdapter
                    binding.spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            selectionDeviceId = maps.toList()[position].second.first
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                }
            }
        }
    }

    private fun getDatePickerDialog(@StringRes title: Int, calendar: Calendar, button: MaterialButton): MaterialDatePicker<Long> {

        var isDateSelected = false
        var isTimeSelected = false
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
            isDateSelected = true
            val scheduledTime = Calendar.getInstance().apply { timeInMillis = calendar.timeInMillis + 60 * 1000 }
            val timePicker = MaterialTimePicker.Builder()
                .setTitleText(title)
                .setPositiveButtonText(android.R.string.ok)
                .setNegativeButtonText(android.R.string.cancel)
                .setHour(scheduledTime.get(Calendar.HOUR_OF_DAY))
                .setMinute(scheduledTime.get(Calendar.MINUTE))
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .build()
            timePicker.addOnPositiveButtonClickListener { _ ->
                isTimeSelected = true
                val c = Calendar.getInstance().apply { timeInMillis = it }
                c.set(Calendar.HOUR_OF_DAY, timePicker.hour)
                c.set(Calendar.MINUTE, timePicker.minute)
                calendar.timeInMillis = c.timeInMillis
                button.text = dateFormat.format(calendar.time)
            }
            timePicker.addOnNegativeButtonClickListener {  }
            timePicker.show(supportFragmentManager, "TIME_PICKER_IN_DATE")
        }
        datePicker.addOnNegativeButtonClickListener { }

        return datePicker
    }
}