package com.example.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.util.regex.Pattern

class CodeHighlighter(private val fileExtension: String) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = highlight(text.text, fileExtension)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }

    private fun highlight(code: String, fileExtension: String): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        
        // Default style
        builder.addStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = Color(0xFFF8FAFC)), 0, code.length)

        try {
            val extension = fileExtension.lowercase()
            val patterns = getPatternsForExtension(extension)

            for ((pattern, style) in patterns) {
                val matcher = pattern.matcher(code)
                while (matcher.find()) {
                    val start = matcher.start()
                    val end = matcher.end()
                    builder.addStyle(style, start, end)
                }
            }
        } catch (e: Exception) {
            // Fallback gracefully
        }

        return builder.toAnnotatedString()
    }

    private fun getPatternsForExtension(extension: String): List<Pair<Pattern, SpanStyle>> {
        val list = mutableListOf<Pair<Pattern, SpanStyle>>()

        // Styling definitions
        val keywordStyle = SpanStyle(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold) // Blue
        val stringStyle = SpanStyle(color = Color(0xFF4ADE80)) // Green
        val commentStyle = SpanStyle(color = Color(0xFF64748B)) // Gray
        val numberStyle = SpanStyle(color = Color(0xFFF59E0B)) // Orange
        val typeStyle = SpanStyle(color = Color(0xFFF472B6), fontWeight = FontWeight.SemiBold) // Pink

        when (extension) {
            "py", "python" -> {
                // Comments
                list.add(Pair(Pattern.compile("#.*"), commentStyle))
                // Keywords
                list.add(Pair(Pattern.compile("\\b(def|class|if|else|elif|import|from|return|for|while|in|as|try|except|pass|print|and|or|not|Lambda|is|None|True|False)\\b"), keywordStyle))
                // Strings
                list.add(Pair(Pattern.compile("\".*?\"|'.*?'"), stringStyle))
                // Numbers
                list.add(Pair(Pattern.compile("\\b(\\d+)\\b"), numberStyle))
            }
            "js", "javascript", "ts", "typescript" -> {
                // Line comments
                list.add(Pair(Pattern.compile("//.*"), commentStyle))
                // Block comments
                list.add(Pair(Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL), commentStyle))
                // Keywords
                list.add(Pair(Pattern.compile("\\b(const|let|var|function|return|if|else|import|export|from|class|extends|new|this|typeof|instanceof|for|while|foreach|in|of|null|undefined|true|false|try|catch|finally)\\b"), keywordStyle))
                // Strings
                list.add(Pair(Pattern.compile("\".*?\"|'.*?'|`.*?`"), stringStyle))
                // Numbers
                list.add(Pair(Pattern.compile("\\b(\\d+)\\b"), numberStyle))
            }
            "html" -> {
                // Tags
                list.add(Pair(Pattern.compile("<[^>]+>"), typeStyle))
                // Comments
                list.add(Pair(Pattern.compile("<!--.*?-->", Pattern.DOTALL), commentStyle))
                // Strings
                list.add(Pair(Pattern.compile("\".*?\"|'.*?'"), stringStyle))
            }
            "css" -> {
                // Selectors/properties
                list.add(Pair(Pattern.compile("[a-zA-Z\\-]+\\s*(?=:)|(?<=\\{)[^:]+(?:;)"), keywordStyle))
                // Values
                list.add(Pair(Pattern.compile("(?<=:)[^;\\}]+"), stringStyle))
                // Comments
                list.add(Pair(Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL), commentStyle))
            }
            "java" -> {
                // Line Comments
                list.add(Pair(Pattern.compile("//.*"), commentStyle))
                // Keywords
                list.add(Pair(Pattern.compile("\\b(public|private|protected|class|interface|enum|extends|implements|new|return|if|else|try|catch|finally|for|while|void|static|final|package|import|null|true|false)\\b"), keywordStyle))
                // Types
                list.add(Pair(Pattern.compile("\\b(int|double|float|long|short|byte|char|boolean|String|List|Map|Set|HashMap|ArrayList)\\b"), typeStyle))
                // Strings
                list.add(Pair(Pattern.compile("\".*?\""), stringStyle))
                // Numbers
                list.add(Pair(Pattern.compile("\\b(\\d+)\\b"), numberStyle))
            }
        }
        return list
    }
}
