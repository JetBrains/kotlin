package org.jetbrains.dokka.base.testApi.testRunner

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaGenerator
import org.jetbrains.dokka.base.generation.SingleModuleGeneration
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.testApi.logger.TestLogger
import org.jetbrains.dokka.testApi.testRunner.*
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.dokka.utilities.LoggingLevel

class BaseDokkaTestGenerator(
    configuration: DokkaConfiguration,
    logger: DokkaLogger,
    testMethods: BaseTestMethods,
    additionalPlugins: List<DokkaPlugin> = emptyList()
) : DokkaTestGenerator<BaseTestMethods>(configuration, logger, testMethods, additionalPlugins) {

    override fun generate() = with(testMethods) {
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
        documentablesMergingStage(documentationModel!!)

        val transformedDocumentation = singleModuleGeneration.transformDocumentationModelAfterMerge(documentationModel)
        documentablesTransformationStage(transformedDocumentation)

        val pages = singleModuleGeneration.createPages(transformedDocumentation)
        pagesGenerationStage(pages)

        val transformedPages = singleModuleGeneration.transformPages(pages)
        pagesTransformationStage(transformedPages)

        singleModuleGeneration.render(transformedPages)
        renderingStage(transformedPages, context)

        singleModuleGeneration.runPostActions()

        singleModuleGeneration.reportAfterRendering()
    }
}

data class BaseTestMethods(
    override val pluginsSetupStage: (DokkaContext) -> Unit,
    override val verificationStage: (() -> Unit) -> Unit,
    override val documentablesCreationStage: (List<DModule>) -> Unit,
    val documentablesFirstTransformationStep: (List<DModule>) -> Unit,
    override val documentablesMergingStage: (DModule) -> Unit,
    override val documentablesTransformationStage: (DModule) -> Unit,
    override val pagesGenerationStage: (RootPageNode) -> Unit,
    override val pagesTransformationStage: (RootPageNode) -> Unit,
    override val renderingStage: (RootPageNode, DokkaContext) -> Unit
) : CoreTestMethods(
    pluginsSetupStage,
    verificationStage,
    documentablesCreationStage,
    documentablesMergingStage,
    documentablesTransformationStage,
    pagesGenerationStage,
    pagesTransformationStage,
    renderingStage,
)

class BaseTestBuilder : TestBuilder<BaseTestMethods>() {
    var pluginsSetupStage: (DokkaContext) -> Unit = {}
    var verificationStage: (() -> Unit) -> Unit = {}
    var documentablesCreationStage: (List<DModule>) -> Unit = {}
    var preMergeDocumentablesTransformationStage: (List<DModule>) -> Unit = {}
    var documentablesMergingStage: (DModule) -> Unit = {}
    var documentablesTransformationStage: (DModule) -> Unit = {}
    var pagesGenerationStage: (RootPageNode) -> Unit = {}
    var pagesTransformationStage: (RootPageNode) -> Unit = {}
    var renderingStage: (RootPageNode, DokkaContext) -> Unit = { _, _ -> }

    override fun build() = BaseTestMethods(
        pluginsSetupStage,
        verificationStage,
        documentablesCreationStage,
        preMergeDocumentablesTransformationStage,
        documentablesMergingStage,
        documentablesTransformationStage,
        pagesGenerationStage,
        pagesTransformationStage,
        renderingStage
    )
}

abstract class BaseAbstractTest(logger: TestLogger = TestLogger(DokkaConsoleLogger(LoggingLevel.DEBUG))) : AbstractTest<BaseTestMethods, BaseTestBuilder, BaseDokkaTestGenerator>(
    ::BaseTestBuilder,
    ::BaseDokkaTestGenerator,
    logger,
)
