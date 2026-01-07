package com.example.traverse2.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.traverse2.MainActivity
import com.example.traverse2.R

/**
 * StreakService - Foreground service that maintains an ongoing high-priority notification
 * for the user's streak status. On OxygenOS 16+ (Android 14+), this notification can be
 * mirrored as a Live Activity/Dynamic Island style alert.
 * 
 * Features:
 * - Ongoing notification that cannot be dismissed
 * - High priority for visibility
 * - Uses FOREGROUND_SERVICE_TYPE_SPECIAL_USE for live activity support
 * - Updates dynamically when streak changes
 */
class StreakService : Service() {
    
    companion object {
        const val CHANNEL_ID = "streak_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_UPDATE_STREAK = "com.example.traverse2.UPDATE_STREAK"
        const val EXTRA_STREAK_COUNT = "streak_count"
        const val EXTRA_NEXT_DEADLINE = "next_deadline"
        
        fun startService(context: Context, streakCount: Int, nextDeadlineHours: Int = 24) {
            val intent = Intent(context, StreakService::class.java).apply {
                putExtra(EXTRA_STREAK_COUNT, streakCount)
                putExtra(EXTRA_NEXT_DEADLINE, nextDeadlineHours)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun updateStreak(context: Context, streakCount: Int, nextDeadlineHours: Int = 24) {
            val intent = Intent(context, StreakService::class.java).apply {
                action = ACTION_UPDATE_STREAK
                putExtra(EXTRA_STREAK_COUNT, streakCount)
                putExtra(EXTRA_NEXT_DEADLINE, nextDeadlineHours)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            context.stopService(Intent(context, StreakService::class.java))
        }
    }
    
    private var currentStreak = 0
    private var hoursRemaining = 24
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentStreak = intent?.getIntExtra(EXTRA_STREAK_COUNT, 0) ?: 0
        hoursRemaining = intent?.getIntExtra(EXTRA_NEXT_DEADLINE, 24) ?: 24
        
        val notification = buildNotification()
        
        // Start as foreground service with appropriate type for Android 14+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ - Use FOREGROUND_SERVICE_TYPE_SPECIAL_USE for live activity compatibility
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Streak Status",
                NotificationManager.IMPORTANCE_HIGH  // High importance for live activity
            ).apply {
                description = "Shows your current coding streak"
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                // Enable for heads-up notifications
                enableVibration(false)
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun buildNotification(): Notification {
        // Intent to open app when notification is tapped
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build the notification content based on streak status
        val (title, content) = when {
            currentStreak == 0 -> Pair(
                "🎯 Start Your Streak!",
                "Solve a problem today to begin"
            )
            hoursRemaining <= 2 -> Pair(
                "⚠️ $currentStreak Day Streak - Expiring Soon!",
                "Only ${hoursRemaining}h left! Solve a problem now"
            )
            hoursRemaining <= 6 -> Pair(
                "🔥 $currentStreak Day Streak",
                "${hoursRemaining}h remaining to maintain streak"
            )
            else -> Pair(
                "🔥 $currentStreak Day Streak",
                "Keep it going! ${hoursRemaining}h until deadline"
            )
        }
        
        val emoji = if (currentStreak == 0) "🎯" else if (hoursRemaining <= 2) "⚠️" else "🔥"
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)  // Use app icon
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)  // Cannot be dismissed
            .setContentIntent(pendingIntent)
            .setShowWhen(false)
            // For OxygenOS 16+ live activity support
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("$emoji $currentStreak Day Streak")
                    .bigText("$content\n\nTap to open Traverse and solve a problem!")
            )
        
        // Add action button
        val solveIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("navigate_to", "problems")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        builder.addAction(
            android.R.drawable.ic_input_add,
            "Solve Problem",
            solveIntent
        )
        
        // For Android 12+ custom notification styling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }
        
        return builder.build()
    }
}
