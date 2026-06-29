package com.bazaarhelper.app.presentation.screens.add

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bazaarhelper.app.R
import com.bazaarhelper.app.presentation.components.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecordScreen(
    onBack: () -> Unit,
    viewModel: AddEditViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val saveSuccessMsg = stringResource(R.string.save_success)
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            snackbarHostState.showSnackbar(saveSuccessMsg)
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        topBar = {
            BazaarTopBar(
                title = if (state.isEditMode) stringResource(R.string.edit_data) else stringResource(R.string.add_data_today),
                onBack = onBack
            )
        },
        snackbarHost = { BazaarSnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Date Selection Card
            Card(
                onClick = { showDatePicker = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.record_date), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.date.formatArabic(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.change), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }

            // Input Fields
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernInputField(
                    value = state.salesInput,
                    onValueChange = viewModel::onSalesChange,
                    label = stringResource(R.string.total_sales),
                    error = state.salesError
                )

                ModernInputField(
                    value = state.purchasesInput,
                    onValueChange = viewModel::onPurchasesChange,
                    label = stringResource(R.string.total_purchases),
                    error = state.purchasesError
                )

                // Notes Field
                OutlinedTextField(
                    value = state.notesInput,
                    onValueChange = viewModel::onNotesChange,
                    label = { Text(stringResource(R.string.notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )
            }

            // Real-time Profit Preview
            val sales = state.salesInput.toDoubleOrNull() ?: 0.0
            val purchases = state.purchasesInput.toDoubleOrNull() ?: 0.0
            val profit = sales - purchases
            val isProfit = profit >= 0
            val profitColor = if (isProfit) Color(0xFF2E7D32) else Color(0xFFC62828)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = profitColor.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        color = profitColor.copy(alpha = 0.1f),
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = profitColor)
                        }
                    }
                    Column {
                        Text(
                            text = if (isProfit) stringResource(R.string.expected_net_profit) else stringResource(R.string.expected_net_loss),
                            fontSize = 14.sp,
                            color = profitColor.copy(alpha = 0.8f)
                        )
                        AnimatedContent(targetState = profit, label = "profit_anim") { value ->
                            Text(
                                text = value.formatCurrency(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = profitColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = MaterialTheme.shapes.large
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.save_record), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.toEpochDay() * 86_400_000L
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.onDateChange(
                            LocalDate.ofEpochDay(millis / 86_400_000L)
                        )
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.confirm), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun ModernInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = error != null,
            singleLine = true,
            suffix = { Text(stringResource(R.string.currency_symbol), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )
        AnimatedVisibility(visible = error != null) {
            Text(
                text = error ?: "",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
