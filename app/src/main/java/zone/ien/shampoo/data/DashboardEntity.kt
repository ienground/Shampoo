package zone.ien.shampoo.data

data class DashboardEntity(
    var title: String,
    var capacity: Int,
    var type: Int,
    var room: Int,
) {
    companion object {
        const val TYPE_UNKNOWN = -1
        const val TYPE_SHAMPOO = 0
        const val TYPE_CONDITIONER = 1
        const val TYPE_BODYWASH = 2
        const val TYPE_CLEANSING = 3
    }
}
