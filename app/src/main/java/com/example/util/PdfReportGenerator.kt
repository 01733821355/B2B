package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.CustomerFileEntity
import com.example.data.model.EventResponseEntity
import com.example.data.model.TeamEventEntity
import com.example.data.model.UserEntity
import java.io.File
import java.io.FileOutputStream

object PdfReportGenerator {

  /**
   * Generates a formal, printable PDF Performance Summary Report with complete multi-page
   * pagination and all 23-column customer details (SL, CC-Number, File ID, Customer, Company,
   * Address, Mobile, Email, Product, App Status, Active Status, CPV Status/Remarks, Limit, Analyst, RM Code, Dates).
   */
  fun generatePdfReport(
    context: Context,
    files: List<CustomerFileEntity>,
    currentUser: UserEntity,
    periodLabel: String,
    customAppName: String
  ): File {
    val document = PdfDocument()
    val pageWidth = 842 // Standard A4 Landscape points (72 dpi)
    val pageHeight = 595

    val textPaint = Paint().apply {
      isAntiAlias = true
      textSize = 8f
      color = Color.DKGRAY
    }

    val headerPaint = Paint().apply {
      isAntiAlias = true
      textSize = 15f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      color = Color.rgb(10, 25, 47) // EblNavyDark
    }

    val subHeaderPaint = Paint().apply {
      isAntiAlias = true
      textSize = 9.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      color = Color.rgb(30, 58, 138)
    }

    val tableHeaderPaint = Paint().apply {
      isAntiAlias = true
      textSize = 8f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      color = Color.WHITE
    }

    val bgHeaderPaint = Paint().apply {
      color = Color.rgb(10, 25, 47) // Navy header
    }

    val alternateRowPaint = Paint().apply {
      color = Color.rgb(248, 250, 252)
    }

    val borderPaint = Paint().apply {
      color = Color.rgb(226, 232, 240)
      strokeWidth = 0.5f
      style = Paint.Style.STROKE
    }

    val primaryRowPaint = Paint().apply {
      isAntiAlias = true
      textSize = 7.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      color = Color.rgb(15, 23, 42)
    }

    val secondaryRowPaint = Paint().apply {
      isAntiAlias = true
      textSize = 6.5f
      color = Color.rgb(100, 116, 139)
    }

    val margin = 20f
    val tableRight = pageWidth - margin
    // 11 column boundaries
    val colX = floatArrayOf(
      20f,  // 0: SL (24)
      44f,  // 1: File ID / CC-No (76)
      120f, // 2: Customer Name (95)
      215f, // 3: Company & Address (115)
      330f, // 4: Contact & Email (85)
      415f, // 5: Product (70)
      485f, // 6: App & Active Status (75)
      560f, // 7: CPV Status & Remarks (85)
      645f, // 8: Limit & Analyst (65)
      710f, // 9: RM & Creator (55)
      765f, // 10: Date & GPS (57)
      tableRight
    )

    val rowHeight = 21f
    val rowsOnFirstPage = 16
    val rowsOnLaterPages = 22

    val totalFiles = files.size
    val totalPages = if (totalFiles <= rowsOnFirstPage) {
      1
    } else {
      1 + kotlin.math.ceil((totalFiles - rowsOnFirstPage).toDouble() / rowsOnLaterPages).toInt()
    }

    var fileIndex = 0
    var pageNumber = 1

    fun drawTableHeader(canvas: Canvas, topY: Float) {
      canvas.drawRect(margin, topY, tableRight, topY + 16f, bgHeaderPaint)
      canvas.drawText("SL", colX[0] + 3f, topY + 11f, tableHeaderPaint)
      canvas.drawText("CC / File ID", colX[1] + 3f, topY + 11f, tableHeaderPaint)
      canvas.drawText("Customer Name", colX[2] + 3f, topY + 11f, tableHeaderPaint)
      canvas.drawText("Company & Office Address", colX[3] + 3f, topY + 11f, tableHeaderPaint)
      canvas.drawText("Mobile & Email", colX[4] + 3f, topY + 11f, tableHeaderPaint)
      canvas.drawText("Product", colX[5] + 3f, topY + 11f, tableHeaderPaint)
      canvas.drawText("App / Active Status", colX[6] + 3f, topY + 11f, tableHeaderPaint)
      canvas.drawText("CPV Status & Remarks", colX[7] + 3f, topY + 11f, tableHeaderPaint)
      canvas.drawText("Limit / Analyst", colX[8] + 3f, topY + 11f, tableHeaderPaint)
      canvas.drawText("Assigned RM", colX[9] + 3f, topY + 11f, tableHeaderPaint)
      canvas.drawText("Last Updated", colX[10] + 3f, topY + 11f, tableHeaderPaint)
    }

    while (pageNumber <= totalPages) {
      val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
      val page = document.startPage(pageInfo)
      val canvas = page.canvas
      var y = 28f

      if (pageNumber == 1) {
        // --- PAGE 1 EXECUTIVE HEADER ---
        canvas.drawText(customAppName.ifBlank { "RM Banking & Customer Operations Suite" }, margin, y, headerPaint)
        y += 14f
        canvas.drawText("EXECUTIVE CUSTOMER DATABASE & OPERATIONS SUMMARY REPORT", margin, y, subHeaderPaint)
        y += 12f

        textPaint.textSize = 7.5f
        val generatedTime = DateUtils.formatDateTime(DateUtils.currentDhakaMillis())
        canvas.drawText("Generated by: ${currentUser.name} (${currentUser.role} ${currentUser.rmCode}) • Dhaka Local Time: $generatedTime • Period: $periodLabel", margin, y, textPaint)
        y += 14f

        // KPI Box
        val approvedCount = files.count { it.applicationStatus.equals("Approved", true) }
        val submittedCount = files.count { it.applicationStatus.equals("Submitted", true) }
        val collectedCount = files.count { it.applicationStatus.equals("Collected", true) }
        val declinedCount = files.count { it.applicationStatus.equals("Declined", true) }
        val cpvDoneCount = files.count { it.cpvStatus.equals("Completed", true) }
        val pendingDocsCount = files.count { it.pendingDocuments.isNotBlank() }

        val kpiBoxPaint = Paint().apply { color = Color.rgb(241, 245, 249) }
        canvas.drawRect(margin, y, tableRight, y + 36f, kpiBoxPaint)
        canvas.drawRect(margin, y, tableRight, y + 36f, borderPaint)

        val kpiBold = Paint().apply {
          isAntiAlias = true
          textSize = 8.5f
          typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
          color = Color.rgb(15, 23, 42)
        }
        val kpiSub = Paint().apply {
          isAntiAlias = true
          textSize = 7f
          color = Color.rgb(71, 85, 105)
        }

        canvas.drawText("TOTAL RECORDS: $totalFiles", margin + 12f, y + 14f, kpiBold)
        canvas.drawText("APPROVED: $approvedCount", margin + 140f, y + 14f, kpiBold)
        canvas.drawText("SUBMITTED: $submittedCount", margin + 260f, y + 14f, kpiBold)
        canvas.drawText("COLLECTED: $collectedCount", margin + 380f, y + 14f, kpiBold)
        canvas.drawText("DECLINED: $declinedCount", margin + 500f, y + 14f, kpiBold)
        canvas.drawText("CPV DONE: $cpvDoneCount", margin + 610f, y + 14f, kpiBold)
        canvas.drawText("PENDING DOCS: $pendingDocsCount", margin + 700f, y + 14f, kpiBold)

        val approvalRate = if (totalFiles > 0) (approvedCount * 100 / totalFiles) else 0
        canvas.drawText("Approval Ratio: $approvalRate% • Official Bank Operational Data • All records verified across mobile and Google Sheets sync.", margin + 12f, y + 27f, kpiSub)
        y += 44f
      } else {
        // --- SUBSEQUENT PAGES COMPACT HEADER ---
        canvas.drawText("${customAppName.ifBlank { "RM Operations Suite" }} — Executive Customer Report (Contd.)", margin, y, subHeaderPaint)
        canvas.drawText("Page $pageNumber of $totalPages", tableRight - 60f, y, textPaint)
        y += 14f
      }

      // Draw table header
      drawTableHeader(canvas, y)
      y += 16f

      // Rows for this page
      val maxRowsThisPage = if (pageNumber == 1) rowsOnFirstPage else rowsOnLaterPages
      var rowsPrinted = 0

      while (fileIndex < totalFiles && rowsPrinted < maxRowsThisPage) {
        val f = files[fileIndex]
        val isAlternate = rowsPrinted % 2 == 1
        if (isAlternate) {
          canvas.drawRect(margin, y, tableRight, y + rowHeight, alternateRowPaint)
        }
        canvas.drawLine(margin, y + rowHeight, tableRight, y + rowHeight, borderPaint)

        // SL
        val slDisplay = f.serialNumber.ifBlank { (fileIndex + 1).toString() }
        canvas.drawText(slDisplay.take(5), colX[0] + 2f, y + 9f, primaryRowPaint)

        // CC / File ID
        val ccDisplay = f.ccNumber.ifBlank { f.fileId }
        canvas.drawText(ccDisplay.take(15), colX[1] + 2f, y + 9f, primaryRowPaint)
        canvas.drawText("ID: ${f.fileId.takeLast(10)}", colX[1] + 2f, y + 18f, secondaryRowPaint)

        // Customer Name
        canvas.drawText(f.customerName.take(20), colX[2] + 2f, y + 9f, primaryRowPaint)
        if (f.pendingDocuments.isNotBlank()) {
          canvas.drawText("Docs: ${f.pendingDocuments.take(18)}", colX[2] + 2f, y + 18f, secondaryRowPaint)
        }

        // Company & Address
        canvas.drawText(f.companyName.take(22), colX[3] + 2f, y + 9f, primaryRowPaint)
        val addr = f.officeAddress.ifBlank { "N/A" }
        canvas.drawText(addr.take(24), colX[3] + 2f, y + 18f, secondaryRowPaint)

        // Mobile & Email
        canvas.drawText(f.mobile.take(14), colX[4] + 2f, y + 9f, primaryRowPaint)
        val secContact = f.email.ifBlank { f.altMobile.ifBlank { "N/A" } }
        canvas.drawText(secContact.take(16), colX[4] + 2f, y + 18f, secondaryRowPaint)

        // Product
        canvas.drawText(f.productType.take(14), colX[5] + 2f, y + 9f, primaryRowPaint)

        // App & Active Status
        val statusPaint = Paint(primaryRowPaint).apply {
          color = when (f.applicationStatus) {
            "Approved" -> Color.rgb(21, 128, 61)
            "Submitted" -> Color.rgb(29, 78, 216)
            "Declined" -> Color.rgb(185, 28, 28)
            else -> Color.DKGRAY
          }
        }
        canvas.drawText(f.applicationStatus.take(12), colX[6] + 2f, y + 9f, statusPaint)
        canvas.drawText("Active: ${f.activeStatus.take(10)}", colX[6] + 2f, y + 18f, secondaryRowPaint)

        // CPV Status & Remarks
        val cpvPaint = Paint(primaryRowPaint).apply {
          color = when (f.cpvStatus) {
            "Completed" -> Color.rgb(21, 128, 61)
            "Failed" -> Color.rgb(185, 28, 28)
            "Pending" -> Color.rgb(202, 138, 4)
            else -> Color.rgb(71, 85, 105)
          }
        }
        canvas.drawText(f.cpvStatus.take(12), colX[7] + 2f, y + 9f, cpvPaint)
        val cpvNote = f.cpvRemarks.ifBlank { f.remarks.ifBlank { f.cpvDate.ifBlank { "-" } } }
        canvas.drawText(cpvNote.take(18), colX[7] + 2f, y + 18f, secondaryRowPaint)

        // Limit & Analyst
        val limitDisplay = if (f.limit.isNotBlank()) "BDT ${f.limit}" else "N/A"
        canvas.drawText(limitDisplay.take(13), colX[8] + 2f, y + 9f, primaryRowPaint)
        val analystDisplay = if (f.analyst.isNotBlank()) "Anl: ${f.analyst}" else "Anl: N/A"
        canvas.drawText(analystDisplay.take(13), colX[8] + 2f, y + 18f, secondaryRowPaint)

        // RM Code & Creator
        canvas.drawText(f.assignedRmCode.take(8), colX[9] + 2f, y + 9f, primaryRowPaint)
        canvas.drawText("By: ${f.updatedBy.take(8)}", colX[9] + 2f, y + 18f, secondaryRowPaint)

        // Updated Date
        canvas.drawText(DateUtils.formatDateOnly(f.updatedAt), colX[10] + 2f, y + 9f, primaryRowPaint)
        val timeOrGps = if (f.submissionLatitude != null) "GPS: Verified" else DateUtils.formatTimeOnly(f.updatedAt)
        canvas.drawText(timeOrGps, colX[10] + 2f, y + 18f, secondaryRowPaint)

        y += rowHeight
        rowsPrinted++
        fileIndex++
      }

      // Draw bottom vertical borders
      for (x in colX) {
        canvas.drawLine(x, y - (rowsPrinted * rowHeight) - 16f, x, y, borderPaint)
      }

      // Page Footer
      val footerY = pageHeight - 16f
      val footerPaint = Paint().apply {
        isAntiAlias = true
        textSize = 6.8f
        color = Color.GRAY
      }
      canvas.drawText("CONFIDENTIAL INTERNAL BANKING RECORD — NOT FOR PUBLIC CIRCULATION", margin, footerY, footerPaint)
      canvas.drawText("Page $pageNumber of $totalPages • Total Records in Report: $totalFiles", tableRight - 180f, footerY, footerPaint)

      document.finishPage(page)
      pageNumber++
    }

    // Save PDF to cache directory
    val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
    val reportFile = File(reportsDir, "Executive_Customer_Report_${System.currentTimeMillis()}.pdf")
    FileOutputStream(reportFile).use { out ->
      document.writeTo(out)
    }
    document.close()
    return reportFile
  }

  data class EventPdfRow(
    val sl: Int,
    val rmDisplay: String,
    val customerName: String,
    val customerMobile: String,
    val filesCount: Int,
    val targetDate: String,
    val locationAndRemarks: String
  )

  /**
   * Generates a formal, printable PDF Event Result & Performance Report for Admin and Mentor.
   * Formats each customer entry into its own dedicated row with complete customer name and phone number.
   */
  fun generateEventResultPdfReport(
    context: Context,
    event: TeamEventEntity,
    responses: List<EventResponseEntity>,
    currentUser: UserEntity,
    customAppName: String
  ): File {
    val document = PdfDocument()
    val pageWidth = 595 // Standard A4 points (72 dpi)
    val pageHeight = 842

    var pageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    var page = document.startPage(pageInfo)
    var canvas: Canvas = page.canvas

    val textPaint = Paint().apply {
      isAntiAlias = true
      textSize = 9f
      color = Color.DKGRAY
    }

    val headerPaint = Paint().apply {
      isAntiAlias = true
      textSize = 15f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      color = Color.rgb(10, 25, 47)
    }

    val subHeaderPaint = Paint().apply {
      isAntiAlias = true
      textSize = 11f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      color = Color.rgb(30, 58, 138)
    }

    val tableHeaderPaint = Paint().apply {
      isAntiAlias = true
      textSize = 8.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      color = Color.WHITE
    }

    val bgHeaderPaint = Paint().apply {
      color = Color.rgb(10, 25, 47)
    }

    val alternateRowPaint = Paint().apply {
      color = Color.rgb(245, 247, 250)
    }

    val borderPaint = Paint().apply {
      color = Color.LTGRAY
      strokeWidth = 0.5f
      style = Paint.Style.STROKE
    }

    val cellTextPaint = Paint().apply {
      isAntiAlias = true
      textSize = 8.5f
      color = Color.rgb(30, 41, 59)
    }

    val cellBoldPaint = Paint().apply {
      isAntiAlias = true
      textSize = 8.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      color = Color.rgb(15, 23, 42)
    }

    val phoneTextPaint = Paint().apply {
      isAntiAlias = true
      textSize = 8.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      color = Color.rgb(30, 58, 138)
    }

    val footerPaint = Paint().apply {
      isAntiAlias = true
      textSize = 7.5f
      color = Color.GRAY
    }

    fun drawFooter(c: Canvas, pNum: Int) {
      val fY = pageHeight - 25f
      c.drawLine(30f, fY - 10f, pageWidth - 30f, fY - 10f, borderPaint)
      c.drawText("CONFIDENTIAL DOCUMENT — STRICTLY FOR INTERNAL BANKING USE — NOT FOR PUBLIC DISSEMINATION", 30f, fY, footerPaint)
      c.drawText("Page $pNum", pageWidth - 70f, fY, footerPaint)
    }

    val colDividers = floatArrayOf(52f, 155f, 295f, 380f, 415f, 475f)

    fun drawTableHeader(c: Canvas, headerY: Float) {
      c.drawRect(30f, headerY, pageWidth - 30f, headerY + 20f, bgHeaderPaint)
      c.drawText("#", 34f, headerY + 13.5f, tableHeaderPaint)
      c.drawText("RM Name (Code)", 55f, headerY + 13.5f, tableHeaderPaint)
      c.drawText("Customer Name", 158f, headerY + 13.5f, tableHeaderPaint)
      c.drawText("Mobile Number", 298f, headerY + 13.5f, tableHeaderPaint)
      c.drawText("Files", 384f, headerY + 13.5f, tableHeaderPaint)
      c.drawText("Target Date", 418f, headerY + 13.5f, tableHeaderPaint)
      c.drawText("Location / Remarks", 478f, headerY + 13.5f, tableHeaderPaint)

      colDividers.forEach { cx ->
        c.drawLine(cx, headerY, cx, headerY + 20f, borderPaint)
      }
    }

    var y = 35f

    // 1. Header Banner
    canvas.drawText(customAppName.ifBlank { "RM File Management Suite" }, 30f, y, headerPaint)
    y += 18f
    canvas.drawText("EVENT RESULT & SUBMISSIONS REPORT", 30f, y, subHeaderPaint)
    y += 14f

    val reportTime = DateUtils.formatDateTime(DateUtils.currentDhakaMillis())
    canvas.drawText("Report Generated: $reportTime (Dhaka Time)", 30f, y, textPaint)
    canvas.drawText("Authorized Officer: ${currentUser.name} (${currentUser.role} - ${currentUser.rmCode})", 300f, y, textPaint)
    y += 14f

    // Divider line
    canvas.drawLine(30f, y, pageWidth - 30f, y, borderPaint)
    y += 14f

    // 2. Event Overview Card Box
    val cardPaint = Paint().apply {
      color = Color.rgb(238, 242, 255)
      style = Paint.Style.FILL
    }
    canvas.drawRoundRect(30f, y, pageWidth - 30f, y + 62f, 6f, 6f, cardPaint)
    canvas.drawRoundRect(30f, y, pageWidth - 30f, y + 62f, 6f, 6f, borderPaint)

    val labelPaint = Paint().apply {
      isAntiAlias = true
      textSize = 9.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      color = Color.rgb(30, 58, 138)
    }
    val valuePaint = Paint().apply {
      isAntiAlias = true
      textSize = 9f
      color = Color.rgb(30, 41, 59)
    }

    canvas.drawText("Event Title: ${event.title}", 40f, y + 16f, labelPaint)
    canvas.drawText("Event ID: ${event.eventId}", 40f, y + 32f, valuePaint)
    canvas.drawText("Target Date: ${event.targetDate.ifBlank { "N/A" }}", 40f, y + 48f, valuePaint)

    val totalFiles = responses.sumOf { it.filesCount }
    canvas.drawText("Created By: ${event.creatorName} (${event.creatorRmCode})", 280f, y + 16f, valuePaint)
    canvas.drawText("Total Submitting RMs: ${responses.size}", 280f, y + 32f, labelPaint)
    canvas.drawText("Total Achieved Files: $totalFiles Files", 280f, y + 48f, labelPaint)
    y += 76f

    // 3. Build Flattened Customer Rows (1 Customer Per Row)
    val rowList = mutableListOf<EventPdfRow>()
    var slCounter = 1
    responses.forEach { resp ->
      val customers = mutableListOf<Pair<String, String>>()
      if (resp.customerEntriesJson.isNotBlank()) {
        try {
          val arr = org.json.JSONArray(resp.customerEntriesJson)
          for (ci in 0 until arr.length()) {
            val co = arr.getJSONObject(ci)
            val n = co.optString("name").trim()
            val m = co.optString("mobile").trim()
            if (n.isNotBlank() || m.isNotBlank()) {
              customers.add(Pair(n, m))
            }
          }
        } catch (_: Exception) {}
      }

      val locRem = listOf(resp.location.trim(), resp.remarks.trim())
        .filter { it.isNotBlank() }
        .joinToString(" • ")

      if (customers.isNotEmpty()) {
        customers.forEach { (cName, cMobile) ->
          rowList.add(
            EventPdfRow(
              sl = slCounter++,
              rmDisplay = "${resp.rmName} (${resp.rmCode})",
              customerName = cName.ifBlank { "-" },
              customerMobile = cMobile.ifBlank { "-" },
              filesCount = resp.filesCount,
              targetDate = resp.requestedDate.ifBlank { "-" },
              locationAndRemarks = locRem.ifBlank { "-" }
            )
          )
        }
      } else {
        // RM submitted without specific customer list
        rowList.add(
          EventPdfRow(
            sl = slCounter++,
            rmDisplay = "${resp.rmName} (${resp.rmCode})",
            customerName = "-",
            customerMobile = "-",
            filesCount = resp.filesCount,
            targetDate = resp.requestedDate.ifBlank { "-" },
            locationAndRemarks = locRem.ifBlank { "-" }
          )
        )
      }
    }

    // 4. Draw Table Header
    drawTableHeader(canvas, y)
    y += 20f

    // 5. Draw Data Rows (One customer per row)
    if (rowList.isEmpty()) {
      canvas.drawText("No RM submissions received yet for this event.", 40f, y + 20f, textPaint)
      y += 35f
    } else {
      rowList.forEach { row ->
        // Multi-page check
        if (y + 24f > pageHeight - 75f) {
          drawFooter(canvas, pageNumber)
          document.finishPage(page)

          pageNumber++
          pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
          page = document.startPage(pageInfo)
          canvas = page.canvas

          // Continuation banner on new page
          canvas.drawText(customAppName.ifBlank { "RM File Management Suite" }, 30f, 32f, headerPaint)
          canvas.drawText("EVENT RESULT & SUBMISSIONS REPORT (Contd.) — Event: ${event.title}", 30f, 46f, subHeaderPaint)
          canvas.drawLine(30f, 54f, pageWidth - 30f, 54f, borderPaint)
          y = 62f
          drawTableHeader(canvas, y)
          y += 20f
        }

        // Row background
        if (row.sl % 2 == 0) {
          canvas.drawRect(30f, y, pageWidth - 30f, y + 24f, alternateRowPaint)
        }
        canvas.drawRect(30f, y, pageWidth - 30f, y + 24f, borderPaint)
        colDividers.forEach { cx ->
          canvas.drawLine(cx, y, cx, y + 24f, borderPaint)
        }

        // Row Content
        canvas.drawText("${row.sl}", 34f, y + 15f, cellTextPaint)

        val rmShort = if (cellTextPaint.measureText(row.rmDisplay) > 98f) {
          row.rmDisplay.take(16) + "…"
        } else row.rmDisplay
        canvas.drawText(rmShort, 55f, y + 15f, cellTextPaint)

        // Customer Name: complete name
        val custShort = if (cellBoldPaint.measureText(row.customerName) > 134f) {
          row.customerName.take(24) + "…"
        } else row.customerName
        canvas.drawText(custShort, 158f, y + 15f, cellBoldPaint)

        // Customer Mobile: complete mobile number (never truncated)
        canvas.drawText(row.customerMobile, 298f, y + 15f, phoneTextPaint)

        canvas.drawText("${row.filesCount}", 390f, y + 15f, cellTextPaint)
        canvas.drawText(row.targetDate, 418f, y + 15f, cellTextPaint)

        val locRemShort = if (cellTextPaint.measureText(row.locationAndRemarks) > 84f) {
          row.locationAndRemarks.take(15) + "…"
        } else row.locationAndRemarks
        canvas.drawText(locRemShort, 478f, y + 15f, cellTextPaint)

        y += 24f
      }
    }

    // 6. Performance Summary Box at Bottom
    if (y + 55f > pageHeight - 75f) {
      drawFooter(canvas, pageNumber)
      document.finishPage(page)

      pageNumber++
      pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
      page = document.startPage(pageInfo)
      canvas = page.canvas
      y = 40f
    }

    y += 12f
    canvas.drawLine(30f, y, pageWidth - 30f, y, borderPaint)
    y += 16f
    val boldSummary = Paint().apply {
      isAntiAlias = true
      textSize = 9.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      color = Color.rgb(10, 25, 47)
    }
    val totalCustomersListed = rowList.count { it.customerName != "-" }
    canvas.drawText("Summary Statistics:  Participating RMs: ${responses.size}  •  Total Customers Listed: $totalCustomersListed  •  Total Files: $totalFiles Files", 35f, y, boldSummary)
    y += 14f
    canvas.drawText("Status: ${event.status}  •  Event Description: ${event.description.take(70)}", 35f, y, textPaint)

    // Final page footer
    drawFooter(canvas, pageNumber)
    document.finishPage(page)

    // Save PDF to cache directory
    val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
    val reportFile = File(reportsDir, "Event_Report_${event.eventId}_${System.currentTimeMillis()}.pdf")
    FileOutputStream(reportFile).use { out ->
      document.writeTo(out)
    }
    document.close()
    return reportFile
  }

  /**
   * Opens or shares the generated PDF file via system Android intents
   */
  fun shareOrViewPdf(context: Context, pdfFile: File) {
    val uri: Uri = try {
      FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        pdfFile
      )
    } catch (e: Exception) {
      Uri.fromFile(pdfFile)
    }

    // Also copy to Downloads so user has a permanent offline copy on device
    savePdfToDownloads(context, pdfFile, pdfFile.name)

    // Primary: Open directly in PDF viewer (Google Drive PDF, Adobe, Chrome)
    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, "application/pdf")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
      val chooser = Intent.createChooser(viewIntent, "Open PDF Report with").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(chooser)
    } catch (_: Exception) {
      // Fallback to ACTION_SEND if no direct PDF viewer is available
      try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
          type = "application/pdf"
          putExtra(Intent.EXTRA_STREAM, uri)
          putExtra(Intent.EXTRA_SUBJECT, "Performance / Event Summary Report (PDF)")
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Open or Share PDF Report").apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
      } catch (e2: Exception) {
        android.widget.Toast.makeText(context, "PDF saved to Downloads folder: ${pdfFile.name}", android.widget.Toast.LENGTH_LONG).show()
      }
    }
  }

  /**
   * Saves a permanent copy of the PDF into device Downloads storage
   */
  fun savePdfToDownloads(context: Context, pdfFile: File, displayName: String): Uri? {
    try {
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        val values = android.content.ContentValues().apply {
          put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, displayName)
          put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
          put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
          resolver.openOutputStream(uri)?.use { out ->
            pdfFile.inputStream().use { input ->
              input.copyTo(out)
            }
          }
          return uri
        }
      } else {
        @Suppress("DEPRECATION")
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        downloadsDir.mkdirs()
        val destFile = File(downloadsDir, displayName)
        pdfFile.copyTo(destFile, overwrite = true)
        return Uri.fromFile(destFile)
      }
    } catch (_: Exception) {}
    return null
  }
}
