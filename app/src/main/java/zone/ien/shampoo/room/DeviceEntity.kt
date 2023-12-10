package zone.ien.shampoo.room

import android.content.Context
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import zone.ien.shampoo.R
import java.util.Date

@Entity (tableName = "DeviceDatabase")
data class DeviceEntity(
    var title: String,
    var address: String,
    var product: String,
    var max: Int,
    var model: Int,
    var type: Int,
    var room: Long,
) {
    @PrimaryKey(autoGenerate = true) var id: Long? = null
    @Ignore var battery: Int = 0
    @Ignore var lastConnectionTime: Long = 0L
    @Ignore var logs: ArrayList<DeviceLogEntity> = arrayListOf()
    @Ignore var capacity: Int = 0

    override fun toString(): String {
        val builder = StringBuilder("[$id] $title(${address}) - $product($model), $capacity/$max, ${getTypeString(type)} in ${room} / battery ${battery}% / last: ${Date(lastConnectionTime)}[")
        builder.append(logs.joinToString(",") { it.toString() })
        builder.append("]")

        return builder.toString()
    }

    private fun getTypeString(type: Int): String {
        return when (type) {
            TYPE_SHAMPOO -> "SHAMPOO"
            TYPE_CONDITIONER -> "CONDITIONER"
            TYPE_BODYWASH -> "BODYWASH"
            TYPE_CLEANSING -> "CLEANSING"
            else -> "UNKNOWN($type)"
        }
    }

    companion object {
        const val TYPE_UNKNOWN = -1
        const val TYPE_SHAMPOO = 0
        const val TYPE_CONDITIONER = 1
        const val TYPE_BODYWASH = 2
        const val TYPE_CLEANSING = 3

        fun getTypeString(context: Context, type: Int): String {
            return context.getString(when (type) {
                TYPE_SHAMPOO -> R.string.shampoo
                TYPE_CONDITIONER -> R.string.conditioner
                TYPE_BODYWASH ->R.string.bodywash
                TYPE_CLEANSING -> R.string.cleansing
                else -> R.string.unknown
            })
        }
    }
}
