package com.robbrown.dosetrack.ui.medications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.robbrown.dosetrack.data.entity.Medication
import com.robbrown.dosetrack.data.entity.isRefillWarning
import com.robbrown.dosetrack.data.entity.toColor
import com.robbrown.dosetrack.util.Constants
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Lists active medications, mirroring the iOS Medications screen's layout: a top bar
 * with an add action, a colored dot + name/dosage + refill-warning row per medication,
 * a free-tier "X of 5" counter, and a matching empty state. The full schedule-builder
 * add/edit form, swipe-to-reorder, and caregiver features are later phases — this covers
 * a simple add dialog and a per-row delete action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationsScreen(viewModel: MedicationsViewModel = hiltViewModel()) {
    val medications by viewModel.medications.collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun addMedication(name: String, dosage: String, unit: String) {
        scope.launch {
            val result = viewModel.addMedication(
                Medication(id = UUID.randomUUID().toString(), name = name, dosage = dosage, unit = unit),
                isPro = false,
            )
            if (result.isSuccess) showAddDialog = false else errorMessage = result.exceptionOrNull()?.message
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medications") },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add medication")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (medications.isEmpty()) {
            EmptyMedicationsState(
                modifier = Modifier.padding(innerPadding),
                onAddClick = { showAddDialog = true },
            )
        } else {
            Column(modifier = Modifier.padding(innerPadding)) {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(medications, key = { it.id }) { medication ->
                        MedicationRow(
                            medication = medication,
                            onDelete = { scope.launch { viewModel.softDelete(medication) } },
                        )
                        HorizontalDivider()
                    }
                }
                FreeTierCounter(count = medications.size)
            }
        }
    }

    if (showAddDialog) {
        AddMedicationDialog(onDismiss = { showAddDialog = false }, onAdd = ::addMedication)
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
private fun MedicationRow(medication: Medication, onDelete: () -> Unit) {
    ListItem(
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(medication.toColor(), CircleShape),
            )
        },
        headlineContent = { Text(medication.name) },
        supportingContent = { Text("${medication.dosage} ${medication.unit}") },
        trailingContent = {
            Row {
                if (medication.isRefillWarning) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = "Refill warning",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete ${medication.name}")
                }
            }
        },
    )
}

@Composable
private fun FreeTierCounter(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            "$count of ${Constants.FREE_TIER_MED_LIMIT} medications (free tier)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyMedicationsState(modifier: Modifier = Modifier, onAddClick: () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Medication,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "No Medications Yet",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                "Add your first medication to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Button(onClick = onAddClick, modifier = Modifier.padding(top = 16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Add Medication")
                }
            }
        }
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
