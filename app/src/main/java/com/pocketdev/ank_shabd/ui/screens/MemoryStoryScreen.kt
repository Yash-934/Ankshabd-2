package com.pocketdev.ank_shabd.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketdev.ank_shabd.data.repository.GeneratedStoryPackage
import com.pocketdev.ank_shabd.domain.models.MemoryStory
import com.pocketdev.ank_shabd.domain.models.PegWord
import com.pocketdev.ank_shabd.ui.components.CodeBadge
import com.pocketdev.ank_shabd.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryStoryScreen(
    initialSequence: String = "1857",
    savedStories: List<MemoryStory>,
    onGenerateStory: (String, (GeneratedStoryPackage) -> Unit) -> Unit,
    onSaveStory: (MemoryStory) -> Unit,
    onExportToNote: (MemoryStory) -> Unit,
    onDeleteStory: (String) -> Unit,
    onSpeakWord: (String) -> Unit,
    onNavigateToWordLab: (String) -> Unit
) {
    val context = LocalContext.current
    var inputSeq by remember { mutableStateOf(initialSequence) }
    var storyPackage by remember { mutableStateOf<GeneratedStoryPackage?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Studio/Cinema, 1 = Saved Stories
    var currentCinemaIndex by remember { mutableStateOf(0) }
    var isGenerating by remember { mutableStateOf(false) }
    var copyNotice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialSequence) {
        if (initialSequence.isNotBlank()) {
            inputSeq = initialSequence
            isGenerating = true
            onGenerateStory(initialSequence) { pkg ->
                storyPackage = pkg
                isGenerating = false
                currentCinemaIndex = 0
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title Row
        Text(
            text = "स्मृति सिनेमा | Memory Cinema",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Transform number sequences into vivid mental stories",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("कथा स्टूडियो (Studio)") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("सुरक्षित कथाएँ (${savedStories.size})") })
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Input Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Enter Number Sequence", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Continuous numbers (e.g. 1857, 111828) or hyphenated (e.g. 11-18-28)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = inputSeq,
                                    onValueChange = { inputSeq = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("e.g. 1857") },
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                                )

                                Button(
                                    onClick = {
                                        if (inputSeq.isNotBlank()) {
                                            isGenerating = true
                                            onGenerateStory(inputSeq) { pkg ->
                                                storyPackage = pkg
                                                isGenerating = false
                                                currentCinemaIndex = 0
                                            }
                                        }
                                    },
                                    enabled = !isGenerating && inputSeq.isNotBlank(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(if (isGenerating) "..." else "Generate")
                                }
                            }

                            // Quick preset samples
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Samples:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                listOf("1857", "11-18-28", "23-19-04", "110-211").forEach { sample ->
                                    Text(
                                        text = sample,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                            .clickable {
                                                inputSeq = sample
                                                isGenerating = true
                                                onGenerateStory(sample) { pkg ->
                                                    storyPackage = pkg
                                                    isGenerating = false
                                                    currentCinemaIndex = 0
                                                }
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                storyPackage?.let { pkg ->
                    // Slot Mapping Chips
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Peg Word Sequence Chunks", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(pkg.segmentMapping) { (seg, peg) ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (peg != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                            modifier = Modifier.clickable {
                                                if (peg != null) {
                                                    onSpeakWord(peg.word)
                                                } else {
                                                    onNavigateToWordLab(seg)
                                                }
                                            }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                CodeBadge(code = seg)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = peg?.word ?: "[रिक्त $seg]",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = if (peg != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                                )
                                                if (peg != null) {
                                                    Text(
                                                        text = peg.meaningEn.take(15),
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                    )
                                                } else {
                                                    Text(
                                                        text = "Add peg in Lab →",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (pkg.missingSlots.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "⚠️ Codes without verified pegs: ${pkg.missingSlots.joinToString(", ")}. Tap slot to find or add candidate.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Cinema Player
                    item {
                        val validWords = pkg.segmentMapping.mapNotNull { it.second }
                        if (validWords.isNotEmpty()) {
                            val activeIndex = currentCinemaIndex.coerceIn(0, validWords.size - 1)
                            val currentPeg = validWords[activeIndex]

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = VedicNavy
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🎬 CINEMA SCENE ${activeIndex + 1} / ${validWords.size}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AmberGold
                                        )
                                        IconButton(
                                            onClick = { onSpeakWord(currentPeg.word) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Text("🔊", fontSize = 16.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    CodeBadge(code = currentPeg.code)

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = currentPeg.word,
                                        fontSize = 32.sp,
                                        lineHeight = 46.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        text = currentPeg.meaningHi,
                                        fontSize = 15.sp,
                                        lineHeight = 23.sp,
                                        color = Color.White.copy(alpha = 0.9f),
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        text = "(${currentPeg.meaningEn})",
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp,
                                        color = AmberGold
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = currentPeg.visualScene.ifBlank { "कल्पना करें कि यह वस्तु आश्चर्यजनक रूप से बड़ी होकर चमक रही है!" },
                                        fontSize = 13.sp,
                                        lineHeight = 21.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Player Controls
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = {
                                                if (currentCinemaIndex > 0) currentCinemaIndex--
                                            },
                                            enabled = activeIndex > 0,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f))
                                        ) {
                                            Text("◀ Previous", color = Color.White)
                                        }

                                        Text(
                                            text = "${activeIndex + 1} of ${validWords.size}",
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )

                                        Button(
                                            onClick = {
                                                if (currentCinemaIndex < validWords.size - 1) currentCinemaIndex++
                                            },
                                            enabled = activeIndex < validWords.size - 1,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f))
                                        ) {
                                            Text("Next ▶", color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Complete Story Text & Export Options
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = pkg.story.storyTitle,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = pkg.story.storyHi,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onSaveStory(pkg.story)
                                            copyNotice = "Story saved to collection!"
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("💾 Save")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            onExportToNote(pkg.story)
                                            copyNotice = "Exported as smart note!"
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("📝 Note")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cm.setPrimaryClip(ClipData.newPlainText("Memory Story", "${pkg.story.storyTitle}\n\n${pkg.story.storyHi}"))
                                            copyNotice = "Copied to clipboard!"
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("📋 Copy")
                                    }
                                }

                                copyNotice?.let { notice ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = notice,
                                        fontSize = 12.sp,
                                        color = EmeraldGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Saved Stories Tab
            if (savedStories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("कोई सुरक्षित कथा नहीं है | No saved stories", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(savedStories) { story ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = story.storyTitle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    IconButton(
                                        onClick = { onDeleteStory(story.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("🗑", fontSize = 13.sp)
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Seq: ${story.numberSequence}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("•", fontSize = 12.sp)
                                    Text(story.pegWords.joinToString(" → "), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = story.storyHi.take(120) + if (story.storyHi.length > 120) "..." else "",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { onExportToNote(story) }) {
                                        Text("Export to Note →")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
