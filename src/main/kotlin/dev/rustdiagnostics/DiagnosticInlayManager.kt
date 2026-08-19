package dev.rustdiagnostics

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.project.Project
import java.util.WeakHashMap

class DiagnosticInlayManager(
    private val project: Project
) {

    private val log =
        Logger.getInstance(DiagnosticInlayManager::class.java)

    private val inlays =
        WeakHashMap<Editor, MutableList<Inlay<*>>>()

    fun refresh(editor: Editor) {
        if (project.isDisposed || editor.isDisposed) {
            return
        }

        if (!isRustEditor(editor)) {
            clear(editor)
            return
        }

        if (!ApplicationManager
                .getApplication()
                .isDispatchThread
        ) {
            ApplicationManager
                .getApplication()
                .invokeLater {
                    refresh(editor)
                }

            return
        }

        clear(editor)

        val diagnostics =
            try {
                DiagnosticCollector.collect(
                    project,
                    editor
                )
            } catch (e: Exception) {
                log.warn(
                    "Failed to collect Rust diagnostics",
                    e
                )
                return
            }

        if (diagnostics.isEmpty()) {
            return
        }

        val document = editor.document

        val grouped =
            diagnostics.groupBy {
                val offset =
                    it.startOffset.coerceIn(
                        0,
                        document.textLength
                    )

                document.getLineNumber(offset)
            }

        val created =
            mutableListOf<Inlay<*>>()

        for ((_, diagnosticsOnLine) in grouped) {
            val ordered =
                diagnosticsOnLine
                    .sortedByDescending {
                        severityPriority(it)
                    }
                    .take(4)

            for (diagnostic in ordered) {
                val diagnosticOffset =
                    diagnostic.startOffset.coerceIn(
                        0,
                        document.textLength
                    )

                val line =
                    document.getLineNumber(
                        diagnosticOffset
                    )

                val offset =
                    document.getLineEndOffset(line)

                val renderer =
                    DiagnosticRenderer(
                        editor,
                        diagnostic
                    )

                val inlay =
                    editor.inlayModel.addBlockElement(
                        offset,
                        true,
                        false,
                        0,
                        renderer
                    )

                if (inlay != null) {
                    created += inlay
                }
            }
        }

        if (created.isNotEmpty()) {
            inlays[editor] = created
        }
    }

    private fun severityPriority(
        diagnostic: Diagnostic
    ): Int {
        return when (diagnostic.severity) {
            HighlightSeverity.ERROR -> 400
            HighlightSeverity.WARNING -> 300
            HighlightSeverity.WEAK_WARNING -> 200
            HighlightSeverity.INFORMATION -> 100
            else -> 0
        }
    }

    fun clear(editor: Editor) {
        val existing =
            inlays.remove(editor)
                ?: return

        existing.forEach {
            if (it.isValid) {
                it.dispose()
            }
        }
    }

    fun clearAll() {
        inlays.keys
            .toList()
            .forEach(::clear)
    }

    private fun isRustEditor(
        editor: Editor
    ): Boolean {
        return editor.virtualFile
            ?.extension
            ?.equals(
                "rs",
                ignoreCase = true
            ) == true
    }
}