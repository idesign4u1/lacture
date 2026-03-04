package com.jewish.calendar.model

/**
 * Configuration for a single tool card in the Tools screen.
 * Admin can toggle visibility and (optionally) rename tools.
 */
data class ToolConfig(
    val id: String,               // stable identifier (= route name)
    val emoji: String,
    val titleHe: String,          // Hebrew display title
    val categoryHe: String,       // Hebrew category label
    val isVisible: Boolean = true
)

/**
 * The full admin configuration, persisted to DataStore as JSON.
 */
data class AdminConfig(
    /** PIN to enter the admin area (4-digit string). Default is "1234". */
    val pin: String = "1234",

    /** Set of tool IDs that are hidden from the Tools screen. */
    val hiddenToolIds: Set<String> = emptySet(),

    /**
     * Custom ARGB color as Long for the app primary color.
     * null = use default theme color.
     */
    val customPrimaryColor: Long? = null,

    /**
     * Admin-managed daily inspiration messages (Hebrew).
     * If non-empty, these override the built-in list in DailyInspirationScreen.
     */
    val customDailyMessages: List<String> = emptyList(),

    /** Custom welcome text shown in the Halachic Bot screen. */
    val botWelcomeText: String = ""
)

/** The complete list of tools, in display order, with their defaults. */
val DEFAULT_TOOLS: List<ToolConfig> = listOf(
    // תפילה
    ToolConfig("siddur",           "📖", "סידור תפילה",    "תפילה"),
    ToolConfig("special_prayers",  "🙏", "תפילות מיוחדות", "תפילה"),
    ToolConfig("blessings",        "✡",  "ברכות יומיות",   "תפילה"),
    ToolConfig("tikkun",           "📜", "תיקון הכללי",    "תפילה"),
    // לימוד תורה
    ToolConfig("torah",            "🕍", "תּוֹרָה",         "לימוד תורה"),
    ToolConfig("mishna",           "📚", "מִשְׁנָה",        "לימוד תורה"),
    ToolConfig("psalms",           "📜", "תהילים",          "לימוד תורה"),
    ToolConfig("omer",             "🌾", "ספירת העומר",     "לימוד תורה"),
    ToolConfig("gematria",         "🔢", "גימטרייה",        "לימוד תורה"),
    // חיזוק רוחני
    ToolConfig("daily_inspiration","⭐", "חיזוק יומי",      "חיזוק רוחני"),
    ToolConfig("gratitude",        "🌸", "יומן הודיה",      "חיזוק רוחני"),
    ToolConfig("shalom_bayit",     "💑", "שלום בית",        "חיזוק רוחני"),
    ToolConfig("spiritual_tracking","📊","מעקב רוחני",      "חיזוק רוחני"),
    // כלים
    ToolConfig("compass",          "✡",  "מצפן ירושלים",   "כלים"),
    ToolConfig("kotel",            "🕍", "הכותל המערבי",    "כלים"),
    ToolConfig("synagogue_map",    "🗺️", "בתי כנסת",        "כלים"),
    ToolConfig("challa",           "🫓", "הפרשת חלה",       "כלים")
)
