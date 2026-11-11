package com.netcar.app.ui.fillups

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.netcar.app.model.FuelType
import com.netcar.app.model.SupportedLanguage
import com.netcar.app.ui.components.TopBar
import com.netcar.app.ui.theme.FuelGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFillUpScreen(
    viewModel: FillUpViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formState by viewModel.formState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isFr = uiState.userSettings.language == SupportedLanguage.FR
    val currencySymbol = uiState.userSettings.currency.symbol
    val distUnit = uiState.userSettings.units.distUnit
    val volUnit = uiState.userSettings.units.volUnit
    var fuelDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopBar(
                title = if (isFr) "Enregistrer un plein" else "Log Fill-up",
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

            OutlinedTextField(
                value = formState.odometer,
                onValueChange = { viewModel.updateOdometer(it) },
                label = {
                    Text(
                        if (isFr) "Kilométrage ($distUnit) *"
                        else "Odometer Reading ($distUnit) *"
                    )
                },
                placeholder = {
                    Text(
                        (if (isFr) "Actuel : " else "Current: ") +
                            "${uiState.selectedVehicle?.currentOdometer ?: 0}"
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = formState.pricePerUnit,
                    onValueChange = { viewModel.updatePricePerUnit(it) },
                    label = {
                        Text(
                            (if (isFr) "Prix" else "Price") + " ($currencySymbol / $volUnit)"
                        )
                    },
                    placeholder = { Text(if (isFr) "ex. 29,00" else "e.g. 29.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = formState.volume,
                    onValueChange = { viewModel.updateVolume(it) },
                    label = {
                        Text(
                            if (formState.volume.isNotBlank() &&
                                formState.totalCost.isNotBlank() &&
                                formState.pricePerUnit.isNotBlank()
                            ) {
                                if (isFr) "Volume (auto)" else "Volume (Auto)"
                            } else {
                                "Volume ($volUnit) *"
                            }
                        )
                    },
                    placeholder = {
                        Text(if (isFr) "Calculé automatiquement" else "Auto from total")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = formState.totalCost,
                onValueChange = { viewModel.updateTotalCost(it) },
                label = {
                    Text(
                        (if (isFr) "Total payé" else "Total Paid") + " ($currencySymbol) *"
                    )
                },
                placeholder = {
                    Text(
                        if (isFr) "ex. 2000 (calcule le volume)"
                        else "e.g. 2000 (calculates volume)"
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Full Tank Switch Card
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
                            text = if (isFr) "Plein complet" else "Full Tank",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = if (isFr) {
                                "Requis pour un calcul précis de la consommation"
                            } else {
                                "Required for accurate consumption calculations"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = formState.isFullTank,
                        onCheckedChange = { viewModel.updateIsFullTank(it) }
                    )
                }
            }

            // Fuel Type Dropdown
            ExposedDropdownMenuBox(
                expanded = fuelDropdownExpanded,
                onExpandedChange = { fuelDropdownExpanded = !fuelDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = if (isFr) formState.fuelType.displayNameFr else formState.fuelType.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (isFr) "Type de carburant" else "Fuel Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = fuelDropdownExpanded,
                    onDismissRequest = { fuelDropdownExpanded = false }
                ) {
                    FuelType.values().forEach { fuel ->
                        DropdownMenuItem(
                            text = { Text(if (isFr) fuel.displayNameFr else fuel.displayName) },
                            onClick = {
                                viewModel.updateFuelType(fuel)
                                fuelDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = formState.station,
                onValueChange = { viewModel.updateStation(it) },
                label = {
                    Text(
                        if (isFr) "Nom de la station (facultatif)"
                        else "Fuel Station Name (Optional)"
                    )
                },
                placeholder = {
                    Text(if (isFr) "ex. TotalEnergies, Shell, BP" else "e.g. TotalEnergies, Shell, BP")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = formState.notes,
                onValueChange = { viewModel.updateNotes(it) },
                label = { Text(if (isFr) "Notes (facultatif)" else "Notes (Optional)") },
                minLines = 2,
                maxLines = 4,
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
                onClick = { viewModel.saveFillUp(onSuccess = onNavigateBack) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FuelGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (isFr) "Enregistrer le plein" else "Save Fill-Up",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
