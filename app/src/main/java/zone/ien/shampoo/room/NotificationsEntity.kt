package zone.ien.shampoo.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity (tableName = "NotificationsDatabase")
data class NotificationsEntity(
    var type: Int,
    var timestamp: Long,
    var title: String,
    var content: String,
    var deviceId: Long
) {
    @PrimaryKey(autoGenerate = true) var id: Long? = null

    override fun toString(): String {
        return "[$id] from ${deviceId} $type $title: $content at ${Date(timestamp)}"
    }
}
