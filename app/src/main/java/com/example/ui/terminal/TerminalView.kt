package com.example.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.workspace.TerminalLine
import com.example.ui.workspace.TerminalType
import com.example.ui.workspace.WorkspaceViewModel
import kotlinx.coroutines.launch

@Composable
fun TerminalView(
    viewModel: WorkspaceViewModel,
    modifier: Modifier = Modifier
) {
    val logs by viewModel.terminalLogs.collectAsState()
    val tabs by viewModel.terminalTabs.collectAsState()
    val activeTab by viewModel.activeTerminalTab.collectAsState()

    var activeInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to lowest output line
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(logs.size - 1)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617)) // Deep terminals slate
            .padding(8.dp)
    ) {
        // Tab Headers Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Terminal emulator",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Scrollable tabs List
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEach { tabName ->
                    val isActive = tabName == activeTab
                    Card(
                        modifier = Modifier
                            .clickable {
                                // VM can handle multiple separate logging files or simple switches
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) Color(0xFF1E293B) else Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tabName,
                                fontSize = 10.sp,
                                color = if (isActive) Color(0xFF34D399) else Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Divider(color = Color(0xFF1E293B), thickness = 1.dp)

        // Logs Output list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(logs) { logLine ->
                TerminalLogItem(logLine)
            }
        }

        // Quick tapping shortcut helpers row below editor
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val shortcuts = listOf("help", "clear", "ls", "python main.py", "npm install", "git status")
            shortcuts.forEach { shortcut ->
                Card(
                    modifier = Modifier.clickable {
                        viewModel.runCommandLine(shortcut)
                    },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = shortcut,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Active Prompt Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B1329))
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "codespark:~# ",
                color = Color(0xFF38BDF8),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            BasicTextField(
                value = activeInput,
                onValueChange = { activeInput = it },
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(Color(0xFF10B981)),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (activeInput.isNotEmpty()) {
                            viewModel.runCommandLine(activeInput)
                            activeInput = ""
                        }
                    }
                ),
                modifier = Modifier.weight(1f)
            )

            if (activeInput.isNotEmpty()) {
                TextButton(
                    onClick = {
                        viewModel.runCommandLine(activeInput)
                        activeInput = ""
                    },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("RUN", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TerminalLogItem(line: TerminalLine) {
    val color = when (line.type) {
        TerminalType.INPUT -> Color(0xFFF59E0B) // Goldish Amber
        TerminalType.INFO -> Color(0xFFE2E8F0) // Subtle white
        TerminalType.SUCCESS -> Color(0xFF10B981) // Neon green
        TerminalType.WARN -> Color(0xFFFBBF24) // Yellowish warn
        TerminalType.ERROR -> Color(0xFFEF4444) // Bright red
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        Text(
            text = "[${line.sender}] ",
            color = Color(0xFF64748B),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = line.message,
            color = color,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
