package com.callkit.helpers

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.callkit.R
import com.callkit.extensions.notificationManager


class CallNotificationManager(private val context: Context) {
  private val CALL_NOTIFICATION_ID = 42
  private val ACCEPT_CALL_CODE = 0
  private val DECLINE_CALL_CODE = 1
  private val notificationManager = context.notificationManager
  //private val callContactAvatarHelper = CallContactAvatarHelper(context)

  /*@SuppressLint("NewApi")
  fun setupNotification(forceLowPriority: Boolean = false) {
    getCallContact(context.applicationContext, CallRepository.getPrimaryCall(),"") { callContact ->
      val callContactAvatar = callContactAvatarHelper.getCallContactAvatar(callContact)
      val callState = CallRepository.getState()
      val isHighPriority = context.powerManager.isInteractive && callState == Call.STATE_RINGING && !forceLowPriority
      val channelId = if (isHighPriority) "react_dialer_high_priority" else "react_dialer_call"
      if (isOreoPlus()) {
        val importance = if (isHighPriority) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
        val name = if (isHighPriority) "call_notification_channel_high_priority" else "call_notification_channel"

        NotificationChannel(channelId, name, importance).apply {
          setSound(null, null)
          notificationManager.createNotificationChannel(this)
        }
      }

      val openAppIntent = CallActivity.getStartIntent(context)
      val openAppPendingIntent = PendingIntent.getActivity(context, 0, openAppIntent, PendingIntent.FLAG_MUTABLE)

      val acceptCallIntent = Intent(context, CallActionReceiver::class.java)
      acceptCallIntent.action = ACCEPT_CALL
      val acceptPendingIntent =
        PendingIntent.getBroadcast(context, ACCEPT_CALL_CODE, acceptCallIntent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE)

      val declineCallIntent = Intent(context, CallActionReceiver::class.java)
      declineCallIntent.action = DECLINE_CALL
      val declinePendingIntent =
        PendingIntent.getBroadcast(context, DECLINE_CALL_CODE, declineCallIntent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE)

      var callerName = if (callContact.name.isNotEmpty()) callContact.name else context.getString(R.string.unknown_caller)
      if (callContact.numberLabel.isNotEmpty()) {
        callerName += " - ${callContact.numberLabel}"
      }

      val contentTextId = when (callState) {
        Call.STATE_RINGING -> R.string.is_calling
        Call.STATE_DIALING -> R.string.dialing
        Call.STATE_DISCONNECTED -> R.string.call_ended
        Call.STATE_DISCONNECTING -> R.string.call_ending
        else -> R.string.ongoing_call
      }

      val collapsedView = RemoteViews(context.packageName, R.layout.call_notification).apply {
        setText(R.id.notification_caller_name, callerName)
        setText(R.id.notification_call_status, context.getString(contentTextId))
        setVisibleIf(R.id.notification_accept_call, callState == Call.STATE_RINGING)

        setOnClickPendingIntent(R.id.notification_decline_call, declinePendingIntent)
        setOnClickPendingIntent(R.id.notification_accept_call, acceptPendingIntent)

        if (callContactAvatar != null) {
          setImageViewBitmap(R.id.notification_thumbnail, callContactAvatarHelper.getCircularBitmap(callContactAvatar))
        }
      }

      val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(com.facebook.react.R.drawable.ic_resume)
        .setContentIntent(openAppPendingIntent)
        .setPriority(if (isHighPriority) NotificationManager.IMPORTANCE_HIGH else NotificationCompat.PRIORITY_DEFAULT)
        .setCategory(Notification.CATEGORY_CALL)
        .setCustomContentView(collapsedView)
        .setOngoing(true)
        .setSound(null)
        .setUsesChronometer(callState == Call.STATE_ACTIVE)
        .setChannelId(channelId)
        .setStyle(NotificationCompat.DecoratedCustomViewStyle())

      if (isHighPriority) {
        builder.setFullScreenIntent(openAppPendingIntent, true)
      }

      val notification = builder.build()
      // it's rare but possible for the call state to change by now
      if (CallRepository.getState() == callState) {
        notificationManager.notify(CALL_NOTIFICATION_ID, notification)
      }
    }
  }*/
  @SuppressLint("NewApi")
  fun showBlockedCallNotification(number: String) {
    val channelId = "react_dialer_blocked_call"
    val channelName = "Blocked Call Notifications"

    if (isOreoPlus()) {
      val channel = NotificationChannel(
        channelId,
        channelName,
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Notifications for blocked calls"
        setSound(null, null)
      }
      notificationManager.createNotificationChannel(channel)
    }

    val contentTitle = context.getString(R.string.blocked_call)
    val contentText = "Blocked 1 call from $number"

    val contentIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("blocked", true)
      putExtra("number",number)
    }

    val contentPendingIntent =
      PendingIntent.getActivity(context, ACCEPT_CALL_CODE, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

    val builder = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(com.facebook.react.R.drawable.ic_resume)
      .setContentTitle(contentTitle)
      .setContentText(contentText)
      .setAutoCancel(true)
      .setContentIntent(contentPendingIntent)
      .setPriority(NotificationCompat.PRIORITY_LOW)

    notificationManager.notify(number.hashCode(), builder.build())
  }

  fun cancelNotification() {
    notificationManager.cancel(CALL_NOTIFICATION_ID)
  }
}
