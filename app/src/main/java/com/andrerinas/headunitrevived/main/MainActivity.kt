package com.andrerinas.headunitrevived.main

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button // Added import
import android.widget.ImageButton // Added import
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.andrerinas.headunitrevived.App
import com.andrerinas.headunitrevived.R
import com.andrerinas.headunitrevived.aap.AapProjectionActivity
import com.andrerinas.headunitrevived.utils.AppLog
import com.andrerinas.headunitrevived.utils.SystemUI
import com.andrerinas.headunitrevived.utils.toInetAddress
//import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

import android.widget.Toast // Added import

class MainActivity : FragmentActivity() {

    private var lastBackPressTime: Long = 0
    var keyListener: KeyListener? = null
    private val viewModel: MainViewModel by viewModels()

    private lateinit var video_button: ImageButton // Declared
    private lateinit var usb: ImageButton // Declared
    private lateinit var settings: ImageButton // Declared
    private lateinit var wifi: ImageButton // Declared

    interface KeyListener
    {
        fun onKeyEvent(event: KeyEvent?): Boolean
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme) // Switch back to the normal theme
        setContentView(R.layout.activity_main)

        video_button = findViewById(R.id.video_button) // Initialized
        usb = findViewById(R.id.usb) // Initialized
        settings = findViewById(R.id.settings) // Initialized
        wifi = findViewById(R.id.wifi) // Initialized

        video_button.setOnClickListener {
            if (App.provide(this).transport.isAlive) {
                val aapIntent = Intent(this@MainActivity, AapProjectionActivity::class.java)
                aapIntent.putExtra(AapProjectionActivity.EXTRA_FOCUS, true)
                startActivity(aapIntent)
            } else {
                Toast.makeText(this, getString(R.string.no_android_auto_device_connected), Toast.LENGTH_LONG).show()
            }
        }

        usb.setOnClickListener {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.main_content, UsbListFragment())
                    .commit()
        }

        settings.setOnClickListener {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.main_content, SettingsFragment())
                    .commit()
        }

        wifi.setOnClickListener {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.main_content, NetworkListFragment())
                    .commit()
        }

        viewModel.register()

        try {
            val currentIp = App.provide(this).wifiManager.connectionInfo.ipAddress
            val inet = currentIp.toInetAddress()
            val ipView = findViewById<TextView>(R.id.ip_address)
            ipView.text = inet.hostAddress ?: ""
        } catch (ignored: IOException) { }

        ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION
        ), permissionRequestCode)

        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.main_content, HomeFragment())
                    .commit()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        SystemUI.hide(window.decorView)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onBackPressed() {
        if (System.currentTimeMillis() - lastBackPressTime < 2000) {
            super.onBackPressed()
        } else {
            lastBackPressTime = System.currentTimeMillis()
            Toast.makeText(this, R.string.press_back_again_to_exit, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        AppLog.i("onKeyDown: %d", keyCode)

        return keyListener?.onKeyEvent(event) ?: super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        AppLog.i("onKeyUp: %d", keyCode)

        return keyListener?.onKeyEvent(event) ?: super.onKeyUp(keyCode, event)
    }

    companion object {
        private const val permissionRequestCode = 97
    }
}
