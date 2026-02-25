package com.jewish.calendar.model

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import java.util.Date

data class HebrewDateModel(
    val gregorianDate: Date,
    val hebrewDateString: String,
    val hebrewMonthName: String,
    val hebrewDay: Int,
    val hebrewYear: Int,
    val isShabbat: Boolean,
    val isYomTov: Boolean,
    val isRoshChodesh: Boolean,
    val isFastDay: Boolean,
    val isHoliday: Boolean,
    val holidayName: String?,
    val parshaName: String?,
    val omerCount: Int?,          // null if not in omer period
    val additionalEvents: List<String> = emptyList()  // yahrzeits & special days
)

enum class HebrewMonth(val hebrewName: String, val value: Int) {
    NISSAN("ניסן", 1),
    IYAR("אייר", 2),
    SIVAN("סיוון", 3),
    TAMMUZ("תמוז", 4),
    AV("אב", 5),
    ELUL("אלול", 6),
    TISHREI("תשרי", 7),
    CHESHVAN("חשוון", 8),
    KISLEV("כסלו", 9),
    TEVET("טבת", 10),
    SHVAT("שבט", 11),
    ADAR("אדר", 12),
    ADAR_II("אדר ב'", 13)
}

data class JewishHoliday(
    val name: String,
    val hebrewName: String,
    val month: Int,
    val day: Int,
    val isYomTov: Boolean,
    val description: String
)

// Hebrew number letters
object HebrewNumbers {
    private val ones = listOf("", "א", "ב", "ג", "ד", "ה", "ו", "ז", "ח", "ט")
    private val tens = listOf("", "י", "כ", "ל", "מ", "נ", "ס", "ע", "פ", "צ")
    private val hundreds = listOf("", "ק", "ר", "ש", "ת")

    fun toGematria(number: Int): String {
        if (number <= 0 || number > 9999) return number.toString()

        // Handle special cases: 15 = ט"ו, 16 = ט"ז (avoid יה / יו)
        if (number % 100 == 15) return "ט\"ו"
        if (number % 100 == 16) return "ט\"ז"

        var n = number
        val sb = StringBuilder()

        val hundredsDigit = n / 100
        n %= 100
        val tensDigit = n / 10
        val onesDigit = n % 10

        if (hundredsDigit > 0) sb.append(hundreds[hundredsDigit])
        if (tensDigit > 0) sb.append(tens[tensDigit])
        if (onesDigit > 0) sb.append(ones[onesDigit])

        if (sb.isEmpty()) return number.toString()

        // Add gershayim before last letter if more than one letter, else geresh
        return if (sb.length > 1) {
            sb.insert(sb.length - 1, "\"").toString()
        } else {
            sb.append("'").toString()
        }
    }
}
