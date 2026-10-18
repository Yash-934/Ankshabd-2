package com.pocketdev.ank_shabd.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketdev.ank_shabd.domain.encoder.PhoneticMatraEncoder
import com.pocketdev.ank_shabd.domain.models.Flashcard
import com.pocketdev.ank_shabd.domain.models.RevisionStats
import com.pocketdev.ank_shabd.ui.components.CodeBadge
import com.pocketdev.ank_shabd.ui.theme.*

@Composable
fun MemoryGymScreen(
    dueCards: List<Flashcard>,
    stats: RevisionStats?,
    onRecordReview: (cardId: String, rating: Int) -> Unit,
    onSpeakWord: (String) -> Unit,
    onRefreshCards: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var sessionReviewsCompleted by remember { mutableStateOf(0) }
    var sessionCorrectCount by remember { mutableStateOf(0) }
    var practiceMode by remember { mutableStateOf(0) } // 0 = Flashcard, 1 = Multiple Choice Quiz
    var selectedAnswer by remember { mutableStateOf<String?>(null) }

    val cards = remember(dueCards) { dueCards }
    val isSessionComplete = cards.isEmpty() || currentIndex >= cards.size

    val cardRotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(400),
        label = "flashcard_flip"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "स्मृति व्यायामशाला | Memory Gym",
                    fontSize = 21.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "SM-2 Spaced Repetition flashcards & active recall quiz",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    currentIndex = 0
                    isFlipped = false
                    selectedAnswer = null
                    onRefreshCards()
                }
            ) {
                Text("🔄", fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mode Selector Tabs: Flashcards vs Dedicated MCQ Quiz
        TabRow(
            selectedTabIndex = practiceMode,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = practiceMode == 0,
                onClick = {
                    practiceMode = 0
                    isFlipped = false
                    selectedAnswer = null
                },
                modifier = Modifier.testTag("tab_flashcard_mode"),
                text = {
                    Text(
                        text = "🃏 फ्लैशकार्ड (Flashcards)",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        fontWeight = if (practiceMode == 0) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            )
            Tab(
                selected = practiceMode == 1,
                onClick = {
                    practiceMode = 1
                    isFlipped = false
                    selectedAnswer = null
                },
                modifier = Modifier.testTag("tab_mcq_mode"),
                text = {
                    Text(
                        text = "✅ बहुविकल्पीय क्विज़ (MCQ)",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        fontWeight = if (practiceMode == 1) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Stats Dashboard Card (Fixed visibility in both Dark & Light mode)
        stats?.let { s ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GymStatItem(
                        label = "Due Today",
                        value = s.dueToday.toString(),
                        color = MaterialTheme.colorScheme.error
                    )
                    GymStatItem(
                        label = "Learned",
                        value = s.learnedCount.toString(),
                        color = EmeraldGreen
                    )
                    GymStatItem(
                        label = "Accuracy",
                        value = "${s.accuracyRate.toInt()}%",
                        color = MaterialTheme.colorScheme.primary
                    )
                    GymStatItem(
                        label = "Streak",
                        value = "${s.currentStreak} 🔥",
                        color = AmberGold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isSessionComplete) {
            // Session Completed Screen
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🎉", fontSize = 48.sp, lineHeight = 56.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "शाबाश! सत्र संपन्न हुआ",
                        fontSize = 22.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Great job! All due cards for this session reviewed.",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Reviews Completed: $sessionReviewsCompleted",
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (sessionReviewsCompleted > 0) {
                        val sessionAccuracy = (sessionCorrectCount.toFloat() / sessionReviewsCompleted * 100).toInt()
                        Text(
                            text = "Session Accuracy: $sessionAccuracy%",
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            currentIndex = 0
                            isFlipped = false
                            selectedAnswer = null
                            onRefreshCards()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "अभ्यास जारी रखें | Practice More",
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        } else {
            val card = cards[currentIndex]

            fun submitReview(rating: Int) {
                onRecordReview(card.id, rating)
                sessionReviewsCompleted++
                if (rating >= 2) sessionCorrectCount++
                isFlipped = false
                selectedAnswer = null
                currentIndex++
            }

            // Progress Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (practiceMode == 0) {
                        "Card ${currentIndex + 1} of ${cards.size}"
                    } else {
                        "प्रश्न ${currentIndex + 1} / ${cards.size}  •  सही: $sessionCorrectCount"
                    },
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = formatCardTypeLabel(card.cardType),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = (currentIndex.toFloat()) / cards.size.coerceAtLeast(1),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (practiceMode == 0) {
                // =========================================================================
                // MODE 0: 3D INTERACTIVE FLIPPING FLASHCARD (With Fixed Line Heights!)
                // =========================================================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable { isFlipped = !isFlipped }
                        .graphicsLayer {
                            rotationY = cardRotation
                            cameraDistance = 12f * density
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFlipped) MaterialTheme.colorScheme.surfaceVariant else VedicNavy
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    if (cardRotation <= 90f) {
                        // Front of Flashcard
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AmberGold.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "🃏 स्मृति फ्लैशकार्ड • PROMPT",
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberGold,
                                    letterSpacing = 0.8.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = card.frontPrompt,
                                fontSize = 26.sp,
                                lineHeight = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(28.dp))

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "💡 उत्तर देखने के लिए कार्ड पर टैप करें (Tap to flip)",
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    } else {
                        // Back of Flashcard (Flipped)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationY = 180f }
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "✓ उत्तर एवं V2.3 कोडिंग विश्लेषण",
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = { onSpeakWord(card.backAnswer) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("🔊", fontSize = 18.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = card.backAnswer,
                                    fontSize = 30.sp,
                                    lineHeight = 44.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )

                                // Deterministic encoding trace
                                val encResult = remember(card.backAnswer) {
                                    PhoneticMatraEncoder.encode(card.backAnswer)
                                }
                                if (encResult.isValid && encResult.code.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CodeBadge(code = encResult.code)
                                        Text(
                                            text = encResult.formattedTraceSummary(),
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (card.vividHook.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "🧠 ${card.vividHook}",
                                            fontSize = 13.sp,
                                            lineHeight = 21.sp,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // SM-2 Review Rating Buttons
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "आपको यह कितना याद रहा? (Rate Recall)",
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = { submitReview(0) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Again", fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold)
                                            Text("पुनः", fontSize = 10.sp, lineHeight = 14.sp)
                                        }
                                    }

                                    Button(
                                        onClick = { submitReview(1) },
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Hard", fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                            Text("कठिन", fontSize = 10.sp, lineHeight = 14.sp, color = Color.Black)
                                        }
                                    }

                                    Button(
                                        onClick = { submitReview(2) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Good", fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold)
                                            Text("अच्छा", fontSize = 10.sp, lineHeight = 14.sp)
                                        }
                                    }

                                    Button(
                                        onClick = { submitReview(3) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Easy", fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold)
                                            Text("सरल", fontSize = 10.sp, lineHeight = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // =========================================================================
                // MODE 1: DEDICATED MULTIPLE CHOICE QUIZ UI (NOT A FLIPPING FLASHCARD!)
                // =========================================================================
                val quizOptions = remember(card, cards) {
                    if (card.options.size >= 2) {
                        card.options
                    } else {
                        val distractors = cards
                            .map { it.backAnswer }
                            .filter { it != card.backAnswer }
                            .distinct()
                            .shuffled()
                            .take(3)
                        (distractors + card.backAnswer).shuffled()
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Quiz Question Banner Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AmberCode
                                ) {
                                    Text(
                                        text = "प्रश्न #${currentIndex + 1}",
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = "नीचे दिए गए 4 विकल्पों में से सही उत्तर चुनें",
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = card.frontPrompt,
                                fontSize = 21.sp,
                                lineHeight = 33.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 2. Option Cards (A, B, C, D)
                    val optionLabels = listOf("A", "B", "C", "D", "E", "F")
                    val hasAnswered = selectedAnswer != null

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        quizOptions.forEachIndexed { idx, option ->
                            val isSelected = selectedAnswer == option
                            val isCorrectOption = option == card.backAnswer

                            val containerColor = when {
                                !hasAnswered -> MaterialTheme.colorScheme.surface
                                isCorrectOption -> GreenApprovedContainer.copy(alpha = 0.85f)
                                isSelected && !isCorrectOption -> RedRejectedContainer.copy(alpha = 0.85f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            }

                            val borderColor = when {
                                !hasAnswered -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                isCorrectOption -> GreenApproved
                                isSelected && !isCorrectOption -> RedRejected
                                else -> Color.Transparent
                            }

                            val contentColor = when {
                                !hasAnswered -> MaterialTheme.colorScheme.onSurface
                                isCorrectOption -> Color(0xFF064E3B)
                                isSelected && !isCorrectOption -> RedRejected
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            val badgeBgColor = when {
                                !hasAnswered -> MaterialTheme.colorScheme.surfaceVariant
                                isCorrectOption -> GreenApproved
                                isSelected && !isCorrectOption -> RedRejected
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }

                            val badgeTextColor = when {
                                !hasAnswered -> MaterialTheme.colorScheme.onSurface
                                isCorrectOption || isSelected -> Color.White
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = containerColor,
                                border = BorderStroke(
                                    width = if (hasAnswered && (isCorrectOption || isSelected)) 2.dp else 1.dp,
                                    color = borderColor
                                ),
                                tonalElevation = if (!hasAnswered) 2.dp else 0.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(enabled = !hasAnswered) {
                                        selectedAnswer = option
                                    }
                                    .testTag("mcq_option_$idx")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Option Letter Circle Badge (A, B, C, D)
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(badgeBgColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = optionLabels.getOrElse(idx) { "${idx + 1}" },
                                            fontSize = 14.sp,
                                            lineHeight = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = badgeTextColor
                                        )
                                    }

                                    // Option Text
                                    Text(
                                        text = option,
                                        fontSize = 17.sp,
                                        lineHeight = 26.sp,
                                        fontWeight = if (hasAnswered && (isCorrectOption || isSelected)) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = contentColor,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Feedback Status Badge after answering
                                    if (hasAnswered) {
                                        when {
                                            isCorrectOption -> {
                                                Text(
                                                    text = "✓ सही",
                                                    fontSize = 12.sp,
                                                    lineHeight = 18.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = GreenApproved
                                                )
                                            }
                                            isSelected -> {
                                                Text(
                                                    text = "✕ गलत",
                                                    fontSize = 12.sp,
                                                    lineHeight = 18.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = RedRejected
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Instant Explanation & Next Question Card (Shown after selecting an option)
                    AnimatedVisibility(visible = hasAnswered) {
                        val isCorrect = selectedAnswer == card.backAnswer
                        val encResult = remember(card.backAnswer) {
                            PhoneticMatraEncoder.encode(card.backAnswer)
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isCorrect) {
                                            "🎉 बिल्कुल सही उत्तर!"
                                        } else {
                                            "💡 सही उत्तर: ${card.backAnswer}"
                                        },
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isCorrect) EmeraldGreen else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(
                                        onClick = { onSpeakWord(card.backAnswer) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("🔊", fontSize = 18.sp)
                                    }
                                }

                                if (encResult.isValid && encResult.code.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CodeBadge(code = encResult.code)
                                        Text(
                                            text = "V2.3 कोडिंग: ${encResult.formattedTraceSummary()}",
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                if (card.vividHook.isNotBlank()) {
                                    Text(
                                        text = "🧠 स्मृति सूत्र: ${card.vividHook}",
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = {
                                        submitReview(if (isCorrect) 3 else 0)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("mcq_next_question_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCorrect) EmeraldGreen else MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text(
                                        text = "अगला प्रश्न ➔ | Next Question",
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun formatCardTypeLabel(rawType: String): String = when (rawType.uppercase()) {
    "NUMBER_TO_WORD" -> "🔢 संख्या ➔ शब्द"
    "WORD_TO_NUMBER" -> "🔤 शब्द ➔ संख्या"
    "MEANING_TO_WORD" -> "📖 अर्थ ➔ शब्द"
    else -> rawType.replace("_", " ")
}

@Composable
private fun GymStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
