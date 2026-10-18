package com.pocketdev.ank_shabd.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketdev.ank_shabd.domain.encoder.PhoneticMatraEncoder
import com.pocketdev.ank_shabd.domain.models.PegWord
import com.pocketdev.ank_shabd.domain.models.SeedDictionary
import com.pocketdev.ank_shabd.ui.components.CodeBadge
import com.pocketdev.ank_shabd.ui.components.ComponentTraceVisual
import com.pocketdev.ank_shabd.ui.components.GradeBadge
import com.pocketdev.ank_shabd.ui.components.VerificationStatusBadge
import com.pocketdev.ank_shabd.ui.theme.AmberCode
import com.pocketdev.ank_shabd.ui.theme.GreenApproved
import com.pocketdev.ank_shabd.ui.theme.GreenApprovedContainer

@Composable
fun WordDetailsScreen(
    peg: PegWord,
    onBackClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onSpeakClick: () -> Unit,
    onNavigateToTranslate: (String) -> Unit,
    onNavigateToNewNote: (String, String) -> Unit
) {
    val scrollState = rememberScrollState()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Back Button & Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Text("←", fontSize = 22.sp)
            }
            Text(
                text = "शब्द विवरण | Word Details (V2.3)",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header with word, code, and actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "2. सर्वश्रेष्ठ हिंदी शब्द (Hindi Word)",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = peg.word,
                            fontSize = 32.sp,
                            lineHeight = 46.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (peg.transliteration.isNotBlank()) {
                            Text(
                                text = peg.transliteration,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = onSpeakClick) {
                            Text("🔊", fontSize = 20.sp)
                        }
                        IconButton(onClick = onFavoriteToggle) {
                            Text(if (peg.isFavorite) "★" else "☆", fontSize = 22.sp, color = AmberCode)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 1. Entered Number & 5. Final Encoded Number
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GreenApprovedContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("1. दर्ज संख्या (Entered Number)", fontSize = 11.sp, color = GreenApproved)
                            Text(
                                text = peg.code,
                                fontSize = 20.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = GreenApproved
                            )
                        }
                        Text("➔", fontSize = 18.sp, color = GreenApproved)
                        Column(horizontalAlignment = Alignment.End) {
                            Text("5. अंतिम निकला कोड (Encoded Number)", fontSize = 11.sp, color = GreenApproved)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CodeBadge(code = encResult.code, large = false)
                                if (encResult.code == peg.code) {
                                    Text("✓ सटीक मेल", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenApproved)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 8. Word Quality & 9. Verification Status
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GradeBadge(grade = peg.qualityGrade)
                    VerificationStatusBadge(
                        status = peg.verificationStatus,
                        source = peg.verificationSource
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = peg.category,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // 3. Meaning in simple Hindi
                Text("3. सरल हिंदी अर्थ:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = peg.meaningHi.ifBlank { "अर्थ दर्ज नहीं" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Exact character-by-character encoding
                Text("4. अक्षर-दर-अक्षर कोडिंग:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                ComponentTraceVisual(traces = encResult.traces)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = encResult.formattedTraceSummary(),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 6. Why the word is a good mnemonic peg
                Text("6. यह अच्छा मेमोरी पेग क्यों है?:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🎯 $whyGoodPeg",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 7. Vivid mental image or short Hindi story
                Text("7. जीवंत मानसिक चित्र / छोटी हिंदी कहानी:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (!peg.visualAssociation.isNullOrBlank()) {
                            Text(
                                text = "🖼️ दृश्य संकेत: ${peg.visualAssociation}",
                                fontSize = 12.sp,
                                color = AmberCode,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            text = "🎬 $vividStory",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { onNavigateToTranslate(peg.code) },
                modifier = Modifier.weight(1f)
            ) {
                Text("🔢 अंक ${peg.code} से शब्द बनाएँ")
            }

            OutlinedButton(
                onClick = { onNavigateToNewNote(peg.word, peg.code) },
                modifier = Modifier.weight(1f)
            ) {
                Text("📝 नोट में जोड़ें")
            }
        }
    }
}
