package com.pocketdev.ank_shabd.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketdev.ank_shabd.domain.encoder.PhoneticMatraEncoder
import com.pocketdev.ank_shabd.domain.models.Note
import com.pocketdev.ank_shabd.domain.models.NoteFolder
import com.pocketdev.ank_shabd.domain.models.Notebook
import com.pocketdev.ank_shabd.ui.components.CodeBadge
import com.pocketdev.ank_shabd.ui.components.MarkdownSnippetPreview
import com.pocketdev.ank_shabd.ui.components.formatInlineMarkdownAnnotated
import com.pocketdev.ank_shabd.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private enum class NotesViewMode {
    LIST,
    READER,
    EDITOR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    notebooks: List<Notebook>,
    notes: List<Note>,
    folders: List<NoteFolder>,
    activeEditingNote: Note? = null,
    onSaveNote: (Note) -> Unit,
    onDeleteNote: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onCreateNotebook: (String, String, String) -> Unit,
    onCreateFolder: (String, String, String?) -> Unit,
    onRenameFolder: (String, String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onCopyNote: (Note) -> Unit,
    onSearchNotes: (String) -> Unit,
    onSelectNotebook: (String?) -> Unit,
    onWordBacklinkClick: (String) -> Unit,
    onGenerateAiHook: (String, String, (String) -> Unit) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedNotebookId by remember { mutableStateOf<String?>(null) }

    val initialMode = remember(activeEditingNote) {
        when {
            activeEditingNote == null -> NotesViewMode.LIST
            notes.any { it.id == activeEditingNote.id } -> NotesViewMode.READER
            else -> NotesViewMode.EDITOR
        }
    }

    var viewMode by remember { mutableStateOf(initialMode) }
    var currentActiveNote by remember {
        mutableStateOf(
            activeEditingNote ?: createEmptyNote(notebooks.firstOrNull()?.id ?: "nb_default")
        )
    }
    var openedEditorFromReader by remember { mutableStateOf(false) }

    var showNewNotebookDialog by remember { mutableStateOf(false) }
    var newNbName by remember { mutableStateOf("") }
    var newNbDesc by remember { mutableStateOf("") }

    LaunchedEffect(activeEditingNote) {
        if (activeEditingNote != null) {
            currentActiveNote = activeEditingNote
            val existsInSaved = notes.any { it.id == activeEditingNote.id }
            viewMode = if (existsInSaved) NotesViewMode.READER else NotesViewMode.EDITOR
            openedEditorFromReader = false
        }
    }

    // Keep currentActiveNote synced if favorite/update happens in list
    LaunchedEffect(notes) {
        val updated = notes.find { it.id == currentActiveNote.id }
        if (updated != null && viewMode == NotesViewMode.READER) {
            currentActiveNote = updated
        }
    }

    if (showNewNotebookDialog) {
        AlertDialog(
            onDismissRequest = { showNewNotebookDialog = false },
            title = { Text("नया नोटबुक बनाएँ | Create Notebook") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newNbName,
                        onValueChange = { newNbName = it },
                        label = { Text("Notebook Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newNbDesc,
                        onValueChange = { newNbDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newNbName.isNotBlank()) {
                            onCreateNotebook(newNbName.trim(), newNbDesc.trim(), "#4F46E5")
                            showNewNotebookDialog = false
                            newNbName = ""
                            newNbDesc = ""
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewNotebookDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    when (viewMode) {
        NotesViewMode.READER -> {
            BackHandler {
                viewMode = NotesViewMode.LIST
            }
            NoteReaderView(
                note = currentActiveNote,
                notebooks = notebooks,
                folders = folders,
                onBack = { viewMode = NotesViewMode.LIST },
                onEditClick = {
                    openedEditorFromReader = true
                    viewMode = NotesViewMode.EDITOR
                },
                onToggleFavorite = {
                    onToggleFavorite(currentActiveNote.id)
                    currentActiveNote = currentActiveNote.copy(isFavorite = !currentActiveNote.isFavorite)
                },
                onCopy = { onCopyNote(currentActiveNote) },
                onDelete = {
                    onDeleteNote(currentActiveNote.id)
                    viewMode = NotesViewMode.LIST
                },
                onWordBacklinkClick = onWordBacklinkClick
            )
        }

        NotesViewMode.EDITOR -> {
            BackHandler {
                viewMode = if (openedEditorFromReader) NotesViewMode.READER else NotesViewMode.LIST
            }
            NoteEditorView(
                note = currentActiveNote,
                notebooks = notebooks,
                folders = folders,
                onSave = { saved ->
                    onSaveNote(saved)
                    currentActiveNote = saved
                    viewMode = NotesViewMode.READER
                },
                onCancel = {
                    viewMode = if (openedEditorFromReader) NotesViewMode.READER else NotesViewMode.LIST
                },
                onWordBacklinkClick = onWordBacklinkClick,
                onGenerateAiHook = onGenerateAiHook
            )
        }

        NotesViewMode.LIST -> {
            NotesListView(
                notes = notes,
                notebooks = notebooks,
                folders = folders,
                selectedNotebookId = selectedNotebookId,
                searchQuery = searchQuery,
                onSearchChange = {
                    searchQuery = it
                    onSearchNotes(it)
                },
                onNotebookSelect = {
                    selectedNotebookId = it
                    onSelectNotebook(it)
                },
                onNewNotebookClick = { showNewNotebookDialog = true },
                onNoteClick = { note ->
                    // Clicking a note opens Full-Screen Beautiful Markdown Reader Page!
                    currentActiveNote = note
                    openedEditorFromReader = false
                    viewMode = NotesViewMode.READER
                },
                onEditNoteClick = { note ->
                    currentActiveNote = note
                    openedEditorFromReader = false
                    viewMode = NotesViewMode.EDITOR
                },
                onNewNoteClick = { template, folderId ->
                    val folder = folders.find { it.id == folderId }
                    val nbId = folder?.notebookId ?: selectedNotebookId ?: notebooks.firstOrNull()?.id ?: "nb_default"
                    currentActiveNote = createTemplateNote(nbId, template).copy(folderId = folderId)
                    openedEditorFromReader = false
                    viewMode = NotesViewMode.EDITOR
                },
                onToggleFavorite = onToggleFavorite,
                onDeleteNote = onDeleteNote,
                onCreateFolder = onCreateFolder,
                onRenameFolder = onRenameFolder,
                onDeleteFolder = onDeleteFolder,
                onCopyNote = onCopyNote
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesListView(
    notes: List<Note>,
    notebooks: List<Notebook>,
    folders: List<NoteFolder>,
    selectedNotebookId: String?,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onNotebookSelect: (String?) -> Unit,
    onNewNotebookClick: () -> Unit,
    onNoteClick: (Note) -> Unit,
    onEditNoteClick: (Note) -> Unit,
    onNewNoteClick: (String, String?) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onCreateFolder: (String, String, String?) -> Unit,
    onRenameFolder: (String, String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onCopyNote: (Note) -> Unit
) {
    var showTemplateMenu by remember { mutableStateOf(false) }
    var currentFolderId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(selectedNotebookId) { currentFolderId = null }
    var showFolderDialog by remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf("") }
    var folderToRename by remember { mutableStateOf<NoteFolder?>(null) }
    val currentFolder = folders.find { it.id == currentFolderId }
    val visibleFolders = folders.filter { it.notebookId == selectedNotebookId || selectedNotebookId == null }
        .filter { it.parentFolderId == currentFolderId }
    val visibleNotes = notes.filter { it.folderId == currentFolderId }

    if (showFolderDialog || folderToRename != null) {
        val renaming = folderToRename
        AlertDialog(
            onDismissRequest = {
                showFolderDialog = false
                folderToRename = null
                folderName = ""
            },
            title = { Text(if (renaming == null) "नया फ़ोल्डर" else "फ़ोल्डर का नाम बदलें") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("फ़ोल्डर का नाम") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = folderName.isNotBlank(),
                    onClick = {
                        if (renaming == null) {
                            val folderNotebookId = currentFolder?.notebookId
                                ?: selectedNotebookId
                                ?: notebooks.firstOrNull()?.id
                                ?: "nb_default"
                            onCreateFolder(folderName.trim(), folderNotebookId, currentFolderId)
                        } else {
                            onRenameFolder(renaming.id, folderName.trim())
                        }
                        showFolderDialog = false
                        folderToRename = null
                        folderName = ""
                    }
                ) { Text(if (renaming == null) "बनाएँ" else "सहेजें") }
            },
            dismissButton = {
                TextButton(onClick = { showFolderDialog = false; folderToRename = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "स्मार्ट नोट्स | Smart Notes",
                    fontSize = 22.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    softWrap = true
                )
                Text(
                    text = "कार्ड पर टैप करके फुल-स्क्रीन Markdown रीडर में पढ़ें",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                Button(
                    onClick = { showTemplateMenu = true },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+ नया नोट", fontSize = 13.sp, lineHeight = 19.sp)
                }

                DropdownMenu(
                    expanded = showTemplateMenu,
                    onDismissRequest = { showTemplateMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("📌 Peg Word Note") },
                        onClick = {
                            showTemplateMenu = false
                            onNewNoteClick("PEG_WORD", currentFolderId)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("📖 Vocabulary Note") },
                        onClick = {
                            showTemplateMenu = false
                            onNewNoteClick("VOCABULARY", currentFolderId)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🔢 Number Memory Note") },
                        onClick = {
                            showTemplateMenu = false
                            onNewNoteClick("NUMBER_MEMORY", currentFolderId)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("📝 Free Markdown Note") },
                        onClick = {
                            showTemplateMenu = false
                            onNewNoteClick("FREE_MARKDOWN", currentFolderId)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("नोट्स, टैग, संख्या या शब्द खोजें...") },
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Notebook Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedNotebookId == null,
                    onClick = { onNotebookSelect(null) },
                    label = { Text("All (${notes.size})") }
                )
            }
            items(notebooks) { nb ->
                FilterChip(
                    selected = selectedNotebookId == nb.id,
                    onClick = { onNotebookSelect(nb.id) },
                    label = { Text(nb.name) }
                )
            }
            item {
                OutlinedButton(
                    onClick = onNewNotebookClick,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("+ Notebook", fontSize = 12.sp)
                }
            }
        }

        // Folder Bar (Horizontally scrollable so folders never overflow)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentFolder != null) {
                TextButton(onClick = { currentFolderId = currentFolder.parentFolderId }) {
                    Text("← ${currentFolder.name}")
                }
            } else {
                Text("फ़ोल्डर:", style = MaterialTheme.typography.labelLarge)
            }
            visibleFolders.forEach { folder ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = false,
                        onClick = { currentFolderId = folder.id },
                        label = { Text("📁 ${folder.name}", maxLines = 1) }
                    )
                    var menuExpanded by remember(folder.id) { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { menuExpanded = true }) { Text("⋮") }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(text = { Text("नाम बदलें") }, onClick = {
                                folderName = folder.name
                                folderToRename = folder
                                menuExpanded = false
                            })
                            DropdownMenuItem(text = { Text("हटाएँ") }, onClick = {
                                onDeleteFolder(folder.id)
                                if (currentFolderId == folder.id) currentFolderId = currentFolder?.parentFolderId
                                menuExpanded = false
                            })
                        }
                    }
                }
            }
            TextButton(onClick = { folderName = ""; showFolderDialog = true }) { Text("+ फ़ोल्डर") }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Note List
        if (visibleNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "कोई नोट नहीं मिला | No notes found",
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Create a new note with '+ नया नोट' above",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(visibleNotes) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onNoteClick(note) },
                        onEditClick = { onEditNoteClick(note) },
                        onToggleFavorite = { onToggleFavorite(note.id) },
                        onDelete = { onDeleteNote(note.id) },
                        onCopy = { onCopyNote(note) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("note_card_${note.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = note.title.ifBlank { "Untitled Note" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        lineHeight = 25.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!note.targetNumber.isNullOrBlank()) {
                        CodeBadge(code = note.targetNumber)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                        Text(
                            text = if (note.isFavorite) "★" else "☆",
                            fontSize = 18.sp,
                            color = if (note.isFavorite) AmberGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                        Text("✏️", fontSize = 13.sp)
                    }
                    var menuExpanded by remember(note.id) { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(28.dp)) {
                            Text("⋮", fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("📖 पूरा पढ़ें (Read Full Page)") },
                                onClick = { onClick(); menuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("✏️ संपादित करें (Edit)") },
                                onClick = { onEditClick(); menuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("📋 कॉपी बनाएँ (Duplicate)") },
                                onClick = { onCopy(); menuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("🗑 हटाएँ (Delete)") },
                                onClick = { onDelete(); menuExpanded = false }
                            )
                        }
                    }
                }
            }

            if (!note.targetWord.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "🎯 लक्षित शब्द: ${note.targetWord}",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Formatted Markdown Preview Box inside the Note List Card
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                modifier = Modifier.fillMaxWidth()
            ) {
                MarkdownSnippetPreview(
                    markdown = note.content,
                    maxLines = 4,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Template Tag, Tags & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = note.templateType.replace("_", " "),
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    note.tags.take(3).forEach { tag ->
                        Text(
                            text = "#$tag",
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "📖 पढ़ें • ${dateFormat.format(Date(note.updatedAt))}",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * FULL-SCREEN BEAUTIFUL MARKDOWN READER PAGE
 * Opened when clicking any note in the list (instead of opening raw edit mode).
 */
@Composable
private fun NoteReaderView(
    note: Note,
    notebooks: List<Notebook>,
    folders: List<NoteFolder>,
    onBack: () -> Unit,
    onEditClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onWordBacklinkClick: (String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val notebookName = remember(note.notebookId, notebooks) {
        notebooks.find { it.id == note.notebookId }?.name ?: "Personal Notes"
    }
    val folderName = remember(note.folderId, folders) {
        folders.find { it.id == note.folderId }?.name
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Reader Top Action Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("note_reader_back_button")
                ) {
                    Text(
                        text = "← सभी नोट्स",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onToggleFavorite) {
                        Text(
                            text = if (note.isFavorite) "★" else "☆",
                            fontSize = 20.sp,
                            color = if (note.isFavorite) AmberGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onCopy) {
                        Text("📋", fontSize = 16.sp)
                    }
                    IconButton(onClick = onDelete) {
                        Text("🗑", fontSize = 16.sp)
                    }
                    Button(
                        onClick = onEditClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("note_reader_edit_button")
                    ) {
                        Text(
                            text = "✏️ संपादित करें",
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Full-Screen Reader Scrollable Article Body
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Article Hero Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Breadcrumb & Template Badges
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "📓 $notebookName",
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            if (!folderName.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = "📁 $folderName",
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AmberCode.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = note.templateType.replace("_", " "),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberCode,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Note Title
                        Text(
                            text = note.title.ifBlank { "Untitled Note" },
                            fontSize = 24.sp,
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Target Number & Target Word Highlight Box (if linked to a Peg Word / Code)
                        if (!note.targetNumber.isNullOrBlank() || !note.targetWord.isNullOrBlank()) {
                            val enc = remember(note.targetWord) {
                                note.targetWord?.takeIf { it.isNotBlank() }?.let { PhoneticMatraEncoder.encode(it) }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        if (!note.targetNumber.isNullOrBlank()) {
                                            CodeBadge(code = note.targetNumber, large = true)
                                        }
                                        if (!note.targetWord.isNullOrBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.clickable { onWordBacklinkClick(note.targetWord) }
                                            ) {
                                                Text(
                                                    text = "🔗 ${note.targetWord}",
                                                    fontSize = 18.sp,
                                                    lineHeight = 26.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (enc != null && enc.isValid && enc.code.isNotBlank()) {
                                        Text(
                                            text = "V2.3 अक्षर कोडिंग: ${enc.formattedTraceSummary()}",
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // Tags & Timestamp Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (note.tags.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    note.tags.forEach { tag ->
                                        Text(
                                            text = "#$tag",
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            Text(
                                text = "अद्यतन: ${dateFormat.format(Date(note.updatedAt))}",
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3. Full Markdown Article Content Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MarkdownDocumentContent(
                            markdown = note.content,
                            onWordBacklinkClick = onWordBacklinkClick
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders full Markdown document blocks with beautiful typography, headings, bullet lists,
 * numbered lists, blockquotes, fenced code blocks, dividers, and interactive `[[Word]]` backlinks.
 */
@Composable
private fun MarkdownDocumentContent(
    markdown: String,
    onWordBacklinkClick: (String) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    if (markdown.isBlank()) {
        Text(
            text = "इस नोट में अभी कोई सामग्री नहीं है। ऊपर '✏️ संपादित करें' पर टैप करके लिखें।",
            fontSize = 14.sp,
            lineHeight = 22.sp,
            fontStyle = FontStyle.Italic,
            color = onSurfaceVariant
        )
        return
    }

    val lines = remember(markdown) { markdown.lines() }
    var inCodeBlock = false
    val codeBlockBuffer = mutableListOf<String>()

    @Composable
    fun FlushCodeBlock(codeLines: List<String>) {
        if (codeLines.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = codeLines.joinToString("\n"),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = onSurface,
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp)
                )
            }
        }
    }

    lines.forEach { rawLine ->
        val trimmed = rawLine.trim()

        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                FlushCodeBlock(codeBlockBuffer.toList())
                codeBlockBuffer.clear()
                inCodeBlock = false
            } else {
                inCodeBlock = true
            }
            return@forEach
        }

        if (inCodeBlock) {
            codeBlockBuffer.add(rawLine)
            return@forEach
        }

        when {
            trimmed.isBlank() -> {
                Spacer(modifier = Modifier.height(4.dp))
            }

            trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
            }

            trimmed.startsWith("# ") -> {
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                    Text(
                        text = formatInlineMarkdownAnnotated(trimmed.removePrefix("# ").trim(), primaryColor),
                        fontSize = 22.sp,
                        lineHeight = 33.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Divider(color = primaryColor.copy(alpha = 0.25f), thickness = 1.5.dp)
                }
            }

            trimmed.startsWith("## ") -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(22.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(primaryColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatInlineMarkdownAnnotated(trimmed.removePrefix("## ").trim(), primaryColor),
                        fontSize = 19.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurface
                    )
                }
            }

            trimmed.startsWith("### ") -> {
                Text(
                    text = formatInlineMarkdownAnnotated(trimmed.removePrefix("### ").trim(), primaryColor),
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberCode,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
            }

            trimmed.startsWith("> ") -> {
                Surface(
                    shape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .heightIn(min = 36.dp)
                                .background(primaryColor)
                        )
                        Text(
                            text = formatInlineMarkdownAnnotated(trimmed.removePrefix("> ").trim(), primaryColor),
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            fontStyle = FontStyle.Italic,
                            color = onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                val indentSpaces = rawLine.takeWhile { it == ' ' }.length
                val itemText = trimmed.substring(2).trim()
                Column(
                    modifier = Modifier.padding(start = (indentSpaces * 4).coerceAtMost(24).dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 9.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(primaryColor)
                        )
                        Text(
                            text = formatInlineMarkdownAnnotated(itemText, primaryColor),
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            color = onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    RenderExtractedBacklinkChips(itemText, onWordBacklinkClick)
                }
            }

            Regex("^\\d+\\.\\s+.*").matches(trimmed) -> {
                val numPart = trimmed.substringBefore(".").trim()
                val restText = trimmed.substringAfter(".").trim()
                Column {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = primaryColor.copy(alpha = 0.15f),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "$numPart.",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                        Text(
                            text = formatInlineMarkdownAnnotated(restText, primaryColor),
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            color = onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    RenderExtractedBacklinkChips(restText, onWordBacklinkClick)
                }
            }

            trimmed.startsWith("|") && trimmed.endsWith("|") -> {
                if (!trimmed.replace("|", "").replace("-", "").replace(" ", "").isEmpty()) {
                    val cells = trimmed.removePrefix("|").removeSuffix("|").split("|").map { it.trim() }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            cells.forEach { cell ->
                                Text(
                                    text = formatInlineMarkdownAnnotated(cell, primaryColor),
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    color = onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                Column {
                    Text(
                        text = formatInlineMarkdownAnnotated(rawLine, primaryColor),
                        fontSize = 16.sp,
                        lineHeight = 26.sp,
                        color = onSurface
                    )
                    RenderExtractedBacklinkChips(rawLine, onWordBacklinkClick)
                }
            }
        }
    }

    if (inCodeBlock && codeBlockBuffer.isNotEmpty()) {
        FlushCodeBlock(codeBlockBuffer.toList())
    }
}

@Composable
private fun RenderExtractedBacklinkChips(
    line: String,
    onBacklinkClick: (String) -> Unit
) {
    val regex = remember { Regex("\\[\\[(.*?)\\]\\]") }
    val matches = remember(line) { regex.findAll(line).map { it.groupValues[1].trim() }.filter { it.isNotBlank() }.toList() }
    if (matches.isNotEmpty()) {
        Row(
            modifier = Modifier
                .padding(top = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            matches.forEach { linkedWord ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.clickable { onBacklinkClick(linkedWord) }
                ) {
                    Text(
                        text = "🔗 शब्द खोलें: $linkedWord",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditorView(
    note: Note,
    notebooks: List<Notebook>,
    folders: List<NoteFolder>,
    onSave: (Note) -> Unit,
    onCancel: () -> Unit,
    onWordBacklinkClick: (String) -> Unit,
    onGenerateAiHook: (String, String, (String) -> Unit) -> Unit
) {
    var title by remember { mutableStateOf(note.title) }
    var notebookId by remember { mutableStateOf(note.notebookId) }
    var folderId by remember { mutableStateOf(note.folderId) }
    var targetNumber by remember { mutableStateOf(note.targetNumber ?: "") }
    var targetWord by remember { mutableStateOf(note.targetWord ?: "") }
    var content by remember { mutableStateOf(note.content) }
    var tagsInput by remember { mutableStateOf(note.tags.joinToString(", ")) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Edit, 1 = Live Preview
    var isGeneratingHook by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("← रद्द करें")
            }

            Text(
                text = "नोट संपादक | Note Editor",
                fontSize = 18.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = {
                    val parsedTags = tagsInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val updatedNote = note.copy(
                        title = title.ifBlank { "Untitled Note" },
                        notebookId = notebookId,
                        folderId = folderId,
                        targetNumber = targetNumber.ifBlank { null },
                        targetWord = targetWord.ifBlank { null },
                        content = content,
                        tags = parsedTags,
                        updatedAt = System.currentTimeMillis()
                    )
                    onSave(updatedNote)
                }
            ) {
                Text("सहेजें (Save)")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tab Row: Edit vs Preview
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("✏️ संपादक (Edit)") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("📖 पूर्वावलोकन (Preview)") })
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            // Edit Mode
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("शीर्षक | Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var notebookMenuExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { notebookMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "नोटबुक: ${notebooks.find { it.id == notebookId }?.name ?: "Personal Notes"}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(
                            expanded = notebookMenuExpanded,
                            onDismissRequest = { notebookMenuExpanded = false }
                        ) {
                            notebooks.forEach { notebook ->
                                DropdownMenuItem(
                                    text = { Text(notebook.name, maxLines = 1) },
                                    onClick = {
                                        notebookId = notebook.id
                                        if (folders.none { it.id == folderId && it.notebookId == notebook.id }) folderId = null
                                        notebookMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    var folderMenuExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { folderMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val folderLabel = folders.find { it.id == folderId }?.name ?: "रूट फ़ोल्डर"
                            Text("फ़ोल्डर: $folderLabel", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        DropdownMenu(expanded = folderMenuExpanded, onDismissRequest = { folderMenuExpanded = false }) {
                            DropdownMenuItem(text = { Text("रूट फ़ोल्डर") }, onClick = {
                                folderId = null
                                folderMenuExpanded = false
                            })
                            folders.filter { it.notebookId == notebookId }.forEach { folder ->
                                DropdownMenuItem(text = { Text(folder.name, maxLines = 1) }, onClick = {
                                    folderId = folder.id
                                    folderMenuExpanded = false
                                })
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = targetNumber,
                        onValueChange = { targetNumber = it },
                        label = { Text("Number Code") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = targetWord,
                        onValueChange = { word ->
                            targetWord = word
                            if (word.isNotBlank()) {
                                val enc = PhoneticMatraEncoder.encode(word)
                                if (enc.isValid && targetNumber.isBlank()) {
                                    targetNumber = enc.code
                                }
                            }
                        },
                        label = { Text("Target Word (Hindi)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // AI Hook Assistant Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tags: comma separated",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            if (targetWord.isNotBlank() || targetNumber.isNotBlank()) {
                                isGeneratingHook = true
                                onGenerateAiHook(targetNumber, targetWord) { hook ->
                                    isGeneratingHook = false
                                    content = if (content.isNotBlank()) {
                                        content + "\n\n### 🧠 Memory Hook\n" + hook
                                    } else {
                                        "### 🧠 Memory Hook\n" + hook
                                    }
                                }
                            }
                        },
                        enabled = !isGeneratingHook && (targetWord.isNotBlank() || targetNumber.isNotBlank()),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isGeneratingHook) "Generating..." else "✨ AI Memory Hook", fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    placeholder = { Text("e.g. peg, history, physics, exam") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Formatting Quick Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { content += " [[Word]] " },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) { Text("[[Link]]", fontSize = 11.sp) }
                    OutlinedButton(
                        onClick = { content += "\n# Heading 1\n" },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) { Text("# H1", fontSize = 11.sp) }
                    OutlinedButton(
                        onClick = { content += "\n### Section\n" },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) { Text("### H3", fontSize = 11.sp) }
                    OutlinedButton(
                        onClick = { content += " **Bold** " },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) { Text("B", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    OutlinedButton(
                        onClick = { content += "\n- Bullet item\n" },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) { Text("• List", fontSize = 11.sp) }
                    OutlinedButton(
                        onClick = { content += "\n> महत्वपूर्ण सूत्र\n" },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) { Text("❝ Quote", fontSize = 11.sp) }
                }

                // Main Markdown Content Area
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("सामग्री (Markdown Content)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                )
            }
        } else {
            // Live Preview Tab inside Editor using the same rich Markdown renderer
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = title.ifBlank { "Untitled Note" },
                        fontSize = 22.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    MarkdownDocumentContent(
                        markdown = content,
                        onWordBacklinkClick = onWordBacklinkClick
                    )
                }
            }
        }
    }
}

private fun createEmptyNote(notebookId: String): Note {
    val now = System.currentTimeMillis()
    return Note(
        id = UUID.randomUUID().toString(),
        notebookId = notebookId,
        folderId = null,
        title = "",
        content = "",
        templateType = "FREE_MARKDOWN",
        targetNumber = null,
        targetWord = null,
        tags = emptyList(),
        isFavorite = false,
        createdAt = now,
        updatedAt = now
    )
}

private fun createTemplateNote(notebookId: String, templateType: String): Note {
    val now = System.currentTimeMillis()
    val initialContent = when (templateType) {
        "PEG_WORD" -> """
# Peg Word Mnemonic Record

- **Target Word**: 
- **Phonetic Matra Code**: 
- **Mental Picture**: Vivid, bizarre, emotional scene.
- **Visual Triggers**: 
- **Sentence in Context**: 

## Memory Story Hook
Imagine...
        """.trimIndent()

        "VOCABULARY" -> """
# Hindi Vocabulary Builder

- **Word (शब्द)**: 
- **Transliteration**: 
- **Primary Meaning**: 
- **English Translation**: 
- **Number Code**: 

## Examples & Usage
1. 

## Synonyms & Related Words
- 
        """.trimIndent()

        "NUMBER_MEMORY" -> """
# Number Memory Sequence

- **Sequence**: 
- **Chunk Breakdown**:
  - Chunk 1: [Word]
  - Chunk 2: [Word]
- **Story Progression**:
  1.
  2.
        """.trimIndent()

        else -> """
# New Note

Enter note content here...
        """.trimIndent()
    }

    return Note(
        id = UUID.randomUUID().toString(),
        notebookId = notebookId,
        folderId = null,
        title = when (templateType) {
            "PEG_WORD" -> "Peg Word Record"
            "VOCABULARY" -> "Vocabulary Record"
            "NUMBER_MEMORY" -> "Number Sequence Note"
            else -> "Quick Note"
        },
        content = initialContent,
        templateType = templateType,
        targetNumber = null,
        targetWord = null,
        tags = listOf(templateType.lowercase()),
        isFavorite = false,
        createdAt = now,
        updatedAt = now
    )
}
