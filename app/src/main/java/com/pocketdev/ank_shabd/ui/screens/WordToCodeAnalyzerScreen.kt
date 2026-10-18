package com.pocketdev.ank_shabd.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketdev.ank_shabd.domain.encoder.ComponentTrace
import com.pocketdev.ank_shabd.domain.encoder.EncodingResult
import com.pocketdev.ank_shabd.domain.encoder.PhoneticMatraEncoder
import com.pocketdev.ank_shabd.domain.models.PegWord
import com.pocketdev.ank_shabd.ui.components.CodeBadge
import com.pocketdev.ank_shabd.ui.components.ComponentTraceVisual
import com.pocketdev.ank_shabd.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordToCodeAnalyzerScreen(
    initialInput: String = "दवा",
    onFindSavedWithCode: (String) -> List<PegWord>,
    onSaveWord: (String, String) -> Unit,
    onNavigateToTranslate: (String) -> Unit,
    onNavigateToNewNoteWithWord: (String, String) -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf(initialInput) }
    var selectedTraceDetail by remember { mutableStateOf<ComponentTrace?>(null) }
    var exportMessage by remember { mutableStateOf<String?>(null) }

    // Parse single word or multi-word input
    val words = remember(inputText) {
        inputText.lines().map { it.trim() }.filter { it.isNotBlank() }
    }

    val encodingResults = remember(words) {
        words.map { word -> PhoneticMatraEncoder.encode(word) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Text(
            text = "शब्द-से-कोड विश्लेषक | Word to Code",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "ध्वन्यात्मक एवं मात्रा संरचना का घटक-वार विश्लेषण",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Multi-line Input Box
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("हिंदी शब्द दर्ज करें (एक से अधिक शब्द नई पंक्ति में लिखें)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Examples Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("उदाहरण:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val examples = listOf("दवा", "खेल", "गेहूँ", "हंस", "शहर")
            examples.forEach { ex ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { inputText = ex }
                ) {
                    Text(
                        text = ex,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Export Actions Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val textToCopy = encodingResults.joinToString("\n") { "${it.originalWord} → ${it.code} (${it.formattedTraceSummary()})" }
                    copyToClipboard(context, "AnkShabd Analysis", textToCopy)
                    exportMessage = "क्लिपबोर्ड पर कॉपी किया गया!"
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("📋 कॉपी", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = {
                    val markdown = generateMarkdownTable(encodingResults)
                    copyToClipboard(context, "AnkShabd Markdown", markdown)
                    exportMessage = "Markdown तालिका कॉपी की गई!"
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("📄 Markdown", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = {
                    val csv = generateCsv(encodingResults)
                    copyToClipboard(context, "AnkShabd CSV", csv)
                    exportMessage = "CSV कॉपी किया गया!"
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("📊 CSV", fontSize = 11.sp)
            }
        }

        if (exportMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = exportMessage!!,
                color = GreenApproved,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Results Section
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(encodingResults) { res ->
                val sameCodeWords = remember(res.code) { onFindSavedWithCode(res.code) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Word and Code Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = res.originalWord,
                                fontSize = 24.sp,
                                lineHeight = 35.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("कोड:", fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                CodeBadge(code = res.code, large = true)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Visual Sequence String
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = res.visualSequence(),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Component Trace
                        Text("घटक विभाजन (घटक पर टैप करके नियम देखें):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ComponentTraceVisual(
                            traces = res.traces,
                            onTraceClick = { selectedTraceDetail = it }
                        )

                        // Warnings if any
                        if (res.warnings.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = RedRejectedContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    res.warnings.forEach { warn ->
                                        Text("⚠️ $warn", color = RedRejected, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        // Same code saved words
                        if (sameCodeWords.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "समान कोड वाले अन्य शब्द: ${sameCodeWords.joinToString(", ") { it.word }}",
                                fontSize = 12.sp,
                                color = AmberCode,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons: Save, Translate, Add to Note
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onSaveWord(res.originalWord, res.code) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("शब्द सहेजें", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { onNavigateToTranslate(res.code) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("अंक ${res.code} से शब्द", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { onNavigateToNewNoteWithWord(res.originalWord, res.code) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("नोट्स में जोड़ें", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Component Trace Rule Inspector Dialog
    if (selectedTraceDetail != null) {
        val trace = selectedTraceDetail!!
        AlertDialog(
            onDismissRequest = { selectedTraceDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(trace.character, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    if (trace.digitEmitted != null) CodeBadge(code = trace.digitEmitted)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("लागू नियम:", fontWeight = FontWeight.Bold)
                    Text(trace.ruleApplied, fontSize = 14.sp)
                    Text("घटक प्रकार: ${trace.componentType}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (trace.warningMessage != null) {
                        Text("चेतावनी: ${trace.warningMessage}", color = RedRejected, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedTraceDetail = null }) {
                    Text("ठीक है")
                }
            }
        )
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}

private fun generateMarkdownTable(results: List<EncodingResult>): String {
    val sb = StringBuilder()
    sb.append("| शब्द | निकला कोड | घटक कोडिंग | स्थिति |\n")
    sb.append("| --- | --- | --- | --- |\n")
    results.forEach { r ->
        val status = if (r.isValid) "वैध (Valid)" else "चेतावनी (Warning)"
        sb.append("| ${r.originalWord} | ${r.code} | ${r.formattedTraceSummary()} | $status |\n")
    }
    return sb.toString()
}

private fun generateCsv(results: List<EncodingResult>): String {
    val sb = StringBuilder()
    sb.append("Word,Code,Trace,Valid\n")
    results.forEach { r ->
        sb.append("\"${r.originalWord}\",\"${r.code}\",\"${r.formattedTraceSummary()}\",${r.isValid}\n")
    }
    return sb.toString()
}
