package zone.ien.shampoo.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [PlaceEntity::class], version = 2, exportSchema = false)
abstract class PlaceDatabase: RoomDatabase() {
    abstract fun getDao(): PlaceDao

    companion object {
        private var instance: PlaceDatabase? = null
        fun getInstance(context: Context): PlaceDatabase? {
            if (instance == null) {
                val migration1to2 = object: Migration(1, 2) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("ALTER TABLE PlaceDatabase ADD COLUMN icon TEXT NOT NULL DEFAULT 🛁")
                    }
                }
                synchronized(PlaceDatabase::class) {
                    instance = Room.databaseBuilder(context.applicationContext, PlaceDatabase::class.java, "PlaceDatabase.db")
                        .addMigrations(migration1to2)
                        .build()
                }
            }

            return instance
        }
    }
}