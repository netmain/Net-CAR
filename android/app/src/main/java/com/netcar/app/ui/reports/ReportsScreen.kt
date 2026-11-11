package com.netcar.app.ui.reports

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netcar.app.model.SupportedLanguage
import com.netcar.app.ui.components.StatCard
import com.netcar.app.ui.components.TopBar
import com.netcar.app.ui.components.formatMoney
import com.netcar.app.ui.theme.ExpenseAmber
import com.netcar.app.ui.theme.ExpenseAmberContainer
import com.netcar.app.ui.theme.FuelGreen
import com.netcar.app.ui.theme.FuelGreenContainer
import com.netcar.app.ui.theme.MaintenanceBlue
import com.netcar.app.ui.theme.MaintenanceBlueContainer
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isFr = uiState.userSettings.language == SupportedLanguage.FR
    val currencySymbol = uiState.userSettings.currency.symbol
    val distUnit = uiState.userSettings.units.distUnit
    val consUnit = uiState.userSettings.units.consUnit
    val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

    Scaffold(
        topBar = {
            TopBar(
                title = if (isFr) "Rapports et analyses" else "Reports & Analytics",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
            )
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
            // Period Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ReportPeriod.values()) { period ->
                        FilterChip(
                            selected = uiState.selectedPeriod == period,
                            onClick = { viewModel.selectPeriod(period) },
                            label = { Text(if (isFr) period.labelFr else period.label) }
                        )
                    }
                }
            }

            // Summary Stats 2x2
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = if (isFr) "Dépenses totales" else "Total Spend",
                            value = formatMoney(currencySymbol, uiState.totalSpend, decimals = 0),
                            icon = Icons.Default.Payments,
                            iconTint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )

                        StatCard(
                            title = (if (isFr) "Coût par " else "Cost per ") + distUnit,
                            value = formatMoney(currencySymbol, uiState.costPerKm),
                            unit = "/$distUnit",
                            icon = Icons.Default.Speed,
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
                            title = if (isFr) "Consommation moy." else "Avg Consumption",
                            value = String.format(Locale.US, "%.1f", uiState.avgConsumption),
                            unit = consUnit,
                            icon = Icons.Default.LocalGasStation,
                            iconTint = FuelGreen,
                            iconContainerColor = FuelGreenContainer,
                            modifier = Modifier.weight(1f)
                        )

                        StatCard(
                            title = if (isFr) "Distance" else "Distance",
                            value = numberFormat.format(uiState.totalDistance),
                            unit = distUnit,
                            icon = Icons.Default.Speed,
                            iconTint = MaintenanceBlue,
                            iconContainerColor = MaintenanceBlueContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Cost Distribution Breakdown Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isFr) "Répartition des coûts" else "Cost Distribution",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Segmented Distribution Bar
                        if (uiState.totalSpend > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            ) {
                                if (uiState.fuelPercentage > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(uiState.fuelPercentage)
                                            .fillMaxSize()
                                            .background(FuelGreen)
                                    )
                                }
                                if (uiState.maintenancePercentage > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(uiState.maintenancePercentage)
                                            .fillMaxSize()
                                            .background(MaintenanceBlue)
                                    )
                                }
                                if (uiState.expensesPercentage > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(uiState.expensesPercentage)
                                            .fillMaxSize()
                                            .background(ExpenseAmber)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3 Main pillars
                        DistributionPillar(
                            title = if (isFr) "Carburant" else "Fuel",
                            amount = formatMoney(currencySymbol, uiState.fuelSpend),
                            percentage = (uiState.fuelPercentage * 100).toInt(),
                            color = FuelGreen,
                            icon = Icons.Default.LocalGasStation
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        DistributionPillar(
                            title = if (isFr) "Entretien" else "Maintenance",
                            amount = formatMoney(currencySymbol, uiState.maintenanceSpend),
                            percentage = (uiState.maintenancePercentage * 100).toInt(),
                            color = MaintenanceBlue,
                            icon = Icons.Default.Build
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        DistributionPillar(
                            title = if (isFr) {
                                "Dépenses (stationnement, péage, lavage)"
                            } else {
                                "Expenses (Parking, Tolls, Wash)"
                            },
                            amount = formatMoney(currencySymbol, uiState.expensesSpend),
                            percentage = (uiState.expensesPercentage * 100).toInt(),
                            color = ExpenseAmber,
                            icon = Icons.Default.Receipt
                        )
                    }
                }
            }

            // Detailed Category Breakdown
            if (uiState.categoryBreakdown.isNotEmpty()) {
                item {
                    Text(
                        text = if (isFr) "Détail par catégorie" else "Detailed Category Breakdown",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(uiState.categoryBreakdown) { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatMoney(currencySymbol, item.amount),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "${(item.percentage * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DistributionPillar(
    title: String,
    amount: String,
    percentage: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
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
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = amount,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = color.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = color,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
