package com.bazaarhelper.app.presentation.screens.monthly

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bazaarhelper.app.R
import com.bazaarhelper.app.domain.model.DailyRecord
import com.bazaarhelper.app.domain.model.MonthlyReport
import com.bazaarhelper.app.presentation.components.*

@Composable
fun MonthlyReportScreen(
    onBack: () -> Unit,
    viewModel: MonthlyReportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            BazaarTopBar(
                title = stringResource(R.string.monthly_report),
                onBack = onBack,
                actions = {
                    if ((state.report?.daysCount ?: 0) > 0) {
                        if (state.isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = { viewModel.exportPdf(context) }) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = stringResource(R.string.export_pdf))
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MonthYearPicker(
                year = state.selectedYear,
                month = state.selectedMonth,
                onYearChange = viewModel::onYearChange,
                onMonthChange = viewModel::onMonthChange
            )

            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val report = state.report
                if (report == null || report.daysCount == 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.no_data_this_month),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    MonthlySummaryCards(report)
                    MonthlyBarChart(report)
                    MonthlyDetailTable(report.records)
                }
            }
        }
    }
}

@Composable
private fun MonthYearPicker(
    year: Int,
    month: Int,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit
) {
    var showMonthDropdown by remember { mutableStateOf(false) }
    val currentYear = java.time.LocalDate.now().year

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Year navigation
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onYearChange(year - 1) }) {
                    Text("▶", fontSize = 20.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text(
                    text = "$year",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                IconButton(
                    onClick = { if (year < currentYear) onYearChange(year + 1) },
                    enabled = year < currentYear
                ) {
                    Text(
                        "◀",
                        fontSize = 20.sp,
                        color = if (year < currentYear)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                    )
                }
            }

            // Month dropdown
            Box {
                OutlinedButton(
                    onClick = { showMonthDropdown = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(monthName(month), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                DropdownMenu(
                    expanded = showMonthDropdown,
                    onDismissRequest = { showMonthDropdown = false }
                ) {
                    (1..12).forEach { m ->
                        DropdownMenuItem(
                            text = { Text(monthName(m), fontSize = 16.sp) },
                            onClick = {
                                onMonthChange(m)
                                showMonthDropdown = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlySummaryCards(report: MonthlyReport) {
    val colors = MaterialTheme.colorScheme

    Text(
        text = stringResource(R.string.monthly_stats),
        fontWeight = FontWeight.Black,
        fontSize = 22.sp,
        color = colors.onSurface,
        modifier = Modifier.padding(top = 8.dp)
    )

    // Premium Main Balance Card
    val isProfit = report.totalProfit >= 0
    val profitGradient = if (isProfit) {
        Brush.linearGradient(listOf(Color(0xFF2E7D32), Color(0xFF4CAF50)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFD32F2F), Color(0xFFEF5350)))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .background(profitGradient)
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = if (isProfit) stringResource(R.string.total_profit_month) else stringResource(R.string.total_loss_month),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = report.totalProfit.formatCurrency(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                report.profitChangePercentage?.let { change ->
                    val isBetter = change >= 0
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBetter) "↑" else "↓",
                                color = if (isBetter) Color(0xFF81C784) else Color(0xFFFFB74D),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${String.format("%.1f", kotlin.math.abs(change))}% ${stringResource(R.string.compared_to_last_month)}",
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricMiniItem(stringResource(R.string.daily_avg), report.avgDailyProfit.formatCurrency())
                    MetricMiniItem(stringResource(R.string.days_count), "${report.daysCount} ${stringResource(R.string.day)}")
                }
            }
        }
    }

    // Secondary Stats
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ModernStatCard(
            label = stringResource(R.string.total_sales),
            value = report.totalSales.formatCurrency(),
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )
        ModernStatCard(
            label = stringResource(R.string.total_purchases),
            value = report.totalPurchases.formatCurrency(),
            color = Color(0xFFF44336),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricMiniItem(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun ModernStatCard(label: String, value: String, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
private fun MonthlyBarChart(report: MonthlyReport) {
    if (report.records.size < 2) return

    val colors = MaterialTheme.colorScheme
    val negColor = Color(0xFFF44336)
    val profitColor = Color(0xFF4CAF50)

    Text(stringResource(R.string.chart_title), fontWeight = FontWeight.Bold, fontSize = 18.sp)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(color = profitColor, label = stringResource(R.string.profit))
                LegendDot(color = negColor, label = stringResource(R.string.loss))
            }

            Spacer(modifier = Modifier.height(12.dp))

            val maxAbsProfit = report.records.maxOf { kotlin.math.abs(it.profit) }.coerceAtLeast(1.0)
            val barMaxHeight = 150.dp
            val barWidth = 14.dp
            val barSpacing = 6.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(barSpacing)
            ) {
                report.records.forEach { record ->
                    val profitFraction = (kotlin.math.abs(record.profit) / maxAbsProfit).coerceIn(0.0, 1.0).toFloat()
                    val profitIsNeg = record.profit < 0

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Profit/Loss bar
                        Box(
                            modifier = Modifier
                                .width(barWidth)
                                .height(barMaxHeight * profitFraction)
                        ) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRect(color = if (profitIsNeg) negColor else profitColor)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        // Day label
                        Text(
                            text = "${record.date.dayOfMonth}",
                            fontSize = 10.sp,
                            color = colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(12.dp)
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = color)
            }
        }
        Text(label, fontSize = 12.sp)
    }
}

@Composable
private fun MonthlyDetailTable(records: List<DailyRecord>) {
    val colors = MaterialTheme.colorScheme

    Text(stringResource(R.string.daily_details), fontWeight = FontWeight.Bold, fontSize = 18.sp)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                TableHeaderCell(stringResource(R.string.date), Modifier.weight(1.2f))
                TableHeaderCell(stringResource(R.string.sales), Modifier.weight(1f))
                TableHeaderCell(stringResource(R.string.purchases), Modifier.weight(1f))
                TableHeaderCell(stringResource(R.string.profit), Modifier.weight(1f))
            }
            HorizontalDivider(color = colors.primary, thickness = 1.dp)

            records.forEachIndexed { index, record ->
                val rowBg = if (index % 2 == 0) colors.surface else colors.surfaceVariant.copy(alpha = 0.4f)
                val profitColor = if (record.profit >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)

                Surface(color = rowBg) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            record.date.formatArabic(),
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1.2f)
                        )
                        Text(
                            shortNum(record.sales),
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            shortNum(record.purchases),
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            shortNum(record.profit),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = profitColor,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                if (index < records.lastIndex) {
                    HorizontalDivider(thickness = 0.4.dp, color = colors.outline.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@Composable
private fun TableHeaderCell(text: String, modifier: Modifier) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
        textAlign = TextAlign.Center
    )
}

private fun shortNum(d: Double): String =
    if (d == d.toLong().toDouble()) d.toLong().toString()
    else "%.1f".format(d)
