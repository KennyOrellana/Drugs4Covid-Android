package app.kaisa.drugs4covid.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "atc")
data class Atc(
    @PrimaryKey val id: String,
    val name: String,
)
