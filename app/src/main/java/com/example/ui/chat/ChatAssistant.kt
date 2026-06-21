package com.example.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessageEntity
import com.example.ui.workspace.WorkspaceViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatAssistant(
    viewModel: WorkspaceViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.chatMessages.collectAsState(initial = emptyList())
    val isLoading by viewModel.aiLoading.collectAsState()
    val activeFile by viewModel.activeFile.collectAsState()

    var chatInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to lowest chat line
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E293B)) // Charcoal Slate Background for AI Workspace
            .padding(10.dp)
    ) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "SparkCoder Companion",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Powered by Gemini 3.5 AI Core",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = { viewModel.clearChat() }) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear Chat logs",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Divider(color = Color(0xFF334155))

        // Chat logs viewport
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OfflineBolt,
                            contentDescription = "AI offline",
                            tint = Color(0xFFE2E8F0).copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Ask anything about your codebase",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Or tap custom AI actions below to auto-analyze files.",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { message ->
                        ChatBubbleItem(message, onApplyCode = { extracted ->
                            viewModel.applyCodeToActiveFile(extracted)
                        })
                    }
                    if (isLoading) {
                        item {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "SparkCoder is processing codebase modules...",
                                    fontSize = 11.sp,
                                    color = Color.LightGray,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Analysis Shortcuts Indicators
        if (activeFile != null) {
            Text(
                text = "File Context Enabled: $activeFile",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4ADE80),
                modifier = Modifier.padding(bottom = 4.dp),
                fontFamily = FontFamily.Monospace
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val prompts = listOf("Explain file", "Refactor Code", "Write logic tests")
                prompts.forEach { prompt ->
                    Card(
                        modifier = Modifier.clickable {
                            viewModel.sendChatMessage("$prompt to optimize efficiency and maintain best formatting conventions according to design patterns.")
                        },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = prompt,
                            fontSize = 10.sp,
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Chat Input box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF334155))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = chatInput,
                onValueChange = { chatInput = it },
                placeholder = { Text("Ask SparkCoder to gen template, fix bugs...", fontSize = 12.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = TextStyle(fontSize = 13.sp),
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    if (chatInput.trim().isNotEmpty()) {
                        viewModel.sendChatMessage(chatInput)
                        chatInput = ""
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send prompt",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ChatBubbleItem(
    msg: ChatMessageEntity,
    onApplyCode: (String) -> Unit
) {
    val isUser = msg.role == "user"
    val align = if (isUser) Alignment.End else Alignment.Start
    val bg = if (isUser) Color(0xFF0F172A) else Color(0xFF334155) // Slate Dark vs Medium

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .padding(horizontal = 2.dp),
            shape = RoundedCornerShape(
                topStart = 10.dp,
                topEnd = 10.dp,
                bottomStart = if (isUser) 10.dp else 0.dp,
                bottomEnd = if (isUser) 0.dp else 10.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bg)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Sender label
                Text(
                    text = if (isUser) "You" else "SparkCoder",
                    fontSize = 10.sp,
                    color = if (isUser) Color(0xFF60A5FA) else Color(0xFF34D399),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp),
                    fontFamily = FontFamily.Monospace
                )

                SelectionContainer {
                    Text(
                        text = msg.text,
                        fontSize = 12.sp,
                        color = Color.White,
                        lineHeight = 16.sp
                    )
                }

                // Code extraction overlay button detector
                val extractedCode = remember(msg.text) { extractMarkdownCode(msg.text) }
                if (extractedCode != null && !isUser) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onApplyCode(extractedCode) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Code to active file", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// Markdown extractor logic
private fun extractMarkdownCode(payload: String): String? {
    if (!payload.contains("```")) return null
    try {
        val blocks = payload.split("```")
        if (blocks.size >= 3) {
            // Usually index 1 displays the content inside block tags
            // Let's strip initial language tag lines like "python\n" or "javascript\n"
            var raw = blocks[1]
            val lines = raw.split("\n")
            if (lines.isNotEmpty() && lines[0].trim().length <= 12 && !lines[0].contains(" ")) {
                raw = lines.subList(1, lines.size).joinToString("\n")
            }
            return raw.trim()
        }
    } catch (e: Exception) {
        // Skip
    }
    return null
}
