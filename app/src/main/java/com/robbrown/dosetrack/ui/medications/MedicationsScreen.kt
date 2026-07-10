package com.robbrown.dosetrack.ui.medications

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.robbrown.dosetrack.data.entity.Medication
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Lists active medications and lets the user add one via a simple dialog. The full
 * schedule-builder add/edit form is a later phase; this covers name/dosage/unit only.
 */
@Composable
fun MedicationsScreen(viewModel: MedicationsViewModel = hiltViewModel()) {
    val medications by viewModel.medications.collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add medication")
            }
        },
    ) { innerPadding ->
        if (medications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No medications yet", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                items(medications, key = { it.id }) { medication ->
                    ListItem(
                        headlineContent = { Text(medication.name) },
                        supportingContent = { Text("${medication.dosage} • ${medication.unit}") },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddMedicationDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, dosage, unit ->
                scope.launch {
                    val result = viewModel.addMedication(
                        Medication(id = UUID.randomUUID().toString(), name = name, dosage = dosage, unit = unit),
                        isPro = false,
                    )
                    if (result.isSuccess) {
                        showAddDialog = false
                    } else {
                        errorMessage = result.exceptionOrNull()?.message
                    }
                }
            },
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            confirmButton = { TextButton(onClick = { errorMessage = null }) { Text("OK") } },
            title = { Text("Couldn't add medication") },
            text = { Text(message) },
        )
    }
}

@Composable
private fun AddMedicationDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, dosage: String, unit: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add medication") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosage") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, dosage, unit) },
                enabled = name.isNotBlank() && dosage.isNotBlank() && unit.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
