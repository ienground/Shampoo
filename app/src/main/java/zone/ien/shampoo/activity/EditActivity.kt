package zone.ien.shampoo.activity

import android.app.AlarmManager
import android.content.Intent
import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings.Global
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.animation.CycleInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.MenuProvider
import androidx.core.view.iterator
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zone.ien.shampoo.R
import zone.ien.shampoo.adapter.EmojiMoreAdapter
import zone.ien.shampoo.callback.EmojiClickCallback
import zone.ien.shampoo.constant.ActionID
import zone.ien.shampoo.constant.IntentKey
import zone.ien.shampoo.constant.IntentValue
import zone.ien.shampoo.databinding.ActivityDevEditBinding
import zone.ien.shampoo.databinding.ActivityEditBinding
import zone.ien.shampoo.databinding.DialogEmojiMoreBinding
import zone.ien.shampoo.databinding.DialogInputBinding
import zone.ien.shampoo.room.DeviceDatabase
import zone.ien.shampoo.room.DeviceEntity
import zone.ien.shampoo.room.NotificationsEntity
import zone.ien.shampoo.room.PlaceDatabase
import zone.ien.shampoo.room.PlaceEntity
import zone.ien.shampoo.utils.ColorUtils
import zone.ien.shampoo.utils.Colors
import zone.ien.shampoo.utils.Dlog
import zone.ien.shampoo.utils.MyUtils
import zone.ien.shampoo.utils.MyUtils.timeZero

class EditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditBinding
    private var deviceDatabase: DeviceDatabase? = null
    private var placeDatabase: PlaceDatabase? = null
    private var device: DeviceEntity? = null
    private var selectionDeviceId = -1L
    private var id = -1L

    private lateinit var barcodeLauncher: ActivityResultLauncher<ScanOptions>

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(ContextCompat.getDrawable(this, R.drawable.ic_arrow_back)?.apply { DrawableCompat.setTint(this, ColorUtils.getAttrColor(theme, Colors.colorOnSecondaryContainer)) })

        addMenuProvider(object: MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_edit, menu)
                for (menuItem in menu.iterator()) {
                    menuItem.iconTintList = ColorStateList.valueOf(ColorUtils.getAttrColor(theme, Colors.colorOnSecondaryContainer))
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    android.R.id.home -> {
                        onBackPressedDispatcher.onBackPressed()
                        true
                    }
                    R.id.menu_save -> {
                        GlobalScope.launch(Dispatchers.IO) {
                            device?.title = binding.inputName.editText?.text?.toString() ?: ""
                            device?.let { deviceDatabase?.getDao()?.update(it) }

                            withContext(Dispatchers.Main) {
                                setResult(RESULT_OK, Intent().apply { putExtra(IntentKey.DATA_ID, id) })
                                finish()
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
        })
        deviceDatabase = DeviceDatabase.getInstance(this)
        placeDatabase = PlaceDatabase.getInstance(this)

        GlobalScope.launch(Dispatchers.IO) {
            val places = placeDatabase?.getDao()?.getAll() as ArrayList<PlaceEntity>
            places.add(PlaceEntity("➕", getString(R.string.add_new_place)))

            withContext(Dispatchers.Main) {
                val items = places.map { "${it.icon}  ${it.title}" } as ArrayList
                val arrayAdapter: ArrayAdapter<String> = ArrayAdapter<String>(applicationContext, android.R.layout.simple_spinner_dropdown_item, items)

                binding.inputRoomAuto.setAdapter(arrayAdapter)
                binding.inputRoomAuto.setOnItemClickListener { parent, v, position, id ->
                    if (position == items.lastIndex) {
                        val dialogBinding: DialogInputBinding = DataBindingUtil.inflate(LayoutInflater.from(this@EditActivity), R.layout.dialog_input, null, false)
                        val dialog = MaterialAlertDialogBuilder(this@EditActivity, R.style.Theme_Shampoo_MaterialAlertDialog).apply {
                            dialogBinding.inputLayout.hint = context.getString(R.string.new_place)
                            dialogBinding.tvEmojiPreview.text = "🛁"

                            setPositiveButton(android.R.string.ok) { dialog, id -> }
                            setNegativeButton(android.R.string.cancel) { dialog, id ->
                                binding.inputRoomAuto.setText("", false)
                            }

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
                            dialog2 = MaterialAlertDialogBuilder(this@EditActivity, R.style.Theme_Shampoo_MaterialAlertDialog).apply {
                                val emojiBinding: DialogEmojiMoreBinding = DataBindingUtil.inflate(LayoutInflater.from(applicationContext), R.layout.dialog_emoji_more, LinearLayout(applicationContext), false)
                                for (i in 0 until emojiBinding.tabLayout.tabCount) emojiBinding.tabLayout.getTabAt(i)?.text = MyUtils.emoji_labels[i]

                                val gridLayoutManager = GridLayoutManager(applicationContext, MyUtils.calculateNoOfColumns(applicationContext, 48f))
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
                            dialog2.show()
                        }

                        dialog.show()
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val title = dialogBinding.inputLayout.editText?.text?.toString() ?: ""
                            val icon = dialogBinding.tvEmojiPreview.text.toString()
                            if (title != "") {
                                GlobalScope.launch(Dispatchers.IO) {
                                    val id = placeDatabase?.getDao()?.add(PlaceEntity(icon, title))
                                    withContext(Dispatchers.Main) {
                                        val entity = PlaceEntity(icon, title).apply { this.id = id }
                                        places.add(entity)
                                        arrayAdapter.insert("${icon}  ${title}", items.lastIndex)
                                        arrayAdapter.notifyDataSetChanged()
                                        binding.inputRoomAuto.setText("${icon}  ${title}", false)

                                        selectionDeviceId = entity.id ?: -1
                                        dialog.dismiss()
                                    }
                                }
                            } else {
                                dialog.window?.decorView?.animate()?.translationX(16f)?.interpolator = CycleInterpolator(7f)
                                dialogBinding.inputLayout.error = getString(R.string.fill_out_the_title)
                            }
                        }


                    } else {
                        selectionDeviceId = places[position].id ?: -1
                    }
                }
            }
        }

        binding.tvModel.isSelected = true
        binding.btnScan.setOnClickListener {
//            barcodeLauncher.launch()
        }

        // barcode
        barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
            if (result.contents == null) {
                Dlog.d(TAG, "result null")
            } else {
                Dlog.d(TAG, "Scanned ${result.contents}")
//                DeviceAddActivity.deviceBarcode = result.contents
            }
        }

        id = intent.getLongExtra(IntentKey.DATA_ID, -1L)
        if (id != -1L) {
            GlobalScope.launch(Dispatchers.IO) {
                val entity = deviceDatabase?.getDao()?.get(id)
                val room = placeDatabase?.getDao()?.get(entity?.room ?: -1)
                entity?.let {
                    device = it

                    withContext(Dispatchers.Main) {
                        binding.inputName.editText?.setText(device?.title)
                        binding.inputRoomAuto.setText("${room?.icon}  ${room?.title}")
                        binding.tvModel.text = device?.product
                    }
                }
            }
        }
    }
}