package com.kate.contact

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ContactsScreen(viewModel: ContactsViewModel = viewModel()) {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.READ_CONTACTS) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    var permissionRequested by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.load()
        } else if (!permissionRequested) {
            permissionRequested = true
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    val contacts by viewModel.contacts.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            val list = contacts
            when {
                !hasPermission -> Message(R.string.contacts_permission_required)
                list == null -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                list.isEmpty() -> Message(R.string.no_contacts_found)
                else -> {
                    Text(
                        text = pluralStringResource(
                            R.plurals.contacts_found,
                            list.size,
                            list.size
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                    ContactsList(list, onClick = { context.dial(it.phoneNumber) })
                }
            }
        }
    }
}

@Composable
private fun Message(@StringRes textRes: Int) {
    Text(text = stringResource(textRes), modifier = Modifier.padding(16.dp))
}

@Composable
private fun ContactsList(contacts: List<Contact>, onClick: (Contact) -> Unit) {
    LazyColumn {
        items(contacts) { contact ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(contact) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(text = contact.name, style = MaterialTheme.typography.titleLarge)
                Text(text = contact.phoneNumber, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun Context.dial(phoneNumber: String) {
    val intent = Intent(
        Intent.ACTION_DIAL,
        Uri.fromParts("tel", phoneNumber, null)
    )
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {

    }
}
