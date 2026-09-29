/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.linkage.issues.PartialLinkageErrorsLogged
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageLogger
import org.jetbrains.kotlin.backend.konan.serialization.SerializedPartialLinkageIssue
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.config.PartialLinkageLogLevel
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.library.KotlinLibrary

/**
 * Reports the partial linkage issues that have been recorded in the caches which the current compilation reuses.
 *
 * The partial linkage engine only inspects the code that is compiled from the IR in the current compilation.
 * The code that comes from a cache is linked as is, so the issues detected when that cache was built would be
 * reported just once - in the very compilation that built the cache - unless they are replayed here (KT-78253).
 *
 * @param isRebuiltInThisRun Tells whether the cache of the given file (or of the whole library,
 *   if the file ID is `null`) has been built during current compilation. Such issues have already
 *   been reported by the cache building compilation itself, so they are not repeated.
 */
internal fun replayPartialLinkageIssuesFromCaches(
        config: NativeSecondStageCompilationConfig,
        isRebuiltInThisRun: (KotlinLibrary, String?) -> Boolean,
) {
    // Note: The issues stored in different caches may coincide, e.g. an issue that is not attributed
    // to any particular file is stored in the caches of all the files that were built together (see
    // `CacheStorage.savePartialLinkageIssues`). Report every distinct issue exactly once.
    val issues = LinkedHashSet<SerializedPartialLinkageIssue>()

    for (library in config.loadedKlibs.all) {
        when (val cache = config.cachedLibraries.getLibraryCache(library)) {
            null -> continue
            is CachedLibraries.Cache.Monolithic -> {
                if (!isRebuiltInThisRun(library, null))
                    issues += cache.getPartialLinkageIssues(fileId = null)
            }
            is CachedLibraries.Cache.PerFile -> {
                for (fileId in cache.fileIds) {
                    if (!isRebuiltInThisRun(library, fileId))
                        issues += cache.getPartialLinkageIssues(fileId)
                }
            }
        }
    }

    if (issues.isEmpty()) return

    val logLevel = config.partialLinkageConfig.logLevel
    val logger = PartialLinkageLogger(
            KtDiagnosticReporterWithImplicitIrBasedContext(
                    config.configuration.diagnosticsCollector,
                    config.languageVersionSettings,
            ),
            logLevel,
    )

    issues.forEach { logger.log(it.message, it.location, it.significance) }

    // Make sure that the replayed issues abort the compilation exactly like the freshly detected ones would.
    if (logLevel == PartialLinkageLogLevel.ERROR)
        PartialLinkageErrorsLogged.raiseIssue(logger.diagnosticReporter)
}
