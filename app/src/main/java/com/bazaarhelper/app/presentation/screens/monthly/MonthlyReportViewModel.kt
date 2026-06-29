package com.bazaarhelper.app.presentation.screens.monthly

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bazaarhelper.app.R
import com.bazaarhelper.app.data.repository.RecordRepository
import com.bazaarhelper.app.domain.model.MonthlyReport
import com.bazaarhelper.app.presentation.components.formatArabic
import com.bazaarhelper.app.presentation.components.getMonthName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import javax.inject.Inject

data class MonthlyReportUiState(
    val selectedYear: Int = LocalDate.now().year,
    val selectedMonth: Int = LocalDate.now().monthValue,
    val report: MonthlyReport? = null,
    val isLoading: Boolean = true,
    val isExporting: Boolean = false
)

@HiltViewModel
class MonthlyReportViewModel @Inject constructor(
    private val repository: RecordRepository
) : ViewModel() {

    private val _year = MutableStateFlow(LocalDate.now().year)
    private val _month = MutableStateFlow(LocalDate.now().monthValue)

    private val _uiState = MutableStateFlow(MonthlyReportUiState())
    val uiState: StateFlow<MonthlyReportUiState> = _uiState.asStateFlow()

    init {
        combine(_year, _month) { y, m -> y to m }
            .flatMapLatest { (year, month) ->
                _uiState.update { it.copy(isLoading = true, selectedYear = year, selectedMonth = month) }
                repository.getMonthlyReport(year, month).map { report ->
                    _uiState.update { it.copy(report = report, isLoading = false) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onYearChange(year: Int) = _year.update { year }
    fun onMonthChange(month: Int) = _month.update { month }

    fun exportPdf(context: Context) {
        Log.d("PDF_EXPORT", "exportPdf called")
        val report = uiState.value.report
        if (report == null) {
            Log.e("PDF_EXPORT", "Report is null")
            return
        }
        if (report.daysCount <= 0) {
            Log.e("PDF_EXPORT", "Report has no records")
            return
        }

        viewModelScope.launch {
            try {
                Log.d("PDF_EXPORT", "Starting export process")
                _uiState.update { it.copy(isExporting = true) }
                
                val file = withContext(Dispatchers.IO) { 
                    Log.d("PDF_EXPORT", "Building PDF in IO thread")
                    buildPdf(context, report) 
                }
                
                Log.d("PDF_EXPORT", "PDF built at: ${file.absolutePath}")
                _uiState.update { it.copy(isExporting = false) }
                
                val uri: Uri = FileProvider.getUriForFile(
                    context, "${context.packageName}.provider", file
                )
                Log.d("PDF_EXPORT", "URI generated: $uri")

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Log.d("PDF_EXPORT", "Activity started")
            } catch (e: Exception) {
                Log.e("PDF_EXPORT", "Error during export", e)
                _uiState.update { it.copy(isExporting = false) }
            }
        }
    }

    private fun buildPdf(context: Context, report: MonthlyReport): File {
        val pageWidth = 595   // A4 pt
        val pageHeight = 842
        val marginL = 40f
        val marginR = pageWidth - 40f
        
        val isArabic = context.resources.configuration.locales[0].language == "ar"

        val pdfDoc = PdfDocument()

        // ── Paints ──────────────────────────────────────────────────────────
        val titlePaint = Paint().apply {
            textSize = 22f; isFakeBoldText = true; textAlign = if (isArabic) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val headPaint = Paint().apply {
            textSize = 15f; isFakeBoldText = true; textAlign = if (isArabic) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val bodyPaint = Paint().apply { 
            textSize = 13f; textAlign = if (isArabic) Paint.Align.RIGHT else Paint.Align.LEFT 
        }
        val cellPaint = Paint().apply { textSize = 12f; textAlign = Paint.Align.CENTER }
        val headerBg = Paint().apply { color = Color.parseColor("#673AB7") } // Purple
        val rowEven = Paint().apply { color = Color.parseColor("#F3E5F5") } // Light Purple
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

        var y = 60f

        var currentPage: PdfDocument.Page? = null

        fun newPage(): Canvas {
            val pi = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdfDoc.pages.size + 1).create()
            currentPage = pdfDoc.startPage(pi)
            y = 60f
            return currentPage!!.canvas
        }

        var canvas = newPage()
        val textX = if (isArabic) marginR else marginL

        fun writeLine(text: String, paint: Paint, x: Float = textX) {
            canvas.drawText(text, x, y, paint)
            y += paint.textSize + 10f
        }

        fun gap(dp: Float = 12f) { y += dp }

        // ── Title ────────────────────────────────────────────────────────────
        canvas.drawText("Bazaar Helper", textX, y, titlePaint); y += 35f
        
        val monthTitle = "${context.getString(R.string.monthly_report)} ${getMonthName(context, report.month)} ${report.year}"
        canvas.drawText(monthTitle, textX, y, titlePaint.also { it.textSize = 17f }); y += 30f
        
        canvas.drawLine(marginL, y, marginR, y, linePaint); gap(20f)

        // ── Summary ──────────────────────────────────────────────────────────
        writeLine(context.getString(R.string.monthly_stats), headPaint)
        gap(8f)
        
        val currency = context.getString(R.string.currency_symbol)
        writeLine("${context.getString(R.string.total_sales)}: ${"%.2f".format(java.util.Locale.ROOT, report.totalSales)} $currency", bodyPaint)
        writeLine("${context.getString(R.string.total_purchases)}: ${"%.2f".format(java.util.Locale.ROOT, report.totalPurchases)} $currency", bodyPaint)
        writeLine("${context.getString(R.string.profit)}: ${"%.2f".format(java.util.Locale.ROOT, report.totalProfit)} $currency", bodyPaint)
        writeLine("${context.getString(R.string.days_count)}: ${report.daysCount}", bodyPaint)
        gap(20f)
        canvas.drawLine(marginL, y, marginR, y, linePaint); gap(15f)

        // ── Table ─────────────────────────────────────────────────────────────
        writeLine(context.getString(R.string.daily_details), headPaint)
        gap(10f)

        val col0 = marginL
        val col1 = marginL + 120f
        val col2 = marginL + 240f
        val col3 = marginL + 360f
        val rowH = 22f

        // Table header
        canvas.drawRect(col0, y - 16f, marginR, y + 6f, headerBg)
        val whiteCell = cellPaint.apply { color = Color.WHITE }
        canvas.drawText(context.getString(R.string.date), col0 + 40f, y, whiteCell)
        canvas.drawText(context.getString(R.string.sales), col1 + 40f, y, whiteCell)
        canvas.drawText(context.getString(R.string.purchases), col2 + 40f, y, whiteCell)
        canvas.drawText(context.getString(R.string.profit), col3 + 30f, y, whiteCell)
        y += rowH

        cellPaint.color = Color.BLACK
        report.records.forEachIndexed { i, rec ->
            if (y > pageHeight - 60) {
                pdfDoc.finishPage(currentPage)
                canvas = newPage()
            }
            if (i % 2 == 0) canvas.drawRect(col0, y - 15f, marginR, y + 7f, rowEven)
            canvas.drawText(rec.date.formatArabic(), col0 + 40f, y, cellPaint)
            canvas.drawText("%.1f".format(java.util.Locale.ROOT, rec.sales), col1 + 40f, y, cellPaint)
            canvas.drawText("%.1f".format(java.util.Locale.ROOT, rec.purchases), col2 + 40f, y, cellPaint)
            val profitPaint = Paint(cellPaint).apply {
                color = if (rec.profit >= 0) Color.parseColor("#1B5E20") else Color.RED
                isFakeBoldText = true
            }
            canvas.drawText("%.1f".format(java.util.Locale.ROOT, rec.profit), col3 + 30f, y, profitPaint)
            y += rowH
            canvas.drawLine(col0, y - rowH + 7f, marginR, y - rowH + 7f, linePaint)
        }

        pdfDoc.finishPage(currentPage)

        val dir = File(context.getExternalFilesDir(null), "reports").apply { mkdirs() }
        val fileName = "${context.getString(R.string.monthly_report)}_${report.year}_${"%02d".format(java.util.Locale.ROOT, report.month)}.pdf"
        val file = File(dir, fileName)
        FileOutputStream(file).use { pdfDoc.writeTo(it) }
        pdfDoc.close()
        return file
    }
}
