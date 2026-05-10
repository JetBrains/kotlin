/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters.incremental

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.disposeRootInWriteAction
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.js.config.artifactConfigurations
import org.jetbrains.kotlin.js.config.icFilesToLoad
import org.jetbrains.kotlin.test.NonGroupingTestRunner
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.testConfiguration
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.RECOMPILE
import org.jetbrains.kotlin.test.impl.NonGroupingPhaseTestConfigurationImpl
import org.jetbrains.kotlin.test.impl.testConfiguration
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsSecondStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.impl.TestModuleStructureImpl

abstract class CommonRecompileModuleJsBackendFacade(
    val testServices: TestServices,
    val backendKind: TargetBackend
) : AbstractTestFacade<BinaryArtifacts.Js, BinaryArtifacts.Js>() {
    override val inputKind: ArtifactKinds.Js
        get() = ArtifactKinds.Js
    override val outputKind: ArtifactKinds.Js
        get() = ArtifactKinds.Js

    abstract fun TestConfigurationBuilder.configure(module: TestModule)
    abstract fun TestServices.register(module: TestModule)

    private class JsIcEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
        override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
            configuration.icFilesToLoad = module.files.map { "/${it.relativePath}" }.toSet()
            configuration.artifactConfigurations = JsSecondStageEnvironmentConfigurator.getArtifactConfigurations(
                testServices,
                module,
                configuration,
                firstTimeCompilation = false,
            )
        }
    }

    @OptIn(TestInfrastructureInternals::class)
    override fun transform(module: TestModule, inputArtifact: BinaryArtifacts.Js): BinaryArtifacts.Js {
        val filesToRecompile = module.files.filter { RECOMPILE in it.directives }

        val builder = (testServices.testConfiguration as NonGroupingPhaseTestConfigurationImpl).originalBuilder
        val incrementalConfiguration = testConfiguration(builder.testDataPath) {
            assertions = builder.assertions
            testInfo = builder.testInfo
            startingArtifactFactory = builder.startingArtifactFactory
            useSourcePreprocessor(*builder.sourcePreprocessors.toTypedArray())
            useMetaInfoProcessors(*builder.additionalMetaInfoProcessors.toTypedArray())
            useConfigurators(
                *builder.environmentConfigurators.toTypedArray(),
                ::JsIcEnvironmentConfigurator,
            )
            useDirectives(*builder.directives.toTypedArray())
            useAdditionalServices(*builder.additionalServices.toTypedArray())
            useCustomCompilerConfigurationProvider(::CompilerConfigurationProviderImpl)
            builder.globalDefaultsConfigurators.forEach { globalDefaults(it) }
            builder.defaultDirectiveConfigurators.forEach { defaultDirectives(it) }

            configure(module)
        }

        // The incremental configuration creates its own test root disposable. It needs to be properly handled to avoid disposable leaks.
        Disposer.register(testServices.testConfiguration.rootDisposable, incrementalConfiguration.rootDisposable)

        val moduleStructure = testServices.moduleStructure
        val incrementalModule = module.copy(files = filesToRecompile)
        val incrementalModuleStructure = TestModuleStructureImpl(
            moduleStructure.modules.map {
                if (it != module) return@map it
                else incrementalModule
            },
            moduleStructure.originalTestDataFiles
        )
        val incrementalRunner = NonGroupingTestRunner(incrementalConfiguration)
        val incrementalArtifactsProvider = testServices.artifactsProvider.copy().also {
            it.unregisterAllArtifacts(module)
        }

        val incrementalServices = incrementalConfiguration.testServices
        incrementalServices.registerArtifactsProvider(incrementalArtifactsProvider)
        incrementalServices.register(TestModuleStructure::class, incrementalModuleStructure)
        incrementalServices.register(TemporaryDirectoryManager::class, testServices.temporaryDirectoryManager)

        incrementalServices.register(module)

        val incrementalArtifact = try {
            incrementalRunner.processModule(incrementalModule, incrementalArtifactsProvider)
            incrementalRunner.failuresInterceptor.reportFailures(checkForUnmuting = true)
            incrementalArtifactsProvider.getArtifact(incrementalModule, ArtifactKinds.Js)
        } finally {
            disposeRootInWriteAction(incrementalConfiguration.rootDisposable)
        }

        return IncrementalJsArtifact(inputArtifact, incrementalArtifact)
    }

    override fun shouldTransform(module: TestModule): Boolean {
        return testServices.defaultsProvider.targetBackend == backendKind &&
                JsEnvironmentConfigurator.run { incrementalEnabled(testServices) && module.hasFilesToRecompile() }
    }
}
