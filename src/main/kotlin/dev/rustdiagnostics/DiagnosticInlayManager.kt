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
        Logger.getInstance(
            DiagnosticInlayManager::class.java
        )

    private val inlays =
        WeakHashMap<
                Editor,
                MutableList<Inlay<*>>
                >()

    fun refresh(
        editor: Editor
    ) {
        if (
            project.isDisposed ||
            editor.isDisposed
        ) {
            return
        }

        if (!isRustEditor(editor)) {
            clear(editor)
            return
        }

        if (
            !ApplicationManager
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

        val document =
            editor.document

        val displayItems =
            mutableListOf<DisplayDiagnostic>()

        for (diagnostic in diagnostics) {

            displayItems +=
                DisplayDiagnostic(
                    startOffset =
                        diagnostic.startOffset,
                    endOffset =
                        diagnostic.endOffset,
                    message =
                        diagnostic.message,
                    severity =
                        diagnostic.severity,
                    type =
                        DiagnosticType.PRIMARY
                )

            for (
            related in diagnostic.related
            ) {
                displayItems +=
                    DisplayDiagnostic(
                        startOffset =
                            related.startOffset,
                        endOffset =
                            related.endOffset,
                        message =
                            related.message,
                        severity =
                            HighlightSeverity.INFORMATION,
                        type =
                            DiagnosticType.RELATED
                    )
            }
        }

        val grouped =
            displayItems.groupBy {
                val offset =
                    it.startOffset.coerceIn(
                        0,
                        document.textLength
                    )

                document.getLineNumber(
                    offset
                )
            }

        val created =
            mutableListOf<Inlay<*>>()

        for (
        (_, diagnosticsOnLine) in grouped
        ) {
            val ordered =
                diagnosticsOnLine
                    .distinctBy {
                        Triple(
                            it.startOffset,
                            it.endOffset,
                            it.message
                        )
                    }
                    .sortedWith(
                        compareByDescending<DisplayDiagnostic> {
                            diagnosticPriority(it)
                        }.thenBy {
                            it.startOffset
                        }
                    )
                    .take(6)

            for (
            diagnostic in ordered
            ) {
                val diagnosticOffset =
                    diagnostic.startOffset
                        .coerceIn(
                            0,
                            document.textLength
                        )

                val line =
                    document.getLineNumber(
                        diagnosticOffset
                    )

                val offset =
                    document.getLineEndOffset(
                        line
                    )

                val renderer =
                    DiagnosticRenderer(
                        editor = editor,
                        diagnostic = diagnostic
                    )

                val inlay =
                    editor.inlayModel
                        .addBlockElement(
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

    private fun diagnosticPriority(
        diagnostic: DisplayDiagnostic
    ): Int {
        if (
            diagnostic.type ==
            DiagnosticType.RELATED
        ) {
            return 50
        }

        return when (
            diagnostic.severity
        ) {
            HighlightSeverity.ERROR ->
                400

            HighlightSeverity.WARNING ->
                300

            HighlightSeverity.WEAK_WARNING ->
                200

            HighlightSeverity.INFORMATION ->
                100

            else ->
                0
        }
    }

    fun clear(
        editor: Editor
    ) {
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

data class DisplayDiagnostic(
    val startOffset: Int,
    val endOffset: Int,
    val message: String,
    val severity: HighlightSeverity,
    val type: DiagnosticType
)

enum class DiagnosticType {
    PRIMARY,
    RELATED
}