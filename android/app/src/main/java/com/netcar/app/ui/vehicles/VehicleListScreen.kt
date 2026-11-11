package com.netcar.app.ui.vehicles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netcar.app.model.SupportedLanguage
import com.netcar.app.model.Vehicle
import com.netcar.app.ui.components.TopBar
import com.netcar.app.ui.components.VehiclePhotoDialog
import com.netcar.app.ui.components.VehiclePhotoThumbnail
import com.netcar.app.ui.theme.OverdueRed
import com.netcar.app.ui.theme.OverdueRedContainer
import java.text.NumberFormat
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun VehicleListScreen(
    viewModel: VehicleViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddVehicle: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isFr = uiState.userSettings.language == SupportedLanguage.FR
    val distUnit = uiState.userSettings.units.distUnit
    var editingPhotoVehicleId by remember { mutableStateOf<String?>(null) }
    val editingPhotoVehicle = uiState.vehicles.find { it.id == editingPhotoVehicleId }

    Scaffold(
        topBar = {
            TopBar(
                title = if (isFr) "Mes véhicules" else "My Vehicles",
                canNavigateBack = onOpenDrawer == null,
                onNavigateBack = onNavigateBack,
                onOpenDrawer = onOpenDrawer
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddVehicle,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (isFr) "Ajouter un véhicule" else "Add Vehicle"
                )
            }
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val photoError = uiState.photoState.errorMessage
            if (!photoError.isNullOrBlank() && editingPhotoVehicle == null) {
                item(key = "photo_error") {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = OverdueRedContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = photoError,
                                style = MaterialTheme.typography.bodySmall,
                                color = OverdueRed,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.clearPhotoMessage() }) {
                                Text(
                                    text = if (isFr) "Fermer" else "Dismiss",
                                    color = OverdueRed
                                )
                            }
                        }
                    }
                }
            }

            items(uiState.vehicles, key = { it.id }) { vehicle ->
                val isSelected = vehicle.id == uiState.selectedVehicleId
                VehicleCard(
                    vehicle = vehicle,
                    isSelected = isSelected,
                    isFr = isFr,
                    distUnit = distUnit,
                    onSelect = { viewModel.selectVehicle(vehicle.id) },
                    onChangePhoto = { editingPhotoVehicleId = vehicle.id },
                    onDelete = { viewModel.deleteVehicle(vehicle.id) }
                )
            }

            if (uiState.vehicles.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isFr) "Aucun véhicule ajouté" else "No vehicles added yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isFr) {
                                "Appuyez sur + pour ajouter votre premier véhicule."
                            } else {
                                "Tap the + button to add your first vehicle."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (editingPhotoVehicle != null) {
        VehiclePhotoDialog(
            vehicleName = editingPhotoVehicle.displayName,
            photoUrl = editingPhotoVehicle.photoUrl,
            isFr = isFr,
            isBusy = uiState.photoState.isBusy &&
                uiState.photoState.vehicleId == editingPhotoVehicle.id,
            errorMessage = uiState.photoState.errorMessage,
            onDismiss = {
                editingPhotoVehicleId = null
                viewModel.clearPhotoMessage()
            },
            onPickPhoto = { uri -> viewModel.uploadVehiclePhoto(editingPhotoVehicle.id, uri) },
            onApplyUrl = { url -> viewModel.applyVehiclePhotoUrl(editingPhotoVehicle.id, url) },
            onRemove = { viewModel.removeVehiclePhoto(editingPhotoVehicle.id) }
        )
    }
}

@Composable
private fun VehicleCard(
    vehicle: Vehicle,
    isSelected: Boolean,
    isFr: Boolean,
    distUnit: String,
    onSelect: () -> Unit,
    onChangePhoto: () -> Unit,
    onDelete: () -> Unit
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    VehiclePhotoThumbnail(
                        photoUrl = vehicle.photoUrl,
                        iconSize = 26,
                        modifier = Modifier.size(52.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = vehicle.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${vehicle.year} • ${vehicle.make}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onChangePhoto) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = if (isFr) "Modifier la photo" else "Change photo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = if (isFr) "Actif" else "Active",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFr) "Actif" else "Active",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                } else {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = if (isFr) "Supprimer" else "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp
                    ) {
                        Text(
                            text = vehicle.plate,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (isFr) vehicle.fuelType.displayNameFr else vehicle.fuelType.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${numberFormat.format(vehicle.currentOdometer)} $distUnit",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (vehicle.vin.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = (if (isFr) "VIN : " else "VIN: ") + vehicle.vin,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}
