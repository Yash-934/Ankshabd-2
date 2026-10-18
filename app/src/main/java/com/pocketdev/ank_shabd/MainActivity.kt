package com.pocketdev.ank_shabd

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketdev.ank_shabd.ai.GeminiAiProvider
import com.pocketdev.ank_shabd.ai.RejectedCandidateAudit
import com.pocketdev.ank_shabd.data.importexport.BackupRestoreRepository
import com.pocketdev.ank_shabd.data.repository.*
import com.pocketdev.ank_shabd.domain.encoder.EncodingRuleSet
import com.pocketdev.ank_shabd.domain.encoder.PhoneticMatraEncoder
import com.pocketdev.ank_shabd.domain.generator.NumberToWordGenerator
import com.pocketdev.ank_shabd.domain.generator.SegmentedNumberBreakdown
import com.pocketdev.ank_shabd.domain.models.*
import com.pocketdev.ank_shabd.ui.screens.*
import com.pocketdev.ank_shabd.ui.theme.AnkShabdTheme
import kotlinx.coroutines.launch
import java.util.*

enum class AppDestination(val title: String, val icon: String) {
    HOME("होम", "🏠"),
    WORD_LAB("शब्द लैब", "🔬"),
    ANALYZER("शब्द का कोड", "🔍"),
    TRANSLATE("अंक → शब्द", "🧩"),
    NOTES("नोट्स", "📝"),
    MEMORY_GYM("स्मृति जिम", "🧠"),
    MEMORY_STORY("स्मृति सिनेमा", "🎬"),
    IMPORT_EXPORT("डेटा", "💾"),
    SYSTEM_RULES("V2.3 नियम", "📜"),
    DIAGNOSTICS("जाँच", "🧪"),
    SETTINGS("सेटिंग्स", "⚙️"),
    WORD_DETAILS("शब्द विवरण", "📄")
}

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        setContent {
            val prefs = remember {
                getSharedPreferences("ai_provider_settings", android.content.Context.MODE_PRIVATE)
            }
            var themeMode by remember {
                mutableStateOf(prefs.getString("theme_mode", "system") ?: "system")
            }
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemDark
            }

            AnkShabdTheme(darkTheme = isDarkTheme) {
                AnkShabdApp(
                    themeMode = themeMode,
                    isDarkThemeActive = isDarkTheme,
                    onThemeModeChange = { newMode ->
                        themeMode = newMode
                        prefs.edit().putString("theme_mode", newMode).apply()
                    },
                    onSpeak = { text -> speakHindi(text) }
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.ENGLISH)
            }
            isTtsReady = true
        }
    }

    private fun speakHindi(text: String) {
        if (isTtsReady && text.isNotBlank()) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AnkShabd_${System.currentTimeMillis()}")
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnkShabdApp(
    themeMode: String = "system",
    isDarkThemeActive: Boolean = false,
    onThemeModeChange: (String) -> Unit = {},
    onSpeak: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Repositories
    val dictRepo = remember { DictionaryRepository(context) }
    val notesRepo = remember { NotesRepository(context) }
    val transRepo = remember { TranslationRepository(context) }
    val storyRepo = remember { MemoryStoryRepository(context, dictRepo, notesRepo) }
    val gymRepo = remember { RevisionRepository(context) }
    val ruleRepo = remember { EncodingRuleRepository(context, dictRepo) }
    val backupRepo = remember { BackupRestoreRepository(context, dictRepo, notesRepo) }
    val aiPreferences = remember {
        context.getSharedPreferences("ai_provider_settings", android.content.Context.MODE_PRIVATE)
    }
    val aiProvider = remember {
        val buildConfigKey = BuildConfig.GEMINI_API_KEY.takeIf {
            it.isNotBlank() && it != "MY_GEMINI_API_KEY"
        }
        GeminiAiProvider(
            apiKey = aiPreferences.getString("api_key", null)?.takeIf { it.isNotBlank() } ?: buildConfigKey,
            model = aiPreferences.getString("model", "gemini-2.5-flash") ?: "gemini-2.5-flash"
        ).apply {
            configure(
                aiPreferences.getString("provider", "gemini") ?: "gemini",
                aiPreferences.getString("endpoint", "") ?: "",
                aiPreferences.getString("model", "gemini-2.5-flash") ?: "gemini-2.5-flash"
            )
        }
    }
    val numberGenerator = remember {
        NumberToWordGenerator(dictRepo, aiProvider)
    }

    // Navigation state
    var currentDestination by remember { mutableStateOf(AppDestination.HOME) }
    var previousDestination by remember { mutableStateOf<AppDestination?>(null) }
    var selectedWordForDetails by remember { mutableStateOf<PegWord?>(null) }
    var activeNoteToEdit by remember { mutableStateOf<Note?>(null) }
    var wordLabInitialCode by remember { mutableStateOf("") }
    var analyzerInitialWord by remember { mutableStateOf("दवा") }
    var translationInitialText by remember { mutableStateOf("") }
    var memoryStoryInitialSeq by remember { mutableStateOf("1857") }

    // Data States
    var coverageStats by remember { mutableStateOf<CoverageStats?>(null) }
    var recentPegs by remember { mutableStateOf<List<PegWord>>(emptyList()) }
    var recentNotes by remember { mutableStateOf<List<Note>>(emptyList()) }
    var universalResults by remember { mutableStateOf<UniversalSearchResult?>(null) }
    var homeSearchQuery by remember { mutableStateOf("") }

    var wordLabResults by remember { mutableStateOf<List<PegWord>>(emptyList()) }
    var wordLabIsGenerating by remember { mutableStateOf(false) }
    var wordLabStatusMessage by remember { mutableStateOf<String?>(null) }
    var wordLabSegmentedBreakdown by remember { mutableStateOf<SegmentedNumberBreakdown?>(null) }
    var wordLabRejectedAudits by remember { mutableStateOf<List<RejectedCandidateAudit>>(emptyList()) }

    var currentTranslation by remember { mutableStateOf<TranslationRecord?>(null) }
    var translationError by remember { mutableStateOf<String?>(null) }
    var savedTranslations by remember { mutableStateOf<List<TranslationRecord>>(emptyList()) }
    var notebooks by remember { mutableStateOf<List<Notebook>>(emptyList()) }
    var noteFolders by remember { mutableStateOf<List<NoteFolder>>(emptyList()) }
    var notesList by remember { mutableStateOf<List<Note>>(emptyList()) }
    var dueCards by remember { mutableStateOf<List<Flashcard>>(emptyList()) }
    var revisionStats by remember { mutableStateOf<RevisionStats?>(null) }
    var savedStories by remember { mutableStateOf<List<MemoryStory>>(emptyList()) }
    var userApiKey by remember { mutableStateOf(aiProvider.getApiKey()) }
    var aiProviderId by remember { mutableStateOf(aiProvider.providerId()) }
    var aiEndpoint by remember { mutableStateOf(aiProvider.endpoint()) }
    var aiModel by remember { mutableStateOf(aiProvider.modelId()) }

    var showDrawerMenu by remember { mutableStateOf(false) }

    // Initial data loader
    fun refreshData() {
        coroutineScope.launch {
            coverageStats = dictRepo.getCoverageStats()
            recentPegs = dictRepo.getRecentPegs(10)
            recentNotes = notesRepo.getRecentNotes(10)
            savedTranslations = transRepo.getSavedTranslations()
            notebooks = notesRepo.getAllNotebooks()
            noteFolders = notesRepo.getFolders()
            notesList = notesRepo.getNotes()
            dueCards = gymRepo.getDueFlashcards(20)
            revisionStats = gymRepo.getRevisionStats()
            savedStories = storyRepo.getAllStories()
            if (wordLabResults.isEmpty() && wordLabInitialCode.isBlank()) {
                wordLabResults = dictRepo.getAllPegs()
            }
        }
    }

    fun executeWordLabLookup(rawCode: String, forceAi: Boolean) {
        coroutineScope.launch {
            val clean = GeminiAiProvider.sanitizeNumberInput(rawCode)
            if (clean.isBlank()) {
                wordLabIsGenerating = false
                wordLabStatusMessage = null
                wordLabSegmentedBreakdown = null
                wordLabRejectedAudits = emptyList()
                wordLabResults = dictRepo.getAllPegs()
                return@launch
            }
            wordLabIsGenerating = true
            try {
                val response = numberGenerator.generateForNumber(
                    rawNumber = clean,
                    invokeAi = forceAi
                )
                wordLabResults = response.allCandidates.map { it.pegWord }
                wordLabSegmentedBreakdown = response.segmentedBreakdown
                wordLabRejectedAudits = response.rejectedAiCandidates
                wordLabStatusMessage = response.statusBannerHi
                coverageStats = dictRepo.getCoverageStats()
                recentPegs = dictRepo.getRecentPegs(10)
            } finally {
                wordLabIsGenerating = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    fun navigateTo(dest: AppDestination) {
        previousDestination = currentDestination
        currentDestination = dest
    }

    BackHandler(enabled = currentDestination != AppDestination.HOME) {
        if (currentDestination == AppDestination.WORD_DETAILS) {
            currentDestination = previousDestination ?: AppDestination.HOME
        } else {
            currentDestination = AppDestination.HOME
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "अंकशब्द",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "• ${currentDestination.title}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onThemeModeChange(if (isDarkThemeActive) "light" else "dark")
                        }
                    ) {
                        Text(if (isDarkThemeActive) "☀️" else "🌙", fontSize = 18.sp)
                    }
                    IconButton(onClick = { showDrawerMenu = !showDrawerMenu }) {
                        Text("⋮", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }

                    DropdownMenu(
                        expanded = showDrawerMenu,
                        onDismissRequest = { showDrawerMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("🔍 Word-to-Code Analyzer") },
                            onClick = {
                                showDrawerMenu = false
                                navigateTo(AppDestination.ANALYZER)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🎬 Memory Cinema (Story)") },
                            onClick = {
                                showDrawerMenu = false
                                navigateTo(AppDestination.MEMORY_STORY)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📜 System Rules (V2.3)") },
                            onClick = {
                                showDrawerMenu = false
                                navigateTo(AppDestination.SYSTEM_RULES)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🧪 Live Regression Diagnostics") },
                            onClick = {
                                showDrawerMenu = false
                                navigateTo(AppDestination.DIAGNOSTICS)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("💾 Data Import & Export") },
                            onClick = {
                                showDrawerMenu = false
                                navigateTo(AppDestination.IMPORT_EXPORT)
                            }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("⚙️ Settings") },
                            onClick = {
                                showDrawerMenu = false
                                navigateTo(AppDestination.SETTINGS)
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val primaryNavItems = listOf(
                    AppDestination.HOME to "होम",
                    AppDestination.WORD_LAB to "शब्द लैब",
                    AppDestination.TRANSLATE to "अंक → शब्द",
                    AppDestination.NOTES to "नोट्स",
                    AppDestination.MEMORY_GYM to "स्मृति जिम"
                )

                primaryNavItems.forEach { (dest, label) ->
                    val isSelected = currentDestination == dest
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { navigateTo(dest) },
                        icon = { Text(dest.icon, fontSize = 20.sp) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                AppDestination.HOME -> {
                    HomeScreen(
                        coverageStats = coverageStats,
                        recentPegs = recentPegs,
                        recentNotes = recentNotes,
                        universalSearchResults = universalResults,
                        searchQuery = homeSearchQuery,
                        onSearchQueryChange = { q ->
                            homeSearchQuery = q
                            if (q.isNotBlank()) {
                                coroutineScope.launch {
                                    universalResults = dictRepo.searchUniversal(q)
                                }
                            } else {
                                universalResults = null
                            }
                        },
                        onNavigateToWordLab = { code ->
                            wordLabInitialCode = code ?: ""
                            navigateTo(AppDestination.WORD_LAB)
                            if (!code.isNullOrBlank()) {
                                executeWordLabLookup(code, forceAi = true)
                            }
                        },
                        onNavigateToAnalyzer = { navigateTo(AppDestination.ANALYZER) },
                        onNavigateToTranslate = { navigateTo(AppDestination.TRANSLATE) },
                        onNavigateToNotes = { navigateTo(AppDestination.NOTES) },
                        onNavigateToMemoryStory = { navigateTo(AppDestination.MEMORY_STORY) },
                        onNavigateToMemoryGym = { navigateTo(AppDestination.MEMORY_GYM) },
                        onNavigateToWordDetails = { peg ->
                            selectedWordForDetails = peg
                            navigateTo(AppDestination.WORD_DETAILS)
                        },
                        onNavigateToNoteEditor = { note ->
                            activeNoteToEdit = note
                            navigateTo(AppDestination.NOTES)
                        },
                        onFavoriteToggle = { word ->
                            coroutineScope.launch {
                                dictRepo.toggleFavorite(word)
                                refreshData()
                            }
                        },
                        onSpeakWord = { word -> onSpeak(word) }
                    )
                }

                AppDestination.WORD_LAB -> {
                    WordLabScreen(
                        initialCode = wordLabInitialCode,
                        searchResults = wordLabResults,
                        isGenerating = wordLabIsGenerating,
                        statusMessageHi = wordLabStatusMessage,
                        segmentedBreakdown = wordLabSegmentedBreakdown,
                        rejectedAiCandidates = wordLabRejectedAudits,
                        onSearchCode = { code ->
                            executeWordLabLookup(code, forceAi = false)
                        },
                        onDiscoverCandidates = { code ->
                            executeWordLabLookup(code, forceAi = true)
                        },
                        onApproveCandidate = { peg, grade ->
                            coroutineScope.launch {
                                val updated = peg.copy(
                                    qualityGrade = grade,
                                    verificationStatus = "approved",
                                    verificationSource = "user_verified"
                                )
                                dictRepo.savePeg(updated, expectedCode = peg.code)
                                executeWordLabLookup(peg.code, forceAi = false)
                                refreshData()
                            }
                        },
                        onRejectCandidate = { peg, _ ->
                            coroutineScope.launch {
                                dictRepo.deletePeg(peg.word)
                                wordLabResults = wordLabResults.filter { it.word != peg.word }
                                refreshData()
                            }
                        },
                        onAddNewCandidate = { word, meaningHi, meaningEn, category ->
                            coroutineScope.launch {
                                val enc = PhoneticMatraEncoder.encodeSingleWordCandidate(word)
                                if (enc.isValid && enc.code.isNotBlank()) {
                                    val peg = PegWord(
                                        id = UUID.randomUUID().toString(),
                                        word = enc.normalizedWord,
                                        normalizedWord = enc.normalizedWord,
                                        code = enc.code,
                                        ruleSetId = EncodingRuleSet.DEFAULT_RULESET_ID,
                                        category = category.ifBlank { "सामान्य" },
                                        meaningHi = meaningHi.ifBlank { enc.normalizedWord },
                                        meaningEn = meaningEn,
                                        transliteration = "",
                                        qualityGrade = "A",
                                        verificationStatus = "approved",
                                        verificationSource = "user_added",
                                        encodingBreakdown = enc.formattedTraceSummary(),
                                        mnemonicStory = SeedDictionary.buildVividMnemonicStory(
                                            word = enc.normalizedWord,
                                            code = enc.code,
                                            meaningHi = meaningHi,
                                            visualAssociation = meaningHi
                                        ),
                                        dictionaryEvidence = SeedDictionary.buildWhyGoodPeg(
                                            word = enc.normalizedWord,
                                            code = enc.code,
                                            category = category,
                                            meaningHi = meaningHi,
                                            qualityGrade = "A"
                                        )
                                    )
                                    dictRepo.savePeg(peg, expectedCode = enc.code)
                                    wordLabInitialCode = enc.code
                                    executeWordLabLookup(enc.code, forceAi = false)
                                    refreshData()
                                }
                            }
                        },
                        onUpdateDictionaryFolder = { peg, folder ->
                            coroutineScope.launch {
                                dictRepo.updateDictionaryFolder(peg.id, folder)
                                executeWordLabLookup(peg.code, forceAi = false)
                                refreshData()
                            }
                        },
                        onWordClick = { peg ->
                            selectedWordForDetails = peg
                            navigateTo(AppDestination.WORD_DETAILS)
                        },
                        onFavoriteToggle = { word ->
                            coroutineScope.launch {
                                dictRepo.toggleFavorite(word)
                                refreshData()
                            }
                        },
                        onSpeakWord = { word -> onSpeak(word) },
                        onNavigateToStory = { seq ->
                            memoryStoryInitialSeq = seq
                            navigateTo(AppDestination.MEMORY_STORY)
                        }
                    )
                }

                AppDestination.ANALYZER -> {
                    WordToCodeAnalyzerScreen(
                        initialInput = analyzerInitialWord,
                        onFindSavedWithCode = { code ->
                            dictRepo.getPegsByCodeSync(code)
                        },
                        onSaveWord = { word, _ ->
                            coroutineScope.launch {
                                val enc = PhoneticMatraEncoder.encodeSingleWordCandidate(word)
                                if (enc.isValid && enc.code.isNotBlank()) {
                                    val peg = PegWord(
                                        id = UUID.randomUUID().toString(),
                                        word = enc.normalizedWord,
                                        normalizedWord = enc.normalizedWord,
                                        code = enc.code,
                                        ruleSetId = EncodingRuleSet.DEFAULT_RULESET_ID,
                                        category = "सामान्य",
                                        meaningHi = enc.normalizedWord,
                                        meaningEn = "",
                                        transliteration = "",
                                        qualityGrade = "B",
                                        verificationStatus = "approved",
                                        verificationSource = "analyzer",
                                        encodingBreakdown = enc.formattedTraceSummary()
                                    )
                                    dictRepo.savePeg(peg, expectedCode = enc.code)
                                    refreshData()
                                }
                            }
                        },
                        onNavigateToTranslate = { text ->
                            translationInitialText = text
                            navigateTo(AppDestination.TRANSLATE)
                        },
                        onNavigateToNewNoteWithWord = { word, code ->
                            activeNoteToEdit = Note(
                                id = UUID.randomUUID().toString(),
                                notebookId = notebooks.firstOrNull()?.id ?: "nb_default",
                                title = "विश्लेषण: $word ($code)",
                                content = "### शब्द: $word\n**कोड:** $code\n\n- घटक विवरण: ${PhoneticMatraEncoder.encode(word).formattedTraceSummary()}\n- अर्थ:",
                                templateType = "PEG_WORD",
                                targetNumber = code,
                                targetWord = word,
                                tags = listOf("peg", code)
                            )
                            navigateTo(AppDestination.NOTES)
                        }
                    )
                }

                AppDestination.TRANSLATE -> {
                    TranslationScreen(
                        initialText = translationInitialText,
                        savedTranslations = savedTranslations,
                        translationError = translationError,
                        onTranslate = { text, src, tgt ->
                            coroutineScope.launch {
                                try {
                                    val record = transRepo.translate(text, src, tgt, aiProvider)
                                    currentTranslation = record
                                    translationError = null
                                    savedTranslations = transRepo.getSavedTranslations()
                                } catch (e: Exception) {
                                    translationError = e.localizedMessage ?: "अनुवाद विफल हुआ।"
                                }
                            }
                        },
                        currentTranslation = currentTranslation,
                        onSaveToNotes = { record ->
                            coroutineScope.launch {
                                transRepo.saveTranslationToNotes(record, notebooks.firstOrNull()?.id ?: "nb_default")
                                refreshData()
                            }
                        },
                        onSaveToDictionary = { word, hi, en ->
                            coroutineScope.launch {
                                val enc = PhoneticMatraEncoder.encodeSingleWordCandidate(word)
                                if (enc.isValid && enc.code.isNotBlank()) {
                                    val peg = PegWord(
                                        id = UUID.randomUUID().toString(),
                                        word = enc.normalizedWord,
                                        normalizedWord = enc.normalizedWord,
                                        code = enc.code,
                                        ruleSetId = EncodingRuleSet.DEFAULT_RULESET_ID,
                                        category = "सामान्य",
                                        meaningHi = hi,
                                        meaningEn = en,
                                        transliteration = "",
                                        qualityGrade = "A",
                                        verificationStatus = "approved",
                                        verificationSource = "translation",
                                        encodingBreakdown = enc.formattedTraceSummary()
                                    )
                                    dictRepo.savePeg(peg, expectedCode = enc.code)
                                    refreshData()
                                }
                            }
                        },
                        onFavoriteToggle = { id ->
                            coroutineScope.launch {
                                transRepo.toggleFavorite(id)
                                savedTranslations = transRepo.getSavedTranslations()
                            }
                        },
                        onSpeakText = { text -> onSpeak(text) },
                        onTranslationError = { message -> translationError = message.ifBlank { null } }
                    )
                }

                AppDestination.NOTES -> {
                    NotesScreen(
                        notebooks = notebooks,
                        notes = notesList,
                        folders = noteFolders,
                        activeEditingNote = activeNoteToEdit,
                        onSaveNote = { note ->
                            coroutineScope.launch {
                                notesRepo.saveNote(note)
                                activeNoteToEdit = null
                                refreshData()
                            }
                        },
                        onDeleteNote = { id ->
                            coroutineScope.launch {
                                notesRepo.deleteNote(id)
                                refreshData()
                            }
                        },
                        onToggleFavorite = { id ->
                            coroutineScope.launch {
                                notesRepo.toggleFavorite(id)
                                refreshData()
                            }
                        },
                        onCreateNotebook = { name, desc, color ->
                            coroutineScope.launch {
                                notesRepo.createNotebook(name, desc, color)
                                refreshData()
                            }
                        },
                        onCreateFolder = { name, notebookId, parentFolderId ->
                            coroutineScope.launch {
                                notesRepo.saveFolder(
                                    NoteFolder(
                                        id = UUID.randomUUID().toString(),
                                        notebookId = notebookId,
                                        name = name,
                                        parentFolderId = parentFolderId
                                    )
                                )
                                noteFolders = notesRepo.getFolders()
                            }
                        },
                        onRenameFolder = { id, name ->
                            coroutineScope.launch {
                                notesRepo.renameFolder(id, name)
                                noteFolders = notesRepo.getFolders()
                            }
                        },
                        onDeleteFolder = { id ->
                            coroutineScope.launch {
                                notesRepo.deleteFolder(id)
                                noteFolders = notesRepo.getFolders()
                                refreshData()
                            }
                        },
                        onCopyNote = { note ->
                            coroutineScope.launch {
                                notesRepo.saveNote(
                                    note.copy(
                                        id = UUID.randomUUID().toString(),
                                        title = "${note.title} (copy)",
                                        createdAt = System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                                refreshData()
                            }
                        },
                        onSearchNotes = { q ->
                            coroutineScope.launch {
                                notesList = notesRepo.getNotes(query = q)
                            }
                        },
                        onSelectNotebook = { nbId ->
                            coroutineScope.launch {
                                notesList = notesRepo.getNotes(notebookId = nbId)
                            }
                        },
                        onWordBacklinkClick = { word ->
                            coroutineScope.launch {
                                val peg = dictRepo.getPegByWord(word)
                                if (peg != null) {
                                    selectedWordForDetails = peg
                                    navigateTo(AppDestination.WORD_DETAILS)
                                } else {
                                    analyzerInitialWord = word
                                    navigateTo(AppDestination.ANALYZER)
                                }
                            }
                        },
                        onGenerateAiHook = { num, word, onResult ->
                            coroutineScope.launch {
                                val prompt = "Generate a vivid, memorable visual mnemonic hook in Hindi and English for number $num and Hindi word '$word'."
                                val response = aiProvider.generateText(prompt)
                                onResult(response)
                            }
                        }
                    )
                }

                AppDestination.MEMORY_GYM -> {
                    MemoryGymScreen(
                        dueCards = dueCards,
                        stats = revisionStats,
                        onRecordReview = { cardId, rating ->
                            coroutineScope.launch {
                                gymRepo.recordReview(cardId, rating)
                                revisionStats = gymRepo.getRevisionStats()
                            }
                        },
                        onSpeakWord = { word -> onSpeak(word) },
                        onRefreshCards = {
                            coroutineScope.launch {
                                dueCards = gymRepo.getDueFlashcards(20)
                                revisionStats = gymRepo.getRevisionStats()
                            }
                        }
                    )
                }

                AppDestination.MEMORY_STORY -> {
                    MemoryStoryScreen(
                        initialSequence = memoryStoryInitialSeq,
                        savedStories = savedStories,
                        onGenerateStory = { seq, onResult ->
                            coroutineScope.launch {
                                val pkg = storyRepo.generateStory(seq)
                                onResult(pkg)
                            }
                        },
                        onSaveStory = { story ->
                            coroutineScope.launch {
                                storyRepo.saveStory(story)
                                refreshData()
                            }
                        },
                        onExportToNote = { story ->
                            coroutineScope.launch {
                                storyRepo.exportStoryToNotes(story, notebooks.firstOrNull()?.id ?: "nb_default")
                                refreshData()
                            }
                        },
                        onDeleteStory = { id ->
                            coroutineScope.launch {
                                storyRepo.deleteStory(id)
                                refreshData()
                            }
                        },
                        onSpeakWord = { word -> onSpeak(word) },
                        onNavigateToWordLab = { code ->
                            wordLabInitialCode = code
                            navigateTo(AppDestination.WORD_LAB)
                            executeWordLabLookup(code, forceAi = true)
                        }
                    )
                }

                AppDestination.IMPORT_EXPORT -> {
                    ImportExportScreen(
                        onGeneratePreview = { content, format, onResult ->
                            coroutineScope.launch {
                                val preview = backupRepo.generateValidationPreview(content, format)
                                onResult(preview)
                            }
                        },
                        onExecuteImport = { content, format, policy, onResult ->
                            coroutineScope.launch {
                                val report = backupRepo.executeImport(content, format, policy)
                                refreshData()
                                onResult(report)
                            }
                        },
                        onExportBackupJson = { onResult ->
                            coroutineScope.launch {
                                try {
                                    onResult(backupRepo.exportFullBackupJson(), null)
                                } catch (e: Exception) {
                                    onResult(null, e.localizedMessage ?: "बैकअप clipboard निर्यात विफल हुआ।")
                                }
                            }
                        },
                        onExportBackupFile = { uri, onResult ->
                            coroutineScope.launch {
                                try {
                                    val output = context.contentResolver.openOutputStream(uri)
                                        ?: throw IllegalStateException("फ़ाइल लिखने के लिए उपलब्ध नहीं है।")
                                    backupRepo.exportFullBackupJson(output)
                                    onResult("पूरा बैकअप फ़ाइल में सुरक्षित हुआ।")
                                } catch (e: Exception) {
                                    onResult("फ़ाइल निर्यात विफल: ${e.localizedMessage ?: e.javaClass.simpleName}")
                                }
                            }
                        },
                        onExportCsv = { onResult ->
                            coroutineScope.launch {
                                val csv = backupRepo.exportDictionaryCsv()
                                onResult(csv)
                            }
                        },
                        onRestoreBackupJson = { json, onResult ->
                            coroutineScope.launch {
                                val res = backupRepo.restoreFromJson(json)
                                refreshData()
                                onResult(res)
                            }
                        },
                        onRestoreBackupFile = { uri, onResult ->
                            coroutineScope.launch {
                                try {
                                    val input = context.contentResolver.openInputStream(uri)
                                        ?: throw IllegalStateException("फ़ाइल पढ़ने के लिए उपलब्ध नहीं है।")
                                    val result = backupRepo.restoreFromJson(input)
                                    refreshData()
                                    onResult(result)
                                } catch (e: Exception) {
                                    onResult(
                                        RestoreResult(
                                            false, 0, 0, 0,
                                            "फ़ाइल पुनर्स्थापना विफल: ${e.localizedMessage ?: e.javaClass.simpleName}"
                                        )
                                    )
                                }
                            }
                        }
                    )
                }

                AppDestination.SYSTEM_RULES -> {
                    SystemRulesScreen(
                        onNavigateToDiagnostics = { navigateTo(AppDestination.DIAGNOSTICS) }
                    )
                }

                AppDestination.DIAGNOSTICS -> {
                    DiagnosticsScreen()
                }

                AppDestination.SETTINGS -> {
                    SettingsScreen(
                        apiKey = userApiKey,
                        providerId = aiProviderId,
                        apiEndpoint = aiEndpoint,
                        apiModel = aiModel,
                        themeMode = themeMode,
                        isDarkThemeActive = isDarkThemeActive,
                        onThemeModeChange = onThemeModeChange,
                        onSaveAiConfig = { key, provider, endpoint, model ->
                            userApiKey = key
                            aiProvider.saveApiKey(key)
                            aiProvider.configure(provider, endpoint, model)
                            aiProviderId = provider
                            aiEndpoint = endpoint
                            aiModel = model
                            aiPreferences.edit()
                                .putString("api_key", key)
                                .putString("provider", provider)
                                .putString("endpoint", endpoint)
                                .putString("model", model)
                                .apply()
                        },
                        onTestAiConnection = { onResult ->
                            coroutineScope.launch {
                                val (success, message) = aiProvider.testConnection()
                                onResult(success, message)
                            }
                        },
                        onTestTts = { sampleText -> onSpeak(sampleText) },
                        coverageStats = coverageStats
                    )
                }

                AppDestination.WORD_DETAILS -> {
                    selectedWordForDetails?.let { peg ->
                        WordDetailsScreen(
                            peg = peg,
                            onBackClick = {
                                currentDestination = previousDestination ?: AppDestination.HOME
                            },
                            onFavoriteToggle = {
                                coroutineScope.launch {
                                    dictRepo.toggleFavorite(peg.word)
                                    selectedWordForDetails = selectedWordForDetails?.copy(isFavorite = !peg.isFavorite)
                                    refreshData()
                                }
                            },
                            onSpeakClick = { onSpeak(peg.word) },
                            onNavigateToTranslate = { word ->
                                translationInitialText = word
                                navigateTo(AppDestination.TRANSLATE)
                            },
                            onNavigateToNewNote = { word, code ->
                                activeNoteToEdit = Note(
                                    id = UUID.randomUUID().toString(),
                                    notebookId = notebooks.firstOrNull()?.id ?: "nb_default",
                                    title = "पेग शब्द: $word ($code)",
                                    content = "### पेग शब्द: $word\n**कोड:** $code\n**घटक कोडिंग:** ${peg.encodingBreakdown ?: PhoneticMatraEncoder.encode(word).formattedTraceSummary()}\n**अर्थ (हिंदी):** ${peg.meaningHi}\n**अर्थ (अंग्रेजी):** ${peg.meaningEn}\n\n### मानसिक चित्र (Mental Hook):\n${peg.visualScene}",
                                    templateType = "PEG_WORD",
                                    targetNumber = code,
                                    targetWord = word,
                                    tags = listOf("peg", code, peg.category)
                                )
                                navigateTo(AppDestination.NOTES)
                            }
                        )
                    } ?: run {
                        currentDestination = AppDestination.HOME
                    }
                }
            }
        }
    }
}
