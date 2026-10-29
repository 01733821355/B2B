package com.example.util

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object GeminiTranslator {

  suspend fun translateChecklistItemsToBangla(
    items: List<String>
  ): Map<String, String> = withContext(Dispatchers.IO) {
    if (items.isEmpty()) return@withContext emptyMap()

    try {
      val prompt = """
        You are a senior banking documentation specialist at Eastern Bank PLC (EBL), Bangladesh.
        Translate the following banking checklist document titles into clear, formal, and authentic Bangladeshi banking Bengali (বাংলা).
        Preserve standard acronyms in English/parentheses, e.g., NID, e-TIN, MOA, AOA, CIB, BIN, DBR.
        Return ONLY a raw JSON object where keys are the original English names and values are the Bengali translations.
        Do not include markdown codeblocks or explanation.
        
        Items to translate:
        ${items.joinToString("\n") { "- $it" }}
      """.trimIndent()

      val rawResponse = withTimeoutOrNull(8_000L) {
        try {
          val model = Firebase.ai.generativeModel("gemini-2.5-flash")
          model.generateContent(prompt).text
        } catch (_: Exception) {
          try {
            val fallbackModel = Firebase.ai.generativeModel("gemini-1.5-flash")
            fallbackModel.generateContent(prompt).text
          } catch (_: Exception) {
            null
          }
        }
      }

      if (!rawResponse.isNullOrBlank()) {
        val parsed = parseJsonTranslations(rawResponse, items)
        if (parsed.isNotEmpty()) {
          return@withContext parsed
        }
      }
      fallbackToComprehensiveDictionary(items)
    } catch (_: Exception) {
      fallbackToComprehensiveDictionary(items)
    }
  }

  private fun parseJsonTranslations(rawText: String, originalItems: List<String>): Map<String, String> {
    val result = mutableMapOf<String, String>()
    try {
      val jsonBlock = rawText
        .replace("```json", "")
        .replace("```", "")
        .trim()
      val startIndex = jsonBlock.indexOf('{')
      val endIndex = jsonBlock.lastIndexOf('}')
      if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
        val content = jsonBlock.substring(startIndex + 1, endIndex)
        val entries = content.split(Regex(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"))
        for (entry in entries) {
          val kv = entry.split(Regex(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"))
          if (kv.size >= 2) {
            val k = kv[0].replace("\"", "").trim()
            val v = kv[1].replace("\"", "").trim()
            if (k.isNotBlank() && v.isNotBlank()) {
              result[k] = v
            }
          }
        }
      }
    } catch (_: Exception) {}

    // Complete missing with dictionary
    originalItems.forEach { itm ->
      if (!result.containsKey(itm)) {
        result[itm] = ChecklistTranslator.translateDocumentTitle(itm, ChecklistTranslator.ChecklistLanguage.BANGLA)
      }
    }
    return result
  }

  fun fallbackToComprehensiveDictionary(items: List<String>): Map<String, String> {
    return items.associateWith { item ->
      ChecklistTranslator.translateDocumentTitle(item, ChecklistTranslator.ChecklistLanguage.BANGLA)
    }
  }
}
