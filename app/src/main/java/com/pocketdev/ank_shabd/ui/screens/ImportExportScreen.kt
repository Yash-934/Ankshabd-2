package com.pocketdev.ank_shabd.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketdev.ank_shabd.domain.models.*
import com.pocketdev.ank_shabd.ui.components.CodeBadge
import com.pocketdev.ank_shabd.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportScreen(
    onGeneratePreview: (content: String, format: String, (ImportValidationPreview) -> Unit) -> Unit,
    onExecuteImport: (content: String, format: String, policy: String, (ImportReport) -> Unit) -> Unit,
    onExportBackupJson: ((String?, String?) -> Unit) -> Unit,
    onExportBackupFile: (Uri, (String) -> Unit) -> Unit,
    onExportCsv: ((String) -> Unit) -> Unit,
    onRestoreBackupJson: (String, (RestoreResult) -> Unit) -> Unit,
    onRestoreBackupFile: (Uri, (RestoreResult) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0 = Import, 1 = Export & Backup
    var importFormat by remember { mutableStateOf("JSON") } // "JSON" or "CSV"
    var duplicatePolicy by remember { mutableStateOf("SKIP") } // "SKIP" or "OVERWRITE"
    var importText by remember { mutableStateOf("") }
    var previewResult by remember { mutableStateOf<ImportValidationPreview?>(null) }
    var importReport by remember { mutableStateOf<ImportReport?>(null) }
    var isValidating by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    var restoreInputText by remember { mutableStateOf("") }
    var pendingExportName by remember { mutableStateOf<String?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var restoreResult by remember { mutableStateOf<RestoreResult?>(null) }
    var isRestoring by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var statusNotice by remember { mutableStateOf<String?>(null) }
    val createFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val exportName = pendingExportName
        pendingExportName = null
        if (uri != null && exportName != null) {
            onExportBackupFile(uri) { message -> statusNotice = message }
        }
    }
    val openFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                pendingRestoreUri = uri
                showRestoreConfirmDialog = true
                statusNotice = "बैकअप फ़ाइल चुनी गई। पुनर्स्थापना की पुष्टि करें।"
            } catch (e: Exception) {
                statusNotice = "फ़ाइल पढ़ना विफल: ${e.localizedMessage ?: e.javaClass.simpleName}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "डेटा प्रबंधन | Data Management",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Transactional import, validation preview, versioned backup & restore",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("आयात (Import)") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("बैकअप / पुनर्स्थापना (Backup)") })
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    // Format & Policy Controls Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Format:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = importFormat == "JSON",
                                        onClick = { importFormat = "JSON" },
                                        label = { Text("JSON") }
                                    )
                                    FilterChip(
                                        selected = importFormat == "CSV",
                                        onClick = { importFormat = "CSV" },
                                        label = { Text("CSV") }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Duplicate Policy:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = duplicatePolicy == "SKIP",
                                        onClick = { duplicatePolicy = "SKIP" },
                                        label = { Text("Skip Existing") }
                                    )
                                    FilterChip(
                                        selected = duplicatePolicy == "OVERWRITE",
                                        onClick = { duplicatePolicy = "OVERWRITE" },
                                        label = { Text("Overwrite") }
                                    )
                                }
                            }

                            // Quick sample loader
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Presets:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                OutlinedButton(
                                    onClick = {
                                        importFormat = "JSON"
                                        importText = """
[
  { "word": "कमल", "code": "19", "meaningHi": "एक सुंदर फूल", "meaningEn": "Lotus" },
  { "word": "दवा", "code": "28", "meaningHi": "औषधि", "meaningEn": "Medicine" },
  { "word": "हंस", "code": "57", "meaningHi": "श्वेत पक्षी", "meaningEn": "Swan" }
]
                                        """.trimIndent()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Sample JSON", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        importFormat = "CSV"
                                        importText = """
word,code,meaningHi,meaningEn
कमल,19,एक सुंदर फूल,Lotus
दवा,28,औषधि,Medicine
शहर,04,नगर,City
                                        """.trimIndent()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Sample CSV", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Text Input Area
                item {
                    OutlinedTextField(
                        value = importText,
                        onValueChange = {
                            importText = it
                            previewResult = null
                            importReport = null
                        },
                        label = { Text("Paste JSON or CSV to import") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    )
                }

                // Action Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (importText.isNotBlank()) {
                                    isValidating = true
                                    onGeneratePreview(importText, importFormat) { preview ->
                                        previewResult = preview
                                        isValidating = false
                                    }
                                }
                            },
                            enabled = !isValidating && importText.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isValidating) "Validating..." else "🔍 1. Preview & Validate")
                        }

                        Button(
                            onClick = {
                                if (importText.isNotBlank()) {
                                    isImporting = true
                                    onExecuteImport(importText, importFormat, duplicatePolicy) { report ->
                                        importReport = report
                                        isImporting = false
                                        statusNotice = "Import completed successfully!"
                                    }
                                }
                            },
                            enabled = !isImporting && previewResult != null,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isImporting) "Importing..." else "📥 2. Execute Import")
                        }
                    }
                }

                // Validation Preview Report
                previewResult?.let { preview ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "📊 Import Validation Preview",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${preview.totalRecords}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("Total", fontSize = 11.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${preview.validMatchesCount}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EmeraldGreen)
                                        Text("Valid Matches", fontSize = 11.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${preview.codeMismatchesCount}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (preview.codeMismatchesCount > 0) MaterialTheme.colorScheme.error else Color.Gray)
                                        Text("Mismatches", fontSize = 11.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${preview.duplicateCount}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AmberGold)
                                        Text("Existing", fontSize = 11.sp)
                                    }
                                }

                                if (preview.sampleMismatches.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("⚠️ Code Mismatches Detected:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                    preview.sampleMismatches.forEach { (word, detail) ->
                                        Text("• $word: $detail", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }

                                if (preview.unsupportedSymbols.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("⚠️ Unknown / Unsupported Characters:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = AmberGold)
                                    preview.unsupportedSymbols.forEach { sym ->
                                        Text("• $sym", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Final Import Report
                importReport?.let { report ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "✅ Import Transaction Successful",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("• Accepted New Words: ${report.acceptedCount}", fontSize = 13.sp)
                                Text("• Updated Words: ${report.updatedCount}", fontSize = 13.sp)
                                Text("• Skipped Duplicates: ${report.skippedCount}", fontSize = 13.sp)
                                if (report.rejectedCount > 0) {
                                    Text("• Rejected Records: ${report.rejectedCount}", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Backup & Restore Tab
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Export Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("डेटा बैकअप निर्यात | Export Full Backup", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Export all verified words, custom pegs, notes, and revision data as versioned JSON.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onExportBackupJson { json, error ->
                                            if (error != null) {
                                                statusNotice = error
                                                return@onExportBackupJson
                                            }
                                            if (json == null) {
                                                statusNotice = "बैकअप निर्यात विफल हुआ।"
                                                return@onExportBackupJson
                                            }
                                            if (json.length > 200_000) {
                                                statusNotice = "बैकअप clipboard के लिए बहुत बड़ा है। कृपया फ़ाइल निर्यात करें।"
                                                return@onExportBackupJson
                                            }
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cm.setPrimaryClip(ClipData.newPlainText("AnkShabd Backup", json))
                                            statusNotice = "Complete JSON Backup copied to clipboard!"
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("📦 Export JSON")
                                }

                                OutlinedButton(
                                    onClick = {
                                        onExportCsv { csv ->
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cm.setPrimaryClip(ClipData.newPlainText("AnkShabd Dictionary CSV", csv))
                                            statusNotice = "Dictionary CSV copied to clipboard!"
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("📄 Export CSV")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    pendingExportName = "AnkShabd-Backup.json"
                                    createFileLauncher.launch("AnkShabd-Backup.json")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("💾 पूरी बैकअप फ़ाइल निर्यात करें")
                            }

                            statusNotice?.let { notice ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(notice, fontSize = 12.sp, color = EmeraldGreen, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Restore Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("डेटा पुनर्स्थापना | Restore from Backup", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Paste a previously exported AnkShabd JSON backup to restore your knowledge base.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = restoreInputText,
                                onValueChange = { restoreInputText = it },
                                label = { Text("Paste Backup JSON") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { showRestoreConfirmDialog = true },
                                enabled = !isRestoring && (restoreInputText.isNotBlank() || pendingRestoreUri != null),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(if (isRestoring) "Restoring..." else "⚠️ Restore Database")
                            }
                            OutlinedButton(
                                onClick = { openFileLauncher.launch(arrayOf("application/json", "text/*")) },
                                enabled = !isRestoring,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("📂 बैकअप फ़ाइल से पुनर्स्थापित करें")
                            }

                            restoreResult?.let { res ->
                                Spacer(modifier = Modifier.height(10.dp))
                                if (res.isSuccess) {
                                    Text(
                                        text = "✅ Restore Successful! Restored ${res.wordsRestored} words, ${res.notesRestored} notes, ${res.flashcardsRestored} flashcards.",
                                        fontSize = 12.sp,
                                        color = EmeraldGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = "❌ Restore Failed: ${res.errorMessage}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("पुनर्स्थापना की पुष्टि करें | Confirm Restore") },
            text = {
                Text("This action will merge the backup records into your database. Existing matching records will be updated. Do you wish to proceed?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        isRestoring = true
                        val backupUri = pendingRestoreUri
                        if (backupUri != null) {
                            onRestoreBackupFile(backupUri) { res ->
                                restoreResult = res
                                isRestoring = false
                                pendingRestoreUri = null
                            }
                        } else {
                            onRestoreBackupJson(restoreInputText) { res ->
                                restoreResult = res
                                isRestoring = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Proceed with Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
