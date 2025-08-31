package com.callkit.callManager

import android.media.AudioManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class CallViewModel(private val callRepo: CallRepository,
                    private val audioManager: AudioManager):ViewModel()
{
  private val _call = MutableStateFlow(callRepo.getCall());
  val phoneState: StateFlow<PhoneState> = callRepo.phoneState
  private val TAG = "CallViewModel"
  val call: StateFlow<Call?>
    get() = _call


  private val _callState = MutableStateFlow(_call.value?.state!!)
  val callState: StateFlow<Int>
    get() = _callState

  private val _isSpeakerOn = MutableStateFlow(false)
  val isSpeakerOn: StateFlow<Boolean> get() = _isSpeakerOn

  private val _isMuted = MutableStateFlow(false)
  val isMuted: StateFlow<Boolean> get() = _isMuted

  private val _isHold = MutableStateFlow(false)
  val isHold: StateFlow<Boolean> get() = _isHold

  val currentAudioState :StateFlow<CallAudioState?> = (callRepo.audioState)

  private val callback = object : Call.Callback() {
    override fun onStateChanged(call: Call, newState: Int) {
      _callState.value = newState
      //Log.d("call view model",newState.toString())
    }
    override fun onDetailsChanged(call: Call, details: Call.Details) {
      //Log.d("call view model","deatail chmhe")
    }

    override fun onConferenceableCallsChanged(call: Call, conferenceableCalls: MutableList<Call>) {
      Log.d("view model conference",conferenceableCalls.size.toString())
    }
  }

  init {
    call.value?.registerCallback(callback)
    setAudioModeForCall()
  }

  fun toggleSpeaker() {
    val newSpeakerState = !_isSpeakerOn.value
    val isBluetoothAvailable = currentAudioState.value?.supportedBluetoothDevices?.isNotEmpty() ?:false
    if(newSpeakerState){
      if(isBluetoothAvailable){
        callRepo.setAudioRoute(CallAudioState.ROUTE_BLUETOOTH)
      }
      else{
        callRepo.setAudioRoute(CallAudioState.ROUTE_SPEAKER)
      }
    }
    else{
      callRepo.setAudioRoute(CallAudioState.ROUTE_EARPIECE)
    }
    _isSpeakerOn.value = newSpeakerState
  }

  fun toogleHold(){
    _isHold.value = callRepo.toggleHold()
  }

  fun toggleSwap(){
    callRepo.swap()
  }

  fun toggleMerge(){
    callRepo.merge()
  }

  fun toggleMute() {
    val newMuteState = !_isMuted.value
    audioManager.isMicrophoneMute = newMuteState
    _isMuted.value = newMuteState
  }


  fun answerCall() {
    _call.value?.answer(VideoProfile.STATE_AUDIO_ONLY)
  }

  fun rejectCall() {
    callRepo.getPrimaryCall()?.disconnect()
    resetAudioMode()
  }

  private fun setAudioModeForCall() {
    audioManager.mode = AudioManager.MODE_IN_CALL
  }

  private fun resetAudioMode() {
    audioManager.mode = AudioManager.MODE_NORMAL
  }

  override fun onCleared() {
    super.onCleared()
    resetAudioMode()
    _call.value?.unregisterCallback(callback)
  }

}
