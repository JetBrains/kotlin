package org.jetbrains.dokka.testApi.testRunner

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaGenerator
import org.jetbrains.dokka.generation.SingleModuleGeneration
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.utilities.DokkaLogger

internal class DokkaTestGenerator(
    private val configuration: DokkaConfiguration,
    private val logger: DokkaLogger,
    private val testMethods: TestMethods,
    private val additionalPlugins: List<DokkaPlugin> = emptyList()
) {

    fun generate() = with(testMethods) {
        val dokkaGenerator = DokkaGenerator(configuration, logger)

        val context =
            dokkaGenerator.initializePlugins(configuration, logger, additionalPlugins)
        pluginsSetupStage(context)

        val singleModuleGeneration = context.single(CoreExtensions.generation) as SingleModuleGeneration

        val modulesFromPlatforms = singleModuleGeneration.createDocumentationModels()
        documentablesCreationStage(modulesFromPlatforms)

        verificationStage { singleModuleGeneration.validityCheck(context) }

        val filteredModules = singleModuleGeneration.transformDocumentationModelBeforeMerge(modulesFromPlatforms)
        documentablesFirstTransformationStep(filteredModules)

        val documentationModel = singleModuleGeneration.mergeDocumentationModels(filteredModules)
        documentablesMergingStage(documentationModel)

        val transformedDocumentation = singleModuleGeneration.transformDocumentationModelAfterMerge(documentationModel)
        documentablesTransformationStage(transformedDocumentation)

        val pages = singleModuleGeneration.createPages(transformedDocumentation)
        pagesGenerationStage(pages)

        val transformedPages = singleModuleGeneration.transformPages(pages)
        pagesTransformationStage(transformedPages)

        singleModuleGeneration.render(transformedPages)
        renderingStage(transformedPages, context)

        singleModuleGeneration.reportAfterRendering()
    }
}
