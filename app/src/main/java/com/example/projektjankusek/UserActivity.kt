package com.example.projektjankusek

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Base64
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


class UserActivity : AppCompatActivity() {
    private lateinit var userDao: UserDao
    private var currentUser: User? = null
    private lateinit var usernameEditText: TextView
    private lateinit var passwordEditText: TextView
    private lateinit var pinEditText: EditText
    private lateinit var generateTokenText: EditText
    private lateinit var updateButton: Button
    private lateinit var deleteButton: Button
    private lateinit var exportButton: Button
    private lateinit var generateToken: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)
        usernameEditText = findViewById(R.id.UserUserNameTextView)
        passwordEditText = findViewById(R.id.UserPasswordTextView)
        generateTokenText = findViewById(R.id.generateTokenText)
        pinEditText = findViewById(R.id.UserPinPassword)
        updateButton = findViewById(R.id.UserButtonSave)
        deleteButton = findViewById(R.id.UserButtonRemove)
        exportButton = findViewById(R.id.UserButtonExport)
        generateToken = findViewById(R.id.generateToken)
        val switch: Switch = findViewById(R.id.switch1)

        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), 1
        )

        val receivedID = intent.getIntExtra(
            "ID",
            0
        )
        userDao = AppDatabase.getDatabase(this).userDao()

        if (receivedID != 0) {
            lifecycleScope.launch {
                getData(receivedID)
            }
        } else {
            showMessage("User is not logged in!")
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        updateButton.setOnClickListener {
            lifecycleScope.launch {
                updatePin(receivedID)
            }
        }

        deleteButton.setOnClickListener {
            lifecycleScope.launch {
                removePin(receivedID)
            }
        }

        exportButton.setOnClickListener {
            val downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absolutePath
            createBackup(this,
                currentUser!!, currentUser!!.password,Uri.parse(downloadDirectory))

        }
        generateToken.setOnClickListener {
            val username = currentUser!!.username
            val password = currentUser!!.password
            val token = generateToken(username, password)
            generateTokenText.setText(token)
            updateToken(token,receivedID)
        }



        switch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                pinEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            } else {
                pinEditText.transformationMethod = PasswordTransformationMethod.getInstance()
            }
        }


    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun getData(id: Int) {
        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                userDao.getUserByID(id)
            }
            if (user != null) {
                currentUser = user
                usernameEditText.text = currentUser!!.username
                passwordEditText.text = currentUser!!.password
                currentUser!!.pin?.let { pinEditText.setText(it.toString()) }
                currentUser!!.token?.let { generateTokenText.setText(it.toString()) }
                if (currentUser!!.pin == null)
                    showMessage("No PIN Defined")
                showMessage("User data received!")
                Log.d("Database", "User: ${user.username}, ${user.token}")
                val user2 = withContext(Dispatchers.IO) {
                    userDao.getUserByToken(generateTokenText.text.toString())
                }
                Log.e("Database", "User: $user")

            }
        }
    }

    private fun updatePin(id: Int) {
        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                userDao.getUserByID(id)
            }
            val pinInput = pinEditText.text.toString()
            val newPin = pinInput.toIntOrNull()
            user?.let {
                val updatedUser = it.copy(pin = newPin)
                withContext(Dispatchers.IO) {
                    userDao.updateUser(updatedUser)
                }
            }
            pinEditText.text = null
            showMessage("Pin Updated!")
            getData(id)
        }
    }

    private fun removePin(id: Int) {
        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                userDao.getUserByID(id)
            }
            user?.let {
                val updatedUser = it.copy(pin = null)
                withContext(Dispatchers.IO) {
                    userDao.updateUser(updatedUser)
                }
            }
            pinEditText.text = null
            showMessage("Pin Removed!")
            getData(id)
        }
    }

    fun createBackup(context: Context, user: User, backupPassword: String, directoryUri: Uri?) {
        val backupData = "${user.username},${user.password},${user.pin}"

        val encryptedData = encryptData(backupData, backupPassword)

        val encodedData = Base64.encodeToString(encryptedData.toByteArray(), Base64.DEFAULT)


        try {
            val backupFile = createBackupFile(context, directoryUri)
            FileOutputStream(backupFile).use { outputStream ->
                outputStream.write(encodedData.toByteArray())
                outputStream.flush()
            }
            Log.d("Backup", "Backup created successfully at: ${backupFile.absolutePath}")
            showMessage("Backup created successfully at: ${backupFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("Backup", "Error creating backup: ${e.message}")
        }
    }

    private fun encryptData(data: String, password: String): String {
        val encryptedBytes = ByteArray(data.length)
        for (i in data.indices) {
            encryptedBytes[i] = (data[i].toInt() xor password[i % password.length].toInt()).toByte()
        }
        return String(encryptedBytes)
    }

    private fun createBackupFile(context: Context, directoryUri: Uri?): File {
        val backupDir: File = if (directoryUri != null) {
            File(directoryUri.path)
        } else {
            val backupDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "UserBackups")
            backupDir.mkdirs()
            backupDir
        }

        val timestamp = System.currentTimeMillis()
        val backupFileName = "user_backup_$timestamp.txt"
        return File(backupDir, backupFileName)
    }


    private fun generateToken(username: String, password: String): String {
        val timestamp = System.currentTimeMillis()
        val tokenData = "$password,$timestamp"

        // HMAC-SHA256 algorithm
        val signingKey = SecretKeySpec(username.toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(signingKey)
        val tokenBytes = mac.doFinal(tokenData.toByteArray())
        val token = Base64.encodeToString(tokenBytes, Base64.NO_PADDING or Base64.NO_WRAP)

        // Remove special characters from the token
        val cleanedToken = token.replace("[^A-Za-z0-9]".toRegex(), "")

        return cleanedToken
    }

    private fun updateToken(token: String,id: Int) {
        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                userDao.getUserByID(id)
            }
            user?.let {
                val updatedUser = it.copy(token = token)
                withContext(Dispatchers.IO) {
                    userDao.updateUser(updatedUser)
                }
            }
            generateTokenText.text = null
            showMessage("Token Updated!")
            getData(id)
        }
    }

}
