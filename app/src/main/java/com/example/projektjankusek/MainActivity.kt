package com.example.projektjankusek

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {
    private lateinit var userDao: UserDao
    private var currentUser: User? = null
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var rememberPasswordCheckBox: CheckBox

    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var importButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        usernameEditText = findViewById(R.id.editTextUserName)
        passwordEditText = findViewById(R.id.editTextPassword)
        rememberPasswordCheckBox = findViewById(R.id.CheckPassword)

        loginButton = findViewById(R.id.loginButton)
        registerButton = findViewById(R.id.registerButton)
        importButton = findViewById(R.id.ButtonImport)

        val sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        val savedUsername = sharedPreferences.getString("username", null)
        val savedPassword = sharedPreferences.getString("password", null)

        if (savedUsername != null && savedPassword != null) {
            usernameEditText.setText(savedUsername)
            passwordEditText.setText(savedPassword)
            rememberPasswordCheckBox.isChecked = true
        }

        userDao = AppDatabase.getDatabase(this).userDao()

        loginButton.setOnClickListener {
            loginUser()
        }
        registerButton.setOnClickListener {
            registerUser()
        }
        importButton.setOnClickListener {
            val intent = Intent(this, ImportActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun startUserActivity(loginId : Int) {
        if (loginId != 0) {
            val intent = Intent(this, UserActivity::class.java)
            intent.putExtra("ID", loginId)
            startActivity(intent)
        }
    }


    private fun registerUser() {
        val username = usernameEditText.text.toString()
        val password = passwordEditText.text.toString()

        lifecycleScope.launch {
            val existingUser = withContext(Dispatchers.IO) {
                userDao.getUserByUsername(username)
            }
            if (existingUser == null) {
                val newUser = User(null,username, password,null,null)

                withContext(Dispatchers.IO) {
                    userDao.insertUser(newUser)
                }
                val users = withContext(Dispatchers.IO) {
                    userDao.getAllUsers()
                }

                showMessage("Registration successful")
            } else {
                showMessage("Username already exists")
            }
        }
    }

    private fun loginUser() {
        val username = usernameEditText.text.toString()
        val password = passwordEditText.text.toString()
        val rememberPassword = rememberPasswordCheckBox.isChecked

        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                userDao.getUserByUsername(username)
            }

            if (user != null && user.password == password) {
                currentUser = user
                val sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("username", username)
                if (rememberPassword) {
                    editor.putString("password", password)
                } else {
                    editor.remove("password")
                }
                editor.apply()
                showMessage("Login successful")
                var id = user.id!!
                startUserActivity(id)
            } else {
                showMessage("Invalid username or password")
            }
        }
    }

}




