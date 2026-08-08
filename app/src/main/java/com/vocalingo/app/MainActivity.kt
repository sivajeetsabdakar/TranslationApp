package com.vocalingo.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.vocalingo.app.audio.EarCalibrationPlayer
import com.vocalingo.app.audio.StereoPcmPlayer
import com.vocalingo.app.conversation.AppLanguage
import com.vocalingo.app.conversation.CalibrationResult
import com.vocalingo.app.conversation.ConversationEventType
import com.vocalingo.app.conversation.ConversationSession
import com.vocalingo.app.conversation.LanguageCatalog
import com.vocalingo.app.conversation.SpeakerSide

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VocaLingoApp()
        }
    }
}

@Composable
private fun VocaLingoApp() {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Color(0xFF0F766E),
            secondary = Color(0xFF7C3AED),
            background = Color(0xFFF8FAFC),
            surface = Color.White,
        ),
    ) {
        ConversationScreen()
    }
}

@Composable
private fun ConversationScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = remember {
        ConversationSession(
            context = context,
            backendWsUrl = BuildConfig.BACKEND_WS_URL,
            backendApiToken = BuildConfig.BACKEND_API_TOKEN,
            scope = scope,
        )
    }
    val calibrationPlayer = remember { EarCalibrationPlayer(StereoPcmPlayer()) }

    var leftLanguage by remember { mutableStateOf(LanguageCatalog.byId("en-IN")) }
    var rightLanguage by remember { mutableStateOf(LanguageCatalog.byId("hi-IN")) }
    var activeSide by remember { mutableStateOf<SpeakerSide?>(null) }
    var leftTranscript by remember { mutableStateOf("") }
    var rightTranscript by remember { mutableStateOf("") }
    var leftTranslation by remember { mutableStateOf("") }
    var rightTranslation by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Ready. Choose languages, test earphones, then start a mic.") }
    var calibrationResult by remember { mutableStateOf<CalibrationResult?>(null) }
    var showEarTestDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        status = if (granted) "Microphone ready." else "Microphone permission is required."
    }

    LaunchedEffect(session) {
        session.events.collect { event ->
            when (event.type) {
                ConversationEventType.PARTIAL_TRANSCRIPT -> {
                    if (event.side == SpeakerSide.LEFT_USER) leftTranscript = event.text else rightTranscript = event.text
                }
                ConversationEventType.SEGMENT_COMMITTED -> {
                    if (event.side == SpeakerSide.LEFT_USER) leftTranscript = event.text else rightTranscript = event.text
                    status = "Segment committed for ${event.side.name.lowercase().replace('_', ' ')}"
                }
                ConversationEventType.TRANSLATED_TEXT -> {
                    if (event.side == SpeakerSide.LEFT_USER) rightTranslation = event.text else leftTranslation = event.text
                }
                ConversationEventType.TTS_AUDIO -> status = "Playing translated chunk."
                ConversationEventType.LATENCY_METRICS -> status = "Last chunk latency: ${event.latencyMs} ms"
                ConversationEventType.SESSION_ERROR -> {
                    activeSide = null
                    status = event.text.ifBlank { "Session error." }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { session.shutdown() }
    }

    fun toggle(side: SpeakerSide) {
        if (activeSide == side) {
            session.stop()
            activeSide = null
            status = "Stopped."
            return
        }
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        val source = if (side == SpeakerSide.LEFT_USER) leftLanguage else rightLanguage
        val target = if (side == SpeakerSide.LEFT_USER) rightLanguage else leftLanguage
        activeSide = side
        status = if (calibrationResult == CalibrationResult.STEREO_OK) {
            "Listening to ${source.displayName}; translating to ${target.displayName}."
        } else {
            "Listening now. Run the ear test before relying on ear separation."
        }
        session.start(side, source, target)
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TopStatusBar(
                status = status,
                calibrationResult = calibrationResult,
                onEarTest = {
                    calibrationPlayer.playLeftThenRight(scope)
                    showEarTestDialog = true
                    status = "Ear test playing. Confirm whether left and right are separate."
                },
                onStop = {
                    session.stop()
                    activeSide = null
                    status = "Stopped."
                },
            )
            ConversationPanel(
                modifier = Modifier
                    .weight(1f)
                    .rotate(180f),
                title = "Right user",
                language = rightLanguage,
                active = activeSide == SpeakerSide.RIGHT_USER,
                transcript = rightTranscript,
                translatedText = rightTranslation,
                onLanguageChange = { rightLanguage = it },
                onMic = { toggle(SpeakerSide.RIGHT_USER) },
            )
            ConversationPanel(
                modifier = Modifier.weight(1f),
                title = "Left user",
                language = leftLanguage,
                active = activeSide == SpeakerSide.LEFT_USER,
                transcript = leftTranscript,
                translatedText = leftTranslation,
                onLanguageChange = { leftLanguage = it },
                onMic = { toggle(SpeakerSide.LEFT_USER) },
            )
        }
        if (showEarTestDialog) {
            EarTestDialog(
                onStereoOk = {
                    calibrationResult = CalibrationResult.STEREO_OK
                    status = "Ear test passed. Stereo separation is ready."
                    showEarTestDialog = false
                },
                onMonoDetected = {
                    calibrationResult = CalibrationResult.MONO_OUTPUT_DETECTED
                    status = "Ear test failed. Use wired/USB-C stereo earphones for best results."
                    showEarTestDialog = false
                },
                onDismiss = {
                    calibrationResult = CalibrationResult.UNKNOWN_BLUETOOTH_BEHAVIOR
                    status = "Ear test skipped. Bluetooth separation may vary by device."
                    showEarTestDialog = false
                },
            )
        }
    }
}

@Composable
private fun EarTestDialog(
    onStereoOk: () -> Unit,
    onMonoDetected: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ear test") },
        text = {
            Text(
                text = "You should hear the first tone in the left ear and the second tone in the right ear.",
                textAlign = TextAlign.Start,
            )
        },
        confirmButton = {
            Button(
                modifier = Modifier.testTag("ear-test-pass-button"),
                onClick = onStereoOk,
            ) {
                Text("Stereo works")
            }
        },
        dismissButton = {
            OutlinedButton(
                modifier = Modifier.testTag("ear-test-fail-button"),
                onClick = onMonoDetected,
            ) {
                Text("Not separate")
            }
        },
    )
}

@Composable
private fun TopStatusBar(
    status: String,
    calibrationResult: CalibrationResult?,
    onEarTest: () -> Unit,
    onStop: () -> Unit,
) {
    Card(
        modifier = Modifier.testTag("top-status-bar"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("VocaLingo", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text(
                    text = calibrationLabel(calibrationResult) ?: status,
                    modifier = Modifier.testTag("conversation-status"),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF475569),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedButton(
                modifier = Modifier.testTag("ear-test-button"),
                onClick = onEarTest,
            ) {
                Text("Ear test")
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(
                modifier = Modifier.testTag("stop-all-button"),
                onClick = onStop,
            ) {
                Text("Stop")
            }
        }
    }
}

@Composable
private fun ConversationPanel(
    modifier: Modifier,
    title: String,
    language: AppLanguage,
    active: Boolean,
    transcript: String,
    translatedText: String,
    onLanguageChange: (AppLanguage) -> Unit,
    onMic: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("${title.lowercase().replace(' ', '-')}-panel"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                )
                LanguagePicker(language, onLanguageChange)
            }
            TwoTextBlocks(
                modifier = Modifier.weight(1f),
                transcript = transcript,
                translatedText = translatedText,
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("${title.lowercase().replace(' ', '-')}-mic-button"),
                onClick = onMic,
            ) {
                Text(if (active) "Stop listening" else "Start mic")
            }
        }
    }
}

@Composable
private fun TwoTextBlocks(
    modifier: Modifier,
    transcript: String,
    translatedText: String,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextBlock(
            modifier = Modifier.weight(1f),
            label = "Heard",
            text = transcript.ifBlank { "No speech captured yet." },
        )
        TextBlock(
            modifier = Modifier.weight(1f),
            label = "Translated",
            text = translatedText.ifBlank { "No translated audio yet." },
        )
    }
}

private fun calibrationLabel(result: CalibrationResult?): String? = when (result) {
    CalibrationResult.STEREO_OK -> "Ear test passed."
    CalibrationResult.MONO_OUTPUT_DETECTED -> "Ear test failed: output is not separated."
    CalibrationResult.UNKNOWN_BLUETOOTH_BEHAVIOR -> "Ear test skipped: Bluetooth behavior unknown."
    null -> null
}

@Composable
private fun TextBlock(modifier: Modifier, label: String, text: String) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                .padding(10.dp),
        ) {
            Text(
                text = text,
                modifier = Modifier.verticalScroll(rememberScrollState()),
                color = Color(0xFF0F172A),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePicker(
    selected: AppLanguage,
    onSelected: (AppLanguage) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        TextField(
            modifier = Modifier
                .menuAnchor()
                .width(180.dp)
                .testTag("language-picker-${selected.id}"),
            readOnly = true,
            value = selected.displayName,
            onValueChange = {},
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            LanguageCatalog.indianFirstLanguages.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.displayName) },
                    onClick = {
                        onSelected(language)
                        expanded = false
                    },
                )
            }
        }
    }
}
