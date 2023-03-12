package app.kaisa.drugs4covid.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.kaisa.drugs4covid.db.entity.Drug

@Dao
interface DrugDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(persons: List<Drug>)

    @Query("SELECT * FROM drug WHERE name LIKE :search_query")
    fun search(search_query: String?): List<Drug>

    @Query("SELECT COUNT(id) FROM drug")
    fun count(): Int
}
