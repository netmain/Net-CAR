package com.netcar.app.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Shared state used by the dashboard and vehicle screens while a photo is being
 * uploaded or fetched from a remote URL.
 */
data class VehiclePhotoState(
    val vehicleId: String? = null,
    val isBusy: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Small rounded preview used in vehicle cards and the dashboard header.
 */
@Composable
fun VehiclePhotoThumbnail(
    photoUrl: String,
    modifier: Modifier = Modifier,
    iconSize: Int = 28
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.DirectionsCar,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(iconSize.dp)
        )
        if (photoUrl.isNotBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Dialog that lets the user upload a photo from the device or apply a remote
 * image URL. The caller owns the repository call and reports progress through
 * [isBusy] and [errorMessage].
 */
@Composable
fun VehiclePhotoDialog(
    vehicleName: String,
    photoUrl: String,
    isFr: Boolean,
    isBusy: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onPickPhoto: (Uri) -> Unit,
    onApplyUrl: (String) -> Unit,
    onRemove: () -> Unit
) {
    var urlInput by remember(photoUrl) { mutableStateOf(photoUrl) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onPickPhoto(uri)
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = {
            Text(
                text = if (isFr) "Photo du véhicule" else "Vehicle Photo",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = vehicleName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        if (photoUrl.isBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isFr) "Aucune photo" else "No photo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = if (isFr) "Photo du véhicule" else "Vehicle photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { photoPicker.launch("image/*") },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFr) "Choisir" else "Choose",
                            maxLines = 1
                        )
                    }

                    if (photoUrl.isNotBlank()) {
                        OutlinedButton(
                            onClick = onRemove,
                            enabled = !isBusy,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isFr) "Retirer" else "Remove")
                        }
                    }
                }

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text(if (isFr) "URL de l'image" else "Image URL") },
                    placeholder = { Text("https://…") },
                    singleLine = true,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onApplyUrl(urlInput) },
                    enabled = !isBusy && urlInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isFr) "Charger depuis l'URL" else "Fetch from URL")
                }

                if (isBusy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text(if (isFr) "Fermer" else "Close")
            }
        }
    )
}
