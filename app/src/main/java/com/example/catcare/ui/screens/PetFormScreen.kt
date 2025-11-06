package com.example.catcare.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.catcare.data.model.PetEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetFormScreen(
    onSave: (PetEntity) -> Unit,
    onCancel: () -> Unit,
    nextId: () -> Long
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var years by remember { mutableStateOf("") }
    var months by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var food by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }


    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {

            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { /* ignore if already granted */ }
            photoUri = uri
        }
    }

    // editable breed with suggestions
    val allBreeds = listOf(
        "Calico", "Turkish Angora", "Persian", "Siamese", "Bengal",
        "British Shorthair", "Domestic Short Hair", "Maine Coon", "Ragdoll",
        "Scottish Fold", "Sphynx", "Norwegian Forest", "Abyssinian", "Custom"
    )
    var breedMenu by remember { mutableStateOf(false) }
    val filtered = remember(breed) {
        if (breed.isBlank()) allBreeds else allBreeds.filter { it.contains(breed, ignoreCase = true) }
    }

    // lock color when calico
    val isCalico = breed.equals("Calico", ignoreCase = true)
    if (isCalico && color != "Brown, White & Black") color = "Brown, White & Black"

    // validation
    val yrs = years.toIntOrNull() ?: 0
    val mths = months.toIntOrNull()?.coerceIn(0, 11) ?: 0
    val canSave = name.isNotBlank() &&
            (yrs > 0 || mths > 0) &&
            breed.isNotBlank() &&
            color.isNotBlank()

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Add Cat", style = MaterialTheme.typography.headlineLarge)


        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(Modifier.size(84.dp)) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Cat photo",
                    modifier = Modifier.fillMaxSize()
                )
            }
            OutlinedButton(onClick = { pickImage.launch(arrayOf("image/*")) }) {
                Text(if (photoUri == null) "Choose photo" else "Change photo")
            }
        }

        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Name") }, modifier = Modifier.fillMaxWidth()
        )


        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = years,
                onValueChange = { input -> years = input.filter(Char::isDigit) },
                label = { Text("Age (years)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = months,
                onValueChange = { input ->
                    val digits = input.filter(Char::isDigit)
                    val n = digits.toIntOrNull()
                    months = when {
                        n == null -> digits
                        n > 11 -> "11"
                        else -> n.toString()
                    }
                },
                label = { Text("Months (0–11)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }


        ExposedDropdownMenuBox(expanded = breedMenu, onExpandedChange = { breedMenu = it }) {
            OutlinedTextField(
                value = breed,
                onValueChange = { breed = it },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                label = { Text("Breed") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = breedMenu) }
            )
            ExposedDropdownMenu(expanded = breedMenu, onDismissRequest = { breedMenu = false }) {
                filtered.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            breed = option
                            breedMenu = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = color,
            onValueChange = { if (!isCalico) color = it },
            enabled = !isCalico,
            label = { Text(if (isCalico) "Color (locked for Calico)" else "Color") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = food, onValueChange = { food = it },
            label = { Text("Favorite food") }, modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel) { Text("Cancel") }
            Button(
                onClick = {

                    val approxYears = yrs + if (mths >= 6) 1 else 0
                    val pet = PetEntity(
                        id = 0, // room autogenerates
                        name = name.trim(),
                        ageYears = approxYears,
                        ageMonths = months.toIntOrNull() ?: 0,
                        breed = breed.trim(),
                        color = color.trim(),
                        favoriteFood = food.trim(),
                        photoUri = photoUri?.toString()
                    )
                    onSave(pet)
                },
                enabled = canSave
            ) { Text("Save") }
        }
    }
}
