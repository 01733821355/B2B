package com.example.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class CompanySheetData(
  val sheetName: String,
  val headers: List<String>,
  val rows: List<List<String>>
)

@Immutable
data class CompanyWorkbookData(
  val fileName: String = "EBL_Corporate_Profiles.xlsx",
  val uploadedBy: String = "System Admin",
  val uploadedAt: Long = System.currentTimeMillis(),
  val sheets: List<CompanySheetData> = emptyList()
) {
  val totalRows: Int
    get() = sheets.sumOf { it.rows.size }
}

@Immutable
data class CompanySearchResult(
  val sheetName: String,
  val headers: List<String>,
  val rowData: List<String>,
  val matchedQuery: String
)
