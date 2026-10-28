package com.example.util

object ChecklistTranslator {

  enum class ChecklistLanguage(val label: String, val badge: String) {
    ENGLISH("English", "ENG"),
    BANGLA("বাংলা", "BAN"),
    BILINGUAL("বাংলা + English", "উভয়")
  }

  private val documentDict = mapOf(
    "nid" to "জাতীয় পরিচয়পত্র (NID)",
    "national id" to "জাতীয় পরিচয়পত্র",
    "national id card" to "জাতীয় পরিচয়পত্র (NID)",
    "smart nid" to "স্মার্ট জাতীয় পরিচয়পত্র",
    "smart card" to "স্মার্ট জাতীয় পরিচয়পত্র",
    "tin" to "ই-টিন সার্টিফিকেট (e-TIN)",
    "e-tin" to "ই-টিন সার্টিফিকেট (e-TIN)",
    "tin certificate" to "ই-টিন সার্টিফিকেট",
    "tax certificate" to "আয়কর সনদ / ট্যাক্স সার্টিফিকেট",
    "tax return acknowledgement" to "আয়কর রিটার্ন জমার প্রমাণপত্র (Tax Return Acknowledgement)",
    "office id" to "অফিস আইডি কার্ড (Office ID)",
    "office id card" to "অফিস আইডি কার্ড",
    "job id" to "চাকরির পরিচয়পত্র",
    "salary certificate" to "বেতন সনদ / স্যালারি সার্টিফিকেট",
    "salary slip" to "বেতন স্লিপ (Salary Slip)",
    "pay slip" to "পে-স্লিপ (Pay Slip)",
    "pay slip (last 3 months)" to "বিগত ৩ মাসের পে-স্লিপ",
    "account statement (6 months)" to "ব্যাংক স্টেটমেন্ট (বিগত ৬ মাসের)",
    "bank statement (6 months)" to "ব্যাংক একাউন্ট স্টেটমেন্ট (বিগত ৬ মাসের)",
    "bank statement" to "ব্যাংক একাউন্ট স্টেটমেন্ট",
    "account statement" to "একাউন্ট স্টেটমেন্ট",
    "trade license" to "হালনাগাদ ট্রেড লাইসেন্স",
    "trade license 2024-25" to "ট্রেড লাইসেন্স (২০২৪-২৫)",
    "trade license 2025-26" to "ট্রেড লাইসেন্স (২০২৫-২৬)",
    "trade license 2026-27" to "ট্রেড লাইসেন্স (২০২৬-২৭)",
    "bin" to "বিআইএন (BIN) / ভ্যাট সনদ",
    "bin certificate" to "বিআইএন (BIN) সনদপত্র",
    "vat certificate" to "ভ্যাট রেজিস্ট্রেশন সনদ",
    "loan certificate" to "লোন স্টেটমেন্ট / দায়বদ্ধতা সনদ",
    "liability certificate" to "দায়বদ্ধতা সনদপত্র",
    "card statement (month)" to "ক্রেডিট কার্ড স্টেটমেন্ট (সর্বশেষ মাস)",
    "card statement" to "ক্রেডিট কার্ড স্টেটমেন্ট",
    "card copy" to "ক্রেডিট কার্ডের কপি (উভয় পিঠ)",
    "credit card copy" to "ক্রেডিট কার্ডের কপি",
    "passport" to "পাসপোর্ট কপি",
    "passport copy" to "পাসপোর্ট সাইজ কপি / মূল পাসপোর্টের কপি",
    "utility bill" to "ইউটিলিটি বিল (গ্যাস/বিদ্যুৎ/পানির বিল)",
    "electricity bill" to "বিদ্যুৎ বিলের কপি",
    "gas bill" to "গ্যাস বিলের কপি",
    "visiting card" to "অফিসিয়াল ভিজিটিং কার্ড",
    "2 passport size photos" to "২ কপি পাসপোর্ট সাইজ রঙিন ছবি",
    "passport size photo" to "পাসপোর্ট সাইজ রঙিন ছবি",
    "nominee photo & nid" to "নমিনির ছবি ও জাতীয় পরিচয়পত্র",
    "nominee nid" to "নমিনির জাতীয় পরিচয়পত্র",
    "nominee photo" to "নমিনির ছবি",
    "moa" to "মেমোরেন্ডাম অব অ্যাসোসিয়েশন (MOA)",
    "aoa" to "আর্টিকেল অব অ্যাসোসিয়েশন (AOA)",
    "moa & aoa" to "মেমোরেন্ডাম ও আর্টিকেল অব অ্যাসোসিয়েশন (MOA & AOA)",
    "board resolution" to "বোর্ড রেজোলিউশন (Board Resolution)",
    "form 117" to "ফর্ম ১১৭ (Form 117)",
    "form xii" to "ফর্ম XII (Form XII)",
    "schedule x" to "তফসিল ১০ (Schedule X)",
    "partnership deed" to "অংশীদারি চুক্তিপত্র (Partnership Deed)",
    "cib undertaking form" to "সিআইবি আন্ডারটেকিং ফরম (CIB Undertaking)",
    "cheque leaf" to "বাতিলকৃত চেকের পাতা (Cancelled Cheque Leaf)",
    "cancelled cheque" to "বাতিলকৃত চেকের পাতা",
    "tin & tax return" to "টিন সনদ ও ট্যাক্স রিটার্ন",
    "limit enhancement request form" to "লিমিট বৃদ্ধির আবেদনপত্র (Limit Enhancement Request Form)",
    "income tax return" to "আয়কর রিটার্নের কপি",
    "dbr calculation sheet" to "ডিবিআর ক্যালকুলেশন শিট"
  )

  fun translateDocumentTitle(engTitle: String, language: ChecklistLanguage): String {
    val clean = engTitle.trim()
    if (clean.isBlank()) return clean

    val lower = clean.lowercase()
    val banglaTranslation = documentDict[lower] ?: run {
      // Fuzzy dictionary lookup
      documentDict.entries.firstOrNull { lower.contains(it.key) }?.value ?: autoTranslatePhrase(clean)
    }

    return when (language) {
      ChecklistLanguage.ENGLISH -> clean
      ChecklistLanguage.BANGLA -> banglaTranslation
      ChecklistLanguage.BILINGUAL -> {
        if (clean.equals(banglaTranslation, ignoreCase = true)) clean
        else "$banglaTranslation / $clean"
      }
    }
  }

  fun translatePresetName(presetName: String, language: ChecklistLanguage): String {
    val clean = presetName.trim()
    val bangla = when (clean.lowercase()) {
      "corporate card" -> "কর্পোরেট ক্রেডিট কার্ড"
      "credit card limit enhance" -> "ক্রেডিট কার্ড লিমিট বৃদ্ধি"
      "credit card" -> "ক্রেডিট কার্ড"
      "casa" -> "সেভিংস ও কারেন্ট ব্যাংক হিসাব"
      "b2b" -> "বি২বি ও পেরোল সেবা"
      "pending docs checklist" -> "পেন্ডিং ডকুমেন্টস চেকলিস্ট"
      else -> autoTranslatePhrase(clean)
    }
    return when (language) {
      ChecklistLanguage.ENGLISH -> clean
      ChecklistLanguage.BANGLA -> bangla
      ChecklistLanguage.BILINGUAL -> "$bangla ($clean)"
    }
  }

  fun getHeaderTemplate(language: ChecklistLanguage): String {
    return when (language) {
      ChecklistLanguage.ENGLISH ->
        "Dear {CUSTOMER_NAME},\nGreetings from Eastern Bank PLC (EBL).\nTo process your application for {PRESET_NAME}, please provide the following required documents:"
      ChecklistLanguage.BANGLA ->
        "প্রিয় {CUSTOMER_NAME},\nইস্টার্ন ব্যাংক পিএলসি (EBL)-এর পক্ষ থেকে আন্তরিক শুভেচ্ছা।\n{PRESET_NAME}-এর আবেদন প্রক্রিয়াকরণের জন্য অনুগ্রহ করে নিচের প্রয়োজনীয় ডকুমেন্টসগুলো প্রদান করুন:"
      ChecklistLanguage.BILINGUAL ->
        "প্রিয় / Dear {CUSTOMER_NAME},\nইস্টার্ন ব্যাংক পিএলসি (EBL)-এর পক্ষ থেকে শুভেচ্ছা। Greetings from Eastern Bank PLC.\n{PRESET_NAME}-এর জন্য প্রয়োজনীয় ডকুমেন্টস / Required documents for your application:"
    }
  }

  fun getRegardsTemplate(language: ChecklistLanguage): String {
    return when (language) {
      ChecklistLanguage.ENGLISH ->
        "For any query or assistance, please contact:\n{RM_NAME}\nRM Code: {RM_CODE}\nMobile: {RM_PHONE}\nEastern Bank PLC"
      ChecklistLanguage.BANGLA ->
        "যেকোনো তথ্য বা সহায়তার জন্য যোগাযোগ করুন:\n{RM_NAME}\nআরএম কোড: {RM_CODE}\nমোবাইল: {RM_PHONE}\nইস্টার্ন ব্যাংক পিএলসি"
      ChecklistLanguage.BILINGUAL ->
        "যেকোনো প্রয়োজনে যোগাযোগ করুন / For any query, contact:\n{RM_NAME}\nRM Code: {RM_CODE} | Mobile: {RM_PHONE}\nEastern Bank PLC (EBL)"
    }
  }

  fun getCategoryTitle(category: String, language: ChecklistLanguage): String {
    return when (category.uppercase()) {
      "COMPANY" -> when (language) {
        ChecklistLanguage.ENGLISH -> "🏢 [PART 1: COMPANY DOCUMENTS]"
        ChecklistLanguage.BANGLA -> "🏢 [অংশ ১: কোম্পানি ডকুমেন্টস]"
        ChecklistLanguage.BILINGUAL -> "🏢 [অংশ ১: কোম্পানি ডকুমেন্টস / Company Documents]"
      }
      "EMPLOYEE" -> when (language) {
        ChecklistLanguage.ENGLISH -> "👤 [PART 2: EMPLOYEE DOCUMENTS]"
        ChecklistLanguage.BANGLA -> "👤 [অংশ ২: কর্মকর্তা/কর্মচারী ডকুমেন্টস]"
        ChecklistLanguage.BILINGUAL -> "👤 [অংশ ২: কর্মকর্তা/কর্মচারী ডকুমেন্টস / Employee Documents]"
      }
      else -> category
    }
  }

  private fun autoTranslatePhrase(phrase: String): String {
    var result = phrase
    val replacements = listOf(
      "Certificate" to "সনদপত্র",
      "Statement" to "স্টেটমেন্ট",
      "License" to "লাইসেন্স",
      "Copy" to "কপি",
      "Bill" to "বিল",
      "Letter" to "চিঠি/সনদ",
      "Form" to "ফরম",
      "Card" to "কার্ড",
      "Document" to "ডকুমেন্ট",
      "Documents" to "ডকুমেন্টস",
      "Account" to "হিসাব/একাউন্ট",
      "Office" to "অফিস",
      "Salary" to "বেতন",
      "Income" to "আয়",
      "Tax" to "ট্যাক্স",
      "Return" to "রিটার্ন",
      "Photo" to "ছবি",
      "Photos" to "ছবি",
      "Passport" to "পাসপোর্ট",
      "Bank" to "ব্যাংক",
      "Cheque" to "চেক",
      "Report" to "রিপোর্ট",
      "Resolution" to "রেজোলিউশন",
      "Deed" to "দলিল/চুক্তিপত্র"
    )
    replacements.forEach { (eng, bng) ->
      result = result.replace(Regex("(?i)\\b$eng\\b"), bng)
    }
    return result
  }
}
