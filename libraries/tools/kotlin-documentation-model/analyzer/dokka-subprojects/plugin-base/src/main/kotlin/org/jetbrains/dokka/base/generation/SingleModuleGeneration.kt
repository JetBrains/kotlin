/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.generation

import kotlinx.coroutines.*
import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaException
import org.jetbrains.dokka.Timer
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.generation.Generation
import org.jetbrains.dokka.generation.exitGenerationGracefully
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.transformers.sources.AsyncSourceToDocumentableTranslator
import org.jetbrains.dokka.utilities.parallelMap
import org.jetbrains.dokka.utilities.report

public class SingleModuleGeneration(private val context: DokkaContext) : Generation {

    override fun Timer.generate() {
        report("Validity check")
        validityCheck(context)

        // Step 1: translate sources into documentables & transform documentables (change internally)
        report("Creating documentation models")
        val modulesFromPlatforms = createDocumentationModels()

        report("Transforming documentation model before merging")
        val transformedDocumentationBeforeMerge = transformDocumentationModelBeforeMerge(modulesFromPlatforms)

        report("Merging documentation models")
        val transformedDocumentationAfterMerge = mergeDocumentationModels(transformedDocumentationBeforeMerge)
            ?: exitGenerationGracefully("Nothing to document")

        report("Transforming documentation model after merging")
        val transformedDocumentation = transformDocumentationModelAfterMerge(transformedDocumentationAfterMerge)

        // Step 2: Generate pages & transform them (change internally)
        report("Creating pages")
        val pages = createPages(transformedDocumentation)

        report("Transforming pages")
        val transformedPages = transformPages(pages)

        // Step 3: Rendering
        report("Rendering")
        render(transformedPages)

        report("Running post-actions")
        runPostActions()

        reportAfterRendering()
    }

    override val generationName: String = "documentation for ${context.configuration.moduleName}"

    /**
     * Implementation note: it runs in a separated single thread due to existing support of coroutines (see #2936)
     */
    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    public fun createDocumentationModels(): List<DModule> = newSingleThreadContext("Generating documentable model").use { coroutineContext -> // see https://github.com/Kotlin/dokka/issues/3151
        runBlocking(coroutineContext) {
            context.configuration.sourceSets.parallelMap { sourceSet -> translateSources(sourceSet, context) }.flatten()
                .also { modules -> if (modules.isEmpty()) exitGenerationGracefully("Nothing to document") }
        }
    }


    public fun transformDocumentationModelBeforeMerge(modulesFromPlatforms: List<DModule>): List<DModule> {
        return context.plugin<DokkaBase>()
            .query { preMergeDocumentableTransformer }
            .fold(modulesFromPlatforms) { acc, t -> t(acc) }
    }

    public fun mergeDocumentationModels(modulesFromPlatforms: List<DModule>): DModule? =
        context.single(CoreExtensions.documentableMerger).invoke(modulesFromPlatforms)

    public fun transformDocumentationModelAfterMerge(documentationModel: DModule): DModule =
        context[CoreExtensions.documentableTransformer].fold(documentationModel) { acc, t -> t(acc, context) }

    public fun createPages(transformedDocumentation: DModule): RootPageNode =
        context.single(CoreExtensions.documentableToPageTranslator).invoke(transformedDocumentation)

    public fun transformPages(pages: RootPageNode): RootPageNode =
        context[CoreExtensions.pageTransformer].fold(pages) { acc, t -> t(acc) }

    public fun render(transformedPages: RootPageNode) {
        context.single(CoreExtensions.renderer).render(transformedPages)
    }

    public fun runPostActions() {
        context[CoreExtensions.postActions].forEach { it() }
    }

    public fun validityCheck(context: DokkaContext) {
        val (preGenerationCheckResult, checkMessages) = context[CoreExtensions.preGenerationCheck].fold(
            Pair(true, emptyList<String>())
        ) { acc, checker -> checker() + acc }
        if (!preGenerationCheckResult) throw DokkaException(
            "Pre-generation validity check failed: ${checkMessages.joinToString(",")}"
        )
    }

    public fun reportAfterRendering() {
        context.unusedPoints.takeIf { it.isNotEmpty() }?.also {
            context.logger.info("Unused extension points found: ${it.joinToString(", ")}")
        }

        context.logger.report()

        if (context.configuration.failOnWarning && (context.logger.warningsCount > 0 || context.logger.errorsCount > 0)) {
            throw DokkaException(
                "Failed with warningCount=${context.logger.warningsCount} and errorCount=${context.logger.errorsCount}"
            )
        }
    }

    private suspend fun translateSources(sourceSet: DokkaConfiguration.DokkaSourceSet, context: DokkaContext) =
        context[CoreExtensions.sourceToDocumentableTranslator].parallelMap { translator ->
            when (translator) {
                is AsyncSourceToDocumentableTranslator -> translator.invokeSuspending(sourceSet, context)
                else -> translator.invoke(sourceSet, context)
            }
        }
}
