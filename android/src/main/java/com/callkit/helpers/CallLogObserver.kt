package com.callkit.helpers

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import com.callkit.callManager.CallActivity
import com.facebook.react.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CallLogObserver(
    private val context: Context,
    private val callSettingDataStore: CallSettingDataStore
) : ContentObserver(Handler(Looper.getMainLooper())) {

    companion object {
        private const val CHANNEL_ID = "blocked_call_channel"
        private const val CHANNEL_NAME = "Blocked Call Notifications"
        private const val NOTIFICATION_ID = 2
    }

    @RequiresPermission(Manifest.permission.READ_CALL_LOG)
    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        Log.d("CallLogObserver", "Call log changed, selfChange: $selfChange")
        checkRecentCall()
    }

    @RequiresPermission(Manifest.permission.READ_CALL_LOG)
    private fun checkRecentCall() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if block notification is enabled
                val showNotification = callSettingDataStore.showBlockNotificationStatus.first()
                if (!showNotification) {
                    Log.d("CallLogObserver", "Block notifications disabled, skipping check")
                    return@launch
                }

                val cursor = context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(
                        CallLog.Calls.NUMBER,
                        CallLog.Calls.TYPE,
                        CallLog.Calls.DATE,
                        CallLog.Calls.DURATION
                    ),
                    null,
                    null,
                    "${CallLog.Calls.DATE} DESC LIMIT 5" // Limit to last 5 calls
                )

                cursor?.use {
                    if (it.moveToFirst()) {
                        val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                        val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
                        val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
                        val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)

                        if (numberIndex != -1 && typeIndex != -1 && dateIndex != -1) {
                            do {
                                val number = it.getString(numberIndex) ?: continue
                                val callType = it.getInt(typeIndex)
                                val callDate = it.getLong(dateIndex)
                                val duration = it.getLong(durationIndex)
                                val callTypeName = when (callType) {
                                    CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                                    CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                                    CallLog.Calls.MISSED_TYPE -> "MISSED"
                                    CallLog.Calls.REJECTED_TYPE -> "REJECTED"
                                    CallLog.Calls.BLOCKED_TYPE -> "BLOCKED"
                                    CallLog.Calls.VOICEMAIL_TYPE -> "VOICEMAIL"
                                    CallLog.Calls.ANSWERED_EXTERNALLY_TYPE -> "ANSWERED_EXTERNALLY"
                                    else -> "UNKNOWN ($callType)"
                                }
                                Log.d(
                                    "CallLogObserver",
                                    "Call: Number=$number, Type=$callTypeName, Date=$callDate, Duration=$duration, Recent=${isRecentCall(callDate)}"
                                )

                                // Check if the number is blocked, regardless of call type
                                if (isRecentCall(callDate)) {
                                    val isBlocked = callSettingDataStore.isNumberBlocked(number)
                                    Log.d(
                                        "CallLogObserver",
                                        "Checking if $number is blocked: $isBlocked"
                                    )
                                    if (isBlocked) {
                                        withContext(Dispatchers.Main) {
                                            showBlockedCallNotification(number)
                                        }
                                    }
                                }
                            } while (it.moveToNext())
                        } else {
                            Log.e("CallLogObserver", "Required columns not found in call log")
                        }
                    } else {
                        Log.d("CallLogObserver", "No call log entries found")
                    }
                }
            } catch (e: SecurityException) {
                Log.e("CallLogObserver", "Permission denied: READ_CALL_LOG", e)
            } catch (e: Exception) {
                Log.e("CallLogObserver", "Error checking call log", e)
            }
        }
    }

    private fun isRecentCall(callDate: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        val fifteenMinutesAgo = currentTime - 15 * 60 * 1000 // 15 minutes
        return callDate > fifteenMinutesAgo
    }

    @SuppressLint("MissingPermission")
    private fun showBlockedCallNotification(callerNumber: String?) {
        Log.d("CallLogObserver", "Preparing notification for number: $callerNumber")
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
                Log.d("CallLogObserver", "Notification channel created")
            }
        }

        getCallContact(context, null, callerNumber ?: "Unknown") { contact ->
            val callerName = contact.name.takeIf { it.isNotBlank() } ?: callerNumber ?: "Unknown Caller"
            Log.d("CallLogObserver", "Showing notification for blocked call from $callerName")

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
                Log.d("CallLogObserver", "Notification posted for $callerNumber")
            }
        }
    }
}
