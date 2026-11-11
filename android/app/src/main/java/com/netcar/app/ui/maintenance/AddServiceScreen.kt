package com.netcar.app.ui.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.netcar.app.model.ServiceType
import com.netcar.app.model.SupportedLanguage
import com.netcar.app.ui.components.TopBar
import com.netcar.app.ui.theme.MaintenanceBlue
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceScreen(
    viewModel: MaintenanceViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formState by viewModel.formState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val currencySymbol = uiState.userSettings.currency.symbol
    val distUnit = uiState.userSettings.units.distUnit
    val isFr = uiState.userSettings.language == SupportedLanguage.FR
    var serviceDropdownExpanded by remember { mutableStateOf(false) }
    var showAddTypeDialog by remember { mutableStateOf(false) }
    var newTypeName by remember { mutableStateOf("") }
    var newTypeError by remember { mutableStateOf<String?>(null) }

    val customTypes = remember(
        uiState.userSettings.customServiceTypes,
        uiState.serviceRecords
    ) {
        (
            uiState.userSettings.customServiceTypes +
                uiState.serviceRecords.map { it.customType }
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
                title = if (isFr) "Enregistrer un entretien" else "Log Maintenance Service",
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
                    (uiState.selectedVehicle?.displayName
                        ?: if (isFr) "Véhicule sélectionné" else "Selected Car"),
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
                                serviceDropdownExpanded = false
                            }
                        )
                    }
                    customTypes.forEach { custom ->
                        DropdownMenuItem(
                            text = { Text(custom) },
                            onClick = {
                                viewModel.updateCustomType(custom)
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = formState.odometer,
                    onValueChange = { viewModel.updateOdometer(it) },
                    label = {
                        Text(if (isFr) "Kilométrage ($distUnit) *" else "Odometer ($distUnit) *")
                    },
                    placeholder = { Text("e.g. ${uiState.selectedVehicle?.currentOdometer ?: 0}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = formState.cost,
                    onValueChange = { viewModel.updateCost(it) },
                    label = {
                        Text(if (isFr) "Coût ($currencySymbol) *" else "Cost ($currencySymbol) *")
                    },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = formState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text(if (isFr) "Description (facultatif)" else "Description (Optional)") },
                placeholder = {
                    Text(
                        if (isFr) {
                            "ex. Vidange huile synthétique 5W-30 + filtre"
                        } else {
                            "e.g. Full synthetic 5W-30 oil change + filter"
                        }
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = formState.parts,
                onValueChange = { viewModel.updateParts(it) },
                label = {
                    Text(
                        if (isFr) "Pièces remplacées (séparées par des virgules)"
                        else "Parts Replaced (comma separated)"
                    )
                },
                placeholder = {
                    Text(
                        if (isFr) {
                            "ex. Filtre à huile, Filtre à air, Plaquettes avant"
                        } else {
                            "e.g. Oil filter, Air filter, Brembo brake pads"
                        }
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = formState.notes,
                onValueChange = { viewModel.updateNotes(it) },
                label = {
                    Text(
                        if (isFr) "Garage / Mécanicien / Notes (facultatif)"
                        else "Garage / Mechanic / DIY Notes (Optional)"
                    )
                },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (formState.errorMessage != null) {
                Text(
                    text = formState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.saveServiceRecord(onSuccess = onNavigateBack) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaintenanceBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (isFr) "Enregistrer l'entretien" else "Save Service Record",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
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
}
