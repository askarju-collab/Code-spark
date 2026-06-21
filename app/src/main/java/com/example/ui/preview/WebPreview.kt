package com.example.ui.preview

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.workspace.WorkspaceViewModel
import java.io.File

@Composable
fun WebPreview(
    viewModel: WorkspaceViewModel,
    modifier: Modifier = Modifier
) {
    val currentProject by viewModel.currentProject.collectAsState()
    var htmlContent by remember { mutableStateOf("") }
    var cssContent by remember { mutableStateOf("") }
    var jsContent by remember { mutableStateOf("") }
    var previewToken by remember { mutableStateOf(0) }

    fun reloadPreview() {
        val proj = currentProject ?: return
        val baseDir = viewModel.getProjectDirectory(proj.id)
        val htmlFile = File(baseDir, "index.html")
        val cssFile = File(baseDir, "style.css")
        val jsFile = File(baseDir, "script.js")

        htmlContent = if (htmlFile.exists()) htmlFile.readText() else "<h1>No index.html file found!</h1><p>Create index.html to preview static content.</p>"
        cssContent = if (cssFile.exists()) cssFile.readText() else ""
        jsContent = if (jsFile.exists()) jsFile.readText() else ""
        previewToken++
    }

    // Trigger initial load
    LaunchedEffect(currentProject) {
        reloadPreview()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(6.dp)
    ) {
        // Preview Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sandbox Live WebView",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row {
                IconButton(
                    onClick = { reloadPreview() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh preview",
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Divider(color = Color(0xFF1E293B), thickness = 1.dp)

        if (htmlContent.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else {
            // Render actual web rendering in local WebView container!
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = WebViewClient()
                    }
                },
                update = { webView ->
                    // Inject CSS and JS scripts seamlessly inside HTML head/body!
                    val compositeHtml = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="utf-8">
                            <style>
                                $cssContent
                            </style>
                        </head>
                        <body>
                            $htmlContent
                            <script>
                                $jsContent
                            </script>
                        </body>
                        </html>
                    """.trimIndent()
                    webView.loadDataWithBaseURL("file:///android_asset/", compositeHtml, "text/html", "UTF-8", null)
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .background(Color.White)
            )
        }
    }
}
