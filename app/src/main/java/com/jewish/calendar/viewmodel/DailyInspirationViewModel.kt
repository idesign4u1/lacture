package com.jewish.calendar.viewmodel

import androidx.lifecycle.ViewModel
import com.jewish.calendar.data.HebrewCalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Calendar
import javax.inject.Inject

data class InspirationItem(
    val verse: String,
    val source: String,
    val teaching: String,
    val teacher: String
)

data class DailyInspirationUiState(
    val inspiration: InspirationItem? = null,
    val hebrewDate: String = "",
    val parasha: String = ""
)

@HiltViewModel
class DailyInspirationViewModel @Inject constructor(
    private val calendarRepository: HebrewCalendarRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyInspirationUiState())
    val uiState: StateFlow<DailyInspirationUiState> = _uiState.asStateFlow()

    private val inspirations = listOf(
        InspirationItem(
            "וְאָהַבְתָּ לְרֵעֲךָ כָּמוֹךָ",
            "ויקרא יט, יח",
            "זו כלל גדול בתורה. כשאתה אוהב אחרים כמו עצמך, אתה מגלה את צלם האלוקים בכל אדם.",
            "רבי עקיבא"
        ),
        InspirationItem(
            "בְּכָל דְּרָכֶיךָ דָעֵהוּ וְהוּא יְיַשֵּׁר אֹרְחֹתֶיךָ",
            "משלי ג, ו",
            "אל תפריד בין החיים היום-יומיים לבין עבודת ה׳. כל מעשה, אפילו הקטן ביותר, יכול להיות קודש.",
            "רבי יהודה הנשיא"
        ),
        InspirationItem(
            "אֵיזֶהוּ עָשִׁיר? הַשָּׂמֵחַ בְּחֶלְקוֹ",
            "פרקי אבות ד, א",
            "עושר אמיתי אינו בממון, אלא בשמחה עם מה שיש לך. תרגל הכרת טובה כל יום.",
            "בן זומא"
        ),
        InspirationItem(
            "כָּל יִשְׂרָאֵל עֲרֵבִים זֶה לָזֶה",
            "שבועות לט, א",
            "אנחנו אחד. כשאדם מישראל כואב, כולנו כואבים. כשאחד שמח, שמחתנו מתחברת.",
            "תלמוד בבלי"
        ),
        InspirationItem(
            "חֲזַק וֶאֱמָץ אַל תִּירָא וְאַל תֵּחָת",
            "יהושע א, ט",
            "ה׳ מצווה אותנו להיות חזקים. הפחד הוא טבעי, אבל הגבורה היא לפעול למרות הפחד.",
            "יהושע בן נון"
        ),
        InspirationItem(
            "לֵב שָׂמֵחַ יֵיטִב גֵּהָה",
            "משלי יז, כב",
            "השמחה היא רפואה לנפש ולגוף. חפש בכל יום סיבה אחת לשמוח, ותמצא.",
            "שלמה המלך"
        ),
        InspirationItem(
            "שְׁמַע יִשְׂרָאֵל ה׳ אֱלֹהֵינוּ ה׳ אֶחָד",
            "דברים ו, ד",
            "האחדות האלוקית היא הבסיס לכל. כשאנו מכירים שה׳ אחד, מבינים שכל הנשמות מחוברות.",
            "משה רבינו"
        ),
        InspirationItem(
            "וּבְטַחְתָּ בַּה׳ בְּכָל לִבֶּךָ",
            "משלי ג, ה",
            "הביטחון בה׳ הוא מנוחה לנפש. כשאתה מאמין שה׳ מנהיג, תוכל לשחרר את הדאגות.",
            "חכמי הקבלה"
        ),
        InspirationItem(
            "מַה טֹּבוּ אֹהָלֶיךָ יַעֲקֹב מִשְׁכְּנֹתֶיךָ יִשְׂרָאֵל",
            "במדבר כד, ה",
            "כל בית יהודי הוא מקדש מעט. ברוח הבית שלך, ריח השבת, נרות החנוכה — שם שורה השכינה.",
            "בלעם הנביא"
        ),
        InspirationItem(
            "תּוֹרָה צִוָּה לָנוּ מֹשֶׁה מוֹרָשָׁה קְהִלַּת יַעֲקֹב",
            "דברים לג, ד",
            "התורה היא ירושתנו. כל דף גמרא שלמדת, כל מצווה שקיימת — זה חלקך בנצח.",
            "משה רבינו"
        ),
        InspirationItem(
            "אֲנִי לְדוֹדִי וְדוֹדִי לִי",
            "שיר השירים ו, ג",
            "הקשר בין כנסת ישראל לקב״ה הוא כקשר אוהבים. ה׳ מחכה לך שתפנה אליו.",
            "שלמה המלך"
        ),
        InspirationItem(
            "רַבִּי אוֹמֵר: אֵיזוֹ הִיא דֶרֶךְ יְשָׁרָה שֶׁיָּבֹר לוֹ הָאָדָם",
            "פרקי אבות ב, א",
            "שאל תמיד: האם הדרך שבחרתי מכבדת אותי ומכבדת אחרים? זו הדרך הישרה.",
            "רבי יהודה הנשיא"
        ),
        InspirationItem(
            "כָּל הַמְקַיֵּם נֶפֶשׁ אַחַת מַעֲלֶה עָלָיו כְּאִילּוּ קִיֵּם עוֹלָם מָלֵא",
            "סנהדרין לז, א",
            "כל אדם הוא עולם שלם. כשאתה עוזר לאחד, אתה מציל עולם שלם.",
            "תלמוד בבלי"
        ),
        InspirationItem(
            "וְהָלַכְתָּ בִּדְרָכָיו",
            "דברים כח, ט",
            "הידמות לקב״ה: מה הוא חנון — אף אתה חנון. מה הוא רחום — אף אתה רחום.",
            "ספרי דברים"
        )
    )

    init { loadInspiration() }

    private fun loadInspiration() {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val item = inspirations[dayOfYear % inspirations.size]
        val today = calendarRepository.getTodayHebrewDate()
        val hebrewDate = today.hebrewDateString
        val parasha = today.parshaName ?: ""

        _uiState.update {
            it.copy(
                inspiration = item,
                hebrewDate  = hebrewDate,
                parasha     = parasha
            )
        }
    }

    fun refresh() { loadInspiration() }
}
