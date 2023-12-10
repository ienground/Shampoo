package zone.ien.shampoo.room

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.Date

@Entity (tableName = "PlaceDatabase")
data class PlaceEntity(
    var icon: String,
    var title: String,
) {
    @PrimaryKey(autoGenerate = true) var id: Long? = null

    override fun toString(): String {
        val builder = StringBuilder("[$id] {${icon}} $title")
        return builder.toString()
    }
}
