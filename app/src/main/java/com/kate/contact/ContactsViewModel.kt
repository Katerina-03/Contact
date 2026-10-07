package com.kate.contact

import android.app.Application
import android.content.Context
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactsViewModel(app: Application) : AndroidViewModel(app) {
    private val contactsMutable = MutableStateFlow<List<Contact>?>(null)
    val contacts: StateFlow<List<Contact>?> = contactsMutable.asStateFlow()

    private var started = false

    fun load() {
        if (started) return
        started = true
        viewModelScope.launch {
            contactsMutable.value = withContext(Dispatchers.IO) {
                try {
                    fetchContacts(getApplication())
                } catch (_: SecurityException) {
                    emptyList()
                }
            }
        }
    }
}

private fun fetchContacts(context: Context): List<Contact> {
    val projection = arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER)
    val cursor = context.contentResolver.query(
        Phone.CONTENT_URI,
        projection,
        null,
        null,
        null
    ) ?: return emptyList()

    cursor.use {
        val nameIndex = it.getColumnIndexOrThrow(Phone.DISPLAY_NAME)
        val numberIndex = it.getColumnIndexOrThrow(Phone.NUMBER)
        val result = ArrayList<Contact>()

        while (it.moveToNext()) {
            val number = it.getString(numberIndex) ?: continue
            val name = it.getString(nameIndex) ?: number
            result.add(Contact(name, number))
        }

        return result.sortedWith(
            compareBy(String.CASE_INSENSITIVE_ORDER) { c -> c.name }
        )
    }
}
