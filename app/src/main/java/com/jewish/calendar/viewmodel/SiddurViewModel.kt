package com.jewish.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.data.LocalSefariaRepository
import com.jewish.calendar.data.SefariaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

// ── Prayer time ────────────────────────────────────────────────────────

enum class SiddurPrayerTime(val hebrewName: String, val icon: String, val hourStart: Int, val hourEnd: Int) {
    SHACHARIT("שחרית", "🌅", 5, 10),
    MINCHA("מנחה", "☀️", 11, 17),
    MAARIV("ערבית", "🌙", 18, 22),
    BEDTIME("ק\"ש המיטה", "⭐", 23, 4)
}

// ── Prayer style (nusach) ──────────────────────────────────────────────

enum class NusachType(val hebrewName: String, val shortName: String, val icon: String) {
    ASHKENAZ  ("אשכנז",        "אשכנז",  "🕍"),
    SEPHARDI  ("ספרד",         "ספרד",   "✡️"),
    MIZRACHI  ("עדות המזרח",  "מזרח",   "🌙"),
    HASIDIC   ("חסידי (נוסח הארי)", "ח\"בד", "📖")
}

// ── Prayer section ─────────────────────────────────────────────────────

data class PrayerSectionData(
    val id: String,
    val title: String,
    val openingLine: String = "",       // Short representative line shown in list
    val sefariaRef: String? = null,     // Sefaria API ref for full biblical text
    val localAsset: String? = null,     // Bundled asset: "file.json" or "file.json|Section.Key"
    val staticText: String = "",        // Full text for short rabbinical prayers
    val halachicNote: String = "",      // Instruction or note
    val nusachNote: String = ""         // Nusach-specific label (e.g. "נוסח ספרד בלבד")
)

// ── UI State ───────────────────────────────────────────────────────────

data class SiddurUiState(
    val currentTime: SiddurPrayerTime = SiddurPrayerTime.SHACHARIT,
    val selectedCategory: SiddurPrayerTime = SiddurPrayerTime.SHACHARIT,
    val selectedNusach: NusachType = NusachType.SEPHARDI,
    val selectedSection: PrayerSectionData? = null,
    val displayedText: String = "",
    val isLoadingText: Boolean = false,
    val fontSize: Int = 22,
    val error: String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────

@HiltViewModel
class SiddurViewModel @Inject constructor(
    private val sefariaRepository: SefariaRepository,
    private val localSefariaRepository: LocalSefariaRepository
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

    fun selectNusach(nusach: NusachType) {
        _uiState.update {
            it.copy(
                selectedNusach = nusach,
                selectedSection = null,
                displayedText = "",
                error = null
            )
        }
    }

    fun selectSection(section: PrayerSectionData) {
        val needsLoading = section.sefariaRef != null || section.localAsset != null
        _uiState.update {
            it.copy(
                selectedSection = section,
                displayedText = "",
                error = null,
                isLoadingText = needsLoading
            )
        }
        when {
            section.localAsset != null -> fetchFromLocalAsset(section)
            section.sefariaRef != null -> fetchFromSefaria(section)
            else -> _uiState.update { it.copy(displayedText = section.staticText, isLoadingText = false) }
        }
    }

    fun clearSection() = _uiState.update { it.copy(selectedSection = null, displayedText = "", error = null) }
    fun increaseFontSize() = _uiState.update { it.copy(fontSize = (it.fontSize + 2).coerceAtMost(40)) }
    fun decreaseFontSize() = _uiState.update { it.copy(fontSize = (it.fontSize - 2).coerceAtLeast(14)) }

    // ── Local asset fetch ──────────────────────────────────────────────

    private fun fetchFromLocalAsset(section: PrayerSectionData) {
        viewModelScope.launch {
            val text = localSefariaRepository.getTextFromAsset(section.localAsset!!)
            _uiState.update {
                it.copy(
                    displayedText = if (text.isNotBlank()) text
                                    else section.staticText.ifBlank { "לא ניתן לטעון את הטקסט" },
                    isLoadingText = false
                )
            }
        }
    }

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

    fun getSectionsFor(time: SiddurPrayerTime): List<PrayerSectionData> {
        val nusach = _uiState.value.selectedNusach
        return when (time) {
            SiddurPrayerTime.SHACHARIT -> shacharitSections + nusachShacharitAdditions(nusach) +
                                          listOf(birkatHaMazonSection(nusach), hallelSection())
            SiddurPrayerTime.MINCHA   -> minchaSections + listOf(birkatHaMazonSection(nusach))
            SiddurPrayerTime.MAARIV   -> maarivSections + nusachMaarivAdditions(nusach) +
                                          listOf(birkatHaMazonSection(nusach), kabbalatShabbatSection())
            SiddurPrayerTime.BEDTIME  -> bedtimeSections
        }
    }

    // ── Bundled liturgy helpers ────────────────────────────────────────

    private fun birkatHaMazonSection(nusach: NusachType): PrayerSectionData {
        val assetFile = when (nusach) {
            NusachType.ASHKENAZ -> "birkat_hamazon_ashkenaz.json"
            NusachType.SEPHARDI -> "birkat_hamazon_sefard.json"
            NusachType.MIZRACHI -> "birkat_hamazon_mizrach.json"
            NusachType.HASIDIC  -> "birkat_hamazon_ari.json"
        }
        return PrayerSectionData(
            id           = "birkat_hamazon_${nusach.name.lowercase()}",
            title        = "ברכת המזון",
            openingLine  = "רַבּוֹתַי נְבָרֵךְ",
            localAsset   = "$assetFile|Birkat Hamazon",
            halachicNote = "נאמרת לאחר אכילת לחם – מדאורייתא",
            nusachNote   = nusach.shortName
        )
    }

    private fun kabbalatShabbatSection() = PrayerSectionData(
        id           = "kabbalat_shabbat",
        title        = "קבלת שבת",
        openingLine  = "לְכוּ נְרַנֲנָה לַיהֹוָה",
        localAsset   = "kabbalat_shabbat.json",
        halachicNote = "נאמרת בערב שבת לפני תפילת ערבית"
    )

    private fun hallelSection() = PrayerSectionData(
        id           = "hallel",
        title        = "הַלֵּל",
        openingLine  = "הַלְלוּיָהּ הַלְלוּ עַבְדֵי ה׳",
        localAsset   = "hallel.json",
        halachicNote = "נאמר בר\"ח, חנוכה, סוכות, פסח ושבועות"
    )

    // ── Nusach-specific additions ──────────────────────────────────────

    private fun nusachShacharitAdditions(nusach: NusachType): List<PrayerSectionData> = when (nusach) {
        NusachType.ASHKENAZ -> listOf(
            PrayerSectionData(
                id          = "ledavid_ashkenaz",
                title       = "לְדָוִד ה׳ אוֹרִי",
                openingLine = "ה׳ אוֹרִי וְיִשְׁעִי מִמִּי אִירָא",
                sefariaRef  = "Psalms 27",
                nusachNote  = "נוסח אשכנז",
                halachicNote = "נאמר מאלול עד הושענא רבה"
            )
        )
        NusachType.SEPHARDI -> listOf(
            PrayerSectionData(
                id          = "ana_bekoa_sep",
                title       = "אָנָּא בְּכֹחַ",
                openingLine = "אָנָּא בְּכֹחַ גְּדֻלַּת יְמִינְךָ",
                staticText  = buildAnaBeKoach(),
                nusachNote  = "נוסח ספרד",
                halachicNote = "נאמר לפני פסוקי דזמרה"
            )
        )
        NusachType.HASIDIC -> listOf(
            PrayerSectionData(
                id          = "ana_bekoa_hasid",
                title       = "אָנָּא בְּכֹחַ",
                openingLine = "אָנָּא בְּכֹחַ גְּדֻלַּת יְמִינְךָ",
                staticText  = buildAnaBeKoach(),
                nusachNote  = "נוסח חסידי",
                halachicNote = "נאמר לפני פסוקי דזמרה"
            ),
            PrayerSectionData(
                id          = "leshem_yichud",
                title       = "לְשֵׁם יִחוּד",
                openingLine = "לְשֵׁם יִחוּד קֻדְשָׁא בְּרִיךְ הוּא",
                staticText  = buildLeshemYichud(),
                nusachNote  = "נוסח חסידי",
                halachicNote = "נאמר לפני כל מצווה – נוהג חסידים"
            )
        )
        NusachType.MIZRACHI -> listOf(
            PrayerSectionData(
                id          = "bakkashot",
                title       = "בַּקָּשׁוֹת – אדון עולם",
                openingLine = "אֲדוֹן עוֹלָם אֲשֶׁר מָלַךְ",
                staticText  = buildAdonOlam(),
                nusachNote  = "עדות המזרח",
                halachicNote = "שיר הפותח תפילת שחרית – עדות המזרח"
            ),
            PrayerSectionData(
                id          = "ana_bekoa_miz",
                title       = "אָנָּא בְּכֹחַ",
                openingLine = "אָנָּא בְּכֹחַ גְּדֻלַּת יְמִינְךָ",
                staticText  = buildAnaBeKoach(),
                nusachNote  = "עדות המזרח"
            )
        )
    }

    private fun nusachMaarivAdditions(nusach: NusachType): List<PrayerSectionData> = when (nusach) {
        NusachType.ASHKENAZ -> listOf(
            PrayerSectionData(
                id          = "hashkivenu_ashkenaz",
                title       = "הַשְׁכִּיבֵנוּ",
                openingLine = "הַשְׁכִּיבֵנוּ ה׳ אֱלֹהֵינוּ לְשָׁלוֹם",
                staticText  = buildHashkivenu(ashkenaz = true),
                nusachNote  = "נוסח אשכנז"
            )
        )
        NusachType.SEPHARDI, NusachType.MIZRACHI, NusachType.HASIDIC -> listOf(
            PrayerSectionData(
                id          = "hashkivenu_sep",
                title       = "הַשְׁכִּיבֵנוּ",
                openingLine = "הַשְׁכִּיבֵנוּ ה׳ אֱלֹהֵינוּ לְשָׁלוֹם",
                staticText  = buildHashkivenu(ashkenaz = false),
                nusachNote  = if (nusach == NusachType.SEPHARDI) "נוסח ספרד" else if (nusach == NusachType.MIZRACHI) "עדות המזרח" else "נוסח חסידי"
            )
        )
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

בָּרוּךְ אַתָּה ה׳ אֱלֹהֵינוּ מֶלֶךְ הָעוֹלָם, יוֹצֵר אוֹר וּבוֹרֵא חֹשֶׁךְ, עֹשֶׂה שָׁלוֹם וּבוֹרֵא אֶת הַכֹּל.

הַמֵּאִיר לָאָרֶץ וְלַדָּרִים עָלֶיהָ בְּרַחֲמִים, וּבְטוּבוֹ מְחַדֵּשׁ בְּכָל יוֹם תָּמִיד מַעֲשֵׂה בְרֵאשִׁית. מָה רַבּוּ מַעֲשֶׂיךָ ה׳, כֻּלָּם בְּחָכְמָה עָשִׂיתָ, מָלְאָה הָאָרֶץ קִנְיָנֶךָ.

הַמֶּלֶךְ הַמְרוֹמָם לְבַדּוֹ מֵאָז, הַמְשֻׁבָּח וְהַמְפֹאָר וְהַמִּתְנַשֵּׂא מִימוֹת עוֹלָם. אֱלֹהֵי עוֹלָם, בְּרַחֲמֶיךָ הָרַבִּים רַחֵם עָלֵינוּ, אֲדוֹן עֻזֵּנוּ, צוּר מִשְׂגַּבֵּנוּ, מָגֵן יִשְׁעֵנוּ, מִשְׂגָּב בַּעֲדֵנוּ.

אֵל בָּרוּךְ גְּדוֹל דֵּעָה, הֵכִין וּפָעַל זָהֳרֵי חַמָּה; טוֹב יָצַר כָּבוֹד לִשְׁמוֹ; מְאוֹרוֹת נָתַן סְבִיבוֹת עֻזּוֹ; פִּנּוֹת צְבָאָיו קְדוֹשִׁים רוֹמְמֵי שַׁדַּי; תָּמִיד מְסַפְּרִים כְּבוֹד אֵל וּקְדֻשָּׁתוֹ.

תִּתְבָּרַךְ ה׳ אֱלֹהֵינוּ עַל שֶׁבַח מַעֲשֵׂה יָדֶיךָ, וְעַל מְאוֹרֵי אוֹר שֶׁעָשִׂיתָ יְפָאֲרוּךָ סֶּלָה.

בָּרוּךְ אַתָּה ה׳ יוֹצֵר הַמְּאוֹרוֹת.
""".trimIndent()

private fun buildAmidahShacharit(): String = buildFullAmidah("שחרית")

private fun buildAmidahMincha(): String = buildFullAmidah("מנחה")

private fun buildAmidahMaariv(): String = buildFullAmidah("ערבית") + "\n\n" +
    "─── מוצאי שבת ───\n" +
    "אַתָּה חוֹנַנְתָּנוּ לְמַדַּע תּוֹרָתֶךָ, וַתְּלַמְּדֵנוּ לַעֲשׂוֹת חֻקֵּי רְצוֹנֶךָ. " +
    "וַתַּבְדֵּל ה׳ אֱלֹהֵינוּ בֵּין קֹדֶשׁ לְחֹל, בֵּין אוֹר לְחֹשֶׁךְ, " +
    "בֵּין יִשְׂרָאֵל לָעַמִּים, בֵּין יוֹם הַשְּׁבִיעִי לְשֵׁשֶׁת יְמֵי הַמַּעֲשֶׂה."

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

private fun buildAnaBeKoach(): String = """
אָנָּא בְּכֹחַ גְּדֻלַּת יְמִינְךָ, תַּתִּיר צְרוּרָה.
קַבֵּל רִנַּת עַמְּךָ, שַׂגְּבֵנוּ טַהֲרֵנוּ נוֹרָא.
נָא גִבּוֹר דּוֹרְשֵׁי יִחוּדְךָ, כְּבָבַת שָׁמְרֵם.
בָּרְכֵם טַהֲרֵם רַחֲמֵי צִדְקָתְךָ, תָּמִיד גָּמְלֵם.
חֲסִין קָדוֹשׁ בְּרֹב טוּבְךָ, נַהֵל עֲדָתֶךָ.
יָחִיד גֵּאֶה לְעַמְּךָ פְּנֵה, זוֹכְרֵי קְדֻשָּׁתֶךָ.
שַׁוְעָתֵנוּ קַבֵּל וּשְׁמַע צַעֲקָתֵנוּ, יוֹדֵעַ תַּעֲלוּמוֹת.

בָּרוּךְ שֵׁם כְּבוֹד מַלְכוּתוֹ לְעוֹלָם וָעֶד.
""".trimIndent()

private fun buildLeshemYichud(): String = """
לְשֵׁם יִחוּד קֻדְשָׁא בְּרִיךְ הוּא וּשְׁכִינְתֵּיהּ, בִּדְחִילוּ וּרְחִימוּ, וּרְחִימוּ וּדְחִילוּ, לְיַחֲדָא שֵׁם יוּ"ד הֵ"א בְּוָא"ו הֵ"א בְּיִחוּדָא שְׁלִים, בְּשֵׁם כָּל יִשְׂרָאֵל.

הֲרֵינִי מְקַבֵּל עָלַי עוֹל מַלְכוּת שָׁמַיִם, וְאֶהְיֶה מְזֻמָּן לְקַיֵּם מִצְוַת בּוֹרְאִי בְּלֵב שָׁלֵם וּבְנֶפֶשׁ חֲפֵצָה.

יְהִי רָצוֹן מִלְּפָנֶיךָ ה׳ אֱלֹהֵינוּ וֵאלֹהֵי אֲבוֹתֵינוּ, שֶׁתְּהֵא חֲשׁוּבָה תְּפִלָּה זוֹ לְפָנֶיךָ כְּאִלּוּ הִתְפַּלַּלְנוּ בְּכָל הַכַּוָּנָה הָרְאוּיָה.
""".trimIndent()

private fun buildAdonOlam(): String = """
אֲדוֹן עוֹלָם אֲשֶׁר מָלַךְ, בְּטֶרֶם כָּל יְצִיר נִבְרָא.
לְעֵת נַעֲשָׂה בְחֶפְצוֹ כֹּל, אֲזַי מֶלֶךְ שְׁמוֹ נִקְרָא.
וְאַחֲרֵי כִּכְלוֹת הַכֹּל, לְבַדּוֹ יִמְלוֹךְ נוֹרָא.
וְהוּא הָיָה וְהוּא הֹוֶה, וְהוּא יִהְיֶה בְּתִפְאָרָה.

וְהוּא אֶחָד וְאֵין שֵׁנִי, לְהַמְשִׁיל לוֹ לְהַחְבִּירָה.
בְּלִי רֵאשִׁית בְּלִי תַכְלִית, וְלוֹ הָעֹז וְהַמִּשְׂרָה.

וְהוּא אֵלִי וְחַי גֹּאֲלִי, וְצוּר חֶבְלִי בְּעֵת צָרָה.
וְהוּא נִסִּי וּמָנוֹס לִי, מְנָת כּוֹסִי בְּיוֹם אֶקְרָא.

בְּיָדוֹ אַפְקִיד רוּחִי, בְּעֵת אִישַׁן וְאָעִירָה.
וְעִם רוּחִי גְּוִיָּתִי, ה׳ לִי וְלֹא אִירָא.
""".trimIndent()

private fun buildHashkivenu(ashkenaz: Boolean): String = """
הַשְׁכִּיבֵנוּ ה׳ אֱלֹהֵינוּ לְשָׁלוֹם, וְהַעֲמִידֵנוּ מַלְכֵּנוּ לְחַיִּים, וּפְרֹשׂ עָלֵינוּ סֻכַּת שְׁלוֹמֶךָ, וְתַקְּנֵנוּ בְּעֵצָה טוֹבָה מִלְּפָנֶיךָ, וְהוֹשִׁיעֵנוּ לְמַעַן שְׁמֶךָ.

וְהָגֵן בַּעֲדֵנוּ, וְהָסֵר מֵעָלֵינוּ אוֹיֵב, דֶּבֶר, וְחֶרֶב, וְרָעָב, וְיָגוֹן, וְהָסֵר שָׂטָן מִלְּפָנֵינוּ וּמֵאַחֲרֵינוּ.

${if (ashkenaz) "וּבְצֵל כְּנָפֶיךָ תַּסְתִּירֵנוּ, כִּי אֵל שׁוֹמְרֵנוּ וּמַצִּילֵנוּ אָתָּה, כִּי אֵל מֶלֶךְ חַנּוּן וְרַחוּם אָתָּה." else "וּבְצֵל כְּנָפֶיךָ תַּסְתִּירֵנוּ, כִּי אֵל שׁוֹמְרֵנוּ אָתָּה, כִּי אֵל מֶלֶךְ חַנּוּן וְרַחוּם אָתָּה."}

וּשְׁמֹר צֵאתֵנוּ וּבוֹאֵנוּ, לְחַיִּים וּלְשָׁלוֹם, מֵעַתָּה וְעַד עוֹלָם.

בָּרוּךְ אַתָּה ה׳, שׁוֹמֵר עַמּוֹ יִשְׂרָאֵל לָעַד.
""".trimIndent()

// ── Full Weekday Amidah (all 19 blessings) ────────────────────────────

private fun buildFullAmidah(prayerName: String): String = """
עמידה – $prayerName (שמונה עשרה ברכות)

אֲדֹנָי שְׂפָתַי תִּפְתָּח וּפִי יַגִּיד תְּהִלָּתֶךָ.

א. אָבוֹת
בָּרוּךְ אַתָּה ה׳ אֱלֹהֵינוּ וֵאלֹהֵי אֲבוֹתֵינוּ, אֱלֹהֵי אַבְרָהָם, אֱלֹהֵי יִצְחָק, וֵאלֹהֵי יַעֲקֹב, הָאֵל הַגָּדוֹל הַגִּבּוֹר וְהַנּוֹרָא, אֵל עֶלְיוֹן, גּוֹמֵל חֲסָדִים טוֹבִים, וְקוֹנֵה הַכֹּל, וְזוֹכֵר חַסְדֵי אָבוֹת, וּמֵבִיא גוֹאֵל לִבְנֵי בְנֵיהֶם, לְמַעַן שְׁמוֹ בְּאַהֲבָה.
מֶלֶךְ עוֹזֵר וּמוֹשִׁיעַ וּמָגֵן.
בָּרוּךְ אַתָּה ה׳ מָגֵן אַבְרָהָם.

ב. גְּבוּרוֹת
אַתָּה גִּבּוֹר לְעוֹלָם אֲדֹנָי, מְחַיֵּה מֵתִים אַתָּה, רַב לְהוֹשִׁיעַ.
מוֹרִיד הַטָּל. (בחורף: מַשִּׁיב הָרוּחַ וּמוֹרִיד הַגֶּשֶׁם)
מְכַלְכֵּל חַיִּים בְּחֶסֶד, מְחַיֵּה מֵתִים בְּרַחֲמִים רַבִּים, סוֹמֵךְ נוֹפְלִים, וְרוֹפֵא חוֹלִים, וּמַתִּיר אֲסוּרִים, וּמְקַיֵּם אֱמוּנָתוֹ לִישֵׁנֵי עָפָר.
מִי כָמוֹךָ בַּעַל גְּבוּרוֹת וּמִי דּוֹמֶה לָּךְ, מֶלֶךְ מֵמִית וּמְחַיֶּה וּמַצְמִיחַ יְשׁוּעָה.
וְנֶאֱמָן אַתָּה לְהַחֲיוֹת מֵתִים.
בָּרוּךְ אַתָּה ה׳ מְחַיֵּה הַמֵּתִים.

ג. קְדֻשַּׁת הַשֵּׁם
אַתָּה קָדוֹשׁ וְשִׁמְךָ קָדוֹשׁ, וּקְדוֹשִׁים בְּכָל יוֹם יְהַלְלוּךָ סֶּלָה.
בָּרוּךְ אַתָּה ה׳ הָאֵל הַקָּדוֹשׁ.

ד. בִּינָה
אַתָּה חוֹנֵן לְאָדָם דַּעַת, וּמְלַמֵּד לֶאֱנוֹשׁ בִּינָה.
חָנֵּנוּ מֵאִתְּךָ חָכְמָה בִּינָה וָדָעַת.
בָּרוּךְ אַתָּה ה׳ חוֹנֵן הַדָּעַת.

ה. תְּשׁוּבָה
הֲשִׁיבֵנוּ אָבִינוּ לְתוֹרָתֶךָ, וְקָרְבֵנוּ מַלְכֵּנוּ לַעֲבוֹדָתֶךָ, וְהַחֲזִירֵנוּ בִּתְשׁוּבָה שְׁלֵמָה לְפָנֶיךָ.
בָּרוּךְ אַתָּה ה׳ הָרוֹצֶה בִּתְשׁוּבָה.

ו. סְלִיחָה
סְלַח לָנוּ אָבִינוּ כִּי חָטָאנוּ, מְחַל לָנוּ מַלְכֵּנוּ כִּי פָּשַׁעְנוּ, כִּי מוֹחֵל וְסוֹלֵחַ אָתָּה.
בָּרוּךְ אַתָּה ה׳ חַנּוּן הַמַּרְבֶּה לִסְלוֹחַ.

ז. גְּאֻלָּה
רְאֵה בְעָנְיֵנוּ וְרִיבָה רִיבֵנוּ, וּגְאָלֵנוּ מְהֵרָה לְמַעַן שְׁמֶךָ, כִּי גוֹאֵל חָזָק אָתָּה.
בָּרוּךְ אַתָּה ה׳ גּוֹאֵל יִשְׂרָאֵל.

ח. רְפוּאָה
רְפָאֵנוּ ה׳ וְנֵרָפֵא, הוֹשִׁיעֵנוּ וְנִוָּשֵׁעָה, כִּי תְהִלָּתֵנוּ אָתָּה. וְהַעֲלֵה אֲרוּכָה וּמַרְפֵּא לְכָל מַכּוֹתֵינוּ.
בָּרוּךְ אַתָּה ה׳ רוֹפֵא חוֹלֵי עַמּוֹ יִשְׂרָאֵל.

ט. בִּרְכַּת הַשָּׁנִים
בָּרֵךְ עָלֵינוּ ה׳ אֱלֹהֵינוּ אֶת הַשָּׁנָה הַזֹּאת וְאֶת כָּל מִינֵי תְבוּאָתָהּ לְטוֹבָה.
(בחורף: וְתֵן טַל וּמָטָר לִבְרָכָה, בקיץ: וְתֵן בְּרָכָה)
עַל פְּנֵי הָאֲדָמָה, וְשַׂבְּעֵנוּ מִטּוּבֶךָ, וּבָרֵךְ שְׁנָתֵנוּ כַּשָּׁנִים הַטּוֹבוֹת.
בָּרוּךְ אַתָּה ה׳ מְבָרֵךְ הַשָּׁנִים.

י. קִבּוּץ גָּלֻיּוֹת
תְּקַע בְּשׁוֹפָר גָּדוֹל לְחֵרוּתֵנוּ, וְשָׂא נֵס לְקַבֵּץ גָּלֻיּוֹתֵינוּ, וְקַבְּצֵנוּ יַחַד מֵאַרְבַּע כַּנְפוֹת הָאָרֶץ.
בָּרוּךְ אַתָּה ה׳ מְקַבֵּץ נִדְחֵי עַמּוֹ יִשְׂרָאֵל.

יא. הַשְׁבַּת הַמִּשְׁפָּט
הָשִׁיבָה שׁוֹפְטֵינוּ כְּבָרִאשׁוֹנָה וְיוֹעֲצֵינוּ כְּבַתְּחִלָּה, וְהָסֵר מִמֶּנּוּ יָגוֹן וַאֲנָחָה, וּמְלֹךְ עָלֵינוּ אַתָּה ה׳ לְבַדְּךָ בְּחֶסֶד וּבְרַחֲמִים, וְצַדְּקֵנוּ בַּמִּשְׁפָּט.
בָּרוּךְ אַתָּה ה׳ מֶלֶךְ אוֹהֵב צְדָקָה וּמִשְׁפָּט.

יב. עַל הַמִּינִים
וְלַמַּלְשִׁינִים אַל תְּהִי תִקְוָה, וְכָל הָרִשְׁעָה כְּרֶגַע תֹּאבֵד, וְכָל אוֹיְבֶיךָ מְהֵרָה יִכָּרֵתוּ. וְהַזֵּדִים מְהֵרָה תְּעַקֵּר וּתְשַׁבֵּר וּתְמַגֵּר וְתַכְנִיעַ בִּמְהֵרָה בְיָמֵינוּ.
בָּרוּךְ אַתָּה ה׳ שׁוֹבֵר אוֹיְבִים וּמַכְנִיעַ זֵדִים.

יג. עַל הַצַּדִּיקִים
עַל הַצַּדִּיקִים וְעַל הַחֲסִידִים וְעַל זִקְנֵי עַמְּךָ בֵּית יִשְׂרָאֵל, וְעַל פְּלֵיטַת סוֹפְרֵיהֶם, וְעַל גֵּרֵי הַצֶּדֶק וְעָלֵינוּ, יֶהֱמוּ רַחֲמֶיךָ ה׳ אֱלֹהֵינוּ. וְתֵן שָׂכָר טוֹב לְכָל הַבּוֹטְחִים בְּשִׁמְךָ בֶּאֱמֶת, וְשִׂים חֶלְקֵנוּ עִמָּהֶם, וּלְעוֹלָם לֹא נֵבוֹשׁ כִּי בְךָ בָטָחְנוּ.
בָּרוּךְ אַתָּה ה׳ מִשְׁעָן וּמִבְטָח לַצַּדִּיקִים.

יד. בִּנְיַן יְרוּשָׁלַיִם
וְלִירוּשָׁלַיִם עִירְךָ בְּרַחֲמִים תָּשׁוּב, וְתִשְׁכּוֹן בְּתוֹכָהּ כַּאֲשֶׁר דִּבַּרְתָּ, וּבְנֵה אוֹתָהּ בְּקָרוֹב בְּיָמֵינוּ בִּנְיַן עוֹלָם, וְכִסֵּא דָוִד עַבְדְּךָ מְהֵרָה בְתוֹכָהּ תָּכִין.
בָּרוּךְ אַתָּה ה׳ בּוֹנֵה יְרוּשָׁלָיִם.

טו. מַלְכוּת בֵּית דָּוִד
אֶת צֶמַח דָּוִד עַבְדְּךָ מְהֵרָה תַצְמִיחַ, וְקַרְנוֹ תָּרוּם בִּישׁוּעָתֶךָ, כִּי לִישׁוּעָתְךָ קִוִּינוּ כָּל הַיּוֹם.
בָּרוּךְ אַתָּה ה׳ מַצְמִיחַ קֶרֶן יְשׁוּעָה.

טז. שְׁמִיעַת תְּפִלָּה
שְׁמַע קוֹלֵנוּ ה׳ אֱלֹהֵינוּ, חוּס וְרַחֵם עָלֵינוּ, וְקַבֵּל בְּרַחֲמִים וּבְרָצוֹן אֶת תְּפִלָּתֵנוּ, כִּי אֵל שׁוֹמֵעַ תְּפִלּוֹת וְתַחֲנוּנִים אָתָּה. וּמִלְּפָנֶיךָ מַלְכֵּנוּ רֵיקָם אַל תְּשִׁיבֵנוּ.
בָּרוּךְ אַתָּה ה׳ שׁוֹמֵעַ תְּפִלָּה.

יז. עֲבוֹדָה (רְצֵה)
רְצֵה ה׳ אֱלֹהֵינוּ בְּעַמְּךָ יִשְׂרָאֵל וּבִתְפִלָּתָם, וְהָשֵׁב אֶת הָעֲבוֹדָה לִדְבִיר בֵּיתֶךָ, וְאִשֵּׁי יִשְׂרָאֵל וּתְפִלָּתָם בְּאַהֲבָה תְקַבֵּל בְּרָצוֹן, וּתְהִי לְרָצוֹן תָּמִיד עֲבוֹדַת יִשְׂרָאֵל עַמֶּךָ.
וְתֶחֱזֶינָה עֵינֵינוּ בְּשׁוּבְךָ לְצִיּוֹן בְּרַחֲמִים.
בָּרוּךְ אַתָּה ה׳ הַמַּחֲזִיר שְׁכִינָתוֹ לְצִיּוֹן.

יח. הוֹדָאָה (מוֹדִים)
מוֹדִים אֲנַחְנוּ לָךְ שָׁאַתָּה הוּא ה׳ אֱלֹהֵינוּ וֵאלֹהֵי אֲבוֹתֵינוּ לְעוֹלָם וָעֶד. צוּר חַיֵּינוּ, מָגֵן יִשְׁעֵנוּ אַתָּה הוּא לְדוֹר וָדוֹר.
נוֹדֶה לְּךָ וּנְסַפֵּר תְּהִלָּתֶךָ עַל חַיֵּינוּ הַמְּסוּרִים בְּיָדֶךָ, וְעַל נִשְׁמוֹתֵינוּ הַפְּקוּדוֹת לָךְ, וְעַל נִסֶּיךָ שֶׁבְּכָל יוֹם עִמָּנוּ, וְעַל נִפְלְאוֹתֶיךָ וְטוֹבוֹתֶיךָ שֶׁבְּכָל עֵת, עֶרֶב וָבֹקֶר וְצָהֳרָיִם.
הַטּוֹב, כִּי לֹא כָלוּ רַחֲמֶיךָ; וְהַמְרַחֵם, כִּי לֹא תַמּוּ חֲסָדֶיךָ; מֵעוֹלָם קִוִּינוּ לָךְ.
בָּרוּךְ אַתָּה ה׳ הַטּוֹב שִׁמְךָ וּלְךָ נָאֶה לְהוֹדוֹת.

יט. שָׁלוֹם
שִׂים שָׁלוֹם טוֹבָה וּבְרָכָה, חֵן וָחֶסֶד וְרַחֲמִים, עָלֵינוּ וְעַל כָּל יִשְׂרָאֵל עַמֶּךָ. בָּרְכֵנוּ אָבִינוּ כֻּלָּנוּ כְּאֶחָד בְּאוֹר פָּנֶיךָ, כִּי בְאוֹר פָּנֶיךָ נָתַתָּ לָּנוּ ה׳ אֱלֹהֵינוּ תּוֹרַת חַיִּים וְאַהֲבַת חֶסֶד, וּצְדָקָה וּבְרָכָה וְרַחֲמִים וְחַיִּים וְשָׁלוֹם. וְטוֹב בְּעֵינֶיךָ לְבָרֵךְ אֶת עַמְּךָ יִשְׂרָאֵל בְּכָל עֵת וּבְכָל שָׁעָה בִּשְׁלוֹמֶךָ.
בָּרוּךְ אַתָּה ה׳ הַמְבָרֵךְ אֶת עַמּוֹ יִשְׂרָאֵל בַּשָּׁלוֹם.

יִהְיוּ לְרָצוֹן אִמְרֵי פִי וְהֶגְיוֹן לִבִּי לְפָנֶיךָ ה׳ צוּרִי וְגֹאֲלִי.
""".trimIndent()
