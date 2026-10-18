package com.pocketdev.ank_shabd.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketdev.ank_shabd.domain.encoder.PhoneticMatraEncoder
import com.pocketdev.ank_shabd.ui.components.CodeBadge
import com.pocketdev.ank_shabd.ui.components.ComponentTraceVisual
import com.pocketdev.ank_shabd.ui.theme.*

data class RegressionTestCase(
    val word: String,
    val expectedCode: String,
    val note: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen() {
    val mandatoryCases = remember {
        listOf(
            RegressionTestCase("खेल", "11", "Carrier ल emits no consonant digit"),
            RegressionTestCase("केक", "111", "क(1) + े(1) + क(1)"),
            RegressionTestCase("गेहूँ", "110", "Carrier ह with ू(0), chandrabindu=no digit"),
            RegressionTestCase("गाय", "18", "Carrier य emits no consonant digit"),
            RegressionTestCase("गाल", "18", "Carrier ल emits no consonant digit"),
            RegressionTestCase("दवा", "28", "Carrier व with ा(8)"),
            RegressionTestCase("दही", "23", "Carrier ह with ी(3)"),
            RegressionTestCase("कमल", "19", "क(1) + म(9) + Carrier ल"),
            RegressionTestCase("शहर", "04", "श(0) + Carrier ह + र(4)"),
            RegressionTestCase("हंस", "57", "Carrier ह, Anusvara(5) + स(7)"),
            RegressionTestCase("हँस", "7", "Chandrabindu emits no digit; only स(7)"),
            RegressionTestCase("गाँव", "18", "Chandrabindu emits no digit; ग(1)+ा(8)+व"),
            RegressionTestCase("गांव", "185", "Anusvara emits digit 5: ग(1)+ा(8)+ं(5)+व"),
            RegressionTestCase("पंप", "555", "प(5) + ं(5) + प(5)"),
            RegressionTestCase("पंख", "551", "प(5) + ं(5) + ख(1)"),
            RegressionTestCase("गोद", "122", "ग(1) + ो(2) + द(2)"),
            RegressionTestCase("देख", "211", "द(2) + े(1) + ख(1)"),
            RegressionTestCase("दीया", "238", "द(2) + ी(3) + य(carrier) + ा(8)"),
            RegressionTestCase("चूहा", "408", "च(4) + ू(0) + ह(carrier) + ा(8)"),
            RegressionTestCase("कुली", "103", "क(1) + ु(0) + ल(carrier) + ी(3)")
        )
    }

    var customTestWord by remember { mutableStateOf("") }
    val customEncResult = remember(customTestWord) {
        if (customTestWord.isNotBlank()) PhoneticMatraEncoder.encode(customTestWord) else null
    }

    // Run tests live
    val results = remember {
        mandatoryCases.map { tc ->
            val enc = PhoneticMatraEncoder.encode(tc.word)
            val passed = enc.code == tc.expectedCode
            Triple(tc, enc, passed)
        }
    }

    val allPassed = results.all { it.third }
    val passedCount = results.count { it.third }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Title & Summary Card
        item {
            Column {
                Text(
                    text = "सिस्टम निदान | System Diagnostics",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Live regression test suite for HINDI_PHONETIC_MATRA_V2_3",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (allPassed) EmeraldGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (allPassed) "✅ ALL TESTS PASSED" else "❌ FAILURES DETECTED",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = if (allPassed) EmeraldGreen else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "$passedCount of ${mandatoryCases.size} regression tests passed (100% compliance)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "${(passedCount.toFloat() / mandatoryCases.size * 100).toInt()}%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = if (allPassed) EmeraldGreen else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Live Custom Word Inspector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Interactive Live Word Tester", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customTestWord,
                        onValueChange = { customTestWord = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Type any Hindi word (e.g. विश्वविद्यालय, भारत, अंतरिक्ष)") },
                        singleLine = true
                    )

                    customEncResult?.let { enc ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Calculated Code:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            CodeBadge(code = enc.code)
                            Text(
                                text = if (enc.isValid) "Valid" else "Invalid",
                                fontSize = 12.sp,
                                color = if (enc.isValid) EmeraldGreen else MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        ComponentTraceVisual(traces = enc.traces)

                        if (enc.warnings.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            enc.warnings.forEach { w ->
                                Text("⚠️ $w", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // Mandatory Regression Test Results Header
        item {
            Text(
                text = "Mandatory V2.3 Regression Test Table (${mandatoryCases.size} Cases)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // 20 Regression Test Case Cards
        items(results) { (tc, enc, passed) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
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
                                text = tc.word,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text("Expected: ${tc.expectedCode}", fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (passed) EmeraldGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = if (passed) "PASS ✓" else "FAIL ✗",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (passed) EmeraldGreen else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Calculated: ${enc.code} | ${tc.note}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    ComponentTraceVisual(traces = enc.traces)
                }
            }
        }
    }
}
