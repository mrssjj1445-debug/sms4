package com.norineh.smsgateway

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var status: TextView

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        status = TextView(this).apply {
            text = "در حال بررسی مجوزها..."
            textSize = 18f
            setPadding(30, 60, 30, 30)
        }

        val start = Button(this).apply {

            text = "فعال کردن Gateway"

            setOnClickListener {

                if (hasSmsPermission()) {
                    startGateway()
                } else {
                    requestSmsPermission()
                }
            }
        }

        val stop = Button(this).apply {

            text = "توقف Gateway"

            setOnClickListener {

                stopService(
                    Intent(
                        this@MainActivity,
                        SmsGatewayService::class.java
                    )
                )

                status.text =
                    "Gateway متوقف شد"
            }
        }

        setContentView(
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    30,
                    30,
                    30,
                    30
                )

                addView(status)
                addView(start)
                addView(stop)
            }
        )

        if (!hasSmsPermission()) {
            requestSmsPermission()
        } else {
            requestNotificationPermission()

            status.text =
                "آماده فعال‌سازی Gateway"
        }
    }

    private fun hasSmsPermission(): Boolean {

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestSmsPermission() {

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.SEND_SMS
            ),
            1001
        )
    }

    private fun requestNotificationPermission() {

        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS
                ),
                1002
            )
        }
    }

    private fun startGateway() {

        if (!hasSmsPermission()) {
            requestSmsPermission()
            return
        }

        ContextCompat.startForegroundService(
            this,
            Intent(
                this,
                SmsGatewayService::class.java
            )
        )

        status.text =
            "Gateway فعال است؛ در حال بررسی صف پیامک"
    }
}
