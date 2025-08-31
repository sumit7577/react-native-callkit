package com.callkit.extensions

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.provider.ContactsContract
import java.io.ByteArrayOutputStream

fun Context.getPhotoThumbnailSize(): Int {
  val uri = ContactsContract.DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI
  val projection = arrayOf(ContactsContract.DisplayPhoto.THUMBNAIL_MAX_DIM)
  var cursor: Cursor? = null
  try {
    cursor = contentResolver.query(uri, projection, null, null, null)
    if (cursor?.moveToFirst() == true) {
      return cursor.getIntValue(ContactsContract.DisplayPhoto.THUMBNAIL_MAX_DIM)
    }
  } catch (ignored: Exception) {
  } finally {
    cursor?.close()
  }
  return 0
}

fun Bitmap.getByteArray(): ByteArray {
  var baos: ByteArrayOutputStream? = null
  try {
    baos = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 80, baos)
    return baos.toByteArray()
  } finally {
    baos?.close()
  }
}
