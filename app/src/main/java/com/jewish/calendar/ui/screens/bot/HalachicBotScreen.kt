package com.jewish.calendar.ui.screens.bot

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jewish.calendar.R
import com.jewish.calendar.model.ChatMessage
import com.jewish.calendar.model.HALACHIC_TOPICS
import com.jewish.calendar.model.HalachicTopic
import com.jewish.calendar.ui.theme.*
import com.jewish.calendar.viewmodel.HalachicBotViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HalachicBotScreen(
    viewModel: HalachicBotViewModel = hiltViewModel(),
    onSignOut: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-scroll to last message
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "הרב שמואל כהן",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "שאלות הלכתיות • לעיון בלבד",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        // Rabbi avatar (replace res/drawable/rabbi_samuel.xml with rabbi_samuel.png 400×400px)
                        RabbiAvatar(size = 44)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearConversation() }) {
                        Icon(Icons.Default.Delete, contentDescription = "נקה שיחה")
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.Logout, contentDescription = "התנתקות")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                inputText = uiState.inputText,
                isLoading = uiState.isLoading,
                onInputChanged = viewModel::onInputChanged,
                onSend = {
                    viewModel.sendMessage()
                    keyboardController?.hide()
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Disclaimer banner
            DisclaimerBanner()

            // Messages list or empty state with topics
            if (uiState.messages.isEmpty()) {
                EmptyStateWithTopics(
                    onTopicQuestionClick = { viewModel.sendMessage(it) }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.messages) { message ->
                        ChatBubble(message = message)
                    }
                }
            }

            // Error snackbar
            uiState.error?.let { error ->
                ErrorBanner(error = error, onDismiss = viewModel::dismissError)
            }
        }
    }

}

@Composable
private fun DisclaimerBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Gold60.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "התשובות לעיון בלבד • אינן מחליפות שאלת רב",
                style = MaterialTheme.typography.labelSmall,
                color = Gold80,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = Gold80,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun EmptyStateWithTopics(onTopicQuestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text("📿", fontSize = 56.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "שאל שאלה הלכתית",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "בחר נושא או כתוב שאלה חופשית",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        // Topic grid
        HALACHIC_TOPICS.forEach { topic ->
            TopicCard(topic = topic, onQuestionClick = onTopicQuestionClick)
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(100.dp)) // Bottom padding for keyboard
    }
}

@Composable
private fun TopicCard(topic: HalachicTopic, onQuestionClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text(topic.icon, fontSize = 20.sp)
            }
            Spacer(Modifier.height(8.dp))
            topic.questions.forEach { question ->
                TextButton(
                    onClick = { onQuestionClick(question) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "← $question",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ── Rabbi avatar composable ────────────────────────────────────────────

@Composable
private fun RabbiAvatar(size: Int = 32) {
    // To use a real photo: replace res/drawable/rabbi_samuel.xml with rabbi_samuel.png (400×400px square)
    Box(modifier = Modifier.size(size.dp).clip(CircleShape)) {
        Image(
            painter            = painterResource(R.drawable.rabbi_samuel),
            contentDescription = "הרב שמואל כהן",
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.matchParentSize()
        )
    }
}

// ── Chat bubble ────────────────────────────────────────────────────────

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser

    // In RTL layout: Arrangement.Start = RIGHT side, Arrangement.End = LEFT side
    // User messages → RIGHT (Start), Bot messages → LEFT (End)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.Start else Arrangement.End
    ) {
        // User avatar is first (rightmost in RTL Start row)
        if (isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
        }

        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(
                // In RTL: topStart = top-right, topEnd = top-left
                // User (right side): sharp corner at right (topStart) = 4dp
                // Bot  (left  side): sharp corner at left  (topEnd)   = 4dp
                topStart    = if (isUser) 4.dp else 16.dp,
                topEnd      = if (isUser) 16.dp else 4.dp,
                bottomStart = 16.dp,
                bottomEnd   = 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primary
                                 else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            if (message.isLoading) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Blue60)
                    Spacer(Modifier.width(8.dp))
                    Text("חושב...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text(
                    text      = message.content,
                    modifier  = Modifier.padding(12.dp),
                    color     = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    style     = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End
                )
            }
        }

        // Bot avatar is last (leftmost in RTL End row)
        if (!isUser) {
            Spacer(Modifier.width(8.dp))
            RabbiAvatar(size = 32)
        }
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    isLoading: Boolean,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(
                onClick = onSend,
                enabled = inputText.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputText.isNotBlank() && !isLoading) Blue60
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {
                Icon(
                    if (isLoading) Icons.Default.HourglassEmpty else Icons.Default.Send,
                    contentDescription = "שלח",
                    tint = if (inputText.isNotBlank() && !isLoading) Color.White
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "שאל שאלה הלכתית...",
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send,
                    keyboardType = KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                enabled = !isLoading
            )
        }
    }
}

@Composable
private fun ErrorBanner(error: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = HolidayRed.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "סגור", modifier = Modifier.size(16.dp))
            }
            Text(
                text = error,
                color = HolidayRed,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
    }
}

