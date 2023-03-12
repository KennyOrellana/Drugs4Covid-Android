package app.kaisa.drugs4covid.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.kaisa.drugs4covid.db.entity.Atc
import kotlinx.coroutines.flow.Flow

@Dao
interface AtcDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(persons: List<Atc>)

    @Query("SELECT * FROM atc WHERE name LIKE :search_query")
    fun search(search_query: String?): List<Atc>

    @Query("SELECT COUNT(id) FROM atc")
    fun count(): Int
}
