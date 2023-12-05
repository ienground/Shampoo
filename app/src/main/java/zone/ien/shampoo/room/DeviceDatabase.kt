package zone.ien.shampoo.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [DeviceEntity::class], version = 1, exportSchema = false)
abstract class DeviceDatabase: RoomDatabase() {
    abstract fun getDao(): DeviceDao

    companion object {
        private var instance: DeviceDatabase? = null
        fun getInstance(context: Context): DeviceDatabase? {
            if (instance == null) {
                synchronized(DeviceDatabase::class) {
                    instance = Room.databaseBuilder(context.applicationContext, DeviceDatabase::class.java, "DeviceDatabase.db")
                        .build()
                }
            }

            return instance
        }
    }
}