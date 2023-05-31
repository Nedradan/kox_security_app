package com.example.projektjankusek

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import java.lang.IllegalArgumentException

class PinContentProvider : ContentProvider() {
    companion object {
        const val AUTHORITY = "com.example.projektjankusek.PinContentProvider"
        const val PATH_USER_PIN = "user_pin"
        val URI_USER_PIN: Uri = Uri.parse("content://$AUTHORITY/$PATH_USER_PIN")
    }

    private lateinit var userDao: UserDao

    override fun onCreate(): Boolean {
        userDao = context?.let { AppDatabase.getDatabase(it).userDao() }!!
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return when (uri) {
            URI_USER_PIN -> {
                val token = selectionArgs?.getOrNull(0)
                Log.d("ContentProvider", "token: $token")
                if (token != null) {
                    var user = userDao.getUserByToken(token.toString())

                    if (user != null) {
                        val cursor = MatrixCursor(arrayOf("pin"), 1)
                        cursor.addRow(arrayOf(user.pin))
                        cursor
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
            else -> null
        }
    }

    override fun getType(uri: Uri): String? {
        return when (uri) {
            URI_USER_PIN -> "vnd.android.cursor.item/user_pin"
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        return 0
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }
}

