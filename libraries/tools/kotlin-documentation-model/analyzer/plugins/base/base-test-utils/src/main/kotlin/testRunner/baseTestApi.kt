/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

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
import org.jetbrains.dokka.testApi.testRunner.AbstractTest
import org.jetbrains.dokka.testApi.testRunner.CoreTestMethods
import org.jetbrains.dokka.testApi.testRunner.DokkaTestGenerator
import org.jetbrains.dokka.testApi.testRunner.TestBuilder
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.dokka.utilities.LoggingLevel

public class BaseDokkaTestGenerator(
    configuration: DokkaConfiguration,
    logger: DokkaLogger,
    testMethods: BaseTestMethods,
    additionalPlugins: List<DokkaPlugin> = emptyList()
) : DokkaTestGenerator<BaseTestMethods>(configuration, logger, testMethods, additionalPlugins) {

    override fun generate() {
        with(testMethods) {
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
}

public data class BaseTestMethods(
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

public class BaseTestBuilder : TestBuilder<BaseTestMethods>() {
    public var pluginsSetupStage: (DokkaContext) -> Unit = {}
    public var verificationStage: (() -> Unit) -> Unit = {}
    public var documentablesCreationStage: (List<DModule>) -> Unit = {}
    public var preMergeDocumentablesTransformationStage: (List<DModule>) -> Unit = {}
    public var documentablesMergingStage: (DModule) -> Unit = {}
    public var documentablesTransformationStage: (DModule) -> Unit = {}
    public var pagesGenerationStage: (RootPageNode) -> Unit = {}
    public var pagesTransformationStage: (RootPageNode) -> Unit = {}
    public var renderingStage: (RootPageNode, DokkaContext) -> Unit = { _, _ -> }

    override fun build(): BaseTestMethods {
        return BaseTestMethods(
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
}

public abstract class BaseAbstractTest(
    logger: TestLogger = TestLogger(DokkaConsoleLogger(LoggingLevel.DEBUG))
) : AbstractTest<BaseTestMethods, BaseTestBuilder, BaseDokkaTestGenerator>(
    ::BaseTestBuilder,
    ::BaseDokkaTestGenerator,
    logger,
)
