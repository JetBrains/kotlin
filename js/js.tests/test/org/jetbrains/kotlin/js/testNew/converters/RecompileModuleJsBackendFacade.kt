/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testNew.converters

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapParser
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapSuccess
import org.jetbrains.kotlin.js.testNew.utils.jsClassicIncrementalDataProvider
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.backend.classic.ClassicBackendInput
import org.jetbrains.kotlin.test.builders.testConfiguration
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.RECOMPILE
import org.jetbrains.kotlin.test.impl.testConfiguration
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.impl.TestModuleStructureImpl
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.charset.Charset

@Suppress("warnings")
class RecompileModuleJsBackendFacade<R : ResultingArtifact.FrontendOutput<R>>(
    val testServices: TestServices,
    private val frontendFacade: Constructor<FrontendFacade<R>>,
    private val frontend2BackendConverter: Constructor<Frontend2BackendConverter<R, ClassicBackendInput>>
) : AbstractTestFacade<BinaryArtifacts.Js, BinaryArtifacts.Js>() {
    override val inputKind: ArtifactKinds.Js
        get() = ArtifactKinds.Js
    override val outputKind: ArtifactKinds.Js
        get() = ArtifactKinds.Js

    @OptIn(TestInfrastructureInternals::class)
    override fun transform(module: TestModule, inputArtifact: BinaryArtifacts.Js): BinaryArtifacts.Js {
        val filesToRecompile = module.files.filter { RECOMPILE in it.directives }
        if (filesToRecompile.isEmpty()) return inputArtifact

        val builder = testServices.testConfiguration.originalBuilder
        val incrementalConfiguration = testConfiguration(builder.testDataPath) {
            assertions = builder.assertions
            testInfo = builder.testInfo
            startingArtifactFactory = builder.startingArtifactFactory
            useSourcePreprocessor(*builder.sourcePreprocessors.toTypedArray())
            useMetaInfoProcessors(*builder.additionalMetaInfoProcessors.toTypedArray())
            useConfigurators(*builder.environmentConfigurators.toTypedArray())
            useDirectives(*builder.directives.toTypedArray())
            useAdditionalServices(*builder.additionalServices.toTypedArray())
            builder.globalDefaultsConfigurators.forEach { globalDefaults(it) }
            builder.defaultDirectiveConfigurators.forEach { defaultDirectives(it) }
            builder.configurationsByPositiveTestDataCondition.forEach { (regex, init) -> forTestsMatching(regex, init) }
            builder.configurationsByNegativeTestDataCondition.forEach { (regex, init) -> forTestsNotMatching(regex, init) }

            facadeStep(frontendFacade)
            facadeStep(frontend2BackendConverter)
            facadeStep { ClassicJsBackendFacade(it, incrementalCompilationEnabled = true) }
        }

        val moduleStructure = testServices.moduleStructure
        val incrementalModule = module.copy(files = filesToRecompile)
        val incrementalModuleStructure = TestModuleStructureImpl(
            moduleStructure.modules.map {
                if (it != module) return@map it
                else incrementalModule
            },
            moduleStructure.originalTestDataFiles
        )
        val incrementalRunner = TestRunner(incrementalConfiguration)
        val incrementalDependencyProvider = testServices.dependencyProvider.copy().also {
            it.unregisterAllArtifacts(module)
        } as DependencyProviderImpl

        val incrementalServices = incrementalConfiguration.testServices
        incrementalServices.registerDependencyProvider(incrementalDependencyProvider)
        incrementalServices.register(TestModuleStructure::class, incrementalModuleStructure)
        incrementalServices.register(TemporaryDirectoryManager::class, testServices.temporaryDirectoryManager)

        val incrementalData = testServices.jsClassicIncrementalDataProvider.getIncrementalData(module).copy()
        for (testFile in filesToRecompile) {
            incrementalData.translatedFiles.remove(File("/${testFile.relativePath}"))
        }

        incrementalServices.jsClassicIncrementalDataProvider.recordIncrementalData(
            module,
            incrementalData
        )
        incrementalRunner.processModule(incrementalModule, incrementalDependencyProvider)
        incrementalRunner.reportFailures(incrementalServices)

        val incrementalArtifact = incrementalDependencyProvider.getArtifact(incrementalModule, ArtifactKinds.Js)
        return BinaryArtifacts.Js.IncrementalJsArtifact(inputArtifact, incrementalArtifact)
    }

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return module.targetBackend == TargetBackend.JS && module.files.any { RECOMPILE in it.directives }
    }
}
