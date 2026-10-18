package com.pocketdev.ank_shabd.data.repository

import android.content.ContentValues
import android.content.Context
import com.pocketdev.ank_shabd.data.database.AnkShabdDatabaseHelper
import com.pocketdev.ank_shabd.domain.encoder.EncodingRuleSet
import com.pocketdev.ank_shabd.domain.encoder.PhoneticMatraEncoder
import com.pocketdev.ank_shabd.domain.models.PegWord
import com.pocketdev.ank_shabd.domain.models.RuleSetDiff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class EncodingRuleRepository(
    private val context: Context,
    private val dictionaryRepository: DictionaryRepository
) {
    private val dbHelper = AnkShabdDatabaseHelper.getInstance(context)

    // Current active ruleset in memory
    private var activeRuleSet: EncodingRuleSet = EncodingRuleSet.V2_3

    fun getActiveRuleSet(): EncodingRuleSet {
        return activeRuleSet
    }

    fun setActiveRuleSet(ruleSet: EncodingRuleSet) {
        activeRuleSet = ruleSet
    }

    suspend fun getAllRuleSets(): List<EncodingRuleSet> = withContext(Dispatchers.IO) {
        val list = mutableListOf(EncodingRuleSet.V2_3)
        val db = dbHelper.readableDatabase
        val cursor = db.query("rulesets", null, "id != ?", arrayOf(EncodingRuleSet.V2_3.id), null, null, "name ASC")
        cursor.use {
            while (it.moveToNext()) {
                val jsonStr = it.getString(it.getColumnIndexOrThrow("ruleset_json"))
                val rs = parseRuleSetJson(jsonStr)
                if (rs != null) list.add(rs)
            }
        }
        list
    }

    suspend fun saveCustomRuleSet(ruleSet: EncodingRuleSet): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val jsonStr = exportRuleSetToJson(ruleSet)
        val cv = ContentValues().apply {
            put("id", ruleSet.id)
            put("name", ruleSet.name)
            put("version", ruleSet.version)
            put("description", ruleSet.description)
            put("is_default", if (ruleSet.isDefault) 1 else 0)
            put("is_custom", 1)
            put("ruleset_json", jsonStr)
            put("created_at", System.currentTimeMillis())
        }
        val row = db.insertWithOnConflict("rulesets", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        row != -1L
    }

    suspend fun findInvalidatedWords(newRuleSet: EncodingRuleSet): List<Pair<PegWord, String>> = withContext(Dispatchers.IO) {
        val allWords = dictionaryRepository.getAllPegs()
        val invalidated = mutableListOf<Pair<PegWord, String>>()

        for (peg in allWords) {
            val newResult = PhoneticMatraEncoder.encode(peg.word, newRuleSet)
            if (newResult.code != peg.code || !newResult.isValid) {
                invalidated.add(peg to newResult.code)
            }
        }
        invalidated
    }

    fun compareRuleSets(r1: EncodingRuleSet, r2: EncodingRuleSet): RuleSetDiff {
        val consonantChanges = mutableListOf<String>()
        val carrierChanges = mutableListOf<String>()
        val vowelChanges = mutableListOf<String>()
        val matraChanges = mutableListOf<String>()

        // Check consonant diffs
        val allConsonants = (r1.consonantMapping.keys + r2.consonantMapping.keys).distinct()
        for (c in allConsonants) {
            val val1 = r1.consonantMapping[c]
            val val2 = r2.consonantMapping[c]
            if (val1 != val2) {
                consonantChanges.add("$c: [${r1.name} = $val1] vs [${r2.name} = $val2]")
            }
        }

        // Check carrier diffs
        val allCarriers = (r1.carrierConsonants + r2.carrierConsonants).distinct()
        for (c in allCarriers) {
            val in1 = r1.carrierConsonants.contains(c)
            val in2 = r2.carrierConsonants.contains(c)
            if (in1 != in2) {
                carrierChanges.add("$c: वाहक स्थिति में अंतर")
            }
        }

        return RuleSetDiff(
            ruleSet1Id = r1.id,
            ruleSet2Id = r2.id,
            consonantChanges = consonantChanges,
            carrierChanges = carrierChanges,
            vowelChanges = vowelChanges,
            matraChanges = matraChanges
        )
    }

    fun exportRuleSetToJson(ruleSet: EncodingRuleSet): String {
        val root = JSONObject().apply {
            put("id", ruleSet.id)
            put("name", ruleSet.name)
            put("version", ruleSet.version)
            put("description", ruleSet.description)
            put("isDefault", ruleSet.isDefault)
            put("isCustom", ruleSet.isCustom)

            val cMap = JSONObject()
            ruleSet.consonantMapping.forEach { (k, v) -> cMap.put(k, v) }
            put("consonantMapping", cMap)

            put("carrierConsonants", org.json.JSONArray(ruleSet.carrierConsonants))

            val vMap = JSONObject()
            ruleSet.independentVowelMapping.forEach { (k, v) -> vMap.put(k, v) }
            put("independentVowelMapping", vMap)

            val mMap = JSONObject()
            ruleSet.matraMapping.forEach { (k, v) -> mMap.put(k, v) }
            put("matraMapping", mMap)
        }
        return root.toString(2)
    }

    fun parseRuleSetJson(jsonStr: String): EncodingRuleSet? {
        return try {
            val obj = JSONObject(jsonStr)
            val cMap = mutableMapOf<String, String>()
            val cObj = obj.optJSONObject("consonantMapping") ?: JSONObject()
            cObj.keys().forEach { cMap[it] = cObj.getString(it) }

            val carriers = mutableSetOf<String>()
            val cArr = obj.optJSONArray("carrierConsonants")
            if (cArr != null) {
                for (i in 0 until cArr.length()) carriers.add(cArr.getString(i))
            } else {
                carriers.addAll(setOf("य", "व", "ह", "ल"))
            }

            EncodingRuleSet(
                id = obj.getString("id"),
                name = obj.getString("name"),
                version = obj.optString("version", "1.0"),
                description = obj.optString("description", ""),
                isDefault = obj.optBoolean("isDefault", false),
                isCustom = true,
                consonantMapping = if (cMap.isNotEmpty()) cMap else EncodingRuleSet.V2_3.consonantMapping,
                carrierConsonants = carriers,
                independentVowelMapping = EncodingRuleSet.V2_3.independentVowelMapping,
                matraMapping = EncodingRuleSet.V2_3.matraMapping,
                diacriticMapping = EncodingRuleSet.V2_3.diacriticMapping,
                zeroDigitSigns = EncodingRuleSet.V2_3.zeroDigitSigns,
                unsupportedSymbols = EncodingRuleSet.V2_3.unsupportedSymbols
            )
        } catch (e: Exception) {
            null
        }
    }
}
