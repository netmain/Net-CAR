package com.netcar.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netcar.app.model.SupportedLanguage
import com.netcar.app.model.UserSettings
import com.netcar.app.model.Vehicle
import com.netcar.app.ui.theme.ExpenseAmber
import com.netcar.app.ui.theme.ExpenseAmberContainer
import com.netcar.app.ui.theme.FuelGreen
import com.netcar.app.ui.theme.OverdueRed
import com.netcar.app.ui.theme.SuccessGreen
import com.netcar.app.ui.theme.SuccessGreenContainer
import java.text.NumberFormat
import java.util.Locale

private data class DrawerMenuItem(
    val screen: Screen,
    val labelFr: String,
    val labelEn: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun NetCarDrawerSheet(
    currentRoute: String?,
    selectedVehicle: Vehicle?,
    userSettings: UserSettings,
    overdueRemindersCount: Int,
    onNavigate: (String) -> Unit,
    onQuickLogFuel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFr = userSettings.language == SupportedLanguage.FR
    val isCloudConnected = userSettings.isDriveBackupConnected && !userSettings.connectedEmail.isNullOrBlank()
    val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

    val menuItems = listOf(
        DrawerMenuItem(
            screen = Screen.Dashboard,
            labelFr = "Tableau de bord",
            labelEn = "Dashboard",
            icon = Icons.Default.Dashboard
        ),
        DrawerMenuItem(
            screen = Screen.Vehicles,
            labelFr = "Mes Véhicules",
            labelEn = "My Vehicles",
            icon = Icons.Default.DirectionsCar
        ),
        DrawerMenuItem(
            screen = Screen.Fuel,
            labelFr = "Carburant & Pleins",
            labelEn = "Fuel Log",
            icon = Icons.Default.LocalGasStation
        ),
        DrawerMenuItem(
            screen = Screen.Expenses,
            labelFr = "Dépenses",
            labelEn = "Expenses",
            icon = Icons.Default.Receipt
        ),
        DrawerMenuItem(
            screen = Screen.Maintenance,
            labelFr = "Entretien & Services",
            labelEn = "Maintenance",
            icon = Icons.Default.Build
        ),
        DrawerMenuItem(
            screen = Screen.Reminders,
            labelFr = "Rappels & Alertes",
            labelEn = "Reminders",
            icon = Icons.Default.Notifications,
            badgeCount = overdueRemindersCount
        ),
        DrawerMenuItem(
            screen = Screen.Reports,
            labelFr = "Rapports & Analyses",
            labelEn = "Reports & Analytics",
            icon = Icons.Default.BarChart
        ),
        DrawerMenuItem(
            screen = Screen.Settings,
            labelFr = "Paramètres & Cloud",
            labelEn = "Settings & Cloud",
            icon = Icons.Default.Settings
        )
    )

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.width(310.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // 1. Brand Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Net-CAR",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "VEHICLE TELEMETRY OS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Active Vehicle Summary Card
            if (selectedVehicle != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigate(Screen.Vehicles.route) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedVehicle.displayName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = selectedVehicle.plate,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${numberFormat.format(selectedVehicle.currentOdometer)} ${userSettings.units.distUnit} • ${selectedVehicle.fuelType.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // 3. Honest Cloud Sync Status Pill (Tapping opens Settings & Cloud)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isCloudConnected) SuccessGreenContainer else ExpenseAmberContainer.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onNavigate(Screen.Settings.route) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = if (isCloudConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (isCloudConnected) SuccessGreen else ExpenseAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isCloudConnected) {
                                if (isFr) "Cloud Synchronisé" else "Cloud Connected"
                            } else {
                                if (isFr) "Cloud non connecté" else "Cloud Not Connected"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isCloudConnected) SuccessGreen else ExpenseAmber
                        )
                        Text(
                            text = if (isCloudConnected) {
                                userSettings.connectedEmail ?: ""
                            } else {
                                if (isFr) "Appuyez pour connecter un compte" else "Tap to sign in & sync"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Spacer(modifier = Modifier.height(12.dp))

            // 4. Main Navigation Items
            Text(
                text = if (isFr) "MENU PRINCIPAL" else "MAIN MENU",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            menuItems.forEach { item ->
                val isSelected = currentRoute == item.screen.route
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = if (isFr) item.labelFr else item.labelEn,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null
                        )
                    },
                    badge = if (item.badgeCount > 0) {
                        {
                            Badge(
                                containerColor = OverdueRed,
                                contentColor = MaterialTheme.colorScheme.onError
                            ) {
                                Text(text = "${item.badgeCount}")
                            }
                        }
                    } else null,
                    selected = isSelected,
                    onClick = { onNavigate(item.screen.route) },
                    shape = RoundedCornerShape(14.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(16.dp))

            // 5. Quick Action CTA Button at Bottom of Drawer
            Button(
                onClick = onQuickLogFuel,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FuelGreen,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isFr) "Ajouter un plein" else "Log Fuel Fill-Up",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
