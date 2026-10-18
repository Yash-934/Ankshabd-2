package com.pocketdev.ank_shabd.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketdev.ank_shabd.domain.encoder.ComponentTrace
import com.pocketdev.ank_shabd.domain.encoder.ComponentType
import com.pocketdev.ank_shabd.domain.encoder.PhoneticMatraEncoder
import com.pocketdev.ank_shabd.domain.models.PegWord
import com.pocketdev.ank_shabd.domain.models.SeedDictionary
import com.pocketdev.ank_shabd.ui.theme.*

@Composable
fun CodeBadge(
    code: String,
    modifier: Modifier = Modifier,
    large: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(if (large) 10.dp else 8.dp),
        color = AmberCode
    ) {
        Text(
            text = code.ifEmpty { "—" },
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = if (large) 18.sp else 14.sp,
            lineHeight = if (large) 24.sp else 20.sp,
            color = Color.White,
            modifier = Modifier.padding(
                horizontal = if (large) 14.dp else 10.dp,
                vertical = if (large) 6.dp else 4.dp
            )
        )
    }
}

@Composable
fun GradeBadge(
    grade: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (grade.uppercase()) {
        "A" -> Triple(GreenApprovedContainer, GreenApproved, "A-स्तर (उत्कृष्ट)")
        "B" -> Triple(OrangeWarningContainer, OrangeWarning, "B-स्तर (उपयोगी)")
        else -> Triple(Color(0xFFE2E8F0), LightTextSecondary, "असत्यापित (Unverified)")
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun VerificationStatusBadge(
    status: String,
    source: String = "",
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status.lowercase()) {
        "approved" -> Triple(
            GreenApprovedContainer,
            GreenApproved,
            if (source == "v2_3_seed") "✓ V2.3 प्रमाणित शब्दकोश" else "✓ सत्यापित (Approved)"
        )
        "rejected" -> Triple(RedRejectedContainer, RedRejected, "✕ अस्वीकृत (Rejected)")
        else -> Triple(
            OrangeWarningContainer,
            OrangeWarning,
            if (source.contains("gemini") || source.contains("ai")) "✨ AI निर्मित (एनकोडर द्वारा कोड-सत्यापित)" else "⏳ समीक्षाधीन (Unverified)"
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun ComponentTraceVisual(
    traces: List<ComponentTrace>,
    modifier: Modifier = Modifier,
    onTraceClick: ((ComponentTrace) -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            traces.forEach { trace ->
                val chipColor = when {
                    trace.isWarning -> RedRejectedContainer
                    trace.digitEmitted != null && (trace.componentType == ComponentType.MATRA || trace.componentType == ComponentType.DIACRITIC) ->
                        GreenApprovedContainer.copy(alpha = 0.85f)
                    trace.digitEmitted != null ->
                        MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }

                val textColor = when {
                    trace.isWarning -> RedRejected
                    trace.digitEmitted != null && (trace.componentType == ComponentType.MATRA || trace.componentType == ComponentType.DIACRITIC) ->
                        GreenApproved
                    trace.digitEmitted != null ->
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(chipColor)
                        .then(
                            if (onTraceClick != null) {
                                Modifier.clickable { onTraceClick(trace) }
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = trace.character,
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = trace.digitEmitted ?: "—",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (trace.digitEmitted != null) AmberCode else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            traces.forEach { trace ->
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "• '${trace.character}'",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.width(56.dp),
                        color = if (trace.isWarning) RedRejected else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "➔ ${trace.digitEmitted ?: "कोई अंक नहीं"}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.width(96.dp),
                        color = if (trace.digitEmitted != null) AmberCode else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "(${trace.ruleApplied})",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Displays the complete V2.3 Available Alphabets (Consonants), Matras/Vowels,
 * Carrier Letters, Combined Syllable Hints, and Available Dictionary Words for [targetCode],
 * with a direct action button so Gemini AI uses these exact building blocks to form meaningful Hindi words.
 */
@Composable
fun NumberAvailableBlocksCard(
    targetCode: String,
    availableMatchingWords: List<PegWord>,
    isGenerating: Boolean = false,
    onGenerateWithAi: () -> Unit,
    onWordClick: (PegWord) -> Unit,
    modifier: Modifier = Modifier
) {
    val cleanCode = targetCode.filter { it in '0'..'9' }
    if (cleanCode.isBlank()) return

    val palette = remember(cleanCode) { PhoneticMatraEncoder.buildNumberPalette(cleanCode) }
    val allSeedPegs = remember { SeedDictionary.getValidatedSeedPegs() }

    val exactWords = remember(cleanCode, availableMatchingWords) {
        (availableMatchingWords + allSeedPegs.filter { it.code == cleanCode })
            .distinctBy { it.normalizedWord }
    }

    val segmentHelperWords = remember(cleanCode) {
        if (cleanCode.length <= 2) {
            emptyList()
        } else {
            val segments = mutableListOf<String>()
            var idx = 0
            while (idx < cleanCode.length) {
                val end = (idx + 2).coerceAtMost(cleanCode.length)
                segments.add(cleanCode.substring(idx, end))
                idx = end
            }
            segments.distinct().mapNotNull { seg ->
                val matches = allSeedPegs.filter { it.code == seg }.take(3)
                if (matches.isNotEmpty()) seg to matches else null
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("available_blocks_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    Text(
                        text = "🧩 संख्या '$cleanCode' के उपलब्ध अक्षर, मात्राएँ एवं शब्द",
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "V2.3 मानक: प्रत्येक अंक के नियत व्यंजन और मात्राएँ (AI इन्हीं का उपयोग करके सार्थक शब्द बनाता है)",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                CodeBadge(code = cleanCode, large = true)
            }

            // 1. Digit-by-digit Available Consonants & Matras
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                palette.digitBlocks.forEach { block ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AmberCode
                                ) {
                                    Text(
                                        text = "अंक #${block.position} : ${block.digit}",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = block.usageHintHi,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Available Consonants
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "उपलब्ध व्यंजन: ",
                                    fontSize = 13.sp,
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = block.availableConsonants.joinToString("  •  "),
                                    fontSize = 15.sp,
                                    lineHeight = 23.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Available Matras & Independent Vowels
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "उपलब्ध मात्रा/स्वर: ",
                                    fontSize = 13.sp,
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenApproved
                                )
                                val matraAndVowelText = buildString {
                                    if (block.availableMatras.isNotEmpty()) {
                                        append(block.availableMatras.joinToString("  •  "))
                                    } else {
                                        append("कोई मात्रा नहीं (केवल व्यंजन)")
                                    }
                                    if (block.availableIndependentVowels.isNotEmpty()) {
                                        append("   |   स्वर: ${block.availableIndependentVowels.joinToString(", ")}")
                                    }
                                }
                                Text(
                                    text = matraAndVowelText,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                    color = if (block.availableMatras.isNotEmpty()) GreenApproved else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (block.carrierMatraExamples.isNotEmpty()) {
                                Text(
                                    text = "वाहक (य/व/ह/ल) + मात्रा: ${block.carrierMatraExamples.joinToString(", ")}",
                                    fontSize = 11.sp,
                                    lineHeight = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Combined Syllable Hints
            if (palette.readySyllableHints.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "⚡ अक्षर + मात्रा जोड़कर तैयार शब्दांश (2–3 अंक एक साथ):",
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = palette.readySyllableHints.joinToString("   •   "),
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 3. Free Carrier Consonants & Helpers
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🆓 सहायक वाहक अक्षर (इनका कोई व्यंजन-अंक नहीं जुड़ता — शब्द पूरा करने के लिए उपयोगी):",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "वाहक व्यंजन: य , व , ह , ल   |   बिना अंक के स्वर/चिह्न: अ , ँ (चंद्रबिंदु) , ् (हलंत)",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 4. Available Dictionary Words for this Number / Segments
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📚 इस संख्या ($cleanCode) के उपलब्ध सार्थक शब्द (${exactWords.size}):",
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenApproved
                    )
                    if (exactWords.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            exactWords.forEach { peg ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = GreenApprovedContainer.copy(alpha = 0.85f),
                                    border = BorderStroke(1.dp, GreenApproved.copy(alpha = 0.25f)),
                                    modifier = Modifier.clickable { onWordClick(peg) }
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = peg.word,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 16.sp,
                                                lineHeight = 24.sp,
                                                color = GreenApproved
                                            )
                                            Text(
                                                text = "[${peg.qualityGrade}]",
                                                fontSize = 11.sp,
                                                lineHeight = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GreenApproved
                                            )
                                        }
                                        Text(
                                            text = peg.meaningHi,
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp,
                                            color = Color(0xFF064E3B)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "पूर्ण संख्या '$cleanCode' के लिए शब्दकोश में अभी कोई पूर्व-सहेजा एकल शब्द नहीं है — नीचे बटन दबाकर इन उपलब्ध अक्षरों और मात्राओं से AI द्वारा नया सार्थक शब्द बनाएँ!",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = OrangeWarning
                        )
                    }

                    if (segmentHelperWords.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "🔗 खंडों (Segments) में उपलब्ध सार्थक शब्द:",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        segmentHelperWords.forEach { (segCode, pegs) ->
                            Text(
                                text = "• अंक '$segCode' ➔ ${pegs.joinToString(", ") { "${it.word} (${it.meaningHi})" }}",
                                fontSize = 12.sp,
                                lineHeight = 19.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 5. Action Button
            Button(
                onClick = onGenerateWithAi,
                enabled = !isGenerating,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("ai_build_from_blocks_button")
            ) {
                Text(
                    text = if (isGenerating) {
                        "⏳ उपलब्ध अक्षरों और मात्राओं से शब्द बनाए जा रहे हैं..."
                    } else {
                        "✨ इन उपलब्ध अक्षरों और मात्राओं से AI द्वारा सार्थक शब्द बनाएँ"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
fun WordCard(
    peg: PegWord,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onSpeakClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val breakdown = peg.encodingBreakdown?.takeIf { it.isNotBlank() }
        ?: PhoneticMatraEncoder.encode(peg.word).formattedTraceSummary()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = peg.word,
                        fontSize = 20.sp,
                        lineHeight = 29.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    CodeBadge(code = peg.code)
                    GradeBadge(grade = peg.qualityGrade)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onSpeakClick != null) {
                        IconButton(onClick = onSpeakClick, modifier = Modifier.size(32.dp)) {
                            Text("🔊", fontSize = 16.sp)
                        }
                    }
                    IconButton(onClick = onFavoriteToggle, modifier = Modifier.size(32.dp)) {
                        Text(if (peg.isFavorite) "★" else "☆", fontSize = 18.sp, color = AmberCode)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            VerificationStatusBadge(
                status = peg.verificationStatus,
                source = peg.verificationSource
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (peg.meaningHi.isNotBlank()) {
                Text(
                    text = "अर्थ: ${peg.meaningHi}",
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (breakdown.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "कोडिंग: $breakdown",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (peg.visualScene.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "💡 दृश्य कहानी: ${peg.visualScene}",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = AmberCode,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Formats inline markdown (`**bold**`, `*italic*`, `` `code` ``, `~~strike~~`, `[[backlink]]`)
 * into an [AnnotatedString] for clean card previews and document rendering.
 */
fun formatInlineMarkdownAnnotated(
    raw: String,
    primaryColor: Color = AmberCode,
    codeBgColor: Color = Color(0x2294A3B8)
): AnnotatedString = buildAnnotatedString {
    val pattern = Regex("""(\[\[(.*?)\]\]|\*\*(.+?)\*\*|\*(.+?)\*|`(.+?)`|~~(.+?)~~)""")
    var cursor = 0
    pattern.findAll(raw).forEach { match ->
        if (match.range.first > cursor) {
            append(raw.substring(cursor, match.range.first))
        }
        val token = match.value
        when {
            token.startsWith("[[") && token.endsWith("]]") -> {
                val linked = token.removePrefix("[[").removeSuffix("]]")
                pushStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        background = primaryColor.copy(alpha = 0.14f)
                    )
                )
                append(" 🔗$linked ")
                pop()
            }
            token.startsWith("**") && token.endsWith("**") -> {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(token.removePrefix("**").removeSuffix("**"))
                pop()
            }
            token.startsWith("~~") && token.endsWith("~~") -> {
                pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                append(token.removePrefix("~~").removeSuffix("~~"))
                pop()
            }
            token.startsWith("`") && token.endsWith("`") -> {
                pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        background = codeBgColor
                    )
                )
                append(" ${token.removeSurrounding("`")} ")
                pop()
            }
            else -> {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                append(token.removeSurrounding("*"))
                pop()
            }
        }
        cursor = match.range.last + 1
    }
    if (cursor < raw.length) {
        append(raw.substring(cursor))
    }
}

/**
 * Renders a compact, rich Markdown preview inside Note list cards so users see formatted
 * headings, bullet points, bold highlights, and backlinks rather than raw `#` or `**` symbols.
 */
@Composable
fun MarkdownSnippetPreview(
    markdown: String,
    maxLines: Int = 4,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val previewLines = remember(markdown) {
        markdown.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("```") && it != "---" }
            .take(maxLines)
    }

    if (previewLines.isEmpty()) {
        Text(
            text = "कोई सामग्री नहीं (Empty note)",
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontStyle = FontStyle.Italic,
            color = onSurfaceVariant,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        previewLines.forEach { line ->
            when {
                line.startsWith("### ") || line.startsWith("## ") || line.startsWith("# ") -> {
                    val cleanHeading = line.replaceFirst(Regex("^#{1,3}\\s+"), "")
                    Text(
                        text = formatInlineMarkdownAnnotated(cleanHeading, primaryColor),
                        fontSize = 13.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") || line.startsWith("• ") -> {
                    val itemText = line.substring(2).trim()
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "•",
                            fontSize = 13.sp,
                            lineHeight = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Text(
                            text = formatInlineMarkdownAnnotated(itemText, primaryColor),
                            fontSize = 13.sp,
                            lineHeight = 21.sp,
                            color = onSurface.copy(alpha = 0.95f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                line.startsWith("> ") -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(primaryColor.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = formatInlineMarkdownAnnotated(line.removePrefix("> ").trim(), primaryColor),
                            fontSize = 12.sp,
                            lineHeight = 20.sp,
                            fontStyle = FontStyle.Italic,
                            color = onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                else -> {
                    Text(
                        text = formatInlineMarkdownAnnotated(line, primaryColor),
                        fontSize = 13.sp,
                        lineHeight = 21.sp,
                        color = onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
