package com.example.projektjankusek

import androidx.room.*

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username")
    fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE token = :token")
    fun getUserByToken(token: String): User?

    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserByID(id: Int): User?

    @Query("SELECT * FROM users")
    fun getAllUsers(): List<User>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(user: User)

    @Update
    fun updateUser(user: User)
}