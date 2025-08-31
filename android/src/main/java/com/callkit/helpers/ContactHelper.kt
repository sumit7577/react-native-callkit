package com.callkit.helpers

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.callkit.models.Contact
import android.provider.ContactsContract
import android.provider.ContactsContract.*
import android.provider.MediaStore
import android.text.TextUtils
import com.facebook.react.bridge.ReadableMap
import com.callkit.extensions.getByteArray
import com.callkit.extensions.getIntValue
import com.callkit.extensions.getLongValue
import com.callkit.extensions.getPhotoThumbnailSize
import com.callkit.extensions.getStringValue
import com.callkit.extensions.queryCursor
import com.callkit.extensions.showErrorToast
import com.callkit.extensions.toast
import com.callkit.models.Address
import com.callkit.models.Email
import com.callkit.models.Event
import com.callkit.models.Group
import com.callkit.models.IM
import com.callkit.models.Organization
import com.callkit.models.PhoneNumber

class ContactHelper(val context: Context) {
  private val BATCH_SIZE = 50

  fun readableMapToContact(map: ReadableMap): Contact {
    val id = map.getInt("id")
    val prefix = map.getString("prefix") ?: ""
    val firstName = map.getString("firstName") ?: ""
    val middleName = map.getString("middleName") ?: ""
    val surname = map.getString("surname") ?: ""
    val suffix = map.getString("suffix") ?: ""
    val nickname = map.getString("nickname") ?: ""
    val photoUri = map.getString("photoUri") ?: ""
    val contactId = map.getInt("contactId")
    val thumbnailUri = map.getString("thumbnailUri") ?: ""
    val notes = map.getString("notes") ?: ""
    val source = map.getString("source") ?: ""
    //val starred = map.getInt("starred") ?:0
    val mimetype = map.getString("mimetype") ?: ""
    val ringtone = if (map.hasKey("ringtone") && !map.isNull("ringtone")) map.getString("ringtone") else null

    val phoneNumbers = arrayListOf<PhoneNumber>()
    if (map.hasKey("phoneNumbers") && map.getType("phoneNumbers").name == "Array") {
      val arr = map.getArray("phoneNumbers")
      if (arr != null) {
        for (i in 0 until arr.size()) {
          val phoneMap = arr.getMap(i)
          val value = phoneMap.getString("value") ?: ""
          val type = phoneMap.getInt("type")
          val label = phoneMap.getString("label") ?: ""
          val normalizedNumber = phoneMap.getString("normalizedNumber") ?: ""
          phoneNumbers.add(PhoneNumber(value, type, label,normalizedNumber))
        }
      }
    }

    val emails = arrayListOf<Email>()
    if (map.hasKey("emails") && map.getType("emails").name == "Array") {
      val arr = map.getArray("emails")
      if (arr != null) {
        for (i in 0 until arr.size()) {
          val emailMap = arr.getMap(i)
          val value = emailMap.getString("value") ?: ""
          val type = emailMap.getInt("type")
          val label = emailMap.getString("label") ?: ""
          emails.add(Email(value, type, label))
        }
      }
    }

    val addresses = arrayListOf<Address>()
    if (map.hasKey("addresses") && map.getType("addresses").name == "Array") {
      val arr = map.getArray("addresses")
      if (arr != null) {
        for (i in 0 until arr.size()) {
          val addressMap = arr.getMap(i)
          val value = addressMap.getString("value") ?: ""
          val type = addressMap.getInt("type")
          val label = addressMap.getString("label") ?: ""
          addresses.add(Address(value, type, label))
        }
      }
    }

    val events = arrayListOf<Event>()
    if (map.hasKey("events") && map.getType("events").name == "Array") {
      val arr = map.getArray("events")
      if (arr != null) {
        for (i in 0 until arr.size()) {
          val eventMap = arr.getMap(i)
          val value = eventMap.getString("value") ?: ""
          val type = eventMap.getInt("type")
          events.add(Event(value, type))
        }
      }
    }

    val groups = arrayListOf<Group>()
    if (map.hasKey("groups") && map.getType("groups").name == "Array") {
      val arr = map.getArray("groups")
      if (arr != null) {
        for (i in 0 until arr.size()) {
          val groupMap = arr.getMap(i)
          val groupId = groupMap.getInt("id")
          val title = groupMap.getString("title") ?: ""
          groups.add(Group(groupId.toLong(), title))
        }
      }
    }

    val organization = if (map.hasKey("organization") && map.getType("organization").name == "Map") {
      val orgMap = map.getMap("organization")
      if (orgMap != null) {
        val company = orgMap.getString("company") ?: ""
        val title = orgMap.getString("title") ?: ""
        Organization(company, title)
      } else Organization("", "")
    } else Organization("", "")

    val websites = arrayListOf<String>()
    if (map.hasKey("websites") && map.getType("websites").name == "Array") {
      val arr = map.getArray("websites")
      if (arr != null) {
        for (i in 0 until arr.size()) {
          val site = arr.getString(i)
          websites.add(site)
        }
      }
    }

    val IMs = arrayListOf<IM>()
    if (map.hasKey("IMs") && map.getType("IMs").name == "Array") {
      val arr = map.getArray("IMs")
      if (arr != null) {
        for (i in 0 until arr.size()) {
          val imMap = arr.getMap(i)
          val value = imMap.getString("value") ?: ""
          val type = imMap.getInt("type")
          val label = imMap.getString("label") ?: ""
          IMs.add(IM(value, type, label))
        }
      }
    }

    return Contact(
      id = id,
      prefix = prefix,
      firstName = firstName,
      middleName = middleName,
      surname = surname,
      suffix = suffix,
      nickname = nickname,
      photoUri = photoUri,
      phoneNumbers = phoneNumbers,
      emails = emails,
      addresses = addresses,
      events = events,
      source = source,
      starred = 0,
      contactId = contactId,
      thumbnailUri = thumbnailUri,
      photo = null, // Bitmap not handled here
      notes = notes,
      groups = groups,
      organization = organization,
      websites = websites,
      IMs = IMs,
      mimetype = mimetype,
      ringtone = ringtone
    )
  }


  private fun getContactSourceType(accountName: String) =  ""

  private fun getContactProjection() = arrayOf(
    Data.MIMETYPE,
    Data.CONTACT_ID,
    Data.RAW_CONTACT_ID,
    CommonDataKinds.StructuredName.PREFIX,
    CommonDataKinds.StructuredName.GIVEN_NAME,
    CommonDataKinds.StructuredName.MIDDLE_NAME,
    CommonDataKinds.StructuredName.FAMILY_NAME,
    CommonDataKinds.StructuredName.SUFFIX,
    CommonDataKinds.StructuredName.PHOTO_URI,
    CommonDataKinds.StructuredName.PHOTO_THUMBNAIL_URI,
    CommonDataKinds.StructuredName.STARRED,
    CommonDataKinds.StructuredName.CUSTOM_RINGTONE,
    RawContacts.ACCOUNT_NAME,
    RawContacts.ACCOUNT_TYPE
  )

  private fun addFullSizePhoto(contactId: Long, fullSizePhotoData: ByteArray) {
    val baseUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, contactId)
    val displayPhotoUri = Uri.withAppendedPath(baseUri, RawContacts.DisplayPhoto.CONTENT_DIRECTORY)
    val fileDescriptor = context.contentResolver.openAssetFileDescriptor(displayPhotoUri, "rw")
    val photoStream = fileDescriptor!!.createOutputStream()
    photoStream.write(fullSizePhotoData)
    photoStream.close()
    fileDescriptor.close()
  }

  private fun getRealContactId(id: Long): Int {
    val uri = Data.CONTENT_URI
    val projection = getContactProjection()
    val selection = "(${Data.MIMETYPE} = ? OR ${Data.MIMETYPE} = ?) AND ${Data.RAW_CONTACT_ID} = ?"
    val selectionArgs = arrayOf(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, CommonDataKinds.Organization.CONTENT_ITEM_TYPE, id.toString())

    val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
    cursor?.use {
      if (cursor.moveToFirst()) {
        return cursor.getIntValue(Data.CONTACT_ID)
      }
    }
    return 0
  }

  fun insertContact(contact: Contact): Boolean {
    try {
      val operations = ArrayList<ContentProviderOperation>()
      ContentProviderOperation.newInsert(RawContacts.CONTENT_URI).apply {
        withValue(RawContacts.ACCOUNT_NAME, contact.source)
        withValue(RawContacts.ACCOUNT_TYPE, getContactSourceType(contact.source))
        operations.add(build())
      }

      // names
      ContentProviderOperation.newInsert(Data.CONTENT_URI).apply {
        withValueBackReference(Data.RAW_CONTACT_ID, 0)
        withValue(Data.MIMETYPE, CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
        withValue(CommonDataKinds.StructuredName.PREFIX, contact.prefix)
        withValue(CommonDataKinds.StructuredName.GIVEN_NAME, contact.firstName)
        withValue(CommonDataKinds.StructuredName.MIDDLE_NAME, contact.middleName)
        withValue(CommonDataKinds.StructuredName.FAMILY_NAME, contact.surname)
        withValue(CommonDataKinds.StructuredName.SUFFIX, contact.suffix)
        operations.add(build())
      }

      // nickname
      ContentProviderOperation.newInsert(Data.CONTENT_URI).apply {
        withValueBackReference(Data.RAW_CONTACT_ID, 0)
        withValue(Data.MIMETYPE, CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
        withValue(CommonDataKinds.Nickname.NAME, contact.nickname)
        operations.add(build())
      }

      // phone numbers
      contact.phoneNumbers.forEach {
        ContentProviderOperation.newInsert(Data.CONTENT_URI).apply {
          withValueBackReference(Data.RAW_CONTACT_ID, 0)
          withValue(Data.MIMETYPE, CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
          withValue(CommonDataKinds.Phone.NUMBER, it.value)
          withValue(CommonDataKinds.Phone.NORMALIZED_NUMBER, it.normalizedNumber)
          withValue(CommonDataKinds.Phone.TYPE, it.type)
          withValue(CommonDataKinds.Phone.LABEL, it.label)
          withValue(CommonDataKinds.Phone.IS_PRIMARY, it.isPrimary)
          operations.add(build())
        }
      }

      // emails
      contact.emails.forEach {
        ContentProviderOperation.newInsert(Data.CONTENT_URI).apply {
          withValueBackReference(Data.RAW_CONTACT_ID, 0)
          withValue(Data.MIMETYPE, CommonDataKinds.Email.CONTENT_ITEM_TYPE)
          withValue(CommonDataKinds.Email.DATA, it.value)
          withValue(CommonDataKinds.Email.TYPE, it.type)
          withValue(CommonDataKinds.Email.LABEL, it.label)
          operations.add(build())
        }
      }

      // addresses
      contact.addresses.forEach {
        ContentProviderOperation.newInsert(Data.CONTENT_URI).apply {
          withValueBackReference(Data.RAW_CONTACT_ID, 0)
          withValue(Data.MIMETYPE, CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
          withValue(CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, it.value)
          withValue(CommonDataKinds.StructuredPostal.TYPE, it.type)
          withValue(CommonDataKinds.StructuredPostal.LABEL, it.label)
          operations.add(build())
        }
      }

      // IMs
      contact.IMs.forEach {
        ContentProviderOperation.newInsert(Data.CONTENT_URI).apply {
          withValueBackReference(Data.RAW_CONTACT_ID, 0)
          withValue(Data.MIMETYPE, CommonDataKinds.Im.CONTENT_ITEM_TYPE)
          withValue(CommonDataKinds.Im.DATA, it.value)
          withValue(CommonDataKinds.Im.PROTOCOL, it.type)
          withValue(CommonDataKinds.Im.CUSTOM_PROTOCOL, it.label)
          operations.add(build())
        }
      }

      // events
      contact.events.forEach {
        ContentProviderOperation.newInsert(Data.CONTENT_URI).apply {
          withValueBackReference(Data.RAW_CONTACT_ID, 0)
          withValue(Data.MIMETYPE, CommonDataKinds.Event.CONTENT_ITEM_TYPE)
          withValue(CommonDataKinds.Event.START_DATE, it.value)
          withValue(CommonDataKinds.Event.TYPE, it.type)
          operations.add(build())
        }
      }

      // notes
      ContentProviderOperation.newInsert(Data.CONTENT_URI).apply {
        withValueBackReference(Data.RAW_CONTACT_ID, 0)
        withValue(Data.MIMETYPE, CommonDataKinds.Note.CONTENT_ITEM_TYPE)
        withValue(CommonDataKinds.Note.NOTE, contact.notes)
        operations.add(build())
      }

      // organization
      if (contact.organization.isNotEmpty()) {
        ContentProviderOperation.newInsert(Data.CONTENT_URI).apply {
          withValueBackReference(Data.RAW_CONTACT_ID, 0)
          withValue(Data.MIMETYPE, CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
          withValue(CommonDataKinds.Organization.COMPANY, contact.organization.company)
          withValue(CommonDataKinds.Organization.TYPE, DEFAULT_ORGANIZATION_TYPE)
          withValue(CommonDataKinds.Organization.TITLE, contact.organization.jobPosition)
          withValue(CommonDataKinds.Organization.TYPE, DEFAULT_ORGANIZATION_TYPE)
          operations.add(build())
        }
      }

      // websites
      contact.websites.forEach {
        ContentProviderOperation.newInsert(Data.CONTENT_URI).apply {
          withValueBackReference(Data.RAW_CONTACT_ID, 0)
          withValue(Data.MIMETYPE, CommonDataKinds.Website.CONTENT_ITEM_TYPE)
          withValue(CommonDataKinds.Website.URL, it)
          withValue(CommonDataKinds.Website.TYPE, DEFAULT_WEBSITE_TYPE)
          operations.add(build())
        }
      }

      // groups
      contact.groups.forEach {
        ContentProviderOperation.newInsert(Data.CONTENT_URI).apply {
          withValueBackReference(Data.RAW_CONTACT_ID, 0)
          withValue(Data.MIMETYPE, CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
          withValue(CommonDataKinds.GroupMembership.GROUP_ROW_ID, it.id)
          operations.add(build())
        }
      }

      // photo (inspired by https://gist.github.com/slightfoot/5985900)
      var fullSizePhotoData: ByteArray? = null
      if (contact.photoUri.isNotEmpty()) {
        val photoUri = Uri.parse(contact.photoUri)
        fullSizePhotoData = context.contentResolver.openInputStream(photoUri)?.readBytes()
      }

      val results = context.contentResolver.applyBatch(AUTHORITY, operations)

      // storing contacts on some devices seems to be messed up and they move on Phone instead, or disappear completely
      // try storing a lighter contact version with this oldschool version too just so it wont disappear, future edits work well
      if (getContactSourceType(contact.source).contains(".sim")) {
        val simUri = Uri.parse("content://icc/adn")
        ContentValues().apply {
          put("number", contact.phoneNumbers.firstOrNull()?.value ?: "")
          put("tag", contact.getNameToDisplay())
          context.contentResolver.insert(simUri, this)
        }
      }

      // fullsize photo
      val rawId = ContentUris.parseId(results[0].uri!!)
      if (contact.photoUri.isNotEmpty() && fullSizePhotoData != null) {
        addFullSizePhoto(rawId, fullSizePhotoData)
      }

      // favorite, ringtone
      val userId = getRealContactId(rawId)
      if (userId != 0) {
        val uri = Uri.withAppendedPath(Contacts.CONTENT_URI, userId.toString())
        val contentValues = ContentValues(2)
        contentValues.put(Contacts.STARRED, contact.starred)
        contentValues.put(Contacts.CUSTOM_RINGTONE, contact.ringtone)
        context.contentResolver.update(uri, contentValues, null, null)
      }

      return true
    } catch (e: Exception) {
      e.printStackTrace()
      context.showErrorToast(e)
      return false
    }
  }
  // Helper function to validate RAW_CONTACT_ID
  private fun isRawContactIdValid(rawContactId: Int): Boolean {
    val cursor = context.contentResolver.query(
      ContactsContract.RawContacts.CONTENT_URI,
      arrayOf(ContactsContract.RawContacts._ID),
      "${ContactsContract.RawContacts._ID} = ?",
      arrayOf(rawContactId.toString()),
      null
    )
    val exists = cursor?.count ?: 0 > 0
    cursor?.close()
    return exists
  }
  private fun checkForDuplicateContact(contact: Contact): String? {
    contact.phoneNumbers.firstOrNull()?.value?.let { phone ->
      val normalizedPhone = phone.replace("[^0-9+]".toRegex(), "")
      val cursor = context.contentResolver.query(
        Data.CONTENT_URI,
        arrayOf(Data.RAW_CONTACT_ID),
        "${Data.MIMETYPE} = ? AND ${CommonDataKinds.Phone.NUMBER} LIKE ?",
        arrayOf(CommonDataKinds.Phone.CONTENT_ITEM_TYPE, "%$normalizedPhone%"),
        null
      )
      cursor?.use {
        if (it.moveToFirst()) {
          return it.getString(it.getColumnIndexOrThrow(Data.RAW_CONTACT_ID))
        }
      }
    }
    // Fallback to name if no phone number match
    if (!contact.firstName.isNullOrEmpty() && !contact.surname.isNullOrEmpty()) {
      val cursor = context.contentResolver.query(
        Data.CONTENT_URI,
        arrayOf(Data.RAW_CONTACT_ID),
        "${Data.MIMETYPE} = ? AND ${CommonDataKinds.StructuredName.GIVEN_NAME} = ? AND ${CommonDataKinds.StructuredName.FAMILY_NAME} = ?",
        arrayOf(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, contact.firstName, contact.surname),
        null
      )
      cursor?.use {
        if (it.moveToFirst()) {
          return it.getString(it.getColumnIndexOrThrow(Data.RAW_CONTACT_ID))
        }
      }
    }
    return null
  }
  fun updateContact(contact: Contact, photoUpdateStatus: Int = 4, accountName: String? = null, accountType: String? = null): Boolean {
    context.toast("Updating contact...")
    try {
      val operations = ArrayList<ContentProviderOperation>()

      // Step 1: Validate or find RAW_CONTACT_ID
      var rawContactId = contact.id
      if (rawContactId.toLong() == 0L || !isRawContactIdValid(rawContactId)) {
        // Check for existing contact by phone number or name to prevent duplication
        val existingRawContactId = checkForDuplicateContact(contact)
        if (existingRawContactId != null) {
          rawContactId = existingRawContactId.toInt()
        } else {
          // Create new raw contact if no match found
          ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI).apply {
            if (accountName != null && accountType != null) {
              withValue(ContactsContract.RawContacts.ACCOUNT_NAME, accountName)
              withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, accountType)
            }
            operations.add(build())
          }
          // Placeholder for new RAW_CONTACT_ID
          operations.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
            .withValueBackReference(Data.RAW_CONTACT_ID, 0)
            .withValue(Data.MIMETYPE, CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            .build())
        }
      }

      // Step 2: Update or insert StructuredName
      val cursor = context.contentResolver.query(
        Data.CONTENT_URI,
        arrayOf(Data._ID),
        "${Data.RAW_CONTACT_ID} = ? AND ${Data.MIMETYPE} = ?",
        arrayOf(rawContactId.toString(), CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE),
        null
      )
      if (cursor?.count == 0) {
        ContentProviderOperation.newInsert(Data.CONTENT_URI).apply {
          withValue(Data.RAW_CONTACT_ID, rawContactId)
          withValue(Data.MIMETYPE, CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
          withValue(CommonDataKinds.StructuredName.PREFIX, contact.prefix)
          withValue(CommonDataKinds.StructuredName.GIVEN_NAME, contact.firstName)
          withValue(CommonDataKinds.StructuredName.MIDDLE_NAME, contact.middleName)
          withValue(CommonDataKinds.StructuredName.FAMILY_NAME, contact.surname)
          withValue(CommonDataKinds.StructuredName.SUFFIX, contact.suffix)
          operations.add(build())
        }
      } else {
        ContentProviderOperation.newUpdate(Data.CONTENT_URI).apply {
          val selection = "${Data.RAW_CONTACT_ID} = ? AND ${Data.MIMETYPE} = ?"
          val selectionArgs = arrayOf(rawContactId.toString(), CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
          withSelection(selection, selectionArgs)
          withValue(CommonDataKinds.StructuredName.PREFIX, contact.prefix)
          withValue(CommonDataKinds.StructuredName.GIVEN_NAME, contact.firstName)
          withValue(CommonDataKinds.StructuredName.MIDDLE_NAME, contact.middleName)
          withValue(CommonDataKinds.StructuredName.FAMILY_NAME, contact.surname)
          withValue(CommonDataKinds.StructuredName.SUFFIX, contact.suffix)
          operations.add(build())
        }
      }
      cursor?.close()

      // Step 3: Update nickname
      ContentProviderOperation.newDelete(Data.CONTENT_URI).apply {
        val selection = "${Data.RAW_CONTACT_ID} = ? AND ${Data.MIMETYPE} = ?"
        val selectionArgs = arrayOf(rawContactId.toString(), CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
        withSelection(selection, selectionArgs)
        operations.add(build())
      }
      if (!contact.nickname.isNullOrEmpty()) {
        ContentProviderOperation.newInsert(Data.CONTENT_URI).apply {
          withValue(Data.RAW_CONTACT_ID, rawContactId)
          withValue(Data.MIMETYPE, CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
          withValue(CommonDataKinds.Nickname.NAME, contact.nickname)
          operations.add(build())
        }
      }

      // Step 4: Update phone numbers
      val existingPhoneNumbers = mutableListOf<Triple<String, String, Int>>()
      val phoneCursor = context.contentResolver.query(
        Data.CONTENT_URI,
        arrayOf(Data._ID, CommonDataKinds.Phone.NUMBER, CommonDataKinds.Phone.TYPE),
        "${Data.RAW_CONTACT_ID} = ? AND ${Data.MIMETYPE} = ?",
        arrayOf(rawContactId.toString(), CommonDataKinds.Phone.CONTENT_ITEM_TYPE),
        null
      )
      phoneCursor?.use {
        while (it.moveToNext()) {
          val id = it.getString(it.getColumnIndexOrThrow(Data._ID))
          val number = it.getString(it.getColumnIndexOrThrow(CommonDataKinds.Phone.NUMBER)) ?: ""
          val type = it.getInt(it.getColumnIndexOrThrow(CommonDataKinds.Phone.TYPE))
          existingPhoneNumbers.add(Triple(id, number, type))
        }
      }

      if (contact.phoneNumbers.isNotEmpty()) {
        contact.phoneNumbers.forEach { newPhone ->
          val normalizedNew = newPhone.value.replace("[^0-9+]".toRegex(), "")
          val matchingPhone = existingPhoneNumbers.find { existing ->
            val normalizedExisting = existing.second.replace("[^0-9+]".toRegex(), "")
            normalizedExisting == normalizedNew
          }
          if (matchingPhone != null) {
            ContentProviderOperation.newUpdate(Data.CONTENT_URI).apply {
              val selection = "${Data._ID} = ? AND ${Data.MIMETYPE} = ?"
              val selectionArgs = arrayOf(matchingPhone.first, CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
              withSelection(selection, selectionArgs)
              withValue(CommonDataKinds.Phone.NUMBER, newPhone.value)
              withValue(CommonDataKinds.Phone.NORMALIZED_NUMBER, newPhone.normalizedNumber)
              withValue(CommonDataKinds.Phone.TYPE, newPhone.type)
              withValue(CommonDataKinds.Phone.LABEL, newPhone.label)
              withValue(CommonDataKinds.Phone.IS_PRIMARY, if (newPhone.isPrimary) 1 else 0)
              operations.add(build())
            }
            existingPhoneNumbers.remove(matchingPhone)
          } else {
            ContentProviderOperation.newInsert(Data.CONTENT_URI).apply {
              withValue(Data.RAW_CONTACT_ID, rawContactId)
              withValue(Data.MIMETYPE, CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
              withValue(CommonDataKinds.Phone.NUMBER, newPhone.value)
              withValue(CommonDataKinds.Phone.NORMALIZED_NUMBER, newPhone.normalizedNumber)
              withValue(CommonDataKinds.Phone.TYPE, newPhone.type)
              withValue(CommonDataKinds.Phone.LABEL, newPhone.label)
              withValue(CommonDataKinds.Phone.IS_PRIMARY, if (newPhone.isPrimary) 1 else 0)
              operations.add(build())
            }
          }
        }

        existingPhoneNumbers.forEach { unmatched ->
          ContentProviderOperation.newDelete(Data.CONTENT_URI).apply {
            val selection = "${Data._ID} = ? AND ${Data.MIMETYPE} = ?"
            val selectionArgs = arrayOf(unmatched.first, CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
            withSelection(selection, selectionArgs)
            operations.add(build())
          }
        }
      }

      // Step 5: Update emails
      val existingEmails = mutableListOf<Triple<String, String, Int>>()
      val emailCursor = context.contentResolver.query(
        Data.CONTENT_URI,
        arrayOf(Data._ID, CommonDataKinds.Email.DATA, CommonDataKinds.Email.TYPE),
        "${Data.RAW_CONTACT_ID} = ? AND ${Data.MIMETYPE} = ?",
        arrayOf(rawContactId.toString(), CommonDataKinds.Email.CONTENT_ITEM_TYPE),
        null
      )
      emailCursor?.use {
        while (it.moveToNext()) {
          val id = it.getString(it.getColumnIndexOrThrow(Data._ID))
          val email = it.getString(it.getColumnIndexOrThrow(CommonDataKinds.Email.DATA)) ?: ""
          val type = it.getInt(it.getColumnIndexOrThrow(CommonDataKinds.Email.TYPE))
          existingEmails.add(Triple(id, email, type))
        }
      }

      contact.emails.forEach { newEmail ->
        val matchingEmail = existingEmails.find { it.second == newEmail.value }
        if (matchingEmail != null) {
          ContentProviderOperation.newUpdate(Data.CONTENT_URI).apply {
            val selection = "${Data._ID} = ? AND ${Data.MIMETYPE} = ?"
            val selectionArgs = arrayOf(matchingEmail.first, CommonDataKinds.Email.CONTENT_ITEM_TYPE)
            withSelection(selection, selectionArgs)
            withValue(CommonDataKinds.Email.DATA, newEmail.value)
            withValue(CommonDataKinds.Email.TYPE, newEmail.type)
            withValue(CommonDataKinds.Email.LABEL, newEmail.label)
            operations.add(build())
          }
          existingEmails.remove(matchingEmail)
        } else {
          ContentProviderOperation.newInsert(Data.CONTENT_URI).apply {
            withValue(Data.RAW_CONTACT_ID, rawContactId)
            withValue(Data.MIMETYPE, CommonDataKinds.Email.CONTENT_ITEM_TYPE)
            withValue(CommonDataKinds.Email.DATA, newEmail.value)
            withValue(CommonDataKinds.Email.TYPE, newEmail.type)
            withValue(CommonDataKinds.Email.LABEL, newEmail.label)
            operations.add(build())
          }
        }
      }

      existingEmails.forEach { unmatched ->
        ContentProviderOperation.newDelete(Data.CONTENT_URI).apply {
          val selection = "${Data._ID} = ? AND ${Data.MIMETYPE} = ?"
          val selectionArgs = arrayOf(unmatched.first, CommonDataKinds.Email.CONTENT_ITEM_TYPE)
          withSelection(selection, selectionArgs)
          operations.add(build())
        }
      }

      // Step 6: Update addresses
      val existingAddresses = mutableListOf<Triple<String, String, Int>>()
      val addressCursor = context.contentResolver.query(
        Data.CONTENT_URI,
        arrayOf(Data._ID, CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, CommonDataKinds.StructuredPostal.TYPE),
        "${Data.RAW_CONTACT_ID} = ? AND ${Data.MIMETYPE} = ?",
        arrayOf(rawContactId.toString(), CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE),
        null
      )
      addressCursor?.use {
        while (it.moveToNext()) {
          val id = it.getString(it.getColumnIndexOrThrow(Data._ID))
          val address = it.getString(it.getColumnIndexOrThrow(CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)) ?: ""
          val type = it.getInt(it.getColumnIndexOrThrow(CommonDataKinds.StructuredPostal.TYPE))
          existingAddresses.add(Triple(id, address, type))
        }
      }

      contact.addresses.forEach { newAddress ->
        val matchingAddress = existingAddresses.find { it.second == newAddress.value }
        if (matchingAddress != null) {
          ContentProviderOperation.newUpdate(Data.CONTENT_URI).apply {
            val selection = "${Data._ID} = ? AND ${Data.MIMETYPE} = ?"
            val selectionArgs = arrayOf(matchingAddress.first, CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
            withSelection(selection, selectionArgs)
            withValue(CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, newAddress.value)
            withValue(CommonDataKinds.StructuredPostal.TYPE, newAddress.type)
            withValue(CommonDataKinds.StructuredPostal.LABEL, newAddress.label)
            operations.add(build())
          }
          existingAddresses.remove(matchingAddress)
        } else {
          ContentProviderOperation.newInsert(Data.CONTENT_URI).apply {
            withValue(Data.RAW_CONTACT_ID, rawContactId)
            withValue(Data.MIMETYPE, CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
            withValue(CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, newAddress.value)
            withValue(CommonDataKinds.StructuredPostal.TYPE, newAddress.type)
            withValue(CommonDataKinds.StructuredPostal.LABEL, newAddress.label)
            operations.add(build())
          }
        }
      }

      existingAddresses.forEach { unmatched ->
        ContentProviderOperation.newDelete(Data.CONTENT_URI).apply {
          val selection = "${Data._ID} = ? AND ${Data.MIMETYPE} = ?"
          val selectionArgs = arrayOf(unmatched.first, CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
          withSelection(selection, selectionArgs)
          operations.add(build())
        }
      }

      // Step 7: Update IMs
      ContentProviderOperation.newDelete(Data.CONTENT_URI).apply {
        val selection = "${Data.RAW_CONTACT_ID} = ? AND ${Data.MIMETYPE} = ?"
        val selectionArgs = arrayOf(rawContactId.toString(), CommonDataKinds.Im.CONTENT_ITEM_TYPE)
        withSelection(selection, selectionArgs)
        operations.add(build())
      }
      contact.IMs.forEach {
        ContentProviderOperation.newInsert(Data.CONTENT_URI).apply {
          withValue(Data.RAW_CONTACT_ID, rawContactId)
          withValue(Data.MIMETYPE, CommonDataKinds.Im.CONTENT_ITEM_TYPE)
          withValue(CommonDataKinds.Im.DATA, it.value)
          withValue(CommonDataKinds.Im.PROTOCOL, it.type)
          withValue(CommonDataKinds.Im.CUSTOM_PROTOCOL, it.label)
          operations.add(build())
        }
      }

      // Step 8: Update events
      ContentProviderOperation.newDelete(Data.CONTENT_URI).apply {
        val selection = "${Data.RAW_CONTACT_ID} = ? AND ${Data.MIMETYPE} = ?"
        val selectionArgs = arrayOf(rawContactId.toString(), CommonDataKinds.Event.CONTENT_ITEM_TYPE)
        withSelection(selection, selectionArgs)
        operations.add(build())
      }
      contact.events.forEach {
        ContentProviderOperation.newInsert(Data.CONTENT_URI).apply {
          withValue(Data.RAW_CONTACT_ID, rawContactId)
          withValue(Data.MIMETYPE, CommonDataKinds.Event.CONTENT_ITEM_TYPE)
          withValue(CommonDataKinds.Event.START_DATE, it.value)
          withValue(CommonDataKinds.Event.TYPE, it.type)
          operations.add(build())
        }
      }

      // Step 9: Update notes
      ContentProviderOperation.newDelete(Data.CONTENT_URI).apply {
        val selection = "${Data.RAW_CONTACT_ID} = ? AND ${Data.MIMETYPE} = ?"
        val selectionArgs = arrayOf(rawContactId.toString(), CommonDataKinds.Note.CONTENT_ITEM_TYPE)
        withSelection(selection, selectionArgs)
        operations.add(build())
      }
      if (!contact.notes.isNullOrEmpty()) {
        ContentProviderOperation.newInsert(Data.CONTENT_URI).apply {
          withValue(Data.RAW_CONTACT_ID, rawContactId)
          withValue(Data.MIMETYPE, CommonDataKinds.Note.CONTENT_ITEM_TYPE)
          withValue(CommonDataKinds.Note.NOTE, contact.notes)
          operations.add(build())
        }
      }

      // Step 10: Update organization
      ContentProviderOperation.newDelete(Data.CONTENT_URI).apply {
        val selection = "${Data.RAW_CONTACT_ID} = ? AND ${Data.MIMETYPE} = ?"
        val selectionArgs = arrayOf(rawContactId.toString(), CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
        withSelection(selection, selectionArgs)
        operations.add(build())
      }
      if (contact.organization != null) {
        ContentProviderOperation.newInsert(Data.CONTENT_URI).apply {
          withValue(Data.RAW_CONTACT_ID, rawContactId)
          withValue(Data.MIMETYPE, CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
          withValue(CommonDataKinds.Organization.COMPANY, contact.organization.company)
          withValue(CommonDataKinds.Organization.TYPE, DEFAULT_ORGANIZATION_TYPE)
          withValue(CommonDataKinds.Organization.TITLE, contact.organization.jobPosition)
          operations.add(build())
        }
      }

      // Step 11: Update websites
      ContentProviderOperation.newDelete(Data.CONTENT_URI).apply {
        val selection = "${Data.RAW_CONTACT_ID} = ? AND ${Data.MIMETYPE} = ?"
        val selectionArgs = arrayOf(rawContactId.toString(), CommonDataKinds.Website.CONTENT_ITEM_TYPE)
        withSelection(selection, selectionArgs)
        operations.add(build())
      }
      contact.websites.forEach {
        ContentProviderOperation.newInsert(Data.CONTENT_URI).apply {
          withValue(Data.RAW_CONTACT_ID, rawContactId)
          withValue(Data.MIMETYPE, CommonDataKinds.Website.CONTENT_ITEM_TYPE)
          withValue(CommonDataKinds.Website.URL, it)
          withValue(CommonDataKinds.Website.TYPE, DEFAULT_WEBSITE_TYPE)
          operations.add(build())
        }
      }

      // Step 12: Update groups
      val relevantGroupIDs = getStoredGroupsSync().map { it.id }
      if (relevantGroupIDs.isNotEmpty()) {
        val IDsString = TextUtils.join(",", relevantGroupIDs)
        ContentProviderOperation.newDelete(Data.CONTENT_URI).apply {
          val selection = "${Data.CONTACT_ID} = ? AND ${Data.MIMETYPE} = ? AND ${Data.DATA1} IN ($IDsString)"
          val selectionArgs = arrayOf(contact.contactId.toString(), CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
          withSelection(selection, selectionArgs)
          operations.add(build())
        }
      }
      contact.groups.forEach {
        ContentProviderOperation.newInsert(Data.CONTENT_URI).apply {
          withValue(Data.RAW_CONTACT_ID, rawContactId)
          withValue(Data.MIMETYPE, CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
          withValue(CommonDataKinds.GroupMembership.GROUP_ROW_ID, it.id)
          operations.add(build())
        }
      }

      // Step 13: Update favorite and ringtone
      try {
        val uri = Uri.withAppendedPath(Contacts.CONTENT_URI, contact.contactId.toString())
        val contentValues = ContentValues().apply {
          put(Contacts.STARRED, contact.starred)
          put(Contacts.CUSTOM_RINGTONE, contact.ringtone)
        }
        context.contentResolver.update(uri, contentValues, null, null)
      } catch (e: Exception) {
        context.showErrorToast(e)
      }

      // Step 14: Update photo
      when (photoUpdateStatus) {
        PHOTO_ADDED, PHOTO_CHANGED -> addPhoto(contact, operations)
        PHOTO_REMOVED -> removePhoto(contact, operations)
      }

      // Step 15: Apply batch operations
      context.contentResolver.applyBatch(AUTHORITY, operations)

      // Step 16: Notify contact aggregation
      context.contentResolver.notifyChange(ContactsContract.RawContacts.CONTENT_URI, null)

      return true
    } catch (e: Exception) {
      context.showErrorToast(e)
      return false
    }
  }
  fun getContactById(rawContactId: Long): Contact? {
    try {
      val uri = Data.CONTENT_URI
      val projection = arrayOf(
        Data.MIMETYPE,
        Data.CONTACT_ID,
        Data.RAW_CONTACT_ID,
        CommonDataKinds.StructuredName.PREFIX,
        CommonDataKinds.StructuredName.GIVEN_NAME,
        CommonDataKinds.StructuredName.MIDDLE_NAME,
        CommonDataKinds.StructuredName.FAMILY_NAME,
        CommonDataKinds.StructuredName.SUFFIX,
        CommonDataKinds.StructuredName.PHOTO_URI,
        CommonDataKinds.StructuredName.PHOTO_THUMBNAIL_URI,
        CommonDataKinds.StructuredName.STARRED,
        CommonDataKinds.StructuredName.CUSTOM_RINGTONE,
        RawContacts.ACCOUNT_NAME,
        RawContacts.ACCOUNT_TYPE,
        CommonDataKinds.Nickname.NAME,
        CommonDataKinds.Phone.NUMBER,
        CommonDataKinds.Phone.TYPE,
        CommonDataKinds.Phone.LABEL,
        CommonDataKinds.Phone.NORMALIZED_NUMBER,
        CommonDataKinds.Phone.IS_PRIMARY,
        CommonDataKinds.Email.DATA,
        CommonDataKinds.Email.TYPE,
        CommonDataKinds.Email.LABEL,
        CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
        CommonDataKinds.StructuredPostal.TYPE,
        CommonDataKinds.StructuredPostal.LABEL,
        CommonDataKinds.Event.START_DATE,
        CommonDataKinds.Event.TYPE,
        CommonDataKinds.GroupMembership.GROUP_ROW_ID,
        CommonDataKinds.Website.URL,
        CommonDataKinds.Im.DATA,
        CommonDataKinds.Im.PROTOCOL,
        CommonDataKinds.Im.CUSTOM_PROTOCOL,
        CommonDataKinds.Organization.COMPANY,
        CommonDataKinds.Organization.TITLE,
        CommonDataKinds.Note.NOTE
      )
      val selection = "${Data.RAW_CONTACT_ID} = ?"
      val selectionArgs = arrayOf(rawContactId.toString())

      val phoneNumbers = arrayListOf<PhoneNumber>()
      val emails = arrayListOf<Email>()
      val addresses = arrayListOf<Address>()
      val events = arrayListOf<Event>()
      val groups = arrayListOf<Group>()
      val websites = arrayListOf<String>()
      val ims = arrayListOf<IM>()
      var organization = Organization("", "")
      var prefix = ""
      var firstName = ""
      var middleName = ""
      var surname = ""
      var suffix = ""
      var nickname = ""
      var photoUri = ""
      var source = ""
      var starred = 0
      var contactId = 0
      var thumbnailUri = ""
      var notes = ""
      var mimetype = ""
      var ringtone: String? = null

      context.queryCursor(uri, projection, selection, selectionArgs, showErrors = true) { cursor ->
        if (contactId == 0) { // Set contactId only once
          contactId = cursor.getIntValue(Data.CONTACT_ID)
        }
        starred = cursor.getIntValue(CommonDataKinds.StructuredName.STARRED)
        source = cursor.getStringValue(RawContacts.ACCOUNT_NAME) ?: ""
        ringtone = cursor.getStringValue(CommonDataKinds.StructuredName.CUSTOM_RINGTONE)
        photoUri = cursor.getStringValue(CommonDataKinds.StructuredName.PHOTO_URI) ?: ""
        thumbnailUri = cursor.getStringValue(CommonDataKinds.StructuredName.PHOTO_THUMBNAIL_URI) ?: ""

        when (cursor.getStringValue(Data.MIMETYPE)) {
          CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE -> {
            prefix = cursor.getStringValue(CommonDataKinds.StructuredName.PREFIX) ?: ""
            firstName = cursor.getStringValue(CommonDataKinds.StructuredName.GIVEN_NAME) ?: ""
            middleName = cursor.getStringValue(CommonDataKinds.StructuredName.MIDDLE_NAME) ?: ""
            surname = cursor.getStringValue(CommonDataKinds.StructuredName.FAMILY_NAME) ?: ""
            suffix = cursor.getStringValue(CommonDataKinds.StructuredName.SUFFIX) ?: ""
          }
          CommonDataKinds.Nickname.CONTENT_ITEM_TYPE -> {
            nickname = cursor.getStringValue(CommonDataKinds.Nickname.NAME) ?: ""
          }
          CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
            val number = cursor.getStringValue(CommonDataKinds.Phone.NUMBER) ?: ""
            val type = cursor.getIntValue(CommonDataKinds.Phone.TYPE)
            val label = cursor.getStringValue(CommonDataKinds.Phone.LABEL) ?: ""
            val normalizedNumber = cursor.getStringValue(CommonDataKinds.Phone.NORMALIZED_NUMBER) ?: ""
            val isPrimary = cursor.getIntValue(CommonDataKinds.Phone.IS_PRIMARY) == 1
            phoneNumbers.add(PhoneNumber(number, type, label, normalizedNumber, isPrimary))
          }
          CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
            val email = cursor.getStringValue(CommonDataKinds.Email.DATA) ?: ""
            val type = cursor.getIntValue(CommonDataKinds.Email.TYPE)
            val label = cursor.getStringValue(CommonDataKinds.Email.LABEL) ?: ""
            emails.add(Email(email, type, label))
          }
          CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
            val address = cursor.getStringValue(CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS) ?: ""
            val type = cursor.getIntValue(CommonDataKinds.StructuredPostal.TYPE)
            val label = cursor.getStringValue(CommonDataKinds.StructuredPostal.LABEL) ?: ""
            addresses.add(Address(address, type, label))
          }
          CommonDataKinds.Event.CONTENT_ITEM_TYPE -> {
            val date = cursor.getStringValue(CommonDataKinds.Event.START_DATE) ?: ""
            val type = cursor.getIntValue(CommonDataKinds.Event.TYPE)
            events.add(Event(date, type))
          }
          CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE -> {
            val groupId = cursor.getLongValue(CommonDataKinds.GroupMembership.GROUP_ROW_ID)
            val groupTitle = getGroupTitle(groupId)
            if (groupTitle.isNotEmpty()) {
              groups.add(Group(groupId, groupTitle))
            }
          }
          CommonDataKinds.Website.CONTENT_ITEM_TYPE -> {
            val url = cursor.getStringValue(CommonDataKinds.Website.URL) ?: ""
            if (url.isNotEmpty()) {
              websites.add(url)
            }
          }
          CommonDataKinds.Im.CONTENT_ITEM_TYPE -> {
            val im = cursor.getStringValue(CommonDataKinds.Im.DATA) ?: ""
            val type = cursor.getIntValue(CommonDataKinds.Im.PROTOCOL)
            val label = cursor.getStringValue(CommonDataKinds.Im.CUSTOM_PROTOCOL) ?: ""
            ims.add(IM(im, type, label))
          }
          CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> {
            val company = cursor.getStringValue(CommonDataKinds.Organization.COMPANY) ?: ""
            val title = cursor.getStringValue(CommonDataKinds.Organization.TITLE) ?: ""
            organization = Organization(company, title)
          }
          CommonDataKinds.Note.CONTENT_ITEM_TYPE -> {
            notes = cursor.getStringValue(CommonDataKinds.Note.NOTE) ?: ""
          }
        }
      }

      // If no meaningful data was found, return null
      if (firstName.isEmpty() && surname.isEmpty() && phoneNumbers.isEmpty() && emails.isEmpty()) {
        return null
      }

      return Contact(
        id = rawContactId.toInt(),
        prefix = prefix,
        firstName = firstName,
        middleName = middleName,
        surname = surname,
        suffix = suffix,
        nickname = nickname,
        photoUri = photoUri,
        phoneNumbers = phoneNumbers,
        emails = emails,
        addresses = addresses,
        events = events,
        source = source,
        starred = starred,
        contactId = contactId,
        thumbnailUri = thumbnailUri,
        photo = null,
        notes = notes,
        groups = groups,
        organization = organization,
        websites = websites,
        IMs = ims,
        mimetype = mimetype,
        ringtone = ringtone
      )
    } catch (e: Exception) {
      context.showErrorToast(e)
      return null
    }
  }

  private fun getGroupTitle(groupId: Long): String {
    val uri = Groups.CONTENT_URI
    val projection = arrayOf(Groups.TITLE)
    val selection = "${Groups._ID} = ?"
    val selectionArgs = arrayOf(groupId.toString())

    var title = ""
    context.queryCursor(uri, projection, selection, selectionArgs, showErrors = true) { cursor ->
      title = cursor.getStringValue(Groups.TITLE) ?: ""
    }
    return title
  }
  fun addPhoto(contact: Contact, operations: ArrayList<ContentProviderOperation>): ArrayList<ContentProviderOperation> {
    if (contact.photoUri.isNotEmpty()) {
      val photoUri = Uri.parse(contact.photoUri)
      var bitmap: Bitmap? = null
      var scaledPhoto: Bitmap? = null
      try {
        // Load the bitmap
        bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, photoUri)

        // Create scaled thumbnail
        val thumbnailSize = context.getPhotoThumbnailSize()
        scaledPhoto = Bitmap.createScaledBitmap(bitmap, thumbnailSize, thumbnailSize, false)
        val scaledSizePhotoData = scaledPhoto.getByteArray()

        // Get full-size photo data
        val fullSizePhotoData = bitmap.getByteArray()

        // Add thumbnail to operations
        ContentProviderOperation.newInsert(Data.CONTENT_URI).apply {
          withValue(Data.RAW_CONTACT_ID, contact.id)
          withValue(Data.MIMETYPE, CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
          withValue(CommonDataKinds.Photo.PHOTO, scaledSizePhotoData)
          operations.add(build())
        }

        // Add full-size photo
        addFullSizePhoto(contact.id.toLong(), fullSizePhotoData)
      } catch (e: Exception) {
        context.showErrorToast(e)
      } finally {
        // Recycle bitmaps only after all operations are complete
        scaledPhoto?.let {
          if (!it.isRecycled) it.recycle()
        }
        bitmap?.let {
          if (!it.isRecycled) it.recycle()
        }
      }
    }
    return operations
  }

  fun deleteContact(originalContact: Contact, deleteClones: Boolean = false, callback: (success: Boolean) -> Unit) {
    ensureBackgroundThread {
      if (deleteContacts(arrayListOf(originalContact))) {
        callback(true)
      }
    }
  }

  fun deleteContacts(contacts: ArrayList<Contact>): Boolean {
    return try {
      val operations = ArrayList<ContentProviderOperation>()
      val selection = "${RawContacts._ID} = ?"
      contacts.filter { !it.isPrivate() }.forEach {
        ContentProviderOperation.newDelete(RawContacts.CONTENT_URI).apply {
          val selectionArgs = arrayOf(it.id.toString())
          withSelection(selection, selectionArgs)
          operations.add(build())
        }

        if (operations.size % BATCH_SIZE == 0) {
          context.contentResolver.applyBatch(AUTHORITY, operations)
          operations.clear()
        }
      }
      context.contentResolver.applyBatch(AUTHORITY, operations)
      true
    } catch (e: Exception) {
      context.showErrorToast(e)
      false
    }
  }

  /*fun getDuplicatesOfContact(contact: Contact, addOriginal: Boolean, callback: (ArrayList<Contact>) -> Unit) {
    ensureBackgroundThread {
      getContacts(true, true) { contacts ->
        val duplicates =
          contacts.filter { it.id != contact.id && it.getHashToCompare() == contact.getHashToCompare() }.toMutableList() as ArrayList<Contact>
        if (addOriginal) {
          duplicates.add(contact)
        }
        callback(duplicates)
      }
    }
  }*/

  private fun removePhoto(contact: Contact, operations: ArrayList<ContentProviderOperation>): ArrayList<ContentProviderOperation> {
    ContentProviderOperation.newDelete(Data.CONTENT_URI).apply {
      val selection = "${Data.RAW_CONTACT_ID} = ? AND ${Data.MIMETYPE} = ?"
      val selectionArgs = arrayOf(contact.id.toString(), CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
      withSelection(selection, selectionArgs)
      operations.add(build())
    }

    return operations
  }

  fun getStoredGroupsSync(): ArrayList<Group> {
    val groups = getDeviceStoredGroups()
    return groups
  }

  private fun getDeviceStoredGroups(): ArrayList<Group> {
    val groups = ArrayList<Group>()

    val uri = Groups.CONTENT_URI
    val projection = arrayOf(
      Groups._ID,
      Groups.TITLE,
      Groups.SYSTEM_ID
    )

    val selection = "${Groups.AUTO_ADD} = ? AND ${Groups.FAVORITES} = ?"
    val selectionArgs = arrayOf("0", "0")

    context.queryCursor(uri, projection, selection, selectionArgs, showErrors = true) { cursor ->
      val id = cursor.getLongValue(Groups._ID)
      val title = cursor.getStringValue(Groups.TITLE) ?: return@queryCursor

      val systemId = cursor.getStringValue(Groups.SYSTEM_ID)
      if (groups.map { it.title }.contains(title) && systemId != null) {
        return@queryCursor
      }

      groups.add(Group(id, title))
    }
    return groups
  }

}
