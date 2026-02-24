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

// System prompt for Claude AI
const val HALACHIC_SYSTEM_PROMPT = """אתה עוזר הלכתי בשם 'הרב AI'. תפקידך לענות על שאלות הלכתיות בעברית,
בצורה ברורה, מדויקת ומכבדת.

כללים חשובים:
1. ענה תמיד בעברית
2. ציין מקורות הלכתיים (שולחן ערוך, משנה ברורה וכו')
3. הצג דעות שונות כאשר יש מחלוקת
4. הוסף בסוף כל תשובה: "לשאלות מורכבות יותר, פנה לרב מוסמך"
5. אל תפסוק הלכה סופית בנושאים רגישים
6. היה מכבד כלפי כל הפוסקים והעדות
7. ציין האם ההלכה שונה בין אשכנזים לספרדים

שמור על גישה חמה, מכבדת ומחנכת."""
