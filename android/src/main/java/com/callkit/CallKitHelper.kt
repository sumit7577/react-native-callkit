package com.callkit


import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Context.ROLE_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telecom.TelecomManager.ACTION_CHANGE_DEFAULT_DIALER
import android.telecom.TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME
import android.util.Log
import android.util.SparseArray
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat.getSystemService
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.callkit.helpers.SimpleContactsHelper
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.callkit.helpers.ContactHelper
import com.callkit.helpers.ensureBackgroundThread
import com.callkit.helpers.setCallForwarding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class CallKitHelper(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext),
  ActivityEventListener {

  private val dialerResultCallbacks =SparseArray<Promise>()
  private var dialerRequestCode = 9876

  override fun getName(): String {
    return NAME
  }

  init {
    CallEventRepository.initialize(reactContext)
    reactContext.addActivityEventListener(this)
  }

  override fun onActivityResult(activity: Activity?, requestCode: Int, resultCode: Int, data: Intent?) {
    val promise = dialerResultCallbacks.get(requestCode) ?: return
    if (resultCode == Activity.RESULT_OK) {
      promise.resolve("Accepted")
    } else {
      promise.reject("Rejected", "User did not accept default dialer")
    }
    dialerResultCallbacks.remove(requestCode)
  }

  override fun onNewIntent(intent: Intent?) {
    // Not needed for this case
  }


  @RequiresApi(Build.VERSION_CODES.Q)
  @ReactMethod
  fun requestRole(promise: Promise) {
    val roleManager = reactApplicationContext.getSystemService(RoleManager::class.java)
    val telecomManager = reactApplicationContext.getSystemService(TelecomManager::class.java)
    val packageName = reactApplicationContext.packageName

    if (roleManager == null) {
      promise.reject("TelecomError", "TelecomManager not available")
      return
    }

    if (telecomManager.defaultDialerPackage == packageName) {
      promise.resolve("Already Default Dialer")
      return
    }

    val activity = currentActivity
    if (activity == null) {
      promise.reject("ActivityError", "No current activity")
      return
    }

    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)

    dialerResultCallbacks.put(dialerRequestCode, promise)
    activity.startActivityForResult(intent, dialerRequestCode)
    dialerRequestCode++
  }

  @ReactMethod
  fun makeCall(phoneNumber: String, promise: Promise){
    try {
      if (phoneNumber.isEmpty()) {
        promise.reject("INVALID_NUMBER", "Phone number is missing or invalid")
      }
      val telecomManager = reactApplicationContext.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
      val uri = Uri.fromParts("tel", phoneNumber, null)
      val callIntent = Intent(Intent.ACTION_CALL, uri)
      if (reactApplicationContext.checkSelfPermission(Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
        telecomManager.placeCall(uri, null)
        promise.resolve("Call placed successfully")
      } else {
        promise.reject("PERMISSION_DENIED", "Permission to make calls not granted")
      }
    } catch (e: Exception) {
      promise.reject("CALL_FAILED", "Failed to place the call: ${e.message}")
    }
  }

  @ReactMethod
  fun toggleSecureNumber(value:Boolean, promise: Promise){
    try{
      val dataStore = CallSettingDataStore(context = reactApplicationContext)
      CoroutineScope(Dispatchers.IO).launch {
        try {
          dataStore.updateCallSettings(hideCall = value, updateCall = false) // Assuming updateCall is false by default
          promise.resolve("Secure Number Updated")
        } catch (e: Exception) {
          promise.reject("Secure Failed", "Failed to secure the number: ${e.message}")
        }
      }
    }
    catch (e:Exception){
      promise.reject("Secure Failed", "Failed to secure the number: ${e.message}")
    }
  }

  @ReactMethod
  fun getSecureNumber(promise: Promise) {
    try {
      val dataStore = CallSettingDataStore(context = reactApplicationContext)

      CoroutineScope(Dispatchers.IO).launch {
        try {
          val hideCallValue = dataStore.secureNumberStatus.first()
          promise.resolve(hideCallValue)
        } catch (e: Exception) {
          promise.reject("Fetch Failed", "Failed to get secure number: ${e.message}")
        }
      }
    } catch (e: Exception) {
      promise.reject("Fetch Failed", "Failed to get secure number: ${e.message}")
    }
  }

  @ReactMethod
  fun toggleVibration(value: Boolean, promise: Promise) {
    try {
      val dataStore = CallSettingDataStore(context = reactApplicationContext)
      CoroutineScope(Dispatchers.IO).launch {
        try {
          dataStore.updateVibrationSetting(vibrate = value)
          promise.resolve("Vibration setting updated")
        } catch (e: Exception) {
          promise.reject("Vibration Failed", "Failed to update vibration setting: ${e.message}")
        }
      }
    } catch (e: Exception) {
      promise.reject("Vibration Failed", "Failed to update vibration setting: ${e.message}")
    }
  }

  @ReactMethod
  fun getVibrationStatus(promise: Promise) {
    try {
      val dataStore = CallSettingDataStore(context = reactApplicationContext)
      CoroutineScope(Dispatchers.IO).launch {
        try {
          val vibrationEnabled = dataStore.vibrationStatus.first()
          promise.resolve(vibrationEnabled)
        } catch (e: Exception) {
          promise.reject("Fetch Failed", "Failed to get vibration status: ${e.message}")
        }
      }
    } catch (e: Exception) {
      promise.reject("Fetch Failed", "Failed to get vibration status: ${e.message}")
    }
  }

  @RequiresApi(Build.VERSION_CODES.O)
  @ReactMethod
  fun forwardAllCalls(cfi: Boolean, phoneNumber: String, countryCode: String?,subscriptionId: Int ,promise: Promise){
    try {
      setCallForwarding(reactApplicationContext, cfi, phoneNumber,countryCode, subscriptionId,promise)
    } catch (e: Exception) {
      promise.reject("ERROR", "Failed to forward calls: ${e.localizedMessage}")
    }
  }

  @ReactMethod
  fun saveReplies(reply: String, promise: Promise) {
    val dataStore = CallSettingDataStore(context = reactApplicationContext)
    CoroutineScope(Dispatchers.IO).launch {
      try {
        dataStore.saveReply(reactApplicationContext, reply)
        withContext(Dispatchers.Main) { promise.resolve("Saved") }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) { promise.reject("SAVE_ERROR", e) }
      }
    }
  }
  @ReactMethod
  fun updateReplies(replies: ReadableArray, promise: Promise) {
    val dataStore = CallSettingDataStore(context = reactApplicationContext)
    CoroutineScope(Dispatchers.IO).launch {
      try {
        // Convert ReadableArray to List<String>
        val repliesList = mutableListOf<String>()
        for (i in 0 until replies.size()) {
          repliesList.add(replies.getString(i))
        }
        dataStore.updateReplies(reactApplicationContext, repliesList)
        withContext(Dispatchers.Main) { promise.resolve("Replies Updated") }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) { promise.reject("UPDATE_ERROR", e) }
      }
    }
  }
  @ReactMethod
  fun deleteReply(reply: String, promise: Promise) {
    val dataStore = CallSettingDataStore(context = reactApplicationContext)
    CoroutineScope(Dispatchers.IO).launch {
      try {
        dataStore.deleteReply(reply)
        withContext(Dispatchers.Main) { promise.resolve("Reply Deleted") }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) { promise.reject("DELETE_ERROR", e) }
      }
    }
  }

  @ReactMethod
  fun getReplies(promise: Promise) {
    val dataStore = CallSettingDataStore(context = reactApplicationContext)
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val replies = dataStore.repliesFlow.first()
        val result = Arguments.createArray().apply {
          replies.forEach { pushString(it) }
        }
        withContext(Dispatchers.Main) { promise.resolve(result) }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) { promise.reject("GET_ERROR", e) }
      }
    }
  }

  @ReactMethod
  fun getAllContacts(promise: Promise) {
    try {
      val contacts = SimpleContactsHelper(reactApplicationContext)
      val result = Arguments.createArray()
      contacts.getAvailableContacts(false){ items->
        items.forEach { contact ->
          val contactMap = Arguments.createMap().apply {
            putInt("rawId", contact.rawId)
            putInt("contactId", contact.contactId)
            putString("name", contact.name)
            putString("photoUri", contact.photoUri)

            // phone numbers
            val phoneArray = Arguments.createArray()
            contact.phoneNumbers.forEach { number ->
              val numberMap = Arguments.createMap()
              numberMap.putString("value", number.value)
              numberMap.putInt("type", number.type)
              numberMap.putString("label", number.label)
              numberMap.putString("normalizedNumber", number.normalizedNumber)
              numberMap.putBoolean("isPrimary", number.isPrimary)
              phoneArray.pushMap(numberMap)
            }
            putArray("phoneNumbers", phoneArray)

            // birthdays
            val birthdaysArray = Arguments.createArray()
            contact.birthdays.forEach { birthdaysArray.pushString(it) }
            putArray("birthdays", birthdaysArray)

            // anniversaries
            val anniversariesArray = Arguments.createArray()
            contact.anniversaries.forEach { anniversariesArray.pushString(it) }
            putArray("anniversaries", anniversariesArray)
          }
          result.pushMap(contactMap)
        }
      }
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("GET_CONTACTS_ERROR", "Failed to fetch contacts: ${e.localizedMessage}", e)
    }
  }
  @ReactMethod
  fun getContactById(rawContactId: Int, promise: Promise) {
    ensureBackgroundThread {
      try {
        val contactHelper = ContactHelper(reactApplicationContext)
        val contact = contactHelper.getContactById(rawContactId.toLong())

        if (contact != null) {
          val contactMap = Arguments.createMap().apply {
            putInt("rawId", contact.rawId)
            putInt("contactId", contact.contactId)
            putString("name", contact.name)
            putString("photoUri", contact.photoUri)
            putString("prefix", contact.prefix)
            putString("firstName", contact.firstName)
            putString("middleName", contact.middleName)
            putString("surname", contact.surname)
            putString("suffix", contact.suffix)
            putString("nickname", contact.nickname)
            putString("thumbnailUri", contact.thumbnailUri)
            putString("notes", contact.notes)
            putString("source", contact.source)
            putInt("starred", contact.starred)
            putString("mimetype", contact.mimetype)
            putString("ringtone", contact.ringtone)

            // Phone numbers
            val phoneArray = Arguments.createArray()
            contact.phoneNumbers.forEach { number ->
              val numberMap = Arguments.createMap()
              numberMap.putString("value", number.value)
              numberMap.putInt("type", number.type)
              numberMap.putString("label", number.label)
              numberMap.putString("normalizedNumber", number.normalizedNumber)
              numberMap.putBoolean("isPrimary", number.isPrimary)
              phoneArray.pushMap(numberMap)
            }
            putArray("phoneNumbers", phoneArray)

            // Emails
            val emailArray = Arguments.createArray()
            contact.emails.forEach { email ->
              val emailMap = Arguments.createMap()
              emailMap.putString("value", email.value)
              emailMap.putInt("type", email.type)
              emailMap.putString("label", email.label)
              emailArray.pushMap(emailMap)
            }
            putArray("emails", emailArray)

            // Addresses
            val addressArray = Arguments.createArray()
            contact.addresses.forEach { address ->
              val addressMap = Arguments.createMap()
              addressMap.putString("value", address.value)
              addressMap.putInt("type", address.type)
              addressMap.putString("label", address.label)
              addressArray.pushMap(addressMap)
            }
            putArray("addresses", addressArray)

            // Events
            val eventArray = Arguments.createArray()
            contact.events.forEach { event ->
              val eventMap = Arguments.createMap()
              eventMap.putString("value", event.value)
              eventMap.putInt("type", event.type)
              eventArray.pushMap(eventMap)
            }
            putArray("events", eventArray)

            // Birthdays
            val birthdaysArray = Arguments.createArray()
            contact.birthdays.forEach { birthdaysArray.pushString(it) }
            putArray("birthdays", birthdaysArray)

            // Anniversaries
            val anniversariesArray = Arguments.createArray()
            contact.anniversaries.forEach { anniversariesArray.pushString(it) }
            putArray("anniversaries", anniversariesArray)

            // Groups
            val groupArray = Arguments.createArray()
            contact.groups.forEach { group ->
              val groupMap = Arguments.createMap()
              group.id?.let { groupMap.putInt("id", it.toInt()) }
              groupMap.putString("title", group.title)
              groupArray.pushMap(groupMap)
            }
            putArray("groups", groupArray)

            // Organization
            val orgMap = Arguments.createMap()
            orgMap.putString("company", contact.organization.company)
            orgMap.putString("title", contact.organization.jobPosition)
            putMap("organization", orgMap)

            // Websites
            val websiteArray = Arguments.createArray()
            contact.websites.forEach { websiteArray.pushString(it) }
            putArray("websites", websiteArray)

            // IMs
            val imArray = Arguments.createArray()
            contact.IMs.forEach { im ->
              val imMap = Arguments.createMap()
              imMap.putString("value", im.value)
              imMap.putInt("type", im.type)
              imMap.putString("label", im.label)
              imArray.pushMap(imMap)
            }
            putArray("IMs", imArray)
          }
          promise.resolve(contactMap)
        } else {
          promise.reject("CONTACT_NOT_FOUND", "Contact with ID $rawContactId not found")
        }
      } catch (e: Exception) {
        promise.reject("GET_CONTACT_ERROR", "Failed to fetch contact: ${e.localizedMessage}", e)
      }
    }
  }
  @ReactMethod
  fun createNewContact(contactMap: ReadableMap, promise: Promise){
    try {
      val contactHelper = ContactHelper(reactApplicationContext)
      val contact = contactHelper.readableMapToContact(contactMap)
      val success = contactHelper.insertContact(contact)

      if (success) {
        promise.resolve("Contact created successfully")
      } else {
        promise.reject("INSERT_FAILED", "Failed to insert contact")
      }
    } catch (e: Exception) {
      promise.reject("ERROR_CREATING_CONTACT", e.message, e)
    }
  }

  @ReactMethod
  fun updateContact(contactMap: ReadableMap, photoStatus: Int, promise: Promise) {
    try {
      val contactHelper = ContactHelper(reactApplicationContext)
      val contact = contactHelper.readableMapToContact(contactMap)

      val success = contactHelper.updateContact(contact,photoStatus)

      if (success) {
        promise.resolve("Contact updated successfully")
      } else {
        promise.reject("UPDATE_FAILED", "Failed to update contact")
      }
    } catch (e: Exception) {
      promise.reject("ERROR_UPDATING_CONTACT", e.message, e)
    }
  }

  @ReactMethod
  fun deleteContact(contact: ReadableMap, promise: Promise) {
    ensureBackgroundThread {
      try {
        val contactHelper = ContactHelper(reactApplicationContext)
        val parsedContact = contactHelper.readableMapToContact(contact)
        contactHelper.deleteContact(parsedContact) { success ->
          if (success) {
            promise.resolve("Contact deleted successfully")
          } else {
            promise.reject("DELETE_FAILED", "Failed to delete contact")
          }
        }
      } catch (e: Exception) {
        promise.reject("UNEXPECTED_ERROR", "An error occurred while deleting contact: ${e.localizedMessage}", e)
      }
    }
  }
  @ReactMethod
  fun isNumberBlocked(phoneNumber: String, promise: Promise) {
    try {
      val dataStore = CallSettingDataStore(reactApplicationContext)
      CoroutineScope(Dispatchers.IO).launch {
        try {
          val isBlocked = dataStore.isNumberBlocked(phoneNumber)
          withContext(Dispatchers.Main) { promise.resolve(isBlocked) }
        } catch (e: Exception) {
          withContext(Dispatchers.Main) { promise.reject("BLOCK_CHECK_ERROR", "Failed to check if number is blocked: ${e.message}") }
        }
      }
    } catch (e: Exception) {
      promise.reject("BLOCK_CHECK_ERROR", "Failed to check if number is blocked: ${e.message}")
    }
  }
  @ReactMethod
  fun getBlockedNumbers(promise: Promise) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val dataStore = CallSettingDataStore(reactApplicationContext.applicationContext)
        val blocked = dataStore.blockedNumbers.first()
        promise.resolve(Arguments.fromList(blocked.toList()))
      } catch (e: Exception) {
        promise.reject("GET_BLOCKED_FAILED", e)
      }
    }
  }

  @ReactMethod
  fun addBlockedNumber(number: String, promise: Promise) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val dataStore = CallSettingDataStore(reactApplicationContext)
        dataStore.addBlockedNumber(number)
        promise.resolve("Number $number successfully added to block list.")
      } catch (e: Exception) {
        promise.reject("ADD_BLOCKED_FAILED", e)
      }
    }
  }

  @ReactMethod
  fun removeBlockedNumber(number: String, promise: Promise) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val dataStore = CallSettingDataStore(reactApplicationContext)
        dataStore.removeBlockedNumber(number)
        promise.resolve("Number $number successfully removed from block list.")
      } catch (e: Exception) {
        promise.reject("REMOVE_BLOCKED_FAILED", e)
      }
    }
  }

  @ReactMethod
  fun toggleShowBlockNotification(promise: Promise) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val dataStore = CallSettingDataStore(reactApplicationContext)
        val current = dataStore.showBlockNotificationStatus
        dataStore.toggleShowBlockNotification()
        promise.resolve("Show block notification toggled to ${!current.first()}")
      } catch (e: Exception) {
        promise.reject("TOGGLE_NOTIFICATION_FAILED", e)
      }
    }
  }
  @ReactMethod
  fun getBlockNotificationStatus(promise: Promise) {
    try {
      val dataStore = CallSettingDataStore(reactApplicationContext)
      CoroutineScope(Dispatchers.IO).launch {
        try {
          val status = dataStore.showBlockNotificationStatus.first()
          withContext(Dispatchers.Main) { promise.resolve(status) }
        } catch (e: Exception) {
          withContext(Dispatchers.Main) {
            promise.reject("FETCH_NOTIFICATION_STATUS_FAILED", "Failed to get block notification status: ${e.message}")
          }
        }
      }
    } catch (e: Exception) {
      promise.reject("FETCH_NOTIFICATION_STATUS_FAILED", "Failed to get block notification status: ${e.message}")
    }
  }
  companion object{
    const val NAME = "DialPadHelper"
  }

}
