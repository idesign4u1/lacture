package com.jewish.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jewish.calendar.data.SefariaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Data models ────────────────────────────────────────────────────────

data class TorahMatch(
    val word: String,
    val gematria: Int,
    val sourceHe: String,    // e.g. "בראשית א:א"
    val context: String,     // short surrounding phrase
    val sefariaRef: String   // for Sefaria API
)

data class GematriaUiState(
    val input: String = "",
    val gematriaValue: Int? = null,
    val letterBreakdown: String = "",
    val matches: List<TorahMatch> = emptyList(),
    val selectedMatch: TorahMatch? = null,
    val verseText: String = "",
    val isLoadingVerse: Boolean = false,
    val fontSize: Int = 22
)

// ── ViewModel ──────────────────────────────────────────────────────────

@HiltViewModel
class GematriaViewModel @Inject constructor(
    private val sefariaRepository: SefariaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GematriaUiState())
    val uiState: StateFlow<GematriaUiState> = _uiState.asStateFlow()

    // Standard gematria letter values
    private val letterValues = mapOf(
        'א' to 1,  'ב' to 2,  'ג' to 3,  'ד' to 4,  'ה' to 5,
        'ו' to 6,  'ז' to 7,  'ח' to 8,  'ט' to 9,  'י' to 10,
        'כ' to 20, 'ך' to 20, 'ל' to 30, 'מ' to 40, 'ם' to 40,
        'נ' to 50, 'ן' to 50, 'ס' to 60, 'ע' to 70, 'פ' to 80,
        'ף' to 80, 'צ' to 90, 'ץ' to 90, 'ק' to 100,'ר' to 200,
        'ש' to 300,'ת' to 400
    )

    // ── Public actions ─────────────────────────────────────────────────

    fun onInputChange(text: String) {
        val hebrewOnly = text.filter { it in letterValues || it == ' ' }
        val value = hebrewOnly.filter { it in letterValues }.sumOf { letterValues[it]!! }
        val breakdown = buildBreakdown(hebrewOnly)
        val matches = if (value > 0) torahDictionary.filter { it.gematria == value } else emptyList()
        _uiState.update {
            it.copy(
                input          = text,
                gematriaValue  = if (value > 0) value else null,
                letterBreakdown = breakdown,
                matches        = matches,
                selectedMatch  = null,
                verseText      = ""
            )
        }
    }

    fun selectMatch(match: TorahMatch) {
        _uiState.update { it.copy(selectedMatch = match, verseText = "", isLoadingVerse = true) }
        viewModelScope.launch {
            sefariaRepository.getTextForRef(match.sefariaRef)
                .onSuccess { text -> _uiState.update { it.copy(verseText = text, isLoadingVerse = false) } }
                .onFailure { _uiState.update { it.copy(verseText = match.context, isLoadingVerse = false) } }
        }
    }

    fun clearMatch()       = _uiState.update { it.copy(selectedMatch = null, verseText = "") }
    fun increaseFontSize() = _uiState.update { it.copy(fontSize = (it.fontSize + 2).coerceAtMost(40)) }
    fun decreaseFontSize() = _uiState.update { it.copy(fontSize = (it.fontSize - 2).coerceAtLeast(14)) }

    // ── Letter breakdown ───────────────────────────────────────────────

    private fun buildBreakdown(text: String): String {
        val letters = text.filter { it in letterValues }
        if (letters.isEmpty()) return ""
        val parts = letters.map { "$it(${letterValues[it]})" }
        val total = letters.sumOf { letterValues[it]!! }
        return parts.joinToString(" + ") + " = $total"
    }

    // ── Torah dictionary ───────────────────────────────────────────────
    // Letter values: א=1 ב=2 ג=3 ד=4 ה=5 ו=6 ז=7 ח=8 ט=9
    //   י=10 כ/ך=20 ל=30 מ/ם=40 נ/ן=50 ס=60 ע=70 פ/ף=80 צ/ץ=90
    //   ק=100 ר=200 ש=300 ת=400

    val torahDictionary: List<TorahMatch> = listOf(

        // 13 ─ אהבה = אחד (אהבת ה׳ = אחדות ה׳)
        TorahMatch("אַהֲבָה", 13, "בראשית כט:כ",
            "וַיֶּאֱהַב אֹתָהּ", "Genesis 29:20"),
        TorahMatch("אֶחָד", 13, "דברים ו:ד",
            "שְׁמַע יִשְׂרָאֵל ה׳ אֱלֹהֵינוּ ה׳ אֶחָד", "Deuteronomy 6:4"),

        // 14
        TorahMatch("יָד", 14, "שמות ג:כ",
            "וְשָׁלַחְתִּי אֶת יָדִי וְהִכֵּיתִי אֶת מִצְרַיִם", "Exodus 3:20"),
        TorahMatch("דָּוִד", 14, "שמואל א טז:יג",
            "וַיִּמְשַׁח אֹתוֹ בְּקֶרֶב אֶחָיו", "I Samuel 16:13"),

        // 17
        TorahMatch("טוֹב", 17, "בראשית א:לא",
            "וְהִנֵּה טוֹב מְאֹד", "Genesis 1:31"),

        // 18
        TorahMatch("חַי", 18, "בראשית ג:כ",
            "כִּי הִוא הָיְתָה אֵם כָּל חָי", "Genesis 3:20"),

        // 19
        TorahMatch("חַוָּה", 19, "בראשית ג:כ",
            "וַיִּקְרָא הָאָדָם שֵׁם אִשְׁתּוֹ חַוָּה", "Genesis 3:20"),

        // 26 ─ שם הוי"ה
        TorahMatch("יְהוָה", 26, "שמות ג:יד",
            "אֶהְיֶה אֲשֶׁר אֶהְיֶה", "Exodus 3:14"),

        // 31
        TorahMatch("אֵל", 31, "שמות טו:ב",
            "עָזִּי וְזִמְרָת יָהּ וַיְהִי לִי לִישׁוּעָה", "Exodus 15:2"),

        // 32 ─ לב = כבוד
        TorahMatch("לֵב", 32, "דברים ו:ה",
            "וְאָהַבְתָּ בְּכָל לְבָבְךָ", "Deuteronomy 6:5"),
        TorahMatch("כָּבוֹד", 32, "שמות טז:ז",
            "וּרְאִיתֶם אֶת כְּבוֹד ה׳", "Exodus 16:7"),

        // 45 ─ אדם = מאד
        TorahMatch("אָדָם", 45, "בראשית א:כז",
            "וַיִּבְרָא אֱלֹהִים אֶת הָאָדָם בְּצַלְמוֹ", "Genesis 1:27"),
        TorahMatch("מְאֹד", 45, "בראשית א:לא",
            "וְהִנֵּה טוֹב מְאֹד", "Genesis 1:31"),

        // 53
        TorahMatch("גַּן", 53, "בראשית ב:ח",
            "וַיִּטַּע ה׳ אֱלֹהִים גַּן בְּעֵדֶן", "Genesis 2:8"),

        // 58 ─ נח = חן
        TorahMatch("נֹחַ", 58, "בראשית ו:ח",
            "וְנֹחַ מָצָא חֵן בְּעֵינֵי ה׳", "Genesis 6:8"),
        TorahMatch("חֵן", 58, "בראשית ו:ח",
            "וְנֹחַ מָצָא חֵן בְּעֵינֵי ה׳", "Genesis 6:8"),

        // 68
        TorahMatch("חַיִּים", 68, "ויקרא יח:ה",
            "אֲשֶׁר יַעֲשֶׂה אֹתָם הָאָדָם וָחַי בָּהֶם", "Leviticus 18:5"),

        // 72
        TorahMatch("חֶסֶד", 72, "שמות לד:ו",
            "ה׳ ה׳ אֵל רַחוּם וְחַנּוּן אֶרֶךְ אַפַּיִם וְרַב חֶסֶד", "Exodus 34:6"),

        // 75
        TorahMatch("כֹּהֵן", 75, "שמות כח:א",
            "וְאַתָּה הַקְרֵב אֵלֶיךָ אֶת אַהֲרֹן אָחִיךָ", "Exodus 28:1"),

        // 86 ─ אלהים = הטבע (בגימטרייה)
        TorahMatch("אֱלֹהִים", 86, "בראשית א:א",
            "בְּרֵאשִׁית בָּרָא אֱלֹהִים", "Genesis 1:1"),

        // 90 ─ מים = מלך
        TorahMatch("מַיִם", 90, "בראשית א:ב",
            "וְרוּחַ אֱלֹהִים מְרַחֶפֶת עַל פְּנֵי הַמָּיִם", "Genesis 1:2"),
        TorahMatch("מֶלֶךְ", 90, "שמות טו:יח",
            "ה׳ יִמְלֹךְ לְעֹלָם וָעֶד", "Exodus 15:18"),

        // 91 ─ אמן (יהוה + אדני)
        TorahMatch("אָמֵן", 91, "במדבר ה:כב",
            "וְאָמְרָה הָאִשָּׁה אָמֵן אָמֵן", "Numbers 5:22"),

        // 102
        TorahMatch("אֱמוּנָה", 102, "שמות יז:יב",
            "וַיְהִי יָדָיו אֱמוּנָה עַד בֹּא הַשָּׁמֶשׁ", "Exodus 17:12"),

        // 124
        TorahMatch("עֵדֶן", 124, "בראשית ב:ח",
            "וַיִּטַּע ה׳ אֱלֹהִים גַּן בְּעֵדֶן", "Genesis 2:8"),

        // 136
        TorahMatch("קוֹל", 136, "שמות יט:יט",
            "מֹשֶׁה יְדַבֵּר וְהָאֱלֹהִים יַעֲנֶנּוּ בְקוֹל", "Exodus 19:19"),

        // 141
        TorahMatch("מִצְוָה", 141, "דברים ו:כה",
            "כִּי נִשְׁמֹר לַעֲשׂוֹת אֶת כָּל הַמִּצְוָה הַזֹּאת", "Deuteronomy 6:25"),

        // 146
        TorahMatch("עוֹלָם", 146, "בראשית כא:לג",
            "וַיִּקְרָא שָׁם בְּשֵׁם ה׳ אֵל עוֹלָם", "Genesis 21:33"),

        // 148
        TorahMatch("פֶּסַח", 148, "שמות יב:יא",
            "וְכָכָה תֹּאכְלוּ אֹתוֹ פְּסַח הוּא לַה׳", "Exodus 12:11"),

        // 156 ─ יוסף = ציון
        TorahMatch("יוֹסֵף", 156, "בראשית ל:כד",
            "וַתִּקְרָא אֶת שְׁמוֹ יוֹסֵף", "Genesis 30:24"),
        TorahMatch("צִיּוֹן", 156, "דברים ד:מח",
            "עַד הַר סִיאֹן הוּא חֶרְמוֹן", "Deuteronomy 4:48"),

        // 177
        TorahMatch("גַּן עֵדֶן", 177, "בראשית ב:טו",
            "וַיַּנִּחֵהוּ בְגַן עֵדֶן לְעָבְדָהּ וּלְשָׁמְרָהּ", "Genesis 2:15"),

        // 182
        TorahMatch("יַעֲקֹב", 182, "בראשית כה:כו",
            "וְיָדוֹ אֹחֶזֶת בַּעֲקֵב עֵשָׂו", "Genesis 25:26"),

        // 194
        TorahMatch("צֶדֶק", 194, "דברים טז:כ",
            "צֶדֶק צֶדֶק תִּרְדֹּף", "Deuteronomy 16:20"),

        // 207 ─ אור = רז
        TorahMatch("אוֹר", 207, "בראשית א:ג",
            "וַיֹּאמֶר אֱלֹהִים יְהִי אוֹר וַיְהִי אוֹר", "Genesis 1:3"),
        TorahMatch("רָז", 207, "בראשית מ:ח",
            "הֲלוֹא לֵאלֹהִים פִּתְרֹנִים", "Genesis 40:8"),

        // 208
        TorahMatch("יִצְחָק", 208, "בראשית כא:ג",
            "וַיִּקְרָא אַבְרָהָם אֶת שֶׁם בְּנוֹ הַנּוֹלַד לוֹ יִצְחָק", "Genesis 21:3"),

        // 214
        TorahMatch("רוּחַ", 214, "בראשית א:ב",
            "וְרוּחַ אֱלֹהִים מְרַחֶפֶת עַל פְּנֵי הַמָּיִם", "Genesis 1:2"),

        // 227
        TorahMatch("בְּרָכָה", 227, "בראשית יב:ב",
            "וַאֲגַדְּלָה שְׁמֶךָ וֶהְיֵה בְּרָכָה", "Genesis 12:2"),

        // 246
        TorahMatch("מִדְבָּר", 246, "במדבר א:א",
            "בְּמִדְבַּר סִינַי בְּאֹהֶל מוֹעֵד", "Numbers 1:1"),

        // 248 ─ אברהם = רמח (248 מצוות עשה)
        TorahMatch("אַבְרָהָם", 248, "בראשית יב:א",
            "וַיֹּאמֶר ה׳ אֶל אַבְרָם לֶךְ לְךָ", "Genesis 12:1"),

        // 257
        TorahMatch("אָרוֹן", 257, "שמות כה:י",
            "וְעָשׂוּ אֲרוֹן עֲצֵי שִׁטִּים", "Exodus 25:10"),

        // 291
        TorahMatch("אֶרֶץ", 291, "בראשית א:א",
            "בְּרֵאשִׁית בָּרָא אֱלֹהִים אֵת הַשָּׁמַיִם וְאֵת הָאָרֶץ", "Genesis 1:1"),

        // 301 ─ מנורה = אש
        TorahMatch("מְנוֹרָה", 301, "שמות כה:לא",
            "וְעָשִׂיתָ מְנֹרַת זָהָב טָהוֹר", "Exodus 25:31"),
        TorahMatch("אֵשׁ", 301, "שמות ג:ב",
            "וַיֵּרָא מַלְאַךְ ה׳ אֵלָיו בְּלַבַּת אֵשׁ", "Exodus 3:2"),

        // 340 ─ שם = ספר
        TorahMatch("שֵׁם", 340, "בראשית יב:ב",
            "וְנַעֲשֶׂה לָּנוּ שֵׁם", "Genesis 11:4"),
        TorahMatch("סֵפֶר", 340, "שמות יז:יד",
            "כְּתֹב זֹאת זִכָּרוֹן בַּסֵּפֶר", "Exodus 17:14"),

        // 345
        TorahMatch("מֹשֶׁה", 345, "שמות ב:י",
            "וַיִּקְרָא שְׁמוֹ מֹשֶׁה", "Exodus 2:10"),

        // 355
        TorahMatch("פַּרְעֹה", 355, "שמות ה:א",
            "כֹּה אָמַר ה׳ אֱלֹהֵי יִשְׂרָאֵל שַׁלַּח אֶת עַמִּי", "Exodus 5:1"),

        // 358 ─ משיח = נחש (סוד)
        TorahMatch("נָחָשׁ", 358, "בראשית ג:א",
            "וְהַנָּחָשׁ הָיָה עָרוּם מִכֹּל חַיַּת הַשָּׂדֶה", "Genesis 3:1"),

        // 376
        TorahMatch("שָׁלוֹם", 376, "במדבר כה:יב",
            "הִנְנִי נֹתֵן לוֹ אֶת בְּרִיתִי שָׁלוֹם", "Numbers 25:12"),

        // 380
        TorahMatch("מִצְרַיִם", 380, "בראשית יב:י",
            "וַיֵּרֶד אַבְרָם מִצְרַיְמָה", "Genesis 12:10"),

        // 385
        TorahMatch("שְׁכִינָה", 385, "שמות כה:ח",
            "וְשָׁכַנְתִּי בְּתוֹכָם", "Exodus 25:8"),

        // 390
        TorahMatch("שָׁמַיִם", 390, "בראשית א:א",
            "בְּרֵאשִׁית בָּרָא אֱלֹהִים אֵת הַשָּׁמַיִם", "Genesis 1:1"),

        // 395
        TorahMatch("נְשָׁמָה", 395, "בראשית ב:ז",
            "וַיִּפַּח בְּאַפָּיו נִשְׁמַת חַיִּים", "Genesis 2:7"),

        // 410 ─ שמע = קדוש
        TorahMatch("שְׁמַע", 410, "דברים ו:ד",
            "שְׁמַע יִשְׂרָאֵל ה׳ אֱלֹהֵינוּ ה׳ אֶחָד", "Deuteronomy 6:4"),
        TorahMatch("קָדוֹשׁ", 410, "ויקרא יא:מד",
            "כִּי אֲנִי ה׳ אֱלֹהֵיכֶם וְהִתְקַדִּשְׁתֶּם וִהְיִיתֶם קְדֹשִׁים", "Leviticus 11:44"),

        // 430
        TorahMatch("נֶפֶשׁ", 430, "בראשית ב:ז",
            "וַיְהִי הָאָדָם לְנֶפֶשׁ חַיָּה", "Genesis 2:7"),

        // 441
        TorahMatch("אֱמֶת", 441, "בראשית לב:יא",
            "קָטֹנְתִּי מִכֹּל הַחֲסָדִים וּמִכָּל הָאֱמֶת", "Genesis 32:11"),

        // 541
        TorahMatch("יִשְׂרָאֵל", 541, "בראשית לב:כט",
            "כִּי שָׂרִיתָ עִם אֱלֹהִים וְעִם אֲנָשִׁים וַתּוּכָל", "Genesis 32:29"),

        // 586
        TorahMatch("שׁוֹפָר", 586, "ויקרא כה:ט",
            "וְהַעֲבַרְתָּ שׁוֹפַר תְּרוּעָה", "Leviticus 25:9"),

        // 611 ─ תורה
        TorahMatch("תּוֹרָה", 611, "דברים ד:מד",
            "וְזֹאת הַתּוֹרָה אֲשֶׁר שָׂם מֹשֶׁה לִפְנֵי בְּנֵי יִשְׂרָאֵל", "Deuteronomy 4:44"),

        // 612 ─ ברית (גם מספר המצוות של התורה = 613, ברית = 612)
        TorahMatch("בְּרִית", 612, "בראשית טו:יח",
            "בַּיּוֹם הַהוּא כָּרַת ה׳ אֶת אַבְרָם בְּרִית", "Genesis 15:18"),

        // 640
        TorahMatch("שֶׁמֶשׁ", 640, "בראשית א:טז",
            "וַיַּעַשׂ אֱלֹהִים אֶת שְׁנֵי הַמְּאֹרֹת הַגְּדֹלִים", "Genesis 1:16"),

        // 702 ─ שבת
        TorahMatch("שַׁבָּת", 702, "בראשית ב:ב",
            "וַיְכַל אֱלֹהִים בַּיּוֹם הַשְּׁבִיעִי מְלַאכְתּוֹ", "Genesis 2:2"),

        // 746
        TorahMatch("שְׁמוֹת", 746, "שמות א:א",
            "וְאֵלֶּה שְׁמוֹת בְּנֵי יִשְׂרָאֵל הַבָּאִים מִצְרָיְמָה", "Exodus 1:1"),

        // 913
        TorahMatch("בְּרֵאשִׁית", 913, "בראשית א:א",
            "בְּרֵאשִׁית בָּרָא אֱלֹהִים אֵת הַשָּׁמַיִם וְאֵת הָאָרֶץ", "Genesis 1:1")
    )
}
