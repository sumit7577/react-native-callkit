package com.callkit

import android.telecom.Call
import com.callkit.models.CallContact
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.callkit.helpers.getCallContact
import kotlinx.coroutines.flow.MutableStateFlow

object CallEventRepository {
  private var reactContext: ReactApplicationContext?= null

  fun initialize(context: ReactApplicationContext) {
    reactContext = context
  }

  fun emitEvent(eventName: String, eventData: Any?) {
    reactContext?.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      ?.emit(eventName, eventData)
  }

  fun emitMissedCallEvent(call: Call,callerNumber:String){
    val contactInfo = MutableStateFlow<CallContact?>(null)
    reactContext?.let { it1 -> getCallContact(it1,call, callerNumber) { contactInfo.value = it } }
    val data = Arguments.createMap().apply {
      putString("number",callerNumber)
      putString("name",contactInfo.value?.name?:"")
    }
    reactContext?.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      ?.emit("MissedCall",data)
  }

  fun emitRejectedCallEvent(call: Call,callerNumber:String){
    val contactInfo = MutableStateFlow<CallContact?>(null)
    reactContext?.let { it1 -> getCallContact(it1,call, callerNumber) { contactInfo.value = it } }
    val data = Arguments.createMap().apply {
      putString("number",callerNumber)
      putString("name",contactInfo.value?.name?:"")
    }
    reactContext?.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      ?.emit("RejectedCall",data)
  }

  fun isInitialized(): Boolean {
    return reactContext != null
  }

  fun getContext(): ReactApplicationContext?{
    return reactContext
  }
}

