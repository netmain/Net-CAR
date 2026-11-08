package com.netcar.app.ui.navigation

sealed class Screen(val route: String, val title: String) {
    data object Dashboard : Screen("dashboard", "Dashboard")
    data object Vehicles : Screen("vehicles", "Vehicles")
    data object AddVehicle : Screen("add_vehicle", "Add Vehicle")
    data object Fuel : Screen("fuel", "Fuel History")
    data object AddFillUp : Screen("add_fill_up", "Log Fuel")
    data object Expenses : Screen("expenses", "Expenses")
    data object AddExpense : Screen("add_expense", "Log Expense")
    data object Maintenance : Screen("maintenance", "Maintenance")
    data object AddService : Screen("add_service", "Log Service")
    data object Reminders : Screen("reminders", "Reminders")
    data object AddReminder : Screen("add_reminder", "Add Reminder")
    data object Reports : Screen("reports", "Reports")
    data object Settings : Screen("settings", "Settings")
}
