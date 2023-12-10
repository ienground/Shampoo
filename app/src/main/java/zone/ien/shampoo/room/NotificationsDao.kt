package zone.ien.shampoo.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface NotificationsDao {
    @Query("SELECT * FROM NotificationsDatabase")
    fun getAll(): List<NotificationsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(data: NotificationsEntity): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(data: NotificationsEntity)

    @Query("SELECT * FROM NotificationsDatabase WHERE id = :id")
    fun get(id: Long): NotificationsEntity

    @Query("SELECT * FROM NotificationsDatabase WHERE deviceId = :deviceId")
    fun getByDeviceId(deviceId: Long): List<NotificationsEntity>

    @Query("SELECT * FROM NotificationsDatabase WHERE (deviceId = :deviceId AND timestamp >= :startTime AND timestamp <= :endTime)")
    fun getByDeviceId(deviceId: Long, startTime: Long, endTime: Long): List<NotificationsEntity>

    @Query("DELETE FROM NotificationsDatabase WHERE deviceId = :deviceId")
    fun deleteByDeviceId(deviceId: Long)

    @Query("SELECT EXISTS(SELECT * FROM NotificationsDatabase WHERE id = :id)")
    fun checkIsAlreadyInDB(id: Long): Boolean

}