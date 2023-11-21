package zone.ien.shampoo.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface DeviceDao {
    @Query("SELECT * FROM DeviceDatabase")
    fun getAll(): List<DeviceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(data: DeviceEntity): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(data: DeviceEntity)

    @Query("SELECT * FROM DeviceDatabase WHERE id = :id")
    fun get(id: Long): DeviceEntity

    @Query("SELECT * FROM DeviceDatabase WHERE address = :address")
    fun get(address: String): DeviceEntity

    @Query("SELECT EXISTS(SELECT * FROM DeviceDatabase WHERE id = :id)")
    fun checkIsAlreadyInDB(id: Long): Boolean

    @Query("SELECT EXISTS(SELECT * FROM DeviceDatabase WHERE address = :address)")
    fun checkIsAlreadyInDB(address: String): Boolean

}