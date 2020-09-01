@file:Suppress("SameParameterValue")

package org.jetbrains.dokka

import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.DokkaConfiguration.*
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.dokka.utilities.report


/**
 * DokkaGenerator is the main entry point for generating documentation
 *
 * [generate] method has been split into submethods for test reasons
 */
class DokkaGenerator(
    private val configuration: DokkaConfiguration,
    private val logger: DokkaLogger
) {
    fun generate() = timed(logger) {
        printDokkaMaturityWarning(logger)

        report("Initializing plugins")
        val context = initializePlugins(configuration, logger)

        report("Creating documentation models")
        val modulesFromPlatforms = createDocumentationModels(context)

        report("Transforming documentation model before merging")
        val transformedDocumentationBeforeMerge = transformDocumentationModelBeforeMerge(modulesFromPlatforms, context)

        report("Merging documentation models")
        val documentationModel = mergeDocumentationModels(transformedDocumentationBeforeMerge, context)

        report("Transforming documentation model after merging")
        val transformedDocumentation = transformDocumentationModelAfterMerge(documentationModel, context)

        report("Creating pages")
        val pages = createPages(transformedDocumentation, context)

        report("Transforming pages")
        val transformedPages = transformPages(pages, context)

        report("Rendering")
        render(transformedPages, context)

        reportAfterRendering(context)
    }.dump("\n\n === TIME MEASUREMENT ===\n")

    fun generateAllModulesPage() = timed {
        report("Initializing plugins")
        val context = initializePlugins(configuration, logger)

        report("Creating all modules page")
        val pages = createAllModulePage(context)

        report("Transforming pages")
        val transformedPages = transformAllModulesPage(pages, context)

        report("Rendering")
        render(transformedPages, context)
    }.dump("\n\n === TIME MEASUREMENT ===\n")


    fun initializePlugins(
        configuration: DokkaConfiguration,
        logger: DokkaLogger,
        additionalPlugins: List<DokkaPlugin> = emptyList()
    ) = DokkaContext.create(configuration, logger, additionalPlugins)

    fun createDocumentationModels(
        context: DokkaContext
    ) = context.configuration.sourceSets
        .flatMap { sourceSet -> translateSources(sourceSet, context) }
        .also { modules -> if (modules.isEmpty()) exitGenerationGracefully("Nothing to document") }

    fun transformDocumentationModelBeforeMerge(
        modulesFromPlatforms: List<DModule>,
        context: DokkaContext
    ) = context[CoreExtensions.preMergeDocumentableTransformer].fold(modulesFromPlatforms) { acc, t -> t(acc) }

    fun mergeDocumentationModels(
        modulesFromPlatforms: List<DModule>,
        context: DokkaContext
    ) = context.single(CoreExtensions.documentableMerger).invoke(modulesFromPlatforms, context)

    fun transformDocumentationModelAfterMerge(
        documentationModel: DModule,
        context: DokkaContext
    ) = context[CoreExtensions.documentableTransformer].fold(documentationModel) { acc, t -> t(acc, context) }

    fun createPages(
        transformedDocumentation: DModule,
        context: DokkaContext
    ) = context.single(CoreExtensions.documentableToPageTranslator).invoke(transformedDocumentation)

    fun createAllModulePage(
        context: DokkaContext
    ) = context.single(CoreExtensions.allModulePageCreator).invoke()

    fun transformPages(
        pages: RootPageNode,
        context: DokkaContext
    ) = context[CoreExtensions.pageTransformer].fold(pages) { acc, t -> t(acc) }

    fun transformAllModulesPage(
        pages: RootPageNode,
        context: DokkaContext
    ) = context[CoreExtensions.allModulePageTransformer].fold(pages) { acc, t -> t(acc) }

    fun render(
        transformedPages: RootPageNode,
        context: DokkaContext
    ) {
        val renderer = context.single(CoreExtensions.renderer)
        renderer.render(transformedPages)
    }

    fun reportAfterRendering(context: DokkaContext) {
        context.unusedPoints.takeIf { it.isNotEmpty() }?.also {
            logger.info("Unused extension points found: ${it.joinToString(", ")}")
        }

        logger.report()

        if (context.configuration.failOnWarning && (logger.warningsCount > 0 || logger.errorsCount > 0)) {
            throw DokkaException(
                "Failed with warningCount=${logger.warningsCount} and errorCount=${logger.errorsCount}"
            )
        }
    }

    private fun translateSources(sourceSet: DokkaSourceSet, context: DokkaContext) =
        context[CoreExtensions.sourceToDocumentableTranslator].map {
            it.invoke(sourceSet, context)
        }
}

private class Timer(startTime: Long, private val logger: DokkaLogger?) {
    private val steps = mutableListOf("" to startTime)

    fun report(name: String) {
        logger?.progress(name)
        steps += (name to System.currentTimeMillis())
    }

    fun dump(prefix: String = "") {
        logger?.info(prefix)
        val namePad = steps.map { it.first.length }.max() ?: 0
        val timePad = steps.windowed(2).map { (p1, p2) -> p2.second - p1.second }.max()?.toString()?.length ?: 0
        steps.windowed(2).forEach { (p1, p2) ->
            if (p1.first.isNotBlank()) {
                logger?.info("${p1.first.padStart(namePad)}: ${(p2.second - p1.second).toString().padStart(timePad)}")
            }
        }
    }
}

private fun timed(logger: DokkaLogger? = null, block: Timer.() -> Unit): Timer =
    Timer(System.currentTimeMillis(), logger).apply {
        try {
            block()
        } catch (exit: GracefulGenerationExit) {
            report("Exiting Generation: ${exit.reason}")
        } finally {
            report("")
        }
    }

private fun exitGenerationGracefully(reason: String): Nothing {
    throw GracefulGenerationExit(reason)
}

private class GracefulGenerationExit(val reason: String) : Throwable()
