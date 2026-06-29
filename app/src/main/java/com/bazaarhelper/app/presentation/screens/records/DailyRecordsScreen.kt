package com.bazaarhelper.app.presentation.screens.records

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bazaarhelper.app.R
import com.bazaarhelper.app.core.security.BackupManager
import com.bazaarhelper.app.domain.model.DailyRecord
import com.bazaarhelper.app.presentation.components.*
import java.time.LocalDate

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyRecordsScreen(
    onBack: () -> Unit,
    onEditRecord: (Long) -> Unit,
    onMonthlyReport: () -> Unit,
    viewModel: DailyRecordsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val backupManager = remember { BackupManager(context) }
    
    var recordToDelete by remember { mutableStateOf<DailyRecord?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val importSuccessMsg = stringResource(R.string.import_success)
    val importFailedMsg = stringResource(R.string.import_failed)

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            backupManager.importBackup(
                uri = it,
                onSuccess = { 
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = importSuccessMsg,
                            duration = SnackbarDuration.Long
                        )
                    }
                    showMenu = false
                    viewModel.onSearchChange("") // force reload
                },
                onError = { error ->
                    scope.launch {
                        snackbarHostState.showSnackbar("$importFailedMsg: $error")
                    }
                }
            )
        }
    }

    val deleteSuccessMsg = stringResource(R.string.delete_success)
    LaunchedEffect(state.deleteSuccess) {
        if (state.deleteSuccess) {
            snackbarHostState.showSnackbar(deleteSuccessMsg)
            viewModel.clearDeleteSuccess()
        }
    }

    Scaffold(
        topBar = {
            BazaarTopBar(
                title = stringResource(R.string.daily_records),
                onBack = onBack,
                actions = {
                    IconButton(onClick = onMonthlyReport) {
                        Icon(Icons.Default.BarChart, contentDescription = stringResource(R.string.monthly_report))
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.additional_options))
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_backup)) },
                                onClick = {
                                    showMenu = false
                                    backupManager.exportBackup()
                                },
                                leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.import_backup)) },
                                onClick = {
                                    filePicker.launch("*/*")
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null) }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { BazaarSnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Date Search Trigger
            val displayValue = if (state.searchQuery.isNotEmpty()) {
                try {
                    LocalDate.parse(state.searchQuery).formatArabic()
                } catch (_: Exception) {
                    state.searchQuery
                }
            } else ""

            OutlinedTextField(
                value = displayValue,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text(stringResource(R.string.filter_by_date), fontSize = 16.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                            }
                        }
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.pick_date), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.large,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.filteredRecords.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (state.searchQuery.isBlank()) stringResource(R.string.no_records_yet) else stringResource(R.string.no_results),
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.filteredRecords, key = { it.id }) { record ->
                        ModernRecordCard(
                            record = record,
                            onEdit = { onEditRecord(record.id) },
                            onDelete = { recordToDelete = record }
                        )
                    }
                }
            }
        }
    }

    recordToDelete?.let { record ->
        ConfirmDeleteDialog(
            onConfirm = {
                viewModel.deleteRecord(record.id)
                recordToDelete = null
            },
            onDismiss = { recordToDelete = null }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = LocalDate.ofEpochDay(millis / 86_400_000L)
                        viewModel.onSearchChange(selectedDate.toString())
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.confirm)) }
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
private fun ModernRecordCard(
    record: DailyRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isProfit = record.profit >= 0
    val statusColor = if (isProfit) Color(0xFF4CAF50) else Color(0xFFF44336)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = statusColor.copy(alpha = 0.1f),
                        shape = CircleShape,
                        modifier = Modifier.size(8.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = record.date.formatArabic(),
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = colors.onSurface
                    )
                }
                Row {
                    FilledTonalIconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = colors.primaryContainer.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit), modifier = Modifier.size(18.dp), tint = colors.primary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalIconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = colors.errorContainer.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(18.dp), tint = colors.error)
                    }
                }
            }
            
            if (record.notes.isNotBlank()) {
                Text(
                    text = record.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Surface(
                color = colors.surfaceVariant.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RecordDetailItem(label = stringResource(R.string.sales), value = record.sales.formatCurrency(), color = colors.onSurface)
                    RecordDetailItem(label = stringResource(R.string.purchases), value = record.purchases.formatCurrency(), color = colors.onSurface)
                    RecordDetailItem(
                        label = if (isProfit) stringResource(R.string.profit) else stringResource(R.string.loss),
                        value = record.profit.formatCurrency(),
                        color = statusColor,
                        isBold = true
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordDetailItem(label: String, value: String, color: Color, isBold: Boolean = false) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = if (isBold) FontWeight.Black else FontWeight.Bold,
            color = color
        )
    }
}
