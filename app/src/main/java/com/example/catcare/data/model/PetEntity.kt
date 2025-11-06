package com.example.catcare.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ageYears: Int,
    val ageMonths: Int = 0,
    val breed: String,
    val color: String,
    val favoriteFood: String,
    val photoUri: String? = null
)
