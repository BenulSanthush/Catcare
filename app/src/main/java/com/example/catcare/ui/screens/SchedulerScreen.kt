package com.example.catcare.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import com.example.catcare.R
import com.example.catcare.data.model.ReminderEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun SchedulerScreen(
    reminders: List<ReminderEntity>,
    onAddReminder: () -> Unit,
    onDelete: (ReminderEntity) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.bg_reminders),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.22f),
                        1f to Color.Black.copy(alpha = 0.40f)
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(onClick = onAddReminder) {
                    Icon(Icons.Filled.Add, contentDescription = "Add reminder")
                }
            }
        ) { padding ->
            Column(Modifier.padding(padding).padding(16.dp)) {
                Text(
                    "Reminders",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )
                Spacer(Modifier.height(16.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(reminders, key = { it.id }) { r ->
                        ReminderRow(r = r, onDelete = { onDelete(r) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderRow(
    r: ReminderEntity,
    onDelete: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(r.title, style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = onDelete) { Text("Delete") }
            }
            formatDue(r)?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if ((r.repeatMinutes ?: 0) > 0) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Repeats every ${formatRepeat(r.repeatMinutes!!)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDue(r: ReminderEntity): String? =
    r.dueAt?.let {
        val fmt = DateTimeFormatter.ofPattern("EEE d MMM, h:mm a")
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(fmt)
    }

private fun formatRepeat(mins: Int): String {
    val m = abs(mins)
    val h = m / 60
    val mm = m % 60
    return when {
        h > 0 && mm > 0 -> "${h}h ${mm}m"
        h > 0 -> "${h}h"
        else -> "${mm}m"
    }
}