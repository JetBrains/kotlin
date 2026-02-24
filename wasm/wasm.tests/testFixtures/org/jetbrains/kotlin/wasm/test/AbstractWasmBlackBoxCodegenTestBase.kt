/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.KlibBackendDiagnosticsHandler
import org.jetbrains.kotlin.test.backend.handlers.NoIrCompilationErrorsHandler
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.configuration.commonClassicFrontendHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_DUMP
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.RENDER_FIR_DECLARATION_ATTRIBUTES
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.configuration.WasmFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmSecondStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.utils.bind
import org.jetbrains.kotlin.wasm.test.converters.WasmPreSerializationLoweringFacade
import org.jetbrains.kotlin.wasm.test.handlers.WasmIrHandler
import org.jetbrains.kotlin.wasm.test.handlers.WasmDtsHandler
import org.jetbrains.kotlin.wasm.test.handlers.WasmTypeScriptCompilationHandler

abstract class AbstractWasmBlackBoxCodegenTestBase<R : ResultingArtifact.FrontendOutput<R>, I : ResultingArtifact.BackendInput<I>, A : ResultingArtifact.Binary<A>>(
    private val targetFrontend: FrontendKind<R>,
    targetBackend: TargetBackend,
    private val targetPlatform: TargetPlatform,
    private val pathToTestDir: String,  // must be set to the common path prefix for all testroots provided for a certain class in GenerateWasmTests.kt
    private val testGroupOutputDirPrefix: String,
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    abstract val frontendFacade: Constructor<FrontendFacade<R>>
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<R, I>>
    abstract val backendFacade: Constructor<BackendFacade<I, A>>
    abstract val afterBackendFacade: Constructor<AbstractTestFacade<A, BinaryArtifacts.Wasm>>
    abstract val wasmBoxTestRunner: Constructor<AnalysisHandler<BinaryArtifacts.Wasm>>
    abstract val wasmTarget: WasmTarget
    open val additionalSourceProvider: Constructor<AdditionalSourceProvider>? = null
    protected open val customIgnoreDirective: ValueDirective<TargetBackend>?
        get() = null
    protected open val additionalIgnoreDirectives: List<ValueDirective<TargetBackend>>?
        get() = null

    protected fun TestConfigurationBuilder.commonConfigurationForWasmBlackBoxCodegenTest(
        customIgnoreDirective: ValueDirective<TargetBackend>? = null,
        additionalIgnoreDirectives: List<ValueDirective<TargetBackend>>? = null,
    ) {
        commonConfigurationForWasmFirstStageTest(
            targetFrontend,
            targetPlatform,
            wasmTarget,
            frontendFacade,
            frontendToBackendConverter,
            backendFacade,
            additionalSourceProvider,
            customIgnoreDirective,
            additionalIgnoreDirectives,
        )
        commonConfigurationForWasmSecondStageTest(
            pathToTestDir,
            testGroupOutputDirPrefix,
        )
        useConfigurators(
            ::WasmSecondStageEnvironmentConfigurator.bind(wasmTarget),
        )
        facadeStep(afterBackendFacade)

        wasmArtifactsHandlersStep {
            useHandlers(::WasmTypeScriptCompilationHandler)
            useHandlers(wasmBoxTestRunner)
            useHandlers(::WasmIrHandler)
            useHandlers(::WasmDtsHandler)
        }
    }

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        commonConfigurationForWasmBlackBoxCodegenTest(customIgnoreDirective, additionalIgnoreDirectives)

        forTestsNotMatching(
            "compiler/testData/codegen/box/diagnostics/functions/tailRecursion/*" or
                    "compiler/testData/diagnostics/*"
        ) {
            defaultDirectives {
                DIAGNOSTICS with "-warnings"
            }
        }

        enableMetaInfoHandler()
        forTestsMatching("compiler/testData/codegen/box/evaluate/*") {
            enableMetaInfoHandler()
            defaultDirectives {
                +FIR_DUMP
                +RENDER_FIR_DECLARATION_ATTRIBUTES
            }
        }
    }
}

fun <R : ResultingArtifact.FrontendOutput<R>, I : ResultingArtifact.BackendInput<I>, A : ResultingArtifact.Binary<A>>
        TestConfigurationBuilder.commonConfigurationForWasmFirstStageTest(
    targetFrontend: FrontendKind<R>,
    targetPlatform: TargetPlatform,
    wasmTarget: WasmTarget,
    frontendFacade: Constructor<FrontendFacade<R>>,
    frontendToBackendConverter: Constructor<Frontend2BackendConverter<R, I>>,
    backendFacade: Constructor<BackendFacade<I, A>>,
    additionalSourceProvider: Constructor<AdditionalSourceProvider>?,
    customIgnoreDirective: ValueDirective<TargetBackend>? = null,
    additionalIgnoreDirectives: List<ValueDirective<TargetBackend>>? = null,
) {
    globalDefaults {
        frontend = targetFrontend
        this.targetPlatform = targetPlatform
        dependencyKind = DependencyKind.Binary
    }

    defaultDirectives {
        LANGUAGE with "+JsAllowImplementingFunctionInterface"
    }

    useConfigurators(
        ::WasmFirstStageEnvironmentConfigurator.bind(wasmTarget),
    )

    useAdditionalSourceProviders(
        ::WasmAdditionalSourceProvider,
        ::CoroutineHelpersSourceFilesProvider,
        ::AdditionalDiagnosticsSourceFilesProvider,
    )

    additionalSourceProvider?.let {
        useAdditionalSourceProviders(it)
    }

    useAdditionalService(::LibraryProvider)

    useAfterAnalysisCheckers(
        ::BlackBoxCodegenSuppressor.bind(customIgnoreDirective, additionalIgnoreDirectives),
    )

    facadeStep(frontendFacade)

    classicFrontendHandlersStep {
        commonClassicFrontendHandlersForCodegenTest()
        useHandlers(::ClassicDiagnosticsHandler)
    }

    firHandlersStep {
        useHandlers(::FirDiagnosticsHandler)
    }

    facadeStep(frontendToBackendConverter)
    irHandlersStep {
        useHandlers(::NoIrCompilationErrorsHandler)
    }

    facadeStep(::WasmPreSerializationLoweringFacade)
    loweredIrHandlersStep {
        useHandlers(::NoIrCompilationErrorsHandler)
    }

    loweredIrHandlersStep()

    facadeStep(backendFacade)
    klibArtifactsHandlersStep {
        useHandlers(::KlibBackendDiagnosticsHandler)
    }
}

/**
 * Sets up configuration for Wasm second compilation phase (compilation from KLib to Wasm code).
 * First compilation phase should be set up separately with [commonConfigurationForWasmFirstStageTest].
 */
fun TestConfigurationBuilder.commonConfigurationForWasmSecondStageTest(
    pathToTestDir: String,
    testGroupOutputDirPrefix: String,
) {
    val pathToRootOutputDir = System.getProperty("kotlin.wasm.test.root.out.dir") ?: error("'kotlin.wasm.test.root.out.dir' is not set")
    val pathToNodeDir = System.getProperty("kotlin.wasm.test.node.dir") ?: error("'kotlin.wasm.test.node.dir' is not set")
    defaultDirectives {
        WasmEnvironmentConfigurationDirectives.PATH_TO_ROOT_OUTPUT_DIR with pathToRootOutputDir
        WasmEnvironmentConfigurationDirectives.PATH_TO_NODE_DIR with pathToNodeDir
        WasmEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR with pathToTestDir
        WasmEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX with testGroupOutputDirPrefix
    }
}