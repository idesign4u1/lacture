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

        val month = jewishCalendar.jewishMonth
        val day   = jewishCalendar.jewishDayOfMonth
        val additionalEvents = ADDITIONAL_EVENTS[Pair(month, day)] ?: emptyList()

        return HebrewDateModel(
            gregorianDate = date,
            hebrewDateString = formatter.format(jewishCalendar),
            hebrewMonthName = formatter.formatMonth(jewishCalendar),
            hebrewDay = day,
            hebrewYear = jewishCalendar.jewishYear,
            isShabbat = isShabbat,
            isYomTov = isYomTov,
            isRoshChodesh = isRoshChodesh,
            isFastDay = isFastDay,
            isHoliday = holidayName != null,
            holidayName = holidayName,
            parshaName = parshaName,
            omerCount = omerCount,
            additionalEvents = additionalEvents
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

    companion object {
        // Key: Pair(hebrewMonth, hebrewDay)  — month 1=Nissan … 7=Tishrei … 12=Adar / 13=Adar II
        val ADDITIONAL_EVENTS: Map<Pair<Int,Int>, List<String>> = mapOf(
            // ── תשרי (7) ──
            Pair(7, 9)  to listOf("יארצייט: הגרי\"ז סולובייצ'יק"),
            Pair(7, 10) to listOf("יארצייט: רבי עקיבא"),
            Pair(7, 13) to listOf("יארצייט: רבי עקיבא איגר"),
            Pair(7, 14) to listOf("יארצייט: המגיד מקוז'ניץ"),
            Pair(7, 18) to listOf("יארצייט: רבי נחמן מברסלב"),
            Pair(7, 19) to listOf("יארצייט: הגאון מוילנא (הגר\"א)"),
            Pair(7, 24) to listOf("יארצייט: רבי יעקב יוסף מפולנאה"),
            Pair(7, 25) to listOf("יארצייט: החתם סופר", "יארצייט: רבי לוי יצחק מברדיטשוב"),
            Pair(7, 29) to listOf("יארצייט: שמעון הצדיק"),
            // ── חשון (8) ──
            Pair(8, 3)  to listOf("יארצייט: הרב עובדיה יוסף"),
            Pair(8, 5)  to listOf("יארצייט: רבי צבי אלימלך מדינוב (בני יששכר)"),
            Pair(8, 7)  to listOf("התחלת שאילת גשמים (ברך עלינו)"),
            Pair(8, 11) to listOf("יארצייט: רחל אמנו", "יארצייט: רבי מנחם נחום מטשרנוביל"),
            Pair(8, 15) to listOf("יארצייט: החזון איש"),
            Pair(8, 16) to listOf("יארצייט: הרב שך"),
            // ── כסלו (9) ──
            Pair(9, 9)  to listOf("יארצייט: האדמו\"ר האמצעי מחב\"ד (רבי דוב בער שניאורי)"),
            Pair(9, 18) to listOf("יארצייט: רבי ברוך ממז'יבוז' (נכד הבעש\"ט)"),
            Pair(9, 19) to listOf("חג הגאולה (י\"ט כסלו) - שחרור אדמו\"ר הזקן", "יארצייט: המגיד ממעזריטש"),
            Pair(9, 20) to listOf("יארצייט: רבי שלום רוקח מבעלזא"),
            Pair(9, 21) to listOf("יארצייט: רבי אברהם יהושע השל מאפטא (האוהב ישראל)"),
            Pair(9, 24) to listOf("יארצייט: הרב שטיינמן"),
            // ── טבת (10) ──
            Pair(10, 3)  to listOf("יארצייט: ר' חיים שמואלביץ"),
            Pair(10, 5)  to listOf("יארצייט: הרש\"ב מלובאוויטש"),
            Pair(10, 10) to listOf("יארצייט: ר' נתן מברסלב"),
            Pair(10, 16) to listOf("יארצייט: רבי ישראל פרידמן מסדיגורא"),
            Pair(10, 20) to listOf("יארצייט: הרמב\"ם"),
            Pair(10, 24) to listOf("יארצייט: בעל התניא (אדמו\"ר הזקן)"),
            Pair(10, 29) to listOf("יארצייט: הרב יצחק כדורי"),
            // ── שבט (11) ──
            Pair(11, 2)  to listOf("יארצייט: רבי זושא מאניפולי"),
            Pair(11, 4)  to listOf("יארצייט: הבבא סאלי (רבי ישראל אבוחצירא)"),
            Pair(11, 5)  to listOf("יארצייט: השפת אמת מגור"),
            Pair(11, 10) to listOf("יארצייט: הריי\"צ מליובאוויטש"),
            Pair(11, 22) to listOf("יארצייט: הרבי מקוצק"),
            Pair(11, 25) to listOf("יארצייט: ר' ישראל סלנטר"),
            // ── אדר (12) — גם בשנים רגילות ──
            Pair(12, 1)  to listOf("יארצייט: הש\"ך", "משנכנס אדר מרבין בשמחה"),
            Pair(12, 2)  to listOf("יארצייט: רבי מאיר מפרמישלאן"),
            Pair(12, 7)  to listOf("יארצייט: משה רבינו (יום פטירתו ולידתו)", "יארצייט: אדמו\"ר הזקן (לפי חלק מהדעות)"),
            Pair(12, 9)  to listOf("יארצייט: רבי משה ליב מסאסוב"),
            Pair(12, 11) to listOf("יארצייט: החיד\"א"),
            Pair(12, 13) to listOf("יארצייט: ר' משה פיינשטיין"),
            Pair(12, 14) to listOf("יארצייט: הרב צבי יהודה קוק", "יארצייט: רבי זאב וולף מז'יטומיר"),
            Pair(12, 15) to listOf("יארצייט: הרב קנייבסקי"),
            Pair(12, 17) to listOf("יארצייט: רבי שמעלקא מניקלשבורג", "יארצייט: רבי אברהם מקאליסק"),
            Pair(12, 18) to listOf("יארצייט: רבי אלכסנדר זושא מקומרנא"),
            Pair(12, 20) to listOf("יארצייט: הרב שלמה זלמן אוירבך"),
            Pair(12, 21) to listOf("יארצייט: ר' אלימלך מליז'ענסק", "יארצייט: רבי קלונימוס קלמן מקראקא"),
            Pair(12, 23) to listOf("יארצייט: רבי זדוק הכהן מלובלין"),
            Pair(12, 25) to listOf("יארצייט: רבי יצחק מוורקא"),
            // ── אדר ב (13) — שנים מעוברות ──
            Pair(13, 1)  to listOf("יארצייט: הש\"ך", "משנכנס אדר מרבין בשמחה"),
            Pair(13, 2)  to listOf("יארצייט: רבי מאיר מפרמישלאן"),
            Pair(13, 7)  to listOf("יארצייט: משה רבינו (יום פטירתו ולידתו)"),
            Pair(13, 9)  to listOf("יארצייט: רבי משה ליב מסאסוב"),
            Pair(13, 11) to listOf("יארצייט: החיד\"א"),
            Pair(13, 13) to listOf("יארצייט: ר' משה פיינשטיין"),
            Pair(13, 14) to listOf("יארצייט: הרב צבי יהודה קוק"),
            Pair(13, 15) to listOf("יארצייט: הרב קנייבסקי"),
            Pair(13, 18) to listOf("יארצייט: רבי אלכסנדר זושא מקומרנא"),
            Pair(13, 20) to listOf("יארצייט: הרב שלמה זלמן אוירבך"),
            Pair(13, 21) to listOf("יארצייט: ר' אלימלך מליז'ענסק", "יארצייט: רבי קלונימוס קלמן מקראקא"),
            Pair(13, 23) to listOf("יארצייט: רבי זדוק הכהן מלובלין"),
            Pair(13, 25) to listOf("יארצייט: רבי יצחק מוורקא"),
            // ── ניסן (1) ──
            Pair(1, 4)  to listOf("יארצייט: רבי אהרן מקרלין"),
            Pair(1, 7)  to listOf("יארצייט: האר\"י הקדוש (יום הולדת)"),
            Pair(1, 11) to listOf("יארצייט: הרמב\"ן / השל\"ה הקדוש"),
            Pair(1, 13) to listOf("יארצייט: הצמח צדק מחב\"ד"),
            Pair(1, 15) to listOf("יארצייט: יצחק אבינו"),
            Pair(1, 25) to listOf("יארצייט: הרב חיים מצאנז (בעל דברי חיים)"),
            Pair(1, 26) to listOf("יארצייט: יהושע בן נון"),
            Pair(1, 27) to listOf("יארצייט: רבי חיים מאיר יחיאל שפירא מדרוהוביטש"),
            // ── אייר (2) ──
            Pair(2, 1)  to listOf("יארצייט: רבי מנחם מנדל מויטבסק (פרי הארץ)"),
            Pair(2, 3)  to listOf("יארצייט: רבי ישעיה'לה מקרסטיר"),
            Pair(2, 11) to listOf("יארצייט: רבי נפתלי צבי מרופשיץ", "יארצייט: רבי מרדכי מנסכיז"),
            Pair(2, 20) to listOf("יארצייט: רבי מרדכי מטשרנוביל"),
            Pair(2, 23) to listOf("יארצייט: רבי גרשון מקיטוב (גיסו של הבעש\"ט)"),
            // ── סיון (3) ──
            Pair(3, 3)  to listOf("יארצייט: רבי ישראל מוויזניץ"),
            Pair(3, 6)  to listOf("יארצייט: דוד המלך", "יארצייט: הבעש\"ט"),
            Pair(3, 14) to listOf("יארצייט: ר' חיים מוולוז'ין (מייסד ישיבת וולוז'ין)"),
            Pair(3, 25) to listOf("יארצייט: רבן שמעון בן גמליאל"),
            // ── תמוז (4) ──
            Pair(4, 1)  to listOf("יארצייט: יוסף הצדיק"),
            Pair(4, 3)  to listOf("יארצייט: הרבי מליובאוויטש (האדמו\"ר השביעי)"),
            Pair(4, 4)  to listOf("יארצייט: רבי פנחס הלוי הורוביץ (בעל ההפלאה)"),
            Pair(4, 12) to listOf("יארצייט: רבי אברהם דב מאבריטש (בעל בת עין)"),
            Pair(4, 27) to listOf("יארצייט: רבי שלמה מקרלין"),
            Pair(4, 29) to listOf("יארצייט: רש\"י (פרשן התורה והתלמוד)"),
            // ── אב (5) ──
            Pair(5, 1)  to listOf("יארצייט: אהרן הכהן"),
            Pair(5, 5)  to listOf("יארצייט: האר\"י הקדוש"),
            Pair(5, 9)  to listOf("יארצייט: רבי ישראל מרוז'ין"),
            Pair(5, 15) to listOf("יארצייט: רבי שמחה בונים מפשיסחא", "יארצייט: רבי צבי הירש מזידיטשוב"),
            Pair(5, 21) to listOf("יארצייט: ר' חיים מבריסק"),
            Pair(5, 28) to listOf("יארצייט: הנצי\"ב מוולוז'ין"),
            // ── אלול (6) ──
            Pair(6, 3)  to listOf("יארצייט: הרב קוק (הרב הראשי הראשון לא\"י)"),
            Pair(6, 5)  to listOf("יארצייט: רבי ישראל מסטולין (הינוקא)"),
            Pair(6, 10) to listOf("יארצייט: רבי פנחס מקוריץ"),
            Pair(6, 18) to listOf("יארצייט: המהר\"ל מפראג", "יום הולדת: הבעש\"ט"),
            Pair(6, 24) to listOf("יארצייט: החפץ חיים"),
            Pair(6, 25) to listOf("יום בריאת העולם (כ\"ה אלול)", "יארצייט: רבי יחיאל מיכל מזלוטשוב"),
        )
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
