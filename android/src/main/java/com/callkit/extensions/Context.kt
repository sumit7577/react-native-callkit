package com.callkit.extensions

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.loader.content.CursorLoader
import com.callkit.helpers.PERMISSION_ACCESS_COARSE_LOCATION
import com.callkit.helpers.PERMISSION_ACCESS_FINE_LOCATION
import com.callkit.helpers.PERMISSION_CALL_PHONE
import com.callkit.helpers.PERMISSION_CAMERA
import com.callkit.helpers.PERMISSION_GET_ACCOUNTS
import com.callkit.helpers.PERMISSION_MEDIA_LOCATION
import com.callkit.helpers.PERMISSION_POST_NOTIFICATIONS
import com.callkit.helpers.PERMISSION_READ_CALENDAR
import com.callkit.helpers.PERMISSION_READ_CALL_LOG
import com.callkit.helpers.PERMISSION_READ_CONTACTS
import com.callkit.helpers.PERMISSION_READ_MEDIA_AUDIO
import com.callkit.helpers.PERMISSION_READ_MEDIA_IMAGES
import com.callkit.helpers.PERMISSION_READ_MEDIA_VIDEO
import com.callkit.helpers.PERMISSION_READ_MEDIA_VISUAL_USER_SELECTED
import com.callkit.helpers.PERMISSION_READ_PHONE_STATE
import com.callkit.helpers.PERMISSION_READ_SMS
import com.callkit.helpers.PERMISSION_READ_STORAGE
import com.callkit.helpers.PERMISSION_READ_SYNC_SETTINGS
import com.callkit.helpers.PERMISSION_RECORD_AUDIO
import com.callkit.helpers.PERMISSION_SEND_SMS
import com.callkit.helpers.PERMISSION_WRITE_CALENDAR
import com.callkit.helpers.PERMISSION_WRITE_CALL_LOG
import com.callkit.helpers.PERMISSION_WRITE_CONTACTS
import com.callkit.helpers.PERMISSION_WRITE_STORAGE
import com.callkit.helpers.PREFS_KEY
import com.callkit.helpers.DirectSendSms
import com.callkit.helpers.MyContactsContentProvider
import com.callkit.helpers.isOnMainThread
import com.callkit.helpers.isQPlus

fun Context.getSharedPrefs() = getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
val Context.powerManager: PowerManager get() = getSystemService(Context.POWER_SERVICE) as PowerManager
val Context.notificationManager: NotificationManager get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

fun Context.queryCursor(
  uri: Uri,
  projection: Array<String>,
  selection: String? = null,
  selectionArgs: Array<String>? = null,
  sortOrder: String? = null,
  showErrors: Boolean = false,
  callback: (cursor: Cursor) -> Unit
) {
  try {
    val cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
    cursor?.use {
      if (cursor.moveToFirst()) {
        do {
          callback(cursor)
        } while (cursor.moveToNext())
      }
    }
  } catch (e: Exception) {
    if (showErrors) {
      showErrorToast(e)
    }
  }
}


fun Context.getMyContactsCursor(favoritesOnly: Boolean, withPhoneNumbersOnly: Boolean) = try {
  val getFavoritesOnly = if (favoritesOnly) "1" else "0"
  val getWithPhoneNumbersOnly = if (withPhoneNumbersOnly) "1" else "0"
  val args = arrayOf(getFavoritesOnly, getWithPhoneNumbersOnly)
  CursorLoader(this, MyContactsContentProvider.CONTACTS_CONTENT_URI, null, null, args, null).loadInBackground()
} catch (e: Exception) {
  null
}

fun Context.getCurrentContact(
  uri: Uri,
  projection: Array<String>,
  selection: String? = null,
  selectionArgs: Array<String>? = null,
  sortOrder: String? = null,
  showErrors: Boolean = false) = try {
  contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
  } catch (e:Exception) {
    null
  }

fun Context.sendDirectMessage(
  phoneNumber: String, msg: String
)=try{
  DirectSendSms(this,phoneNumber,msg)
}catch(e:Exception){
  null
}

fun Context.toast(id: Int, length: Int = Toast.LENGTH_SHORT) {
  toast(getString(id), length)
}

fun Context.toast(msg: String, length: Int = Toast.LENGTH_SHORT) {
  try {
    if (isOnMainThread()) {
      doToast(this, msg, length)
    } else {
      Handler(Looper.getMainLooper()).post {
        doToast(this, msg, length)
      }
    }
  } catch (e: Exception) {
  }
}

private fun doToast(context: Context, message: String, length: Int) {
  if (context is Activity) {
    if (!context.isFinishing && !context.isDestroyed) {
      Toast.makeText(context, message, length).show()
    }
  } else {
    Toast.makeText(context, message, length).show()
  }
}

fun Context.showErrorToast(msg: String, length: Int = Toast.LENGTH_LONG) {
  toast(msg, length)
}

fun Context.showErrorToast(exception: Exception, length: Int = Toast.LENGTH_LONG) {
  showErrorToast(exception.toString(), length)
}

fun Context.hasPermission(permId: Int) = ContextCompat.checkSelfPermission(this, getPermissionString(permId)) == PackageManager.PERMISSION_GRANTED

fun Context.getPermissionString(id: Int) = when (id) {
  PERMISSION_READ_STORAGE -> Manifest.permission.READ_EXTERNAL_STORAGE
  PERMISSION_WRITE_STORAGE -> Manifest.permission.WRITE_EXTERNAL_STORAGE
  PERMISSION_CAMERA -> Manifest.permission.CAMERA
  PERMISSION_RECORD_AUDIO -> Manifest.permission.RECORD_AUDIO
  PERMISSION_READ_CONTACTS -> Manifest.permission.READ_CONTACTS
  PERMISSION_WRITE_CONTACTS -> Manifest.permission.WRITE_CONTACTS
  PERMISSION_READ_CALENDAR -> Manifest.permission.READ_CALENDAR
  PERMISSION_WRITE_CALENDAR -> Manifest.permission.WRITE_CALENDAR
  PERMISSION_CALL_PHONE -> Manifest.permission.CALL_PHONE
  PERMISSION_READ_CALL_LOG -> Manifest.permission.READ_CALL_LOG
  PERMISSION_WRITE_CALL_LOG -> Manifest.permission.WRITE_CALL_LOG
  PERMISSION_GET_ACCOUNTS -> Manifest.permission.GET_ACCOUNTS
  PERMISSION_READ_SMS -> Manifest.permission.READ_SMS
  PERMISSION_SEND_SMS -> Manifest.permission.SEND_SMS
  PERMISSION_READ_PHONE_STATE -> Manifest.permission.READ_PHONE_STATE
  PERMISSION_MEDIA_LOCATION -> if (isQPlus()) Manifest.permission.ACCESS_MEDIA_LOCATION else ""
  PERMISSION_POST_NOTIFICATIONS -> Manifest.permission.POST_NOTIFICATIONS
  PERMISSION_READ_MEDIA_IMAGES -> Manifest.permission.READ_MEDIA_IMAGES
  PERMISSION_READ_MEDIA_VIDEO -> Manifest.permission.READ_MEDIA_VIDEO
  PERMISSION_READ_MEDIA_AUDIO -> Manifest.permission.READ_MEDIA_AUDIO
  PERMISSION_ACCESS_COARSE_LOCATION -> Manifest.permission.ACCESS_COARSE_LOCATION
  PERMISSION_ACCESS_FINE_LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
  PERMISSION_READ_MEDIA_VISUAL_USER_SELECTED -> Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
  PERMISSION_READ_SYNC_SETTINGS -> Manifest.permission.READ_SYNC_SETTINGS
  else -> ""
}


