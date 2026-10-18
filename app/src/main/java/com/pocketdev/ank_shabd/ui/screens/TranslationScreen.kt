package com.pocketdev.ank_shabd.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketdev.ank_shabd.ai.GeminiAiProvider
import com.pocketdev.ank_shabd.domain.encoder.PhoneticMatraEncoder
import com.pocketdev.ank_shabd.domain.models.SeedDictionary
import com.pocketdev.ank_shabd.domain.models.TranslationRecord
import com.pocketdev.ank_shabd.ui.components.CodeBadge
import com.pocketdev.ank_shabd.ui.components.ComponentTraceVisual
import com.pocketdev.ank_shabd.ui.components.NumberAvailableBlocksCard
import com.pocketdev.ank_shabd.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationScreen(
    initialText: String = "",
    savedTranslations: List<TranslationRecord>,
    onTranslate: (String, String, String) -> Unit,
    currentTranslation: TranslationRecord?,
    translationError: String? = null,
    onSaveToNotes: (TranslationRecord) -> Unit,
    onSaveToDictionary: (String, String, String) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    onSpeakText: (String) -> Unit,
    onTranslationError: (String) -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf(GeminiAiProvider.sanitizeNumberInput(initialText).ifBlank { "185" }) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Number-to-Word Builder, 1 = Saved History
    var copyNotice by remember { mutableStateOf<String?>(null) }
    var isBuildingWords by remember { mutableStateOf(false) }

    val allSeedPegs = remember { SeedDictionary.getValidatedSeedPegs() }

    LaunchedEffect(initialText) {
        val clean = GeminiAiProvider.sanitizeNumberInput(initialText)
        if (clean.isNotBlank()) {
            inputText = clean
            isBuildingWords = true
            onTranslate(clean, "number", "hi")
        }
    }

    LaunchedEffect(currentTranslation, translationError) {
        isBuildingWords = false
    }

    val cleanCurrentNumber = remember(inputText) {
        GeminiAiProvider.sanitizeNumberInput(inputText)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Header (No Hindi-English language translator — strictly Number-to-Word Builder)
        Text(
            text = "संख्या → सार्थक शब्द अनुवादक",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "संख्या के उपलब्ध अक्षर (व्यंजन), मात्राएँ व शब्द देखें और AI से नियम अनुसार सार्थक शब्द बनाएँ",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Tab Row: Number-to-Word Builder vs Saved History
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("संख्या → शब्द निर्माण", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("सहेजे गए रिकॉर्ड (${savedTranslations.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (!translationError.isNullOrBlank()) {
                    item {
                        Text(
                            text = translationError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                // 1. Number Input Field & Presets
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { raw ->
                                    inputText = raw.filter { it.isDigit() || it in '०'..'९' || it == ' ' || it == ',' }
                                },
                                label = { Text("संख्या दर्ज करें (जैसे 11, 24, 42, 185, 1947, 1001)") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("number_translate_input"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                trailingIcon = {
                                    if (inputText.isNotBlank()) {
                                        IconButton(onClick = { inputText = "" }) {
                                            Text("✕", fontSize = 14.sp)
                                        }
                                    }
                                }
                            )

                            Button(
                                onClick = {
                                    if (cleanCurrentNumber.isNotBlank()) {
                                        onTranslationError("")
                                        isBuildingWords = true
                                        onTranslate(inputText, "number", "hi")
                                    }
                                },
                                enabled = cleanCurrentNumber.isNotBlank() && !isBuildingWords,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(56.dp)
                                    .testTag("number_translate_button")
                            ) {
                                Text(
                                    text = if (isBuildingWords) "निर्माण..." else "सार्थक शब्द\nबनाएँ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Quick Number Presets
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val presets = listOf(
                                "11", "24", "42", "185", "111", "110",
                                "1947", "2026", "356", "1001", "04", "18", "23", "28", "57"
                            )
                            items(presets) { code ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (cleanCurrentNumber == code) AmberCode else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable {
                                        inputText = code
                                        onTranslationError("")
                                        isBuildingWords = true
                                        onTranslate(code, "number", "hi")
                                    }
                                ) {
                                    Text(
                                        text = code,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (cleanCurrentNumber == code) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Live Available Alphabets (Consonants), Matras, Carrier Letters & Dictionary Words Card
                if (cleanCurrentNumber.isNotBlank()) {
                    item {
                        NumberAvailableBlocksCard(
                            targetCode = cleanCurrentNumber,
                            availableMatchingWords = allSeedPegs.filter { it.code == cleanCurrentNumber },
                            isGenerating = isBuildingWords,
                            onGenerateWithAi = {
                                onTranslationError("")
                                isBuildingWords = true
                                onTranslate(cleanCurrentNumber, "number", "hi")
                            },
                            onWordClick = { peg ->
                                onSpeakText(peg.word)
                            }
                        )
                    }
                }

                if (copyNotice != null) {
                    item {
                        Text(copyNotice!!, color = GreenApproved, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // 3. AI + V2.3 Rule-Verified Generated Hindi Words Output Card
                if (currentTranslation != null && currentTranslation.sourceLang == "number") {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("number_translation_result_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
                                            text = "✨ नियम-संगत सार्थक हिंदी शब्द परिणाम",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GreenApproved
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = currentTranslation.translatedText,
                                            fontSize = 21.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            softWrap = true
                                        )
                                    }

                                    Row {
                                        IconButton(onClick = { onSpeakText(currentTranslation.translatedText) }) {
                                            Text("🔊", fontSize = 18.sp)
                                        }
                                        IconButton(onClick = {
                                            copyToClipboard(context, "AnkShabd Word", currentTranslation.translatedText)
                                            copyNotice = "सार्थक शब्द कॉपी किया गया!"
                                        }) {
                                            Text("📋", fontSize = 16.sp)
                                        }
                                    }
                                }

                                // Progressive 1-word -> 2-word -> 3-word attempt log
                                if (currentTranslation.exampleSentences.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "🔄 चरणबद्ध शब्द निर्माण (पहले 1 शब्द ➔ न मिलने पर 2 शब्द ➔ 3 शब्द...):",
                                                fontSize = 12.sp,
                                                lineHeight = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            currentTranslation.exampleSentences.forEach { stepLog ->
                                                val isSuccess = stepLog.startsWith("✓")
                                                Text(
                                                    text = stepLog,
                                                    fontSize = 12.sp,
                                                    lineHeight = 18.sp,
                                                    fontWeight = if (isSuccess) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSuccess) GreenApproved else OrangeWarning
                                                )
                                            }
                                        }
                                    }
                                }

                                // Show character-by-character trace for each generated word in synonyms
                                if (currentTranslation.synonyms.isNotEmpty()) {
                                    Text(
                                        text = "प्रयुक्त अक्षर और मात्रा की V2.3 जाँच:",
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    currentTranslation.synonyms.take(6).forEach { validWord ->
                                        val enc = PhoneticMatraEncoder.encode(validWord)
                                        val seedMatch = allSeedPegs.find { it.normalizedWord == enc.normalizedWord }
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Text(
                                                            text = validWord,
                                                            fontSize = 19.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        CodeBadge(code = enc.code)
                                                    }
                                                    Text(
                                                        text = "✓ कोड सत्यापित",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = GreenApproved
                                                    )
                                                }
                                                if (seedMatch != null && seedMatch.meaningHi.isNotBlank()) {
                                                    Text(
                                                        text = "सरल हिंदी अर्थ: ${seedMatch.meaningHi}",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                ComponentTraceVisual(traces = enc.traces)
                                                if (seedMatch != null && !seedMatch.mnemonicStory.isNullOrBlank()) {
                                                    Text(
                                                        text = "🎬 दृश्य कहानी: ${seedMatch.mnemonicStory}",
                                                        fontSize = 12.sp,
                                                        color = AmberCode,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (currentTranslation.meaningHi.isNotBlank()) {
                                    Text(
                                        text = "विस्तृत अक्षर-मात्रा विश्लेषण व कहानी:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = currentTranslation.meaningHi,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onSaveToNotes(currentTranslation) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("📝 नोट्स में सहेजें", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            if (currentTranslation.synonyms.isNotEmpty()) {
                                                currentTranslation.synonyms.forEach { w ->
                                                    onSaveToDictionary(w, "$w (सार्थक शब्द)", "")
                                                }
                                            } else {
                                                currentTranslation.translatedText.lines()
                                                    .map { it.substringAfter("→", "").substringBefore("(").trim() }
                                                    .filter { it.isNotBlank() }
                                                    .forEach { word -> onSaveToDictionary(word, word, "") }
                                            }
                                            copyNotice = "शब्दकोश में सहेजा गया!"
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("📚 शब्दकोश में जोड़ें", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Saved Number-to-Word History Tab
            if (savedTranslations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("कोई सहेजा गया रिकॉर्ड नहीं है।", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(savedTranslations) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val digits = GeminiAiProvider.sanitizeNumberInput(item.sourceText)
                                    if (digits.isNotBlank()) {
                                        inputText = digits
                                        selectedTab = 0
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${item.sourceText} ➔ ${item.translatedText}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { onFavoriteToggle(item.id) }, modifier = Modifier.size(28.dp)) {
                                        Text(if (item.isFavorite) "★" else "☆", color = AmberCode)
                                    }
                                }
                                if (item.meaningHi.isNotBlank()) {
                                    Text(item.meaningHi, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}
