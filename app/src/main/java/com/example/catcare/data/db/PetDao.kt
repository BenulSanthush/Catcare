package com.example.catcare.data.db

import androidx.room.*
import com.example.catcare.data.model.PetEntity
import com.example.catcare.data.model.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {

    //pets
    @Query("SELECT * FROM pets ORDER BY id DESC")
    fun getAll(): Flow<List<PetEntity>>

    @Query("SELECT * FROM pets WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pet: PetEntity): Long

    //  Reminders
    @Query("SELECT * FROM reminders ORDER BY completed ASC, createdAt DESC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Insert
    suspend fun insertReminder(r: ReminderEntity): Long

    @Query("UPDATE reminders SET completed = :completed WHERE id = :id")
    suspend fun setReminderCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: Long)

    // delete a pet by id
    @androidx.room.Query("DELETE FROM pets WHERE id = :id")
    suspend fun deletePet(id: Long)

    //get reminder IDs for a specific cat
    @androidx.room.Query("SELECT id FROM reminders WHERE catName = :name")
    suspend fun getReminderIdsForCat(name: String): List<Long>

    // delete all reminders for a specific cat
    @androidx.room.Query("DELETE FROM reminders WHERE catName = :name")
    suspend fun deleteRemindersForCat(name: String)

}

