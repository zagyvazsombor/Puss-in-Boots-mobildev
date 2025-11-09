package com.example.shoeregistry

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val usernameInput = findViewById<TextInputEditText>(R.id.input_username)
        val passwordInput = findViewById<TextInputEditText>(R.id.input_password)
        val loginButton = findViewById<MaterialButton>(R.id.login_button)
        val themeSwitch = findViewById<SwitchMaterial>(R.id.theme_switch)

        loginButton.setOnClickListener {
            val username = usernameInput.text?.toString()?.trim() ?: " "
            val password = passwordInput.text?.toString()?.trim() ?: " "

            if(username.isEmpty() ||  password.isEmpty() ){
                Toast.makeText(
                    this,
                    getString(R.string.dummy_credentials_empty),
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(username != getString(R.string.admin_username) || password != getString(R.string.admin_password)){
                Toast.makeText(
                    this,
                    getString(R.string.dummy_credentials_wrong),
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

        }

        themeSwitch.setOnClickListener {
            val mode = if (themeSwitch.isChecked)
                AppCompatDelegate.MODE_NIGHT_NO
            else
                AppCompatDelegate.MODE_NIGHT_YES

            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }
}