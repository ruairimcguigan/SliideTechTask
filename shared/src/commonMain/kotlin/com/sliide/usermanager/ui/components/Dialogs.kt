package com.sliide.usermanager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/**
 * Polished "Add User" dialog with:
 * - Real-time name and email validation
 * - Gender radio group
 * - Loading state during API call
 *
 * Uses local TextFieldValue state to avoid cursor desync on iOS,
 * syncing text back to the ViewModel on each change.
 */
@Composable
fun AddUserDialog(
    name: String,
    email: String,
    gender: String,
    nameError: String?,
    emailError: String?,
    isCreating: Boolean,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    // Local TextFieldValue state preserves cursor position on iOS
    var nameFieldValue by remember {
        mutableStateOf(TextFieldValue(name, TextRange(name.length)))
    }
    var emailFieldValue by remember {
        mutableStateOf(TextFieldValue(email, TextRange(email.length)))
    }

    // Sync external changes (e.g. dialog reset) back to local state
    LaunchedEffect(name) {
        if (name != nameFieldValue.text) {
            nameFieldValue = TextFieldValue(name, TextRange(name.length))
        }
    }
    LaunchedEffect(email) {
        if (email != emailFieldValue.text) {
            emailFieldValue = TextFieldValue(email, TextRange(email.length))
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = {
            Text(
                text = "Add New User",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name field with validation
                OutlinedTextField(
                    value = nameFieldValue,
                    onValueChange = {
                        nameFieldValue = it
                        onNameChange(it.text)
                    },
                    label = { Text("Full Name") },
                    placeholder = { Text("e.g. John Smith") },
                    isError = nameError != null,
                    supportingText = if (nameError != null) {
                        { Text(text = nameError, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth()
                )

                // Email field with validation
                OutlinedTextField(
                    value = emailFieldValue,
                    onValueChange = {
                        emailFieldValue = it
                        onEmailChange(it.text)
                    },
                    label = { Text("Email Address") },
                    placeholder = { Text("e.g. john@example.com") },
                    isError = emailError != null,
                    supportingText = if (emailError != null) {
                        { Text(text = emailError, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth()
                )

                // Gender selector
                Text(
                    text = "Gender",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    listOf("male" to "Male", "female" to "Female").forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .selectable(
                                    selected = gender == value,
                                    onClick = { onGenderChange(value) },
                                    role = Role.RadioButton,
                                    enabled = !isCreating
                                )
                                .padding(end = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = gender == value,
                                onClick = null,
                                enabled = !isCreating
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = !isCreating && nameFieldValue.text.isNotBlank() && emailFieldValue.text.isNotBlank()
                    && nameError == null && emailError == null
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp).width(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isCreating) "Creating..." else "Create User")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Delete confirmation dialog.
 */
@Composable
fun DeleteConfirmDialog(
    userName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete User",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete $userName? This action can be undone for a short time.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
