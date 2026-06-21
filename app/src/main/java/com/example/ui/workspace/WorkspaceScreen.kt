package com.example.ui.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.TextStyle
import com.example.data.model.ProjectEntity
import com.example.data.model.CommitEntity
import com.example.ui.chat.ChatAssistant
import com.example.ui.editor.CodeEditor
import com.example.ui.explorer.FileExplorer
import com.example.ui.preview.WebPreview
import com.example.ui.terminal.TerminalView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    viewModel: WorkspaceViewModel,
    modifier: Modifier = Modifier
) {
    val currentProject by viewModel.currentProject.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState(initial = emptyList())
    val commits by viewModel.commitsList.collectAsState(initial = emptyList())
    val packages by viewModel.installedPackages.collectAsState()

    var activeSidebarPanel by remember { mutableStateOf("explorer") } // explorer, git, packages, projects
    var sidebarExpanded by remember { mutableStateOf(true) }
    var chatExpanded by remember { mutableStateOf(false) }
    var bottomPanelExpanded by remember { mutableStateOf(true) }
    var activeBottomTab by remember { mutableStateOf("terminal") } // terminal, webview

    // Interactive Dialogs State
    var showProjectSelector by remember { mutableStateOf(false) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showGitCommitDialog by remember { mutableStateOf(false) }

    var newProjectName by remember { mutableStateOf("") }
    var newProjectLanguage by remember { mutableStateOf("Python") }
    var commitMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Code Spark ⚡",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        // Active Project selector capsule
                        Card(
                            onClick = { showProjectSelector = true },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentProject?.name ?: "No project select",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Quick top action bars
                    IconButton(onClick = { showNewProjectDialog = true }) {
                        Icon(Icons.Default.AddBox, contentDescription = "New Project", tint = Color.LightGray)
                    }
                    IconButton(onClick = { showGitCommitDialog = true }) {
                        Icon(Icons.Default.Source, contentDescription = "Local Commit", tint = Color.LightGray)
                    }
                    IconButton(onClick = { chatExpanded = !chatExpanded }) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "AI Companion Chat",
                            tint = if (chatExpanded) MaterialTheme.colorScheme.primary else Color.LightGray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A),
                    titleContentColor = Color.White
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0F172A))
        ) {
            // Collapsible Sidebar Icons Rail
            SidebarRail(
                selectedPanel = activeSidebarPanel,
                onPanelSelected = { panel ->
                    if (activeSidebarPanel == panel) {
                        sidebarExpanded = !sidebarExpanded
                    } else {
                        activeSidebarPanel = panel
                        sidebarExpanded = true
                    }
                },
                sidebarExpanded = sidebarExpanded
            )

            // Split Panels layout
            Row(modifier = Modifier.weight(1f)) {
                // Secondary Left Sidebar panel content (File Explorer / Package list / Local Git history)
                AnimatedVisibility(visible = sidebarExpanded) {
                    Card(
                        modifier = Modifier
                            .width(240.dp)
                            .fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        when (activeSidebarPanel) {
                            "explorer" -> FileExplorer(viewModel)
                            "git" -> GitHistoryTab(
                                commits = commits,
                                onCommitClick = { commit ->
                                    viewModel.gitInit() // trigger log outputs or virtual switches
                                }
                            )
                            "packages" -> LocalPackagesTab(viewModel, packages)
                            "projects" -> ProjectsManagerTab(
                                allProjects = allProjects,
                                activeProj = currentProject,
                                onSelect = { viewModel.selectProject(it) },
                                onDelete = { viewModel.deleteProject(it) }
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.width(1.dp).fillMaxHeight(), color = Color(0xFF334155))

                // Mid primary work area containing Editor (top) and Bottom panels (collapsible Terminal/WebView)
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Box(modifier = Modifier.weight(if (bottomPanelExpanded) 0.55f else 1f)) {
                        CodeEditor(viewModel)
                    }

                    // Collapsible Header For Terminal Drawer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0B1329))
                            .clickable { bottomPanelExpanded = !bottomPanelExpanded }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (bottomPanelExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "CONSOLE & PREVIEWS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Toggle tabs inside bottom console area
                        if (bottomPanelExpanded) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Terminal emulator",
                                    fontSize = 11.sp,
                                    fontWeight = if (activeBottomTab == "terminal") FontWeight.Bold else FontWeight.Normal,
                                    color = if (activeBottomTab == "terminal") Color(0xFF10B981) else Color.Gray,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.clickable { activeBottomTab = "terminal" }
                                )
                                Text(
                                    text = "Live preview",
                                    fontSize = 11.sp,
                                    fontWeight = if (activeBottomTab == "webview") FontWeight.Bold else FontWeight.Normal,
                                    color = if (activeBottomTab == "webview") Color(0xFF38BDF8) else Color.Gray,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.clickable { activeBottomTab = "webview" }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = bottomPanelExpanded, modifier = Modifier.weight(0.45f)) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (activeBottomTab == "terminal") {
                                TerminalView(viewModel)
                            } else {
                                WebPreview(viewModel)
                            }
                        }
                    }
                }

                // AI Chat Assistant Panel on Right side
                AnimatedVisibility(visible = chatExpanded) {
                    Divider(modifier = Modifier.width(1.dp).fillMaxHeight(), color = Color(0xFF334155))
                    Box(modifier = Modifier.width(280.dp).fillMaxHeight()) {
                        ChatAssistant(viewModel)
                    }
                }
            }
        }
    }

    // Git commit message dialog
    if (showGitCommitDialog) {
        AlertDialog(
            onDismissRequest = { showGitCommitDialog = false },
            title = { Text("Local Git Save-Point") },
            text = {
                Column {
                    Text("Register local snapshot backup to offline Git index.", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = commitMessage,
                        onValueChange = { commitMessage = it },
                        label = { Text("Changes summary (message)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (commitMessage.isNotEmpty()) {
                        viewModel.gitAddAll()
                        viewModel.gitCommit(commitMessage)
                        commitMessage = ""
                    }
                    showGitCommitDialog = false
                }) {
                    Text("Create Commit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGitCommitDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }

    // Selector project list modal sheet
    if (showProjectSelector) {
        AlertDialog(
            onDismissRequest = { showProjectSelector = false },
            title = { Text("Switch Project Sandbox") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allProjects) { proj ->
                        Card(
                            onClick = {
                                viewModel.selectProject(proj)
                                showProjectSelector = false
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (proj.id == currentProject?.id) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else Color(0xFF1E293B)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(proj.name, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Language: ${proj.language}", fontSize = 11.sp, color = Color.LightGray)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProjectSelector = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Creation project dialog template selection
    if (showNewProjectDialog) {
        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            title = { Text("Launcher New Spark Project") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newProjectName,
                        onValueChange = { newProjectName = it },
                        label = { Text("Project Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Select Scaffolding Template:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    val options = listOf("Python", "JavaScript", "Html/Css", "Java")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        options.forEach { lang ->
                            val active = lang == newProjectLanguage
                            Card(
                                onClick = { newProjectLanguage = lang },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (active) MaterialTheme.colorScheme.primary else Color(0xFF1E293B)
                                ),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(modifier = Modifier.padding(6.dp), contentAlignment = Alignment.Center) {
                                    Text(lang, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newProjectName.isNotEmpty()) {
                        viewModel.createProject(newProjectName, newProjectLanguage)
                        newProjectName = ""
                    }
                    showNewProjectDialog = false
                }) {
                    Text("Generate Project")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SidebarRail(
    selectedPanel: String,
    onPanelSelected: (String) -> Unit,
    sidebarExpanded: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(55.dp)
            .background(Color(0xFF0F172A))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val menuItems = listOf(
            Triple("explorer", Icons.Default.Folder, "File tree explorer"),
            Triple("git", Icons.Default.History, "Version history index"),
            Triple("packages", Icons.Default.Inventory, "Development packages manager"),
            Triple("projects", Icons.Default.GridView, "Manage Sandbox entries")
        )

        menuItems.forEach { (panelKey, icon, label) ->
            val isSelected = selectedPanel == panelKey && sidebarExpanded
            IconButton(
                onClick = { onPanelSelected(panelKey) },
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun GitHistoryTab(
    commits: List<CommitEntity>,
    onCommitClick: (CommitEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
        Text("Offline Version Logs", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        if (commits.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No commits yet.\nType git commit or use TopBar tool.", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(commits) { commit ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onCommitClick(commit) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("#${commit.hash}", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text("main", color = Color(0xFF60A5FA), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(commit.message, color = Color.White, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Snapshot timestamp: ${commit.timestamp}", color = Color.Gray, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LocalPackagesTab(
    viewModel: WorkspaceViewModel,
    packages: Map<String, String>
) {
    var packageQuery by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
        Text("Pip & Npm Packages", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = packageQuery,
            onValueChange = { packageQuery = it },
            placeholder = { Text("Search pip/npm modules...", fontSize = 11.sp) },
            textStyle = TextStyle(fontSize = 12.sp, color = Color.White),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (packageQuery.isNotEmpty()) {
            Button(
                onClick = {
                    viewModel.runCommandLine("pip install $packageQuery")
                    packageQuery = ""
                },
                modifier = Modifier.fillMaxWidth().height(35.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Simulate Download package", fontSize = 11.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Local Active Installations:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
        Spacer(modifier = Modifier.height(6.dp))

        if (packages.isEmpty()) {
            Text("(No third party modules active)", color = Color.Gray, fontSize = 11.sp)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(packages.keys.toList()) { key ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(key, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text("3.2.1 (compiled)", color = Color(0xFF4ADE80), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectsManagerTab(
    allProjects: List<ProjectEntity>,
    activeProj: ProjectEntity?,
    onSelect: (ProjectEntity) -> Unit,
    onDelete: (ProjectEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
        Text("Sandbox Manager", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(allProjects) { proj ->
                val isActive = proj.id == activeProj?.id
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(proj) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) Color(0xFF0F172A) else Color(0x3D0F172A)
                    ),
                    border = if (isActive) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(proj.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                            IconButton(onClick = { onDelete(proj) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete project", tint = Color.Red, modifier = Modifier.size(14.dp))
                            }
                        }
                        Text("Language: ${proj.language}", fontSize = 10.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}
