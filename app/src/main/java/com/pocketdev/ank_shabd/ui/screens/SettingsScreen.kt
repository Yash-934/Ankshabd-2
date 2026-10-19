package com.pocketdev.ank_shabd.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketdev.ank_shabd.domain.encoder.EncodingRuleSet
import com.pocketdev.ank_shabd.domain.models.CoverageStats
import com.pocketdev.ank_shabd.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    apiKey: String,
    providerId: String,
    apiEndpoint: String,
    apiModel: String,
    themeMode: String = "system",
    isDarkThemeActive: Boolean = false,
    onThemeModeChange: (String) -> Unit = {},
    onSaveAiConfig: (String, String, String, String) -> Unit,
    onTestAiConnection: ((Boolean, String) -> Unit) -> Unit,
    onTestTts: (String) -> Unit,
    coverageStats: CoverageStats?
) {
    var keyInput by remember { mutableStateOf(apiKey) }
    var selectedProvider by remember { mutableStateOf(providerId) }
    var endpointInput by remember { mutableStateOf(apiEndpoint) }
    var modelInput by remember {
        mutableStateOf(
            if (apiModel.isBlank() || apiModel.contains("2.5") || apiModel.contains("1.5") || apiModel.contains("2.0")) "gemini-3.8-flash" else apiModel
        )
    }
    var isTestingAi by remember { mutableStateOf(false) }
    var aiTestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var showApiKey by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Title
        item {
            Column {
                Text(
                    text = "सेटिंग्स | Settings",
                    fontSize = 24.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "दिखावट (Dark/Light Mode), V2.3 नियम, AI कॉन्फ़िगरेशन एवं ऑडियो",
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Appearance: Dark / Light Mode Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🌗 दिखावट एवं थीम | Dark / Light Mode",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isDarkThemeActive) "वर्तमान: डार्क मोड (Dark Mode सक्रिय)" else "वर्तमान: लाइट मोड (Light Mode सक्रिय)",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Quick 1-Tap Dark/Light Mode Switch Button
                        Button(
                            onClick = {
                                val nextMode = if (isDarkThemeActive) "light" else "dark"
                                onThemeModeChange(nextMode)
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("toggle_dark_light_button")
                        ) {
                            Text(
                                text = if (isDarkThemeActive) "☀️ Light करें" else "🌙 Dark करें",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = themeMode == "light",
                            onClick = { onThemeModeChange("light") },
                            modifier = Modifier.weight(1f).testTag("theme_light_chip"),
                            label = {
                                Text(
                                    text = "☀️ Light",
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    maxLines = 1
                                )
                            }
                        )
                        FilterChip(
                            selected = themeMode == "dark",
                            onClick = { onThemeModeChange("dark") },
                            modifier = Modifier.weight(1f).testTag("theme_dark_chip"),
                            label = {
                                Text(
                                    text = "🌙 Dark",
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    maxLines = 1
                                )
                            }
                        )
                        FilterChip(
                            selected = themeMode == "system",
                            onClick = { onThemeModeChange("system") },
                            modifier = Modifier.weight(1f).testTag("theme_system_chip"),
                            label = {
                                Text(
                                    text = "📱 ऑटो (System)",
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }
            }
        }

        // 2. Active Ruleset Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📜 Active Ruleset Specification",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Identifier: ${EncodingRuleSet.DEFAULT_RULESET_ID}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Version: 2.3 Updated (Vedic Hindi Phonetic & Matra Number System)",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "All number encoding, candidate discovery, and word verification are strictly calculated using this deterministic ruleset.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 3. AI Provider Settings Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🤖 AI Provider Configuration",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Optional online discovery & memory story generation via Gemini API. Offline deterministic engine remains 100% active when disconnected.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = selectedProvider == "gemini",
                            onClick = { selectedProvider = "gemini" },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Gemini") }
                        )
                        FilterChip(
                            selected = selectedProvider == "custom",
                            onClick = { selectedProvider = "custom" },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("अन्य AI API (OpenAI-compatible)") }
                        )
                    }

                    if (selectedProvider == "custom") {
                        OutlinedTextField(
                            value = endpointInput,
                            onValueChange = { endpointInput = it },
                            label = { Text("Chat Completions API URL") },
                            placeholder = { Text("https://…/v1/chat/completions") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = modelInput,
                            onValueChange = { modelInput = it },
                            label = { Text("Model name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    } else {
                        OutlinedTextField(
                            value = modelInput,
                            onValueChange = { modelInput = it },
                            label = { Text("Gemini model") },
                            placeholder = { Text("gemini-3.8-flash") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text(if (selectedProvider == "gemini") "Gemini API Key" else "API Key / Bearer Token") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(if (selectedProvider == "gemini") "AIzaSy..." else "API key") },
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showApiKey = !showApiKey }) {
                                Text(if (showApiKey) "Hide" else "Show")
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onSaveAiConfig(keyInput.trim(), selectedProvider, endpointInput.trim(), modelInput.trim())
                                aiTestResult = Pair(true, "API Key saved successfully!")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save Key")
                        }

                        OutlinedButton(
                            onClick = {
                                isTestingAi = true
                                onTestAiConnection { success, message ->
                                    isTestingAi = false
                                    aiTestResult = Pair(success, message)
                                }
                            },
                            enabled = !isTestingAi,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isTestingAi) "Testing..." else "Test Connection")
                        }
                    }

                    aiTestResult?.let { (success, msg) ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (success) "✓ $msg" else "✗ $msg",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (success) EmeraldGreen else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // 4. Pronunciation & TTS Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔊 Hindi Speech (Text-To-Speech)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Native Android Hindi TTS engine for authentic Devanagari audio pronunciation.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onTestTts("नमस्ते! अंकशब्द में आपका स्वागत है। अंक से शब्द, शब्द से ज्ञान।") },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("🔊 Test Hindi Speech")
                    }
                }
            }
        }

        // 5. App & Database Stats Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ℹ️ About AnkShabd",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Text("App: AnkShabd — अंक से शब्द, शब्द से ज्ञान", fontSize = 13.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("Version: 1.0.0 (Production Release)", fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Platform: Android Jetpack Compose + SQLite", fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    coverageStats?.let { stats ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Seed Peg Words: ${stats.totalWords}", fontSize = 12.sp, lineHeight = 18.sp)
                        Text("Two-Digit 00-99 Coverage: ${stats.coveredCodesCount} / 100 (${stats.coveragePercentage.toInt()}%)", fontSize = 12.sp, lineHeight = 18.sp)
                        Text("Approved Pegs: ${stats.gradeACount + stats.gradeBCount}", fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
            }
        }
    }
}
