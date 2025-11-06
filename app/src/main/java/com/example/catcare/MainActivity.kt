package com.example.catcare

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.catcare.data.db.AppDatabase
import com.example.catcare.data.model.ReminderEntity
import com.example.catcare.notifications.cancelReminder
import com.example.catcare.notifications.ensureReminderChannel
import com.example.catcare.notifications.purgeLegacyWork
import com.example.catcare.notifications.scheduleReminder
import com.example.catcare.ui.screens.AddReminderScreen
import com.example.catcare.ui.screens.CatsScreen
import com.example.catcare.ui.screens.PetDetailScreen
import com.example.catcare.ui.screens.PetFormScreen
import com.example.catcare.ui.screens.SchedulerScreen
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK)
        )

        setContent { PetCareApp() }
    }
}

@Composable
fun PetCareApp() {
    val nav = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // room
    val db = remember { AppDatabase.get(context) }
    val dao = remember { db.petDao() }

    // streams
    val pets by dao.getAll().collectAsState(initial = emptyList())
    val reminders by dao.getAllReminders().collectAsState(initial = emptyList())

    // notification permission
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {  }

    var didInit by remember { mutableStateOf(false) }

    // one time app init
    LaunchedEffect(Unit) {
        ensureReminderChannel(context)
        purgeLegacyWork(context)
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        didInit = true
    }


    LaunchedEffect(reminders, didInit) {
        if (!didInit) return@LaunchedEffect
        reminders.filter { !it.completed }.forEach { scheduleReminder(context, it) }
    }

    // bottom nav items
    val routes = listOf("cats", "scheduler")
    val labels = mapOf("cats" to "Cats", "scheduler" to "Schedule")
    val icons = mapOf("cats" to Icons.Outlined.Pets, "scheduler" to Icons.Outlined.Alarm)

    Scaffold(
        bottomBar = {
            val back by nav.currentBackStackEntryAsState()
            val current = back?.destination

            NavigationBar(
                containerColor = Color.Black,
                contentColor = Color.White
            ) {
                routes.forEach { route ->
                    val selected = current.isOnRoute(route)
                    NavigationBarItem(
                        selected = selected,
                        onClick = { if (current?.route != route) nav.navigate(route) },
                        icon = {
                            Icon(
                                imageVector = icons.getValue(route),
                                contentDescription = labels.getValue(route)
                            )
                        },
                        label = { Text(labels.getValue(route)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedIconColor = Color(0xFFB0B0B0),
                            unselectedTextColor = Color(0xFFB0B0B0),
                            indicatorColor = Color(0xFF222222)
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "cats",
            modifier = Modifier.padding(padding)
        ) {
            // cats list
            composable("cats") {
                CatsScreen(
                    cats = pets,
                    onCatClick = { id -> nav.navigate("detail/$id") },
                    onAddCat = { nav.navigate("add") },
                    onDeleteCat = { pet ->
                        scope.launch {

                            reminders.filter { it.catName == pet.name }.forEach { r ->
                                cancelReminder(context, r.id)
                                dao.deleteReminder(r.id)
                            }
                            // delete cat
                            dao.deletePet(pet.id)
                        }
                    }
                )
            }

            // add cat
            composable("add") {
                PetFormScreen(
                    onSave = { newPet ->
                        scope.launch { dao.insert(newPet.copy(id = 0)) }
                        nav.popBackStack()
                    },
                    onCancel = { nav.popBackStack() },
                    nextId = { 0L }
                )
            }

            // cat details
            composable("detail/{id}") { entry ->
                val id = entry.arguments?.getString("id")?.toLongOrNull()
                val pet = pets.firstOrNull { it.id == id }
                if (pet != null) {
                    PetDetailScreen(pet = pet, onBack = { nav.popBackStack() })
                } else {
                    Surface { Text("Pet not found") }
                }
            }

            // reminders list
            composable("scheduler") {
                SchedulerScreen(
                    reminders = reminders,
                    onAddReminder = { nav.navigate("addReminder") },
                    onDelete = { r ->
                        scope.launch {
                            cancelReminder(context, r.id)
                            dao.deleteReminder(r.id)
                        }
                    }
                )
            }

            // add reminder
            composable("addReminder") {
                AddReminderScreen(
                    cats = pets,
                    onSave = { r: ReminderEntity ->
                        scope.launch {
                            val newId = dao.insertReminder(r)
                            scheduleReminder(context, r.copy(id = newId))
                        }
                        nav.popBackStack()
                    },
                    onCancel = { nav.popBackStack() }
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun safeNoop() { }

private fun NavDestination?.isOnRoute(route: String): Boolean =
    this?.hierarchy?.any { it.route == route } == true


private fun formatDue(r: com.example.catcare.data.model.ReminderEntity): String? =
    r.dueAt?.let {
        val fmt = DateTimeFormatter.ofPattern("EEE d MMM, h:mm a")
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(fmt)
    }