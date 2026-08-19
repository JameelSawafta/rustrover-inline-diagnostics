package dev.rustdiagnostics

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Rectangle

class DiagnosticRenderer(
    private val editor: Editor,
    private val diagnostic: Diagnostic
) : EditorCustomElementRenderer {

    private val prefix: String
        get() = when (diagnostic.severity) {
            HighlightSeverity.ERROR ->
                "└─ error: "

            HighlightSeverity.WARNING,
            HighlightSeverity.WEAK_WARNING ->
                "└─ warning: "

            else ->
                "└─ "
        }

    private val renderedText: String
        get() =
            prefix + shorten(diagnostic.message)

    override fun calcWidthInPixels(
        inlay: Inlay<*>
    ): Int {
        val scheme =
            editor.colorsScheme

        val font = Font(
            scheme.editorFontName,
            Font.PLAIN,
            scheme.editorFontSize
        )

        val metrics =
            editor.contentComponent
                .getFontMetrics(font)

        return diagnosticX() +
                metrics.stringWidth(renderedText) +
                20
    }

    override fun calcHeightInPixels(
        inlay: Inlay<*>
    ): Int {
        return editor.lineHeight
    }

    override fun paint(
        inlay: Inlay<*>,
        g: Graphics,
        targetRegion: Rectangle,
        textAttributes: TextAttributes
    ) {
        val scheme =
            editor.colorsScheme

        val font = Font(
            scheme.editorFontName,
            Font.PLAIN,
            scheme.editorFontSize
        )

        g.font = font

        g.color =
            when (diagnostic.severity) {
                HighlightSeverity.ERROR ->
                    JBColor(
                        Color(190, 50, 50),
                        Color(255, 100, 100)
                    )

                HighlightSeverity.WARNING ->
                    JBColor(
                        Color(180, 120, 20),
                        Color(255, 190, 70)
                    )

                HighlightSeverity.WEAK_WARNING ->
                    JBColor(
                        Color(160, 120, 30),
                        Color(220, 180, 80)
                    )

                HighlightSeverity.INFORMATION ->
                    JBColor(
                        Color(50, 100, 180),
                        Color(100, 160, 230)
                    )

                else ->
                    scheme.defaultForeground
            }

        val metrics =
            g.fontMetrics

        val x =
            targetRegion.x +
                    diagnosticX()

        val y =
            targetRegion.y +
                    (
                            targetRegion.height -
                                    metrics.height
                            ) / 2 +
                    metrics.ascent

        g.drawString(
            renderedText,
            x,
            y
        )
    }

    private fun diagnosticX(): Int {
        val document =
            editor.document

        if (document.textLength == 0) {
            return 0
        }

        val offset =
            diagnostic.startOffset.coerceIn(
                0,
                document.textLength
            )

        val line =
            document.getLineNumber(offset)

        val lineStart =
            document.getLineStartOffset(line)

        return try {
            val linePoint =
                editor.offsetToXY(lineStart)

            val diagnosticPoint =
                editor.offsetToXY(offset)

            (
                    diagnosticPoint.x -
                            linePoint.x
                    )
                .coerceAtLeast(0)
                .coerceAtMost(500)
        } catch (_: Exception) {
            0
        }
    }

    private fun shorten(
        message: String
    ): String {
        val normalized =
            message
                .replace('\n', ' ')
                .replace(
                    Regex("\\s+"),
                    " "
                )
                .trim()

        val maxLength = 160

        return if (
            normalized.length <= maxLength
        ) {
            normalized
        } else {
            normalized.take(
                maxLength - 1
            ) + "…"
        }
    }
}