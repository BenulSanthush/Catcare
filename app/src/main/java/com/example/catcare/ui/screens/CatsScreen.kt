package com.example.catcare.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage
import com.example.catcare.R
import com.example.catcare.data.model.PetEntity as Pet

@Composable
fun CatsScreen(
    cats: List<Pet>,
    onCatClick: (Long) -> Unit,
    onAddCat: () -> Unit,
    onDeleteCat: (Pet) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(R.drawable.bg_cats),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.20f),
                        1f to Color.Black.copy(alpha = 0.35f)
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(onClick = onAddCat) {
                    Icon(Icons.Filled.Add, contentDescription = "Add cat")
                }
            }
        ) { padding ->
            if (cats.isEmpty()) {
                EmptyState(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    onAddCat = onAddCat
                )
            } else {
                LazyColumn(
                    modifier = modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(cats, key = { it.id }) { cat ->
                        CatCard(
                            pet = cat,
                            onClick = { onCatClick(cat.id) },
                            onConfirmDelete = { onDeleteCat(cat) },
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CatCard(
    pet: Pet,
    onClick: () -> Unit,
    onConfirmDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var askDelete by remember { mutableStateOf(false) }

    ElevatedCard(onClick = onClick, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!pet.photoUri.isNullOrBlank()) {
                AsyncImage(
                    model = Uri.parse(pet.photoUri),
                    contentDescription = "${pet.name} photo",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pet.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pet.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    val age = formatAge(pet.ageYears, pet.ageMonths)
                    if (age.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = age,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "${pet.breed} · ${pet.color}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { askDelete = true }) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete ${pet.name}")
            }
        }
    }

    if (askDelete) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            title = { Text("Delete ${pet.name}?") },
            text = { Text("This will remove the cat and any related reminders.") },
            confirmButton = {
                TextButton(onClick = { askDelete = false; onConfirmDelete() }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { askDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    onAddCat: () -> Unit
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No cats yet", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Spacer(Modifier.height(6.dp))
        Text(
            "Add your first cat to start tracking care.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAddCat) { Text("Add Cat") }
    }
}


private fun formatAge(years: Int, months: Int?): String {
    val m = (months ?: 0).coerceIn(0, 11)
    return when {
        years > 0 && m > 0 -> "${years}y ${m}m"
        years > 0 -> "${years}y"
        m > 0 -> "0y ${m}m"
        else -> ""
    }
}