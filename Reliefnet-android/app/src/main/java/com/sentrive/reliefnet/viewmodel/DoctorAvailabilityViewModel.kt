package com.sentrive.reliefnet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.sentrive.reliefnet.repository.ReliefNetRepository
import com.sentrive.reliefnet.network.models.TimeSlot
import java.text.SimpleDateFormat
import java.util.*

class DoctorAvailabilityViewModel(
    private val repository: ReliefNetRepository = ReliefNetRepository()
) : ViewModel() {
    sealed class AvailabilityState {
        object Idle : AvailabilityState()
        object Loading : AvailabilityState()
        data class Success(val slots: List<AvailabilitySlot>) : AvailabilityState()
        data class Error(val message: String) : AvailabilityState()
    }

    data class AvailabilitySlot(
        val id: String,
        val startTime: String,
        val endTime: String,
        val isBooked: Boolean = false
    )

    private val _availabilityState = MutableStateFlow<AvailabilityState>(AvailabilityState.Idle)
    val availabilityState: StateFlow<AvailabilityState> = _availabilityState
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun loadAvailability(doctorId: String, date: Date) {
        _availabilityState.value = AvailabilityState.Loading
        viewModelScope.launch {
            try {
                val dateString = dateFormat.format(date)
                val response = repository.getAvailableSlots(doctorId, dateString)
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success) {
                        val slots = body.availableSlots.map { timeSlot ->
                            AvailabilitySlot(
                                id = timeSlot.id ?: "",
                                startTime = timeSlot.startTime,
                                endTime = timeSlot.endTime,
                                isBooked = timeSlot.isBooked
                            )
                        }
                        _availabilityState.value = AvailabilityState.Success(slots)
                    } else {
                        _availabilityState.value = AvailabilityState.Error("No slots available")
                    }
                } else {
                    _availabilityState.value = AvailabilityState.Error("Failed to load availability")
                }
            } catch (e: Exception) {
                _availabilityState.value = AvailabilityState.Error(e.message ?: "Failed to load availability")
            }
        }
    }

    fun addSlot(doctorId: String, date: Date, hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                repository.addDoctorAvailabilitySlot(doctorId, date, hour, minute)
                loadAvailability(doctorId, date)
            } catch (e: Exception) {
                _availabilityState.value = AvailabilityState.Error(e.message ?: "Failed to add slot")
            }
        }
    }

    fun editSlot(doctorId: String, slotId: String, date: Date, hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                repository.editDoctorAvailabilitySlot(doctorId, slotId, date, hour, minute)
                loadAvailability(doctorId, date)
            } catch (e: Exception) {
                _availabilityState.value = AvailabilityState.Error(e.message ?: "Failed to edit slot")
            }
        }
    }

    fun deleteSlot(doctorId: String, slotId: String, date: Date) {
        viewModelScope.launch {
            try {
                repository.deleteDoctorAvailabilitySlot(doctorId, slotId, date)
                loadAvailability(doctorId, date)
            } catch (e: Exception) {
                _availabilityState.value = AvailabilityState.Error(e.message ?: "Failed to delete slot")
            }
        }
    }
}
