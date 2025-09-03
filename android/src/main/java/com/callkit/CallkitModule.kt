package com.callkit

import android.os.Build
import androidx.annotation.RequiresApi
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.module.annotations.ReactModule

@ReactModule(name = CallkitModule.NAME)
class CallkitModule(reactContext: ReactApplicationContext) :
  NativeCallkitSpec(reactContext) {

  private val callKitHelper = CallKitHelper(reactContext)

  override fun getName(): String {
    return NAME
  }

  @RequiresApi(Build.VERSION_CODES.Q)
  override fun requestRole(promise: Promise) {
    callKitHelper.requestRole(promise,reactApplicationContext,reactApplicationContext.currentActivity)
  }

  override fun makeCall(phoneNumber: String, promise: Promise) {
    callKitHelper.makeCall(phoneNumber,promise,reactApplicationContext)
  }

  override fun toggleVibration(value: Boolean, promise: Promise?) {
    TODO("Not yet implemented")
  }

  override fun getVibrationStatus(promise: Promise?) {
    TODO("Not yet implemented")
  }

  override fun forwardAllCalls(cfi: Boolean, phoneNumber: String?, promise: Promise?) {
    TODO("Not yet implemented")
  }

  override fun getReplies(promise: Promise?) {
    TODO("Not yet implemented")
  }

  override fun saveReplies(replies: String?, promise: Promise?) {
    TODO("Not yet implemented")
  }

  override fun getAllContacts(promise: Promise) {
    callKitHelper.getAllContacts(promise,reactApplicationContext)
  }

  override fun createNewContact(contact: ReadableMap, promise: Promise) {
    callKitHelper.createNewContact(contact,promise,reactApplicationContext)
  }

  override fun updateContact(contact: ReadableMap, photoStatus: Double, promise: Promise) {
    callKitHelper.updateContact(contact,photoStatus.toInt(),promise,reactApplicationContext)
  }

  override fun deleteContact(contact: ReadableMap, promise: Promise) {
    callKitHelper.deleteContact(contact,promise,reactApplicationContext)
  }

  override fun getBlockedNumbers(promise: Promise?) {
    TODO("Not yet implemented")
  }

  override fun addBlockedNumber(phoneNumber: String?, promise: Promise?) {
    TODO("Not yet implemented")
  }

  override fun removeBlockedNumber(phoneNumber: String?, promise: Promise?) {
    TODO("Not yet implemented")
  }

  companion object {
    const val NAME = "Callkit"
  }
}
