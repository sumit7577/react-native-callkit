package com.callkit.helpers

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.facebook.react.bridge.Promise

fun getSavedSelectedSimId(context: Context): Int {
    return -1 // TODO: Implement logic to retrieve saved SIM ID
}

@RequiresApi(Build.VERSION_CODES.O)
fun setCallForwarding(context: Context, cfi: Boolean, phoneNumber: String, countryCode: String?,  subscriptionId: Int,promise: Promise) {
    val TAG = "CallForwarding"
    Log.v(TAG, "setCallForwarding enabled=$cfi, phoneNumber=$phoneNumber, countryCode=$countryCode")

    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    var selectedSubId = subscriptionId

    // Fallback to default subscription ID
    if (selectedSubId <= 0) {
        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        selectedSubId = SubscriptionManager.getDefaultSubscriptionId()
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            val activeSubscriptions = subscriptionManager.activeSubscriptionInfoList
            if (!activeSubscriptions.isNullOrEmpty()) {
                selectedSubId = activeSubscriptions[0].subscriptionId
                Log.v(TAG, "Selected subscription ID: $selectedSubId")
            } else {
                promise.reject("NO_ACTIVE_SUBSCRIPTIONS", "No active SIM subscriptions found")
                return
            }
        } else {
            promise.reject("PERMISSION_DENIED", "READ_PHONE_STATE permission not granted")
            return
        }
    }

    // Detect carrier
    val carrierName = if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
        telephonyManager.simOperatorName ?: "Unknown"
    } else {
        "Unknown"
    }
    Log.v(TAG, "Carrier: $carrierName")

    // Sanitize and format phone number
    val sanitizedNumber = phoneNumber.replace("[^0-9+]".toRegex(), "")
    if (cfi && sanitizedNumber.isEmpty()) {
        promise.reject("INVALID_PHONE_NUMBER", "Phone number cannot be empty")
        return
    }
    val formattedNumber = if (sanitizedNumber.startsWith("+")) {
        sanitizedNumber
    } else {
        // Use provided country code or fallback to SIM's country code
        val simCountryIso = if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            telephonyManager.simCountryIso?.uppercase() ?: "IN"
        } else {
            "IN"
        }
        val defaultCountryCode = when (simCountryIso) {
            "IN" -> "+91"
            "US" -> "+1"
            "AU" -> "+61"
            "GB" -> "+44"
            else -> "+1"
        }
        (countryCode ?: defaultCountryCode) + sanitizedNumber
    }

    // Select USSD code based on carrier
    val ussdRequest = when (carrierName) {
        "Verizon" -> if (cfi) "*72$formattedNumber" else "*73"
        "Sprint" -> if (cfi) "*72$formattedNumber" else "*73"
        else -> if (cfi) "*21*$formattedNumber#" else "#21#" // Default GSM
    }
    Log.v(TAG, "USSD request: $ussdRequest")

    // Check CALL_PHONE permission
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
        promise.reject("PERMISSION_DENIED", "CALL_PHONE permission not granted")
        return
    }

    // Send USSD request
    val managerForSim = telephonyManager.createForSubscriptionId(selectedSubId)
    val handler = Handler(Looper.getMainLooper())
    val responseCallback = object : TelephonyManager.UssdResponseCallback() {
        override fun onReceiveUssdResponse(telephonyManager: TelephonyManager, request: String, response: CharSequence) {
            Log.v(TAG, "USSD response: $response")
            handler.post {
                Toast.makeText(context, response.toString(), Toast.LENGTH_SHORT).show()
            }
            promise.resolve(response.toString())
        }

        override fun onReceiveUssdResponseFailed(telephonyManager: TelephonyManager, request: String, failureCode: Int) {
            Log.e(TAG, "USSD request failed: $request, failureCode=$failureCode")
            handler.post {
                Toast.makeText(context, "USSD request failed. Try phone settings or contact carrier.", Toast.LENGTH_LONG).show()
            }
            // Fallback to dialer
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(ussdRequest)}"))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            try {
                context.startActivity(intent)
                promise.resolve("USSD request sent via dialer")
            } catch (e: Exception) {
                promise.reject("USSD_ERROR", "USSD and dialer failed: $failureCode")
            }
        }
    }

    // Check call forwarding status first
    val statusUssdRequest = "*#21#"
    managerForSim.sendUssdRequest(statusUssdRequest, object : TelephonyManager.UssdResponseCallback() {
        override fun onReceiveUssdResponse(telephonyManager: TelephonyManager, request: String, response: CharSequence) {
            Log.v(TAG, "Call forwarding status: $response")
            // Proceed with enabling/disabling
            try {
                managerForSim.sendUssdRequest(ussdRequest, responseCallback, handler)
                Log.v(TAG, "USSD request sent successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending USSD request", e)
                responseCallback.onReceiveUssdResponseFailed(telephonyManager, ussdRequest, -1)
            }
        }

        override fun onReceiveUssdResponseFailed(telephonyManager: TelephonyManager, request: String, failureCode: Int) {
            Log.e(TAG, "Failed to query call forwarding status: $failureCode")
            responseCallback.onReceiveUssdResponseFailed(telephonyManager, ussdRequest, failureCode)
        }
    }, handler)
}
