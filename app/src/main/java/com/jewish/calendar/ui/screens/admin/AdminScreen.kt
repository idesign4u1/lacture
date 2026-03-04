package com.jewish.calendar.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jewish.calendar.model.ToolConfig
import com.jewish.calendar.viewmodel.AdminViewModel
import kotlinx.coroutines.delay

// ── Color palette for theme picker ───────────────────────────────────

private val THEME_COLORS = listOf(
    "כחול כהה"    to Color(0xFF003E7E),
    "כחול"        to Color(0xFF1565C0),
    "ירוק"        to Color(0xFF2E7D32),
    "סגול"        to Color(0xFF6A1B9A),
    "כהה"         to Color(0xFF37474F),
    "בורדו"       to Color(0xFF880E4F),
    "כתום"        to Color(0xFFE65100),
    "ברונזה"      to Color(0xFF6D4C41),
    "ברירת מחדל"  to null
)

// ────────────────────────────────────────────────────────────────────
// AdminScreen
// ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(onBack: () -> Unit, viewModel: AdminViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Auto-dismiss success banner
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            delay(2000)
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ ניהול מערכת", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזור")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Success banner ────────────────────────────────────────
            if (uiState.saveSuccess) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "✓ השינויים נשמרו",
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // ── Tabs ──────────────────────────────────────────────────
            val tabs = listOf("כלים", "תוכן", "עיצוב", "הגדרות")
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            // ── Tab content ───────────────────────────────────────────
            when (selectedTab) {
                0 -> ToolsTab(
                    tools = uiState.tools,
                    onToggle = { id, visible -> viewModel.setToolVisible(id, visible) },
                    onShowAll = { viewModel.showAllTools() }
                )
                1 -> ContentTab(
                    dailyMessages = uiState.config.customDailyMessages,
                    botWelcome    = uiState.config.botWelcomeText,
                    onSaveMessages = viewModel::setDailyMessages,
                    onSaveWelcome  = viewModel::setBotWelcomeText
                )
                2 -> DesignTab(
                    currentColor = uiState.config.customPrimaryColor,
                    onSelectColor = viewModel::setPrimaryColor
                )
                3 -> SettingsTab(
                    onChangePin    = viewModel::changePin,
                    onResetDefaults = {
                        viewModel.resetToDefaults()
                    },
                    errorMessage = uiState.errorMessage,
                    onClearError = viewModel::clearError
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────
// Tab 1 — כלים (Tools visibility)
// ────────────────────────────────────────────────────────────────────

@Composable
private fun ToolsTab(
    tools: List<ToolConfig>,
    onToggle: (String, Boolean) -> Unit,
    onShowAll: () -> Unit
) {
    val hiddenCount = tools.count { !it.isVisible }
    val grouped = tools.groupBy { it.categoryHe }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "מוצגים ${tools.count { it.isVisible }} / ${tools.size} כלים",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hiddenCount > 0) {
                TextButton(onClick = onShowAll) {
                    Text("הצג הכל ($hiddenCount מוסתרים)")
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            grouped.forEach { (category, categoryTools) ->
                item {
                    Text(
                        category,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp)
                    )
                }
                items(categoryTools, key = { it.id }) { tool ->
                    ToolToggleRow(tool = tool, onToggle = { onToggle(tool.id, it) })
                }
            }
        }
    }
}

@Composable
private fun ToolToggleRow(tool: ToolConfig, onToggle: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = if (tool.isVisible)
            MaterialTheme.colorScheme.surface
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = if (tool.isVisible) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(tool.emoji, fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Text(
                tool.titleHe,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (tool.isVisible)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Switch(
                checked = tool.isVisible,
                onCheckedChange = onToggle,
                modifier = Modifier.height(28.dp)
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────
// Tab 2 — תוכן (Content management)
// ────────────────────────────────────────────────────────────────────

@Composable
private fun ContentTab(
    dailyMessages: List<String>,
    botWelcome: String,
    onSaveMessages: (List<String>) -> Unit,
    onSaveWelcome: (String) -> Unit
) {
    var messagesText by remember(dailyMessages) {
        mutableStateOf(dailyMessages.joinToString("\n"))
    }
    var welcomeText by remember(botWelcome) { mutableStateOf(botWelcome) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Daily inspiration messages
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "⭐ הודעות חיזוק יומי",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "כל שורה = הודעה נפרדת. ריק = ברירת מחדל מובנית.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = messagesText,
                    onValueChange = { messagesText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp),
                    placeholder = { Text("הכנס הודעות חיזוק (שורה לכל הודעה)...") },
                    maxLines = 12
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        val lines = messagesText.lines()
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                        onSaveMessages(lines)
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("שמור הודעות")
                }
            }
        }

        // Bot welcome text
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "🤖 טקסט פתיחה של שאל את הרב",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "ריק = טקסט ברירת מחדל.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = welcomeText,
                    onValueChange = { welcomeText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    placeholder = { Text("הכנס טקסט ברכה...") },
                    maxLines = 5
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { onSaveWelcome(welcomeText.trim()) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("שמור טקסט")
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────
// Tab 3 — עיצוב (Design / color picker)
// ────────────────────────────────────────────────────────────────────

@Composable
private fun DesignTab(
    currentColor: Long?,
    onSelectColor: (Long?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "🎨 צבע ראשי",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "בחר את הצבע הראשי של האפליקציה. ישפיע על כפתורים, כותרות וסמלים.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(4.dp))

        // Color grid (3 columns)
        val chunked = THEME_COLORS.chunked(3)
        chunked.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { (name, color) ->
                    val isSelected = if (color == null) currentColor == null
                                     else currentColor == color.value.toLong()
                    ColorSwatch(
                        name = name,
                        color = color ?: MaterialTheme.colorScheme.primary,
                        isDefault = color == null,
                        isSelected = isSelected,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectColor(color?.value?.toLong()) }
                    )
                }
                // Fill remaining cells if row is short
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "הערה: שינוי צבע ישפיע לאחר הפעלה מחדש של האפליקציה.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ColorSwatch(
    name: String,
    color: Color,
    isDefault: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (isDefault) MaterialTheme.colorScheme.primary else color)
                .border(
                    width = if (isSelected) 3.dp else 0.dp,
                    color = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ────────────────────────────────────────────────────────────────────
// Tab 4 — הגדרות (Settings)
// ────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsTab(
    onChangePin: (String) -> Unit,
    onResetDefaults: () -> Unit,
    errorMessage: String?,
    onClearError: () -> Unit
) {
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }

    val pinMismatch = newPin.isNotBlank() && confirmPin.isNotBlank() && newPin != confirmPin

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Error display
        errorMessage?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        msg,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onClearError, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        // Change PIN card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "🔒 שינוי PIN",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
                    label = { Text("PIN חדש (4 ספרות)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Lock, null) }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it },
                    label = { Text("אשר PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = pinMismatch,
                    supportingText = { if (pinMismatch) Text("הPIN לא תואם") },
                    leadingIcon = { Icon(Icons.Default.LockOpen, null) }
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        onChangePin(newPin)
                        newPin = ""
                        confirmPin = ""
                    },
                    enabled = newPin.length == 4 && newPin == confirmPin,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("שנה PIN")
                }
            }
        }

        // Reset to defaults card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "♻️ איפוס להגדרות ברירת מחדל",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "יאפס את נראות הכלים, הצבע, התוכן המותאם אישית. ה-PIN לא ישתנה.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("אפס הכל")
                }
            }
        }

        // App info
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📅 יהודה - לוח שנה יהודי", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "מערכת הניהול מאחסנת את ההגדרות מקומית במכשיר.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // Reset confirmation dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("אישור איפוס", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
            text = { Text("האם אתה בטוח שברצונך לאפס את כל ההגדרות לברירת המחדל?") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetDefaults()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("אפס") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("ביטול") }
            }
        )
    }
}
