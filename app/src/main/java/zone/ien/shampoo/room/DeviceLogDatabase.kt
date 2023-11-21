package zone.ien.shampoo.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [DeviceLogEntity::class], version = 1)
abstract class DeviceLogDatabase: RoomDatabase() {
    abstract fun getDao(): DeviceDao

    companion object {
        private var instance: DeviceLogDatabase? = null
        fun getInstance(context: Context): DeviceLogDatabase? {
            if (instance == null) {
                synchronized(DeviceLogDatabase::class) {
                    instance = Room.databaseBuilder(context.applicationContext, DeviceLogDatabase::class.java, "DeviceLogDatabase.db")
                        .build()
                }
            }

            return instance
        }
    }
}