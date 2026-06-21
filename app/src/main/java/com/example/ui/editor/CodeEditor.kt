package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.workspace.WorkspaceViewModel

@Composable
fun CodeEditor(
    viewModel: WorkspaceViewModel,
    modifier: Modifier = Modifier
) {
    val activeFile by viewModel.activeFile.collectAsState()
    val rawContent by viewModel.activeFileContent.collectAsState()

    var textState by remember { mutableStateOf("") }
    var currentFileTracked by remember { mutableStateOf<String?>(null) }

    // Sync editing content when user opens a different file
    if (activeFile != currentFileTracked) {
        textState = rawContent
        currentFileTracked = activeFile
    }

    var showSearchRow by remember { mutableStateOf(false) }
    var searchPhrase by remember { mutableStateOf("") }
    var replacePhrase by remember { mutableStateOf("") }

    val fileExtension = activeFile?.substringAfterLast(".", "") ?: ""

    Column(modifier = modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        // Tab controls/header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = activeFile ?: "Select a file to edit code",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (activeFile != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showSearchRow = !showSearchRow },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            // Beautify basic code spacing indentation rules
                            textState = formatSourceCode(textState, fileExtension)
                            viewModel.saveActiveFile(textState)
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = "AutoFormat Format Code",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.saveActiveFile(textState) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        // Search and Replace bars
        if (showSearchRow && activeFile != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchPhrase,
                            onValueChange = { searchPhrase = it },
                            placeholder = { Text("Find target...", fontSize = 11.sp) },
                            textStyle = TextStyle(fontSize = 12.sp, color = Color.White),
                            modifier = Modifier.weight(1f).height(45.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = replacePhrase,
                            onValueChange = { replacePhrase = it },
                            placeholder = { Text("Replace text with...", fontSize = 11.sp) },
                            textStyle = TextStyle(fontSize = 12.sp, color = Color.White),
                            modifier = Modifier.weight(1f).height(45.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = {
                            if (searchPhrase.isNotEmpty()) {
                                textState = textState.replace(searchPhrase, replacePhrase)
                                viewModel.saveActiveFile(textState)
                            }
                        }) {
                            Icon(Icons.Default.FindReplace, contentDescription = "Replace all", tint = Color.LightGray)
                        }
                    }
                }
            }
        }

        // Editor Scroll Canvas containing line numbers and text fields
        if (activeFile == null) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DataObject,
                        contentDescription = "Code Spark Engine",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Build Something Electric. ⚡",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Open files on the side, or tell the AI to generate a template.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Line counts gutter
                val lineCount = textState.count { it == '\n' } + 1
                val linesText = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFF475569), fontFamily = FontFamily.Monospace, fontSize = 12.sp)) {
                        for (i in 1..lineCount) {
                            append("$i\n")
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = linesText,
                        lineHeight = 18.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Divider(modifier = Modifier.width(1.dp).fillMaxHeight(), color = Color(0xFF334155))

                // Scrollable text input box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF0F172A))
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    BasicTextField(
                        value = textState,
                        onValueChange = {
                            textState = it
                            // Do not write to disk *literally* on every tiny key stroke 
                            // to avoid write-locks, let user hit save or sync on pauses
                        },
                        textStyle = TextStyle(
                            color = Color(0xFFF1F5F9),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        visualTransformation = CodeHighlighter(fileExtension),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Default
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// Simple local code formatting helper
private fun formatSourceCode(text: String, extension: String): String {
    val lines = text.split("\n")
    val builder = StringBuilder()
    var indentLevel = 0
    val singleIndent = "    " // 4 spaces

    for (line in lines) {
        var trimmed = line.trim()
        if (trimmed.isEmpty()) {
            builder.append("\n")
            continue
        }

        // Simple reduction on closing code blocks/braces
        if (trimmed.startsWith("}") || trimmed.startsWith("]") || trimmed.startsWith(")")) {
            indentLevel = maxOf(0, indentLevel - 1)
        }

        val formattedLine = singleIndent.repeat(indentLevel) + trimmed
        builder.append(formattedLine).append("\n")

        // Increase indent on block openings
        if (trimmed.endsWith("{") || trimmed.endsWith(":") || trimmed.endsWith("[")) {
            indentLevel++
        }
    }
    return builder.toString().trimEnd()
}
