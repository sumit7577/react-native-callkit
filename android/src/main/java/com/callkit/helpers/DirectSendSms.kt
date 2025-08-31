package com.callkit.helpers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

fun DirectSendSms(
  context: Context,
  phoneNumber: String,
  msg: String
) {
  if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
    != PackageManager.PERMISSION_GRANTED) {

    // Request permission
    ActivityCompat.requestPermissions(
      context as Activity,
      arrayOf(Manifest.permission.SEND_SMS),
      1
    )
  } else {
    try {
      val smsManager = SmsManager.getDefault()
      smsManager.sendTextMessage(phoneNumber, null, msg, null, null)
      Toast.makeText(context, "SMS Sent!", Toast.LENGTH_SHORT).show()
    } catch (ex: Exception) {
      Toast.makeText(context, "Failed to send SMS.", Toast.LENGTH_SHORT).show()
    }
  }
}
