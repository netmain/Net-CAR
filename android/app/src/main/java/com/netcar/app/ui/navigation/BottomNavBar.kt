package com.netcar.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.netcar.app.ui.theme.OverdueRed

sealed class BottomNavItem(
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
    val labelFr: String
) {
    data object Dashboard : BottomNavItem(
        screen = Screen.Dashboard,
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        label = "Dashboard",
        labelFr = "Tableau de bord"
    )

    data object Fuel : BottomNavItem(
        screen = Screen.Fuel,
        selectedIcon = Icons.Filled.LocalGasStation,
        unselectedIcon = Icons.Outlined.LocalGasStation,
        label = "Fuel",
        labelFr = "Carburant"
    )

    data object Expenses : BottomNavItem(
        screen = Screen.Expenses,
        selectedIcon = Icons.Filled.Receipt,
        unselectedIcon = Icons.Outlined.Receipt,
        label = "Expenses",
        labelFr = "Dépenses"
    )

    data object Maintenance : BottomNavItem(
        screen = Screen.Maintenance,
        selectedIcon = Icons.Filled.Build,
        unselectedIcon = Icons.Outlined.Build,
        label = "Services",
        labelFr = "Entretien"
    )

    data object Reminders : BottomNavItem(
        screen = Screen.Reminders,
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications,
        label = "Reminders",
        labelFr = "Rappels"
    )
}

@Composable
fun BottomNavBar(
    navController: NavController,
    modifier: Modifier = Modifier,
    isFr: Boolean = false,
    overdueRemindersCount: Int = 0
) {
    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Fuel,
        BottomNavItem.Expenses,
        BottomNavItem.Maintenance,
        BottomNavItem.Reminders
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
            val itemLabel = if (isFr) item.labelFr else item.label

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    if (item == BottomNavItem.Reminders && overdueRemindersCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = OverdueRed,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ) {
                                    Text(overdueRemindersCount.toString())
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = itemLabel
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = itemLabel
                        )
                    }
                },
                label = {
                    Text(
                        text = itemLabel,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
