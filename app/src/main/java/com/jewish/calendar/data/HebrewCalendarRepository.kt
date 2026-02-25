package com.jewish.calendar.data

import com.jewish.calendar.model.*
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HebrewCalendarRepository @Inject constructor() {

    private val formatter = HebrewDateFormatter().apply {
        isHebrewFormat = true
        isUseFinalFormLetters = true
    }

    fun getHebrewDateForDay(date: Date): HebrewDateModel {
        val cal = Calendar.getInstance().apply { time = date }
        val jewishCalendar = JewishCalendar(cal).apply { inIsrael = true }

        val isShabbat = jewishCalendar.dayOfWeek == Calendar.SATURDAY
        val yomTovIndex = jewishCalendar.yomTovIndex
        val isYomTov = jewishCalendar.isYomTovAssurBemelacha
        val isRoshChodesh = jewishCalendar.isRoshChodesh
        val isFastDay = yomTovIndex == JewishCalendar.SEVENTEEN_OF_TAMMUZ ||
                yomTovIndex == JewishCalendar.TISHA_BEAV ||
                yomTovIndex == JewishCalendar.FAST_OF_GEDALYAH ||
                yomTovIndex == JewishCalendar.TENTH_OF_TEVES ||
                yomTovIndex == JewishCalendar.FAST_OF_ESTHER ||
                yomTovIndex == JewishCalendar.YOM_KIPPUR

        val holidayName = when {
            yomTovIndex >= 0 && yomTovIndex != JewishCalendar.ROSH_CHODESH -> getHolidayName(yomTovIndex)
            isRoshChodesh -> {
                // Day 30 of the current month = Rosh Chodesh of the *next* month
                val rcCal = if (jewishCalendar.jewishDayOfMonth == 30) {
                    JewishCalendar(Calendar.getInstance().apply { time = date; add(Calendar.DAY_OF_MONTH, 1) })
                } else {
                    jewishCalendar
                }
                "ראש חודש ${formatter.formatMonth(rcCal)}"
            }
            else -> null
        }

        // Get the parsha for the current week's Shabbat (works for every day of the week)
        val parshaName = try {
            val daysToShabbat = (Calendar.SATURDAY - cal.get(Calendar.DAY_OF_WEEK) + 7) % 7
            val shabbatCal = JewishCalendar(Calendar.getInstance().apply {
                time = date
                add(Calendar.DAY_OF_MONTH, daysToShabbat)
            }).apply { inIsrael = true }
            formatter.formatParsha(shabbatCal).takeIf { it.isNotBlank() }
        } catch (e: Exception) { null }

        val omerCount = try {
            val count = jewishCalendar.dayOfOmer
            if (count in 1..49) count else null
        } catch (e: Exception) { null }

        return HebrewDateModel(
            gregorianDate = date,
            hebrewDateString = formatter.format(jewishCalendar),
            hebrewMonthName = formatter.formatMonth(jewishCalendar),
            hebrewDay = jewishCalendar.jewishDayOfMonth,
            hebrewYear = jewishCalendar.jewishYear,
            isShabbat = isShabbat,
            isYomTov = isYomTov,
            isRoshChodesh = isRoshChodesh,
            isFastDay = isFastDay,
            isHoliday = holidayName != null,
            holidayName = holidayName,
            parshaName = parshaName,
            omerCount = omerCount
        )
    }

    fun getMonthDays(year: Int, month: Int): List<HebrewDateModel> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return (1..daysInMonth).map { day ->
            val dayCal = Calendar.getInstance().apply {
                set(year, month - 1, day)
            }
            getHebrewDateForDay(dayCal.time)
        }
    }

    fun getTodayHebrewDate(): HebrewDateModel = getHebrewDateForDay(Date())

    fun getOmerText(count: Int): String {
        val weeks = count / 7
        val days = count % 7
        val sb = StringBuilder("היום ")
        sb.append(HebrewNumbers.toGematria(count))
        sb.append(" יום")
        if (count >= 7) {
            sb.append(", שהם ")
            sb.append(HebrewNumbers.toGematria(weeks))
            sb.append(" שבועות")
            if (days > 0) {
                sb.append(" ו-")
                sb.append(HebrewNumbers.toGematria(days))
                sb.append(if (days == 1) " יום" else " ימים")
            }
        }
        sb.append(" לעומר")
        return sb.toString()
    }

    private fun getHolidayName(index: Int): String? = when (index) {
        // ימים נוראים
        JewishCalendar.EREV_ROSH_HASHANA -> "ערב ראש השנה"
        JewishCalendar.ROSH_HASHANA -> "ראש השנה"
        JewishCalendar.FAST_OF_GEDALYAH -> "צום גדליה"
        JewishCalendar.YOM_KIPPUR -> "יום כיפור"
        // סוכות
        JewishCalendar.EREV_SUCCOS -> "ערב סוכות"
        JewishCalendar.SUCCOS -> "סוכות"
        JewishCalendar.CHOL_HAMOED_SUCCOS -> "חול המועד סוכות"
        JewishCalendar.HOSHANA_RABBA -> "הושענא רבה"
        JewishCalendar.SHEMINI_ATZERES -> "שמיני עצרת"
        JewishCalendar.SIMCHAS_TORAH -> "שמחת תורה"
        // חנוכה ועוד
        JewishCalendar.CHANUKAH -> "חנוכה"
        JewishCalendar.TENTH_OF_TEVES -> "עשרה בטבת"
        JewishCalendar.TU_BESHVAT -> "ט\"ו בשבט"
        // פורים
        JewishCalendar.PURIM_KATAN -> "פורים קטן"
        JewishCalendar.FAST_OF_ESTHER -> "תענית אסתר"
        JewishCalendar.PURIM -> "פורים"
        JewishCalendar.SHUSHAN_PURIM -> "שושן פורים"
        // פסח
        JewishCalendar.EREV_PESACH -> "ערב פסח"
        JewishCalendar.PESACH -> "פסח"
        JewishCalendar.CHOL_HAMOED_PESACH -> "חול המועד פסח"
        JewishCalendar.PESACH_SHENI -> "פסח שני"
        // עומר וספירה
        JewishCalendar.LAG_BAOMER -> "ל\"ג בעומר"
        // שבועות
        JewishCalendar.EREV_SHAVUOS -> "ערב שבועות"
        JewishCalendar.SHAVUOS -> "שבועות"
        // בין המצרים
        JewishCalendar.SEVENTEEN_OF_TAMMUZ -> "י\"ז בתמוז"
        JewishCalendar.TISHA_BEAV -> "תשעה באב"
        JewishCalendar.TU_BEAV -> "ט\"ו באב"
        // ישראל מודרני
        JewishCalendar.YOM_HASHOAH -> "יום הזיכרון לשואה"
        JewishCalendar.YOM_HAZIKARON -> "יום הזיכרון"
        JewishCalendar.YOM_HAATZMAUT -> "יום העצמאות"
        JewishCalendar.YOM_YERUSHALAYIM -> "יום ירושלים"
        else -> null
    }
}
