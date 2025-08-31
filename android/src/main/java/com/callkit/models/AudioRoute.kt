package com.callkit.models

import android.telecom.CallAudioState

enum class AudioRoute(val route: Int) {
  SPEAKER(CallAudioState.ROUTE_SPEAKER),
  EARPIECE(CallAudioState.ROUTE_EARPIECE),
  BLUETOOTH(CallAudioState.ROUTE_BLUETOOTH),
  WIRED_HEADSET(CallAudioState.ROUTE_WIRED_HEADSET),
  WIRED_OR_EARPIECE(CallAudioState.ROUTE_WIRED_OR_EARPIECE);

  companion object {
    fun fromRoute(route: Int?) = entries.firstOrNull { it.route == route }
  }
}
