package com.devvikram.expensetracker.expensetracker.service

import com.devvikram.expensetracker.expensetracker.enums.AuditAction
import com.devvikram.expensetracker.expensetracker.repository.ExpenseRepository
import com.lowagie.text.Document
import com.lowagie.text.FontFactory
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.time.LocalDateTime

@Service
class ExportService(
    private val expenseRepository: ExpenseRepository,
    private val auditLogService: AuditLogService
) {

    @Transactional(readOnly = true)
    fun exportCsv(userId: Long): ByteArray {
        val expenses = expenseRepository.findByUserId(userId)
            .sortedByDescending { it.createdAt }

        val out = ByteArrayOutputStream()
        val writer = OutputStreamWriter(out, Charsets.UTF_8)

        val format = CSVFormat.DEFAULT.builder()
            .setHeader("ID", "Title", "Amount", "Category", "Note", "Created At")
            .build()

        CSVPrinter(writer, format).use { printer ->
            expenses.forEach { expense ->
                printer.printRecord(
                    expense.id,
                    expense.title,
                    expense.amount,
                    expense.category.name,
                    expense.note ?: "",
                    expense.createdAt.toString()
                )
            }
        }

        auditLogService.log(
            userId     = userId,
            action     = AuditAction.REPORT_EXPORTED,
            entityType = "Expense",
            newValue   = mapOf("format" to "csv", "count" to expenses.size, "exportedAt" to LocalDateTime.now().toString())
        )

        return out.toByteArray()
    }

    @Transactional(readOnly = true)
    fun exportPdf(userId: Long): ByteArray {
        val expenses = expenseRepository.findByUserId(userId)
            .sortedByDescending { it.createdAt }

        val out = ByteArrayOutputStream()
        val document = Document()
        PdfWriter.getInstance(document, out)
        document.open()

        // Title
        val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f)
        document.add(Paragraph("Expense Report", titleFont))
        document.add(Paragraph("Generated: ${LocalDateTime.now()}"))
        document.add(Paragraph("Total expenses: ${expenses.size}"))
        document.add(Paragraph(" "))

        // Table
        val table = PdfPTable(6)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(0.5f, 2.5f, 1.2f, 1.5f, 2.0f, 1.8f))

        val headers = listOf("ID", "Title", "Amount (₹)", "Category", "Note", "Created At")
        val headerColor = Color(63, 81, 181)
        headers.forEach { header ->
            val cell = PdfPCell(Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, Color.WHITE)))
            cell.backgroundColor = headerColor
            cell.paddingTop = 6f
            cell.paddingBottom = 6f
            table.addCell(cell)
        }

        val altColor = Color(240, 240, 240)
        expenses.forEachIndexed { index, expense ->
            val bg = if (index % 2 == 0) Color.WHITE else altColor
            fun cell(text: String) = PdfPCell(Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 9f))).also {
                it.backgroundColor = bg
                it.paddingTop = 4f
                it.paddingBottom = 4f
            }
            table.addCell(cell(expense.id.toString()))
            table.addCell(cell(expense.title))
            table.addCell(cell("%.2f".format(expense.amount)))
            table.addCell(cell(expense.category.name))
            table.addCell(cell(expense.note ?: "-"))
            table.addCell(cell(expense.createdAt.toString().take(19)))
        }

        document.add(table)
        document.close()

        auditLogService.log(
            userId     = userId,
            action     = AuditAction.REPORT_EXPORTED,
            entityType = "Expense",
            newValue   = mapOf("format" to "pdf", "count" to expenses.size, "exportedAt" to LocalDateTime.now().toString())
        )

        return out.toByteArray()
    }
}
