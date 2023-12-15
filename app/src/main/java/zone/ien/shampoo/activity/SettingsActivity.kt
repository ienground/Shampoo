package zone.ien.shampoo.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.location.Geocoder
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.animation.CycleInterpolator
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.MenuProvider
import androidx.core.view.iterator
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.*
import zone.ien.shampoo.BuildConfig
import zone.ien.shampoo.R
import zone.ien.shampoo.adapter.EmojiMoreAdapter
import zone.ien.shampoo.adapter.PlaceAdapter
import zone.ien.shampoo.adapter.SettingsCategoryAdapter
import zone.ien.shampoo.callback.EmojiClickCallback
import zone.ien.shampoo.callback.PlaceCallback
import zone.ien.shampoo.callback.PreferenceCallback
import zone.ien.shampoo.constant.SavedInstanceKey
import zone.ien.shampoo.constant.SharedKey
import zone.ien.shampoo.data.CategoryObject
import zone.ien.shampoo.databinding.ActivitySettingsBinding
import zone.ien.shampoo.databinding.DialogEmojiMoreBinding
import zone.ien.shampoo.databinding.DialogInputBinding
import zone.ien.shampoo.databinding.FragmentNotificationsBinding
import zone.ien.shampoo.databinding.FragmentPlaceBinding
import zone.ien.shampoo.room.DeviceDatabase
import zone.ien.shampoo.room.PlaceDatabase
import zone.ien.shampoo.room.PlaceEntity
import zone.ien.shampoo.utils.ColorUtils.getAttrColor
import zone.ien.shampoo.utils.Colors
import zone.ien.shampoo.utils.MyUtils
import kotlin.collections.ArrayList

class SettingsActivity : AppCompatActivity() {

    lateinit var binding: ActivitySettingsBinding
    lateinit var sharedPreferences: SharedPreferences

    private val onBackPressedCallback = object: OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!binding.slidingPaneLayout.closePane()) {
                setResult(RESULT_OK)
                finish()
            } else {
                binding.title.text = ""
            }
        }
    }

    private val preferenceCallback = object: PreferenceCallback {
        override fun click(menuId: Int) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.container, when (menuId) { // singleton으로
                    R.id.menu_general -> SettingsPreferencesFragment.newInstance()
                    R.id.menu_place -> PlaceFragment.newInstance()
                    R.id.menu_notifications -> NotificationsFragment.newInstance()
                    R.id.menu_info -> InfoPreferencesFragment.newInstance()
                    else -> SettingsPreferencesFragment.newInstance()
                })
                if (binding.slidingPaneLayout.isOpen) {
                    setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                }
            }
            binding.slidingPaneLayout.open()

            binding.title.text = when (menuId) {
                R.id.menu_general -> getString(R.string.settings_general)
                R.id.menu_place -> getString(R.string.place_settings)
                R.id.menu_notifications -> getString(R.string.notifications_settings)
                R.id.menu_info -> getString(R.string.info_and_ask)
                else -> ""
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)
        onBackPressedDispatcher.addCallback(onBackPressedCallback)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(ContextCompat.getDrawable(this, R.drawable.ic_arrow_back)?.apply { DrawableCompat.setTint(this, getAttrColor(theme, Colors.colorOnSecondaryContainer)) })
        supportActionBar?.title = null

        sharedPreferences = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)

        addMenuProvider(object: MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                return when (item.itemId) {
                    android.R.id.home -> {
                        onBackPressedDispatcher.onBackPressed()
                        true
                    }
                    else -> false
                }
            }
        })

        val adapter = SettingsCategoryAdapter(arrayListOf(
            CategoryObject(R.id.menu_general, ContextCompat.getDrawable(this, R.drawable.ic_settings), getString(R.string.settings_general), getMenuContent(R.string.use_dynamic_colors)),
            CategoryObject(R.id.menu_place, ContextCompat.getDrawable(this, R.drawable.ic_location), getString(R.string.place_settings), getMenuContent(R.string.place_settings)),
            CategoryObject(R.id.menu_notifications, ContextCompat.getDrawable(this, R.drawable.ic_notifications), getString(R.string.notifications_settings), getMenuContent(R.string.notifications_settings)),
            CategoryObject(R.id.menu_info, ContextCompat.getDrawable(this, R.drawable.ic_info), getString(R.string.info_and_ask), getMenuContent(R.string.update_log, R.string.ask_to_dev)),
        )).apply {
            setCallbackListener(preferenceCallback)
        }

        if (savedInstanceState != null) {
            adapter.setSelectedId(savedInstanceState.getInt(SavedInstanceKey.PREF_PAGE_ID))
            binding.title.text = when (savedInstanceState.getInt(SavedInstanceKey.PREF_PAGE_ID)) {
                R.id.menu_general -> getString(R.string.settings_general)
                R.id.menu_place -> getString(R.string.place_settings)
                R.id.menu_notifications -> getString(R.string.notifications_settings)
                R.id.menu_info -> getString(R.string.info_and_ask)
                else -> ""
            }
        } else if (resources.getBoolean(R.bool.is_w600dp)) {
            adapter.select(R.id.menu_general)
        }

        binding.navigationView.adapter = adapter
        binding.slidingPaneLayout.lockMode = SlidingPaneLayout.LOCK_MODE_LOCKED
        binding.appInfo.setOnClickListener {
            MaterialAlertDialogBuilder(this, R.style.Theme_Shampoo_MaterialAlertDialog).apply {
                setIcon(R.drawable.ic_icon)
                setTitle(R.string.app_name)
                setMessage(R.string.dev_ienlab)

                setPositiveButton(android.R.string.ok) { dialog, _ -> }
            }.show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(SavedInstanceKey.PREF_PAGE_ID, (binding.navigationView.adapter as SettingsCategoryAdapter).getSelectedId())
        super.onSaveInstanceState(outState)
    }

    private fun getMenuContent(vararg ids: Int): String {
        val builder = StringBuilder()
        ids.forEachIndexed { index, id ->
            builder.append(getString(id))
            if (index != ids.lastIndex) {
                builder.append(" · ")
            }
        }
        return builder.toString()
    }

    class SettingsPreferencesFragment: PreferenceFragmentCompat() {

        lateinit var sharedPreferences: SharedPreferences
        lateinit var geocoder: Geocoder
        lateinit var am: AudioManager

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_general, rootKey)
            sharedPreferences = requireContext().getSharedPreferences("${requireContext().packageName}_preferences", Context.MODE_PRIVATE)
            geocoder = Geocoder(requireContext())
            am = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager

            val colorPrimary = getAttrColor(requireContext().theme, Colors.colorPrimary)
            val prefIsMaterialYou = findPreference<SwitchPreferenceCompat>(SharedKey.MATERIAL_YOU)
            prefIsMaterialYou?.icon?.setTint(colorPrimary)
        }

        companion object {
            @JvmStatic
            fun newInstance() = SettingsPreferencesFragment().apply {
                val args = Bundle()
                arguments = args
            }
        }
    }

    class AlarmPreferencesFragment: PreferenceFragmentCompat() {

        lateinit var sharedPreferences: SharedPreferences

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
//            setPreferencesFromResource(R.xml.preferences_alarm, rootKey)
//            sharedPreferences = requireContext().getSharedPreferences("${requireContext().packageName}_preferences", Context.MODE_PRIVATE)
//            storage = AppStorage(requireContext())
//
//            val colorPrimary = MyUtils.getAttrColor(requireContext().theme, Colors.colorPrimary)
//            val prefAlarmAutoDismiss = findPreference<Preference>("alarm_dismiss_duration")
//            val prefAlarmToCreate = findPreference<Preference>("alarm_to_create")
//            val prefDefaultAlarmSound = findPreference<Preference>("default_alarm_sound_pref")
//            val prefSwipeReverse = findPreference<Preference>(SharedKey.ALARM_SWIPE_REVERSE)
//
//            prefAlarmAutoDismiss?.icon?.setTint(colorPrimary)
//            prefAlarmToCreate?.icon?.setTint(colorPrimary)
//            prefDefaultAlarmSound?.icon?.setTint(colorPrimary)
//            prefSwipeReverse?.icon?.setTint(colorPrimary)
//
//            val subAlarmString: ArrayList<String> = arrayListOf()
//            val ringtonesAlarm: MutableMap<String, String> = mutableMapOf()
//            ringtonesAlarm[getString(R.string.default_alarm_ring)] = SharedDefault.DEFAULT_SOUND
//            ringtonesAlarm.putAll(alarmRingtones)
//
//            prefAlarmAutoDismiss?.summary = sharedPreferences.getInt(SharedKey.ALARM_DISMISS_TIME, SharedDefault.ALARM_DISMISS_TIME).let {
//                val timeArray: ArrayList<String> = arrayListOf()
//                if (it / 60 == 1) timeArray.add(getString(R.string.time_format_1hour))
//                else if (it / 60 != 0) timeArray.add(getString(R.string.time_format_hour, it / 60))
//                if (it % 60 == 1) timeArray.add(getString(R.string.time_format_1minute))
//                else if (it % 60 != 0) timeArray.add(getString(R.string.time_format_minute, it % 60))
//
//                timeArray.joinToString(" ")
//            }
//            prefAlarmAutoDismiss?.setOnPreferenceClickListener { preference ->
//                val value = sharedPreferences.getInt(SharedKey.ALARM_DISMISS_TIME, SharedDefault.ALARM_DISMISS_TIME)
//                val timePicker = MaterialTimePicker.Builder()
//                    .setTitleText(R.string.set_fixed_time)
//                    .setTimeFormat(TimeFormat.CLOCK_24H)
//                    .setPositiveButtonText(android.R.string.ok)
//                    .setNegativeButtonText(android.R.string.cancel)
//                    .setHour(value / 60)
//                    .setMinute(value % 60)
//                    .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
//                    .build()
//                timePicker.addOnPositiveButtonClickListener {
//                    sharedPreferences.edit().putInt(SharedKey.ALARM_DISMISS_TIME, timePicker.hour * 60 + timePicker.minute).apply()
//                    preference.summary = (timePicker.hour * 60 + timePicker.minute).let {
//                        val timeArray: ArrayList<String> = arrayListOf()
//                        if (it / 60 == 1) timeArray.add(getString(R.string.time_format_1hour))
//                        else if (it / 60 != 0) timeArray.add(getString(R.string.time_format_hour, it / 60))
//                        if (it % 60 == 1) timeArray.add(getString(R.string.time_format_1minute))
//                        else if (it % 60 != 0) timeArray.add(getString(R.string.time_format_minute, it % 60))
//
//                        timeArray.joinToString(" ")
//                    }
//                }
//                timePicker.show(parentFragmentManager, "ALARM_DISMISS_PICKER")
//
//                true
//            }
//
//            prefDefaultAlarmSound?.summary = with(ringtonesAlarm.filterValues { it == sharedPreferences.getString(SharedKey.DEFAULT_ALARM_SOUND, SharedDefault.DEFAULT_SOUND) }.getKeyArray()) { if (this.isNotEmpty()) first() else "" }
//
//            prefDefaultAlarmSound?.setOnPreferenceClickListener { preference ->
//                val mediaPlayer = MediaPlayer().apply { setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()) }
//                MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Calarm_MaterialAlertDialog).apply {
//                    var checkedUri = sharedPreferences.getString(SharedKey.DEFAULT_ALARM_SOUND, SharedDefault.DEFAULT_SOUND)
//                    var index = ringtonesAlarm.values.indexOf(checkedUri)
//
//                    setIcon(R.drawable.ic_ring)
//                    setTitle(R.string.default_alarm_sound)
//                    setSingleChoiceItems(ringtonesAlarm.getKeyArray(), index) { _, which ->
//                        mediaPlayer.stop()
//                        try {
//                            mediaPlayer.reset()
//                            ringtonesAlarm[ringtonesAlarm.getKeyArray()[which]].let {
//                                if (it != "" && it != SharedDefault.DEFAULT_SOUND) {
//                                    mediaPlayer.setDataSource(requireContext(), Uri.parse(it))
//                                } else {
//                                    val fileDescriptor = resources.openRawResourceFd(R.raw.alarm_default)
//                                    mediaPlayer.setDataSource(fileDescriptor.fileDescriptor, fileDescriptor.startOffset, fileDescriptor.length)
//                                    fileDescriptor.close()
//                                }
//                            }
//                            mediaPlayer.prepare()
//                            mediaPlayer.start()
//                        } catch (e: Exception) { e.printStackTrace() }
//
//                        index = which
//                        checkedUri = ringtonesAlarm[ringtonesAlarm.getKeyArray()[which]] ?: ""
//                    }
//                    setOnDismissListener {
//                        mediaPlayer.stop()
//                    }
//                    setPositiveButton(android.R.string.ok) { dialog, _ ->
//                        preference.summary = with(ringtonesAlarm.filterValues { it == checkedUri }.getKeyArray()) { if (this.isNotEmpty()) first() else "" }
//                        sharedPreferences.edit().putString(SharedKey.DEFAULT_ALARM_SOUND, checkedUri).apply()
//                        mediaPlayer.stop()
//                    }
//                    setNegativeButton(android.R.string.cancel) { dialog, _ ->
//                        mediaPlayer.stop()
//                    }
//                }.show()
//                true
//            }
//
//            val info = (sharedPreferences.getString(SharedKey.ALARM_CREATE_INFO, SharedDefault.ALARM_CREATE_INFO) ?: SharedDefault.ALARM_CREATE_INFO).split(",")
//            if (info.isNotEmpty()) info.forEach { try {
//                subAlarmString.add(getString(R.string.time_format_before, it.toInt().let {
//                    val timeArray: ArrayList<String> = arrayListOf()
//                    if (it / 60 == 1) timeArray.add(getString(R.string.time_format_1hour_short))
//                    else if (it / 60 != 0) timeArray.add(getString(R.string.time_format_hour_short, it / 60))
//                    if (it % 60 == 1) timeArray.add(getString(R.string.time_format_1minute_short))
//                    else if (it % 60 != 0) timeArray.add(getString(R.string.time_format_minute_short, it % 60))
//
//                    timeArray.joinToString(" ")
//                }))
//            } catch (_: NumberFormatException) {}}
//            prefAlarmToCreate?.summary = subAlarmString.joinToString(", ")
//            prefAlarmToCreate?.setOnPreferenceClickListener { preference ->
//                val adapter: AlarmCreateAdapter
//                val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Calarm_MaterialAlertDialog).apply {
//                    setTitle(R.string.sub_alarm_to_create)
//                    setIcon(R.drawable.ic_add_alarm)
//
//                    val raw = (sharedPreferences.getString(SharedKey.ALARM_CREATE_INFO, SharedDefault.ALARM_CREATE_INFO) ?: SharedDefault.ALARM_CREATE_INFO).split(",")
//                    val data: ArrayList<Int> = arrayListOf()
//
//                    for (d in raw) {
//                        try { data.add(d.toInt()) } catch (_: NumberFormatException) {}
//                    }
//
//                    data.sort()
//
//                    val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_alarm_create, LinearLayout(requireContext()), false)
//                    val list: RecyclerView = view.findViewById(R.id.list)
//                    adapter = AlarmCreateAdapter(data)
//
//                    list.adapter = adapter
//                    setPositiveButton(R.string.close) { dialog, id ->
//                        dialog.dismiss()
//                    }
//                    setNeutralButton(R.string.add_new_alarm) { dialog, id -> }
//                    setOnDismissListener {
//                        sharedPreferences.edit().putString(SharedKey.ALARM_CREATE_INFO, data.joinToString(",")).apply()
//                        val summary: ArrayList<String> = arrayListOf()
//                        for (t in data) {
//                            summary.add(getString(R.string.time_format_before, t.let {
//                                val timeArray: ArrayList<String> = arrayListOf()
//                                if (it / 60 == 1) timeArray.add(getString(R.string.time_format_1hour_short))
//                                else if (it / 60 != 0) timeArray.add(getString(R.string.time_format_hour_short, it / 60))
//                                if (it % 60 == 1) timeArray.add(getString(R.string.time_format_1minute_short))
//                                else if (it % 60 != 0) timeArray.add(getString(R.string.time_format_minute_short, it % 60))
//
//                                timeArray.joinToString(" ")
//                            }))
//                        }
//                        preference.summary = summary.joinToString(", ")
//                    }
//                    setView(view)
//                }.create()
//                dialog.show()
//                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
//                    if (storage.purchasedPro() || adapter.itemCount < FreeLimit.SUB_ALARM_LIMIT) {
//                        val timePicker = MaterialTimePicker.Builder()
//                            .setHour(0)
//                            .setMinute(10)
//                            .setTitleText(R.string.sub_alarm_dialog_title)
//                            .setTimeFormat(TimeFormat.CLOCK_24H)
//                            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
//                            .build()
//                        timePicker.addOnPositiveButtonClickListener {
//                            if (adapter.items.find { it == timePicker.hour * 60 + timePicker.minute } == null) {
//                                adapter.add(timePicker.hour * 60 + timePicker.minute)
//                            } else {
//                                dialog.window?.decorView?.animate()?.translationX(16f)?.interpolator = CycleInterpolator(7f)
//                                Toast.makeText(requireContext(), getString(R.string.sub_alarm_exists), Toast.LENGTH_SHORT).show()
//                            }
//                        }
//                        timePicker.show(parentFragmentManager, "SUB_TIME_PICKER")
//                    } else {
//                        Toast.makeText(requireContext(), R.string.sub_alarm_exists, Toast.LENGTH_SHORT).apply {
//                            setGravity(Gravity.CENTER, 0, 0)
//                        }.show()
//                    }
//                }
//                true
//            }
        }

        companion object {
            @JvmStatic
            fun newInstance() = AlarmPreferencesFragment().apply {
                val args = Bundle()
                arguments = args
            }
        }
    }

    class PlaceFragment: Fragment() {

        lateinit var binding: FragmentPlaceBinding
        private var placeDatabase: PlaceDatabase? = null
        private var deviceDatabase: DeviceDatabase? = null

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_place, container, false)
            return binding.root
        }

        @OptIn(DelicateCoroutinesApi::class)
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            placeDatabase = PlaceDatabase.getInstance(requireContext())
            deviceDatabase = DeviceDatabase.getInstance(requireContext())

            val placeCallback = object: PlaceCallback {
                override fun edit(position: Int, entity: PlaceEntity) {
                    val dialogBinding: DialogInputBinding = DataBindingUtil.inflate(LayoutInflater.from(requireContext()), R.layout.dialog_input, null, false)
                    val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Shampoo_MaterialAlertDialog).apply {
                        dialogBinding.inputLayout.hint = context.getString(R.string.new_place)
                        dialogBinding.inputLayout.editText?.setText(entity.title)
                        dialogBinding.tvEmojiPreview.text = entity.icon

                        setPositiveButton(android.R.string.ok) { dialog, id -> }
                        setNegativeButton(android.R.string.cancel) { dialog, id -> }

                        setView(dialogBinding.root)
                    }.create()

                    dialogBinding.tvEmojiPreview.setOnClickListener {
                        var dialog2: AlertDialog? = null
                        val callbackListener = object: EmojiClickCallback {
                            override fun click(emoji: String) {
                                dialogBinding.tvEmojiPreview.text = emoji
                                dialog2?.dismiss()
                            }
                        }
                        dialog2 = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Shampoo_MaterialAlertDialog).apply {
                            val emojiBinding: DialogEmojiMoreBinding = DataBindingUtil.inflate(LayoutInflater.from(requireContext()), R.layout.dialog_emoji_more, LinearLayout(requireContext()), false)
                            for (i in 0 until emojiBinding.tabLayout.tabCount) emojiBinding.tabLayout.getTabAt(i)?.text = MyUtils.emoji_labels[i]

                            val gridLayoutManager = GridLayoutManager(requireContext(), MyUtils.calculateNoOfColumns(requireContext(), 48f))
                            emojiBinding.tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
                                override fun onTabSelected(tab: TabLayout.Tab) {
                                    emojiBinding.list.adapter = EmojiMoreAdapter(MyUtils.emojis[tab.position]).apply {
                                        setCallbackListener(callbackListener)
                                    }
                                }
                                override fun onTabReselected(tab: TabLayout.Tab) {}
                                override fun onTabUnselected(tab: TabLayout.Tab) {}
                            })
                            emojiBinding.list.layoutManager = gridLayoutManager
                            emojiBinding.list.adapter = EmojiMoreAdapter(MyUtils.emojis[0]).apply {
                                setCallbackListener(callbackListener)
                            }
                            emojiBinding.tabLayout.selectTab(emojiBinding.tabLayout.getTabAt(0))

                            setNegativeButton(android.R.string.cancel) { _, _ -> }

                            setView(emojiBinding.root)
                        }.create()
                        dialog2?.show()
                    }

                    dialog.show()
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val title = dialogBinding.inputLayout.editText?.text?.toString() ?: ""
                        val icon = dialogBinding.tvEmojiPreview.text.toString()
                        if (title != "") {
                            entity.title = title
                            entity.icon = icon
                            (binding.list.adapter as PlaceAdapter).edit(entity)
                            GlobalScope.launch(Dispatchers.IO) {
                                placeDatabase?.getDao()?.update(entity)
                                dialog.dismiss()
                            }
                        } else {
                            dialog.window?.decorView?.animate()?.translationX(16f)?.interpolator = CycleInterpolator(7f)
                            dialogBinding.inputLayout.error = getString(R.string.fill_out_the_title)
                        }
                    }
                }

                override fun delete(position: Int, id: Long) {
                    GlobalScope.launch(Dispatchers.IO) {
                        val entities = deviceDatabase?.getDao()?.getByPlace(id)
                        withContext(Dispatchers.Main) {
                            if (entities?.isEmpty() == true) {
                                MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Shampoo_MaterialAlertDialog).apply {
                                    setIcon(R.drawable.ic_delete)
                                    setTitle(R.string.delete_title)
                                    setMessage(R.string.delete_content)
                                    setPositiveButton(android.R.string.ok) { dialog, _ ->
                                        GlobalScope.launch(Dispatchers.IO) {
                                            placeDatabase?.getDao()?.delete(id)
                                        }
                                        ((binding.root as RecyclerView).adapter as PlaceAdapter).delete(id)
                                    }
                                    setNegativeButton(android.R.string.cancel) { dialog, _ -> }
                                }.show()
                            } else {
                                Snackbar.make(view, getString(R.string.cannot_delete_place), Snackbar.LENGTH_SHORT).show()
                            }
                        }
                    }


                }
            }

            GlobalScope.launch(Dispatchers.IO) {
                val data = placeDatabase?.getDao()?.getAll() as ArrayList

                withContext(Dispatchers.Main) {
                    binding.list.adapter = PlaceAdapter(data).apply {
                        setClickCallback(placeCallback)
                    }
                }

                binding.btnAdd.setOnClickListener {
                    val dialogBinding: DialogInputBinding = DataBindingUtil.inflate(LayoutInflater.from(requireContext()), R.layout.dialog_input, null, false)
                    val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Shampoo_MaterialAlertDialog).apply {
                        dialogBinding.inputLayout.hint = context.getString(R.string.new_place)
                        dialogBinding.tvEmojiPreview.text = "🛁"

                        setPositiveButton(android.R.string.ok) { dialog, id -> }
                        setNegativeButton(android.R.string.cancel) { dialog, id -> }

                        setView(dialogBinding.root)
                    }.create()

                    dialogBinding.tvEmojiPreview.setOnClickListener {
                        var dialog2: AlertDialog? = null
                        val callbackListener = object: EmojiClickCallback {
                            override fun click(emoji: String) {
                                dialogBinding.tvEmojiPreview.text = emoji
                                dialog2?.dismiss()
                            }
                        }
                        dialog2 = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Shampoo_MaterialAlertDialog).apply {
                            val emojiBinding: DialogEmojiMoreBinding = DataBindingUtil.inflate(LayoutInflater.from(requireContext()), R.layout.dialog_emoji_more, LinearLayout(requireContext()), false)
                            for (i in 0 until emojiBinding.tabLayout.tabCount) emojiBinding.tabLayout.getTabAt(i)?.text = MyUtils.emoji_labels[i]

                            val gridLayoutManager = GridLayoutManager(requireContext(), MyUtils.calculateNoOfColumns(requireContext(), 48f))
                            emojiBinding.tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
                                override fun onTabSelected(tab: TabLayout.Tab) {
                                    emojiBinding.list.adapter = EmojiMoreAdapter(MyUtils.emojis[tab.position]).apply {
                                        setCallbackListener(callbackListener)
                                    }
                                }
                                override fun onTabReselected(tab: TabLayout.Tab) {}
                                override fun onTabUnselected(tab: TabLayout.Tab) {}
                            })
                            emojiBinding.list.layoutManager = gridLayoutManager
                            emojiBinding.list.adapter = EmojiMoreAdapter(MyUtils.emojis[0]).apply {
                                setCallbackListener(callbackListener)
                            }
                            emojiBinding.tabLayout.selectTab(emojiBinding.tabLayout.getTabAt(0))

                            setNegativeButton(android.R.string.cancel) { _, _ -> }

                            setView(emojiBinding.root)
                        }.create()
                        dialog2?.show()
                    }

                    dialog.show()
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val title = dialogBinding.inputLayout.editText?.text?.toString() ?: ""
                        val icon = dialogBinding.tvEmojiPreview.text.toString()
                        if (title != "") {
                            val entity = PlaceEntity(icon, title)
                            GlobalScope.launch(Dispatchers.IO) {
                                val id = placeDatabase?.getDao()?.add(entity)
                                entity.id = id
                                withContext(Dispatchers.Main) {
                                    ((binding.root as RecyclerView).adapter as PlaceAdapter).add(entity)
                                    dialog.dismiss()
                                }
                            }
                        } else {
                            dialog.window?.decorView?.animate()?.translationX(16f)?.interpolator = CycleInterpolator(7f)
                            dialogBinding.inputLayout.error = getString(R.string.fill_out_the_title)
                        }
                    }
                }
            }
        }

        companion object {
            @JvmStatic
            fun newInstance() = PlaceFragment().apply {
                val args = Bundle()
                arguments = args
            }
        }
    }

    class NotificationsFragment: Fragment() {

        lateinit var binding: FragmentNotificationsBinding
        private var placeDatabase: PlaceDatabase? = null
        private var deviceDatabase: DeviceDatabase? = null

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_notifications, container, false)
            return binding.root
        }

        @OptIn(DelicateCoroutinesApi::class)
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            placeDatabase = PlaceDatabase.getInstance(requireContext())
            deviceDatabase = DeviceDatabase.getInstance(requireContext())

            requireActivity().addMenuProvider(object: MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
//                    menuInflater.inflate(R.menu.menu_add, menu)
                    for (menuItem in menu.iterator()) {
                        menuItem.iconTintList = ColorStateList.valueOf(getAttrColor(requireContext().theme, Colors.colorOnSecondaryContainer))
                    }
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return false
                }
            }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        }

        companion object {
            @JvmStatic
            fun newInstance() = NotificationsFragment().apply {
                val args = Bundle()
                arguments = args
            }
        }
    }

    class InfoPreferencesFragment: PreferenceFragmentCompat() {

        private lateinit var sharedPreferences: SharedPreferences

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_info, rootKey)

            val colorPrimary = getAttrColor(requireContext().theme, Colors.colorPrimary)
            val prefUpdatelog = findPreference<Preference>("changelog")
            val prefEmail = findPreference<Preference>("ask_to_dev")
            val prefOpensource = findPreference<Preference>("open_source")
            val prefAppVersion = findPreference<Preference>("app_version")

            sharedPreferences = requireContext().getSharedPreferences("${requireContext().packageName}_preferences", Context.MODE_PRIVATE)

            prefUpdatelog?.icon?.setTint(colorPrimary)
            prefEmail?.icon?.setTint(colorPrimary)
            prefOpensource?.icon?.setTint(colorPrimary)
            prefAppVersion?.icon?.setTint(colorPrimary)
            prefAppVersion?.summary = "${getString(R.string.versionName)} (${BuildConfig.VERSION_CODE})"

            prefUpdatelog?.setOnPreferenceClickListener {
                MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Shampoo_MaterialAlertDialog).apply {
                    setIcon(R.drawable.ic_icon)
                    setTitle("${getString(R.string.app_name)} ${BuildConfig.VERSION_NAME} ${getString(R.string.update_log)}")
                    setMessage(MyUtils.fromHtml(MyUtils.readTextFromRaw(resources, R.raw.changelog)))
                    setPositiveButton(R.string.close) { dialog, id ->
                        dialog.dismiss()
                    }
                }.show()
                true
            }
            prefEmail?.setOnPreferenceClickListener {
                Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("my@ien.zone"))
                    putExtra(Intent.EXTRA_SUBJECT, "${getString(R.string.app_name)} ${BuildConfig.VERSION_NAME} ${getString(R.string.ask)}")
                    putExtra(Intent.EXTRA_TEXT, "${getString(R.string.email_text)}\n${Build.BRAND} ${Build.MODEL} Android ${Build.VERSION.RELEASE}\n_\n")
                    type = "message/rfc822"
                    startActivity(this)
                }
                true
            }
            prefOpensource?.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), com.google.android.gms.oss.licenses.OssLicensesMenuActivity::class.java))
                true
            }
        }

        companion object {
            @JvmStatic
            fun newInstance() = InfoPreferencesFragment().apply {
                val args = Bundle()
                arguments = args
            }
        }
    }
}