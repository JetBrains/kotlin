/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.testApi.testRunner

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaGenerator
import org.jetbrains.dokka.base.generation.SingleModuleGeneration
import org.jetbrains.dokka.model.DModule
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
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInAnalysisApi

 class BaseDokkaTestGenerator internal constructor(
    configuration: DokkaConfiguration,
    logger: DokkaLogger,
    testMethods: BaseTestMethods,
    additionalPlugins: List<DokkaPlugin> = emptyList()
) : DokkaTestGenerator<BaseTestMethods>(configuration, logger, testMethods, additionalPlugins) {

    override fun generate(): Unit {
        var singleModuleGenerationForCleanUp: SingleModuleGeneration? = null
        try {
            with(testMethods) {
                val dokkaGenerator = DokkaGenerator(configuration, logger)

                val context =
                    dokkaGenerator.initializePlugins(configuration, logger, additionalPlugins)
                pluginsSetupStage(context)

                val singleModuleGeneration = context.single(CoreExtensions.generation) as SingleModuleGeneration
                singleModuleGenerationForCleanUp = singleModuleGeneration

                verificationStage { singleModuleGeneration.validityCheck(context) }

                val modulesFromPlatforms = singleModuleGeneration.createDocumentationModels()
                documentablesCreationStage(modulesFromPlatforms)

                val filteredModules =
                    singleModuleGeneration.transformDocumentationModelBeforeMerge(modulesFromPlatforms)
                documentablesFirstTransformationStep(filteredModules)

                val documentationModel = singleModuleGeneration.mergeDocumentationModels(filteredModules)
                documentablesMergingStage(documentationModel!!)

                val transformedDocumentation =
                    singleModuleGeneration.transformDocumentationModelAfterMerge(documentationModel)
                documentablesTransformationStage(transformedDocumentation)

                singleModuleGeneration.runPostActions()

                singleModuleGeneration.reportAfterRendering()
            }
        } finally {
            singleModuleGenerationForCleanUp?.cleanUp()
        }
    }
}

@ConsistentCopyVisibility
data class BaseTestMethods internal constructor(
    override val pluginsSetupStage: (DokkaContext) -> Unit,
    override val verificationStage: (() -> Unit) -> Unit,
    override val documentablesCreationStage: (List<DModule>) -> Unit,
    internal val documentablesFirstTransformationStep: (List<DModule>) -> Unit,
    override val documentablesMergingStage: (DModule) -> Unit,
    override val documentablesTransformationStage: (DModule) -> Unit,
 ) : CoreTestMethods(
    pluginsSetupStage,
    verificationStage,
    documentablesCreationStage,
    documentablesMergingStage,
    documentablesTransformationStage,
  )

public class BaseTestBuilder : TestBuilder<BaseTestMethods>() {
    internal var pluginsSetupStage: (DokkaContext) -> Unit = {}
    internal var verificationStage: (() -> Unit) -> Unit = { it() }
    internal var documentablesCreationStage: (List<DModule>) -> Unit = {}
    internal var preMergeDocumentablesTransformationStage: (List<DModule>) -> Unit = {}
    internal var documentablesMergingStage: (DModule) -> Unit = {}
    internal var documentablesTransformationStage: (DModule) -> Unit = {}

    override fun build(): BaseTestMethods {
        return BaseTestMethods(
            pluginsSetupStage,
            verificationStage,
            documentablesCreationStage,
            preMergeDocumentablesTransformationStage,
            documentablesMergingStage,
            documentablesTransformationStage,
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
