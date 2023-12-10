package zone.ien.shampoo.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PlaceDao {
    @Query("SELECT * FROM PlaceDatabase")
    fun getAll(): List<PlaceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(data: PlaceEntity): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(data: PlaceEntity)

    @Query("SELECT * FROM PlaceDatabase WHERE id = :id")
    fun get(id: Long): PlaceEntity

    @Query("DELETE FROM PlaceDatabase WHERE id = :id")
    fun delete(id: Long)

    @Query("SELECT EXISTS(SELECT * FROM PlaceDatabase WHERE id = :id)")
    fun checkIsAlreadyInDB(id: Long): Boolean

}