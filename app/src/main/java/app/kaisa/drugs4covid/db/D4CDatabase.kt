package app.kaisa.drugs4covid.db

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteOpenHelper
import app.kaisa.drugs4covid.db.dao.AtcDao
import app.kaisa.drugs4covid.db.dao.DiseaseDao
import app.kaisa.drugs4covid.db.dao.DrugDao
import app.kaisa.drugs4covid.db.entity.Atc
import app.kaisa.drugs4covid.db.entity.Disease
import app.kaisa.drugs4covid.db.entity.Drug

@Database(
    entities = [Disease::class, Drug::class, Atc::class],
    version = 1,
)
abstract class D4CDatabase : RoomDatabase() {
    abstract fun disease(): DiseaseDao
    abstract fun drug(): DrugDao
    abstract fun atc(): AtcDao

    override fun clearAllTables() {
        TODO("Not yet implemented")
    }

    override fun createInvalidationTracker(): InvalidationTracker {
        TODO("Not yet implemented")
    }

    override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper {
        TODO("Not yet implemented")
    }

    companion object {
        fun initialize(context: Context): D4CDatabase {
            return Room.databaseBuilder(
                context,
                D4CDatabase::class.java,
                "d4c.db",
            ).build()
        }
    }
}
