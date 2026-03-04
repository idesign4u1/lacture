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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jewish.calendar.R
import com.jewish.calendar.model.ChatMessage
import com.jewish.calendar.model.HALACHIC_TOPICS
import com.jewish.calendar.model.HalachicTopic
import com.jewish.calendar.viewmodel.HalachicBotViewModel

// ── Palette ────────────────────────────────────────────────────────────

private val NavyDeep     = Color(0xFF070E28)
private val NavyMid      = Color(0xFF0D1B4E)
private val NavyLight    = Color(0xFF102060)
private val NavyCard     = Color(0xFF162255)
private val NavyBubble   = Color(0xFF1A2B68)
private val GoldAccent   = Color(0xFFD4AF37)
private val GoldSoft     = Color(0xFFF0D060)
private val GoldDim      = Color(0xFFAA8C28)
private val UserBubble   = Color(0xFF1565C0)
private val UserBubbleBright = Color(0xFF1976D2)
private val MutedText    = Color(0xFF8AABDD)
private val WhiteText    = Color(0xF0FFFFFF)
private val SubtleText   = Color(0xAAFFFFFF)

// ── Screen ─────────────────────────────────────────────────────────────

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyDeep, NavyMid, NavyLight)))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    "הרב שמואל כהן",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent
                                )
                                Text(
                                    "שאלות הלכתיות • לעיון בלבד",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedText
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            RabbiAvatar(size = 46)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.clearConversation() }) {
                            Icon(Icons.Default.Delete, contentDescription = "נקה שיחה", tint = MutedText)
                        }
                        IconButton(onClick = onSignOut) {
                            Icon(Icons.Default.Logout, contentDescription = "התנתקות", tint = MutedText)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NavyDeep.copy(alpha = 0.95f)
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
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
}

// ── Disclaimer banner ──────────────────────────────────────────────────

@Composable
private fun DisclaimerBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GoldAccent.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = "התשובות לעיון בלבד • אינן מחליפות שאלת רב",
            style = MaterialTheme.typography.labelSmall,
            color = GoldSoft,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = GoldDim,
            modifier = Modifier.size(14.dp)
        )
    }
}

// ── Empty state with topic cards ───────────────────────────────────────

@Composable
private fun EmptyStateWithTopics(onTopicQuestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))

        // Rabbi icon + greeting
        RabbiAvatar(size = 72)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "שלום! אני הרב שמואל כהן",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = WhiteText,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "שאל שאלה הלכתית או בחר נושא",
            fontSize = 14.sp,
            color = MutedText,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))

        // Topic cards
        HALACHIC_TOPICS.forEach { topic ->
            TopicCard(topic = topic, onQuestionClick = onTopicQuestionClick)
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(100.dp))
    }
}

// ── Topic card ─────────────────────────────────────────────────────────

@Composable
private fun TopicCard(topic: HalachicTopic, onQuestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(NavyCard)
            .border(1.dp, NavyBubble, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = GoldAccent
            )
            Spacer(Modifier.width(8.dp))
            Text(topic.icon, fontSize = 18.sp)
        }
        Spacer(Modifier.height(8.dp))
        topic.questions.forEach { question ->
            TextButton(
                onClick = { onQuestionClick(question) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "• $question",
                    style = MaterialTheme.typography.bodySmall,
                    color = GoldSoft.copy(alpha = 0.85f),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ── Rabbi avatar ───────────────────────────────────────────────────────

@Composable
private fun RabbiAvatar(size: Int = 32) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .border(2.dp, GoldAccent, CircleShape)
    ) {
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

    // RTL layout: Arrangement.Start = RIGHT side, Arrangement.End = LEFT side
    // User messages → RIGHT (Start), Bot messages → LEFT (End)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.Bottom
    ) {
        // User avatar (rightmost in RTL Start row)
        if (isUser) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(UserBubble)
                    .border(1.dp, GoldDim, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
        }

        // Message bubble
        Column(
            modifier = Modifier.widthIn(max = 290.dp),
            horizontalAlignment = if (isUser) Alignment.Start else Alignment.End
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart    = if (isUser) 4.dp else 18.dp,
                            topEnd      = if (isUser) 18.dp else 4.dp,
                            bottomStart = 18.dp,
                            bottomEnd   = 18.dp
                        )
                    )
                    .background(
                        if (isUser)
                            Brush.linearGradient(listOf(UserBubble, UserBubbleBright))
                        else
                            Brush.linearGradient(listOf(NavyCard, NavyBubble))
                    )
                    .border(
                        width = 1.dp,
                        color = if (isUser) UserBubbleBright.copy(alpha = 0.4f) else GoldAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(
                            topStart    = if (isUser) 4.dp else 18.dp,
                            topEnd      = if (isUser) 18.dp else 4.dp,
                            bottomStart = 18.dp,
                            bottomEnd   = 18.dp
                        )
                    )
            ) {
                if (message.isLoading) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = GoldAccent
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "חושב...",
                            color = MutedText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    Text(
                        text      = message.content,
                        modifier  = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color     = if (isUser) Color.White else WhiteText,
                        style     = TextStyle(
                            fontSize      = 15.sp,
                            textAlign     = TextAlign.End,
                            textDirection = TextDirection.Rtl,
                            lineHeight    = 22.sp
                        )
                    )
                }
            }
        }

        // Bot avatar (leftmost in RTL End row)
        if (!isUser) {
            Spacer(Modifier.width(8.dp))
            RabbiAvatar(size = 34)
        }
    }
}

// ── Chat input bar ─────────────────────────────────────────────────────

@Composable
private fun ChatInputBar(
    inputText: String,
    isLoading: Boolean,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = NavyDeep.copy(alpha = 0.97f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.Bottom
        ) {
            // Text input field (right side in RTL — comes first)
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    fontSize      = 16.sp,
                    color         = WhiteText,
                    textAlign     = TextAlign.End,
                    textDirection = TextDirection.Rtl
                ),
                placeholder = {
                    Text(
                        "הקלד את שאלתך כאן...",
                        style = TextStyle(
                            textAlign     = TextAlign.End,
                            textDirection = TextDirection.Rtl,
                            fontSize      = 15.sp
                        ),
                        color = MutedText,
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
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor       = GoldAccent,
                    unfocusedBorderColor     = NavyBubble,
                    focusedContainerColor    = NavyCard,
                    unfocusedContainerColor  = NavyCard,
                    cursorColor              = GoldAccent
                )
            )

            Spacer(Modifier.width(10.dp))

            // Send button (left side in RTL — comes second)
            IconButton(
                onClick = onSend,
                enabled = inputText.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputText.isNotBlank() && !isLoading)
                            Brush.radialGradient(listOf(GoldAccent, GoldDim))
                        else
                            Brush.radialGradient(listOf(NavyCard, NavyCard))
                    )
            ) {
                Icon(
                    if (isLoading) Icons.Default.HourglassEmpty else Icons.Default.Send,
                    contentDescription = "שלח",
                    tint = if (inputText.isNotBlank() && !isLoading) NavyDeep else MutedText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ── Error banner ───────────────────────────────────────────────────────

@Composable
private fun ErrorBanner(error: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF5C0000).copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "סגור", modifier = Modifier.size(16.dp), tint = Color.White)
        }
        Text(
            text = error,
            color = Color(0xFFFF6B6B),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}
