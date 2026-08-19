package dev.rustdiagnostics

import com.intellij.lang.annotation.HighlightSeverity

data class Diagnostic(
    val startOffset: Int,
    val endOffset: Int,
    val message: String,
    val severity: HighlightSeverity
)