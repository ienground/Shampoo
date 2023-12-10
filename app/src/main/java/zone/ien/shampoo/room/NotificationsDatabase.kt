package zone.ien.shampoo.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [NotificationsEntity::class], version = 1, exportSchema = false)
abstract class NotificationsDatabase: RoomDatabase() {
    abstract fun getDao(): NotificationsDao

    companion object {
        private var instance: NotificationsDatabase? = null
        fun getInstance(context: Context): NotificationsDatabase? {
            if (instance == null) {
                synchronized(NotificationsDatabase::class) {
                    instance = Room.databaseBuilder(context.applicationContext, NotificationsDatabase::class.java, "NotificationsDatabase.db")
                        .build()
                }
            }

            return instance
        }
    }
}