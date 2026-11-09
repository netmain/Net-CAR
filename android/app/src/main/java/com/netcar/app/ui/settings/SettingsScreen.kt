package com.netcar.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.netcar.app.model.SupportedCurrency
import com.netcar.app.model.SupportedLanguage
import com.netcar.app.model.Units
import com.netcar.app.ui.components.TopBar
import com.netcar.app.ui.theme.ExpenseAmber
import com.netcar.app.ui.theme.ExpenseAmberContainer
import com.netcar.app.ui.theme.OverdueRed
import com.netcar.app.ui.theme.OverdueRedContainer
import com.netcar.app.ui.theme.SuccessGreen
import com.netcar.app.ui.theme.SuccessGreenContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings
    val isFr = settings.language == SupportedLanguage.FR
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    var emailInput by remember { mutableStateOf(settings.connectedEmail ?: "") }
    var passwordInput by remember { mutableStateOf("") }
    var isCreateAccountMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopBar(
                title = if (isFr) "Paramètres & Cloud" else "Settings & Cloud",
                canNavigateBack = onOpenDrawer == null,
                onNavigateBack = onNavigateBack,
                onOpenDrawer = onOpenDrawer
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
            // 1. Cloud Synchronization & Account Card (Placed at top so user can immediately see status & sign in)
            item {
                val isSyncing = uiState.isSyncing
                val isConnected = settings.isDriveBackupConnected && !settings.connectedEmail.isNullOrBlank()
                val hasError = uiState.syncErrorMessage != null

                val badgeBg = when {
                    isSyncing -> ExpenseAmberContainer
                    hasError -> OverdueRedContainer
                    isConnected -> SuccessGreenContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val badgeTint = when {
                    isSyncing -> ExpenseAmber
                    hasError -> OverdueRed
                    isConnected -> SuccessGreen
                    else -> ExpenseAmber
                }
                val badgeLabel = when {
                    isSyncing -> if (isFr) "Sync..." else "Syncing..."
                    hasError -> if (isFr) "Erreur" else "Sync Error"
                    isConnected -> if (isFr) "Connecté" else "Synced"
                    else -> if (isFr) "Non connecté" else "Not Connected"
                }
                val statusSubtitle = when {
                    isSyncing -> if (isFr) "Synchronisation Firestore en cours..." else "Synchronizing with Firestore..."
                    isConnected -> settings.connectedEmail ?: ""
                    else -> if (isFr) "Mode local uniquement — Connectez votre compte" else "Local mode only — Sign in to sync"
                }

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
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
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(badgeBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when {
                                            isSyncing -> Icons.Default.CloudSync
                                            isConnected -> Icons.Default.CloudDone
                                            else -> Icons.Default.CloudOff
                                        },
                                        contentDescription = null,
                                        tint = badgeTint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = if (isFr) "Synchronisation Cloud" else "Cloud Synchronization",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = statusSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isConnected) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = badgeBg
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(badgeTint)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = badgeLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = badgeTint
                                    )
                                }
                            }
                        }

                        if (isConnected && settings.lastBackupTimestamp != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = (if (isFr) "Dernière synchro : " else "Last synced: ") +
                                    dateFormatter.format(Date(settings.lastBackupTimestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!uiState.syncSuccessMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SuccessGreenContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = uiState.syncSuccessMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = SuccessGreen,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (!uiState.syncErrorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = OverdueRedContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = uiState.syncErrorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = OverdueRed,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (!isConnected) {
                            // Mode Switcher: Sign In vs Create Account
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = !isCreateAccountMode,
                                    onClick = {
                                        isCreateAccountMode = false
                                        viewModel.clearMessages()
                                    },
                                    label = { Text(if (isFr) "Se connecter" else "Sign In") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = isCreateAccountMode,
                                    onClick = {
                                        isCreateAccountMode = true
                                        viewModel.clearMessages()
                                    },
                                    label = { Text(if (isFr) "Créer un compte" else "Create Account") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text(if (isFr) "Adresse e-mail Cloud" else "Cloud Email Address") },
                                placeholder = { Text("barkat.ri36@gmail.com") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.MailOutline, contentDescription = null)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text(if (isFr) "Mot de passe (min. 6 caract.)" else "Password (min. 6 chars)") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                                },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    if (isCreateAccountMode) {
                                        viewModel.signUpWithEmail(emailInput, passwordInput)
                                    } else {
                                        viewModel.signInWithEmail(emailInput, passwordInput)
                                    }
                                },
                                enabled = !isSyncing,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCreateAccountMode) Icons.Default.PersonAdd else Icons.Default.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        isSyncing -> if (isFr) "Connexion & Synchro..." else "Connecting & Syncing..."
                                        isCreateAccountMode -> if (isFr) "Créer le compte & Synchroniser" else "Create Account & Sync"
                                        else -> if (isFr) "Se connecter & Synchroniser" else "Sign In & Sync Now"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            // Connected state actions: Sync Now + Sign Out
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.syncWithGoogleDrive() },
                                    enabled = !isSyncing,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isSyncing) {
                                            if (isFr) "Synchronisation..." else "Syncing..."
                                        } else {
                                            if (isFr) "Synchroniser" else "Sync Now"
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        passwordInput = ""
                                        viewModel.signOut()
                                    },
                                    enabled = !isSyncing,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isFr) "Déconnexion" else "Sign Out")
                                }
                            }
                        }
                    }
                }
            }

            // 2. Language Toggle
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Language / Langue",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SupportedLanguage.values().forEach { lang ->
                                val selected = settings.language == lang
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setLanguage(lang) },
                                    label = { Text(lang.displayName) },
                                    leadingIcon = if (selected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Units Toggle
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isFr) "Unités de mesure" else "Measurement Units",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Units.values().forEach { u ->
                                val selected = settings.units == u
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setUnits(u) },
                                    label = { Text("${u.label} (${u.distUnit}, ${u.volUnit})") },
                                    leadingIcon = if (selected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Currency Selector (Clean Chip Grid)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isFr) "Devise d'affichage" else "Display Currency",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SupportedCurrency.values().forEach { cur ->
                                val selected = settings.currency == cur
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setCurrency(cur) },
                                    label = { Text("${cur.code} (${cur.symbol})") },
                                    leadingIcon = if (selected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }

            // 5. About Section
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Net-CAR Telemetry OS",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isFr) {
                                "Version 2.0.0 • Synchronisation Cloud multi-appareils (Web & Android) via Firebase Auth & Firestore."
                            } else {
                                "Version 2.0.0 • Multi-device Cloud Sync (Web & Android) powered by Firebase Auth & Firestore."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
