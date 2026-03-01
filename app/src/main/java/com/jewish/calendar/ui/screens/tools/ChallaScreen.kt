package com.jewish.calendar.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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

private val ChBg    = Color(0xFFFFF8E1)
private val ChBrown = Color(0xFF6D4C41)
private val ChGold  = Color(0xFFD4AF37)
private val ChWarm  = Color(0xFFFF8F00)
private val ChMuted = Color(0xFF8D6E63)
private val ChGreen = Color(0xFF2E7D32)

data class ChallaStep(val title: String, val description: String, val blessing: String? = null)

private val steps = listOf(
    ChallaStep(
        "שיעור הפרשה",
        "• פחות מ-1.666 ק\"ג קמח: אין חובה להפריש\n• 1.666 – 2.25 ק\"ג: מפרישים אך לא מברכים\n• יותר מ-2.25 ק\"ג: מפרישים ומברכים\n\n(שיעור מינימלי לברכה: כ-43.2 ביצים = כ-1.2 ק\"ג קמח בפועל)\n\nאם יש ספק — הפרישי בלי ברכה",
        null
    ),
    ChallaStep(
        "הברכה",
        "לאחר לישת הבצק, לפני הפרשת החלה, אמרי את הברכה:",
        "בָּרוּךְ אַתָּה ה׳ אֱלֹהֵינוּ מֶלֶךְ הָעוֹלָם אֲשֶׁר קִדְּשָׁנוּ בְּמִצְוֹתָיו וְצִוָּנוּ לְהַפְרִישׁ חַלָּה"
    ),
    ChallaStep(
        "הפרישה",
        "קחי חתיכה קטנה מהבצק — כגודל זית (כ-28 גרם).\n\nאמרי: \"הרי זה חלה\"\n\nאם ירד בלישה יותר ממה שנדרש, קחי פחות — אבל לפחות כגודל זית.",
        null
    ),
    ChallaStep(
        "שריפת החלה",
        "• בזמן הבית: החלה ניתנה לכהן\n• בזמן הזה: מכסים בנייר כסף ושורפים בתנור (בעצמאות מהבישול)\n• אם אין אפשרות לשרוף: עוטפים היטב בשקית ושמים בפח — לא יאכל אותה אדם ובהמה",
        null
    )
)

private val recipes = listOf(
    Triple("🍞", "חלה שבת קלאסית", "1 ק\"ג קמח, 2 ביצים, 1/2 כוס שמן, 1/2 כוס סוכר, 1 כפית מלח, 25 גר' שמרים, 300 מ\"ל מים חמימים"),
    Triple("🌾", "חלה מחיטה מלאה", "500 גר' קמח לבן, 500 גר' קמח מלא, 3 ביצים, 1/4 כוס דבש, 1/3 כוס שמן, 1 כפית מלח, 25 גר' שמרים"),
    Triple("✨", "חלה ספוגית עם שמרים מהירים", "1 ק\"ג קמח, 2 ביצים, 1/2 כוס שמן, 3/4 כוס סוכר, 11 גר' שמרים יבשים, 1/2 כפית מלח, 350 מ\"ל מים חמים")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallaScreen(onBack: () -> Unit) {
    var completedSteps by remember { mutableStateOf(setOf<Int>()) }
    var showRecipes by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("הפרשת חלה", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = ChBrown)
                        Text("מצווה מן התורה", fontSize = 12.sp, color = ChMuted)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזור", tint = ChBrown)
                    }
                },
                actions = {
                    TextButton(onClick = { showRecipes = !showRecipes }) {
                        Text(if (showRecipes) "מדריך" else "מתכונים", color = ChGold, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ChBg)
            )
        },
        containerColor = ChBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(ChGold.copy(0.2f), ChWarm.copy(0.15f))))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🫓", fontSize = 44.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "רֵאשִׁית עֲרִיסֹתֵיכֶם חַלָּה תָּרִימוּ תְרוּמָה",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChBrown,
                        textAlign = TextAlign.Center
                    )
                    Text("במדבר טו, כ", fontSize = 12.sp, color = ChMuted)
                }
            }

            if (!showRecipes) {
                // Progress
                val doneCount = completedSteps.size
                LinearProgressIndicator(
                    progress = { doneCount.toFloat() / steps.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = if (doneCount == steps.size) ChGreen else ChWarm,
                    trackColor = ChWarm.copy(0.15f)
                )
                Text(
                    "$doneCount / ${steps.size} שלבים הושלמו",
                    fontSize = 13.sp,
                    color = ChMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Steps
                steps.forEachIndexed { index, step ->
                    val isDone = index in completedSteps
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.elevatedCardElevation(if (isDone) 0.dp else 3.dp),
                        colors = CardDefaults.elevatedCardColors(if (isDone) Color(0xFFEDF7ED) else Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isDone) ChGreen.copy(0.2f) else ChWarm.copy(0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isDone) {
                                            Icon(Icons.Default.Check, null, tint = ChGreen, modifier = Modifier.size(18.dp))
                                        } else {
                                            Text("${index + 1}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ChWarm)
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        step.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDone) ChGreen else ChBrown
                                    )
                                }
                                Checkbox(
                                    checked = isDone,
                                    onCheckedChange = { checked ->
                                        completedSteps = if (checked) completedSteps + index else completedSteps - index
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = ChGreen)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(step.description, fontSize = 14.sp, color = ChBrown, lineHeight = 22.sp)
                            if (step.blessing != null) {
                                Spacer(Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ChGold.copy(0.12f))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        step.blessing,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ChBrown,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 26.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                if (completedSteps.size == steps.size) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ChGreen.copy(0.15f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "כל הכבוד! 🎉\nקיימת את מצוות הפרשת חלה\nהחלות שלך מבורכות!",
                            fontSize = 16.sp,
                            color = ChGreen,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 26.sp
                        )
                    }
                }
            } else {
                // Recipes
                Text("מתכוני חלה", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ChBrown)
                recipes.forEach { (emoji, name, ingredients) ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.elevatedCardElevation(2.dp),
                        colors = CardDefaults.elevatedCardColors(Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(emoji, fontSize = 24.sp)
                                Spacer(Modifier.width(10.dp))
                                Text(name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ChBrown)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("מרכיבים:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ChMuted)
                            Text(ingredients, fontSize = 14.sp, color = ChBrown, lineHeight = 22.sp)
                        }
                    }
                }
            }
        }
    }
}
