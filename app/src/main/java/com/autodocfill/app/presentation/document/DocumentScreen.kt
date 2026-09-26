package com.autodocfill.app.presentation.document

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.autodocfill.app.data.model.Document
import com.autodocfill.app.data.model.DocumentStatus
import com.autodocfill.app.presentation.signature.SignatureCanvas
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Modern Document list screen with Material Design 3
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentListScreen(
    navController: NavController,
    viewModel: DocumentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val documents by viewModel.documents.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteConfirm by remember { mutableStateOf<Document?>(null) }
    var signingDocument by remember { mutableStateOf<Document?>(null) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importPdf(it) }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DocumentEvent.OpenPdf -> context.openPdf(event.file)
                is DocumentEvent.SharePdf -> context.sharePdf(event.file, event.title)
            }
        }
    }

    val message = uiState.errorMessage ?: uiState.successMessage
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Documents", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "${documents.size} document${if (documents.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { pdfPickerLauncher.launch("application/pdf") },
                icon = { Icon(Icons.Filled.Upload, "Upload") },
                text = { Text("Upload PDF") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when {
                documents.isEmpty() -> {
                    EmptyDocumentState(
                        onUploadClick = { pdfPickerLauncher.launch("application/pdf") },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(documents) { document ->
                            ModernDocumentCard(
                                document = document,
                                onViewClick = { viewModel.viewDocument(document) },
                                onAutofillClick = { viewModel.autofillDocument(document) },
                                onManualEditClick = { 
                                    // Navigate to manual edit screen
                                    navController.navigate("manual_edit/${document.id}")
                                },
                                onExportClick = { viewModel.exportDocument(document) },
                                onSignClick = { signingDocument = document },
                                onDeleteClick = { showDeleteConfirm = document },
                                onRetryClick = { viewModel.retryProcessing(document) }
                            )
                        }
                        
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
            
            // Processing overlay
            if (uiState.isProcessing) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(64.dp),
                            strokeWidth = 6.dp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            uiState.processingStep ?: "Processing document...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    showDeleteConfirm?.let { document ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            icon = { Icon(Icons.Filled.Delete, null) },
            title = { Text("Delete Document?") },
            text = { Text("Are you sure you want to delete \"${document.documentName}\"? This action cannot be undone.") },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        viewModel.deleteDocument(document)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Signature capture
    signingDocument?.let { document ->
        Dialog(
            onDismissRequest = { signingDocument = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            SignatureCanvas(
                onSignatureComplete = { bitmap ->
                    viewModel.signDocument(document, bitmap)
                    signingDocument = null
                },
                onCancel = { signingDocument = null }
            )
        }
    }
}

private fun Context.pdfUri(file: File): Uri =
    FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

private fun Context.openPdf(file: File) {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(pdfUri(file), "application/pdf")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "No app available to open PDFs", Toast.LENGTH_SHORT).show()
    }
}

private fun Context.sharePdf(file: File, title: String) {
    val intent = Intent(Intent.ACTION_SEND)
        .setType("application/pdf")
        .putExtra(Intent.EXTRA_STREAM, pdfUri(file))
        .putExtra(Intent.EXTRA_SUBJECT, title)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    startActivity(Intent.createChooser(intent, "Save or share PDF"))
}

@Composable
fun EmptyDocumentState(
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.Description,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "No documents yet",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Upload a PDF form to get started\nwith automatic form filling",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        FilledTonalButton(
            onClick = onUploadClick,
            modifier = Modifier.height(56.dp)
        ) {
            Icon(Icons.Filled.Upload, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Upload PDF", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernDocumentCard(
    document: Document,
    onViewClick: () -> Unit,
    onAutofillClick: () -> Unit,
    onManualEditClick: () -> Unit,
    onExportClick: () -> Unit,
    onSignClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US) }
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        color = when (document.status) {
                            DocumentStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                            DocumentStatus.PROCESSING -> MaterialTheme.colorScheme.tertiaryContainer
                            DocumentStatus.FILLED, DocumentStatus.EXPORTED -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                when (document.status) {
                                    DocumentStatus.ERROR -> Icons.Filled.Error
                                    DocumentStatus.PROCESSING -> Icons.Filled.HourglassEmpty
                                    DocumentStatus.FILLED -> Icons.Filled.CheckCircle
                                    DocumentStatus.EXPORTED -> Icons.Filled.FileDownload
                                    else -> Icons.Outlined.Description
                                },
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = when (document.status) {
                                    DocumentStatus.ERROR -> MaterialTheme.colorScheme.onErrorContainer
                                    DocumentStatus.PROCESSING -> MaterialTheme.colorScheme.onTertiaryContainer
                                    DocumentStatus.FILLED, DocumentStatus.EXPORTED -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            )
                        }
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            document.documentName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            dateFormat.format(Date(document.uploadedAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, "Options")
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("View PDF") },
                            onClick = {
                                onViewClick()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Outlined.Visibility, null) }
                        )
                        
                        // Always allow manual edit
                        DropdownMenuItem(
                            text = { Text("Manual Edit") },
                            onClick = {
                                onManualEditClick()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) }
                        )
                        
                        if (document.isFillable && document.detectedFieldsCount > 0) {
                            DropdownMenuItem(
                                text = { Text("Export") },
                                onClick = {
                                    onExportClick()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Outlined.Download, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Sign") },
                                onClick = {
                                    onSignClick()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Outlined.Draw, null) }
                            )
                        }
                        
                        if (document.status == DocumentStatus.ERROR || 
                            document.status == DocumentStatus.PROCESSING) {
                            DropdownMenuItem(
                                text = { Text("Retry Processing") },
                                onClick = {
                                    onRetryClick()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Outlined.Refresh, null) }
                            )
                        }
                        
                        Divider()
                        
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                onDeleteClick()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error,
                                leadingIconColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status and details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModernDocumentStatusChip(document.status)
                
                if (document.isFillable && document.status != DocumentStatus.PROCESSING) {
                    AssistChip(
                        onClick = { },
                        label = { 
                            Text(
                                "Fillable",
                                style = MaterialTheme.typography.labelSmall
                            ) 
                        },
                        leadingIcon = { 
                            Icon(
                                Icons.Filled.CheckCircle, 
                                null, 
                                modifier = Modifier.size(14.dp)
                            ) 
                        },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DocumentDetailBadge(
                    icon = Icons.Outlined.Article,
                    label = "${document.pageCount} pages"
                )
                DocumentDetailBadge(
                    icon = Icons.Outlined.Assignment,
                    label = "${document.detectedFieldsCount} fields"
                )
            }
            
            // Progress indicator
            if (document.detectedFieldsCount > 0 && document.status != DocumentStatus.PROCESSING) {
                Spacer(modifier = Modifier.height(16.dp))
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Completion",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${document.filledFieldsCount}/${document.detectedFieldsCount}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = if (document.detectedFieldsCount > 0) {
                            document.filledFieldsCount.toFloat() / document.detectedFieldsCount
                        } else 0f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
            
            // Action buttons
            if (document.status == DocumentStatus.READY_TO_FILL || 
                document.status == DocumentStatus.FILLED ||
                (document.status == DocumentStatus.PROCESSING && document.detectedFieldsCount == 0)) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (document.status == DocumentStatus.READY_TO_FILL || 
                        document.status == DocumentStatus.FILLED) {
                        FilledTonalButton(
                            onClick = onAutofillClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Autofill")
                        }
                    }
                    
                    OutlinedButton(
                        onClick = onManualEditClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Manual Edit")
                    }
                }
            }
            
            // Error message
            if (document.status == DocumentStatus.ERROR) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Filled.Error,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column {
                            Text(
                                "Processing failed",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Unable to detect form fields. Try manual edit or retry processing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            
            // Processing message
            if (document.status == DocumentStatus.PROCESSING) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            if (document.detectedFieldsCount == 0) {
                                "Analyzing document structure..."
                            } else {
                                "Processing ${document.detectedFieldsCount} fields..."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentDetailBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ModernDocumentStatusChip(status: DocumentStatus) {
    val (label, containerColor, contentColor) = when (status) {
        DocumentStatus.UPLOADED -> Triple(
            "Uploaded",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        DocumentStatus.PROCESSING -> Triple(
            "Processing",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        DocumentStatus.READY_TO_FILL -> Triple(
            "Ready",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        DocumentStatus.FILLED -> Triple(
            "Filled",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        DocumentStatus.REVIEWED -> Triple(
            "Reviewed",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        DocumentStatus.SIGNED -> Triple(
            "Signed",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        DocumentStatus.EXPORTED -> Triple(
            "Exported",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        DocumentStatus.ERROR -> Triple(
            "Error",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
    }
    
    AssistChip(
        onClick = { },
        label = { 
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            ) 
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
            labelColor = contentColor
        ),
        modifier = Modifier.height(28.dp)
    )
}
