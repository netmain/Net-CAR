package com.netcar.app.ui.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.netcar.app.model.SupportedCurrency
import com.netcar.app.model.SupportedLanguage
import com.netcar.app.model.Units
import com.netcar.app.ui.components.TopBar
import com.netcar.app.ui.theme.ExpenseAmber
import com.netcar.app.ui.theme.ExpenseAmberContainer
import com.netcar.app.ui.theme.FuelGreen
import com.netcar.app.ui.theme.FuelGreenContainer
import com.netcar.app.ui.theme.OverdueRed
import com.netcar.app.ui.theme.OverdueRedContainer
import com.netcar.app.ui.theme.SuccessGreen
import com.netcar.app.ui.theme.SuccessGreenContainer
import java.io.ByteArrayInputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val GOOGLE_WEB_CLIENT_ID =
    "1033623803537-9aqcjj37isscfde2aq3dp1t0nhj2hict.apps.googleusercontent.com"
private const val FIREBASE_AUTH_HANDLER_URL =
    "https://axiomatic-serenity-8lvvl.firebaseapp.com/__/auth/handler"

private fun extractIdTokenFromUrl(rawUrl: String?): String? {
    if (rawUrl.isNullOrBlank()) return null
    val fragmentOrQuery = rawUrl.substringAfter('#', "") + "&" + rawUrl.substringAfter('?', "").substringBefore('#')
    for (part in fragmentOrQuery.split('&')) {
        if (part.startsWith("id_token=")) {
            val encoded = part.removePrefix("id_token=").trim()
            if (encoded.isNotEmpty()) {
                return runCatching { URLDecoder.decode(encoded, "UTF-8") }.getOrDefault(encoded)
            }
        }
    }
    return null
}

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
    val context = LocalContext.current

    var emailInput by remember { mutableStateOf(settings.connectedEmail ?: "") }
    var passwordInput by remember { mutableStateOf("") }
    var isCreateAccountMode by remember { mutableStateOf(false) }
    var pendingCsvExportContent by remember { mutableStateOf<Pair<String, String>?>(null) }

    // CSV Import Launcher (OpenDocument supports all .csv MIME types)
    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val content = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    viewModel.importCsvContent(content)
                } else {
                    viewModel.reportCsvError(
                        if (isFr) "Le fichier CSV sélectionné est vide."
                        else "The selected CSV file is empty."
                    )
                }
            }.onFailure { err ->
                viewModel.reportCsvError(
                    err.localizedMessage ?: if (isFr) "Impossible de lire le fichier CSV." else "Could not read CSV file."
                )
            }
        }
    }

    // CSV Export Launcher (CreateDocument saves directly to user-chosen location)
    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        val payload = pendingCsvExportContent
        if (uri != null && payload != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(payload.second.toByteArray(Charsets.UTF_8))
                    out.flush()
                }
                viewModel.reportCsvExportSuccess(payload.first)
            }.onFailure { err ->
                viewModel.reportCsvError(
                    err.localizedMessage ?: if (isFr) "Erreur lors de l'enregistrement du CSV." else "Failed to save CSV file."
                )
            }
        }
    }

    if (uiState.showGoogleOAuthDialog) {
        GoogleOAuthDialog(
            emailHint = uiState.googleOAuthEmailHint ?: emailInput,
            isFr = isFr,
            onDismiss = { viewModel.dismissGoogleOAuthDialog() },
            onIdTokenReceived = { idToken ->
                viewModel.completeGoogleSignInWithIdToken(idToken, emailInput)
            },
            onError = { errMsg ->
                viewModel.onGoogleOAuthError(errMsg)
            }
        )
    }

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
            // 1. Cloud Synchronization & Account Card
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
                            // Primary Action: Sign in with Google (OAuth ID Token Flow - No SHA-1 / INVALID_APP_ID restriction)
                            Button(
                                onClick = { viewModel.openGoogleOAuthDialog(emailInput) },
                                enabled = !isSyncing,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF0F172A)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF4285F4),
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "G",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = Color.White
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isSyncing) {
                                        if (isFr) "Connexion Google..." else "Connecting with Google..."
                                    } else {
                                        if (isFr) "Continuer avec Google (Recommandé)" else "Continue with Google (Recommended)"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Divider(modifier = Modifier.weight(1f))
                                Text(
                                    text = if (isFr) "OU EMAIL / MOT DE PASSE" else "OR EMAIL / PASSWORD",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Divider(modifier = Modifier.weight(1f))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

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

            // 2. Fuelio CSV Import & Export Card
            item {
                val activeVehicleName = uiState.activeVehicle?.let { "${it.make} ${it.model}" }
                    ?: if (isFr) "Véhicule actif" else "Active Vehicle"

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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(FuelGreenContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileUpload,
                                        contentDescription = null,
                                        tint = FuelGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isFr) "Import / Export CSV (Fuelio)" else "CSV Import / Export (Fuelio)",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = activeVehicleName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FuelGreen
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isFr) {
                                "Importez vos historiques Fuelio (.csv) ou exportez une sauvegarde complète de vos pleins et dépenses compatible avec l'application Web et Fuelio."
                            } else {
                                "Import your Fuelio (.csv) logs or export a full backup of your fill-ups and expenses compatible with the Web app and Fuelio."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!uiState.csvStatusMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (uiState.csvIsError) OverdueRedContainer else SuccessGreenContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = uiState.csvStatusMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (uiState.csvIsError) OverdueRed else SuccessGreen,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    csvImportLauncher.launch(
                                        arrayOf(
                                            "text/csv",
                                            "text/comma-separated-values",
                                            "text/plain",
                                            "*/*"
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FuelGreen,
                                    contentColor = Color(0xFF052E16)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isFr) "Importer CSV" else "Import CSV",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    val exported = viewModel.exportActiveVehicleCsv()
                                    pendingCsvExportContent = exported
                                    csvExportLauncher.launch(exported.first)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isFr) "Exporter CSV" else "Export CSV",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                val (fileName, csvContent) = viewModel.exportActiveVehicleCsv()
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_SUBJECT, fileName)
                                    putExtra(Intent.EXTRA_TEXT, csvContent)
                                }
                                context.startActivity(
                                    Intent.createChooser(
                                        sendIntent,
                                        if (isFr) "Partager la sauvegarde CSV" else "Share CSV Backup"
                                    )
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isFr) "Partager / Envoyer le CSV" else "Share / Send CSV Backup",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // 3. Language Toggle
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

            // 4. Units Toggle
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

            // 5. Currency Selector
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

            // 6. About Section
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
                                "Version 2.1.0 • Synchronisation Cloud multi-appareils (Web & Android) + Import/Export CSV Fuelio."
                            } else {
                                "Version 2.1.0 • Multi-device Cloud Sync (Web & Android) + Fuelio CSV Import/Export."
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun GoogleOAuthDialog(
    emailHint: String?,
    isFr: Boolean,
    onDismiss: () -> Unit,
    onIdTokenReceived: (String) -> Unit,
    onError: (String) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var hasCompleted by remember { mutableStateOf(false) }

    val oauthUrl = remember(emailHint) {
        val nonce = UUID.randomUUID().toString()
        val encodedRedirect = URLEncoder.encode(FIREBASE_AUTH_HANDLER_URL, "UTF-8")
        val encodedScope = URLEncoder.encode("openid email profile", "UTF-8")
        val base = "https://accounts.google.com/o/oauth2/v2/auth" +
            "?client_id=$GOOGLE_WEB_CLIENT_ID" +
            "&redirect_uri=$encodedRedirect" +
            "&response_type=id_token" +
            "&scope=$encodedScope" +
            "&prompt=select_account" +
            "&nonce=$nonce"
        if (!emailHint.isNullOrBlank() && emailHint.contains("@")) {
            "$base&login_hint=${URLEncoder.encode(emailHint.trim(), "UTF-8")}"
        } else {
            base
        }
    }

    fun deliverTokenOnce(token: String) {
        if (!hasCompleted && token.isNotBlank()) {
            hasCompleted = true
            Handler(Looper.getMainLooper()).post {
                onIdTokenReceived(token)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Dialog Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF4285F4),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "G",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isFr) "Connexion Google Cloud" else "Google Cloud Sign-In",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "accounts.google.com",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                // Use standard Chrome Mobile UA so Google OAuth allows embedded sign-in
                                val rawUa = settings.userAgentString ?: ""
                                val cleanUa = rawUa
                                    .replace("; wv", "")
                                    .replace(Regex("Version/\\d+\\.\\d+\\s*"), "")
                                settings.userAgentString = cleanUa.ifBlank {
                                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                                }

                                addJavascriptInterface(
                                    object {
                                        @JavascriptInterface
                                        fun onOAuthCallback(urlOrHash: String?) {
                                            extractIdTokenFromUrl(urlOrHash)?.let { deliverTokenOnce(it) }
                                        }
                                    },
                                    "NetCarOAuthBridge"
                                )

                                webViewClient = object : WebViewClient() {
                                    override fun shouldInterceptRequest(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): WebResourceResponse? {
                                        val reqUrl = request?.url?.toString() ?: return null
                                        extractIdTokenFromUrl(reqUrl)?.let { deliverTokenOnce(it) }

                                        if (reqUrl.startsWith(FIREBASE_AUTH_HANDLER_URL)) {
                                            val bridgeHtml = """
                                                <!DOCTYPE html>
                                                <html>
                                                <head><meta name="viewport" content="width=device-width, initial-scale=1"/></head>
                                                <body style="background:#0B1120;color:#F8FAFC;font-family:sans-serif;display:flex;flex-direction:column;align-items:center;justify-content:center;height:100vh;margin:0;">
                                                  <p style="font-size:16px;font-weight:bold;">Connexion Google réussie, synchronisation...</p>
                                                  <script>
                                                    try {
                                                      if (window.NetCarOAuthBridge) {
                                                        window.NetCarOAuthBridge.onOAuthCallback(window.location.href);
                                                      }
                                                    } catch (e) {}
                                                  </script>
                                                </body>
                                                </html>
                                            """.trimIndent()
                                            return WebResourceResponse(
                                                "text/html",
                                                "UTF-8",
                                                ByteArrayInputStream(bridgeHtml.toByteArray(Charsets.UTF_8))
                                            )
                                        }
                                        return super.shouldInterceptRequest(view, request)
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val url = request?.url?.toString() ?: return false
                                        extractIdTokenFromUrl(url)?.let {
                                            deliverTokenOnce(it)
                                            return true
                                        }
                                        return false
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        isLoading = true
                                        extractIdTokenFromUrl(url)?.let { deliverTokenOnce(it) }
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                        extractIdTokenFromUrl(url)?.let { deliverTokenOnce(it) }
                                        if (url != null && url.startsWith(FIREBASE_AUTH_HANDLER_URL)) {
                                            view?.evaluateJavascript(
                                                "(function(){ if(window.NetCarOAuthBridge) window.NetCarOAuthBridge.onOAuthCallback(window.location.href); })();",
                                                null
                                            )
                                        }
                                    }
                                }

                                loadUrl(oauthUrl)
                            }
                        }
                    )

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
    }
}
