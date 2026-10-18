package com.pocketdev.ank_shabd.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketdev.ank_shabd.ai.GeminiAiProvider
import com.pocketdev.ank_shabd.ai.RejectedCandidateAudit
import com.pocketdev.ank_shabd.domain.encoder.PhoneticMatraEncoder
import com.pocketdev.ank_shabd.domain.generator.SegmentedNumberBreakdown
import com.pocketdev.ank_shabd.domain.models.PegWord
import com.pocketdev.ank_shabd.domain.models.SeedDictionary
import com.pocketdev.ank_shabd.ui.components.CodeBadge
import com.pocketdev.ank_shabd.ui.components.ComponentTraceVisual
import com.pocketdev.ank_shabd.ui.components.GradeBadge
import com.pocketdev.ank_shabd.ui.components.VerificationStatusBadge
import com.pocketdev.ank_shabd.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordLabScreen(
    initialCode: String = "",
    searchResults: List<PegWord>,
    isGenerating: Boolean = false,
    statusMessageHi: String? = null,
    segmentedBreakdown: SegmentedNumberBreakdown? = null,
    rejectedAiCandidates: List<RejectedCandidateAudit> = emptyList(),
    onSearchCode: (String) -> Unit,
    onDiscoverCandidates: (String) -> Unit,
    onApproveCandidate: (PegWord, String) -> Unit,
    onRejectCandidate: (PegWord, String) -> Unit,
    onAddNewCandidate: (String, String, String, String) -> Unit,
    onUpdateDictionaryFolder: (PegWord, String) -> Unit,
    onWordClick: (PegWord) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    onSpeakWord: (String) -> Unit,
    onNavigateToStory: (String) -> Unit
) {
    var codeInput by remember { mutableStateOf(initialCode) }
    var selectedMode by remember { mutableStateOf(0) } // 0 = Mode A (Verified & Generated), 1 = Mode B (AI Candidate Lab)
    var selectedGradeFilter by remember { mutableStateOf("ALL") } // "ALL", "A", "B", "Unverified"
    var showAddDialog by remember { mutableStateOf(false) }
    var showRejectedAudit by remember { mutableStateOf(false) }
    var candidateInputWord by remember { mutableStateOf("") }
    var candidateMeaningHi by remember { mutableStateOf("") }
    var candidateMeaningEn by remember { mutableStateOf("") }
    var candidateFolder by remember { mutableStateOf("सामान्य") }
    var selectedDictionaryFolder by remember { mutableStateOf("ALL") }
    var folderEditWord by remember { mutableStateOf<PegWord?>(null) }
    var folderEditName by remember { mutableStateOf("") }

    LaunchedEffect(initialCode) {
        if (initialCode.isNotBlank()) {
            val clean = GeminiAiProvider.sanitizeNumberInput(initialCode)
            codeInput = clean
            onSearchCode(clean)
        }
    }

    val dictionaryFolders = searchResults.map { it.category }.distinct().sorted()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "शब्द प्रयोगशाला | Word Lab",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "संख्या से सार्थक हिंदी शब्द, V2.3 कोडिंग एवं दृश्य कहानी",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_custom_word_button")
            ) {
                Text("➕", fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Mode Selector: Mode A vs Mode B
        TabRow(
            selectedTabIndex = selectedMode,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedMode == 0,
                onClick = { selectedMode = 0 },
                text = { Text("मोड A: सर्वश्रेष्ठ एवं स्वीकृत पेग", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedMode == 1,
                onClick = {
                    selectedMode = 1
                    if (codeInput.isNotBlank()) onDiscoverCandidates(codeInput)
                },
                text = { Text("मोड B: AI उम्मीदवार खोजें", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Number/Code Input Field + Generate Word Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = codeInput,
                onValueChange = { raw ->
                    val sanitized = GeminiAiProvider.sanitizeNumberInput(raw)
                    codeInput = sanitized
                    onSearchCode(sanitized)
                },
                label = { Text("संख्या दर्ज करें (उदा. 11, 24, 42, 185, 1947, 1001)") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("number_input_field"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                trailingIcon = {
                    if (codeInput.isNotBlank()) {
                        IconButton(onClick = {
                            codeInput = ""
                            onSearchCode("")
                        }) {
                            Text("✕", fontSize = 14.sp)
                        }
                    }
                }
            )

            Button(
                onClick = {
                    if (codeInput.isNotBlank()) {
                        onDiscoverCandidates(codeInput)
                    } else {
                        onSearchCode("")
                    }
                },
                enabled = !isGenerating,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(56.dp)
                    .testTag("generate_word_button")
            ) {
                Text(
                    text = if (isGenerating) "खोज जारी..." else "शब्द बनाएँ\nGenerate",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Code Presets (includes all mandatory user examples)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val presets = listOf(
                "11", "24", "42", "185", "111", "110",
                "1947", "2026", "356", "1001", "04", "18", "23", "28", "57"
            )
            items(presets) { code ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (codeInput == code) AmberCode else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .testTag("preset_chip_$code")
                        .clickable {
                            codeInput = code
                            onSearchCode(code)
                            if (selectedMode == 1) onDiscoverCandidates(code)
                        }
                ) {
                    Text(
                        text = code,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (codeInput == code) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filters Row (Grade & Folder)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val filters = listOf("ALL" to "सभी", "A" to "A-स्तर", "B" to "B-स्तर", "Unverified" to "उम्मीदवार")
            items(filters) { (key, label) ->
                FilterChip(
                    selected = selectedGradeFilter == key,
                    onClick = { selectedGradeFilter = key },
                    label = { Text(label, fontSize = 11.sp) }
                )
            }
            if (dictionaryFolders.size > 1) {
                item {
                    FilterChip(
                        selected = selectedDictionaryFolder == "ALL",
                        onClick = { selectedDictionaryFolder = "ALL" },
                        label = { Text("📁 सभी फ़ोल्डर", fontSize = 11.sp) }
                    )
                }
                items(dictionaryFolders) { folder ->
                    FilterChip(
                        selected = selectedDictionaryFolder == folder,
                        onClick = { selectedDictionaryFolder = folder },
                        label = { Text(folder, fontSize = 11.sp, maxLines = 1) }
                    )
                }
            }
        }

        if (isGenerating) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "🤖 Gemini AI से अर्थपूर्ण हिंदी शब्दों की खोज और V2.3 एनकोडर से सख्त जाँच जारी है...",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }

        // Filtered & Validated Results
        val filteredResults = remember(searchResults, codeInput, selectedGradeFilter, selectedDictionaryFolder) {
            searchResults.filter { peg ->
                val matchesCode = codeInput.isBlank() || peg.code == codeInput
                val matchesGrade = when (selectedGradeFilter) {
                    "A" -> peg.qualityGrade == "A"
                    "B" -> peg.qualityGrade == "B"
                    "Unverified" -> peg.qualityGrade == "Unverified" || peg.verificationStatus == "unverified"
                    else -> true
                }
                val matchesFolder = selectedDictionaryFolder == "ALL" || peg.category == selectedDictionaryFolder
                matchesCode && matchesGrade && matchesFolder
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. Live Available Alphabets, Matras & Words Palette for the entered number
            if (codeInput.isNotBlank()) {
                item {
                    com.pocketdev.ank_shabd.ui.components.NumberAvailableBlocksCard(
                        targetCode = codeInput,
                        availableMatchingWords = filteredResults,
                        isGenerating = isGenerating,
                        onGenerateWithAi = { onDiscoverCandidates(codeInput) },
                        onWordClick = { peg -> onWordClick(peg) }
                    )
                }
            }

            // Status Banner when a specific number is entered
            if (codeInput.isNotBlank() && !statusMessageHi.isNullOrBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = statusMessageHi,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { onDiscoverCandidates(codeInput) },
                                enabled = !isGenerating
                            ) {
                                Text("✨ AI से और शब्द बनाएँ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Show all matching candidates with all 9 required fields!
            if (filteredResults.isNotEmpty()) {
                items(filteredResults.size) { index ->
                    val peg = filteredResults[index]
                    val isBestMatch = index == 0 && codeInput.isNotBlank()
                    GeneratedPegResultCard(
                        peg = peg,
                        enteredNumber = codeInput.ifBlank { peg.code },
                        isBestMatchHero = isBestMatch,
                        showVerificationActions = peg.verificationStatus == "unverified" || selectedMode == 1,
                        onWordClick = { onWordClick(peg) },
                        onSpeakWord = { onSpeakWord(peg.word) },
                        onFavoriteToggle = { onFavoriteToggle(peg.id) },
                        onFolderEdit = {
                            folderEditWord = peg
                            folderEditName = peg.category
                        },
                        onApproveCandidate = { grade -> onApproveCandidate(peg, grade) },
                        onRejectCandidate = { onRejectCandidate(peg, "अस्वीकृत") }
                    )
                }
            } else if (codeInput.isNotBlank() && !isGenerating) {
                // Honest Empty State for single word + prompt to trigger AI or view segmented chain
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🔍", fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "संख्या '$codeInput' के लिए अभी कोई एकल स्वीकृत शब्द सहेजा नहीं गया है",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "V2.3 नियमों के अनुसार किसी भी संख्या के लिए मनमाना अर्थहीन अक्षर-जोड़ नहीं दिखाया जाता। Gemini AI से वास्तविक हिंदी शब्द खोजें या नीचे दी गई V2.3 खंडशः पेग श्रृंखला का उपयोग करें:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onDiscoverCandidates(codeInput) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("✨ Gemini AI से शब्द बनाएँ", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        candidateInputWord = ""
                                        showAddDialog = true
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("➕ अपना शब्द जोड़ें", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Segmented Mnemonic Breakdown Card (for 2+ digit numbers like 1947, 2026, 356, 1001, or empty slots)
            if (codeInput.length >= 2 && segmentedBreakdown != null && segmentedBreakdown.enteredNumber == codeInput) {
                item {
                    SegmentedBreakdownCard(
                        breakdown = segmentedBreakdown,
                        onSpeakWord = onSpeakWord,
                        onNavigateToStory = { onNavigateToStory(codeInput) }
                    )
                }
            }

            // Rejected AI Candidates Audit Drawer (proves deterministic V2.3 verification)
            if (codeInput.isNotBlank() && rejectedAiCandidates.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = RedRejectedContainer.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showRejectedAudit = !showRejectedAudit },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🛡️ V2.3 एनकोडर ऑडिट: ${rejectedAiCandidates.size} अस्वीकृत AI शब्द",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RedRejected
                                )
                                Text(
                                    text = if (showRejectedAudit) "▲ छिपाएँ" else "▼ कारण देखें",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RedRejected
                                )
                            }
                            if (showRejectedAudit) {
                                Spacer(modifier = Modifier.height(8.dp))
                                rejectedAiCandidates.forEach { audit ->
                                    Text(
                                        text = "• '${audit.candidateWord}' → निकला कोड '${audit.calculatedCode}' (${audit.traceSummary}) — ${audit.reasonHi}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Folder Rename Dialog
    folderEditWord?.let { peg ->
        AlertDialog(
            onDismissRequest = { folderEditWord = null },
            title = { Text("शब्दकोश फ़ोल्डर बदलें") },
            text = {
                OutlinedTextField(
                    value = folderEditName,
                    onValueChange = { folderEditName = it },
                    label = { Text("फ़ोल्डर का नाम") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = folderEditName.isNotBlank(),
                    onClick = {
                        onUpdateDictionaryFolder(peg, folderEditName)
                        folderEditWord = null
                    }
                ) { Text("सहेजें") }
            },
            dismissButton = {
                TextButton(onClick = { folderEditWord = null }) { Text("रद्द करें") }
            }
        )
    }

    // Add Candidate Word Dialog
    if (showAddDialog) {
        val calculatedEncoding = if (candidateInputWord.isNotBlank()) {
            PhoneticMatraEncoder.encodeSingleWordCandidate(candidateInputWord)
        } else null

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("नया पेग शब्द जोड़ें (V2.3 जाँच सहित)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = candidateInputWord,
                        onValueChange = { candidateInputWord = it },
                        label = { Text("हिंदी शब्द (उदा. कमल, खेल, कुहुक)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = candidateFolder,
                        onValueChange = { candidateFolder = it },
                        label = { Text("शब्दकोश फ़ोल्डर / श्रेणी") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (calculatedEncoding != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (calculatedEncoding.isValid) MaterialTheme.colorScheme.surfaceVariant else RedRejectedContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("V2.3 निकला कोड:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    CodeBadge(code = calculatedEncoding.code)
                                }
                                Text(
                                    text = calculatedEncoding.formattedTraceSummary(),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!calculatedEncoding.isValid) {
                                    calculatedEncoding.warnings.forEach { w ->
                                        Text("⚠️ $w", fontSize = 11.sp, color = RedRejected)
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = candidateMeaningHi,
                        onValueChange = { candidateMeaningHi = it },
                        label = { Text("सरल हिंदी अर्थ") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = calculatedEncoding?.isValid == true && calculatedEncoding.code.isNotBlank(),
                    onClick = {
                        if (candidateInputWord.isNotBlank()) {
                            onAddNewCandidate(
                                candidateInputWord.trim(),
                                candidateMeaningHi.trim(),
                                "",
                                candidateFolder.ifBlank { "सामान्य" }
                            )
                            showAddDialog = false
                            candidateInputWord = ""
                            candidateMeaningHi = ""
                            candidateMeaningEn = ""
                            candidateFolder = "सामान्य"
                        }
                    }
                ) {
                    Text("सहेजें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

/**
 * Displays a generated/verified Hindi word candidate with all 9 required fields from the specification:
 * 1. Entered number
 * 2. Best matching Hindi word
 * 3. Meaning of the word in simple Hindi
 * 4. Exact character-by-character encoding
 * 5. Final encoded number
 * 6. Why the word is a good mnemonic peg
 * 7. A vivid, memorable mental image or short Hindi story
 * 8. Word quality: A, B, or unverified
 * 9. Verification status
 */
@Composable
fun GeneratedPegResultCard(
    peg: PegWord,
    enteredNumber: String,
    isBestMatchHero: Boolean,
    showVerificationActions: Boolean,
    onWordClick: () -> Unit,
    onSpeakWord: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onFolderEdit: () -> Unit,
    onApproveCandidate: (String) -> Unit,
    onRejectCandidate: () -> Unit
) {
    val encResult = remember(peg.word) { PhoneticMatraEncoder.encode(peg.word) }
    val whyGoodPeg = remember(peg) {
        peg.dictionaryEvidence?.takeIf { it.isNotBlank() }
            ?: SeedDictionary.buildWhyGoodPeg(
                word = peg.word,
                code = encResult.code,
                category = peg.category,
                meaningHi = peg.meaningHi,
                qualityGrade = peg.qualityGrade
            )
    }
    val vividStory = remember(peg) {
        peg.mnemonicStory?.takeIf { it.isNotBlank() }
            ?: SeedDictionary.buildVividMnemonicStory(
                word = peg.word,
                code = encResult.code,
                meaningHi = peg.meaningHi,
                visualAssociation = peg.visualAssociation
            )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(if (isBestMatchHero) "best_matching_word_card" else "candidate_word_card_${peg.word}")
            .clickable { onWordClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isBestMatchHero) 1.5.dp else 1.dp,
            color = if (isBestMatchHero) AmberCode.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isBestMatchHero) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isBestMatchHero) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AmberCodeContainer,
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Text(
                        text = "🏆 सर्वश्रेष्ठ सार्थक हिंदी शब्द",
                        color = AmberCode,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Row 1: Word (2), Code Badges (1 & 5), Audio & Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = peg.word,
                        fontSize = if (isBestMatchHero) 26.sp else 22.sp,
                        lineHeight = if (isBestMatchHero) 36.sp else 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    CodeBadge(code = encResult.code, large = isBestMatchHero)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onSpeakWord, modifier = Modifier.size(36.dp)) {
                        Text("🔊", fontSize = 18.sp)
                    }
                    IconButton(onClick = onFavoriteToggle, modifier = Modifier.size(36.dp)) {
                        Text(if (peg.isFavorite) "★" else "☆", fontSize = 20.sp, color = AmberCode)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 8. Word Quality & 9. Verification Status + Folder
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GradeBadge(grade = peg.qualityGrade)
                VerificationStatusBadge(
                    status = peg.verificationStatus,
                    source = peg.verificationSource
                )
                Text(
                    text = "📁 ${peg.category}",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onFolderEdit() }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Entered Number & 5. Final Encoded Number verification strip
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = GreenApprovedContainer.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, GreenApproved.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "दर्ज संख्या: $enteredNumber",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = GreenApproved
                    )
                    Text(
                        text = "अंतिम निकला कोड: ${encResult.code} ✓",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = GreenApproved
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Meaning in simple Hindi
            Text(
                text = "हिंदी अर्थ: ${peg.meaningHi.ifBlank { "सार्थक हिंदी शब्द" }}",
                fontSize = 14.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Exact character-by-character encoding
            Text(
                text = "अक्षर-दर-अक्षर कोडिंग (V2.3):",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            ComponentTraceVisual(traces = encResult.traces)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = encResult.formattedTraceSummary(),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 6. Why the word is a good mnemonic peg
            Text(
                text = "🎯 पेग उपयुक्तता: $whyGoodPeg",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 7. Vivid mental image or short Hindi story
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🎬 मानसिक चित्र / कहानी: $vividStory",
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }

            // Candidate Verification Actions if in Mode B or Unverified
            if (showVerificationActions) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onApproveCandidate("A") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenApproved)
                    ) {
                        Text("A-स्वीकृत करें", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { onApproveCandidate("B") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("B-स्वीकृत करें", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = onRejectCandidate,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedRejected)
                    ) {
                        Text("खारिज", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SegmentedBreakdownCard(
    breakdown: SegmentedNumberBreakdown,
    onSpeakWord: (String) -> Unit,
    onNavigateToStory: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("segmented_breakdown_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✨ ${breakdown.wordCountUsed} सार्थक शब्दों में समाधान (${breakdown.enteredNumber})",
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                CodeBadge(code = breakdown.enteredNumber)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progressive 1 -> 2 -> 3 ... attempt log
            if (breakdown.attemptSteps.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🔄 चरणबद्ध प्रयास (पहले 1 शब्द ➔ फिर 2 शब्द ➔ ...):",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        breakdown.attemptSteps.forEach { step ->
                            Text(
                                text = step.summaryHi,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontWeight = if (step.succeeded) FontWeight.Bold else FontWeight.Normal,
                                color = if (step.succeeded) GreenApproved else OrangeWarning
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = breakdown.whySegmentedExplanationHi,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Segment chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(breakdown.segments) { seg ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable { onSpeakWord(seg.word) }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = seg.word,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    lineHeight = 24.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                CodeBadge(code = seg.segmentCode)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = seg.meaningHi,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = seg.traceSummary,
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "पेग श्रृंखला: ${breakdown.combinedWordChain}",
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🎬 संयुक्त स्मृति कहानी: ${breakdown.combinedVividStoryHi}",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onNavigateToStory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🎬 स्मृति सिनेमा में पूर्ण दृश्य कथा देखें", fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}
