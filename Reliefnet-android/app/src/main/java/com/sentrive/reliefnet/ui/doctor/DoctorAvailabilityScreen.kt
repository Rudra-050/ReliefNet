package com.sentrive.reliefnet.ui.doctor

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sentrive.reliefnet.viewmodel.DoctorAvailabilityViewModel
import com.sentrive.reliefnet.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorAvailabilityScreen(
    doctorId: String,
    onBack: () -> Unit,
    viewModel: DoctorAvailabilityViewModel = viewModel()
) {
    val context = LocalContext.current
    val availabilityState by viewModel.availabilityState.collectAsState()
    var selectedDate by remember { mutableStateOf(Date()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var timePickerMode by remember { mutableStateOf("add") }
    var editingSlotId by remember { mutableStateOf<String?>(null) }
    var selectedHour by remember { mutableStateOf(9) }
    var selectedMinute by remember { mutableStateOf(0) }

    // Load availability for selected date
    LaunchedEffect(selectedDate) {
        viewModel.loadAvailability(doctorId, selectedDate)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Availability", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DoctorPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                timePickerMode = "add"
                showTimePicker = true
            },
                containerColor = DoctorPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Slot", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Date Picker Row
            DatePickerRow(selectedDate) { date ->
                selectedDate = date
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Availability List
            when (val state = availabilityState) {
                is DoctorAvailabilityViewModel.AvailabilityState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DoctorPrimary)
                    }
                }
                is DoctorAvailabilityViewModel.AvailabilityState.Success -> {
                    if (state.slots.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No slots set for this date.", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.slots) { slot ->
                                AvailabilitySlotCard(
                                    slot = slot,
                                    onEdit = {
                                        // Parse time from startTime (format: "HH:mm")
                                        val parts = slot.startTime.split(":")
                                        if (parts.size == 2) {
                                            editingSlotId = slot.id
                                            selectedHour = parts[0].toIntOrNull() ?: 9
                                            selectedMinute = parts[1].toIntOrNull() ?: 0
                                            timePickerMode = "edit"
                                            showTimePicker = true
                                        }
                                    },
                                    onDelete = {
                                        viewModel.deleteSlot(doctorId, slot.id, selectedDate)
                                    }
                                )
                            }
                        }
                    }
                }
                is DoctorAvailabilityViewModel.AvailabilityState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
                else -> {}
            }
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                if (timePickerMode == "add") {
                    viewModel.addSlot(doctorId, selectedDate, hour, minute)
                } else if (timePickerMode == "edit" && editingSlotId != null) {
                    viewModel.editSlot(doctorId, editingSlotId!!, selectedDate, hour, minute)
                }
                showTimePicker = false
                editingSlotId = null
            },
            selectedHour,
            selectedMinute,
            false
        ).show()
    }
}

@Composable
fun DatePickerRow(selectedDate: Date, onDateChange: (Date) -> Unit) {
    val sdf = remember { SimpleDateFormat("EEE, MMM dd yyyy", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = {
            val cal = Calendar.getInstance()
            cal.time = selectedDate
            cal.add(Calendar.DATE, -1)
            onDateChange(cal.time)
        }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day")
        }
        Text(
            sdf.format(selectedDate),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = {
            val cal = Calendar.getInstance()
            cal.time = selectedDate
            cal.add(Calendar.DATE, 1)
            onDateChange(cal.time)
        }) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day")
        }
    }
}

@Composable
fun AvailabilitySlotCard(
    slot: DoctorAvailabilityViewModel.AvailabilitySlot,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "${slot.startTime} - ${slot.endTime}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                if (slot.isBooked) {
                    Text(
                        "Booked",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit, enabled = !slot.isBooked) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Slot")
                }
                IconButton(onClick = onDelete, enabled = !slot.isBooked) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Slot")
                }
            }
        }
    }
}
