package com.example.catcare.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.catcare.data.model.PetEntity
import com.example.catcare.data.model.ReminderEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    cats: List<PetEntity>,
    onSave: (ReminderEntity) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    val catNames = cats.map { it.name }.distinct()
    val activities = listOf("Feed", "Groom", "Vet", "Wash", "Play", "Medicine")

    var selectedCat by remember { mutableStateOf<String?>(catNames.firstOrNull()) }
    var selectedActivity by remember { mutableStateOf<String?>(activities.firstOrNull()) }

    // title auto-fills from cat + activity, but stays editable
    var title by remember { mutableStateOf(buildTitle(selectedActivity, selectedCat)) }
    var manualEdited by remember { mutableStateOf(false) }
    fun autoUpdateTitle() {
        if (!manualEdited || title.isBlank()) {
            title = buildTitle(selectedActivity, selectedCat)
        }
    }


    var date by remember { mutableStateOf(LocalDate.now()) }
    var time by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0)) }

    val dateDialog = remember(date) {
        DatePickerDialog(
            context,
            { _, y, m, d -> date = LocalDate.of(y, m + 1, d) },
            date.year, date.monthValue - 1, date.dayOfMonth
        )
    }
    val timeDialog = remember(time) {
        TimePickerDialog(
            context,
            { _, h, min -> time = LocalTime.of(h, min) },
            time.hour, time.minute, false
        )
    }

    // repeat every H:M (optional)
    var repeatHours by remember { mutableStateOf("") }
    var repeatMinutes by remember { mutableStateOf("") }

    //validation
    // requires  a cat is selected and title is not blank.
    val isValid = selectedCat != null && title.trim().isNotEmpty()


    Scaffold { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Add reminder", style = MaterialTheme.typography.headlineLarge)

            ExposedDropdown(
                label = "Cat",
                options = catNames,
                selected = selectedCat,
                onSelected = { selectedCat = it; autoUpdateTitle() }
            )
            if (catNames.isEmpty()) {
                AssistiveText("Add a cat first on the Cats tab.")
            }

            ExposedDropdown(
                label = "Activity",
                options = activities,
                selected = selectedActivity,
                onSelected = { selectedActivity = it; autoUpdateTitle() }
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it; manualEdited = true },
                label = { Text("Reminder text") },
                modifier = Modifier.fillMaxWidth()
            )

            // Date & Time
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { dateDialog.show() }) {
                    Text("Pick date: $date")
                }
                OutlinedButton(onClick = { timeDialog.show() }) {
                    Text("Pick time: ${time.toString().substring(0, 5)}")
                }
            }

            Text("Repeat (optional)", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = repeatHours,
                    onValueChange = { repeatHours = it.filter(Char::isDigit) },
                    label = { Text("Hours") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = repeatMinutes,
                    onValueChange = { repeatMinutes = it.filter(Char::isDigit) },
                    label = { Text("Minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
                Button(
                    onClick = {
                        if (!isValid) return@Button
                        val dueMillis = ZonedDateTime.of(date, time, ZoneId.systemDefault())
                            .toInstant().toEpochMilli()
                        val repeatTotal =
                            (repeatHours.toIntOrNull() ?: 0) * 60 + (repeatMinutes.toIntOrNull() ?: 0)
                        val repeatValue = if (repeatTotal > 0) repeatTotal else null

                        onSave(
                            ReminderEntity(
                                title = title.trim(),
                                dueAt = dueMillis,
                                repeatMinutes = repeatValue,
                                catName = selectedCat,
                                activity = selectedActivity
                            )
                        )
                    },
                    enabled = isValid
                ) { Text("Save reminder") }
            }
        }
    }
}

@Composable
private fun AssistiveText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdown(
    label: String,
    options: List<String>,
    selected: String?,
    onSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (options.isEmpty()) {
                DropdownMenuItem(text = { Text("No options") }, onClick = {})
            } else {
                options.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = { onSelected(item); expanded = false }
                    )
                }
            }
        }
    }
}

private fun buildTitle(activity: String?, cat: String?) =
    listOfNotNull(activity, cat).joinToString(" – ")
