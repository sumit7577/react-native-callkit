package com.callkit.models

import kotlinx.serialization.Serializable

@Serializable
data class IM(
  var value: String,
  var type: Int,
  var label: String
)
