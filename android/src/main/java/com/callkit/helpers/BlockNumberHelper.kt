package com.callkit.helpers

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.BlockedNumberContract
import android.provider.BlockedNumberContract.BlockedNumbers

class BlockNumberHelper(private val context: Context) {

    // Add a number to the system's blocklist
    suspend fun addBlockedNumber(number: String) {
        val normalizedNumber = normalizePhoneNumber(number)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && canBlockNumbers()) {
            try {
                val values = ContentValues().apply {
                    put(BlockedNumbers.COLUMN_ORIGINAL_NUMBER, normalizedNumber)
                    // Optionally, add E164 number if you have it
                    // put(BlockedNumbers.COLUMN_E164_NUMBER, e164Number)
                }
                context.contentResolver.insert(BlockedNumbers.CONTENT_URI, values)
            } catch (e: SecurityException) {
                // Fallback: Prompt user to block in system dialer
                promptSystemBlockNumber(normalizedNumber)
            }
        } else {
            // Fallback for older APIs or no permission
            promptSystemBlockNumber(normalizedNumber)
        }
    }

    // Remove a number from the system's blocklist
    suspend fun removeBlockedNumber(number: String) {
        val normalizedNumber = normalizePhoneNumber(number)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && canBlockNumbers()) {
            try {
                BlockedNumberContract.unblock(context, normalizedNumber)
            } catch (e: SecurityException) {
                // Fallback: Prompt user to unblock in system dialer
                promptSystemUnblockNumber(normalizedNumber)
            }
        } else {
            // Fallback for older APIs or no permission
            promptSystemUnblockNumber(normalizedNumber)
        }
    }

    // Check if a number is blocked in the system blocklist
    suspend fun isNumberBlocked(number: String): Boolean {
        val normalizedNumber = normalizePhoneNumber(number)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && canBlockNumbers()) {
            try {
                BlockedNumberContract.isBlocked(context, normalizedNumber)
            } catch (e: SecurityException) {
                false // Fallback: Assume not blocked if permission is denied
            }
        } else {
            false // Fallback: No system blocklist access on older APIs
        }
    }

    // Check if the app can block numbers
    private fun canBlockNumbers(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            BlockedNumberContract.canCurrentUserBlockNumbers(context)
        } else {
            false
        }
    }

    // Helper function to normalize phone numbers
    fun normalizePhoneNumber(number: String): String {
        return number.replace("[^0-9+]".toRegex(), "")
    }

    // Prompt user to block a number in the system dialer (fallback)
    private fun promptSystemBlockNumber(number: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("tel:$number")
            putExtra("com.android.contacts.extra.BLOCK_NUMBER", true)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle case where system dialer doesn't support the intent
            // Optionally show a toast or log the error
        }
    }

    // Prompt user to unblock a number in the system dialer (fallback)
    private fun promptSystemUnblockNumber(number: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("tel:$number")
            putExtra("com.android.contacts.extra.UNBLOCK_NUMBER", true)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle case where system dialer doesn't support the intent
        }
    }
}
