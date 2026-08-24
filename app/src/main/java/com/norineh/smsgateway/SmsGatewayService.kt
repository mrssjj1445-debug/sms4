package com.norineh.smsgateway

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SmsGatewayService : Service() {

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val api = ApiClient()

    private val channelId = "norineh_sms_gateway"

    override fun onCreate() {
        super.onCreate()

        createChannel()

        val notification = notification(
            "در حال بررسی صف پیامک"
        )

        if (Build.VERSION.SDK_INT >= 29) {

            ServiceCompat.startForeground(
                this,
                1001,
                notification,
                android.content.pm.ServiceInfo
                    .FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )

        } else {

            startForeground(
                1001,
                notification
            )
        }

        serviceScope.launch {
            pollingLoop()
        }
    }

    private suspend fun pollingLoop() {

        while (serviceScope.isActive) {

            try {

                val jobs = api.getPending()

                if (jobs.isEmpty()) {

                    updateNotification(
                        "Gateway فعال است؛ صف خالی است"
                    )

                } else {

                    for (job in jobs) {

                        if (!serviceScope.isActive) {
                            break
                        }

                        sendJob(job)
                    }
                }

            } catch (_: Exception) {

                updateNotification(
                    "خطای ارتباط با سرور؛ تلاش مجدد"
                )
            }

            delay(Config.POLL_INTERVAL_MS)
        }
    }

    private fun sendJob(job: SmsJob) {

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            updateNotification(
                "مجوز ارسال SMS فعال نیست"
            )

            return
        }

        try {

            val smsManager = SmsManager.getDefault()

            val parts =
                smsManager.divideMessage(job.message)

            if (parts.size == 1) {

                smsManager.sendTextMessage(
                    job.phone,
                    null,
                    job.message,
                    null,
                    null
                )

            } else {

                smsManager.sendMultipartTextMessage(
                    job.phone,
                    null,
                    parts,
                    null,
                    null
                )
            }

            runBlocking {

                try {

                    api.reportResult(
                        job.id,
                        true
                    )

                    updateNotification(
                        "پیامک ${job.phone} ارسال شد"
                    )

                } catch (_: Exception) {

                    updateNotification(
                        "SMS ارسال شد ولی گزارش سرور ناموفق بود"
                    )
                }
            }

        } catch (e: Exception) {

            runBlocking {

                try {

                    api.reportResult(
                        job.id,
                        false,
                        e.message ?: "SMS send error"
                    )

                } catch (_: Exception) {
                }
            }

            updateNotification(
                "خطا در ارسال پیامک"
            )
        }
    }

    private fun createChannel() {

        if (Build.VERSION.SDK_INT >= 26) {

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Norineh SMS Gateway",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    private fun notification(
        text: String
    ): Notification {

        return NotificationCompat.Builder(
            this,
            channelId
        )
            .setContentTitle(
                "Norineh SMS Gateway"
            )
            .setContentText(text)
            .setSmallIcon(
                android.R.drawable.ic_dialog_info
            )
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(
        text: String
    ) {

        getSystemService(
            NotificationManager::class.java
        ).notify(
            1001,
            notification(text)
        )
    }

    override fun onDestroy() {

        serviceScope.cancel()

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? = null
}
