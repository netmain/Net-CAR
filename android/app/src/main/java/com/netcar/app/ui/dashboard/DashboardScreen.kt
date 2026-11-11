package com.netcar.app.ui.dashboard

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netcar.app.model.FillUp
import com.netcar.app.model.SupportedLanguage
import com.netcar.app.model.Vehicle
import com.netcar.app.ui.components.ReminderCard
import com.netcar.app.ui.components.StatCard
import com.netcar.app.ui.components.TopBar
import com.netcar.app.ui.components.VehiclePhotoDialog
import com.netcar.app.ui.components.VehiclePhotoThumbnail
import com.netcar.app.ui.theme.ExpenseAmber
import com.netcar.app.ui.theme.ExpenseAmberContainer
import com.netcar.app.ui.theme.FuelGreen
import com.netcar.app.ui.theme.FuelGreenContainer
import com.netcar.app.ui.theme.MaintenanceBlue
import com.netcar.app.ui.theme.MaintenanceBlueContainer
import com.netcar.app.ui.theme.OverdueRed
import com.netcar.app.ui.theme.OverdueRedContainer
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToVehicles: () -> Unit,
    onNavigateToAddVehicle: () -> Unit,
    onNavigateToLogFuel: () -> Unit,
    onNavigateToLogExpense: () -> Unit,
    onNavigateToLogService: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToFuel: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isFr = uiState.userSettings.language == SupportedLanguage.FR
    val currencySymbol = uiState.userSettings.currency.symbol
    val distUnit = uiState.userSettings.units.distUnit
    val consUnit = uiState.userSettings.units.consUnit
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    var showPhotoDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopBar(
            title = "Net-CAR",
            onOpenDrawer = onOpenDrawer,
            actions = {
                IconButton(onClick = onNavigateToVehicles) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = if (isFr) "Véhicules" else "Vehicles",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onNavigateToReports) {
                    Icon(
                        imageVector = Icons.Outlined.BarChart,
                        contentDescription = if (isFr) "Rapports" else "Reports",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = if (isFr) "Paramètres" else "Settings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Vehicle Switcher Header
            item {
                VehicleSwitcherCard(
                    selectedVehicle = uiState.selectedVehicle,
                    vehicles = uiState.vehicles,
                    isFr = isFr,
                    onSelectVehicle = { viewModel.selectVehicle(it) },
                    onManageVehicles = onNavigateToVehicles,
                    onAddVehicle = onNavigateToAddVehicle,
                    onChangePhoto = { showPhotoDialog = true },
                    distUnit = distUnit
                )
            }

            // 2. 4 Stat Cards Grid (2x2)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = if (isFr) "Consommation moy." else "Avg Consumption",
                            value = String.format(Locale.US, "%.1f", uiState.stats.averageConsumption),
                            unit = consUnit,
                            icon = Icons.Default.Speed,
                            trend = if (isFr) "-0,2 vs mois dernier" else "-0.2 vs last month",
                            trendPositive = true,
                            iconTint = FuelGreen,
                            iconContainerColor = FuelGreenContainer,
                            modifier = Modifier.weight(1f)
                        )

                        StatCard(
                            title = if (isFr) "Coût mensuel" else "Monthly Cost",
                            value = "$currencySymbol${String.format(Locale.US, "%.2f", uiState.stats.totalMonthlyCost)}",
                            unit = "",
                            icon = Icons.Default.Payments,
                            trend = if (isFr) "+5,4 % ce mois" else "+5.4% this mo",
                            trendPositive = false,
                            iconTint = ExpenseAmber,
                            iconContainerColor = ExpenseAmberContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = if (isFr) "Rappels dus" else "Reminders Due",
                            value = "${uiState.stats.dueRemindersCount}",
                            unit = if (isFr) {
                                if (uiState.stats.dueRemindersCount == 1) "alerte" else "alertes"
                            } else {
                                if (uiState.stats.dueRemindersCount == 1) "alert" else "alerts"
                            },
                            icon = Icons.Default.NotificationsActive,
                            iconTint = if (uiState.stats.dueRemindersCount > 0) OverdueRed else MaterialTheme.colorScheme.primary,
                            iconContainerColor = if (uiState.stats.dueRemindersCount > 0) OverdueRedContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            onClick = onNavigateToReminders,
                            modifier = Modifier.weight(1f)
                        )

                        StatCard(
                            title = if (isFr) "Compteur" else "Odometer",
                            value = numberFormat.format(uiState.stats.currentOdometer),
                            unit = distUnit,
                            icon = Icons.Default.DirectionsCar,
                            iconTint = MaintenanceBlue,
                            iconContainerColor = MaintenanceBlueContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Quick Action Buttons
            item {
                Column {
                    Text(
                        text = if (isFr) "Actions rapides" else "Quick Actions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionButton(
                            title = if (isFr) "Carburant" else "Log Fuel",
                            icon = Icons.Default.LocalGasStation,
                            containerColor = FuelGreenContainer,
                            contentColor = FuelGreen,
                            onClick = onNavigateToLogFuel,
                            modifier = Modifier.weight(1f)
                        )

                        QuickActionButton(
                            title = if (isFr) "Dépense" else "Log Expense",
                            icon = Icons.Default.Receipt,
                            containerColor = ExpenseAmberContainer,
                            contentColor = ExpenseAmber,
                            onClick = onNavigateToLogExpense,
                            modifier = Modifier.weight(1f)
                        )

                        QuickActionButton(
                            title = if (isFr) "Entretien" else "Log Service",
                            icon = Icons.Default.Build,
                            containerColor = MaintenanceBlueContainer,
                            contentColor = MaintenanceBlue,
                            onClick = onNavigateToLogService,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Upcoming Reminders Preview
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFr) "Rappels à venir" else "Upcoming Reminders",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onNavigateToReminders) {
                        Text(
                            text = if (isFr) "Voir tout" else "View all",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            if (uiState.upcomingReminders.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isFr) {
                                "Aucun rappel en attente pour ce véhicule. Tout est à jour !"
                            } else {
                                "No pending reminders for this vehicle. You are all set!"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(uiState.upcomingReminders) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        currentOdometer = uiState.stats.currentOdometer,
                        onComplete = { viewModel.completeReminder(reminder.id) }
                    )
                }
            }

            // 5. Recent Fill-ups Preview
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFr) "Pleins récents" else "Recent Fill-ups",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onNavigateToFuel) {
                        Text(
                            text = if (isFr) "Voir tout" else "View all",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            if (uiState.recentFillUps.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isFr) {
                                "Aucun historique de plein enregistré."
                            } else {
                                "No fill-up history recorded yet."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(uiState.recentFillUps) { fillUp ->
                    RecentFillUpCard(
                        fillUp = fillUp,
                        isFr = isFr,
                        currencySymbol = currencySymbol,
                        distUnit = distUnit,
                        volUnit = uiState.userSettings.units.volUnit
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    val photoVehicle = uiState.selectedVehicle
    if (showPhotoDialog && photoVehicle != null) {
        VehiclePhotoDialog(
            vehicleName = photoVehicle.displayName,
            photoUrl = photoVehicle.photoUrl,
            isFr = isFr,
            isBusy = uiState.photoState.isBusy &&
                uiState.photoState.vehicleId == photoVehicle.id,
            errorMessage = uiState.photoState.errorMessage,
            onDismiss = {
                showPhotoDialog = false
                viewModel.clearPhotoMessage()
            },
            onPickPhoto = { uri -> viewModel.uploadVehiclePhoto(photoVehicle.id, uri) },
            onApplyUrl = { url -> viewModel.applyVehiclePhotoUrl(photoVehicle.id, url) },
            onRemove = { viewModel.removeVehiclePhoto(photoVehicle.id) }
        )
    }
}

@Composable
private fun VehicleSwitcherCard(
    selectedVehicle: Vehicle?,
    vehicles: List<Vehicle>,
    isFr: Boolean,
    onSelectVehicle: (String) -> Unit,
    onManageVehicles: () -> Unit,
    onAddVehicle: () -> Unit,
    onChangePhoto: () -> Unit,
    distUnit: String
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VehiclePhotoThumbnail(
                    photoUrl = selectedVehicle?.photoUrl.orEmpty(),
                    iconSize = 26,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { dropdownExpanded = true }
                                .padding(4.dp)
                        ) {
                            Text(
                                text = selectedVehicle?.displayName
                                    ?: if (isFr) "Aucun véhicule" else "No vehicle",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = if (isFr) "Changer de véhicule" else "Switch Vehicle",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            vehicles.forEach { vehicle ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = vehicle.displayName,
                                                fontWeight = if (vehicle.id == selectedVehicle?.id) FontWeight.Bold else FontWeight.Normal
                                            )
                                            Text(
                                                text = "${vehicle.plate} • ${numberFormat.format(vehicle.currentOdometer)} $distUnit",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        onSelectVehicle(vehicle.id)
                                        dropdownExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(if (isFr) "+ Ajouter un véhicule" else "+ Add New Vehicle")
                                },
                                onClick = {
                                    dropdownExpanded = false
                                    onAddVehicle()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp
                ) {
                    Text(
                        text = selectedVehicle?.plate ?: "",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = when {
                                selectedVehicle == null -> if (isFr) "Carburant" else "Fuel"
                                isFr -> selectedVehicle.fuelType.displayNameFr
                                else -> selectedVehicle.fuelType.displayName
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (isFr) {
                            "Modèle ${selectedVehicle?.year ?: ""}"
                        } else {
                            "${selectedVehicle?.year ?: ""} Model"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                Text(
                    text = "${numberFormat.format(selectedVehicle?.currentOdometer ?: 0)} $distUnit",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            TextButton(
                onClick = onChangePhoto,
                enabled = selectedVehicle != null,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isFr) "Modifier la photo" else "Change photo",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RecentFillUpCard(
    fillUp: FillUp,
    isFr: Boolean,
    currencySymbol: String,
    distUnit: String,
    volUnit: String
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(FuelGreenContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalGasStation,
                        contentDescription = null,
                        tint = FuelGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = when {
                            fillUp.station.isNotBlank() -> fillUp.station
                            isFr -> "Plein"
                            else -> "Fill-up"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${dateFormatter.format(Date(fillUp.date))} • ${numberFormat.format(fillUp.odometer)} $distUnit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$currencySymbol${String.format(Locale.US, "%.2f", fillUp.totalCost)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${fillUp.volume} $volUnit @ $currencySymbol${fillUp.pricePerUnit}/$volUnit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
