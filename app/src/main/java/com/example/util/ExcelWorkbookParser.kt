package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.example.data.model.CompanySheetData
import com.example.data.model.CompanyWorkbookData
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ExcelWorkbookParser {

  /**
   * Parses an Excel (.xlsx), CSV, or TSV file from a content Uri into a multi-tab CompanyWorkbookData.
   */
  fun parseWorkbookFromUri(
    context: Context,
    uri: Uri,
    fileName: String,
    uploadedBy: String
  ): CompanyWorkbookData {
    val lower = fileName.lowercase()
    val sheets = mutableListOf<CompanySheetData>()

    try {
      context.contentResolver.openInputStream(uri)?.use { inputStream ->
        if (lower.endsWith(".csv") || lower.endsWith(".txt") || lower.endsWith(".tsv")) {
          val sheet = parseDelimitedStream(inputStream, fileName.substringBeforeLast('.'))
          sheets.add(sheet)
        } else {
          // Standard modern Excel .xlsx parsing
          val parsedSheets = parseXlsxStream(inputStream)
          if (parsedSheets.isNotEmpty()) {
            sheets.addAll(parsedSheets)
          } else {
            // Fallback: try delimited
            val fallbackSheet = parseDelimitedStream(inputStream, "Sheet 1")
            sheets.add(fallbackSheet)
          }
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    if (sheets.isEmpty()) {
      return getDefaultSampleWorkbook(uploadedBy = uploadedBy, customFileName = fileName)
    }

    return CompanyWorkbookData(
      fileName = fileName,
      uploadedBy = uploadedBy,
      uploadedAt = System.currentTimeMillis(),
      sheets = sheets
    )
  }

  /**
   * Native lightweight .xlsx parser:
   * .xlsx files are standard ZIP containers with XML files.
   * 1. Reads 'xl/workbook.xml' to discover sheet names and ordering.
   * 2. Reads 'xl/sharedStrings.xml' to populate string lookup table.
   * 3. Reads 'xl/worksheets/sheet*.xml' to extract cell rows and columns.
   */
  private fun parseXlsxStream(inputStream: InputStream): List<CompanySheetData> {
    val sheets = mutableListOf<CompanySheetData>()
    val zipBytes = inputStream.readBytes()

    // 1. Extract shared strings
    val sharedStrings = extractSharedStrings(zipBytes)

    // 2. Extract sheet names from xl/workbook.xml
    val sheetNames = extractSheetNames(zipBytes)

    // 3. Extract worksheet xml files
    val worksheetEntries = findWorksheetEntries(zipBytes)

    for (i in worksheetEntries.indices) {
      val entryName = worksheetEntries[i]
      val rawSheetName = if (i < sheetNames.size) sheetNames[i] else "Sheet ${i + 1}"
      val (headers, rows) = extractSheetRowsAndHeaders(zipBytes, entryName, sharedStrings)

      if (headers.isNotEmpty() || rows.isNotEmpty()) {
        sheets.add(
          CompanySheetData(
            sheetName = rawSheetName,
            headers = headers,
            rows = rows
          )
        )
      }
    }

    return sheets
  }

  private fun extractSharedStrings(zipBytes: ByteArray): List<String> {
    val strings = mutableListOf<String>()
    try {
      val zis = ZipInputStream(zipBytes.inputStream())
      var entry: ZipEntry? = zis.nextEntry
      while (entry != null) {
        if (entry.name.equals("xl/sharedStrings.xml", ignoreCase = true)) {
          val parser = Xml.newPullParser()
          parser.setInput(zis, "UTF-8")
          var eventType = parser.eventType
          var insideT = false
          val currentText = StringBuilder()

          while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
              XmlPullParser.START_TAG -> {
                if (parser.name.equals("t", ignoreCase = true)) {
                  insideT = true
                  currentText.setLength(0)
                } else if (parser.name.equals("si", ignoreCase = true)) {
                  currentText.setLength(0)
                }
              }
              XmlPullParser.TEXT -> {
                if (insideT) {
                  currentText.append(parser.text)
                }
              }
              XmlPullParser.END_TAG -> {
                if (parser.name.equals("t", ignoreCase = true)) {
                  insideT = false
                } else if (parser.name.equals("si", ignoreCase = true)) {
                  strings.add(currentText.toString().trim())
                  currentText.setLength(0)
                }
              }
            }
            eventType = parser.next()
          }
          break
        }
        zis.closeEntry()
        entry = zis.nextEntry
      }
      zis.close()
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return strings
  }

  private fun extractSheetNames(zipBytes: ByteArray): List<String> {
    val names = mutableListOf<String>()
    try {
      val zis = ZipInputStream(zipBytes.inputStream())
      var entry: ZipEntry? = zis.nextEntry
      while (entry != null) {
        if (entry.name.equals("xl/workbook.xml", ignoreCase = true)) {
          val parser = Xml.newPullParser()
          parser.setInput(zis, "UTF-8")
          var eventType = parser.eventType

          while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name.equals("sheet", ignoreCase = true)) {
              val sheetName = parser.getAttributeValue(null, "name")
              if (!sheetName.isNullOrBlank()) {
                names.add(sheetName)
              }
            }
            eventType = parser.next()
          }
          break
        }
        zis.closeEntry()
        entry = zis.nextEntry
      }
      zis.close()
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return names
  }

  private fun findWorksheetEntries(zipBytes: ByteArray): List<String> {
    val entries = mutableListOf<String>()
    try {
      val zis = ZipInputStream(zipBytes.inputStream())
      var entry: ZipEntry? = zis.nextEntry
      while (entry != null) {
        val name = entry.name
        if (name.startsWith("xl/worksheets/sheet", ignoreCase = true) && name.endsWith(".xml", ignoreCase = true)) {
          entries.add(name)
        }
        zis.closeEntry()
        entry = zis.nextEntry
      }
      zis.close()
    } catch (e: Exception) {
      e.printStackTrace()
    }
    entries.sort()
    return entries
  }

  private fun extractSheetRowsAndHeaders(
    zipBytes: ByteArray,
    sheetEntryName: String,
    sharedStrings: List<String>
  ): Pair<List<String>, List<List<String>>> {
    val allRows = mutableListOf<List<String>>()

    try {
      val zis = ZipInputStream(zipBytes.inputStream())
      var entry: ZipEntry? = zis.nextEntry
      while (entry != null) {
        if (entry.name.equals(sheetEntryName, ignoreCase = true)) {
          val parser = Xml.newPullParser()
          parser.setInput(zis, "UTF-8")
          var eventType = parser.eventType

          var currentRow = mutableListOf<String>()
          var currentCellType = ""
          var currentVal = StringBuilder()
          var insideV = false

          while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
              XmlPullParser.START_TAG -> {
                when {
                  parser.name.equals("row", ignoreCase = true) -> {
                    currentRow = mutableListOf()
                  }
                  parser.name.equals("c", ignoreCase = true) -> {
                    currentCellType = parser.getAttributeValue(null, "t") ?: ""
                    currentVal.setLength(0)
                  }
                  parser.name.equals("v", ignoreCase = true) -> {
                    insideV = true
                  }
                }
              }
              XmlPullParser.TEXT -> {
                if (insideV) {
                  currentVal.append(parser.text)
                }
              }
              XmlPullParser.END_TAG -> {
                when {
                  parser.name.equals("v", ignoreCase = true) -> {
                    insideV = false
                  }
                  parser.name.equals("c", ignoreCase = true) -> {
                    val rawVal = currentVal.toString().trim()
                    val cellText = if (currentCellType == "s") {
                      val idx = rawVal.toIntOrNull() ?: -1
                      if (idx in sharedStrings.indices) sharedStrings[idx] else rawVal
                    } else {
                      rawVal
                    }
                    currentRow.add(cellText)
                    currentVal.setLength(0)
                  }
                  parser.name.equals("row", ignoreCase = true) -> {
                    if (currentRow.any { it.isNotBlank() }) {
                      allRows.add(currentRow)
                    }
                  }
                }
              }
            }
            eventType = parser.next()
          }
          break
        }
        zis.closeEntry()
        entry = zis.nextEntry
      }
      zis.close()
    } catch (e: Exception) {
      e.printStackTrace()
    }

    if (allRows.isEmpty()) {
      return Pair(emptyList(), emptyList())
    }

    val headers = allRows.first().map { it.trim() }
    val dataRows = allRows.drop(1).filter { row -> row.any { it.isNotBlank() } }

    return Pair(headers, dataRows)
  }

  private fun parseDelimitedStream(inputStream: InputStream, sheetName: String): CompanySheetData {
    val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
    val rows = mutableListOf<List<String>>()
    var line = reader.readLine()

    while (line != null) {
      if (line.isNotBlank()) {
        val delimiter = if (line.contains('\t')) "\t" else if (line.contains(';')) ";" else ","
        val tokens = parseCsvLine(line, delimiter)
        if (tokens.isNotEmpty()) {
          rows.add(tokens)
        }
      }
      line = reader.readLine()
    }

    if (rows.isEmpty()) {
      return CompanySheetData(sheetName, emptyList(), emptyList())
    }

    val headers = rows.first()
    val data = rows.drop(1)
    return CompanySheetData(sheetName, headers, data)
  }

  private fun parseCsvLine(line: String, delimiter: String): List<String> {
    val result = mutableListOf<String>()
    var cur = StringBuilder()
    var inQuotes = false

    var i = 0
    while (i < line.length) {
      val c = line[i]
      if (c == '\"') {
        inQuotes = !inQuotes
      } else if (!inQuotes && line.startsWith(delimiter, i)) {
        result.add(cur.toString().trim())
        cur = StringBuilder()
        i += delimiter.length - 1
      } else {
        cur.append(c)
      }
      i++
    }
    result.add(cur.toString().trim())
    return result
  }

  /**
   * Pre-loaded default sample multi-tab EBL Company Profiles.
   */
  fun getDefaultSampleWorkbook(
    uploadedBy: String = "Eastern Bank PLC Corporate Admin",
    customFileName: String = "EBL_Corporate_Client_Profiles_Master.xlsx"
  ): CompanyWorkbookData {
    val sheet1 = CompanySheetData(
      sheetName = "Listed Corporates (কর্পোরেট ক্লায়েন্ট)",
      headers = listOf("Company Name", "Industry / Sector", "Category Tier", "Corporate Address", "Payroll Agreement", "Authorized Contact", "Contact Phone"),
      rows = listOf(
        listOf("Square Pharmaceuticals Ltd.", "Pharmaceuticals & Healthcare", "Tier A+ (Prime)", "Square Centre, 48 Mohakhali C/A, Dhaka-1212", "Active (EBL Payroll)", "Md. Tanvir Hossain (HR Head)", "+8801711001122"),
        listOf("Beximco Communications Ltd. (Akash)", "Telecommunications & Media", "Tier A (Corporate)", "SAMU Tower, Gulshan-1, Dhaka", "Active (EBL Payroll)", "Rezaul Karim (Finance Lead)", "+8801819223344"),
        listOf("PRAN-RFL Group", "FMCG & Agro Processing", "Tier A+ (Prime)", "PRAN-RFL Center, 105 Middle Badda, Dhaka", "Active (Full Salary)", "Kamrul Hasan (Group Admin)", "+8801730005566"),
        listOf("Grameenphone Ltd.", "Telecommunications", "MNC / Top Corporate", "GPHouse, Basundhara, Baridhara, Dhaka-1229", "Active (Executive Card)", "Fahim Ahmed (Corporate Affairs)", "+8801711556677"),
        listOf("Walton Hi-Tech Industries PLC", "Consumer Electronics & Tech", "Tier A+ (Prime)", "Chandra, Gazipur / Head Office: Plot-1088, Block-I, Bashundhara R/A", "Active (Corporate Card)", "Mahfuzur Rahman (Director)", "+8801678998877"),
        listOf("Renata PLC", "Pharmaceuticals", "Tier A (Corporate)", "Plot # 1, Milk Vita Road, Section-7, Mirpur, Dhaka", "Active (EBL Payroll)", "Syed Nazmul Huda (Finance)", "+8801912345678"),
        listOf("Apex Footwear Limited", "Leather & Footwear Manufacturing", "Tier A (Export)", "House # 6, Road # 137, Block # SE(D), Gulshan-1, Dhaka", "Active (Payroll Partner)", "Ashikur Rahman (HR Ops)", "+8801844112233"),
        listOf("ACI Limited", "Conglomerate & Healthcare", "Tier A+ (Prime)", "ACI Centre, 245 Tejgaon I/A, Dhaka-1208", "Active (Full Agreement)", "Dr. Arifuzzaman (HR Head)", "+8801713445566"),
        listOf("Bashundhara Group", "Conglomerate & Heavy Industry", "Tier A+ (Prime)", "Bashundhara Industrial Complex, Baridhara, Dhaka", "Active (Corporate Banking)", "Monirul Islam (Finance VP)", "+8801755667788"),
        listOf("Incepta Pharmaceuticals Ltd.", "Pharmaceuticals", "Tier A (Corporate)", "40 Shahid Tajuddin Ahmed Sarani, Tejgaon I/A, Dhaka", "Active (Corporate Card)", "Kazi Zahidul Islam", "+8801715667788")
      )
    )

    val sheet2 = CompanySheetData(
      sheetName = "Payroll Accounts (পেরোল পার্টনার্স)",
      headers = listOf("Company Name", "Agreement Date", "Employee Headcount", "Registered Office", "EBL Branch", "Dedicated RM", "Hotline / Email"),
      rows = listOf(
        listOf("Unilever Bangladesh Limited", "15 Jan 2021", "2,400+ Staff", "ZN Tower, Plot 2, Road 8, Gulshan-1, Dhaka", "Gulshan Branch", "Sabbir Hossain (RM-01)", "payroll@unilever-bd.com"),
        listOf("British American Tobacco BD", "10 Mar 2019", "1,850+ Staff", "New DOHS Road, Mohakhali, Dhaka-1206", "Principal Branch", "Farzana Yeasmin (RM-04)", "ebl.batb@bat.com"),
        listOf("Robi Axiata PLC", "22 Jul 2020", "3,100+ Staff", "53 Gulshan South Avenue, Gulshan 1, Dhaka", "Gulshan Branch", "Mahmudul Hasan (RM-02)", "corporate.payroll@robi.com.bd"),
        listOf("Nestle Bangladesh PLC", "05 Sep 2021", "950+ Staff", "Ninakabbo, Level 4, 227/A Tejgaon-Gulshan Link Road", "Tejgaon Branch", "Sabbir Hossain (RM-01)", "nestle.ebl@nestle.com"),
        listOf("Standard Chartered Global Tech", "18 Feb 2022", "1,200+ Staff", "Silver Tower, Gulshan-2, Dhaka", "North Gulshan Branch", "Tasnim Sultana (RM-03)", "hr.ebl@sc.com"),
        listOf("Marico Bangladesh Limited", "12 Nov 2020", "800+ Staff", "House 1, Road 1, Sector 1, Uttara, Dhaka", "Uttara Branch", "Rahim Uddin (RM-05)", "marico.accounts@marico.com")
      )
    )

    val sheet3 = CompanySheetData(
      sheetName = "SME Commercial (এসএমই ক্লায়েন্ট)",
      headers = listOf("Company Name", "SME Cluster", "Trade License No", "Operating Address", "Annual Turnover (Cr)", "Key Decision Maker", "Mobile"),
      rows = listOf(
        listOf("Bengal Plastics Ltd.", "Plastic & Consumer Goods", "TRAD/DSCC/012984", "45 Motijheel C/A, Dhaka", "BDT 85 Cr", "Jashim Uddin (MD)", "+8801711223300"),
        listOf("Akij City Supermarket Ltd.", "Retail & Commercial Hub", "TRAD/DNCC/087612", "Mohakhali Wireless Gate, Dhaka", "BDT 120 Cr", "Sheikh Nasir Uddin", "+8801819334455"),
        listOf("Rahimafrooz Distribution Ltd.", "Automotive & Energy Storage", "TRAD/DSCC/045123", "Arzed Chamber, 13 Mohakhali C/A, Dhaka", "BDT 95 Cr", "Munir Hossain (GM)", "+8801730112233"),
        listOf("Noman Group of Industries", "Textile & Garments Export", "TRAD/DNCC/099887", "Adamjee Court, Motijheel, Dhaka", "BDT 350 Cr", "Nurul Islam (Director)", "+8801714556677"),
        listOf("DBL Group (Dulal Brothers)", "RMG & Apparel Export", "TRAD/DNCC/065432", "BGMEA Bhaban, Hatirjheel, Dhaka", "BDT 280 Cr", "M. A. Rahim (Vice Chairman)", "+8801713009988"),
        listOf("Meghna Group of Industries", "Consumer Goods & Shipping", "TRAD/DSCC/076543", "Fresh Villa, House 15, Road 34, Gulshan-1", "BDT 500 Cr", "Mostafa Kamal (Chairman)", "+8801711998877")
      )
    )

    val sheet4 = CompanySheetData(
      sheetName = "MNC & Listed (বহুজাতিক ও তালিকাভুক্ত)",
      headers = listOf("Company Name", "Origin Country", "Stock Symbol", "Registered Headquarters", "Company Profile Summary", "EBL Facility Status"),
      rows = listOf(
        listOf("Huawei Technologies (BD) Ltd.", "China (Global MNC)", "Private MNC", "Lotus Kamal Tower-Two, 59 & 61 Gulshan Avenue, Dhaka", "Global ICT infrastructure and smart devices provider, 1,500+ professionals.", "Corporate Credit Cards, Payroll, Dual Currency L/C"),
        listOf("Samsung Electronics BD", "South Korea (MNC)", "Private MNC", "Silver Tower, 52 Gulshan Avenue, Dhaka-1212", "Leading consumer electronics and mobile division headquarter in Bangladesh.", "Corporate Cards, Dual Currency Corporate Facility"),
        listOf("Chevron Bangladesh", "USA (Energy MNC)", "Private MNC", "Park Heights, Plot 2, Road 9, Dhanmondi, Dhaka", "Largest natural gas producer in Bangladesh, operating Bibiyana & Jalalabad.", "Executive Travel Cards, Priority Corporate Accounts"),
        listOf("Ericsson Bangladesh Ltd.", "Sweden (Telecom MNC)", "Private MNC", "Crystal Palace, SE(D) 22, Road 140, Gulshan South Ave", "Global telecom equipment and network management service provider.", "Corporate Payroll & Dual Currency Cards"),
        listOf("MetLife Bangladesh (ALICO)", "USA (Insurance MNC)", "Private MNC", "MetLife Building, 18-20 Motijheel C/A, Dhaka-1000", "Oldest and largest international life insurance provider in Bangladesh.", "Corporate Collection Accounts & Corporate Cards")
      )
    )

    return CompanyWorkbookData(
      fileName = customFileName,
      uploadedBy = uploadedBy,
      uploadedAt = System.currentTimeMillis(),
      sheets = listOf(sheet1, sheet2, sheet3, sheet4)
    )
  }

  /**
   * JSON Serialization for Room AppSettingEntity
   */
  fun toJson(workbook: CompanyWorkbookData): String {
    val root = JSONObject()
    root.put("fileName", workbook.fileName)
    root.put("uploadedBy", workbook.uploadedBy)
    root.put("uploadedAt", workbook.uploadedAt)

    val sheetsArr = JSONArray()
    for (sheet in workbook.sheets) {
      val sObj = JSONObject()
      sObj.put("sheetName", sheet.sheetName)

      val hArr = JSONArray()
      sheet.headers.forEach { hArr.put(it) }
      sObj.put("headers", hArr)

      val rArr = JSONArray()
      for (row in sheet.rows) {
        val rowArr = JSONArray()
        row.forEach { rowArr.put(it) }
        rArr.put(rowArr)
      }
      sObj.put("rows", rArr)
      sheetsArr.put(sObj)
    }
    root.put("sheets", sheetsArr)
    return root.toString()
  }

  /**
   * JSON Deserialization
   */
  fun fromJson(jsonStr: String): CompanyWorkbookData? {
    if (jsonStr.isBlank()) return null
    return try {
      val root = JSONObject(jsonStr)
      val fileName = root.optString("fileName", "Company_Workbook.xlsx")
      val uploadedBy = root.optString("uploadedBy", "Admin")
      val uploadedAt = root.optLong("uploadedAt", System.currentTimeMillis())

      val sheetsArr = root.optJSONArray("sheets") ?: return null
      val sheets = mutableListOf<CompanySheetData>()

      for (i in 0 until sheetsArr.length()) {
        val sObj = sheetsArr.getJSONObject(i)
        val sheetName = sObj.optString("sheetName", "Sheet ${i + 1}")

        val hArr = sObj.optJSONArray("headers")
        val headers = mutableListOf<String>()
        if (hArr != null) {
          for (j in 0 until hArr.length()) {
            headers.add(hArr.getString(j))
          }
        }

        val rArr = sObj.optJSONArray("rows")
        val rows = mutableListOf<List<String>>()
        if (rArr != null) {
          for (k in 0 until rArr.length()) {
            val rowArr = rArr.getJSONArray(k)
            val row = mutableListOf<String>()
            for (m in 0 until rowArr.length()) {
              row.add(rowArr.getString(m))
            }
            rows.add(row)
          }
        }

        sheets.add(CompanySheetData(sheetName, headers, rows))
      }

      CompanyWorkbookData(fileName, uploadedBy, uploadedAt, sheets)
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }
}
