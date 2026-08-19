package dev.rustdiagnostics

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

object DiagnosticCollector {

    fun collect(
        project: Project,
        editor: Editor
    ): List<Diagnostic> {
        val document = editor.document

        if (document.textLength == 0) {
            return emptyList()
        }

        val diagnostics = mutableListOf<Diagnostic>()

        DaemonCodeAnalyzerEx.processHighlights(
            document,
            project,
            HighlightSeverity.WEAK_WARNING,
            0,
            document.textLength
        ) { highlight ->

            val message =
                highlight.description
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }

            if (message != null) {
                val start =
                    highlight.startOffset.coerceIn(
                        0,
                        document.textLength
                    )

                val end =
                    highlight.endOffset.coerceIn(
                        start,
                        document.textLength
                    )

                val related =
                    extractRelatedDiagnostics(
                        editor = editor,
                        startOffset = start,
                        endOffset = end,
                        message = message
                    )

                diagnostics += Diagnostic(
                    startOffset = start,
                    endOffset = end,
                    message = message,
                    severity = highlight.severity,
                    related = related
                )
            }

            true
        }

        return diagnostics.distinctBy {
            Triple(
                it.startOffset,
                it.endOffset,
                it.message
            )
        }
    }

    private fun extractRelatedDiagnostics(
        editor: Editor,
        startOffset: Int,
        endOffset: Int,
        message: String
    ): List<RelatedDiagnostic> {
        val document = editor.document

        if (document.textLength == 0) {
            return emptyList()
        }

        val related =
            mutableListOf<RelatedDiagnostic>()

        val lower = message.lowercase()

        if (
            "use of moved value" in lower ||
            "value used after being moved" in lower
        ) {
            findPreviousMatchingIdentifier(
                editor = editor,
                diagnosticStartOffset = startOffset
            )?.let { match ->

                related += RelatedDiagnostic(
                    startOffset = match.first,
                    endOffset = match.second,
                    message = "value moved here"
                )
            }
        }

        return related
    }

    private fun findPreviousMatchingIdentifier(
        editor: Editor,
        diagnosticStartOffset: Int
    ): Pair<Int, Int>? {
        val document = editor.document

        if (diagnosticStartOffset <= 0) {
            return null
        }

        val text = document.charsSequence

        val identifier =
            readIdentifierAt(
                text = text,
                offset = diagnosticStartOffset
            ) ?: return null

        val searchText =
            text.subSequence(
                0,
                diagnosticStartOffset
            ).toString()

        val index =
            searchText.lastIndexOf(identifier)

        if (index < 0) {
            return null
        }

        return index to (index + identifier.length)
    }

    private fun readIdentifierAt(
        text: CharSequence,
        offset: Int
    ): String? {
        if (text.isEmpty()) {
            return null
        }

        val safeOffset =
            offset.coerceIn(
                0,
                text.length - 1
            )

        var start = safeOffset
        var end = safeOffset

        while (
            start > 0 &&
            isIdentifierChar(text[start - 1])
        ) {
            start--
        }

        while (
            end < text.length &&
            isIdentifierChar(text[end])
        ) {
            end++
        }

        if (start >= end) {
            return null
        }

        return text.subSequence(
            start,
            end
        ).toString()
    }

    private fun isIdentifierChar(
        char: Char
    ): Boolean {
        return char.isLetterOrDigit() ||
                char == '_'
    }
}