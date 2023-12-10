package zone.ien.shampoo.callback

import androidx.annotation.StringRes

interface DeviceAddActivityCallback {
    fun scrollTo(page: Int)
    fun setButtonEnabled(isPrev: Boolean, isEnabled: Boolean)
    fun setFinishButtonShow(isEnabled: Boolean)
    fun setFinishButtonEnabled(isEnabled: Boolean)
    fun setTitle(@StringRes title: Int)
}