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
import com.pocketdev.ank_shabd.domain.models.*
import com.pocketdev.ank_shabd.ui.components.CodeBadge
import com.pocketdev.ank_shabd.ui.components.GradeBadge
import com.pocketdev.ank_shabd.ui.components.MarkdownSnippetPreview
import com.pocketdev.ank_shabd.ui.components.WordCard
import com.pocketdev.ank_shabd.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    coverageStats: CoverageStats?,
    recentPegs: List<PegWord>,
    recentNotes: List<Note>,
    universalSearchResults: UniversalSearchResult?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigateToWordLab: (String?) -> Unit,
    onNavigateToAnalyzer: () -> Unit,
    onNavigateToTranslate: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToMemoryStory: () -> Unit,
    onNavigateToMemoryGym: () -> Unit,
    onNavigateToWordDetails: (PegWord) -> Unit,
    onNavigateToNoteEditor: (Note) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    onSpeakWord: (String) -> Unit
) {
    var quickNumberInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // 1. Welcome & Branding Header
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "अंकशब्द",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "अंक से शब्द, शब्द से ज्ञान",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = AmberCode
                        )
                    }

                    // V2.3 Ruleset & Local status badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("●", color = GreenApproved, fontSize = 10.sp)
                            Text(
                                text = "V2.3 मानक",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 2. Hero Card: Instant Number-to-Hindi-Word Generator
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "✨ किसी भी संख्या से सार्थक हिंदी शब्द बनाएँ (V2.3)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "कोई भी अंक दर्ज करें — Gemini AI सार्थक शब्द सुझाएगा और ऐप का V2.3 एनकोडर उसका सटीक कोड प्रमाणित करेगा",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = quickNumberInput,
                            onValueChange = { quickNumberInput = GeminiAiProvider.sanitizeNumberInput(it) },
                            placeholder = { Text("उदा. 11, 24, 42, 185, 1947, 1001") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_number_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                onNavigateToWordLab(quickNumberInput.ifBlank { "11" })
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("home_generate_word_button")
                        ) {
                            Text("शब्द बनाएँ", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val heroPresets = listOf("11", "24", "42", "185", "111", "110", "1947", "2026", "356", "1001")
                        items(heroPresets) { num ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.clickable {
                                    quickNumberInput = num
                                    onNavigateToWordLab(num)
                                }
                            ) {
                                Text(
                                    text = num,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (quickNumberInput.isNotBlank()) {
            item {
                com.pocketdev.ank_shabd.ui.components.NumberAvailableBlocksCard(
                    targetCode = quickNumberInput,
                    availableMatchingWords = recentPegs.filter { it.code == quickNumberInput },
                    isGenerating = false,
                    onGenerateWithAi = { onNavigateToWordLab(quickNumberInput) },
                    onWordClick = { peg -> onNavigateToWordDetails(peg) }
                )
            }
        }

        // 3. Universal Search Box
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("अंक कोड, शब्द, अर्थ या नोट्स खोजें...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                leadingIcon = { Text("🔍", fontSize = 16.sp) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Text("✕", fontSize = 14.sp)
                        }
                    }
                }
            )
        }

        // Show Universal Search Results if search query is active
        if (searchQuery.isNotBlank() && universalSearchResults != null) {
            item {
                Text(
                    text = "खोज परिणाम (${universalSearchResults.totalCount})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (universalSearchResults.totalCount == 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("कोई मेल नहीं मिला", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "क्या आप इस अंक का नया शब्द बनाना चाहते हैं?",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(onClick = { onNavigateToWordLab(searchQuery) }) {
                                Text("शब्द प्रयोगशाला में शब्द बनाएँ")
                            }
                        }
                    }
                }
            } else {
                items(universalSearchResults.matchingPegs) { peg ->
                    WordCard(
                        peg = peg,
                        onClick = { onNavigateToWordDetails(peg) },
                        onFavoriteToggle = { onFavoriteToggle(peg.id) },
                        onSpeakClick = { onSpeakWord(peg.word) }
                    )
                }

                items(universalSearchResults.matchingNotes) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToNoteEditor(note) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "📝 ${note.title}",
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            MarkdownSnippetPreview(markdown = note.content, maxLines = 3)
                        }
                    }
                }
            }
        } else {
            // 4. Quick Action Hub
            item {
                Text(
                    text = "त्वरित उपकरण",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        title = "अंक से शब्द लैब",
                        subtitle = "सार्थक पेग एवं कहानी",
                        icon = "🔢",
                        badge = "शब्द लैब",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToWordLab(null) }
                    )

                    QuickActionCard(
                        title = "शब्द का कोड",
                        subtitle = "अक्षर-दर-अक्षर जाँच",
                        icon = "🔬",
                        badge = "विश्लेषक",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToAnalyzer
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        title = "संख्या → शब्द अनुवाद",
                        subtitle = "उपलब्ध अक्षर व मात्रा से AI शब्द",
                        icon = "🧩",
                        badge = "अक्षर+मात्रा",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToTranslate
                    )

                    QuickActionCard(
                        title = "ज्ञान नोटबुक",
                        subtitle = "स्मृति नोट्स व सूत्र",
                        icon = "📓",
                        badge = "नोट्स",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToNotes
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        title = "स्मृति सिनेमा",
                        subtitle = "संख्या श्रृंखला कहानी",
                        icon = "🎬",
                        badge = "कहानी",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToMemoryStory
                    )

                    QuickActionCard(
                        title = "अभ्यास जिम",
                        subtitle = "स्मृति पुनरीक्षण कार्ड",
                        icon = "🏋️",
                        badge = "अभ्यास",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToMemoryGym
                    )
                }
            }

            // 5. Dictionary Coverage Dashboard Card
            if (coverageStats != null) {
                item {
                    CoverageDashboardCard(coverageStats = coverageStats, onExploreDictionary = { onNavigateToWordLab(null) })
                }
            }

            // 6. Recent Saved Pegs
            if (recentPegs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "प्रमुख पेग शब्द",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = { onNavigateToWordLab(null) }) {
                            Text("सभी देखें", color = AmberCode)
                        }
                    }
                }

                items(recentPegs.take(5)) { peg ->
                    WordCard(
                        peg = peg,
                        onClick = { onNavigateToWordDetails(peg) },
                        onFavoriteToggle = { onFavoriteToggle(peg.id) },
                        onSpeakClick = { onSpeakWord(peg.word) }
                    )
                }
            }

            // 7. Recent Notes
            if (recentNotes.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "हाल के नोट्स",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = onNavigateToNotes) {
                            Text("नोट्स स्टूडियो", color = AmberCode)
                        }
                    }
                }

                items(recentNotes.take(3)) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToNoteEditor(note) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = note.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )
                                if (note.targetNumber != null) {
                                    CodeBadge(code = note.targetNumber)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            MarkdownSnippetPreview(markdown = note.content, maxLines = 3)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: String,
    badge: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(icon, fontSize = 24.sp, lineHeight = 30.sp)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = badge,
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CoverageDashboardCard(
    coverageStats: CoverageStats,
    onExploreDictionary: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExploreDictionary() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = IndigoPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "00–99 पेग कवरेज डैशबोर्ड",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${String.format("%.1f", coverageStats.coveragePercentage)}%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AmberCodeLight
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Honest coverage progress bar
            LinearProgressIndicator(
                progress = (coverageStats.coveragePercentage / 100f).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = AmberCodeLight,
                trackColor = IndigoLight
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CoverageStatItem(label = "A-स्तर स्वीकृत", count = coverageStats.totalApprovedA, color = GreenApprovedContainer)
                CoverageStatItem(label = "B-स्तर शब्द", count = coverageStats.totalB, color = OrangeWarningContainer)
                CoverageStatItem(label = "रिक्त स्लॉट", count = coverageStats.totalEmptySlots, color = RedRejectedContainer)
                CoverageStatItem(label = "अद्वितीय कोड", count = coverageStats.totalUniqueCodes, color = Color.White)
            }
        }
    }
}

@Composable
fun CoverageStatItem(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count.toString(), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = color)
        Text(text = label, fontSize = 10.sp, color = Color(0xFFCBD5E1))
    }
}
