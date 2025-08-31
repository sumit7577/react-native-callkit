package com.callkit.callManager

import android.annotation.SuppressLint
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.callkit.extensions.getStateCompat
import com.callkit.extensions.hasCapability
import com.callkit.extensions.isConference
import com.callkit.models.AudioRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.CopyOnWriteArraySet


@SuppressLint("StaticFieldLeak")
object CallRepository {
  private var call: Call? = null
  private val calls: MutableList<Call> = mutableListOf()
  private var inCallService: InCallService? = null
  private val listeners = CopyOnWriteArraySet<CallManagerListener>()
  private val _phoneState = MutableStateFlow<PhoneState>(NoCall)
  val phoneState: StateFlow<PhoneState> get() = _phoneState

  //For tracking Audio State
  private val _audioState = MutableStateFlow<CallAudioState?>(null)
  val audioState: StateFlow<CallAudioState?> get() = _audioState

  fun onCallAdded(call: Call) {
    CallRepository.call = call
    calls.add(call)
    _phoneState.value = getPhoneState()
    for (listener in listeners) {
      listener.onPrimaryCallChanged(call)
    }
    call.registerCallback(object : Call.Callback() {
      override fun onStateChanged(call: Call, state: Int) {
        updateState()
      }

      override fun onDetailsChanged(call: Call, details: Call.Details) {
        updateState()
      }

      override fun onConferenceableCallsChanged(call: Call, conferenceableCalls: MutableList<Call>) {
        updateState()
        println("conference changed ${call.toString()} ${conferenceableCalls.size}")
      }
    })
  }

  fun onAudioStateChanged(audioState: CallAudioState) {
    _audioState.value = audioState
    //Not Necessary
    /*for (listener in listeners) {
      listener.onAudioStateChanged(route)
    }*/
  }

  fun getPhoneState(): PhoneState {
    return when (calls.size) {
      0 -> NoCall
      1 -> SingleCall(calls.first())
      2 -> {
        val active = calls.find { it.getStateCompat() == Call.STATE_ACTIVE }
        val newCall = calls.find { it.getStateCompat() == Call.STATE_CONNECTING || it.getStateCompat() == Call.STATE_DIALING }
        val onHold = calls.find { it.getStateCompat() == Call.STATE_HOLDING }
        if (active != null && newCall != null) {
          TwoCalls(newCall, active)
        } else if (newCall != null && onHold != null) {
          TwoCalls(newCall, onHold)
        } else if (active != null && onHold != null) {
          TwoCalls(active, onHold)
        } else {
          TwoCalls(calls[0], calls[1])
        }
      }
      else -> {
        val conference = calls.find { it.isConference() } ?: return NoCall
        val secondCall = if (conference.children.size + 1 != calls.size) {
          calls.filter { !it.isConference() }
            .subtract(conference.children.toSet())
            .firstOrNull()
        } else {
          null
        }
        if (secondCall == null) {
          SingleCall(conference)
        } else {
          val newCallState = secondCall.getStateCompat()
          if (newCallState == Call.STATE_ACTIVE || newCallState == Call.STATE_CONNECTING || newCallState == Call.STATE_DIALING) {
            TwoCalls(secondCall, conference)
          } else {
            TwoCalls(conference, secondCall)
          }
        }
      }
    }
  }

  private fun updateState() {
    val phoneState = getPhoneState()
    _phoneState.value = phoneState
    val primaryCall = when (phoneState) {
      is NoCall -> null
      is SingleCall -> phoneState.call
      is TwoCalls -> phoneState.active
    }
    var notify = true
    if (primaryCall == null) {
      call = null
    } else if (primaryCall != call) {
      call = primaryCall
      for (listener in listeners) {
        listener.onPrimaryCallChanged(primaryCall)
      }
      notify = false
    }
    if (notify) {
      for (listener in listeners) {
        listener.onStateChanged()
      }
    }
    // remove all disconnected calls manually in case they are still here
    calls.removeAll { it.getStateCompat() == Call.STATE_DISCONNECTED }
  }

  fun getCall(): Call? {
    return call
  }

  fun getAllCalls():MutableList<Call>{
    return calls
  }

  fun setInCallService(callService:InCallService){
    inCallService = callService
  }

  fun onCallRemoved(call: Call) {
    calls.remove(call)
    updateState()
  }

  fun getPrimaryCall(): Call? {
    return call
  }

  private fun getCallAudioState() = inCallService?.callAudioState

  fun getSupportedAudioRoutes(): Array<AudioRoute> {
    return AudioRoute.entries.filter {
      val supportedRouteMask = getCallAudioState()?.supportedRouteMask
      if (supportedRouteMask != null) {
        supportedRouteMask and it.route == it.route
      } else {
        false
      }
    }.toTypedArray()
  }

  fun toggleHold(): Boolean {
    val isOnHold = getState() == Call.STATE_HOLDING
    if (isOnHold) {
      call?.unhold()
    } else {
      call?.hold()
    }
    return !isOnHold
  }

  fun swap() {
    if (calls.size > 1) {
      calls.find { it.getStateCompat() == Call.STATE_HOLDING }?.unhold()
    }
  }

  fun merge() {
    val conferenceableCalls = call!!.conferenceableCalls
    if (conferenceableCalls.isNotEmpty()) {
      call!!.conference(conferenceableCalls.first())
    } else {
      if (call!!.hasCapability(Call.Details.CAPABILITY_MERGE_CONFERENCE)) {
        call!!.mergeConference()
      }
    }
  }

  fun addListener(listener: CallManagerListener) {
    listeners.add(listener)
  }

  fun removeListener(listener: CallManagerListener) {
    listeners.remove(listener)
  }

  fun getCallAudioRoute() = AudioRoute.fromRoute(getCallAudioState()?.route)

  fun setAudioRoute(newRoute: Int) {
    inCallService?.setAudioRoute(newRoute)
  }

  fun getConferenceCalls(): List<Call> {
    return calls.find { it.isConference() }?.children ?: emptyList()
  }

  fun getState() = getPrimaryCall()?.getStateCompat()

}


interface CallManagerListener {
  fun onStateChanged()
  fun onAudioStateChanged(audioState: AudioRoute)
  fun onPrimaryCallChanged(call: Call)
}

sealed class PhoneState
object NoCall : PhoneState()
class SingleCall(val call: Call) : PhoneState()
class TwoCalls(val active: Call, val onHold: Call) : PhoneState()
