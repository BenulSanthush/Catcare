package com.example.catcare.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),


    val dueAt: Long? = null,
    val repeatMinutes: Int? = null,
    val catName: String? = null,
    val activity: String? = null
)
