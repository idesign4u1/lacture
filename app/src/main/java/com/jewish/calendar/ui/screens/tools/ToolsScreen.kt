package com.jewish.calendar.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val israeliBlue  = Color(0xFF003E7E)
private val templeGold   = Color(0xFFD4AF37)
private val sectionColor = Color(0xFF5C6370)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    onNavigateToCompass: () -> Unit,
    onNavigateToKotel: () -> Unit,
    onNavigateToSiddur: () -> Unit,
    onNavigateToTikkun: () -> Unit,
    onNavigateToGematria: () -> Unit,
    onNavigateToOmer: () -> Unit,
    onNavigateToPsalms: () -> Unit,
    onNavigateToDailyInspiration: () -> Unit,
    onNavigateToGratitude: () -> Unit,
    onNavigateToShalomBayit: () -> Unit,
    onNavigateToSpecialPrayers: () -> Unit,
    onNavigateToBlessings: () -> Unit,
    onNavigateToChalla: () -> Unit,
    onNavigateToSpiritualTracking: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "כלים נוספים",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── תפילה ולימוד ──
            SectionHeader("תפילה ולימוד")

            ToolCard(
                emoji = "📖",
                title = "סידור תפילה",
                description = "תפילות שחרית, מנחה וערבית עם זיהוי חכם לפי השעה",
                accentColor = Color(0xFF6A1B9A),
                onClick = onNavigateToSiddur
            )
            ToolCard(
                emoji = "📜",
                title = "תהילים",
                description = "כל 150 מזמורי תהילים, מאורגנים בחמישה ספרים",
                accentColor = Color(0xFF4527A0),
                onClick = onNavigateToPsalms
            )
            ToolCard(
                emoji = "🙏",
                title = "תפילות מיוחדות",
                description = "תפילות לרפואה, פרנסה, שידוך ועוד — לכל עת ושעה",
                accentColor = Color(0xFF1A237E),
                onClick = onNavigateToSpecialPrayers
            )
            ToolCard(
                emoji = "✡",
                title = "ברכות יומיות",
                description = "ברכות מותאמות לשעת היום, עם ברכה מלאה לכל מאכל",
                accentColor = Color(0xFF1565C0),
                onClick = onNavigateToBlessings
            )

            Spacer(Modifier.height(4.dp))

            // ── מצוות ומסורת ──
            SectionHeader("מצוות ומסורת")

            ToolCard(
                emoji = "🌾",
                title = "ספירת העומר",
                description = "ספירה יומית עם שמירת רצף, מידות הספירה ונוסח הברכה",
                accentColor = Color(0xFF004D40),
                onClick = onNavigateToOmer
            )
            ToolCard(
                emoji = "📜",
                title = "תיקון הכללי",
                description = "עשרת המזמורים של רבי נחמן מברסלב עם מעקב התקדמות",
                accentColor = Color(0xFF4A148C),
                onClick = onNavigateToTikkun
            )
            ToolCard(
                emoji = "🫓",
                title = "הפרשת חלה",
                description = "מדריך שלב אחר שלב לקיום מצוות הפרשת חלה, כולל מתכונים",
                accentColor = Color(0xFF6D4C41),
                onClick = onNavigateToChalla
            )
            ToolCard(
                emoji = "🔢",
                title = "מחשבון גימטרייה",
                description = "חשב גימטרייה וחפש מילים בעלות אותו ערך בתורה",
                accentColor = Color(0xFF1565C0),
                onClick = onNavigateToGematria
            )

            Spacer(Modifier.height(4.dp))

            // ── חיזוק אישי ──
            SectionHeader("חיזוק אישי")

            ToolCard(
                emoji = "⭐",
                title = "חיזוק יומי",
                description = "פסוק ומחשבה לחיזוק הנפש — מתחלף מדי יום",
                accentColor = Color(0xFF8D6E00),
                onClick = onNavigateToDailyInspiration
            )
            ToolCard(
                emoji = "🌸",
                title = "יומן הודיה",
                description = "רשום את רגעי ההכרת הטובה שלך — הכרת טובה משנה חיים",
                accentColor = Color(0xFFE65100),
                onClick = onNavigateToGratitude
            )
            ToolCard(
                emoji = "💑",
                title = "שלום בית",
                description = "טיפים וחיזוקים לקשר זוגי מבורך — בניית הבית היהודי",
                accentColor = Color(0xFFE91E63),
                onClick = onNavigateToShalomBayit
            )
            ToolCard(
                emoji = "📊",
                title = "מעקב רוחני",
                description = "עקוב אחר תפילה, לימוד תורה, חסד ותהילים — בנה שגרה רוחנית",
                accentColor = Color(0xFF1A237E),
                onClick = onNavigateToSpiritualTracking
            )

            Spacer(Modifier.height(4.dp))

            // ── כלים ─
            SectionHeader("כלים")

            ToolCard(
                emoji = "✡",
                title = "מצפן ירושלים",
                description = "כיוון התפילה עם רטט בהגעה לכיוון הנכון",
                accentColor = israeliBlue,
                onClick = onNavigateToCompass
            )
            ToolCard(
                emoji = "🕍",
                title = "הכותל המערבי",
                description = "צפייה בשידור חי מהכותל המערבי",
                accentColor = templeGold,
                onClick = onNavigateToKotel
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = sectionColor,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolCard(
    emoji: String,
    title: String,
    description: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(accentColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 26.sp)
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.5f)
            )
        }
    }
}
