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
        val document =
            editor.document

        if (document.textLength == 0) {
            return emptyList()
        }

        val diagnostics =
            mutableListOf<Diagnostic>()

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
                    ?.takeIf {
                        it.isNotEmpty()
                    }

            if (message != null) {
                val start =
                    highlight.startOffset
                        .coerceIn(
                            0,
                            document.textLength
                        )

                val end =
                    highlight.endOffset
                        .coerceIn(
                            start,
                            document.textLength
                        )

                diagnostics += Diagnostic(
                    startOffset = start,
                    endOffset = end,
                    message = message,
                    severity = highlight.severity
                )
            }

            true
        }

        return diagnostics
            .distinctBy {
                Triple(
                    it.startOffset,
                    it.endOffset,
                    it.message
                )
            }
    }
}