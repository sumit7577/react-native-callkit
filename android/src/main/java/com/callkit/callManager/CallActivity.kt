package com.callkit.callManager

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.callkit.CallMonitorService
import com.callkit.helpers.isOreoMr1Plus
import com.callkit.helpers.isOreoPlus


class CallActivity : AppCompatActivity() {
  private lateinit var callViewModel: CallViewModel
  private var screenOnWakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
       startService(Intent(this, CallMonitorService::class.java))
      window.setFlags(
        WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN
      )
      addLockScreenFlags()
      val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
      val currentCall = CallRepository.getCall()
      if (currentCall != null) {
        callViewModel = ViewModelProvider(this, CallViewModelFactory(
          CallRepository,
          audioManager)
        )[CallViewModel::class.java]
      } else {
        finish()
        return
      }
      handleIntent(intent)
      setContent {
        MaterialTheme {
          MainCallHandler(callViewModel=callViewModel,onReject={
            callViewModel.rejectCall()
            finish()
          },onAddNewCall= {finish()})
        }
      }
    }

  @SuppressLint("NewApi")
  private fun addLockScreenFlags() {
    if (isOreoMr1Plus()) {
      setShowWhenLocked(true)
      setTurnScreenOn(true)
    } else {
      window.addFlags(
        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
          or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
          or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
          or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
          or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
      )
    }

    if (isOreoPlus()) {
      (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).requestDismissKeyguard(this, null)
    } else {
      window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
    }

    try {
      val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
      screenOnWakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "com.callkit:full_wake_lock")
      screenOnWakeLock!!.acquire(5 * 1000L)
    } catch (e: Exception) {
    }
  }


  private fun handleIntent(intent: Intent?) {
    intent?.action?.let { action ->
      when (action) {
        "REJECT_CALL" -> {
          callViewModel.rejectCall()
        }
        "ANSWER_CALL" -> {
          callViewModel.answerCall()
        }
        else -> {
          // Handle other actions or no action
        }
      }
    }
  }
}


class CallViewModelFactory(private val callRepo: CallRepository,
                           private val audioManager: AudioManager) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(CallViewModel::class.java)) {
      @Suppress("UNCHECKED_CAST")
      return CallViewModel(callRepo,audioManager) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
