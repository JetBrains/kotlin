/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.UnsupportedKotlinArchiveUsage
import java.io.File

/**
 * Reports unexpected usages of '.kar' or '.kar.xz' files and removes them from the collection of libraries.
 * This function is useful for Kotlin versions which do not support .kar files yet to provide an actionable diagnostic.
 */

internal fun File.isKarOrKarXZFile() = name.endsWith(".kar") || name.endsWith(".kar.xz")

internal fun Iterable<File>.filterAndReportUnsupportedKotlinArchiveLibraries(diagnostics: UsesKotlinToolingDiagnostics): Collection<File> {
    reportUnsupportedKotlinArchiveLibraries(diagnostics)
    return filter { !it.isKarOrKarXZFile() }
}

internal fun Iterable<File>.reportUnsupportedKotlinArchiveLibraries(diagnostics: UsesKotlinToolingDiagnostics) {
    val karFiles = filter { it.isKarOrKarXZFile() }
    if (karFiles.isNotEmpty()) {
        diagnostics.reportDiagnostic(UnsupportedKotlinArchiveUsage(karFiles), reportOnce = true)
    }
}
