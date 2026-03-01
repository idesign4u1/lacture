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
import androidx.hilt.navigation.compose.hiltViewModel
import com.jewish.calendar.viewmodel.OmerViewModel

private val OmerBg      = Color(0xFF0D1B4E)
private val OmerBlue    = Color(0xFF1565C0)
private val OmerGold    = Color(0xFFD4AF37)
private val OmerWhite   = Color(0xFFF0F4FF)
private val OmerGreen   = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmerScreen(
    onBack: () -> Unit,
    viewModel: OmerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "ספירת העומר",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = OmerGold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזור", tint = OmerGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OmerBg)
            )
        },
        containerColor = OmerBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (state.omerDay == null) {
                // Not omer season
                Spacer(Modifier.height(60.dp))
                Text("✡", fontSize = 60.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Text(
                    "ספירת העומר אינה בתוקף כעת",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OmerWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "הספירה מתחילה בליל ט\"ז ניסן\n(מוצאי הסדר) ונמשכת 49 יום",
                    fontSize = 15.sp,
                    color = OmerWhite.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 24.sp
                )
            } else {
                val day = state.omerDay!!

                // Day circle
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(OmerGold.copy(0.3f), OmerBlue.copy(0.1f)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$day",
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            color = OmerGold
                        )
                        Text("יום", fontSize = 14.sp, color = OmerWhite.copy(0.8f))
                    }
                }

                // Omer text
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(0.08f))
                        .padding(18.dp)
                ) {
                    Text(
                        state.omerText,
                        fontSize = 20.sp,
                        color = OmerWhite,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = 32.sp
                    )
                }

                // Sefirot
                if (state.sefirot.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(OmerGold.copy(0.12f))
                            .padding(14.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("מידת היום", fontSize = 12.sp, color = OmerGold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                state.sefirot,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = OmerWhite,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Streak
                if (state.streak > 1) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🔥", fontSize = 20.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "רצף: ${state.streak} ימים",
                            fontSize = 16.sp,
                            color = OmerGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Confirm button
                if (state.confirmedToday) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(OmerGreen.copy(0.2f))
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.Check, null, tint = OmerGreen, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "ספרת היום ✓",
                            color = OmerGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                } else {
                    Button(
                        onClick = { viewModel.confirmCounted() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OmerGold),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            "ספרתי את העומר ✓",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }

                // Nusach toggle
                OutlinedButton(
                    onClick = { viewModel.toggleNusach() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OmerWhite)
                ) {
                    Text(
                        if (state.showNusach) "הסתר נוסח" else "הצג נוסח הספירה",
                        fontSize = 14.sp
                    )
                }

                if (state.showNusach) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(0.06f))
                            .padding(16.dp)
                    ) {
                        Text(
                            "לְשֵׁם יִחוּד קֻדְשָׁא בְּרִיךְ הוּא וּשְׁכִינְתֵּהּ...\n\nהִנְנִי מוּכָן וּמְזֻמָּן לְקַיֵּם מִצְוַת עֲשֵׂה שֶׁל סְפִירַת הָעֹמֶר כְּמוֹ שֶׁכָּתוּב בַּתּוֹרָה:\n\"וּסְפַרְתֶּם לָכֶם מִמָּחֳרַת הַשַּׁבָּת... שֶׁבַע שַׁבָּתוֹת תְּמִימֹת תִּהְיֶינָה\"\n\nבָּרוּךְ אַתָּה ה׳ אֱלֹהֵינוּ מֶלֶךְ הָעוֹלָם אֲשֶׁר קִדְּשָׁנוּ בְּמִצְוֹתָיו וְצִוָּנוּ עַל סְפִירַת הָעֹמֶר",
                            fontSize = 16.sp,
                            color = OmerWhite,
                            lineHeight = 28.sp,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}
