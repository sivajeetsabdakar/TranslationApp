package com.vocalingo.app

import android.Manifest
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        window.statusBarColor = AndroidColor.rgb(8, 17, 31)
        window.navigationBarColor = AndroidColor.rgb(8, 17, 31)
        setContent {
            VocaLingoApp()
        }
    }
}

@Composable
private fun VocaLingoApp() {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Color(0xFF43D9C6),
            secondary = Color(0xFFFFB86B),
            background = Color(0xFF08111F),
            surface = Color(0xFF121B2A),
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
        containerColor = Color(0xFF08111F),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF08111F), Color(0xFF10243A), Color(0xFF07101B)),
                    ),
                )
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
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
                    title = "User 2",
                    language = rightLanguage,
                    targetLanguage = leftLanguage,
                    active = activeSide == SpeakerSide.RIGHT_USER,
                    transcript = rightTranscript,
                    translatedText = rightTranslation,
                    onLanguageChange = { rightLanguage = it },
                    onMic = { toggle(SpeakerSide.RIGHT_USER) },
                )
                ConversationPanel(
                    modifier = Modifier.weight(1f),
                    title = "User 1",
                    language = leftLanguage,
                    targetLanguage = rightLanguage,
                    active = activeSide == SpeakerSide.LEFT_USER,
                    transcript = leftTranscript,
                    translatedText = leftTranslation,
                    onLanguageChange = { leftLanguage = it },
                    onMic = { toggle(SpeakerSide.LEFT_USER) },
                )
            }
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
    Surface(
        modifier = Modifier.testTag("top-status-bar"),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF121E30),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x263FE8D0), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "VocaLingo",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 18.sp,
                )
                Text(
                    text = calibrationLabel(calibrationResult) ?: status,
                    modifier = Modifier.testTag("conversation-status"),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFFAFC3D8),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedButton(
                modifier = Modifier.testTag("ear-test-button"),
                onClick = onEarTest,
            ) {
                Text("Test")
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
    targetLanguage: AppLanguage,
    active: Boolean,
    transcript: String,
    translatedText: String,
    onLanguageChange: (AppLanguage) -> Unit,
    onMic: () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("${title.lowercase().replace(' ', '-')}-panel"),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF101A29),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, panelBorder(active), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiveDot(active)
                        Text(
                            title.uppercase(),
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 18.sp,
                        )
                    }
                    Text(
                        "${language.shortName()} to ${targetLanguage.shortName()}",
                        color = Color(0xFFAFC3D8),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                LanguagePicker(language, onLanguageChange)
            }
            TwoTextBlocks(
                modifier = Modifier.weight(1f),
                transcript = transcript,
                translatedText = translatedText,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp),
                contentAlignment = Alignment.Center,
            ) {
                MicButton(
                    active = active,
                    testTag = "${title.lowercase().replace(' ', '-')}-mic-button",
                    onClick = onMic,
                )
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
            label = "Speech",
            text = transcript.ifBlank { "Speak to begin" },
        )
        TextBlock(
            modifier = Modifier.weight(1f),
            label = "Translation",
            text = translatedText.ifBlank { "Translation appears here" },
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
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF7FDCD2),
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF172438), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(8.dp))
                .padding(12.dp),
        ) {
            Text(
                text = text,
                modifier = Modifier.verticalScroll(rememberScrollState()),
                color = if (text == "Speak to begin" || text == "Translation appears here") {
                    Color(0xFF8297AB)
                } else {
                    Color.White
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun LanguagePicker(
    selected: AppLanguage,
    onSelected: (AppLanguage) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.testTag("language-picker-${selected.id}"),
    ) {
        Surface(
            modifier = Modifier
                .width(128.dp)
                .clickable { expanded = true },
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF203148),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    selected.shortName(),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
                Text("v", color = Color(0xFF7FDCD2), fontWeight = FontWeight.Bold)
            }
        }
        DropdownMenu(
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

@Composable
private fun MicButton(active: Boolean, testTag: String, onClick: () -> Unit) {
    val colors = if (active) {
        listOf(Color(0xFFFF7A59), Color(0xFFFFB86B))
    } else {
        listOf(Color(0xFF39D3F2), Color(0xFF43D9C6))
    }
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(Brush.radialGradient(colors), CircleShape)
            .border(3.dp, Color(0x40FFFFFF), CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        MicGlyph(color = Color(0xFF06111C), active = active)
    }
}

@Composable
private fun MicGlyph(color: Color, active: Boolean) {
    Canvas(modifier = Modifier.size(34.dp)) {
        val strokeWidth = 3.2.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.34f, size.height * 0.08f),
            size = Size(size.width * 0.32f, size.height * 0.48f),
            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
            style = Stroke(width = strokeWidth),
        )
        drawArc(
            color = color,
            startAngle = 18f,
            sweepAngle = 144f,
            useCenter = false,
            topLeft = Offset(size.width * 0.18f, size.height * 0.28f),
            size = Size(size.width * 0.64f, size.height * 0.45f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.5f, size.height * 0.73f),
            end = Offset(size.width * 0.5f, size.height * 0.92f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.34f, size.height * 0.92f),
            end = Offset(size.width * 0.66f, size.height * 0.92f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        if (active) {
            drawCircle(color = Color(0xB306111C), radius = 3.5.dp.toPx(), center = Offset(size.width * 0.76f, size.height * 0.2f))
        }
    }
}

@Composable
private fun LiveDot(active: Boolean) {
    val color = if (active) Color(0xFFFF8A63) else Color(0xFF43D9C6)
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(color, CircleShape),
    )
}

private fun panelBorder(active: Boolean): Color =
    if (active) Color(0xFFFFB86B) else Color(0x263FE8D0)

private fun AppLanguage.shortName(): String =
    displayName
        .replace("English (India)", "English")
        .replace("English (US)", "English")
        .substringBefore(" (")
