package zone.ien.shampoo.activity

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zone.ien.shampoo.R
import zone.ien.shampoo.adapter.DeviceAddPageAdapter
import zone.ien.shampoo.callback.DeviceAddActivityCallback
import zone.ien.shampoo.databinding.ActivityDeviceAddBinding
import zone.ien.shampoo.fragment.DeviceAddGuideFragment
import zone.ien.shampoo.fragment.DeviceAddInfoFragment
import zone.ien.shampoo.fragment.DeviceAddScanFragment
import zone.ien.shampoo.fragment.DeviceAddSettingFragment
import zone.ien.shampoo.room.DeviceDatabase
import zone.ien.shampoo.room.DeviceEntity
import zone.ien.shampoo.utils.Dlog

class DeviceAddActivity : AppCompatActivity(),
    DeviceAddScanFragment.OnFragmentInteractionListener,
    DeviceAddInfoFragment.OnFragmentInteractionListener,
    DeviceAddGuideFragment.OnFragmentInteractionListener,
    DeviceAddSettingFragment.OnFragmentInteractionListener {

    private lateinit var binding: ActivityDeviceAddBinding
    private lateinit var pages: List<Fragment>
    private var pagePosition = DEVICE_ADD_PAGE_SCAN

    private var deviceDatabase: DeviceDatabase? = null

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
    }
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_device_add)

        deviceDatabase = DeviceDatabase.getInstance(this)

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
            Dlog.d(TAG, "deviceName $deviceName")
            Dlog.d(TAG, "deviceAddress $deviceAddress")
            Dlog.d(TAG, "deviceProduct $deviceProduct")
            Dlog.d(TAG, "deviceCapacity $deviceCapacity")
            Dlog.d(TAG, "deviceMax $deviceMax")
            Dlog.d(TAG, "deviceType $deviceType")
            Dlog.d(TAG, "deviceRoom $deviceRoom")
            Dlog.d(TAG, "deviceBarcode $deviceBarcode")

            val entity = DeviceEntity(deviceName, deviceAddress, deviceProduct, deviceCapacity, deviceMax, deviceType, deviceRoom)
            GlobalScope.launch(Dispatchers.IO) {
                deviceDatabase?.getDao()?.add(entity)

                withContext(Dispatchers.Main) {
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
        var deviceCapacity = 0.0
        var deviceMax = 0
        var deviceType = DeviceEntity.TYPE_UNKNOWN
        var deviceRoom = -1
        var deviceBarcode = ""
    }
}