package zone.ien.shampoo.callback

interface DeviceAddActivityCallback {
    fun scrollTo(page: Int)
    fun setButtonEnabled(isPrev: Boolean, isEnabled: Boolean)
    fun setFinishButtonShow(isEnabled: Boolean)
    fun setFinishButtonEnabled(isEnabled: Boolean)
}