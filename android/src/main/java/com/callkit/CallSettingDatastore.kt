package com.callkit


import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// DataStore Instance
val Context.dataStore by preferencesDataStore(name = "call_settings")

class CallSettingDataStore(private val context: Context) {

  companion object {
    private val SECURE_NUMBER = booleanPreferencesKey("secure_number")
    private val VIBRATION_SETTING = booleanPreferencesKey("vibration")
    private val REPLIES_KEY = stringSetPreferencesKey("quick_replies")
    private val BLOCKED_NUMBERS_KEY = stringSetPreferencesKey("blocked_numbers")
    private val SHOW_BLOCK_NOTIFICATION = booleanPreferencesKey("show_block_notification")
  }

  // Flows for settings
  val secureNumberStatus: Flow<Boolean> = context.dataStore.data
    .map { preferences ->
      preferences[SECURE_NUMBER] ?: false
    }

  val vibrationStatus: Flow<Boolean> = context.dataStore.data.map { preferences ->
    preferences[VIBRATION_SETTING] ?: false
  }

  val showBlockNotificationStatus: Flow<Boolean> = context.dataStore.data
    .map { preferences ->
      preferences[SHOW_BLOCK_NOTIFICATION] ?: true
    }

  suspend fun toggleShowBlockNotification() {
    context.dataStore.edit { preferences ->
      val current = preferences[SHOW_BLOCK_NOTIFICATION] ?: true
      preferences[SHOW_BLOCK_NOTIFICATION] = !current
    }
  }

  val repliesFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
    prefs[REPLIES_KEY]?.toList() ?: listOf(
      "Can't talk now",
      "Can I call you later?",
      "I'm busy, I'll call you back",
      "Custom"
    )
  }

  val blockedNumbers: Flow<Set<String>> = context.dataStore.data.map { prefs ->
    prefs[BLOCKED_NUMBERS_KEY] ?: emptySet()
  }

  // Update methods for settings
  suspend fun updateVibrationSetting(vibrate: Boolean) {
    context.dataStore.edit { preferences ->
      preferences[VIBRATION_SETTING] = vibrate
    }
  }

  suspend fun updateCallSettings(hideCall: Boolean, updateCall: Boolean) {
    context.dataStore.edit { preferences ->
      preferences[SECURE_NUMBER] = hideCall
    }
  }

  suspend fun saveReply(context: Context, newReply: String) {
    context.dataStore.edit { prefs ->
      val existingReplies = prefs[REPLIES_KEY]?.toMutableSet() ?: mutableSetOf(
        "Can't talk now",
        "Can I call you later?",
        "I'm busy, I'll call you back",
        "Custom"
      )
      existingReplies.add(newReply)
      prefs[REPLIES_KEY] = existingReplies
    }
  }

  suspend fun updateReplies(context: Context, replies: List<String>) {
    context.dataStore.edit { preferences ->
      preferences[REPLIES_KEY] = replies.toSet()
    }
  }

  suspend fun deleteReply(reply: String) {
    context.dataStore.edit { prefs ->
      val existingReplies = prefs[REPLIES_KEY]?.toMutableSet() ?: mutableSetOf()
      existingReplies.remove(reply)
      prefs[REPLIES_KEY] = existingReplies
    }
  }

  // Blocked number operations (managed entirely in DataStore)
  suspend fun addBlockedNumber(number: String) {
    val normalizedNumber = normalizePhoneNumber(number)
    context.dataStore.edit { prefs ->
      val existing = prefs[BLOCKED_NUMBERS_KEY]?.toMutableSet() ?: mutableSetOf()
      existing.add(normalizedNumber)
      prefs[BLOCKED_NUMBERS_KEY] = existing
    }
  }

  suspend fun removeBlockedNumber(number: String) {
    val normalizedNumber = normalizePhoneNumber(number)
    context.dataStore.edit { prefs ->
      val existing = prefs[BLOCKED_NUMBERS_KEY]?.toMutableSet() ?: mutableSetOf()
      existing.remove(normalizedNumber)
      prefs[BLOCKED_NUMBERS_KEY] = existing
    }
  }

  suspend fun isNumberBlocked(number: String): Boolean {
    val normalizedNumber = normalizePhoneNumber(number)
    return blockedNumbers.map { it.contains(normalizedNumber) }.first()
  }

  // Helper method to normalize phone numbers
  private fun normalizePhoneNumber(number: String): String {
    // Remove all non-digit characters and leading/trailing whitespace
    return number.replace(Regex("[^0-9]"), "").trim()
  }
}
