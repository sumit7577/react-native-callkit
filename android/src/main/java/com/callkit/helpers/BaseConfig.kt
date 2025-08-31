package com.callkit.helpers

import android.content.Context
import com.callkit.extensions.getSharedPrefs

open class BaseConfig(val context: Context) {
  protected val prefs = context.getSharedPrefs()

  companion object {
    fun newInstance(context: Context) = BaseConfig(context)
  }

  var ignoredContactSources: HashSet<String>
    get() = prefs.getStringSet(IGNORED_CONTACT_SOURCES, hashSetOf(".")) as HashSet
    set(ignoreContactSources) = prefs.edit().remove(IGNORED_CONTACT_SOURCES).putStringSet(
      IGNORED_CONTACT_SOURCES, ignoreContactSources).apply()
}
