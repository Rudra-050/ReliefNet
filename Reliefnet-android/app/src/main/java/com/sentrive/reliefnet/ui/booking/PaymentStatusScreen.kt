package com.sentrive.reliefnet.ui.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sentrive.reliefnet.viewmodel.BookingViewModel
import kotlinx.coroutines.delay

/**
 * Payment Status Screen
 * Shows when user returns from PhonePe payment
 */
@Composable
fun PaymentStatusScreen(
    merchantTransactionId: String,
    professionalId: String,
    appointmentDate: String,
    appointmentTime: String,
    appointmentEndTime: String = "",
    appointmentType: String = "Online Consultation",
    symptoms: String = "",
    notes: String = "",
    onSuccess: (String) -> Unit, // Navigate to booking details
    onFailed: () -> Unit,
    onBack: () -> Unit,
    viewModel: BookingViewModel = viewModel()
) {
    val paymentState by viewModel.paymentState.collectAsState()
    val bookingState by viewModel.bookingState.collectAsState()
    
    var isVerifying by remember { mutableStateOf(true) }
    
    // Start payment verification
    LaunchedEffect(merchantTransactionId) {
        delay(1000) // Small delay for better UX
        viewModel.checkPaymentStatus(merchantTransactionId)
    }
    
    // Handle payment verification result
    LaunchedEffect(paymentState) {
        when (val state = paymentState) {
            is BookingViewModel.PaymentState.PaymentSuccess -> {
                isVerifying = false
                // Automatically confirm booking
                delay(500)
                
                // Map appointmentType to sessionType
                val sessionType = when (appointmentType) {
                    "Home Visit" -> "home-visit"
                    "Online Consultation" -> "consultation"
                    "In-Person Visit" -> "consultation"
                    else -> "consultation"
                }
                
                viewModel.confirmPaymentAndBooking(
                    merchantTransactionId = merchantTransactionId,
                    professionalId = professionalId,
                    date = appointmentDate,
                    time = appointmentTime,
                    type = sessionType,
                    symptoms = symptoms.ifBlank { null },
                    notes = notes.ifBlank { null }
                )
            }
            is BookingViewModel.PaymentState.PaymentFailed -> {
                isVerifying = false
                delay(2000)
                onFailed()
            }
            is BookingViewModel.PaymentState.Error -> {
                isVerifying = false
                delay(2000)
                onFailed()
            }
            else -> {}
        }
    }
    
    // Handle booking confirmation result
    LaunchedEffect(bookingState) {
        when (val state = bookingState) {
            is BookingViewModel.BookingState.Success -> {
                delay(1500) // Show success for a moment
                onSuccess(state.booking.id)
            }
            else -> {}
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                isVerifying -> {
                    VerifyingPaymentContent()
                }
                paymentState is BookingViewModel.PaymentState.PaymentSuccess -> {
                    PaymentSuccessContent()
                }
                bookingState is BookingViewModel.BookingState.Creating -> {
                    CreatingBookingContent()
                }
                bookingState is BookingViewModel.BookingState.Success -> {
                    BookingSuccessContent(bookingState as BookingViewModel.BookingState.Success)
                }
                paymentState is BookingViewModel.PaymentState.PaymentFailed -> {
                    PaymentFailedContent(
                        reason = (paymentState as BookingViewModel.PaymentState.PaymentFailed).reason,
                        onRetry = onBack
                    )
                }
                paymentState is BookingViewModel.PaymentState.Error ||
                bookingState is BookingViewModel.BookingState.Error -> {
                    val errorMessage = when {
                        paymentState is BookingViewModel.PaymentState.Error ->
                            (paymentState as BookingViewModel.PaymentState.Error).message
                        bookingState is BookingViewModel.BookingState.Error ->
                            (bookingState as BookingViewModel.BookingState.Error).message
                        else -> "Unknown error"
                    }
                    PaymentStatusErrorContent(
                        message = errorMessage,
                        onRetry = onBack
                    )
                }
            }
        }
    }
}

@Composable
fun VerifyingPaymentContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            strokeWidth = 6.dp
        )
        
        Text(
            "Verifying Payment",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            "Please wait while we verify your payment...",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PaymentSuccessContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = "Success",
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(80.dp)
        )
        
        Text(
            "Payment Successful",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )
        
        Text(
            "Your payment has been received",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CreatingBookingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            strokeWidth = 6.dp,
            color = Color(0xFF4CAF50)
        )
        
        Text(
            "Creating Booking",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            "Setting up your appointment...",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BookingSuccessContent(state: BookingViewModel.BookingState.Success) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            Icons.Default.EventAvailable,
            contentDescription = "Booking Confirmed",
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(80.dp)
        )
        
        Text(
            "Booking Confirmed!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoRow("Appointment Date", state.booking.appointmentDate)
                InfoRow("Appointment Time", state.booking.appointmentTime)
                InfoRow("Doctor", state.booking.doctorName)
                InfoRow("Amount Paid", "₹${state.booking.amount.toInt()}")
            }
        }
        
        Text(
            "You will receive a confirmation email shortly",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PaymentFailedContent(
    reason: String,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            Icons.Default.Cancel,
            contentDescription = "Failed",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(80.dp)
        )
        
        Text(
            "Payment Failed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        
        Text(
            reason,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again")
        }
    }
}

@Composable
private fun PaymentStatusErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(80.dp)
        )
        
        Text(
            "Something Went Wrong",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(
                message,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
        
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Go Back")
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
