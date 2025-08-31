package com.callkit

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import coil.ImageLoader
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.google.i18n.phonenumbers.NumberParseException
import com.callkit.callManager.CallActivity
import com.callkit.callManager.CallRepository
import com.callkit.helpers.getCallContact
import com.callkit.helpers.isUpsideDownPlus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

class CallService: InCallService() {

  private val callback = object : Call.Callback() {
    override fun onStateChanged(call: Call, newState: Int) {

      if(newState == Call.STATE_ACTIVE){
        stopForeground(true)
        handleCallChanges(call)
      }

      if(newState == Call.STATE_DISCONNECTED){
        val allCalls = CallRepository.getAllCalls()
        val callerNumber = call.details.handle?.schemeSpecificPart ?: "Unknown"

        if(CallEventRepository.isInitialized()){
          if (allCalls.any { it.details.handle == call.details.handle }) {
            CallEventRepository.emitRejectedCallEvent(call, callerNumber)
          } else {
            CallEventRepository.emitMissedCallEvent(call, callerNumber)
          }
        }
        else{
          println("NO Context found")
        }
      }
    }
  }

  @SuppressLint("MissingPermission", "ForegroundServiceType", "SwitchIntDef")
  private fun handleCallChanges(call: Call){
    val callerNumber = call.details.handle?.schemeSpecificPart ?: return
    getCallContact(this, call, callerNumber) { contact ->
      val callerName = contact.name.takeIf { it.isNotBlank() } ?: callerNumber.orEmpty()
      CoroutineScope(Dispatchers.IO).launch {
        val avatar: Bitmap? = if (contact.photoUri.isNotEmpty()) {
          loadAvatar(contact.photoUri)
        } else {
          null
        }

        withContext(Dispatchers.Main) {

          when (call.state) {
            Call.STATE_RINGING -> {
              val notification =
                showIncomingCallNotification(callerName, isIncoming = true, isOngoing = false,callerNumber,avatar)
              val intent = Intent(this@CallService, CallActivity::class.java).apply {
                putExtra("caller_number", callerNumber)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
              }
              startActivity(intent)
              if (isUpsideDownPlus()) {
                startForeground(
                  NOTIFICATION_ID,
                  notification,
                  ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                )
              } else {
                startForeground(NOTIFICATION_ID, notification)
              }
            }

            Call.STATE_DIALING, Call.STATE_CONNECTING -> {
              val intent = Intent(this@CallService, CallActivity::class.java).apply {
                putExtra("caller_number", callerNumber)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
              }
              startActivity(intent)
              val notification =
                showIncomingCallNotification(callerName, isIncoming = false, isOngoing = false,callerNumber,avatar)
              if (isUpsideDownPlus()) {
                startForeground(
                  NOTIFICATION_ID,
                  notification,
                  ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                )
              } else {
                startForeground(NOTIFICATION_ID, notification)
              }
            }

            Call.STATE_ACTIVE -> {
              val notification =
                showIncomingCallNotification(callerName, isIncoming = false, isOngoing = true,callerNumber,avatar)
              if (isUpsideDownPlus()) {
                startForeground(
                  NOTIFICATION_ID,
                  notification,
                  ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                )
              } else {
                startForeground(NOTIFICATION_ID, notification)
              }
            }

            else -> {
              stopForeground(true)
            }
          }
        }
      }
    }
  }

  override fun onCallAdded(call: Call) {
    super.onCallAdded(call)
    println("call added")
    CallRepository.setInCallService(this)
    CallRepository.onCallAdded(call)
    call.registerCallback(callback)
    handleCallChanges(call)
    if(CallEventRepository.isInitialized()){
      val context = CallEventRepository.getContext()
      val number = call.details.handle?.schemeSpecificPart
      CoroutineScope(Dispatchers.IO).launch {
        val dataStore = context?.applicationContext?.let { CallSettingDataStore(it) }
        val isBlocked = number?.let { dataStore?.isNumberBlocked(it) }
        if(isBlocked == true){
          call.disconnect()
        }
      }
    }
    //Log.d("CallRepository", "Blocked number not detected: $number. Ending call.")
  }

  override fun onCallRemoved(call: Call) {
    super.onCallRemoved(call)
    CallRepository.onCallRemoved(call)
    stopForeground(true)
  }

  override fun onCallAudioStateChanged(audioState: CallAudioState?) {
    super.onCallAudioStateChanged(audioState)
    if (audioState != null) {
      CallRepository.onAudioStateChanged(audioState)
      Log.d("service",audioState.toString())
    }
  }

  suspend fun loadAvatar(avatarUrl: String): Bitmap? {
    val imageLoader = ImageLoader(this)
    val request = ImageRequest.Builder(this)
      .data(avatarUrl)
      .transformations(CircleCropTransformation())
      .build()

    val result = imageLoader.execute(request)
    val drawable = result.drawable

    return (drawable as? BitmapDrawable)?.bitmap
  }


  @SuppressLint("RestrictedApi")
  @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
  private fun showIncomingCallNotification(callerName: String?,
                                           isIncoming:Boolean,
                                           isOngoing:Boolean,
                                           callerNumber:String?,
                                           avatar:Bitmap?):Notification {
    val intent = Intent(this, CallActivity::class.java).apply {
      putExtra("caller_number", callerNumber) // Pass the caller number to the activity
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    // Create the notification channel (if not already created)
    val dataStore = CallSettingDataStore(context = this)

    val isVibrationEnabled = runBlocking {
      dataStore.vibrationStatus.first()
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isOngoing) {
      val notificationManager = getSystemService(NotificationManager::class.java)
      if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
        val channel = NotificationChannel(
          CHANNEL_ID,
          CHANNEL_NAME,
          NotificationManager.IMPORTANCE_HIGH
        )
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        channel.setSound(
          ringtoneUri,
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        channel.enableVibration(true)
        channel.vibrationPattern = longArrayOf(0,500,500,500)
        channel.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(channel)
      }
    }

    val notificationLayout = RemoteViews(packageName, R.layout.notification_layout)
    notificationLayout.setTextViewText(R.id.notification_title, callerName ?: "Unknown Caller")

    // Define actions for the buttons
    val answerIntent = Intent(this, CallActivity::class.java).apply {
      action = "ANSWER_CALL"
    }
    val answerPendingIntent = PendingIntent.getActivity(
      this, 0, answerIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    notificationLayout.setOnClickPendingIntent(R.id.caller_pick, answerPendingIntent)

    val rejectIntent = Intent(this, CallActivity::class.java).apply {
      action = "REJECT_CALL"
    }
    val rejectPendingIntent = PendingIntent.getActivity(
      this, 0, rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    notificationLayout.setOnClickPendingIntent(R.id.caller_decline, rejectPendingIntent)

    val incomingCaller = Person.Builder()
      .setName(callerName ?: "Unknown Caller")
      .setImportant(true)

    if (avatar != null) {
      incomingCaller.setIcon(IconCompat.createFromIcon(Icon.createWithBitmap(avatar)))
    }

    val caller = incomingCaller.build()

    val callStyle = if (isOngoing || !isIncoming) {
      NotificationCompat.CallStyle.forOngoingCall(caller, rejectPendingIntent)
    } else {
      NotificationCompat.CallStyle.forIncomingCall(caller, rejectPendingIntent, answerPendingIntent)
    }

    // Determine the country dynamically
    val countryName = callerNumber?.let { getCountryNameFromNumber(it) } ?: ""

    // Set notification content text: show country only if it's available
    val contentText = if (countryName.isNotEmpty()) {
      "${callerName ?: "Unknown Caller"} $countryName"
    } else {
      callerName ?: "Unknown Caller"
    }

    val builder = NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(com.facebook.react.R.drawable.ic_resume) // Replace with your app's call icon
      .setContentTitle("Incoming Call")
      .setContentText(contentText)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setOngoing(true)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .setStyle(callStyle)
      .addPerson(caller)

    if (!isOngoing) {
      builder.setFullScreenIntent(pendingIntent, true)
    }

    return builder.build()
  }

  // Helper function to get country name from phone number
  private fun getCountryNameFromNumber(phoneNumber: String): String? {
    return try {
      val phoneUtil = PhoneNumberUtil.getInstance()
      val number = phoneUtil.parse(phoneNumber, null) // null region code for international numbers
      if (phoneUtil.isValidNumber(number)) {
        val regionCode = phoneUtil.getRegionCodeForNumber(number)
        regionCode?.let {
          val locale = Locale("", it)
          locale.displayCountry // Returns country name, e.g., "United States"
        }
      } else {
        null
      }
    } catch (e: NumberParseException) {
      Log.e("CallService", "Failed to parse phone number: $phoneNumber", e)
      null
    }
  }
  companion object {
    private const val CHANNEL_ID = "notification_channel"
    private const val CHANNEL_NAME = "Notification Channel"
    private const val NOTIFICATION_ID = 1
  }
}
