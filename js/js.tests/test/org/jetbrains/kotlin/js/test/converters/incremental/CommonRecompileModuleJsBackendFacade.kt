/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters.incremental

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.TestRunner
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.testConfiguration
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives.RECOMPILE
import org.jetbrains.kotlin.test.impl.testConfiguration
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.impl.TestModuleStructureImpl

@Suppress("warnings")
abstract class CommonRecompileModuleJsBackendFacade<R : ResultingArtifact.FrontendOutput<R>, I : ResultingArtifact.BackendInput<I>>(
    val testServices: TestServices,
    val backendKind: TargetBackend
) : AbstractTestFacade<BinaryArtifacts.Js, BinaryArtifacts.Js>() {
    override val inputKind: ArtifactKinds.Js
        get() = ArtifactKinds.Js
    override val outputKind: ArtifactKinds.Js
        get() = ArtifactKinds.Js

    abstract fun TestConfigurationBuilder.configure(module: TestModule)
    abstract fun TestServices.register(module: TestModule)

    @OptIn(TestInfrastructureInternals::class)
    override fun transform(module: TestModule, inputArtifact: BinaryArtifacts.Js): BinaryArtifacts.Js {
        val filesToRecompile = module.files.filter { RECOMPILE in it.directives }

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
        val incrementalRunner = TestRunner(incrementalConfiguration)
        val incrementalDependencyProvider = testServices.dependencyProvider.copy().also {
            it.unregisterAllArtifacts(module)
        } as DependencyProviderImpl

        val incrementalServices = incrementalConfiguration.testServices
        incrementalServices.registerDependencyProvider(incrementalDependencyProvider)
        incrementalServices.register(TestModuleStructure::class, incrementalModuleStructure)
        incrementalServices.register(TemporaryDirectoryManager::class, testServices.temporaryDirectoryManager)

        incrementalServices.register(module)

        val incrementalArtifact = try {
            incrementalRunner.processModule(incrementalModule, incrementalDependencyProvider)
            incrementalRunner.reportFailures(incrementalServices)
            incrementalDependencyProvider.getArtifact(incrementalModule, ArtifactKinds.Js)
        } finally {
            Disposer.dispose(incrementalConfiguration.rootDisposable)
        }

        return BinaryArtifacts.Js.IncrementalJsArtifact(inputArtifact, incrementalArtifact)
    }

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return module.targetBackend == backendKind && JsEnvironmentConfigurator.run { incrementalEnabled(testServices) && module.hasFilesToRecompile()}
    }
}
