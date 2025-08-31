package com.callkit.callManager

import android.telecom.Call

fun Int.asString(): String = when (this) {
  Call.STATE_NEW -> "INCOMING CALL"
  Call.STATE_RINGING -> "INCOMING CALL"
  Call.STATE_DIALING -> "DIALING"
  Call.STATE_ACTIVE -> "ACTIVE"
  Call.STATE_HOLDING -> "HOLDING"
  Call.STATE_DISCONNECTED -> "DISCONNECTED"
  Call.STATE_CONNECTING -> "CONNECTING"
  Call.STATE_DISCONNECTING -> "DISCONNECTING"
  Call.STATE_SELECT_PHONE_ACCOUNT -> "SELECT_PHONE_ACCOUNT"
  else -> {
    "UNKNOWN"
  }
}


