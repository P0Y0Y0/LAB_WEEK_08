package com.example.lab_week_08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class NotificationService : Service() {

    //Create the notification builder that'll be called later on
    private lateinit var notificationBuilder: NotificationCompat.Builder
    //Create a system handler which controls what thread the process is being executed on
    private lateinit var serviceHandler: Handler

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        //Create the notification with all of its contents and configurations
        //in the startForegroundService() custom function
        notificationBuilder = startForegroundService()
        val handlerThread = HandlerThread("SecondThread")
            .apply { start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    private fun startForegroundService(): NotificationCompat.Builder {
        val pendingIntent = getPendingIntent()
        val channelId = createNotificationChannel()

        val notificationBuilder = getNotificationBuilder(
            pendingIntent, channelId
        )
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        return notificationBuilder
    }

    private fun getPendingIntent(): PendingIntent {
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            FLAG_IMMUTABLE else 0

        //Here, we're setting MainActivity into the pending Intent
        //When the user clicks the notification, they will be
        //redirected to the Main Activity of the app
        return PendingIntent.getActivity(
            this, 0, Intent(
                this,
                MainActivity::class.java
            ), flag
        )
    }

    private fun createNotificationChannel(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Create the channel id
            val channelId = "001"
            //Create the channel name
            val channelName = "001 Channel"
            val channelPriority = NotificationManager.IMPORTANCE_DEFAULT

            //Build the channel notification based on all 3 previous attributes
            val channel = NotificationChannel(
                channelId,
                channelName,
                channelPriority
            )
            //Get the NotificationManager class
            val service = requireNotNull(
                ContextCompat.getSystemService(this,
                    NotificationManager::class.java)
            )
            service.createNotificationChannel(channel)

            channelId
        } else { "" }

    private fun getNotificationBuilder(pendingIntent: PendingIntent, channelId:
    String) =
        NotificationCompat.Builder(this, channelId)
            //Sets the title
            .setContentTitle("Second worker process is done")
            //Sets the content
            .setContentText("Check it out!")
            //Sets the notification icon
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("Second worker process is done, check it out!")
            .setOngoing(true)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        val returnValue = super.onStartCommand(intent, flags, startId)

        val Id = intent?.getStringExtra(EXTRA_ID)
            ?: throw IllegalStateException("Channel ID must be provided")

        serviceHandler.post {
            countDownFromTenToZero(notificationBuilder)
            notifyCompletion(Id)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return returnValue
    }

    private fun countDownFromTenToZero(notificationBuilder: NotificationCompat.Builder) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as
                NotificationManager

        for (i in 10 downTo 0) {
            Thread.sleep(1000L)
            notificationBuilder.setContentText("$i seconds until last warning")
                .setSilent(true)
            notificationManager.notify(
                NOTIFICATION_ID,
                notificationBuilder.build()
            )
        }
    }

    private fun notifyCompletion(Id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableID.value = Id
        }
    }

    companion object {
        const val NOTIFICATION_ID = 0xCA7
        const val EXTRA_ID = "Id"
        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}