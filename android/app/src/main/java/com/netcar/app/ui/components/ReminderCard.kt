package com.netcar.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netcar.app.model.Reminder
import com.netcar.app.ui.theme.OverdueRed
import com.netcar.app.ui.theme.OverdueRedContainer
import com.netcar.app.ui.theme.SuccessGreen
import com.netcar.app.ui.theme.SuccessGreenContainer
import com.netcar.app.ui.theme.WarningOrange
import com.netcar.app.ui.theme.WarningOrangeContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun ReminderCard(
    reminder: Reminder,
    currentOdometer: Int,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val now = System.currentTimeMillis()
    val isOverdue = reminder.isOverdue(currentOdometer, now)
    val isDueSoon = reminder.isDueSoon(currentOdometer, now)
    val progress = reminder.calculateProgress(currentOdometer)

    val statusColor = when {
        reminder.isCompleted -> SuccessGreen
        isOverdue -> OverdueRed
        isDueSoon -> WarningOrange
        else -> MaterialTheme.colorScheme.primary
    }

    val statusContainerColor = when {
        reminder.isCompleted -> SuccessGreenContainer
        isOverdue -> OverdueRedContainer
        isDueSoon -> WarningOrangeContainer
        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    }

    val statusText = when {
        reminder.isCompleted -> "Completed"
        isOverdue -> "Overdue"
        isDueSoon -> "Due Soon"
        else -> "Upcoming"
    }

    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Title + Status Badge
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(statusContainerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                reminder.isCompleted -> Icons.Default.CheckCircle
                                isOverdue -> Icons.Default.Warning
                                else -> Icons.Default.NotificationsActive
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = reminder.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else null
                            ),
                            color = if (reminder.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = reminder.type.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = statusContainerColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Due info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Odometer target
                    if (reminder.nextDueOdometer != null) {
                        val kmDiff = reminder.nextDueOdometer - currentOdometer
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val kmText = if (kmDiff < 0) {
                                "${abs(kmDiff)} km overdue (at ${reminder.nextDueOdometer} km)"
                            } else {
                                "in $kmDiff km (at ${reminder.nextDueOdometer} km)"
                            }
                            Text(
                                text = kmText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (kmDiff < 0 && !reminder.isCompleted) OverdueRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Date target
                    if (reminder.nextDueDate != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        val daysDiff = ((reminder.nextDueDate - now) / (1000 * 60 * 60 * 24)).toInt()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val dateText = if (daysDiff < 0) {
                                "${abs(daysDiff)} days overdue (${dateFormatter.format(Date(reminder.nextDueDate))})"
                            } else {
                                "in $daysDiff days (${dateFormatter.format(Date(reminder.nextDueDate))})"
                            }
                            Text(
                                text = dateText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (daysDiff < 0 && !reminder.isCompleted) OverdueRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Complete action button
                if (!reminder.isCompleted) {
                    FilledIconButton(
                        onClick = onComplete,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Mark Complete",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    OutlinedIconButton(
                        onClick = onComplete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = SuccessGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (!reminder.isCompleted && reminder.nextDueOdometer != null) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = progress,
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }
        }
    }
}
