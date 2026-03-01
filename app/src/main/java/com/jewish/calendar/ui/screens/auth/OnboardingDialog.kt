package com.jewish.calendar.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jewish.calendar.viewmodel.OnboardingUiState

private val OBGold  = Color(0xFFD4AF37)
private val OBBlue  = Color(0xFF1565C0)
private val OBDeep  = Color(0xFF0D1B4E)
private val OBLight = Color(0xFFF0F4FF)

@Composable
fun OnboardingDialog(
    uiState: OnboardingUiState,
    onSelectGender: (String) -> Unit,
    onSelectPrayerStyle: (String) -> Unit,
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    if (!uiState.showOnboarding) return

    Dialog(
        onDismissRequest = { /* non-dismissible */ },
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(OBBlue, OBDeep)))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✡️", fontSize = 32.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "ברוך הבא!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "נשמח להכיר אותך",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Step indicator
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(2) { i ->
                        Box(
                            modifier = Modifier
                                .size(if (uiState.step == i + 1) 10.dp else 8.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(if (uiState.step == i + 1) OBBlue else OBBlue.copy(alpha = 0.3f))
                        )
                        if (i == 0) Spacer(Modifier.width(6.dp))
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Animated step content
                AnimatedContent(
                    targetState = uiState.step,
                    transitionSpec = { slideInHorizontally { it } togetherWith slideOutHorizontally { -it } },
                    label = "onboarding_step"
                ) { step ->
                    when (step) {
                        1 -> GenderStep(
                            selected = uiState.gender,
                            onSelect = onSelectGender
                        )
                        2 -> PrayerStyleStep(
                            selected = uiState.prayerStyle,
                            onSelect = onSelectPrayerStyle,
                            onComplete = onComplete,
                            isCompleting = uiState.isCompleting
                        )
                        else -> Unit
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Skip link
                TextButton(onClick = onSkip) {
                    Text("דלג", color = OBBlue.copy(alpha = 0.6f), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun GenderStep(selected: String, onSelect: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "מה המין שלך?",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OBDeep,
            textAlign = TextAlign.Center
        )
        Text(
            "זה יעזור לנו להתאים את האפליקציה עבורך",
            fontSize = 13.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("female" to "אישה", "male" to "גבר").forEach { (value, label) ->
                val isSelected = selected == value
                val icon = if (value == "female") Icons.Default.Female else Icons.Default.Male
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) OBBlue else OBLight)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) OBBlue else Color.LightGray,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelect(value) }
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            icon, null,
                            modifier = Modifier.size(36.dp),
                            tint = if (isSelected) Color.White else OBBlue
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isSelected) Color.White else OBDeep
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerStyleStep(
    selected: String,
    onSelect: (String) -> Unit,
    onComplete: () -> Unit,
    isCompleting: Boolean
) {
    val styles = listOf(
        Triple("ashkenaz", "אשכנז", "משנה ברורה"),
        Triple("sephardi", "ספרד", "שולחן ערוך"),
        Triple("mizrachi", "מזרחי", "בן איש חי"),
        Triple("hasidic",  "חסידי", "מנהג חסידים")
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "מה סגנון התפילה שלך?",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OBDeep,
            textAlign = TextAlign.Center
        )
        Text(
            "ישפיע על תשובות הרב וסידור התפילה",
            fontSize = 13.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            styles.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { (value, label, subtitle) ->
                        val isSelected = selected == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) OBBlue else OBLight)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) OBBlue else Color.LightGray,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelect(value) }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isSelected) Color.White else OBDeep
                                )
                                Text(
                                    subtitle,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray
                                )
                            }
                        }
                    }
                    // Fill empty slot if odd number
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onComplete,
            enabled = !isCompleting,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OBGold)
        ) {
            if (isCompleting) {
                CircularProgressIndicator(
                    Modifier.size(20.dp), strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text("התחל!", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }
        }
    }
}
