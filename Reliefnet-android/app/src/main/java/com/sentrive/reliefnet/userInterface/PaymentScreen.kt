package com.sentrive.reliefnet.userInterface

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sentrive.reliefnet.network.models.CreatePaymentOrderPhonePeRequest
import com.sentrive.reliefnet.repository.ReliefNetRepository
import com.sentrive.reliefnet.utils.openPhonePePayment
import com.sentrive.reliefnet.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    navHostController: NavHostController,
    doctorId: String,
    date: String,
    startTime: String,
    endTime: String,
    amount: String,
    appointmentType: String,
    symptoms: String,
    notes: String
) {
    val context = LocalContext.current
    val repository = remember { ReliefNetRepository() }
    val scope = rememberCoroutineScope()
    
    var selectedPaymentMethod by remember { mutableStateOf("Google Pay / BHIM UPI") }
    var isProcessing by remember { mutableStateOf(false) }
    
    val paymentMethods = listOf(
        "Google Pay / BHIM UPI",
        "PhonePe",
        "Pay at Visit"
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PatientPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Appointment Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = PatientBackground
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Appointment Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    DetailRow("Date:", formatDate(date))
                    DetailRow("Time:", "$startTime - $endTime")
                    DetailRow("Type:", appointmentType)
                    DetailRow("Symptoms:", if (symptoms == "None") "None" else symptoms)
                    DetailRow("Notes:", if (notes == "None") "None" else notes)
                }
            }
            
            // Payment Method Selection
            Text(
                text = "Select Payment Method",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                paymentMethods.forEach { method ->
                    PaymentMethodCard(
                        method = method,
                        isSelected = selectedPaymentMethod == method,
                        onSelect = { selectedPaymentMethod = method }
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Total Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Amount",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "₹$amount",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
            }
            
            // Pay & Confirm Button
            Button(
                onClick = {
                    scope.launch {
                        isProcessing = true
                        try {
                            // Create PhonePe payment order
                            val paymentRequest = CreatePaymentOrderPhonePeRequest(
                                amount = amount.toDouble(),
                                doctorId = doctorId,
                                appointmentDate = date,
                                appointmentTime = startTime
                            )
                            
                            val paymentResponse = repository.createPhonePeOrder(paymentRequest)
                            if (paymentResponse.isSuccessful && paymentResponse.body() != null) {
                                val paymentBody = paymentResponse.body()!!
                                val paymentUrl = paymentBody.paymentUrl
                                
                                // Launch PhonePe payment
                                context.openPhonePePayment(paymentUrl)
                                
                                // Navigate to payment status screen with all booking details
                                navHostController.navigate(
                                    "payment-status/${paymentBody.merchantTransactionId}/$doctorId/$date/$startTime/$endTime/$appointmentType/$symptoms/$notes"
                                ) {
                                    popUpTo("IntegratedBooking/$doctorId") { inclusive = true }
                                }
                            }
                        } catch (e: Exception) {
                            // Handle error
                        } finally {
                            isProcessing = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isProcessing,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PatientPrimary
                )
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Pay & Confirm",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun PaymentMethodCard(
    method: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelect
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PatientPrimaryLight else PatientBackground
        ),
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) BorderStroke(2.dp, PatientPrimary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFF6200EE)
                )
            )
            Text(
                text = method,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val outputFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}
