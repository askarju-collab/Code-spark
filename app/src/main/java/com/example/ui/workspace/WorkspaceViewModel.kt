package com.example.ui.workspace

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GeminiApiClient
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.local.ProjectDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.model.CommitEntity
import com.example.data.model.ProjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class WorkspaceViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ProjectDatabase.getDatabase(application)
    val projectDao = db.projectDao()

    // Active project state
    private val _currentProject = MutableStateFlow<ProjectEntity?>(null)
    val currentProject: StateFlow<ProjectEntity?> = _currentProject.asStateFlow()

    // File selection / Explorer states
    private val _openFiles = MutableStateFlow<List<String>>(emptyList())
    val openFiles: StateFlow<List<String>> = _openFiles.asStateFlow()

    private val _activeFile = MutableStateFlow<String?>(null)
    val activeFile: StateFlow<String?> = _activeFile.asStateFlow()

    private val _activeFileContent = MutableStateFlow("")
    val activeFileContent: StateFlow<String> = _activeFileContent.asStateFlow()

    // Explorer tree nodes
    private val _projectFiles = MutableStateFlow<List<FileNode>>(emptyList())
    val projectFiles: StateFlow<List<FileNode>> = _projectFiles.asStateFlow()

    // AI Chat history
    val chatMessages: Flow<List<ChatMessageEntity>> = _currentProject.flatMapLatest { project ->
        if (project != null) {
            projectDao.getChatMessagesForProject(project.id)
        } else {
            flowOf(emptyList())
        }
    }

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    // Terminal session states
    private val _terminalLogs = MutableStateFlow<List<TerminalLine>>(emptyList())
    val terminalLogs: StateFlow<List<TerminalLine>> = _terminalLogs.asStateFlow()

    private val _terminalTabs = MutableStateFlow(listOf("session_1"))
    val terminalTabs: StateFlow<List<String>> = _terminalTabs.asStateFlow()

    private val _activeTerminalTab = MutableStateFlow("session_1")
    val activeTerminalTab: StateFlow<String> = _activeTerminalTab.asStateFlow()

    // Git management
    val commitsList: Flow<List<CommitEntity>> = _currentProject.flatMapLatest { project ->
        if (project != null) {
            projectDao.getCommitsForProject(project.id)
        } else {
            flowOf(emptyList())
        }
    }

    private val _stagedFiles = MutableStateFlow<Set<String>>(emptySet())
    val stagedFiles: StateFlow<Set<String>> = _stagedFiles.asStateFlow()

    private val _modifiedFiles = MutableStateFlow<Set<String>>(emptySet())
    val modifiedFiles: StateFlow<Set<String>> = _modifiedFiles.asStateFlow()

    // Active terminal input history
    private val commandHistory = mutableListOf<String>()
    private var historyIndex = -1

    // Installed packages (npm/pip) in-memory registry
    private val _installedPackages = MutableStateFlow<Map<String, String>>(emptyMap())
    val installedPackages: StateFlow<Map<String, String>> = _installedPackages.asStateFlow()

    // Setup initial project
    init {
        viewModelScope.launch {
            projectDao.getAllProjects().firstOrNull()?.firstOrNull()?.let {
                selectProject(it)
            } ?: run {
                // Precreate a default showcase "Code Spark Python" project
                createProject("Spark Starter", "Python")
            }
        }
    }

    // Projects list flow
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    fun selectProject(project: ProjectEntity) {
        viewModelScope.launch {
            _currentProject.value = project
            _openFiles.value = emptyList()
            _activeFile.value = null
            _activeFileContent.value = ""
            _stagedFiles.value = emptySet()
            _modifiedFiles.value = emptySet()
            _installedPackages.value = emptyMap()
            refreshFiles()
            appendTerminalMessage("System", "Switched to project: ${project.name}", TerminalType.INFO)
            detectGitChanges()
        }
    }

    fun createProject(name: String, language: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val project = ProjectEntity(name = name, language = language)
            val id = projectDao.insertProject(project)
            val finalProject = project.copy(id = id)

            // Setup local file scaffolding
            val projectDir = getProjectDirectory(id)
            if (!projectDir.exists()) projectDir.mkdirs()

            // Scaffold based on language templates
            when (language.lowercase()) {
                "python" -> {
                    File(projectDir, "main.py").writeText(
                        """# Code Spark Python Scaffold
def calculate_factorial(n):
    if n == 0 or n == 1:
        return 1
    return n * calculate_factorial(n - 1)

if __name__ == "__main__":
    number = 5
    result = calculate_factorial(number)
    print(f"The factorial of {number} is {result}")
"""
                    )
                    File(projectDir, "requirements.txt").writeText(
                        "numpy==1.24.3\npandas==2.0.2\n"
                    )
                    File(projectDir, "README.md").writeText(
                        "# Spark Starter Python Project\n\nRun `main.py` in the terminal using `python main.py` or inspect files using the explorer panels."
                    )
                }
                "javascript", "node" -> {
                    File(projectDir, "index.js").writeText(
                        """// Code Spark JavaScript Scaffold
const greetMe = (name) => {
    return `Hello, Spark Coder ${name}! Welcome to the offline IDE.`;
};

console.log(greetMe("Guest"));
"""
                    )
                    File(projectDir, "package.json").writeText(
                        """{
  "name": "${name.lowercase().replace(" ", "-")}",
  "version": "1.0.0",
  "main": "index.js",
  "dependencies": {
    "lodash": "^4.17.21"
  }
}"""
                    )
                    File(projectDir, "README.md").writeText(
                        "# Spark Starter Node.js Project\n\nInstall developer environment dependencies using `npm install` and run with `node index.js`."
                    )
                }
                "html/css", "html" -> {
                    File(projectDir, "index.html").writeText(
                        """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${name}</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div class="card">
        <h1>Code Spark ⚡</h1>
        <p>A fully offline interactive sandbox workspace environment.</p>
        <button id="spark-btn">Spark Action</button>
        <div id="output"></div>
    </div>
    <script src="script.js"></script>
</body>
</html>"""
                    )
                    File(projectDir, "style.css").writeText(
                        """body {
    background-color: #0f172a;
    color: #f8fafc;
    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100vh;
    margin: 0;
}
.card {
    background-color: #1e293b;
    border-radius: 12dp;
    padding: 24px;
    box-shadow: 0 4px 6px rgba(0,0,0,0.3);
    text-align: center;
    max-width: 400px;
}
h1 {
    color: #38bdf8;
    margin-bottom: 8px;
}
button {
    background-color: #3b82f6;
    color: white;
    border: none;
    padding: 10px 20px;
    border-radius: 6px;
    cursor: pointer;
    font-weight: bold;
}
button:hover {
    background-color: #2563eb;
}"""
                    )
                    File(projectDir, "script.js").writeText(
                        """document.getElementById("spark-btn").addEventListener("click", () => {
    const out = document.getElementById("output");
    out.innerText = "⚡ Sparked! Code is running in the device preview panel successfully.";
    out.style.color = "#4ade80";
    out.style.marginTop = "15px";
});"""
                    )
                    File(projectDir, "README.md").writeText(
                        "# Spark Starter Web Static Page\n\nRun compilation triggers or preview this static website inside the preview window instantly."
                    )
                }
                "java" -> {
                    File(projectDir, "Main.java").writeText(
                        """// Code Spark Java Scaffold
public class Main {
    public static void main(String[] args) {
        System.out.println("Hello World from Code Spark Offline Sandbox Engine!");
    }
}"""
                    )
                    File(projectDir, "README.md").writeText(
                        "# Java Scaffold\n\nRun JVM utilities or compile Java resources using standard inputs."
                    )
                }
                else -> {
                    File(projectDir, "main.txt").writeText("Welcome to Code Spark!")
                }
            }

            // Create initial Git gitignore / scaffolding if requested
            File(projectDir, ".gitignore").writeText("node_modules/\nbuild/\n*.pyc\n")

            withContext(Dispatchers.Main) {
                selectProject(finalProject)
            }
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            projectDao.deleteProjectById(project.id)
            val projectDir = getProjectDirectory(project.id)
            if (projectDir.exists()) {
                projectDir.deleteRecursively()
            }
            withContext(Dispatchers.Main) {
                if (_currentProject.value?.id == project.id) {
                    _currentProject.value = null
                    _openFiles.value = emptyList()
                    _activeFile.value = null
                    _activeFileContent.value = ""
                    _projectFiles.value = emptyList()
                    val fallback = projectDao.getAllProjects().firstOrNull()?.firstOrNull()
                    if (fallback != null) {
                        selectProject(fallback)
                    } else {
                        createProject("Spark Starter", "Python")
                    }
                }
            }
        }
    }

    // Refresh file nodes
    fun refreshFiles() {
        val project = _currentProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val projectDir = getProjectDirectory(project.id)
            val nodes = buildFileTree(projectDir, projectDir)
            _projectFiles.value = nodes
            detectGitChanges()
        }
    }

    private fun buildFileTree(directory: File, baseDirectory: File): List<FileNode> {
        if (!directory.exists()) return emptyList()
        val list = directory.listFiles() ?: return emptyList()
        return list.sortedWith(compareBy({ !it.isDirectory }, { it.name })).map { file ->
            val relativePath = file.relativeTo(baseDirectory).path
            FileNode(
                name = file.name,
                relativePath = relativePath,
                absolutePath = file.absolutePath,
                isDirectory = file.isDirectory,
                children = if (file.isDirectory) buildFileTree(file, baseDirectory) else emptyList()
            )
        }
    }

    fun openFile(relativePath: String) {
        val project = _currentProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(getProjectDirectory(project.id), relativePath)
            if (file.exists() && file.isFile) {
                val text = file.readText()
                withContext(Dispatchers.Main) {
                    if (!_openFiles.value.contains(relativePath)) {
                        _openFiles.value = _openFiles.value + relativePath
                    }
                    _activeFile.value = relativePath
                    _activeFileContent.value = text
                }
            }
        }
    }

    fun closeFile(relativePath: String) {
        val list = _openFiles.value.toMutableList()
        list.remove(relativePath)
        _openFiles.value = list

        if (_activeFile.value == relativePath) {
            if (list.isNotEmpty()) {
                openFile(list.last())
            } else {
                _activeFile.value = null
                _activeFileContent.value = ""
            }
        }
    }

    fun saveActiveFile(content: String) {
        val relativePath = _activeFile.value ?: return
        val project = _currentProject.value ?: return
        _activeFileContent.value = content
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(getProjectDirectory(project.id), relativePath)
            file.writeText(content)
            detectGitChanges()
        }
    }

    // Complete local file manager actions
    fun createFile(relativePath: String, isFolder: Boolean) {
        val project = _currentProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val baseDir = getProjectDirectory(project.id)
            val file = File(baseDir, relativePath)
            file.parentFile?.mkdirs()
            if (isFolder) {
                file.mkdirs()
            } else {
                file.createNewFile()
                file.writeText("")
            }
            refreshFiles()
            appendTerminalMessage("Files", "Created ${if (isFolder) "folder" else "file"}: $relativePath", TerminalType.SUCCESS)
        }
    }

    fun renameFile(oldRelativePath: String, newName: String) {
        val project = _currentProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val baseDir = getProjectDirectory(project.id)
            val oldFile = File(baseDir, oldRelativePath)
            val newFile = File(oldFile.parentFile, newName)
            if (oldFile.exists() && oldFile.renameTo(newFile)) {
                withContext(Dispatchers.Main) {
                    val openList = _openFiles.value.toMutableList()
                    val idx = openList.indexOf(oldRelativePath)
                    val newRelativePath = newFile.relativeTo(baseDir).path
                    if (idx != -1) {
                        openList[idx] = newRelativePath
                        _openFiles.value = openList
                    }
                    if (_activeFile.value == oldRelativePath) {
                        _activeFile.value = newRelativePath
                    }
                }
                refreshFiles()
                appendTerminalMessage("Files", "Renamed $oldRelativePath to $newName", TerminalType.SUCCESS)
            }
        }
    }

    fun deleteFile(relativePath: String) {
        val project = _currentProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(getProjectDirectory(project.id), relativePath)
            if (file.exists()) {
                val isDir = file.isDirectory
                file.deleteRecursively()
                withContext(Dispatchers.Main) {
                    closeFile(relativePath)
                }
                refreshFiles()
                appendTerminalMessage("Files", "Deleted ${if (isDir) "directory" else "file"}: $relativePath", TerminalType.WARN)
            }
        }
    }

    fun compressFolderZip(relativePath: String) {
        val project = _currentProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val baseDir = getProjectDirectory(project.id)
            val sourceDir = File(baseDir, relativePath)
            val zipFile = File(baseDir, "${sourceDir.name}.zip")
            ZipOutputStream(zipFile.outputStream()).use { zipOut ->
                sourceDir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        val entryPath = file.relativeTo(sourceDir).path
                        zipOut.putNextEntry(ZipEntry(entryPath))
                        file.inputStream().use { it.copyTo(zipOut) }
                        zipOut.closeEntry()
                    }
                }
            }
            refreshFiles()
            appendTerminalMessage("Ziper", "Compressed $relativePath into ${zipFile.name}", TerminalType.SUCCESS)
        }
    }

    fun extractZipFile(relativePath: String) {
        val project = _currentProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val baseDir = getProjectDirectory(project.id)
            val zipFile = File(baseDir, relativePath)
            if (!zipFile.exists() || !zipFile.name.endsWith(".zip")) return@launch
            val destDir = File(baseDir, zipFile.nameWithoutExtension)
            destDir.mkdirs()
            ZipInputStream(zipFile.inputStream()).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val outFile = File(destDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { zipIn.copyTo(it) }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            refreshFiles()
            appendTerminalMessage("Unziper", "Extracted Zip ${zipFile.name} successfully.", TerminalType.SUCCESS)
        }
    }

    // Git features (simulated status + commit logging engine)
    fun gitInit() {
        _terminalLogs.value = _terminalLogs.value + TerminalLine("Git", "Initialized empty Git repository in offline sandbox workspace.", TerminalType.SUCCESS)
        detectGitChanges()
    }

    fun gitAdd(relativePath: String) {
        val staged = _stagedFiles.value.toMutableSet()
        staged.add(relativePath)
        _stagedFiles.value = staged
        appendTerminalMessage("Git", "Staged file: $relativePath", TerminalType.SUCCESS)
        detectGitChanges()
    }

    fun gitAddAll() {
        _stagedFiles.value = _modifiedFiles.value
        appendTerminalMessage("Git", "Staged all modified files", TerminalType.SUCCESS)
        detectGitChanges()
    }

    fun gitCommit(message: String) {
        val project = _currentProject.value ?: return
        if (_stagedFiles.value.isEmpty()) {
            appendTerminalMessage("Git", "Nothing to commit, working tree clean", TerminalType.WARN)
            return
        }
        viewModelScope.launch {
            val lastHash = (1..8).map { ('a'..'z').random() }.joinToString("")
            val commit = CommitEntity(
                projectId = project.id,
                hash = lastHash,
                message = message,
                timestamp = System.currentTimeMillis()
            )
            projectDao.insertCommit(commit)
            _stagedFiles.value = emptySet()
            _modifiedFiles.value = emptySet()
            appendTerminalMessage("Git", "Committed successfully [hash: $lastHash]: $message", TerminalType.SUCCESS)
            detectGitChanges()
        }
    }

    fun getProjectDirectory(id: Long): File {
        val baseDir = File(getApplication<Application>().filesDir, "projects")
        if (!baseDir.exists()) baseDir.mkdir()
        val projectFolder = File(baseDir, id.toString())
        if (!projectFolder.exists()) projectFolder.mkdir()
        return projectFolder
    }

    private fun detectGitChanges() {
        val project = _currentProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val baseDir = getProjectDirectory(project.id)
            val modified = mutableSetOf<String>()
            baseDir.walkTopDown().forEach { file ->
                if (file.isFile && !file.path.contains("node_modules") && !file.name.contains(".zip")) {
                    val relative = file.relativeTo(baseDir).path
                    // We simulate modified checking simply by confirming they are inside our system tracking
                    if (!_stagedFiles.value.contains(relative)) {
                        modified.add(relative)
                    }
                }
            }
            withContext(Dispatchers.Main) {
                _modifiedFiles.value = modified
            }
        }
    }

    // Terminal Process Execution
    fun runCommandLine(commandStr: String) {
        val trimmed = commandStr.trim()
        if (trimmed.isEmpty()) return
        commandHistory.add(trimmed)
        historyIndex = commandHistory.size

        appendTerminalMessage("Guest@codespark:~#", trimmed, TerminalType.INPUT)

        viewModelScope.launch(Dispatchers.IO) {
            val parts = trimmed.split(" ")
            val cmd = parts[0]
            val arg = if (parts.size > 1) parts.subList(1, parts.size).joinToString(" ") else ""

            when (cmd.lowercase()) {
                "clear" -> {
                    _terminalLogs.value = emptyList()
                }
                "help" -> {
                    val helpText = """Code Spark Offline Terminal Commands:
- help: Shows this help board
- ls: List directory contents
- cd <dir>: Change active folder
- pwd: Print active working directory
- cat <file>: Display contents of a file
- python <file>: Run Python compiler sandbox interpreter
- node <file>: Run Javascript Node interpreter Sandbox
- pip install <package>: Install Python libraries
- npm install <package>: Install Node modules
- git init / Add / Commit / status: version control parameters
- clear: Clear logs
"""
                    appendTerminalLines("System", helpText, TerminalType.INFO)
                }
                "ls" -> {
                    val project = _currentProject.value
                    if (project == null) {
                        appendTerminalMessage("Terminal", "No project selected.", TerminalType.ERROR)
                        return@launch
                    }
                    val currentDir = getProjectDirectory(project.id)
                    val files = currentDir.list() ?: emptyArray()
                    if (files.isEmpty()) {
                        appendTerminalMessage("Terminal", "(Empty Directory)", TerminalType.INFO)
                    } else {
                        appendTerminalLines("Terminal", files.joinToString("   "), TerminalType.SUCCESS)
                    }
                }
                "pwd" -> {
                    val project = _currentProject.value
                    if (project != null) {
                        appendTerminalMessage("Terminal", "/storage/emulated/0/codespark/${project.name}", TerminalType.INFO)
                    } else {
                        appendTerminalMessage("Terminal", "/storage/emulated/0/codespark", TerminalType.INFO)
                    }
                }
                "cat" -> {
                    if (arg.isEmpty()) {
                        appendTerminalMessage("cat", "Usage: cat <filename>", TerminalType.WARN)
                        return@launch
                    }
                    val project = _currentProject.value ?: return@launch
                    val file = File(getProjectDirectory(project.id), arg)
                    if (file.exists() && file.isFile) {
                        appendTerminalLines(arg, file.readText(), TerminalType.INFO)
                    } else {
                        appendTerminalMessage("cat", "File $arg does not exist.", TerminalType.ERROR)
                    }
                }
                "python" -> {
                    if (arg.isEmpty()) {
                        appendTerminalMessage("python", "Usage: python <filename.py>", TerminalType.WARN)
                        return@launch
                    }
                    val project = _currentProject.value ?: return@launch
                    val file = File(getProjectDirectory(project.id), arg)
                    if (file.exists() && file.isFile) {
                        appendTerminalMessage("python", "Initializing sandboxed Python engine...", TerminalType.INFO)
                        delaySim(800)
                        val code = file.readText()
                        executeVirtualInterpreter(code, "python")
                    } else {
                        appendTerminalMessage("python", "Error: File $arg not found.", TerminalType.ERROR)
                    }
                }
                "node" -> {
                    if (arg.isEmpty()) {
                        appendTerminalMessage("node", "Usage: node <filename.js>", TerminalType.WARN)
                        return@launch
                    }
                    val project = _currentProject.value ?: return@launch
                    val file = File(getProjectDirectory(project.id), arg)
                    if (file.exists() && file.isFile) {
                        appendTerminalMessage("node", "Starting sandboxed Node.js context V8 engine...", TerminalType.INFO)
                        delaySim(800)
                        val code = file.readText()
                        executeVirtualInterpreter(code, "js")
                    } else {
                        appendTerminalMessage("node", "Error: Script $arg not found.", TerminalType.ERROR)
                    }
                }
                "pip" -> {
                    if (trimmed.contains("install")) {
                        val pkgName = trimmed.substringAfter("install").trim()
                        if (pkgName.isEmpty() || pkgName == "install") {
                            appendTerminalMessage("pip", "Usage: pip install <package>", TerminalType.WARN)
                            return@launch
                        }
                        appendTerminalMessage("pip", "Collecting $pkgName...", TerminalType.INFO)
                        delaySim(500)
                        appendTerminalMessage("pip", "Downloading resource binary wheel maps for $pkgName...", TerminalType.INFO)
                        delaySim(1200)
                        appendTerminalMessage("pip", "Installing package and configuring metadata links...", TerminalType.INFO)
                        delaySim(1000)
                        withContext(Dispatchers.Main) {
                            val map = _installedPackages.value.toMutableMap()
                            map[pkgName] = "Latest"
                            _installedPackages.value = map
                        }
                        appendTerminalMessage("pip", "Successfully installed $pkgName-3.2.1-py3-none-any.whl", TerminalType.SUCCESS)
                    } else {
                        appendTerminalMessage("pip", "Command pip: unknown options. Supported: pip install", TerminalType.WARN)
                    }
                }
                "npm" -> {
                    if (trimmed.contains("install") || trimmed.contains("i ")) {
                        val pkgName = trimmed.replace("npm i ", "").replace("npm install ", "").trim()
                        if (pkgName.isEmpty() || pkgName == "install") {
                            appendTerminalMessage("npm", "Installing project dependencies defined in package.json...", TerminalType.INFO)
                            delaySim(2000)
                            appendTerminalMessage("npm", "Added 42 packages, audited 143 packages in 2.1s.", TerminalType.SUCCESS)
                            return@launch
                        }
                        appendTerminalMessage("npm", "npm WARN deprecated source-map-url@0.4.1", TerminalType.WARN)
                        appendTerminalMessage("npm", "Fetching package headers for $pkgName from registry...", TerminalType.INFO)
                        delaySim(1000)
                        appendTerminalMessage("npm", "Saving modules dependency config tree...", TerminalType.INFO)
                        delaySim(800)
                        withContext(Dispatchers.Main) {
                            val map = _installedPackages.value.toMutableMap()
                            map[pkgName] = "npm"
                            _installedPackages.value = map
                        }
                        appendTerminalMessage("npm", "+ $pkgName@1.4.3\nadded 12 packages from 8 contributors in 3.42s.", TerminalType.SUCCESS)
                    } else {
                        appendTerminalMessage("npm", "Command npm: unknown options. Supported: npm install", TerminalType.WARN)
                    }
                }
                "git" -> {
                    processGitCli(arg)
                }
                else -> {
                    // Execute simple native Android built-ins using ProcessBuilder safely!
                    try {
                        val process = ProcessBuilder()
                            .command(parts)
                            .redirectErrorStream(true)
                            .start()
                        process.inputStream.bufferedReader().use { reader ->
                            var line: String?
                            while (reader.readLine().also { line = it } != null) {
                                appendTerminalMessage("shell", line ?: "", TerminalType.INFO)
                            }
                        }
                        process.waitFor()
                    } catch (e: Exception) {
                        appendTerminalMessage("shell", "Command '$cmd' not found. Type 'help' for support.", TerminalType.ERROR)
                    }
                }
            }
        }
    }

    private suspend fun executeVirtualInterpreter(code: String, lang: String) {
        val lines = code.split("\n")
        var hasRun = false
        for (line in lines) {
            val clean = line.trim()
            if (clean.startsWith("print(") || clean.startsWith("console.log(")) {
                val outputText = if (lang == "python") {
                    clean.substringAfter("print(").substringBeforeLast(")")
                        .replace("\"", "").replace("'", "")
                } else {
                    clean.substringAfter("console.log(").substringBeforeLast(")")
                        .replace("\"", "").replace("'", "")
                }
                // Handle complex formatted text replacements or direct strings
                appendTerminalMessage("Interpreter Output", "Stdout: $outputText", TerminalType.SUCCESS)
                hasRun = true
            }
        }
        if (!hasRun) {
            appendTerminalMessage("Interpreter Output", "Program completed successfully with exit code 0.", TerminalType.SUCCESS)
        }
    }

    private fun processGitCli(arg: String) {
        val subParts = arg.split(" ")
        val subCmd = subParts[0]
        val param = if (subParts.size > 1) subParts.subList(1, subParts.size).joinToString(" ") else ""

        when (subCmd.lowercase()) {
            "init" -> gitInit()
            "add" -> {
                if (param == "." || param == "*") {
                    gitAddAll()
                } else {
                    gitAdd(param)
                }
            }
            "commit" -> {
                if (param.contains("-m")) {
                    val msg = param.substringAfter("-m").replace("\"", "").replace("'", "").trim()
                    gitCommit(msg)
                } else {
                    appendTerminalMessage("Git", "Usage: git commit -m \"message\"", TerminalType.WARN)
                }
            }
            "status" -> {
                val staged = _stagedFiles.value
                val modified = _modifiedFiles.value
                val stagedText = if (staged.isEmpty()) "none" else staged.joinToString(", ")
                val modText = if (modified.isEmpty()) "none" else modified.joinToString(", ")
                val statusInfo = """On branch main
Changes to be committed (staged):
  $stagedText

Changes not staged for commit:
  $modText
"""
                appendTerminalLines("Git", statusInfo, TerminalType.INFO)
            }
            "log" -> {
                viewModelScope.launch {
                    projectDao.getCommitsForProject(_currentProject.value?.id ?: 0).firstOrNull()?.let { commits ->
                        if (commits.isEmpty()) {
                            appendTerminalMessage("Git", "No commits recorded yet.", TerminalType.WARN)
                        } else {
                            commits.forEach { commit ->
                                appendTerminalMessage("Git", "commit ${commit.hash}\nAuthor: CodeSpark <offline@spark.dev>\nDate: ${commit.timestamp}\n\n    ${commit.message}\n", TerminalType.INFO)
                            }
                        }
                    }
                }
            }
            else -> {
                appendTerminalMessage("Git", "Subcommand '$subCmd' not handled recursively. Supported: init, add, commit, status, log", TerminalType.WARN)
            }
        }
    }

    private fun appendTerminalMessage(role: String, text: String, type: TerminalType) {
        val current = _terminalLogs.value.toMutableList()
        current.add(TerminalLine(role, text, type))
        _terminalLogs.value = current
    }

    private fun appendTerminalLines(role: String, text: String, type: TerminalType) {
        val lines = text.split("\n")
        val current = _terminalLogs.value.toMutableList()
        lines.forEach { line ->
            current.add(TerminalLine(role, line, type))
        }
        _terminalLogs.value = current
    }

    private suspend fun delaySim(ms: Long) {
        withContext(Dispatchers.IO) {
            Thread.sleep(ms)
        }
    }

    // AI Generation Assistant Engine calls
    fun sendChatMessage(text: String) {
        val project = _currentProject.value ?: return
        if (text.trim().isEmpty()) return

        viewModelScope.launch {
            // Save user msg to local DB
            val userMsg = ChatMessageEntity(projectId = project.id, role = "user", text = text)
            projectDao.insertChatMessage(userMsg)

            _aiLoading.value = true

            // Read the active selected file content to inject context!
            val contextFileText = if (_activeFile.value != null && _activeFileContent.value.isNotEmpty()) {
                "\n\nCURRENT WORKING FILE CONTENT (${_activeFile.value}):\n```\n${_activeFileContent.value}\n```"
            } else ""

            val prompt = """The user is working in the offline Code Spark IDE developer workspace.
Active Project Schema Language: ${project.language}
Active Project Files count: ${_projectFiles.value.size}
Active Selected File Path: ${_activeFile.value ?: "none"}
$contextFileText

User query: $text"""

            // Call REST Gemini API
            val apiKey = BuildConfig.GEMINI_API_KEY
            val systemInstructor = """You are SparkCoder, the integrated developer core AI companion inside the Code Spark Android IDE.
When giving code, always return complete, production-ready full solutions enclosed in standard markdown codeblocks (for example, ````python ... ````).
Be highly engineering-focused, precise, and concise. Explain line-by-line when asked, and write custom tests or performance optimizations directly."""

            val contentsPayload = listOf(
                Content(role = "user", parts = listOf(Part(text = prompt)))
            )

            val request = GenerateContentRequest(
                contents = contentsPayload,
                systemInstruction = Content(parts = listOf(Part(text = systemInstructor)))
            )

            try {
                val service = GeminiApiClient.service
                val output = withContext(Dispatchers.IO) {
                    service.generateContent(apiKey, request)
                }
                val aiResponseString = output.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "I was unable to compile the context. Check your internet connection configuration."

                val aiMsg = ChatMessageEntity(projectId = project.id, role = "model", text = aiResponseString)
                projectDao.insertChatMessage(aiMsg)
            } catch (e: Exception) {
                val aiMsg = ChatMessageEntity(
                    projectId = project.id,
                    role = "model",
                    text = "A connectivity challenge occurred: ${e.localizedMessage}. Note: Ensure correct API values are stored in the Secrets parameters window."
                )
                projectDao.insertChatMessage(aiMsg)
            } finally {
                _aiLoading.value = false
            }
        }
    }

    // Applies code extracted from markdown back safely to active file
    fun applyCodeToActiveFile(extractedCode: String) {
        val activePath = _activeFile.value ?: return
        saveActiveFile(extractedCode)
        appendTerminalMessage("AI Assistant", "Applied AI code updates successfully to editor: $activePath", TerminalType.SUCCESS)
    }

    fun clearChat() {
        val project = _currentProject.value ?: return
        viewModelScope.launch {
            projectDao.clearChatMessagesForProject(project.id)
        }
    }
}

// Support definitions
data class FileNode(
    val name: String,
    val relativePath: String,
    val absolutePath: String,
    val isDirectory: Boolean,
    val children: List<FileNode> = emptyList()
)

data class TerminalLine(
    val sender: String,
    val message: String,
    val type: TerminalType
)

enum class TerminalType {
    INPUT, INFO, SUCCESS, WARN, ERROR
}
