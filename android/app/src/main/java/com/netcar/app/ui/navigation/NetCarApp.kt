package com.netcar.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.netcar.app.ui.dashboard.DashboardScreen
import com.netcar.app.ui.dashboard.DashboardViewModel
import com.netcar.app.ui.expenses.AddExpenseScreen
import com.netcar.app.ui.expenses.ExpenseListScreen
import com.netcar.app.ui.expenses.ExpenseViewModel
import com.netcar.app.ui.fillups.AddFillUpScreen
import com.netcar.app.ui.fillups.FillUpListScreen
import com.netcar.app.ui.fillups.FillUpViewModel
import com.netcar.app.ui.maintenance.AddServiceScreen
import com.netcar.app.ui.maintenance.MaintenanceViewModel
import com.netcar.app.ui.maintenance.ServiceListScreen
import com.netcar.app.ui.reminders.AddReminderScreen
import com.netcar.app.ui.reminders.ReminderListScreen
import com.netcar.app.ui.reminders.ReminderViewModel
import com.netcar.app.ui.reports.ReportsScreen
import com.netcar.app.ui.reports.ReportsViewModel
import com.netcar.app.ui.settings.SettingsScreen
import com.netcar.app.ui.settings.SettingsViewModel
import com.netcar.app.ui.vehicles.AddVehicleScreen
import com.netcar.app.ui.vehicles.VehicleListScreen
import com.netcar.app.ui.vehicles.VehicleViewModel

@Composable
fun NetCarApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom bar on primary top-level tabs
    val bottomBarRoutes = listOf(
        Screen.Dashboard.route,
        Screen.Fuel.route,
        Screen.Expenses.route,
        Screen.Maintenance.route,
        Screen.Reminders.route
    )
    val showBottomBar = currentRoute in bottomBarRoutes

    // Obtain dashboard ViewModel to share badge count on bottom bar
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val dashboardUiState by dashboardViewModel.uiState.collectAsState()
    val overdueCount = dashboardUiState.stats.dueRemindersCount

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    navController = navController,
                    overdueRemindersCount = overdueCount
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Dashboard
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToVehicles = { navController.navigate(Screen.Vehicles.route) },
                    onNavigateToAddVehicle = { navController.navigate(Screen.AddVehicle.route) },
                    onNavigateToLogFuel = { navController.navigate(Screen.AddFillUp.route) },
                    onNavigateToLogExpense = { navController.navigate(Screen.AddExpense.route) },
                    onNavigateToLogService = { navController.navigate(Screen.AddService.route) },
                    onNavigateToReminders = { navController.navigate(Screen.Reminders.route) },
                    onNavigateToFuel = { navController.navigate(Screen.Fuel.route) },
                    onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            // 2. Vehicles
            composable(Screen.Vehicles.route) {
                val vehicleViewModel: VehicleViewModel = hiltViewModel()
                VehicleListScreen(
                    viewModel = vehicleViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddVehicle = { navController.navigate(Screen.AddVehicle.route) }
                )
            }

            composable(Screen.AddVehicle.route) {
                val vehicleViewModel: VehicleViewModel = hiltViewModel()
                AddVehicleScreen(
                    viewModel = vehicleViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 3. Fuel
            composable(Screen.Fuel.route) {
                val fillUpViewModel: FillUpViewModel = hiltViewModel()
                FillUpListScreen(
                    viewModel = fillUpViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddFillUp = { navController.navigate(Screen.AddFillUp.route) }
                )
            }

            composable(Screen.AddFillUp.route) {
                val fillUpViewModel: FillUpViewModel = hiltViewModel()
                AddFillUpScreen(
                    viewModel = fillUpViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 4. Expenses
            composable(Screen.Expenses.route) {
                val expenseViewModel: ExpenseViewModel = hiltViewModel()
                ExpenseListScreen(
                    viewModel = expenseViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddExpense = { navController.navigate(Screen.AddExpense.route) }
                )
            }

            composable(Screen.AddExpense.route) {
                val expenseViewModel: ExpenseViewModel = hiltViewModel()
                AddExpenseScreen(
                    viewModel = expenseViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 5. Maintenance
            composable(Screen.Maintenance.route) {
                val maintenanceViewModel: MaintenanceViewModel = hiltViewModel()
                ServiceListScreen(
                    viewModel = maintenanceViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddService = { navController.navigate(Screen.AddService.route) }
                )
            }

            composable(Screen.AddService.route) {
                val maintenanceViewModel: MaintenanceViewModel = hiltViewModel()
                AddServiceScreen(
                    viewModel = maintenanceViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 6. Reminders
            composable(Screen.Reminders.route) {
                val reminderViewModel: ReminderViewModel = hiltViewModel()
                ReminderListScreen(
                    viewModel = reminderViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddReminder = { navController.navigate(Screen.AddReminder.route) }
                )
            }

            composable(Screen.AddReminder.route) {
                val reminderViewModel: ReminderViewModel = hiltViewModel()
                AddReminderScreen(
                    viewModel = reminderViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 7. Reports
            composable(Screen.Reports.route) {
                val reportsViewModel: ReportsViewModel = hiltViewModel()
                ReportsScreen(
                    viewModel = reportsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 8. Settings
            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
