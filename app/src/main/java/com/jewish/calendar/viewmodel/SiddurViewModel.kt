package com.jewish.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.data.SefariaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

// ── Prayer category ────────────────────────────────────────────────────

enum class SiddurPrayerTime(val hebrewName: String, val icon: String, val hourStart: Int, val hourEnd: Int) {
    SHACHARIT("שחרית", "🌅", 5, 10),
    MINCHA("מנחה", "☀️", 11, 17),
    MAARIV("ערבית", "🌙", 18, 22),
    BEDTIME("ק\"ש המיטה", "⭐", 23, 4)
}

// ── Prayer section ─────────────────────────────────────────────────────

data class PrayerSectionData(
    val id: String,
    val title: String,
    val openingLine: String = "",       // Short representative line shown in list
    val sefariaRef: String? = null,     // Sefaria API ref for full biblical text
    val staticText: String = "",        // Full text for short rabbinical prayers
    val halachicNote: String = ""       // Instruction or note
)

// ── UI State ───────────────────────────────────────────────────────────

data class SiddurUiState(
    val currentTime: SiddurPrayerTime = SiddurPrayerTime.SHACHARIT,
    val selectedCategory: SiddurPrayerTime = SiddurPrayerTime.SHACHARIT,
    val selectedSection: PrayerSectionData? = null,
    val displayedText: String = "",
    val isLoadingText: Boolean = false,
    val fontSize: Int = 22,
    val error: String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────

@HiltViewModel
class SiddurViewModel @Inject constructor(
    private val sefariaRepository: SefariaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SiddurUiState())
    val uiState: StateFlow<SiddurUiState> = _uiState.asStateFlow()

    init {
        val now = detectCurrentPrayerTime()
        _uiState.update { it.copy(currentTime = now, selectedCategory = now) }
    }

    // ── Public actions ─────────────────────────────────────────────────

    fun selectCategory(category: SiddurPrayerTime) {
        _uiState.update {
            it.copy(
                selectedCategory = category,
                selectedSection = null,
                displayedText = "",
                error = null
            )
        }
    }

    fun selectSection(section: PrayerSectionData) {
        _uiState.update {
            it.copy(
                selectedSection = section,
                displayedText = "",
                error = null,
                isLoadingText = section.sefariaRef != null
            )
        }
        if (section.sefariaRef != null) {
            fetchFromSefaria(section)
        } else {
            _uiState.update { it.copy(displayedText = section.staticText, isLoadingText = false) }
        }
    }

    fun clearSection() = _uiState.update { it.copy(selectedSection = null, displayedText = "", error = null) }
    fun increaseFontSize() = _uiState.update { it.copy(fontSize = (it.fontSize + 2).coerceAtMost(40)) }
    fun decreaseFontSize() = _uiState.update { it.copy(fontSize = (it.fontSize - 2).coerceAtLeast(14)) }

    // ── Sefaria fetch ──────────────────────────────────────────────────

    private fun fetchFromSefaria(section: PrayerSectionData) {
        viewModelScope.launch {
            sefariaRepository.getTextForRef(section.sefariaRef!!)
                .onSuccess { text ->
                    _uiState.update { it.copy(displayedText = text, isLoadingText = false, error = null) }
                }
                .onFailure {
                    // Fallback to static text if network fails
                    _uiState.update {
                        it.copy(
                            displayedText = section.staticText.ifBlank { "לא ניתן לטעון את הטקסט" },
                            isLoadingText = false,
                            error = null
                        )
                    }
                }
        }
    }

    // ── Smart prayer time detection ────────────────────────────────────

    private fun detectCurrentPrayerTime(): SiddurPrayerTime {
        val hour = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jerusalem"))
            .get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10  -> SiddurPrayerTime.SHACHARIT
            in 11..17 -> SiddurPrayerTime.MINCHA
            in 18..22 -> SiddurPrayerTime.MAARIV
            else      -> SiddurPrayerTime.BEDTIME
        }
    }

    // ── Prayer data ────────────────────────────────────────────────────

    fun getSectionsFor(time: SiddurPrayerTime): List<PrayerSectionData> = when (time) {
        SiddurPrayerTime.SHACHARIT -> shacharitSections
        SiddurPrayerTime.MINCHA   -> minchaSections
        SiddurPrayerTime.MAARIV   -> maarivSections
        SiddurPrayerTime.BEDTIME  -> bedtimeSections
    }

    // ── Shacharit ──────────────────────────────────────────────────────

    private val shacharitSections = listOf(
        PrayerSectionData(
            id = "modeh_ani",
            title = "מודה אני",
            openingLine = "מוֹדֶה אֲנִי לְפָנֶיךָ",
            staticText = "מוֹדֶה אֲנִי לְפָנֶיךָ מֶלֶךְ חַי וְקַיָּם,\nשֶׁהֶחֱזַרְתָּ בִּי נִשְׁמָתִי בְּחֶמְלָה.\nרַבָּה אֱמוּנָתֶךָ.",
            halachicNote = "נאמר מיד עם הקיצה, לפני נטילת ידיים"
        ),
        PrayerSectionData(
            id = "brachos_shachar",
            title = "ברכות השחר",
            openingLine = "אֲשֶׁר נָתַן לַשֶּׂכְוִי בִינָה",
            staticText = buildMorningBlessings(),
            halachicNote = "ט\"ו ברכות שחר לפני התפילה"
        ),
        PrayerSectionData(
            id = "ashrei_shachar",
            title = "אשרי (תהילים קמ\"ה)",
            openingLine = "אַשְׁרֵי יוֹשְׁבֵי בֵיתֶךָ",
            sefariaRef = "Psalms 145",
            staticText = "אַשְׁרֵי יוֹשְׁבֵי בֵיתֶךָ עוֹד יְהַלְלוּךָ סֶּלָה"
        ),
        PrayerSectionData(
            id = "psalm_150",
            title = "הַלְלוּיָהּ (תהילים קנ)",
            openingLine = "הַלְלוּיָהּ הַלְלוּ אֵל בְּקָדְשׁוֹ",
            sefariaRef = "Psalms 150",
            staticText = "הַלְלוּיָהּ הַלְלוּ אֵל בְּקָדְשׁוֹ"
        ),
        PrayerSectionData(
            id = "yotzer_or",
            title = "יוצר אור",
            openingLine = "בָּרוּךְ אַתָּה ה׳ אֱלֹהֵינוּ מֶלֶךְ הָעוֹלָם",
            staticText = buildYotzerOr(),
            halachicNote = "ברכה ראשונה לפני קריאת שמע"
        ),
        PrayerSectionData(
            id = "shema",
            title = "שמע ישראל",
            openingLine = "שְׁמַע יִשְׂרָאֵל ה׳ אֱלֹהֵינוּ ה׳ אֶחָד",
            sefariaRef = "Deuteronomy 6:4-9",
            staticText = "שְׁמַע יִשְׂרָאֵל ה׳ אֱלֹהֵינוּ ה׳ אֶחָד",
            halachicNote = "יש לאומרה בכוונה ובהדגשת האחד"
        ),
        PrayerSectionData(
            id = "vahavta",
            title = "וְאָהַבְתָּ",
            openingLine = "וְאָהַבְתָּ אֵת ה׳ אֱלֹהֶיךָ",
            sefariaRef = "Deuteronomy 6:5-9",
            staticText = "וְאָהַבְתָּ אֵת ה׳ אֱלֹהֶיךָ בְּכָל לְבָבְךָ"
        ),
        PrayerSectionData(
            id = "vehaya",
            title = "וְהָיָה אִם שָׁמֹעַ",
            openingLine = "וְהָיָה אִם שָׁמֹעַ תִּשְׁמְעוּ",
            sefariaRef = "Deuteronomy 11:13-21"
        ),
        PrayerSectionData(
            id = "vayomer",
            title = "וַיֹּאמֶר (ציצית)",
            openingLine = "וַיֹּאמֶר ה׳ אֶל מֹשֶׁה",
            sefariaRef = "Numbers 15:37-41"
        ),
        PrayerSectionData(
            id = "amidah_shachar",
            title = "עמידה – שחרית",
            openingLine = "אֲדֹנָי שְׂפָתַי תִּפְתָּח",
            staticText = buildAmidahShacharit(),
            halachicNote = "תפילה בלחש בעמידה, רגליים צמודות, ג' פסיעות לפנים"
        ),
        PrayerSectionData(
            id = "aleinu_shachar",
            title = "עָלֵינוּ לְשַׁבֵּחַ",
            openingLine = "עָלֵינוּ לְשַׁבֵּחַ לַאֲדוֹן הַכֹּל",
            staticText = buildAleinu()
        )
    )

    // ── Mincha ─────────────────────────────────────────────────────────

    private val minchaSections = listOf(
        PrayerSectionData(
            id = "ashrei_mincha",
            title = "אשרי",
            openingLine = "אַשְׁרֵי יוֹשְׁבֵי בֵיתֶךָ",
            sefariaRef = "Psalms 145"
        ),
        PrayerSectionData(
            id = "amidah_mincha",
            title = "עמידה – מנחה",
            openingLine = "אֲדֹנָי שְׂפָתַי תִּפְתָּח",
            staticText = buildAmidahMincha(),
            halachicNote = "זמן מנחה: מנחה גדולה – חצי שעה אחרי חצות"
        ),
        PrayerSectionData(
            id = "aleinu_mincha",
            title = "עלינו",
            openingLine = "עָלֵינוּ לְשַׁבֵּחַ לַאֲדוֹן הַכֹּל",
            staticText = buildAleinu()
        )
    )

    // ── Maariv ─────────────────────────────────────────────────────────

    private val maarivSections = listOf(
        PrayerSectionData(
            id = "barchu",
            title = "בָּרְכוּ",
            openingLine = "בָּרְכוּ אֶת ה׳ הַמְבֹרָךְ",
            staticText = "שַׁ\"ץ: בָּרְכוּ אֶת ה׳ הַמְבֹרָךְ\nקהל: בָּרוּךְ ה׳ הַמְבֹרָךְ לְעוֹלָם וָעֶד\nשַׁ\"ץ: בָּרוּךְ ה׳ הַמְבֹרָךְ לְעוֹלָם וָעֶד",
            halachicNote = "נאמר רק בציבור (מניין)"
        ),
        PrayerSectionData(
            id = "shema_maariv",
            title = "קריאת שמע",
            openingLine = "שְׁמַע יִשְׂרָאֵל ה׳ אֱלֹהֵינוּ ה׳ אֶחָד",
            sefariaRef = "Deuteronomy 6:4-9"
        ),
        PrayerSectionData(
            id = "amidah_maariv",
            title = "עמידה – ערבית",
            openingLine = "אֲדֹנָי שְׂפָתַי תִּפְתָּח",
            staticText = buildAmidahMaariv(),
            halachicNote = "בערבית העמידה היא רשות אך הפכה לחובה במנהג"
        ),
        PrayerSectionData(
            id = "aleinu_maariv",
            title = "עלינו",
            openingLine = "עָלֵינוּ לְשַׁבֵּחַ לַאֲדוֹן הַכֹּל",
            staticText = buildAleinu()
        )
    )

    // ── Bedtime Shema ──────────────────────────────────────────────────

    private val bedtimeSections = listOf(
        PrayerSectionData(
            id = "shema_bed",
            title = "שמע ישראל",
            openingLine = "שְׁמַע יִשְׂרָאֵל ה׳ אֱלֹהֵינוּ ה׳ אֶחָד",
            sefariaRef = "Deuteronomy 6:4-9",
            halachicNote = "נאמר בשכיבה"
        ),
        PrayerSectionData(
            id = "hamapil",
            title = "הַמַּפִּיל",
            openingLine = "בָּרוּךְ אַתָּה ה׳ אֱלֹהֵינוּ מֶלֶךְ הָעוֹלָם",
            staticText = buildHamapil(),
            halachicNote = "נאמר לפני השינה, לאחר הברכה יש ללכת לישון מיד"
        ),
        PrayerSectionData(
            id = "psalm_91",
            title = "יוֹשֵׁב בְּסֵתֶר (תהילים צא)",
            openingLine = "יוֹשֵׁב בְּסֵתֶר עֶלְיוֹן",
            sefariaRef = "Psalms 91"
        )
    )
}

// ── Static prayer text builders ────────────────────────────────────────

private fun buildMorningBlessings(): String = """
ברכות השחר

בָּרוּךְ אַתָּה ה׳ אֱלֹהֵינוּ מֶלֶךְ הָעוֹלָם אֲשֶׁר נָתַן לַשֶּׂכְוִי בִינָה לְהַבְחִין בֵּין יוֹם וּבֵין לָיְלָה.

בָּרוּךְ אַתָּה ה׳ אֱלֹהֵינוּ מֶלֶךְ הָעוֹלָם שֶׁלֹּא עָשַׂנִי גוֹי.

בָּרוּךְ אַתָּה ה׳ אֱלֹהֵינוּ מֶלֶךְ הָעוֹלָם שֶׁלֹּא עָשַׂנִי עָבֶד.

בָּרוּךְ אַתָּה ה׳ אֱלֹהֵינוּ מֶלֶךְ הָעוֹלָם פּוֹקֵחַ עִוְרִים.

בָּרוּךְ אַתָּה ה׳ אֱלֹהֵינוּ מֶלֶךְ הָעוֹלָם מַלְבִּישׁ עֲרֻמִּים.

בָּרוּךְ אַתָּה ה׳ אֱלֹהֵינוּ מֶלֶךְ הָעוֹלָם מַתִּיר אֲסוּרִים.

בָּרוּךְ אַתָּה ה׳ אֱלֹהֵינוּ מֶלֶךְ הָעוֹלָם זוֹקֵף כְּפוּפִים.

בָּרוּךְ אַתָּה ה׳ אֱלֹהֵינוּ מֶלֶךְ הָעוֹלָם רוֹקַע הָאָרֶץ עַל הַמָּיִם.

בָּרוּךְ אַתָּה ה׳ אֱלֹהֵינוּ מֶלֶךְ הָעוֹלָם שֶׁעָשָׂה לִי כָּל צָרְכִּי.

בָּרוּךְ אַתָּה ה׳ אֱלֹהֵינוּ מֶלֶךְ הָעוֹלָם הַמֵּכִין מִצְעֲדֵי גָבֶר.

בָּרוּךְ אַתָּה ה׳ אֱלֹהֵינוּ מֶלֶךְ הָעוֹלָם אוֹזֵר יִשְׂרָאֵל בִּגְבוּרָה.

בָּרוּךְ אַתָּה ה׳ אֱלֹהֵינוּ מֶלֶךְ הָעוֹלָם עוֹטֵר יִשְׂרָאֵל בְּתִפְאָרָה.

בָּרוּךְ אַתָּה ה׳ אֱלֹהֵינוּ מֶלֶךְ הָעוֹלָם הַנּוֹתֵן לַיָּעֵף כֹּחַ.

בָּרוּךְ אַתָּה ה׳ אֱלֹהֵינוּ מֶלֶךְ הָעוֹלָם הַמַּעֲבִיר שֵׁנָה מֵעֵינַי.
""".trimIndent()

private fun buildYotzerOr(): String = """
יוצר אור

בָּרוּךְ אַתָּה ה׳ אֱלֹהֵינוּ מֶלֶךְ הָעוֹלָם יוֹצֵר אוֹר וּבוֹרֵא חֹשֶׁךְ, עֹשֶׂה שָׁלוֹם וּבוֹרֵא אֶת הַכֹּל.

הַמֵּאִיר לָאָרֶץ וְלַדָּרִים עָלֶיהָ בְּרַחֲמִים, וּבְטוּבוֹ מְחַדֵּשׁ בְּכָל יוֹם תָּמִיד מַעֲשֵׂה בְרֵאשִׁית.

מָה רַבּוּ מַעֲשֶׂיךָ ה׳ כֻּלָּם בְּחָכְמָה עָשִׂיתָ, מָלְאָה הָאָרֶץ קִנְיָנֶךָ.

בָּרוּךְ אַתָּה ה׳ יוֹצֵר הַמְּאוֹרוֹת.
""".trimIndent()

private fun buildAmidahShacharit(): String = """
עמידה – שחרית (שמונה עשרה)

פתיחה
אֲדֹנָי שְׂפָתַי תִּפְתָּח וּפִי יַגִּיד תְּהִלָּתֶךָ.

א. אבות
בָּרוּךְ אַתָּה ה׳ אֱלֹהֵינוּ וֵאלֹהֵי אֲבוֹתֵינוּ, אֱלֹהֵי אַבְרָהָם אֱלֹהֵי יִצְחָק וֵאלֹהֵי יַעֲקֹב...
בָּרוּךְ אַתָּה ה׳ מָגֵן אַבְרָהָם.

ב. גבורות
אַתָּה גִּבּוֹר לְעוֹלָם אֲדֹנָי, מְחַיֵּה מֵתִים אַתָּה, רַב לְהוֹשִׁיעַ...
בָּרוּךְ אַתָּה ה׳ מְחַיֵּה הַמֵּתִים.

ג. קדושת ה׳
אַתָּה קָדוֹשׁ וְשִׁמְךָ קָדוֹשׁ וּקְדוֹשִׁים בְּכָל יוֹם יְהַלְלוּךָ סֶּלָה.
בָּרוּךְ אַתָּה ה׳ הָאֵל הַקָּדוֹשׁ.

─── ברכות אמצעיות (בחול) ───

ד. בינה | ה. תשובה | ו. סליחה | ז. גאולה
ח. רפואה | ט. ברכת השנים | י. קיבוץ גלויות
יא. השבת המשפט | יב. הפוך על הצדיקים | יג. ירושלים
יד. מלכות בית דוד | טו. שמיעת תפילה

─── ברכות אחרונות ───

טז. עבודה
רְצֵה ה׳ אֱלֹהֵינוּ בְּעַמְּךָ יִשְׂרָאֵל וּבִתְפִלָּתָם...
בָּרוּךְ אַתָּה ה׳ הַמַּחֲזִיר שְׁכִינָתוֹ לְצִיּוֹן.

יז. הודאה (מודים)
מוֹדִים אֲנַחְנוּ לָךְ שָׁאַתָּה הוּא ה׳ אֱלֹהֵינוּ וֵאלֹהֵי אֲבוֹתֵינוּ לְעוֹלָם וָעֶד...
בָּרוּךְ אַתָּה ה׳ הַטּוֹב שִׁמְךָ וּלְךָ נָאֶה לְהוֹדוֹת.

יח. ברכת שלום
שִׂים שָׁלוֹם טוֹבָה וּבְרָכָה, חֵן וָחֶסֶד וְרַחֲמִים עָלֵינוּ...
בָּרוּךְ אַתָּה ה׳ הַמְבָרֵךְ אֶת עַמּוֹ יִשְׂרָאֵל בַּשָּׁלוֹם.

יְהִיוּ לְרָצוֹן אִמְרֵי פִי וְהֶגְיוֹן לִבִּי לְפָנֶיךָ ה׳ צוּרִי וְגֹאֲלִי.
""".trimIndent()

private fun buildAmidahMincha(): String = """
עמידה – מנחה

(זהה לעמידת שחרית בנוסח, ג' ראשונות וג' אחרונות + ברכות אמצעיות)

פתיחה
אֲדֹנָי שְׂפָתַי תִּפְתָּח וּפִי יַגִּיד תְּהִלָּתֶךָ.

א. אבות | ב. גבורות | ג. קדושת ה׳
─── ברכות אמצעיות ───
ד–טו. כשחרית

─── ברכות אחרונות ───
טז. עבודה | יז. הודאה | יח. שלום

יְהִיוּ לְרָצוֹן אִמְרֵי פִי וְהֶגְיוֹן לִבִּי לְפָנֶיךָ ה׳ צוּרִי וְגֹאֲלִי.
""".trimIndent()

private fun buildAmidahMaariv(): String = """
עמידה – ערבית

פתיחה
אֲדֹנָי שְׂפָתַי תִּפְתָּח וּפִי יַגִּיד תְּהִלָּתֶךָ.

א. אבות | ב. גבורות | ג. קדושת ה׳
─── ברכות אמצעיות ───
ד–טו. כשחרית

─── ברכות אחרונות ───
טז. עבודה | יז. הודאה | יח. שלום

הַמַּבְדִּיל בֵּין קֹדֶשׁ לְחֹל (במוצ"ש)

יְהִיוּ לְרָצוֹן אִמְרֵי פִי וְהֶגְיוֹן לִבִּי לְפָנֶיךָ ה׳ צוּרִי וְגֹאֲלִי.
""".trimIndent()

private fun buildAleinu(): String = """
עָלֵינוּ לְשַׁבֵּחַ לַאֲדוֹן הַכֹּל, לָתֵת גְּדֻלָּה לְיוֹצֵר בְּרֵאשִׁית, שֶׁלֹּא עָשָׂנוּ כְּגוֹיֵי הָאֲרָצוֹת, וְלֹא שָׂמָנוּ כְּמִשְׁפְּחוֹת הָאֲדָמָה; שֶׁלֹּא שָׂם חֶלְקֵנוּ כָּהֶם, וְגוֹרָלֵנוּ כְּכָל הֲמוֹנָם. וַאֲנַחְנוּ כּוֹרְעִים וּמִשְׁתַּחֲוִים וּמוֹדִים לִפְנֵי מֶלֶךְ מַלְכֵי הַמְּלָכִים הַקָּדוֹשׁ בָּרוּךְ הוּא.

עַל כֵּן נְקַוֶּה לְּךָ ה׳ אֱלֹהֵינוּ לִרְאוֹת מְהֵרָה בְּתִפְאֶרֶת עֻזֶּךָ, לְהַעֲבִיר גִּלּוּלִים מִן הָאָרֶץ, וְהָאֱלִילִים כָּרוֹת יִכָּרֵתוּן, לְתַקֵּן עוֹלָם בְּמַלְכוּת שַׁדַּי.

וְנֶאֱמַר: וְהָיָה ה׳ לְמֶלֶךְ עַל כָּל הָאָרֶץ, בַּיּוֹם הַהוּא יִהְיֶה ה׳ אֶחָד וּשְׁמוֹ אֶחָד.
""".trimIndent()

private fun buildHamapil(): String = """
בָּרוּךְ אַתָּה ה׳ אֱלֹהֵינוּ מֶלֶךְ הָעוֹלָם הַמַּפִּיל חֶבְלֵי שֵׁנָה עַל עֵינַי וּתְנוּמָה עַל עַפְעַפַּי, וּמֵאִיר לְאִישׁוֹן בַּת עָיִן.

יְהִי רָצוֹן מִלְּפָנֶיךָ ה׳ אֱלֹהַי וֵאלֹהֵי אֲבוֹתַי, שֶׁתַּשְׁכִּיבֵנִי לְשָׁלוֹם וְתַעֲמִידֵנִי לְשָׁלוֹם, וְאַל יְבַהֲלוּנִי רַעְיוֹנַי וַחֲלוֹמוֹת רָעִים.

בָּרוּךְ אַתָּה ה׳ הַמֵּאִיר לְכָל הָעוֹלָם כֻּלּוֹ בִּכְבוֹדוֹ.
""".trimIndent()
