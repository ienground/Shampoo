package zone.ien.shampoo.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface DeviceLogDao {
    @Query("SELECT * FROM DeviceLogDatabase")
    fun getAll(): List<DeviceLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(data: DeviceLogEntity): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(data: DeviceLogEntity)

    @Query("SELECT * FROM DeviceLogDatabase WHERE id = :id")
    fun get(id: Long): DeviceLogEntity

    @Query("SELECT * FROM (SELECT * FROM DeviceLogDatabase WHERE (parentId = :parentId AND battery <> -1) ORDER BY timestamp DESC)")
    fun getBattery(parentId: Long): List<DeviceLogEntity>

    @Query("SELECT * FROM (SELECT * FROM DeviceLogDatabase WHERE (parentId = :parentId AND battery <> -1 AND timestamp >= :startTime AND timestamp < :endTime) ORDER BY timestamp DESC)")
    fun getBattery(parentId: Long, startTime: Long, endTime: Long): List<DeviceLogEntity>

    @Query("SELECT * FROM (SELECT * FROM DeviceLogDatabase WHERE (parentId = :parentId AND battery <> -1) ORDER BY timestamp DESC) LIMIT :count")
    fun getBattery(parentId: Long, count: Int): List<DeviceLogEntity>

    @Query("SELECT * FROM (SELECT * FROM DeviceLogDatabase WHERE (parentId = :parentId AND capacity <> -1) ORDER BY timestamp DESC)")
    fun getCapacity(parentId: Long): List<DeviceLogEntity>

    @Query("SELECT * FROM (SELECT * FROM DeviceLogDatabase WHERE (parentId = :parentId AND capacity <> -1 AND timestamp >= :startTime AND timestamp < :endTime) ORDER BY timestamp DESC)")
    fun getCapacity(parentId: Long, startTime: Long, endTime: Long): List<DeviceLogEntity>

    @Query("SELECT * FROM (SELECT * FROM DeviceLogDatabase WHERE (parentId = :parentId AND capacity <> -1) ORDER BY timestamp DESC) LIMIT :count")
    fun getCapacity(parentId: Long, count: Int): List<DeviceLogEntity>

    @Query("SELECT * FROM DeviceLogDatabase WHERE parentId = :parentId")
    fun getByParentId(parentId: Long): List<DeviceLogEntity>

    @Query("SELECT * FROM DeviceLogDatabase WHERE (parentId = :parentId AND timestamp >= :startTime AND timestamp <= :endTime)")
    fun getByParentId(parentId: Long, startTime: Long, endTime: Long): List<DeviceLogEntity>

    @Query("DELETE FROM DeviceLogDatabase WHERE id = :id")
    fun delete(id: Long)

    @Query("DELETE FROM DeviceLogDatabase WHERE parentId = :parentId")
    fun deleteByParentId(parentId: Long)

    @Query("SELECT EXISTS(SELECT * FROM DeviceLogDatabase WHERE id = :id)")
    fun checkIsAlreadyInDB(id: Long): Boolean

}