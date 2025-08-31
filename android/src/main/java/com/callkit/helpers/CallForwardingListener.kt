package com.callkit.helpers

import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyCallback
import android.util.Log
import androidx.annotation.RequiresApi


@RequiresApi(api = Build.VERSION_CODES.S)
class CallForwardingListener(private val context: Context) : TelephonyCallback(), TelephonyCallback.CallForwardingIndicatorListener {

  private val TAG: String = "CallForwardingListener"

  override fun onCallForwardingIndicatorChanged(callForwardingIndicator: Boolean) {
    Log.i(TAG, "onCallForwardingIndicatorChanged - New CFI: $callForwardingIndicator")
    //PhoneStateService.currentState = callForwardingIndicator
    sendWidgetUpdateBroadcast(callForwardingIndicator)
  }

  private fun sendWidgetUpdateBroadcast(callForwardingIndicator: Boolean) {
    /*val intent = Intent(context, ForwardingStatusWidget::class.java)
    intent.setAction("de.kaiserdragon.callforwardingstatus.APPWIDGET_UPDATE_CFI")
    // Add the CFI value as an extra
    intent.putExtra("cfi", callForwardingIndicator)
    // Send the broadcast
    context.sendBroadcast(intent)*/
  }

}
