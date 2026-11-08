package com.netcar.app.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netcar.app.data.NetCarRepository
import com.netcar.app.model.Expense
import com.netcar.app.model.ExpenseCategory
import com.netcar.app.model.UserSettings
import com.netcar.app.model.Vehicle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ExpenseUiState(
    val expenses: List<Expense> = emptyList(),
    val filteredExpenses: List<Expense> = emptyList(),
    val selectedCategory: ExpenseCategory? = null,
    val selectedVehicle: Vehicle? = null,
    val totalExpenses: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val userSettings: UserSettings = UserSettings(),
    val isLoading: Boolean = false
)

data class AddExpenseFormState(
    val category: ExpenseCategory = ExpenseCategory.PARKING,
    val amount: String = "",
    val description: String = "",
    val odometer: String = "",
    val notes: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val repository: NetCarRepository
) : ViewModel() {

    private val _selectedCategoryFilter = MutableStateFlow<ExpenseCategory?>(null)
    val selectedCategoryFilter: StateFlow<ExpenseCategory?> = _selectedCategoryFilter.asStateFlow()

    val uiState: StateFlow<ExpenseUiState> = combine(
        repository.expenses,
        repository.vehicles,
        repository.selectedVehicleId,
        _selectedCategoryFilter,
        repository.userSettings
    ) { expenses, vehicles, selectedId, filterCat, settings ->
        val selected = vehicles.find { it.id == selectedId } ?: vehicles.firstOrNull()
        val vehicleExpenses = expenses
            .filter { it.vehicleId == selected?.id }
            .sortedByDescending { it.date }

        val filtered = if (filterCat != null) {
            vehicleExpenses.filter { it.category == filterCat }
        } else {
            vehicleExpenses
        }

        val total = vehicleExpenses.sumOf { it.amount }
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 86400000L)
        val monthly = vehicleExpenses.filter { it.date >= thirtyDaysAgo }.sumOf { it.amount }

        ExpenseUiState(
            expenses = vehicleExpenses,
            filteredExpenses = filtered,
            selectedCategory = filterCat,
            selectedVehicle = selected,
            totalExpenses = total,
            monthlyExpenses = monthly,
            userSettings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExpenseUiState(isLoading = true)
    )

    private val _formState = MutableStateFlow(AddExpenseFormState())
    val formState: StateFlow<AddExpenseFormState> = _formState.asStateFlow()

    fun setCategoryFilter(category: ExpenseCategory?) {
        _selectedCategoryFilter.value = category
    }

    fun updateCategory(category: ExpenseCategory) = _formState.apply { value = value.copy(category = category) }
    fun updateAmount(amount: String) = _formState.apply { value = value.copy(amount = amount) }
    fun updateDescription(desc: String) = _formState.apply { value = value.copy(description = desc) }
    fun updateOdometer(odo: String) = _formState.apply { value = value.copy(odometer = odo) }
    fun updateNotes(notes: String) = _formState.apply { value = value.copy(notes = notes) }

    fun deleteExpense(id: String) {
        repository.deleteExpense(id)
    }

    fun saveExpense(onSuccess: () -> Unit) {
        val state = _formState.value
        val vehicle = uiState.value.selectedVehicle ?: return

        val amountDouble = state.amount.toDoubleOrNull()
        if (amountDouble == null || amountDouble <= 0) {
            _formState.value = state.copy(errorMessage = "Please enter a valid amount")
            return
        }

        if (state.description.isBlank()) {
            _formState.value = state.copy(errorMessage = "Please enter a short description")
            return
        }

        val odoInt = state.odometer.toIntOrNull()

        val expense = Expense(
            vehicleId = vehicle.id,
            category = state.category,
            amount = amountDouble,
            description = state.description.trim(),
            odometer = odoInt,
            notes = state.notes.trim()
        )

        repository.addExpense(expense)
        _formState.value = AddExpenseFormState()
        onSuccess()
    }
}
