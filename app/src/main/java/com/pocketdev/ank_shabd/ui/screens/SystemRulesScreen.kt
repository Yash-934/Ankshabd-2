package com.pocketdev.ank_shabd.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.pocketdev.ank_shabd.domain.encoder.EncodingRuleSet
import com.pocketdev.ank_shabd.ui.components.CodeBadge
import com.pocketdev.ank_shabd.ui.theme.*

@Composable
fun SystemRulesScreen(
    onNavigateToDiagnostics: () -> Unit
) {
    val activeRuleset = remember { EncodingRuleSet.V2_3 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Title & Ruleset Badge
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "नियम प्रणाली | System Rules",
                            fontSize = 22.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Deterministic phonetic & matra encoding specification",
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = onNavigateToDiagnostics,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("🧪 Regression Tests", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Active Ruleset:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(activeRuleset.ruleSetId, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                        Text("•", fontSize = 12.sp)
                        Text("Version: ${activeRuleset.version}", fontSize = 12.sp)
                    }
                }
            }
        }

        // Section 1: Consonant Mapping (0-9)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. व्यंजन मैपिंग (Consonant Base Mapping)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Every base consonant emits its mapped digit. Inherited 'अ' emits no separate digit.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val consonantTable = listOf(
                        "0" to "श, ष, ज़",
                        "1" to "क, ख, ग, घ, क़, ख़, ग़",
                        "2" to "द, ध, ड, ढ, ड़, ढ़",
                        "3" to "त, थ, ट, ठ",
                        "4" to "च, ज, र",
                        "5" to "प, फ, फ़",
                        "6" to "छ, झ",
                        "7" to "स",
                        "8" to "ब, भ",
                        "9" to "न, म, ण"
                    )

                    consonantTable.forEach { (digit, chars) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CodeBadge(code = digit)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = chars,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (digit != "9") {
                            Divider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }
        }

        // Section 2: Carrier Consonants
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2. संवाहक व्यंजन (Carrier Consonants)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "य, व, ह, ल",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "• संवाहक व्यंजन स्वयं कोई अंक प्रदान नहीं करते (No base digit emitted).\n• परन्तु यदि इन पर कोई स्वर मात्रा लगी हो, तो वह मात्रा अपना अंक अवश्य प्रदान करती है (Attached vowel matra emits its digit).\n• उदाहरण: 'दीया' → द(2) + ी(3) + य(carrier) + ा(8) = 238\n• उदाहरण: 'चूहा' → च(4) + ू(0) + ह(carrier) + ा(8) = 408",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        // Section 3: Vowels & Matras Mapping (0-9)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "3. स्वतंत्र स्वर एवं मात्राएँ (Vowels & Matras)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Independent vowels and dependent vowel matras mapped to digits 0-9.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val vowelTable = listOf(
                        "0" to ("उ, ऊ" to "ु, ू"),
                        "1" to ("ए, ऐ" to "े, ै"),
                        "2" to ("ओ, औ" to "ो, ौ"),
                        "3" to ("इ, ई" to "ि, ी"),
                        "4" to ("ऋ" to "ृ"),
                        "5" to ("—" to "अनुस्वार (ं)"),
                        "6" to ("—" to "विसर्ग (ः)"),
                        "7" to ("—" to "कोई मानक स्वर-चिह्न नहीं"),
                        "8" to ("आ" to "ा"),
                        "9" to ("—" to "कोई मानक स्वर-चिह्न नहीं")
                    )

                    vowelTable.forEach { (digit, pair) ->
                        val (vowels, matras) = pair
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CodeBadge(code = digit)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "मात्रा: $matras",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (vowels != "—") {
                                    Text(
                                        text = "स्वतंत्र स्वर: $vowels",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (digit != "9") {
                            Divider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }
        }

        // Section 4: Special Phonetic Rules
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "4. विशेष नियम एवं अपवाद (Special Rules)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val rules = listOf(
                        "अन्तर्निहित 'अ'" to "अन्तर्निहित या स्वतंत्र 'अ' कोई अंक नहीं बनाता (Inherent 'a' emits no digit).",
                        "चंद्रबिंदु (ँ)" to "चंद्रबिंदु कोई अंक नहीं बनाता (Chandrabindu emits no digit: 'हँस' = 7, 'गाँव' = 18).",
                        "अनुस्वार (ं)" to "अनुस्वार अंक 5 बनाता है (Anusvara emits 5: 'हंस' = 57, 'गांव' = 185).",
                        "हलंत / विराम (्)" to "हलंत कोई अंक नहीं बनाता, केवल अर्ध-व्यंजन दर्शाता है।",
                        "शून्य का संरक्षण" to "प्रारंभिक शून्य सदैव सुरक्षित रहते हैं ('04' एक स्ट्रिंग है, संख्या 4 नहीं)।",
                        "अज्ञात वर्ण चेतावनी" to "असमर्थित वर्णों पर कभी मूक चूक न करें; लाल चेतावनी बैज प्रदर्शित करें।"
                    )

                    rules.forEach { (ruleTitle, ruleDesc) ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text("• $ruleTitle", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(ruleDesc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
