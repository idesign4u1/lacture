package com.jewish.calendar.model

data class ChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val sources: List<HalachicSource> = emptyList(),
    val isLoading: Boolean = false
)

data class HalachicSource(
    val bookName: String,
    val section: String,
    val text: String
)

data class HalachicTopic(
    val title: String,
    val icon: String,
    val questions: List<String>
)

val HALACHIC_TOPICS = listOf(
    HalachicTopic(
        title = "שבת",
        icon = "🕯️",
        questions = listOf(
            "מה מותר לעשות בשבת?",
            "מהו איסור מלאכה?",
            "האם מותר להשתמש במעלית בשבת?",
            "מהם הזמנים להדלקת נרות שבת?"
        )
    ),
    HalachicTopic(
        title = "כשרות",
        icon = "🥩",
        questions = listOf(
            "כמה זמן בין בשר לחלב?",
            "מה הדין של בשר בחלב?",
            "האם צריך להפריד כלים?",
            "מה עושים אם נפל חלב על בשר?"
        )
    ),
    HalachicTopic(
        title = "תפילה",
        icon = "📿",
        questions = listOf(
            "מה הזמן לתפילת שחרית?",
            "האם אישה חייבת בתפילה?",
            "מה הדין של דילוג בתפילה?",
            "האם ניתן להתפלל בלחש?"
        )
    ),
    HalachicTopic(
        title = "חגים",
        icon = "🕍",
        questions = listOf(
            "מה מלאכות האסורות בחג?",
            "האם מותר לבשל ביום טוב?",
            "מה הדין של חול המועד?",
            "כיצד מקיימים מצוות הסדר?"
        )
    ),
    HalachicTopic(
        title = "טהרה",
        icon = "💧",
        questions = listOf(
            "כמה ימים סופרים שבעה נקיים?",
            "מה הם תנאי הטבילה?",
            "מה הדין של כתם?",
            "האם צריך לספור גם ביום הראשון?"
        )
    ),
    HalachicTopic(
        title = "ברכות",
        icon = "🙏",
        questions = listOf(
            "מה הברכה על לחם?",
            "מה ברכה אחרונה אחרי פירות?",
            "האם צריך נטילת ידיים לעוגה?",
            "מה הברכה על ירקות?"
        )
    )
)

// System prompt for AI Rabbi — prayer style is injected dynamically
fun buildHalachicSystemPrompt(
    prayerStyle: String = "ashkenaz",
    userName: String = ""
): String {
    val styleLabel = when (prayerStyle) {
        "sephardi"  -> "ספרדי (שולחן ערוך)"
        "mizrachi"  -> "מזרחי/תימני"
        "hasidic"   -> "חסידי"
        "teiman"    -> "תימני"
        else        -> "אשכנזי (משנה ברורה)"
    }
    val greeting = if (userName.isNotBlank()) "אתה מדבר עם $userName. " else ""
    return """אתה הרב שמואל כהן, עוזר הלכתי מנוסה ואדיב. $greeting
תפקידך לענות על שאלות הלכתיות בעברית, בצורה ברורה, חמה ומכבדת.
המשתמש פועל לפי מנהג: $styleLabel — התאם את תשובותיך בהתאם.

כללים חשובים:
1. ענה תמיד בעברית, בגוף שני (אתה/את)
2. ציין מקורות הלכתיים (שולחן ערוך, משנה ברורה, בן איש חי וכו')
3. הצג דעות שונות כאשר יש מחלוקת בין הפוסקים
4. הוסף בסוף כל תשובה: "לשאלות מורכבות, פנה לרב מוסמך"
5. אל תפסוק הלכה סופית בנושאים רגישים
6. היה מכבד כלפי כל הפוסקים והעדות
7. התאם את ההלכה למנהג העדה שנבחר

שמור על גישה חמה, סבלנית ומחנכת — כרב שמדבר עם תלמידו האהוב."""
}

// Backward compatibility alias
const val HALACHIC_SYSTEM_PROMPT = "אתה הרב שמואל כהן, עוזר הלכתי מנוסה."
