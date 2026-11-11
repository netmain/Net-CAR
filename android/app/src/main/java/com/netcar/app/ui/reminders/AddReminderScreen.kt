package com.netcar.app.ui.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.netcar.app.model.ServiceType
import com.netcar.app.model.SupportedLanguage
import com.netcar.app.ui.components.TopBar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    viewModel: ReminderViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formState by viewModel.formState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val distUnit = uiState.userSettings.units.distUnit
    val isFr = uiState.userSettings.language == SupportedLanguage.FR
    val isEditing = formState.editingReminderId != null
    var serviceDropdownExpanded by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var showLastServiceDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddTypeDialog by remember { mutableStateOf(false) }
    var newTypeName by remember { mutableStateOf("") }
    var newTypeError by remember { mutableStateOf<String?>(null) }

    val customTypes = remember(
        uiState.userSettings.customServiceTypes,
        uiState.activeReminders,
        uiState.completedReminders
    ) {
        (
            uiState.userSettings.customServiceTypes +
                uiState.activeReminders.map { it.customType } +
                uiState.completedReminders.map { it.customType }
            )
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.US) }
    }

    val selectedTypeLabel = formState.customType.ifBlank {
        if (isFr) formState.type.displayNameFr else formState.type.displayName
    }

    Scaffold(
        topBar = {
            TopBar(
                title = when {
                    isEditing && isFr -> "Modifier le rappel"
                    isEditing -> "Edit Reminder"
                    isFr -> "Nouveau rappel"
                    else -> "Create Reminder"
                },
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = (if (isFr) "Véhicule : " else "Vehicle: ") +
                    (uiState.selectedVehicle?.displayName ?: if (isFr) "Véhicule sélectionné" else "Selected Car"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            // Service Type Dropdown
            ExposedDropdownMenuBox(
                expanded = serviceDropdownExpanded,
                onExpandedChange = { serviceDropdownExpanded = !serviceDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedTypeLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (isFr) "Type de service *" else "Service Type *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = serviceDropdownExpanded,
                    onDismissRequest = { serviceDropdownExpanded = false }
                ) {
                    ServiceType.values().forEach { st ->
                        DropdownMenuItem(
                            text = { Text(if (isFr) st.displayNameFr else st.displayName) },
                            onClick = {
                                viewModel.updateType(st)
                                if (formState.title.isBlank()) {
                                    viewModel.updateTitle(if (isFr) st.displayNameFr else st.displayName)
                                }
                                serviceDropdownExpanded = false
                            }
                        )
                    }
                    customTypes.forEach { custom ->
                        DropdownMenuItem(
                            text = { Text(custom) },
                            onClick = {
                                viewModel.updateCustomType(custom)
                                if (formState.title.isBlank()) {
                                    viewModel.updateTitle(custom)
                                }
                                serviceDropdownExpanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isFr) "＋ Ajouter une catégorie…" else "＋ Add a category…",
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {
                            serviceDropdownExpanded = false
                            newTypeName = ""
                            newTypeError = null
                            showAddTypeDialog = true
                        }
                    )
                }
            }

            OutlinedTextField(
                value = formState.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text(if (isFr) "Titre du rappel" else "Reminder Title") },
                placeholder = { Text(if (isFr) "ex. Vidange & filtre à huile" else "e.g. Engine Oil & Filter Change") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = formState.intervalKm,
                    onValueChange = { viewModel.updateIntervalKm(it) },
                    label = { Text(if (isFr) "Intervalle ($distUnit)" else "Interval ($distUnit)") },
                    placeholder = { Text("15000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = formState.intervalDays,
                    onValueChange = { viewModel.updateIntervalDays(it) },
                    label = { Text(if (isFr) "Intervalle (jours)" else "Interval (Days)") },
                    placeholder = { Text("365") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = formState.dueOdometer,
                    onValueChange = { viewModel.updateDueOdometer(it) },
                    label = {
                        Text(
                            if (isFr) "Kilométrage cible ($distUnit)" else "Target Odometer ($distUnit)"
                        )
                    },
                    placeholder = {
                        Text(
                            if (isEditing) {
                                if (isFr) "Aucune cible" else "No target"
                            } else {
                                if (isFr) "Calculé automatiquement si vide" else "Auto-calculated if blank"
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                if (!isEditing && formState.dueDateMillis == null) {
                    OutlinedTextField(
                        value = formState.dueDaysInFuture,
                        onValueChange = { viewModel.updateDueDays(it) },
                        label = { Text(if (isFr) "Échéance dans (jours)" else "Due in (Days)") },
                        placeholder = { Text("90") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            ReminderDateField(
                label = if (isFr) "Date d'échéance" else "Due Date",
                dateMillis = formState.dueDateMillis,
                notSetLabel = if (isFr) "Non définie" else "Not set",
                onClick = { showDueDatePicker = true },
                onClear = if (formState.dueDateMillis != null) {
                    { viewModel.updateDueDate(null) }
                } else null
            )

            if (isEditing) {
                Text(
                    text = if (isFr) "Dernier entretien" else "Last Service",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = formState.lastServiceOdometer,
                    onValueChange = { viewModel.updateLastServiceOdometer(it) },
                    label = {
                        Text(
                            if (isFr) "Kilométrage du dernier entretien ($distUnit)"
                            else "Last Service Odometer ($distUnit)"
                        )
                    },
                    placeholder = { Text(if (isFr) "Facultatif" else "Optional") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                ReminderDateField(
                    label = if (isFr) "Date du dernier entretien" else "Last Service Date",
                    dateMillis = formState.lastServiceDateMillis,
                    notSetLabel = if (isFr) "Non définie" else "Not set",
                    onClick = { showLastServiceDatePicker = true },
                    onClear = if (formState.lastServiceDateMillis != null) {
                        { viewModel.updateLastServiceDate(null) }
                    } else null
                )
            }

            // Recurring Switch Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isFr) "Rappel récurrent" else "Recurring Reminder",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = if (isFr) {
                                "Réinitialise automatiquement la cible une fois terminé"
                            } else {
                                "Automatically reset target when completed"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = formState.isRecurring,
                        onCheckedChange = { viewModel.updateIsRecurring(it) }
                    )
                }
            }

            if (formState.errorMessage != null) {
                Text(
                    text = formState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.saveReminder(onSuccess = onNavigateBack) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = when {
                        isEditing && isFr -> "Enregistrer les modifications"
                        isEditing -> "Update Reminder"
                        isFr -> "Enregistrer le rappel"
                        else -> "Save Reminder"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (isEditing) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isFr) "Supprimer le rappel" else "Delete Reminder",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }

    if (showAddTypeDialog) {
        AlertDialog(
            onDismissRequest = { showAddTypeDialog = false },
            title = { Text(if (isFr) "Nouvelle catégorie" else "New category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (isFr) {
                            "Ajoutez un type de service ou de rappel personnalisé."
                        } else {
                            "Add a custom service or reminder type."
                        }
                    )
                    OutlinedTextField(
                        value = newTypeName,
                        onValueChange = {
                            newTypeName = it
                            newTypeError = null
                        },
                        singleLine = true,
                        label = { Text(if (isFr) "Nom de la catégorie" else "Category name") },
                        placeholder = {
                            Text(if (isFr) "ex. Assurance flotte" else "e.g. Fleet insurance")
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (newTypeError != null) {
                        Text(
                            text = newTypeError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clean = newTypeName.trim().replace(Regex("\\s+"), " ")
                        if (clean.isBlank()) {
                            newTypeError =
                                if (isFr) "Saisissez un nom de catégorie" else "Enter a category name"
                            return@TextButton
                        }
                        viewModel.addCustomServiceType(clean)
                        if (formState.title.isBlank()) {
                            viewModel.updateTitle(clean)
                        }
                        showAddTypeDialog = false
                    }
                ) {
                    Text(if (isFr) "Ajouter" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTypeDialog = false }) {
                    Text(if (isFr) "Annuler" else "Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(if (isFr) "Supprimer ce rappel ?" else "Delete this reminder?")
            },
            text = {
                Text(
                    if (isFr) {
                        "Le rappel sera supprimé du téléphone et du cloud lors de la prochaine synchronisation."
                    } else {
                        "This reminder will be removed from your phone and from the cloud on the next sync."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        formState.editingReminderId?.let { viewModel.deleteReminder(it) }
                        onNavigateBack()
                    }
                ) {
                    Text(
                        text = if (isFr) "Supprimer" else "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(if (isFr) "Annuler" else "Cancel")
                }
            }
        )
    }

    if (showDueDatePicker) {
        ReminderDatePickerDialog(
            initialDateMillis = formState.dueDateMillis,
            onDismiss = { showDueDatePicker = false },
            onDateSelected = { viewModel.updateDueDate(it) },
            isFrench = isFr
        )
    }

    if (showLastServiceDatePicker) {
        ReminderDatePickerDialog(
            initialDateMillis = formState.lastServiceDateMillis,
            onDismiss = { showLastServiceDatePicker = false },
            onDateSelected = { viewModel.updateLastServiceDate(it) },
            isFrench = isFr
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderDateField(
    label: String,
    dateMillis: Long?,
    notSetLabel: String,
    onClick: () -> Unit,
    onClear: (() -> Unit)?
) {
    val formatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateMillis?.let { formatter.format(Date(it)) } ?: notSetLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (onClear != null) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear date",
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderDatePickerDialog(
    initialDateMillis: Long?,
    onDismiss: () -> Unit,
    onDateSelected: (Long?) -> Unit,
    isFrench: Boolean = false
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis?.let { localDateToUtcMillis(it) }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onDateSelected(state.selectedDateMillis?.let { utcDateToLocalMillis(it) })
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isFrench) "Annuler" else "Cancel")
            }
        }
    ) {
        DatePicker(state = state)
    }
}

private fun localDateToUtcMillis(localMillis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMillis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            local.get(Calendar.YEAR),
            local.get(Calendar.MONTH),
            local.get(Calendar.DAY_OF_MONTH)
        )
    }.timeInMillis
}

private fun utcDateToLocalMillis(utcMillis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    return Calendar.getInstance().apply {
        clear()
        set(
            utc.get(Calendar.YEAR),
            utc.get(Calendar.MONTH),
            utc.get(Calendar.DAY_OF_MONTH),
            12,
            0,
            0
        )
    }.timeInMillis
}
