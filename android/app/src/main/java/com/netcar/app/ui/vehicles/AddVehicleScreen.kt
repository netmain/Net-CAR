package com.netcar.app.ui.vehicles

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.netcar.app.model.FuelType
import com.netcar.app.model.SupportedLanguage
import com.netcar.app.ui.components.TopBar
import com.netcar.app.ui.components.VehiclePhotoThumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleScreen(
    viewModel: VehicleViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formState by viewModel.formState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isFr = uiState.userSettings.language == SupportedLanguage.FR
    var fuelDropdownExpanded by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.updatePhotoUri(uri.toString())
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = if (isFr) "Ajouter un véhicule" else "Add Vehicle",
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
                text = if (isFr) "Photo du véhicule" else "Vehicle Photo",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            VehiclePhotoThumbnail(
                photoUrl = formState.photoUri.ifBlank { formState.photoUrl },
                iconSize = 56,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { photoPicker.launch("image/*") },
                    enabled = !formState.isSaving,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isFr) "Choisir une photo" else "Choose photo", maxLines = 1)
                }

                if (formState.photoUri.isNotBlank() || formState.photoUrl.isNotBlank()) {
                    TextButton(
                        onClick = { viewModel.removeFormPhoto() },
                        enabled = !formState.isSaving
                    ) {
                        Text(if (isFr) "Retirer" else "Remove")
                    }
                }
            }

            OutlinedTextField(
                value = formState.photoUrl,
                onValueChange = { viewModel.updatePhotoUrl(it) },
                label = {
                    Text(if (isFr) "URL de l'image (facultatif)" else "Image URL (Optional)")
                },
                placeholder = { Text("https://…") },
                singleLine = true,
                enabled = !formState.isSaving,
                trailingIcon = {
                    TextButton(
                        onClick = { viewModel.applyPhotoUrl(formState.photoUrl) },
                        enabled = formState.photoUrl.isNotBlank() &&
                            !formState.isPhotoBusy &&
                            !formState.isSaving
                    ) {
                        Text(if (isFr) "Vérifier" else "Check")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (formState.isPhotoBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (!formState.photoErrorMessage.isNullOrBlank()) {
                Text(
                    text = formState.photoErrorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = if (isFr) {
                    "L'envoi d'une photo nécessite une connexion Google ou Email."
                } else {
                    "Uploading a photo requires a Google or Email sign-in."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (isFr) "Détails du véhicule" else "Vehicle Details",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = formState.name,
                onValueChange = { viewModel.updateName(it) },
                label = {
                    Text(
                        if (isFr) "Surnom / Nom personnalisé (facultatif)"
                        else "Nickname / Friendly Name (Optional)"
                    )
                },
                placeholder = { Text(if (isFr) "ex. Voiture quotidienne" else "e.g. Daily Driver") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = formState.make,
                    onValueChange = { viewModel.updateMake(it) },
                    label = { Text(if (isFr) "Marque *" else "Make *") },
                    placeholder = { Text(if (isFr) "ex. Peugeot" else "e.g. Peugeot") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = formState.model,
                    onValueChange = { viewModel.updateModel(it) },
                    label = { Text(if (isFr) "Modèle *" else "Model *") },
                    placeholder = { Text(if (isFr) "ex. 208" else "e.g. 208") },
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
                    value = formState.year,
                    onValueChange = { viewModel.updateYear(it) },
                    label = { Text(if (isFr) "Année" else "Year") },
                    placeholder = { Text("2023") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = formState.plate,
                    onValueChange = { viewModel.updatePlate(it) },
                    label = {
                        Text(if (isFr) "Immatriculation (facultatif)" else "License Plate (Optional)")
                    },
                    placeholder = { Text("AB-123-CD") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
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
                            text = {
                                Text(if (isFr) fuel.displayNameFr else fuel.displayName)
                            },
                            onClick = {
                                viewModel.updateFuelType(fuel)
                                fuelDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = formState.odometer,
                onValueChange = { viewModel.updateOdometer(it) },
                label = {
                    Text(
                        if (isFr) "Kilométrage actuel (km / mi)"
                        else "Current Odometer (km / mi)"
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = formState.vin,
                onValueChange = { viewModel.updateVin(it) },
                label = {
                    Text(
                        if (isFr) "VIN (numéro d'identification du véhicule)"
                        else "VIN (Vehicle Identification Number)"
                    )
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = formState.notes,
                onValueChange = { viewModel.updateNotes(it) },
                label = { Text(if (isFr) "Notes (facultatif)" else "Notes (Optional)") },
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
                onClick = { viewModel.saveVehicle(onSuccess = onNavigateBack) },
                enabled = !formState.isSaving,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (formState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isFr) "Envoi de la photo…" else "Uploading photo…",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                } else {
                    Text(
                        text = if (isFr) "Enregistrer le véhicule" else "Save Vehicle",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
