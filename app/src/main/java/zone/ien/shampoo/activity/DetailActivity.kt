package zone.ien.shampoo.activity

import android.animation.ValueAnimator
import android.app.AlarmManager
import android.content.Intent
import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.view.DragEvent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.MenuProvider
import androidx.core.view.iterator
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.EntryXComparator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.opencsv.CSVReader
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zone.ien.shampoo.BuildConfig
import zone.ien.shampoo.R
import zone.ien.shampoo.adapter.DeviceLogAdapter
import zone.ien.shampoo.constant.ActionID
import zone.ien.shampoo.constant.IntentKey
import zone.ien.shampoo.constant.IntentValue
import zone.ien.shampoo.constant.MessageType
import zone.ien.shampoo.databinding.ActivityDetailBinding
import zone.ien.shampoo.room.DeviceDatabase
import zone.ien.shampoo.room.DeviceEntity
import zone.ien.shampoo.room.DeviceLogDatabase
import zone.ien.shampoo.room.DeviceLogEntity
import zone.ien.shampoo.room.NotificationsDatabase
import zone.ien.shampoo.room.PlaceDatabase
import zone.ien.shampoo.utils.ColorUtils
import zone.ien.shampoo.utils.ColorUtils.getAttrColor
import zone.ien.shampoo.utils.Colors
import zone.ien.shampoo.utils.Dlog
import zone.ien.shampoo.utils.MyUtils
import zone.ien.shampoo.utils.MyUtils.getBatteryDrawable
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding

    private var deviceDatabase: DeviceDatabase? = null
    private var deviceLogDatabase: DeviceLogDatabase? = null
    private var notificationsDatabase: NotificationsDatabase? = null
    private var placeDatabase: PlaceDatabase? = null
    private var id: Long = -1
    private var device: DeviceEntity? = null

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_detail)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(ContextCompat.getDrawable(this, R.drawable.ic_arrow_back)?.apply { DrawableCompat.setTint(this, getAttrColor(theme, Colors.colorOnSecondaryContainer)) })

        addMenuProvider(object: MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_detail, menu)
                for (menuItem in menu.iterator()) {
                    menuItem.iconTintList = ColorStateList.valueOf(getAttrColor(theme, Colors.colorOnSecondaryContainer))
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_delete -> {
                        MaterialAlertDialogBuilder(this@DetailActivity, R.style.Theme_Shampoo_MaterialAlertDialog).apply {
                            setIcon(R.drawable.ic_delete)
                            setTitle(R.string.delete_title)
                            setMessage(R.string.delete_content)
                            setPositiveButton(android.R.string.ok) { dialog, _ ->
                                sendBroadcast(Intent(ActionID.ACTION_DISCONNECT_DEVICE).apply {
                                    putExtra(IntentKey.DEVICE_ADDRESS, device?.address)
                                })
                                GlobalScope.launch(Dispatchers.IO) {
                                    deviceDatabase?.getDao()?.delete(id)
                                    deviceLogDatabase?.getDao()?.deleteByParentId(id)
                                    notificationsDatabase?.getDao()?.deleteByDeviceId(id)
                                }
                                setResult(RESULT_OK, Intent().apply {
                                    putExtra(IntentKey.ACTION_TYPE, IntentValue.ACTION_DELETE)
                                    putExtra(IntentKey.DATA_ID, id)
                                })
                                finish()
                            }
                            setNegativeButton(android.R.string.cancel) { dialog, _ -> }
                        }.show()
                        true
                    }
                    R.id.menu_edit -> {
                        true
                    }
                    else -> false
                }
            }
        })

        val productList: ArrayList<Array<String>> = arrayListOf()
        val inputStream = assets.open("bubble_datasheet.csv")
        val csvReader = CSVReader(InputStreamReader(inputStream, "EUC-KR"))
        productList.addAll(csvReader.readAll())

        deviceDatabase = DeviceDatabase.getInstance(this)
        deviceLogDatabase = DeviceLogDatabase.getInstance(this)
        notificationsDatabase = NotificationsDatabase.getInstance(this)
        placeDatabase = PlaceDatabase.getInstance(this)

        val colorSurfaceVariant = getAttrColor(theme, Colors.colorSurfaceVariant)
        val colorError = getAttrColor(theme, Colors.colorError)
        val colorOnError = getAttrColor(theme, Colors.colorOnError)
        val colorErrorContainer = getAttrColor(theme, Colors.colorErrorContainer)
        val colorOnErrorContainer = getAttrColor(theme, Colors.colorOnErrorContainer)
        val colorOnSecondaryContainer = getAttrColor(theme, Colors.colorOnSecondaryContainer)

        binding.content.btnBattery.visibility = if (BuildConfig.DEBUG) View.VISIBLE else View.GONE
        binding.content.btnLiquid.visibility = if (BuildConfig.DEBUG) View.VISIBLE else View.GONE
        binding.content.btnMeasure.visibility = if (BuildConfig.DEBUG) View.VISIBLE else View.GONE

        binding.content.btnBattery.setOnClickListener {
            sendBroadcast(Intent(ActionID.ACTION_SEND_DEVICE).apply {
                putExtra(IntentKey.DEVICE_ADDRESS, device?.address)
                putExtra(IntentKey.MESSAGE_TYPE, MessageType.TYPE_BATTERY)
                putExtra(IntentKey.MESSAGE_VALUE, "Request")
            })
        }
        binding.content.btnBattery.setOnLongClickListener {
            AlertDialog.Builder(this).apply {
                val view = EditText(applicationContext)
                view.inputType = InputType.TYPE_NUMBER_FLAG_DECIMAL
                setPositiveButton(android.R.string.ok) { dialog, _ ->
                    sendBroadcast(Intent(ActionID.ACTION_SEND_DEVICE).apply {
                        putExtra(IntentKey.DEVICE_ADDRESS, device?.address)
                        putExtra(IntentKey.MESSAGE_TYPE, MessageType.TYPE_BATTERY)
                        putExtra(IntentKey.MESSAGE_VALUE, view.text.toString())
                    })
                }
                setNegativeButton(android.R.string.cancel) { _, _ -> }
                setView(view)
            }.show()
            true
        }
        binding.content.btnLiquid.setOnClickListener {
            sendBroadcast(Intent(ActionID.ACTION_SEND_DEVICE).apply {
                putExtra(IntentKey.DEVICE_ADDRESS, device?.address)
                putExtra(IntentKey.MESSAGE_TYPE, MessageType.TYPE_LEVEL)
                putExtra(IntentKey.MESSAGE_VALUE, "Request")
            })
        }
        binding.content.btnLiquid.setOnLongClickListener {
            AlertDialog.Builder(this).apply {
                val view = EditText(applicationContext)
                view.inputType = InputType.TYPE_NUMBER_FLAG_DECIMAL
                setPositiveButton(android.R.string.ok) { dialog, _ ->
                    sendBroadcast(Intent(ActionID.ACTION_SEND_DEVICE).apply {
                        putExtra(IntentKey.DEVICE_ADDRESS, device?.address)
                        putExtra(IntentKey.MESSAGE_TYPE, MessageType.TYPE_LEVEL)
                        putExtra(IntentKey.MESSAGE_VALUE, view.text.toString())
                    })
                }
                setNegativeButton(android.R.string.cancel) { _, _ -> }
                setView(view)
            }.show()
            true
        }
        binding.content.btnMeasure.setOnClickListener {
            sendBroadcast(Intent(ActionID.ACTION_SEND_DEVICE).apply {
                putExtra(IntentKey.DEVICE_ADDRESS, device?.address)
                putExtra(IntentKey.MESSAGE_TYPE, MessageType.TYPE_MEASURE_INIT)
                putExtra(IntentKey.MESSAGE_VALUE, "Request")
            })
        }
        binding.content.btnMeasure.setOnLongClickListener {
            sendBroadcast(Intent(ActionID.ACTION_NOTIFY_DESCRIPTOR).apply {
                putExtra(IntentKey.DEVICE_ADDRESS, device?.address)
            })
            true
        }

        binding.imgProduct.clipToOutline = true
        binding.tvProduct.isSelected = true
        binding.collapseToolbar.isTitleEnabled = true

        id = intent.getLongExtra(IntentKey.DATA_ID, -1)
        if (id != -1L) {
            GlobalScope.launch(Dispatchers.IO) {
                device = deviceDatabase?.getDao()?.get(id)
                Dlog.d(TAG, "$device")
                val logs = deviceLogDatabase?.getDao()?.getByParentId(id)?.sortedByDescending { it.timestamp }
                val place = placeDatabase?.getDao()?.get(device?.room ?: -1)

                withContext(Dispatchers.Main) {
                    device?.let { entity ->
                        val batteries = logs?.filter { it.battery != -1 }
                        val capacities = logs?.filter { it.capacity != -1 }
                        if (batteries?.isNotEmpty() == true) {
                            entity.battery = batteries[0].battery
                        }
                        if (capacities?.isNotEmpty() == true) {
                            entity.capacity = capacities[0].capacity
                        }

                        binding.collapseToolbar.title = entity.title
                        binding.tvProduct.text = entity.product
                        binding.tvBattery.text = "${entity.battery}%"
                        binding.icBattery.setImageResource(getBatteryDrawable(entity.battery))
                        binding.tvType.text = DeviceEntity.getTypeString(applicationContext, entity.type)
                        binding.content.tvLiquidMax.text = getString(R.string.capacity_max_format, entity.max)
                        binding.content.tvLiquidCurrent.text = getString(R.string.capacity_current_format, entity.capacity)
                        binding.content.progressBar.max = entity.max
                        binding.content.progressBar.progress = entity.capacity
                        binding.content.progressBar.setProgressFormatter { _, _ -> "" }

                        if (entity.capacity.toFloat() / entity.max <= 0.15) {
                            binding.content.tvLiquidCurrent.text = getString(R.string.capacity_current_format_warning, entity.capacity)
                            binding.content.icWarning.visibility = View.VISIBLE
                            ValueAnimator.ofArgb(colorSurfaceVariant, colorOnError).apply {
                                duration = 300
                                addUpdateListener {
                                    binding.content.progressBar.setProgressBackgroundColor(it.animatedValue as Int)
                                }
                            }.start()
                            ValueAnimator.ofArgb(binding.content.cardLiquid.cardBackgroundColor.defaultColor, colorErrorContainer).apply {
                                duration = 300
                                addUpdateListener {
                                    binding.content.cardLiquid.setCardBackgroundColor(it.animatedValue as Int)
                                }
                            }.start()
                            ValueAnimator.ofArgb(colorOnSecondaryContainer, colorOnErrorContainer).apply {
                                duration = 300
                                addUpdateListener {
                                    binding.content.tvLiquidMax.setTextColor(it.animatedValue as Int)
                                    binding.content.tvLiquidCurrent.setTextColor(it.animatedValue as Int)
                                    binding.content.icWarning.imageTintList = ColorStateList.valueOf(it.animatedValue as Int)
                                    binding.content.progressBar.setProgressStartColor(it.animatedValue as Int)
                                    binding.content.progressBar.setProgressEndColor(it.animatedValue as Int)
                                }
                            }.start()
                        }
                        if (entity.battery <= 15) {
                            ValueAnimator.ofArgb(colorOnSecondaryContainer, colorError).apply {
                                duration = 300
                                addUpdateListener {
                                    binding.tvBattery.setTextColor(it.animatedValue as Int)
                                    binding.icBattery.imageTintList = ColorStateList.valueOf(it.animatedValue as Int)
                                }
                            }.start()
                        }

                        val product = productList.find { it[3] == entity.model.toString() }
                        product?.let {
                            if (it[7] != "") {
                                Dlog.d(TAG, "link: ${it[7]}")
                                Glide.with(this@DetailActivity)
                                    .load(it[7])
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .error(R.drawable.ic_error)
                                    .into(binding.imgProduct)
                            }
                        }
                    }

                    place?.let {
                        binding.tvIcon.text = it.icon
                        binding.tvPlace.text = it.title
                    }

                    binding.content.graphUsage.let {
                        it.isDragDecelerationEnabled = false
                        it.setPinchZoom(false)
                        it.setScaleEnabled(false)
                        it.xAxis.axisMaximum = System.currentTimeMillis() / 1000f
                        it.xAxis.axisMinimum = (System.currentTimeMillis() - AlarmManager.INTERVAL_DAY) / 1000f
                        it.xAxis.granularity = 6 * AlarmManager.INTERVAL_HOUR / 1000f
                        it.xAxis.isGranularityEnabled = true
                        it.xAxis.labelCount = 4
                        it.legend.isEnabled = false
                        it.axisRight.isEnabled = false
                        it.xAxis.textColor = getAttrColor(theme, Colors.colorOnSecondaryContainer)
//                        it.minOffset = 0f
                    }
                    logs?.let { list ->
                        binding.content.tabs.getTabAt(1)?.select()
                        binding.content.tabs.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
                            override fun onTabSelected(tab: TabLayout.Tab) {
                                val items: ArrayList<DeviceLogEntity>
                                val type: Int

                                when (tab.position) {
                                    0 -> {
                                        items = list.filter { it.capacity != -1 }.take(5) as ArrayList
                                        type = MessageType.TYPE_LEVEL
                                        binding.content.graphUsage.axisLeft.axisMaximum = (device?.max ?: 1000).toFloat()
                                    }
                                    else -> {
                                        items = list.filter { it.battery != -1 }.take(5) as ArrayList
                                        type = MessageType.TYPE_BATTERY
                                        binding.content.graphUsage.axisLeft.axisMaximum = 100f
                                    }
                                }

                                // Chart
                                val itemsDate = ArrayList<DeviceLogEntity>()
                                var date = -1L
                                val entries = ArrayList<Entry>()
                                for (item in items) {
                                    entries.add(Entry(item.timestamp / 1000f, (if (tab.position == 0) item.capacity else item.battery).toFloat()))
                                    if (!MyUtils.compareDate(date, item.timestamp)) {
                                        date = item.timestamp
                                        itemsDate.add(DeviceLogEntity(id, item.timestamp, -1, -1))
                                    }
                                    itemsDate.add(item)
                                }
                                Collections.sort(entries, EntryXComparator())
                                val dataSet = LineDataSet(entries, "Label")
                                dataSet.color = getAttrColor(theme, Colors.colorPrimary)
                                val lineData = LineData(dataSet)
                                binding.content.graphUsage.xAxis.valueFormatter = object: IndexAxisValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        val date = Date((value * 1000L).toLong())
                                        val timeFormat = SimpleDateFormat(getString(R.string.timeFormat), Locale.getDefault())

                                        return timeFormat.format(date)
                                    }
                                }
                                binding.content.graphUsage.axisLeft.axisMinimum = 0f
                                binding.content.graphUsage.data = lineData
                                binding.content.graphUsage.invalidate()

                                val adapter = DeviceLogAdapter(itemsDate, type)
                                binding.content.list.adapter = adapter
                            }

                            override fun onTabUnselected(tab: TabLayout.Tab?) {}

                            override fun onTabReselected(tab: TabLayout.Tab?) {}
                        })

                        binding.content.tabs.getTabAt(0)?.select()
                    }
                }
            }
        }


    }
}