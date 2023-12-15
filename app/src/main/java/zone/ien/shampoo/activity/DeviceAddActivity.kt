package zone.ien.shampoo.activity

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zone.ien.shampoo.R
import zone.ien.shampoo.adapter.DeviceAddPageAdapter
import zone.ien.shampoo.callback.DeviceAddActivityCallback
import zone.ien.shampoo.constant.ActionID
import zone.ien.shampoo.constant.IntentKey
import zone.ien.shampoo.constant.IntentValue
import zone.ien.shampoo.databinding.ActivityDeviceAddBinding
import zone.ien.shampoo.fragment.DeviceAddGuideFragment
import zone.ien.shampoo.fragment.DeviceAddInfoFragment
import zone.ien.shampoo.fragment.DeviceAddScanFragment
import zone.ien.shampoo.fragment.DeviceAddSettingFragment
import zone.ien.shampoo.room.DeviceDatabase
import zone.ien.shampoo.room.DeviceEntity
import zone.ien.shampoo.room.DeviceLogDatabase
import zone.ien.shampoo.room.DeviceLogEntity
import zone.ien.shampoo.utils.ColorUtils.getAttrColor
import zone.ien.shampoo.utils.Colors
import zone.ien.shampoo.utils.Dlog
import zone.ien.shampoo.utils.MyUtils

class DeviceAddActivity : AppCompatActivity(),
    DeviceAddScanFragment.OnFragmentInteractionListener,
    DeviceAddInfoFragment.OnFragmentInteractionListener,
    DeviceAddGuideFragment.OnFragmentInteractionListener,
    DeviceAddSettingFragment.OnFragmentInteractionListener {

    private lateinit var binding: ActivityDeviceAddBinding
    private lateinit var pages: List<Fragment>
    private var pagePosition = DEVICE_ADD_PAGE_SCAN

    private var deviceDatabase: DeviceDatabase? = null
    private var deviceLogDatabase: DeviceLogDatabase? = null

    private val deviceAddActivityCallback = object: DeviceAddActivityCallback {
        override fun scrollTo(page: Int) {
            pagePosition = page
            binding.viewpager.setCurrentItem(page, true)
        }

        override fun setButtonEnabled(isPrev: Boolean, isEnabled: Boolean) {
            if (isPrev) {
                binding.btnPrev.isEnabled = isEnabled
                binding.btnPrev.alpha = if (isEnabled) 1f else 0.3f
            }
            else {
                binding.btnNext.isEnabled = isEnabled
                binding.btnNext.alpha = if (isEnabled) 1f else 0.3f
                if (isEnabled) setFinishButtonShow(false)
            }
        }

        override fun setFinishButtonShow(isEnabled: Boolean) {
            binding.btnFinish.visibility = if (isEnabled) View.VISIBLE else View.INVISIBLE
            binding.btnNext.visibility = if (isEnabled) View.INVISIBLE else View.VISIBLE
        }

        override fun setFinishButtonEnabled(isEnabled: Boolean) {
            binding.btnFinish.isEnabled = isEnabled
            binding.btnFinish.alpha = if (isEnabled) 1f else 0.3f
        }

        override fun setTitle(@StringRes title: Int) {
            binding.tvTitle.setText(title)
        }

    }
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_device_add)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(ContextCompat.getDrawable(this, R.drawable.ic_arrow_back)?.apply { DrawableCompat.setTint(this, getAttrColor(theme, Colors.colorOnSecondaryContainer)) })

        deviceDatabase = DeviceDatabase.getInstance(this)
        deviceLogDatabase = DeviceLogDatabase.getInstance(this)

        onBackPressedDispatcher.addCallback(object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                MaterialAlertDialogBuilder(this@DeviceAddActivity, R.style.Theme_Shampoo_MaterialAlertDialog).apply {
                    setIcon(R.drawable.ic_sync_disabled)
                    setTitle(R.string.cancel_title)
                    setMessage(R.string.cancel_content)
                    setPositiveButton(android.R.string.ok) { dialog, _ ->
                        sendBroadcast(Intent(ActionID.ACTION_DISCONNECT_DEVICE).apply {
                            putExtra(IntentKey.DEVICE_ADDRESS, deviceAddress)
                        })
                        finish()
                    }
                    setNegativeButton(android.R.string.cancel) { dialog, _ ->

                    }
                }.show()
            }
        })

        pages = listOf(
            DeviceAddScanFragment.newInstance().apply { setCallbackListener(deviceAddActivityCallback) },
            DeviceAddInfoFragment.newInstance().apply { setCallbackListener(deviceAddActivityCallback) },
            DeviceAddGuideFragment.newInstance().apply { setCallbackListener(deviceAddActivityCallback) },
            DeviceAddSettingFragment.newInstance().apply { setCallbackListener(deviceAddActivityCallback) },
        )
        binding.viewpager.offscreenPageLimit = pages.size
        binding.viewpager.adapter = DeviceAddPageAdapter(this, pages)
        binding.viewpager.setCurrentItem(pagePosition, false)
        binding.viewpager.isUserInputEnabled = false

        binding.viewpager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            var currentState = 0
            var currentPos = 0

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
//                binding.viewpager.currentItem
//                if (currentState == ViewPager2.SCROLL_STATE_DRAGGING && currentPos == position) {
//                    if (currentPos == 0) binding.viewpager.currentItem = 2
//                    else if (currentPos == 2) binding.viewpager.currentItem = 0
//                }
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            }

            override fun onPageSelected(position: Int) {
                currentPos = position
                super.onPageSelected(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                currentState = state
                super.onPageScrollStateChanged(state)
            }
        })

        binding.btnPrev.setOnClickListener {
            if (pagePosition - 1 >= 0) binding.viewpager.setCurrentItem(--pagePosition, true)
        }
        binding.btnNext.setOnClickListener {
            Dlog.d(TAG, "$pagePosition, ${pages.size}")
            if (pagePosition + 1 < pages.size) binding.viewpager.setCurrentItem(++pagePosition, true)
        }
        binding.btnFinish.setOnClickListener {
            val entity = DeviceEntity(deviceName, deviceAddress, deviceProduct, deviceMax, deviceModel, deviceType, deviceRoom)
            val log = DeviceLogEntity(-1, System.currentTimeMillis(), -1, deviceCapacity)
            GlobalScope.launch(Dispatchers.IO) {
                val id = deviceDatabase?.getDao()?.add(entity) ?: -1
                if (id != -1L) {
                    log.parentId = id
                    deviceLogDatabase?.getDao()?.add(log)
                }

                withContext(Dispatchers.Main) {
                    setResult(RESULT_OK, Intent().apply {
                        putExtra(IntentKey.DATA_ID, id)
                    })
                    finish()
                }
            }
        }

    }

    override fun onFragmentInteraction(uri: Uri) {}


    companion object {
        const val DEVICE_ADD_PAGE_SCAN = 0
        const val DEVICE_ADD_PAGE_INFO = 1
        const val DEVICE_ADD_PAGE_GUIDE = 2
        const val DEVICE_ADD_PAGE_SETTING = 3

        var deviceName = ""
        var deviceAddress = ""
        var deviceProduct = ""
        var deviceCapacity = 0
        var deviceMax = 0
        var deviceType = DeviceEntity.TYPE_UNKNOWN
        var deviceModel = -1
        var deviceRoom = -1L
        var deviceBarcode = ""
    }
}