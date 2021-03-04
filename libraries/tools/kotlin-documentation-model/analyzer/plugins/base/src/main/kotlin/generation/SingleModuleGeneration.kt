package org.jetbrains.dokka.base.generation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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

class SingleModuleGeneration(private val context: DokkaContext) : Generation {

    override fun Timer.generate() {
        report("Validity check")
        validityCheck(context)

        report("Creating documentation models")
        val modulesFromPlatforms = createDocumentationModels()

        report("Transforming documentation model before merging")
        val transformedDocumentationBeforeMerge = transformDocumentationModelBeforeMerge(modulesFromPlatforms)

        report("Merging documentation models")
        val documentationModel = mergeDocumentationModels(transformedDocumentationBeforeMerge)
            ?: exitGenerationGracefully("Nothing to document")

        report("Transforming documentation model after merging")
        val transformedDocumentation = transformDocumentationModelAfterMerge(documentationModel)

        report("Creating pages")
        val pages = createPages(transformedDocumentation)

        report("Transforming pages")
        val transformedPages = transformPages(pages)

        report("Rendering")
        render(transformedPages)

        reportAfterRendering()
    }

    override val generationName = "documentation for ${context.configuration.moduleName}"

    fun createDocumentationModels() = runBlocking(Dispatchers.Default) {
        context.configuration.sourceSets.parallelMap { sourceSet -> translateSources(sourceSet, context) }.flatten()
            .also { modules -> if (modules.isEmpty()) exitGenerationGracefully("Nothing to document") }
    }

    fun transformDocumentationModelBeforeMerge(modulesFromPlatforms: List<DModule>) =
        context.plugin<DokkaBase>().query { preMergeDocumentableTransformer }
            .fold(modulesFromPlatforms) { acc, t -> t(acc) }

    fun mergeDocumentationModels(modulesFromPlatforms: List<DModule>) =
        context.single(CoreExtensions.documentableMerger).invoke(modulesFromPlatforms)

    fun transformDocumentationModelAfterMerge(documentationModel: DModule) =
        context[CoreExtensions.documentableTransformer].fold(documentationModel) { acc, t -> t(acc, context) }

    fun createPages(transformedDocumentation: DModule) =
        context.single(CoreExtensions.documentableToPageTranslator).invoke(transformedDocumentation)

    fun transformPages(pages: RootPageNode) =
        context[CoreExtensions.pageTransformer].fold(pages) { acc, t -> t(acc) }

    fun render(transformedPages: RootPageNode) {
        context.single(CoreExtensions.renderer).render(transformedPages)
    }

    fun validityCheck(context: DokkaContext) {
        val (preGenerationCheckResult, checkMessages) = context[CoreExtensions.preGenerationCheck].fold(
            Pair(true, emptyList<String>())
        ) { acc, checker -> checker() + acc }
        if (!preGenerationCheckResult) throw DokkaException(
            "Pre-generation validity check failed: ${checkMessages.joinToString(",")}"
        )
    }

    fun reportAfterRendering() {
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
