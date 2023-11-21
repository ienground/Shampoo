package zone.ien.shampoo.room

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.Date

@Entity (tableName = "DeviceLogDatabase")
data class DeviceLogEntity(
    var parentId: Long,
    var timestamp: Long,
    var battery: Int,
    var capacity: Int
) {
    @PrimaryKey(autoGenerate = true) var id: Long? = null

    override fun toString(): String {
        return "[$id] from ${parentId} $capacity battery ${battery}% / at ${Date(timestamp)}"
    }
}
