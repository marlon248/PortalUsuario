package com.marlon.portalusuario.nauta.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.IGNORE
import androidx.room.Query
import androidx.room.Update
import com.marlon.portalusuario.commons.USER_TABLE
import com.marlon.portalusuario.nauta.data.entities.User
import com.marlon.portalusuario.nauta.data.repository.Users
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM $USER_TABLE ORDER BY id ASC")
    fun getUsers(): Flow<Users>

    @Query("SELECT * FROM $USER_TABLE WHERE id = :id")
    suspend fun getUser(id: Int): User

    @Query("SELECT * FROM $USER_TABLE WHERE user_name = :userName")
    suspend fun getUser(userName: String): User

    @Insert(onConflict = IGNORE)
    suspend fun addUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)
}