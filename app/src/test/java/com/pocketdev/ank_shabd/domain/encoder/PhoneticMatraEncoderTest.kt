package com.pocketdev.ank_shabd.domain.encoder

import com.pocketdev.ank_shabd.domain.models.SeedDictionary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneticMatraEncoderTest {

    @Test
    fun testMandatoryRegressionTable() {
        val mandatoryCases = listOf(
            "खेल" to "11",
            "केक" to "111",
            "गेहूँ" to "110",
            "गाय" to "18",
            "गाल" to "18",
            "दवा" to "28",
            "दही" to "23",
            "कमल" to "19",
            "शहर" to "04",
            "हंस" to "57",
            "हँस" to "7",
            "गाँव" to "18",
            "गांव" to "185",
            "पंप" to "555",
            "पंख" to "551",
            "गोद" to "122",
            "देख" to "211",
            "दीया" to "238",
            "चूहा" to "408",
            "कुली" to "103"
        )

        for ((inputWord, expectedCode) in mandatoryCases) {
            val result = PhoneticMatraEncoder.encode(inputWord)
            assertEquals(
                "Failed for word: '$inputWord'. Expected $expectedCode but got ${result.code}. Trace: ${result.formattedTraceSummary()}",
                expectedCode,
                result.code
            )
            assertTrue("Word '$inputWord' should be valid without warnings", result.isValid)
        }
    }

    @Test
    fun testPromptTargetNumbersHaveValidWords() {
        val requiredPromptNumbers = listOf(
            "11", "24", "42", "185", "111", "110", "1947", "2026", "356", "1001",
            "18", "23", "28", "04", "57", "7"
        )
        val builtSeeds = SeedDictionary.getValidatedSeedPegs()
        for (targetNumber in requiredPromptNumbers) {
            val matching = builtSeeds.filter { it.code == targetNumber }
            assertTrue(
                "Expected at least one verified Hindi seed word for target number '$targetNumber'",
                matching.isNotEmpty()
            )
            val best = matching.first()
            val enc = PhoneticMatraEncoder.encodeSingleWordCandidate(best.word)
            assertTrue("Best word '${best.word}' for $targetNumber must be a valid single word", enc.isValid)
            assertEquals(targetNumber, enc.code)
            assertTrue("Meaning must not be blank", !best.meaningHi.isNullOrBlank())
            assertTrue("Mnemonic story must not be blank", !best.mnemonicStory.isNullOrBlank())
            assertTrue("Why good peg must not be blank", !best.dictionaryEvidence.isNullOrBlank())
            assertTrue("Encoding breakdown must not be blank", !best.encodingBreakdown.isNullOrBlank())
        }
    }

    @Test
    fun testLeadingZeroPreservation() {
        val result = PhoneticMatraEncoder.encode("शहर")
        assertEquals("04", result.code)
        assertTrue(result.code.startsWith("0"))
        assertEquals("0", result.traces[0].digitEmitted)
    }

    @Test
    fun testAnusvaraVsChandrabindu() {
        val hans = PhoneticMatraEncoder.encode("हंस")
        val hnas = PhoneticMatraEncoder.encode("हँस")
        assertEquals("57", hans.code)
        assertEquals("7", hnas.code)

        val gaanv1 = PhoneticMatraEncoder.encode("गाँव")
        val gaanv2 = PhoneticMatraEncoder.encode("गांव")
        assertEquals("18", gaanv1.code)
        assertEquals("185", gaanv2.code)
    }

    @Test
    fun testIndependentVowelsAndVisargaAndR() {
        assertEquals("02", PhoneticMatraEncoder.encode("उदय").code)
        assertEquals("27", PhoneticMatraEncoder.encode("ओस").code)
        assertEquals("37", PhoneticMatraEncoder.encode("इस").code)
        assertEquals("81", PhoneticMatraEncoder.encode("आग").code)
        assertEquals("53", PhoneticMatraEncoder.encode("अंत").code)
        assertEquals("94", PhoneticMatraEncoder.encode("अमर").code)
        // Visarga ः = 6
        assertEquals("36", PhoneticMatraEncoder.encode("अतः").code)
        // Vocalic R ऋ = 4, ृ = 4
        assertEquals("403", PhoneticMatraEncoder.encode("ऋषि").code)
        assertEquals("1458", PhoneticMatraEncoder.encode("कृपा").code)
    }

    @Test
    fun testConjunctsAndHalantNeverOmitConsonants() {
        // भक्त: भ=8, क=1, ्=no digit, त=3 -> 813
        assertEquals("813", PhoneticMatraEncoder.encode("भक्त").code)
        // स्कूल: स=7, ्=no digit, क=1, ू=0, ल=carrier -> 710
        assertEquals("710", PhoneticMatraEncoder.encode("स्कूल").code)
        // स्वतंत्र: स=7, ्=no digit, व=carrier, त=3, ं=5, त=3, ्=no digit, र=4 -> 73534
        assertEquals("73534", PhoneticMatraEncoder.encode("स्वतंत्र").code)
    }

    @Test
    fun testSingleWordCandidateValidationRejectsMultiWordOrInvalid() {
        val validSingle = PhoneticMatraEncoder.encodeSingleWordCandidate("कमल")
        assertTrue(validSingle.isValid)
        assertEquals("19", validSingle.code)

        val multiWord = PhoneticMatraEncoder.encodeSingleWordCandidate("नीला कमल")
        assertFalse("Multi-word phrase must be rejected by encodeSingleWordCandidate", multiWord.isValid)

        val englishWord = PhoneticMatraEncoder.encodeSingleWordCandidate("kamal")
        assertFalse("Latin characters must be rejected", englishWord.isValid)

        val candraO = PhoneticMatraEncoder.encodeSingleWordCandidate("डॉक्टर")
        assertFalse("Unsupported Candra O must be rejected", candraO.isValid)
    }

    @Test
    fun testCarrierVowelSigns() {
        // ल carries ई (3)
        assertEquals("103", PhoneticMatraEncoder.encode("कुली").code)
        // ह carries ई (3)
        assertEquals("23", PhoneticMatraEncoder.encode("दही").code)
        // य carries ा (8)
        assertEquals("238", PhoneticMatraEncoder.encode("दीया").code)
        // व carries ा (8)
        assertEquals("28", PhoneticMatraEncoder.encode("दवा").code)
    }

    @Test
    fun testNuktaLetters() {
        // ज़ = 0 (both precomposed and decomposed)
        val zameerPrecomposed = PhoneticMatraEncoder.encode("ज़मीर")
        assertEquals("0934", zameerPrecomposed.code)

        // क़ = 1
        val qalam = PhoneticMatraEncoder.encode("क़लम")
        assertEquals("19", qalam.code)

        // फ़ = 5
        val film = PhoneticMatraEncoder.encode("फ़िल्म")
        assertEquals("539", film.code)
    }

    @Test
    fun testUnsupportedNotationWarning() {
        val result = PhoneticMatraEncoder.encode("डॉक्टर")
        assertFalse(result.isValid)
        assertTrue(result.warnings.isNotEmpty())
    }

    @Test
    fun testEmptyInput() {
        val result = PhoneticMatraEncoder.encode("")
        assertEquals("", result.code)
        assertTrue(result.isValid)
        assertFalse(PhoneticMatraEncoder.encodeSingleWordCandidate("").isValid)
    }

    @Test
    fun testAllSeedDictionaryEntriesMatchCalculatedCodes() {
        val seedEntries = SeedDictionary.SEED_ENTRIES
        for (entry in seedEntries) {
            val enc = PhoneticMatraEncoder.encodeSingleWordCandidate(entry.word)
            assertEquals(
                "Mismatch in SeedDictionary for word '${entry.word}'. Expected ${entry.expectedCode} but got ${enc.code}. Trace: ${enc.formattedTraceSummary()}",
                entry.expectedCode,
                enc.code
            )
            assertTrue(
                "Seed word '${entry.word}' should produce valid single-word encoding without warnings: ${enc.warnings}",
                enc.isValid
            )
        }
    }

    @Test
    fun testNumberToWordGeneratorOutputsAllNineRequiredFields() = kotlinx.coroutines.runBlocking {
        val generator = com.pocketdev.ank_shabd.domain.generator.NumberToWordGenerator(
            dictionaryRepository = null,
            aiProvider = null
        )
        val numbersToTest = listOf("11", "24", "42", "185", "111", "110", "1947", "2026", "356", "1001", "04", "57")
        for (num in numbersToTest) {
            val res = generator.generateForNumber(num, invokeAi = false)
            assertNotNull("Expected bestCandidate for number $num", res.bestCandidate)
            val best = res.bestCandidate!!
            assertEquals(num, best.enteredNumber)
            assertEquals(num, best.finalEncodedNumber)
            assertTrue(best.hindiWord.isNotBlank())
            assertTrue(best.meaningHi.isNotBlank())
            assertTrue(best.characterByCharacterEncoding.isNotBlank())
            assertTrue(best.whyGoodMnemonicPeg.isNotBlank())
            assertTrue(best.vividMentalStory.isNotBlank())
            assertTrue(best.qualityGrade in listOf("A", "B", "unverified", "Unverified"))
            assertTrue(best.verificationStatus in listOf("approved", "unverified", "rejected"))
            assertEquals(1, res.resolvedWordCount)
            assertTrue(res.progressiveAttempts.isNotEmpty())
            assertEquals(1, res.progressiveAttempts.first().wordCount)
            assertTrue(res.progressiveAttempts.first().succeeded)
        }
    }

    @Test
    fun testProgressiveOneWordThenTwoWordsThenThreeWords() = kotlinx.coroutines.runBlocking {
        val generator = com.pocketdev.ank_shabd.domain.generator.NumberToWordGenerator(
            dictionaryRepository = null,
            aiProvider = null
        )

        // 1. Number "1857": No 1-word seed entry exists, so step 1 (1 word) fails, and step 2 (2 words: 18 + 57) succeeds!
        val res2Words = generator.generateForNumber("1857", invokeAi = false)
        assertEquals(2, res2Words.resolvedWordCount)
        assertNotNull(res2Words.segmentedBreakdown)
        assertEquals(2, res2Words.segmentedBreakdown!!.wordCountUsed)
        assertEquals(2, res2Words.progressiveAttempts.size)
        assertEquals(1, res2Words.progressiveAttempts[0].wordCount)
        assertFalse(res2Words.progressiveAttempts[0].succeeded)
        assertEquals(2, res2Words.progressiveAttempts[1].wordCount)
        assertTrue(res2Words.progressiveAttempts[1].succeeded)

        // 2. Number "185719472026" (12 digits): Cannot be formed in 1 or 2 words (max part length 5), resolves in 3 words (1857? or 1947 + 2026 + ...)
        // Let's test "111110185" (9 digits: 111 + 110 + 185 = केक + गेहूँ + गांव):
        // Wait: 9 digits with maxPartLength=5 can be 2 words only if 4+5 or 5+4 exist; neither 1111+10185 nor 11111+0185 exist in seeds, so k=1 fails, k=2 fails, k=3 succeeds!
        val res3Words = generator.generateForNumber("111110185", invokeAi = false)
        assertEquals(3, res3Words.resolvedWordCount)
        assertNotNull(res3Words.segmentedBreakdown)
        assertEquals(3, res3Words.segmentedBreakdown!!.wordCountUsed)
        assertEquals(listOf("111", "110", "185"), res3Words.segmentedBreakdown!!.segments.map { it.segmentCode })
        assertFalse(res3Words.progressiveAttempts[0].succeeded) // k = 1 failed
        assertFalse(res3Words.progressiveAttempts[1].succeeded) // k = 2 failed
        assertTrue(res3Words.progressiveAttempts[2].succeeded)  // k = 3 succeeded!
    }

    @Test
    fun testAvailableNumberBuildingBlocksPalette() {
        val palette = PhoneticMatraEncoder.buildNumberPalette("185")
        assertEquals("185", palette.targetNumber)
        assertEquals(3, palette.digitBlocks.size)
        // Digit '1' has क, ख, ग, घ... and े, ै
        assertTrue(palette.digitBlocks[0].availableConsonants.contains("क"))
        assertTrue(palette.digitBlocks[0].availableMatras.any { it.contains("े") })
        // Digit '8' has ब, भ and ा
        assertTrue(palette.digitBlocks[1].availableConsonants.contains("ब"))
        assertTrue(palette.digitBlocks[1].availableMatras.any { it.contains("ा") })
        // Digit '5' has प, फ, फ़ and ं
        assertTrue(palette.digitBlocks[2].availableConsonants.contains("प"))
        assertTrue(palette.digitBlocks[2].availableMatras.any { it.contains("ं") })
        // Carriers include य, व, ह, ल
        assertEquals(listOf("य", "व", "ह", "ल"), palette.carrierConsonants)
        assertTrue(palette.readySyllableHints.isNotEmpty())
    }
}
