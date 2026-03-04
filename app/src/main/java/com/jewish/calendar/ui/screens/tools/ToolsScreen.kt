package com.jewish.calendar.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    onNavigateToSpiritualTracking: () -> Unit,
    onNavigateToTorah: () -> Unit,
    onNavigateToMishna: () -> Unit,
    onNavigateToSynagogueMap: () -> Unit = {}
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── תפילה ──
            item(span = { GridItemSpan(2) }) { SectionHeader("תפילה") }

            item { ToolGridCard(emoji = "📖", title = "סידור תפילה",   color = MaterialTheme.colorScheme.primary, onClick = onNavigateToSiddur) }
            item { ToolGridCard(emoji = "🙏", title = "תפילות מיוחדות", color = MaterialTheme.colorScheme.tertiary, onClick = onNavigateToSpecialPrayers) }
            item { ToolGridCard(emoji = "✡",  title = "ברכות יומיות",  color = MaterialTheme.colorScheme.secondary, onClick = onNavigateToBlessings) }
            item { ToolGridCard(emoji = "📜", title = "תיקון הכללי",   color = MaterialTheme.colorScheme.primary, onClick = onNavigateToTikkun) }

            // ── לימוד תורה ──
            item(span = { GridItemSpan(2) }) { SectionHeader("לימוד תורה") }

            item { ToolGridCard(emoji = "🕍", title = "תּוֹרָה",      color = MaterialTheme.colorScheme.primary, onClick = onNavigateToTorah) }
            item { ToolGridCard(emoji = "📚", title = "מִשְׁנָה",     color = MaterialTheme.colorScheme.secondary, onClick = onNavigateToMishna) }
            item { ToolGridCard(emoji = "📜", title = "תהילים",       color = MaterialTheme.colorScheme.primary, onClick = onNavigateToPsalms) }
            item { ToolGridCard(emoji = "🌾", title = "ספירת העומר",  color = MaterialTheme.colorScheme.tertiary, onClick = onNavigateToOmer) }
            item { ToolGridCard(emoji = "🔢", title = "גימטרייה",     color = MaterialTheme.colorScheme.secondary, onClick = onNavigateToGematria) }

            // ── חיזוק רוחני ──
            item(span = { GridItemSpan(2) }) { SectionHeader("חיזוק רוחני") }

            item { ToolGridCard(emoji = "⭐", title = "חיזוק יומי",    color = MaterialTheme.colorScheme.secondary, onClick = onNavigateToDailyInspiration) }
            item { ToolGridCard(emoji = "🌸", title = "יומן הודיה",    color = MaterialTheme.colorScheme.tertiary, onClick = onNavigateToGratitude) }
            item { ToolGridCard(emoji = "💑", title = "שלום בית",      color = MaterialTheme.colorScheme.primary, onClick = onNavigateToShalomBayit) }
            item { ToolGridCard(emoji = "📊", title = "מעקב רוחני",    color = MaterialTheme.colorScheme.secondary, onClick = onNavigateToSpiritualTracking) }

            // ── כלים ──
            item(span = { GridItemSpan(2) }) { SectionHeader("כלים") }

            item { ToolGridCard(emoji = "✡",  title = "מצפן ירושלים", color = MaterialTheme.colorScheme.primary, onClick = onNavigateToCompass) }
            item { ToolGridCard(emoji = "🕍", title = "הכותל המערבי", color = MaterialTheme.colorScheme.secondary, onClick = onNavigateToKotel) }
            item { ToolGridCard(emoji = "🗺️", title = "בתי כנסת",     color = MaterialTheme.colorScheme.primary, onClick = onNavigateToSynagogueMap) }
            item { ToolGridCard(emoji = "🫓", title = "הפרשת חלה",    color = MaterialTheme.colorScheme.tertiary, onClick = onNavigateToChalla) }

            item(span = { GridItemSpan(2) }) { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.width(8.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolGridCard(
    emoji: String,
    title: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 28.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}
