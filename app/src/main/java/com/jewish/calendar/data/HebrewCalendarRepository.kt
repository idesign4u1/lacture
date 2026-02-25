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
        val jewishCalendar = JewishCalendar(cal)

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

        val holidayName = if (yomTovIndex >= 0) {
            getHolidayName(yomTovIndex)
        } else if (isRoshChodesh) {
            "ראש חודש ${formatter.formatMonth(jewishCalendar)}"
        } else null

        val parshaName = try {
            if (isShabbat) formatter.formatParsha(jewishCalendar) else null
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
                sb.append(" ימים")
            }
        }
        sb.append(" לעומר")
        return sb.toString()
    }

    private fun getHolidayName(index: Int): String? = when (index) {
        JewishCalendar.ROSH_HASHANA -> "ראש השנה"
        JewishCalendar.YOM_KIPPUR -> "יום כיפור"
        JewishCalendar.SUCCOS -> "סוכות"
        JewishCalendar.CHOL_HAMOED_SUCCOS -> "חול המועד סוכות"
        JewishCalendar.HOSHANA_RABBA -> "הושענא רבה"
        JewishCalendar.SHEMINI_ATZERES -> "שמיני עצרת"
        JewishCalendar.SIMCHAS_TORAH -> "שמחת תורה"
        JewishCalendar.CHANUKAH -> "חנוכה"
        JewishCalendar.TU_BESHVAT -> "ט\"ו בשבט"
        JewishCalendar.PURIM_KATAN -> "פורים קטן"
        JewishCalendar.PURIM -> "פורים"
        JewishCalendar.SHUSHAN_PURIM -> "שושן פורים"
        JewishCalendar.PESACH -> "פסח"
        JewishCalendar.CHOL_HAMOED_PESACH -> "חול המועד פסח"
        JewishCalendar.PESACH_SHENI -> "פסח שני"
        JewishCalendar.LAG_BAOMER -> "ל\"ג בעומר"
        JewishCalendar.SHAVUOS -> "שבועות"
        JewishCalendar.SEVENTEEN_OF_TAMMUZ -> "י\"ז בתמוז"
        JewishCalendar.TISHA_BEAV -> "תשעה באב"
        JewishCalendar.TU_BEAV -> "ט\"ו באב"
        JewishCalendar.YOM_HASHOAH -> "יום הזיכרון לשואה"
        JewishCalendar.YOM_HAZIKARON -> "יום הזיכרון"
        JewishCalendar.YOM_HAATZMAUT -> "יום העצמאות"
        JewishCalendar.YOM_YERUSHALAYIM -> "יום ירושלים"
        else -> null
    }
}
