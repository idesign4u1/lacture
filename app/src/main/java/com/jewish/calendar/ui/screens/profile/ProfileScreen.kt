package com.jewish.calendar.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jewish.calendar.ui.theme.ShabbatBlue
import com.jewish.calendar.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onProfileSaved: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Form state - initialised from the loaded user
    var displayName by remember(uiState.user) { mutableStateOf(uiState.user?.displayName ?: "") }
    var gender by remember(uiState.user) { mutableStateOf(uiState.user?.gender ?: "") }
    var birthDate by remember(uiState.user) { mutableStateOf(uiState.user?.birthDate ?: "") }
    var maritalStatus by remember(uiState.user) { mutableStateOf(uiState.user?.maritalStatus ?: "") }
    var apiKey by remember(uiState.openAiApiKey) { mutableStateOf(uiState.openAiApiKey) }
    var apiKeyVisible by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    // Dismiss success after a moment + notify parent for Tahara tab refresh
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onProfileSaved()
            kotlinx.coroutines.delay(2000)
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("הפרופיל שלי", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.Logout, contentDescription = "התנתקות")
                    }
                }
            )
        }
    ) { padding ->

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Avatar ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.firstOrNull()?.toString()?.uppercase() ?: "?",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = uiState.user?.email ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // ── Success / Error banners ──────────────────────────────
            if (uiState.saveSuccess) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(
                        "הפרופיל נשמר בהצלחה ✓",
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            uiState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(
                        error,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // ── Personal details ────────────────────────────────────
            SectionTitle("פרטים אישיים")

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("שם מלא") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                singleLine = true
            )

            // Birth date field + picker
            OutlinedTextField(
                value = if (birthDate.isNotBlank()) formatDisplayDate(birthDate) else "",
                onValueChange = {},
                label = { Text("תאריך לידה") },
                leadingIcon = { Icon(Icons.Default.Cake, null) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.EditCalendar, "בחר תאריך")
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                readOnly = true,
                singleLine = true
            )
            // Hebrew birthday equivalent
            if (birthDate.isNotBlank()) {
                Text(
                    text = "תאריך עברי: ${hebrewBirthDate(birthDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ShabbatBlue,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    textAlign = TextAlign.End
                )
            } else {
                Spacer(Modifier.height(10.dp))
            }

            // Marital status
            SectionTitle("מצב משפחתי")
            MaritalStatusSelector(selected = maritalStatus, onSelect = { maritalStatus = it })

            Spacer(Modifier.height(12.dp))

            // Gender
            SectionTitle("מין")
            GenderSelector(selected = gender, onSelect = { gender = it })
            if (gender == "male") {
                Text(
                    "שים לב: שינוי לגבר יסגור את גישתך למדור הטהרה",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    textAlign = TextAlign.End
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Save profile button ──────────────────────────────────
            Button(
                onClick = { viewModel.saveProfile(displayName.trim(), gender, birthDate, maritalStatus) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = displayName.isNotBlank() && gender.isNotBlank() && !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("שמור פרופיל", fontWeight = FontWeight.Bold)
                }
            }

            Divider(Modifier.padding(vertical = 20.dp))

            // ── OpenAI API Key ───────────────────────────────────────
            SectionTitle("מפתח OpenAI API")
            Text(
                "נדרש עבור הרב AI. השג מפתח מ-platform.openai.com",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("sk-proj-...") },
                leadingIcon = { Icon(Icons.Default.Key, null) },
                trailingIcon = {
                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                        Icon(
                            if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null
                        )
                    }
                },
                visualTransformation = if (apiKeyVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Button(
                onClick = { viewModel.saveApiKey(apiKey) },
                modifier = Modifier.fillMaxWidth(),
                enabled = apiKey.isNotBlank()
            ) {
                Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("שמור מפתח API")
            }

            Divider(Modifier.padding(vertical = 20.dp))

            // ── Change password ──────────────────────────────────────
            SectionTitle("אבטחה")
            OutlinedButton(
                onClick = { showPasswordDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Lock, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("שנה סיסמה")
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Date picker dialog ───────────────────────────────────────────
    if (showDatePicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance().apply { timeInMillis = millis }
                        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        birthDate = fmt.format(cal.time)
                    }
                    showDatePicker = false
                }) { Text("אישור") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("ביטול") } }
        ) {
            DatePicker(state = state)
        }
    }

    // ── Change password dialog ───────────────────────────────────────
    if (showPasswordDialog) {
        ChangePasswordDialog(
            isSaving = uiState.isSaving,
            onConfirm = { current, new -> viewModel.changePassword(current, new) },
            onDismiss = { showPasswordDialog = false }
        )
    }
}

// ────────────────────────────────────────────────────────────────────
// Helper composables
// ────────────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        textAlign = TextAlign.End
    )
}

@Composable
private fun MaritalStatusSelector(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        "single" to "רווק/ה",
        "married" to "נשוי/אה",
        "divorced" to "גרוש/ה",
        "widowed" to "אלמן/ה"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label, fontSize = 12.sp) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GenderSelector(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf("female" to "אישה", "male" to "גבר").forEach { (value, label) ->
            val icon = if (value == "female") Icons.Default.Female else Icons.Default.Male
            Card(
                onClick = { onSelect(value) },
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected == value)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                border = if (selected == value)
                    androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                else null
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, null, tint = if (selected == value)
                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text(label, fontWeight = if (selected == value) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected == value) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ChangePasswordDialog(
    isSaving: Boolean,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentPw by remember { mutableStateOf("") }
    var newPw by remember { mutableStateOf("") }
    var confirmPw by remember { mutableStateOf("") }
    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    val mismatch = newPw.isNotBlank() && confirmPw.isNotBlank() && newPw != confirmPw

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("שינוי סיסמה", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = currentPw, onValueChange = { currentPw = it },
                    label = { Text("סיסמה נוכחית") },
                    visualTransformation = if (showCurrent) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showCurrent = !showCurrent }) {
                            Icon(if (showCurrent) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPw, onValueChange = { newPw = it },
                    label = { Text("סיסמה חדשה (לפחות 6 תווים)") },
                    visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showNew = !showNew }) {
                            Icon(if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPw, onValueChange = { confirmPw = it },
                    label = { Text("אשר סיסמה חדשה") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = mismatch,
                    supportingText = { if (mismatch) Text("הסיסמאות אינן תואמות") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(currentPw, newPw) },
                enabled = currentPw.isNotBlank() && newPw.length >= 6 && newPw == confirmPw && !isSaving
            ) {
                if (isSaving) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary)
                else Text("שנה")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } }
    )
}

// ── Pure helper functions ────────────────────────────────────────────

private fun formatDisplayDate(dateStr: String): String {
    return try {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFmt = SimpleDateFormat("d בMMMM yyyy", Locale("he"))
        displayFmt.format(fmt.parse(dateStr)!!)
    } catch (_: Exception) { dateStr }
}

private fun hebrewBirthDate(dateStr: String): String {
    return try {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = fmt.parse(dateStr) ?: return ""
        val cal = com.kosherjava.zmanim.hebrewcalendar.JewishCalendar(
            java.util.Calendar.getInstance().apply { time = date }
        )
        com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter().apply {
            isHebrewFormat = true
            isUseFinalFormLetters = true
        }.format(cal)
    } catch (_: Exception) { "" }
}
