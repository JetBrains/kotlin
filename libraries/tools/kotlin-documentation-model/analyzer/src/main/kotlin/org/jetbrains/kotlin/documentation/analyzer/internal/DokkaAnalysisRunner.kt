/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer.internal

import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.base.generation.SingleModuleGeneration
import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.LoggingLevel

/**
 * Runs the Dokka analysis/merge pipeline (without any rendering step, which this module doesn't depend on) and
 * converts the single, merged [DModule] while the analysis session is still open, or returns `null` if there
 * was nothing to document.
 *
 * This mirrors the steps `SingleModuleGeneration.generate()` performs, stopping right after the documentation
 * model is built and merged, since KDM only needs the model, not a rendered site.
 */
internal fun <T> runDokkaAnalysis(configuration: DokkaConfigurationImpl, convert: (DModule, DokkaContext) -> T): T? {
    val logger = DokkaConsoleLogger(LoggingLevel.WARN)
    val context: DokkaContext = DokkaContext.create(configuration, logger, pluginOverrides = listOf(KdmDokkaPlugin()))
    val generation = context.single(CoreExtensions.generation) as SingleModuleGeneration
    try {
        generation.validityCheck(context)
        val modulesFromPlatforms = generation.createDocumentationModels()
        val beforeMerge = generation.transformDocumentationModelBeforeMerge(modulesFromPlatforms)
        val merged = generation.mergeDocumentationModels(beforeMerge) ?: return null
        return convert(generation.transformDocumentationModelAfterMerge(merged), context)
    } finally {
        generation.cleanUp()
    }
}
