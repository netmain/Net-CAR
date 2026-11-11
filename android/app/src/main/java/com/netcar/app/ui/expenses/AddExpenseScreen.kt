package com.netcar.app.ui.expenses

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.netcar.app.model.ExpenseCategory
import com.netcar.app.model.SupportedLanguage
import com.netcar.app.ui.components.TopBar
import com.netcar.app.ui.theme.ExpenseAmber
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formState by viewModel.formState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val currencySymbol = uiState.userSettings.currency.symbol
    val distUnit = uiState.userSettings.units.distUnit
    val isFr = uiState.userSettings.language == SupportedLanguage.FR
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryError by remember { mutableStateOf<String?>(null) }

    val customCategories = remember(
        uiState.userSettings.customExpenseCategories,
        uiState.expenses
    ) {
        (
            uiState.userSettings.customExpenseCategories +
                uiState.expenses.map { it.customCategory }
            )
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.US) }
    }

    val selectedCategoryLabel = formState.customCategory.ifBlank {
        if (isFr) formState.category.displayNameFr else formState.category.displayName
    }

    Scaffold(
        topBar = {
            TopBar(
                title = if (isFr) "Enregistrer une dépense" else "Log Expense",
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

            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCategoryLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = {
                        Text(if (isFr) "Catégorie de dépense *" else "Expense Category *")
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    ExpenseCategory.values().forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(if (isFr) cat.displayNameFr else cat.displayName) },
                            onClick = {
                                viewModel.updateCategory(cat)
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                    customCategories.forEach { custom ->
                        DropdownMenuItem(
                            text = { Text(custom) },
                            onClick = {
                                viewModel.updateCustomCategory(custom)
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isFr) "＋ Ajouter une catégorie…" else "＋ Add a category…",
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {
                            categoryDropdownExpanded = false
                            newCategoryName = ""
                            newCategoryError = null
                            showAddCategoryDialog = true
                        }
                    )
                }
            }

            OutlinedTextField(
                value = formState.amount,
                onValueChange = { viewModel.updateAmount(it) },
                label = {
                    Text(if (isFr) "Montant ($currencySymbol) *" else "Amount ($currencySymbol) *")
                },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = formState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text(if (isFr) "Description *" else "Description *") },
                placeholder = {
                    Text(
                        if (isFr) {
                            "ex. Abonnement parking mensuel, lavage, péage autoroute"
                        } else {
                            "e.g. Monthly parking pass, car wash, highway toll"
                        }
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = formState.odometer,
                onValueChange = { viewModel.updateOdometer(it) },
                label = {
                    Text(
                        if (isFr) "Kilométrage actuel ($distUnit) (facultatif)"
                        else "Current Odometer ($distUnit) (Optional)"
                    )
                },
                placeholder = { Text("e.g. ${uiState.selectedVehicle?.currentOdometer ?: 0}") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                onClick = { viewModel.saveExpense(onSuccess = onNavigateBack) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseAmber),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (isFr) "Enregistrer la dépense" else "Save Expense",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text(if (isFr) "Nouvelle catégorie" else "New category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (isFr) {
                            "Ajoutez une catégorie de dépense personnalisée."
                        } else {
                            "Add a custom expense category."
                        }
                    )
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = {
                            newCategoryName = it
                            newCategoryError = null
                        },
                        singleLine = true,
                        label = { Text(if (isFr) "Nom de la catégorie" else "Category name") },
                        placeholder = {
                            Text(if (isFr) "ex. Location de voiture" else "e.g. Car rental")
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (newCategoryError != null) {
                        Text(
                            text = newCategoryError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clean = newCategoryName.trim().replace(Regex("\\s+"), " ")
                        if (clean.isBlank()) {
                            newCategoryError =
                                if (isFr) "Saisissez un nom de catégorie" else "Enter a category name"
                            return@TextButton
                        }
                        viewModel.addCustomExpenseCategory(clean)
                        showAddCategoryDialog = false
                    }
                ) {
                    Text(if (isFr) "Ajouter" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text(if (isFr) "Annuler" else "Cancel")
                }
            }
        )
    }
}
