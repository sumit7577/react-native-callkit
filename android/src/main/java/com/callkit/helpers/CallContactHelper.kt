package com.callkit.helpers

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.telecom.Call
import com.callkit.extensions.getCurrentContact
import com.callkit.extensions.getStringValue
import com.callkit.extensions.isConference
import com.callkit.models.CallContact

fun getCallContact(
  context: Context,
  call: Call?,
  phoneNumber: String,
  callback: (CallContact) -> Unit
) {

  /*val activity = (context as Activity)
  val REQUEST_CODE_READ_CONTACTS = 100

  if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
    != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_CONTACTS), REQUEST_CODE_READ_CONTACTS)
  }*/

  if (call.isConference()) {
    callback(CallContact("Conference Call", "", "", ""))
    return
  }

  val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
    .appendPath(phoneNumber)
    .build()

  val projection = arrayOf(
    ContactsContract.PhoneLookup.DISPLAY_NAME,
    ContactsContract.PhoneLookup.PHOTO_URI,
    ContactsContract.PhoneLookup.NUMBER,
    ContactsContract.PhoneLookup.NORMALIZED_NUMBER
  )
  val privateCursor = context.getCurrentContact(uri,projection)
  ensureBackgroundThread {
    val callContact = CallContact("", "", "", "")
    val handle = try {
      call?.details?.handle?.toString()
    } catch (e: NullPointerException) {
      null
    }

    if (handle == null) {
      callback(callContact)
      return@ensureBackgroundThread
    }

    val uri = Uri.decode(handle)
    if (uri.startsWith("tel:")) {
      try{
        privateCursor?.use{
          if(privateCursor.moveToFirst()){
            val name = privateCursor.getStringValue(ContactsContract.PhoneLookup.DISPLAY_NAME)
            val photo = privateCursor.getStringValue(ContactsContract.PhoneLookup.PHOTO_URI)
            val number = privateCursor.getStringValue(ContactsContract.PhoneLookup.NUMBER)
            val numberLabel = privateCursor.getStringValue(ContactsContract.PhoneLookup.NORMALIZED_NUMBER)
            callContact.name = name
            callContact.number = number
            callContact.photoUri = photo ?:""
            callContact.numberLabel = numberLabel
          }
        }
      }
      catch (e:Exception){
        e.printStackTrace()
      }
    }

    Handler(Looper.getMainLooper()).post {
      callback(callContact)
    }
  }
}
