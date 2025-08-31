package com.callkit.helpers

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.annotation.RequiresPermission
import android.Manifest
import com.callkit.callManager.CallActivity
import com.facebook.react.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CallStateListener(
    private val context: Context,
    private val blockNumberHelper: BlockNumberHelper
) : PhoneStateListener() {

    companion object {
        private const val CHANNEL_ID = "blocked_call_channel"
        private const val CHANNEL_NAME = "Blocked Call Notifications"
        private const val NOTIFICATION_ID = 2
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
        super.onCallStateChanged(state, phoneNumber)
        Log.d("CallStateListener", "Call state changed: $state, Number: $phoneNumber")

        if (state == TelephonyManager.CALL_STATE_RINGING && !phoneNumber.isNullOrEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val isBlocked = blockNumberHelper.isNumberBlocked(phoneNumber)
                    Log.d("CallStateListener", "Number $phoneNumber blocked: $isBlocked")
                    if (isBlocked) {
                        withContext(Dispatchers.Main) {
                            showBlockedCallNotification(phoneNumber)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CallStateListener", "Error checking blocked number", e)
                }
            }
        }
    }

    private fun showBlockedCallNotification(callerNumber: String?) {
        Log.d("CallStateListener", "Preparing notification for number: $callerNumber")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                )
                channel.setSound(null, null)
                channel.enableVibration(true)
                channel.vibrationPattern = longArrayOf(0, 500, 500, 500)
                channel.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                notificationManager.createNotificationChannel(channel)
                Log.d("CallStateListener", "Notification channel created")
            }
        }

        getCallContact(context, null, callerNumber ?: "Unknown") { contact ->
            val callerName = contact.name.takeIf { it.isNotBlank() } ?: callerNumber ?: "Unknown Caller"
            Log.d("CallStateListener", "Showing notification for blocked call from $callerName")

            val intent = Intent(context, CallActivity::class.java).apply {
                putExtra("caller_number", callerNumber)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_resume)
                .setContentTitle("Blocked Call")
                .setContentText("Call from $callerName was blocked")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, builder.build())
                Log.d("CallStateListener", "Notification posted for $callerNumber")
            }
        }
    }
}
