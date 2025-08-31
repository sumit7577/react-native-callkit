package com.callkit

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.callkit.helpers.CallLogObserver

class CallMonitorService : Service() {

  private var callLogObserver: CallLogObserver? = null

  @RequiresPermission(Manifest.permission.READ_CALL_LOG)
  override fun onCreate() {
    super.onCreate()
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
      == PackageManager.PERMISSION_GRANTED) {
      val callSettingDataStore = CallSettingDataStore(this)
      callLogObserver = CallLogObserver(this, callSettingDataStore).apply {
        contentResolver.registerContentObserver(
          android.provider.CallLog.Calls.CONTENT_URI,
          true,
          this
        )
      }
      Log.d("CallMonitorService", "CallLogObserver registered")
    } else {
      Log.e("CallMonitorService", "READ_CALL_LOG permission not granted")
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    callLogObserver?.let {
      contentResolver.unregisterContentObserver(it)
      Log.d("CallMonitorService", "CallLogObserver unregistered")
    }
  }

  override fun onBind(intent: Intent?): IBinder? {
    return null
  }
}
