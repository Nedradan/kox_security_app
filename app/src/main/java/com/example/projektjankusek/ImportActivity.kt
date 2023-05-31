package com.example.projektjankusek

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ImportActivity : AppCompatActivity() {
    private lateinit var pathEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var importButton: Button
    private lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import)
        pathEditText = findViewById(R.id.editTextPath)
        passwordEditText = findViewById(R.id.editTextTextPassword)
        importButton = findViewById(R.id.ImportButtonImport)

        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), 1
        )

        importButton.setOnClickListener(){
            val backupFile = File(pathEditText.text.toString())
            val backupPassword = passwordEditText.text.toString()
            val restoredUser = restoreBackup(this, backupFile, backupPassword)
            userDao = AppDatabase.getDatabase(this).userDao()
            lifecycleScope.launch {
                    if (restoredUser != null) {
                        if(restoredUser.password == backupPassword ) {
                            Log.e("RestoreBackup", "Password: ${restoredUser.password}")
                            Log.e("RestoreBackup", "Password2: $backupPassword")
                            val existingUser = withContext(Dispatchers.IO) {
                                userDao.getUserByUsername(restoredUser.username)
                            }
                            if (existingUser == null) {
                                withContext(Dispatchers.IO) {
                                    userDao.insertUser(restoredUser)
                                }
                                showMessage("User restored successfully!")
                                showMessage("You can log in now!")
                            } else {
                                showMessage("Username already exists. Skipping import")
                            }
                        }
                    } else {
                        showMessage("Wrong password of filepath!")
                        showMessage("Import failed!")
                    }
            }
        }

    }

    private fun restoreBackup(context: Context, backupFile: File, backupPassword: String): User? {
        try {
            val encodedData = backupFile.readText()

            val encryptedData = Base64.decode(encodedData, Base64.DEFAULT)

            val decryptedData = decryptData(String(encryptedData), backupPassword)

            val parts = decryptedData.split(",")
            if (parts.size >= 3) {
                val username = parts[0]
                val password = parts[1]
                val pin = parts[2].toIntOrNull()

                return User(null, username, password, pin,null)
            }
        } catch (e: Exception) {
            Log.e("RestoreBackup", "Error restoring backup: ${e.message}")
        }
        return null
    }

    private fun decryptData(data: String, password: String): String {
        val decryptedBytes = ByteArray(data.length)
        for (i in data.indices) {
            decryptedBytes[i] = (data[i].toInt() xor password[i % password.length].toInt()).toByte()
        }
        return String(decryptedBytes)
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}