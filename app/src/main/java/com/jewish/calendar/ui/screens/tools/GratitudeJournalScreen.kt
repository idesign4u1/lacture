package com.jewish.calendar.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import com.jewish.calendar.data.GratitudeEntry
import com.jewish.calendar.viewmodel.GratitudeViewModel

private val GratBg     = Color(0xFFFFF8F0)
private val GratOrange = Color(0xFFE65100)
private val GratGold   = Color(0xFFD4AF37)
private val GratDeep   = Color(0xFF3E2723)
private val GratMuted  = Color(0xFF8D6E63)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GratitudeJournalScreen(
    onBack: () -> Unit,
    viewModel: GratitudeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "יומן הודיה",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = GratOrange
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזור", tint = GratOrange)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showDialog() }) {
                        Icon(Icons.Default.Add, "הוסף", tint = GratGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GratBg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showDialog() },
                containerColor = GratOrange
            ) {
                Icon(Icons.Default.Add, "הוסף הודיה", tint = Color.White)
            }
        },
        containerColor = GratBg
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GratOrange)
            }
        } else if (state.entries.isEmpty()) {
            EmptyGratitudeState(
                onAdd = { viewModel.showDialog() },
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    GratitudeHeader(count = state.entries.size)
                    Spacer(Modifier.height(4.dp))
                }
                items(state.entries) { entry ->
                    GratitudeCard(
                        entry = entry,
                        dateStr = viewModel.formatDate(entry.dateMs),
                        onDelete = { viewModel.deleteEntry(entry) }
                    )
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (state.showAddDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialog() },
            title = {
                Text(
                    "הודיה חדשה",
                    fontWeight = FontWeight.Bold,
                    color = GratOrange,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column {
                    Text(
                        "על מה אתה/את אסיר/ת תודה היום?",
                        fontSize = 14.sp,
                        color = GratMuted,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = state.inputText,
                        onValueChange = { viewModel.onInputChange(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        placeholder = { Text("כתוב/י כאן...", color = GratMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GratOrange,
                            cursorColor = GratOrange
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.saveEntry() },
                    colors = ButtonDefaults.buttonColors(containerColor = GratOrange),
                    enabled = state.inputText.isNotBlank()
                ) {
                    Text("שמור")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialog() }) {
                    Text("ביטול", color = GratMuted)
                }
            },
            containerColor = GratBg
        )
    }
}

@Composable
private fun GratitudeHeader(count: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(GratOrange.copy(0.12f), GratGold.copy(0.1f))
                )
            )
            .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("🌟", fontSize = 32.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "יש לך $count רגעי הודיה",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = GratOrange,
                textAlign = TextAlign.Center
            )
            Text(
                "הכרת טובה היא מפתח לאושר",
                fontSize = 13.sp,
                color = GratMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GratitudeCard(
    entry: GratitudeEntry,
    dateStr: String,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text("✨", fontSize = 22.sp, modifier = Modifier.padding(top = 2.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.content,
                    fontSize = 16.sp,
                    color = GratDeep,
                    lineHeight = 24.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    dateStr,
                    fontSize = 12.sp,
                    color = GratMuted
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    "מחק",
                    tint = GratMuted.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyGratitudeState(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🌸", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "יומן ההודיה שלך ריק",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = GratOrange,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "הכרת טובה משנה את חייך.\nהתחל/י לרשום דברים שאת/ה אסיר/ה תודה עליהם",
            fontSize = 15.sp,
            color = GratMuted,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = GratOrange),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("הוסף הודיה ראשונה", fontWeight = FontWeight.Bold)
        }
    }
}
