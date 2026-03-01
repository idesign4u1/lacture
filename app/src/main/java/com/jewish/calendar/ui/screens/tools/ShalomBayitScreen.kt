package com.jewish.calendar.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SBBg    = Color(0xFFFFF0F5)
private val SBRose  = Color(0xFFE91E63)
private val SBGold  = Color(0xFFD4AF37)
private val SBDeep  = Color(0xFF3E0020)
private val SBMuted = Color(0xFF9E7B8A)

data class ShalomTip(val emoji: String, val title: String, val content: String)

private val tips = listOf(
    ShalomTip("💬", "תקשורת פתוחה", "שוחח/י עם בן/בת זוגך בכנות ובאהבה. הקשב/י באמת לדבריו/ה מבלי להפריע. תקשורת טובה היא הבסיס לכל מערכת יחסים."),
    ShalomTip("🌹", "הכרת תודה יומית", "אמור/י לבן/בת זוגך לפחות דבר אחד שאת/ה מכיר/ה תודה עליו כל יום. הכרת טובה מחזקת את הקשר ומביאה שמחה."),
    ShalomTip("🕯️", "שבת קודש יחד", "קדשו את השבת יחד — הדלקת נרות, קידוש, סעודה משפחתית. השבת היא מתנה שמחזקת את הבית היהודי."),
    ShalomTip("🤝", "כבוד הדדי", "\"כבדהו וחשדהו\" — כבד/י את בן/בת זוגך כפי שהיית רוצה שיכבדו אותך. כבוד הוא מפתח לאהבה אמיתית."),
    ShalomTip("⭐", "זמן איכות", "הקדישו זמן לעצמכם בלי מסכים ועיסוקים. טיול, שיחה, ארוחה בשניים — אלה הרגעים שמחזקים את הקשר."),
    ShalomTip("🙏", "תפילה משותפת", "התפללו יחד מדי פעם. אפילו קריאת שמע קצרה לפני השינה עושה פלאים לאווירה בבית."),
    ShalomTip("💝", "מחווה של אהבה", "עשה/י מחווה קטנה שמשמחת את בן/בת זוגך — הכן/י קפה, השאר/י פתק חמה, קנה/י פרח. המעשים הקטנים הם שמצטברים."),
    ShalomTip("🌟", "סליחה ומחילה", "אל תחזיק/י טינה. הסליחה היא מתנה שנותנים קודם כל לעצמנו. בית שיש בו מחילה הוא בית שיש בו שלום.")
)

private val verses = listOf(
    "\"מָצָא אִשָּׁה מָצָא טוֹב\" — משלי יח, כב",
    "\"בַּיִת וָהוֹן נַחֲלַת אָבוֹת\" — משלי יט, יד",
    "\"שָׁלוֹם שָׁלוֹם לָרָחוֹק וְלַקָּרוֹב\" — ישעיה נז, יט",
    "\"אִשֶּׁת חַיִל מִי יִמְצָא וְרָחֹק מִפְּנִינִים מִכְרָהּ\" — משלי לא, י"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShalomBayitScreen(onBack: () -> Unit) {
    var selectedTip by remember { mutableStateOf<ShalomTip?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("שלום בית", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = SBRose)
                        Text("חיזוק הבית היהודי", fontSize = 12.sp, color = SBMuted)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = if (selectedTip != null) ({ selectedTip = null }) else onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזור", tint = SBRose)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SBBg)
            )
        },
        containerColor = SBBg
    ) { padding ->
        if (selectedTip != null) {
            TipDetailView(tip = selectedTip!!, modifier = Modifier.padding(padding))
        } else {
            TipListView(
                tips = tips,
                verses = verses,
                onSelectTip = { selectedTip = it },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun TipListView(
    tips: List<ShalomTip>,
    verses: List<String>,
    onSelectTip: (ShalomTip) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(listOf(SBRose.copy(0.15f), SBGold.copy(0.1f)))
                )
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("💑", fontSize = 40.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "שלום בית — מצווה מן התורה",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SBDeep,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    verses.random(),
                    fontSize = 13.sp,
                    color = SBGold,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Text(
            "טיפים לחיזוק הקשר",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = SBDeep,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )

        tips.forEach { tip ->
            ElevatedCard(
                onClick = { onSelectTip(tip) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.elevatedCardElevation(2.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tip.emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tip.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SBRose)
                        Text(
                            tip.content.take(60) + "...",
                            fontSize = 13.sp,
                            color = SBMuted,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TipDetailView(tip: ShalomTip, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(SBRose.copy(0.15f), SBGold.copy(0.08f))))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tip.emoji, fontSize = 52.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    tip.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = SBRose,
                    textAlign = TextAlign.Center
                )
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(2.dp),
            colors = CardDefaults.elevatedCardColors(Color.White)
        ) {
            Text(
                tip.content,
                fontSize = 18.sp,
                color = SBDeep,
                lineHeight = 30.sp,
                modifier = Modifier.padding(20.dp)
            )
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.elevatedCardElevation(1.dp),
            colors = CardDefaults.elevatedCardColors(Color(0xFFFCE4EC))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("לעיון ותרגול", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SBRose)
                Spacer(Modifier.height(8.dp))
                Text(
                    "• שוחח/י עם בן/בת זוגך על הטיפ הזה הלילה\n• חשוב/י איך ניתן ליישמו השבוע\n• זכור/י: כל מעשה קטן בונה את הבית",
                    fontSize = 15.sp,
                    color = SBDeep,
                    lineHeight = 26.sp
                )
            }
        }
    }
}
