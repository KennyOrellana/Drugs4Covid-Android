package app.kaisa.drugs4covid.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.kaisa.drugs4covid.db.entity.Disease
import kotlinx.coroutines.flow.Flow

@Dao
interface DiseaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(persons: List<Disease>)

    @Query("SELECT * FROM disease WHERE name LIKE :search_query")
    fun search(search_query: String?): List<Disease>

    @Query("SELECT COUNT(id) FROM disease")
    fun count(): Int
}
