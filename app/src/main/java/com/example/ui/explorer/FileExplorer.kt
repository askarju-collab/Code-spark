package com.example.ui.explorer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.workspace.FileNode
import com.example.ui.workspace.WorkspaceViewModel

@Composable
fun FileExplorer(
    viewModel: WorkspaceViewModel,
    modifier: Modifier = Modifier
) {
    val fileTree by viewModel.projectFiles.collectAsState()
    val activeFile by viewModel.activeFile.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var createIsFolder by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var renameTargetNode by remember { mutableStateOf<FileNode?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Projects & Files",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Row {
                IconButton(onClick = {
                    createIsFolder = false
                    newFileName = ""
                    showCreateDialog = true
                }) {
                    Icon(
                        imageVector = Icons.Default.NoteAdd,
                        contentDescription = "New File",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = {
                    createIsFolder = true
                    newFileName = ""
                    showCreateDialog = true
                }) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = "New Folder",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = { viewModel.refreshFiles() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh explorer",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        if (fileTree.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Project contains no files yet.\nUse + buttons to add code assets.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(fileTree) { rootNode ->
                    FileTreeItem(
                        node = rootNode,
                        level = 0,
                        activeFile = activeFile,
                        onFileClick = { viewModel.openFile(rootNode.relativePath) },
                        onRenameClick = {
                            renameTargetNode = rootNode
                            newFileName = rootNode.name
                            showRenameDialog = true
                        },
                        onDeleteClick = { viewModel.deleteFile(rootNode.relativePath) },
                        onZipClick = { viewModel.compressFolderZip(rootNode.relativePath) },
                        onUnzipClick = { viewModel.extractZipFile(rootNode.relativePath) }
                    )
                }
            }
        }
    }

    // New item dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create ${if (createIsFolder) "Folder" else "File"}") },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("Relative path (e.g. src/app.py)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newFileName.isNotEmpty()) {
                        viewModel.createFile(newFileName, createIsFolder)
                    }
                    showCreateDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename dialog
    if (showRenameDialog && renameTargetNode != null) {
        val target = renameTargetNode!!
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename ${target.name}") },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newFileName.isNotEmpty()) {
                        viewModel.renameFile(target.relativePath, newFileName)
                    }
                    showRenameDialog = false
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FileTreeItem(
    node: FileNode,
    level: Int,
    activeFile: String?,
    onFileClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onZipClick: () -> Unit,
    onUnzipClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val trailingOptionsExpanded = remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (level * 12).dp)
                .clickable {
                    if (node.isDirectory) {
                        isExpanded = !isExpanded
                    } else {
                        onFileClick()
                    }
                },
            colors = CardDefaults.cardColors(
                containerColor = if (activeFile == node.relativePath) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                } else Color.Transparent
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (node.isDirectory) {
                            if (isExpanded) Icons.Outlined.FolderOpen else Icons.Default.Folder
                        } else {
                            if (node.name.endsWith(".zip")) Icons.Default.FolderZip
                            else Icons.Outlined.Description
                        },
                        contentDescription = "File Icon",
                        tint = if (node.isDirectory) Color(0xFFFFC107) else Color(0xFF60A5FA),
                        modifier = Modifier.size(20.dp).padding(end = 4.dp)
                    )
                    Text(
                        text = node.name,
                        fontSize = 13.sp,
                        color = if (node.isDirectory) Color(0xFFE2E8F0) else Color(0xFFF1F5F9),
                        fontWeight = if (node.isDirectory) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                Box {
                    IconButton(
                        onClick = { trailingOptionsExpanded.value = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = trailingOptionsExpanded.value,
                        onDismissRequest = { trailingOptionsExpanded.value = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = {
                                trailingOptionsExpanded.value = false
                                onRenameClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        if (node.isDirectory) {
                            DropdownMenuItem(
                                text = { Text("Zip Directory") },
                                onClick = {
                                    trailingOptionsExpanded.value = false
                                    onZipClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) }
                            )
                        } else if (node.name.endsWith(".zip")) {
                            DropdownMenuItem(
                                text = { Text("Extract Zip") },
                                onClick = {
                                    trailingOptionsExpanded.value = false
                                    onUnzipClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Unarchive, contentDescription = null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color.Red) },
                            onClick = {
                                trailingOptionsExpanded.value = false
                                onDeleteClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                        )
                    }
                }
            }
        }

        if (node.isDirectory && isExpanded) {
            node.children.forEach { child ->
                FileTreeItem(
                    node = child,
                    level = level + 1,
                    activeFile = activeFile,
                    onFileClick = { onFileClick() },
                    onRenameClick = { onRenameClick() },
                    onDeleteClick = { onDeleteClick() },
                    onZipClick = { onZipClick() },
                    onUnzipClick = { onUnzipClick() }
                )
            }
        }
    }
}
