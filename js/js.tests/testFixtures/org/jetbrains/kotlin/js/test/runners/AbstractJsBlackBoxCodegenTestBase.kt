/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.runners

import org.jetbrains.kotlin.js.test.JsAdditionalSourceProvider
import org.jetbrains.kotlin.js.test.converters.*
import org.jetbrains.kotlin.js.test.converters.incremental.RecompileModuleJsIrBackendFacade
import org.jetbrains.kotlin.js.test.handlers.*
import org.jetbrains.kotlin.js.test.runners.AbstractJsBlackBoxCodegenTestBase.JsBackendFacades
import org.jetbrains.kotlin.js.test.runners.AbstractJsBlackBoxCodegenTestBase.JsBackendFacades.WithRecompilation.deserializerAndLoweringFacade
import org.jetbrains.kotlin.js.test.runners.AbstractJsBlackBoxCodegenTestBase.JsBackendFacades.WithRecompilation.recompileFacade
import org.jetbrains.kotlin.js.test.runners.AbstractJsBlackBoxCodegenTestBase.JsBackendFacades.WithSeparatedDeserialization.postDeserializationHandler
import org.jetbrains.kotlin.js.test.runners.AbstractJsBlackBoxCodegenTestBase.JsBackendFacades.WithSeparatedDeserialization.preSerializationHandler
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.*
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_DUMP
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.RENDER_FIR_DECLARATION_ATTRIBUTES
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.AbstractEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsSecondStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.bind
import java.lang.Boolean.getBoolean

abstract class AbstractJsBlackBoxCodegenTestBase(
    targetBackend: TargetBackend,
    private val pathToTestDir: String,
    private val testGroupOutputDirPrefix: String,
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    /**
     * There can be several configurations of JS codegen/box tests, which differ in a way how backend part
     * of the test pipeline is executed.
     *
     * [JsBackendFacades] helps to configure the backend part of the test pipeline.
     */
    sealed interface JsBackendFacades {
        /**
         * The backend part of the pipeline consists of the unified KLIB deserializer+lowerings facade [deserializerAndLoweringFacade]
         * and a recompilation facade [recompileFacade].
         *
         * The output artifact of [deserializerAndLoweringFacade] is [BinaryArtifacts.Js], which helps to avoid re-registering
         * [IrBackendInput] for the module from [IrBackendInput.JsIrAfterFrontendBackendInput] to
         * [IrBackendInput.JsIrDeserializedFromKlibBackendInput], which is essential for [recompileFacade].
         */
        object WithRecompilation : JsBackendFacades {
            val deserializerAndLoweringFacade: Constructor<AbstractTestFacade<BinaryArtifacts.KLib, BinaryArtifacts.Js>>
                get() = ::JsUnifiedIrDeserializerAndLoweringFacade

            val recompileFacade: Constructor<AbstractTestFacade<BinaryArtifacts.Js, BinaryArtifacts.Js>>
                get() = ::RecompileModuleJsIrBackendFacade
        }

        /**
         * The backend part that consists of a separated KLIB deserializer facade wrapped by [preSerializationHandler] and
         * [postDeserializationHandler] handlers. Used for testing IR deserialization.
         */
        object WithSeparatedDeserialization : JsBackendFacades {
            val preSerializationHandler: Constructor<AbstractIrHandler>
                get() = { SerializedIrDumpHandler(it, isAfterDeserialization = false) }

            val deserializerFacade: Constructor<AbstractTestFacade<BinaryArtifacts.KLib, IrBackendInput>>
                get() = ::JsIrDeserializerFacade

            val postDeserializationHandler: Constructor<AbstractIrHandler>
                get() = { SerializedIrDumpHandler(it, isAfterDeserialization = true) }
        }
    }

    abstract val backendFacades: JsBackendFacades

    protected open val customIgnoreDirective: ValueDirective<TargetBackend>?
        get() = null

    protected open val additionalIgnoreDirectives: List<ValueDirective<TargetBackend>>?
        get() = null

    protected open val enableBoxHandlers: Boolean
        get() = true

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        commonConfigurationForJsBlackBoxCodegenTest()
        if (enableBoxHandlers) {
            configureJsBoxHandlers()
        }
    }

    protected fun TestConfigurationBuilder.commonConfigurationForJsBlackBoxCodegenTest() {
        commonConfigurationForJsBackendFirstStageTest(
            customIgnoreDirective = customIgnoreDirective,
            additionalIgnoreDirectives = additionalIgnoreDirectives,
        )
        commonConfigurationForJsBackendSecondStageTest(
            pathToTestDir,
            testGroupOutputDirPrefix,
            backendFacades,
        )

        forTestsNotMatching(
            "compiler/testData/codegen/box/diagnostics/functions/tailRecursion/*" or
                    "compiler/testData/diagnostics/*"
        ) {
            defaultDirectives {
                DIAGNOSTICS with "-warnings"
            }
        }

        forTestsMatching("compiler/testData/codegen/box/evaluate/*") {
            defaultDirectives {
                +FIR_DUMP
                +RENDER_FIR_DECLARATION_ATTRIBUTES
            }
        }
    }
}

/**
 * Sets up full configuration for the JS tests for the first compilation phase (compilation to KLib).
 * The configuration includes all minimally required services, handlers and defaults for such tests.
 */
fun TestConfigurationBuilder.commonConfigurationForJsBackendFirstStageTest(
    customIgnoreDirective: ValueDirective<TargetBackend>? = null,
    additionalIgnoreDirectives: List<ValueDirective<TargetBackend>>? = null,
) {
    commonConfigurationForJsTest()
    setupCommonHandlersForJsTest(customIgnoreDirective, additionalIgnoreDirectives)
}

/**
 * Sets up configuration for JS second compilation phase (compilation from KLib to JS code).
 * First compilation phase should be set up separately with [commonConfigurationForJsBackendFirstStageTest].
 * The configuration includes all minimally required services, handlers and defaults for such tests.
 */
fun <FO : ResultingArtifact.FrontendOutput<FO>> TestConfigurationBuilder.commonConfigurationForJsBackendSecondStageTest(
    pathToTestDir: String,
    testGroupOutputDirPrefix: String,
    backendFacades: JsBackendFacades,
) {
    val pathToRootOutputDir = System.getProperty("kotlin.js.test.root.out.dir") ?: error("'kotlin.js.test.root.out.dir' is not set")
    defaultDirectives {
        JsEnvironmentConfigurationDirectives.PATH_TO_ROOT_OUTPUT_DIR with pathToRootOutputDir
        JsEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR with pathToTestDir
        JsEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX with testGroupOutputDirPrefix
        +JsEnvironmentConfigurationDirectives.GENERATE_NODE_JS_RUNNER
        if (getBoolean("kotlin.js.ir.skipRegularMode")) +JsEnvironmentConfigurationDirectives.SKIP_REGULAR_MODE
        LANGUAGE with "+JsAllowValueClassesInExternals"
    }

    when (val backendFacades = backendFacades) {
        is JsBackendFacades.WithRecompilation -> {
            facadeStep(backendFacades.deserializerAndLoweringFacade)
            facadeStep(backendFacades.recompileFacade)
        }

        is JsBackendFacades.WithSeparatedDeserialization -> {
            configureLoweredIrHandlersStep { useHandlers(backendFacades.preSerializationHandler) }
            facadeStep(backendFacades.deserializerFacade)
            deserializedIrHandlersStep { useHandlers(backendFacades.postDeserializationHandler) }
        }
    }

    jsArtifactsHandlersStep {
        useHandlers(
            ::JsSourceMapPathRewriter,
        )
    }

    useAfterAnalysisCheckers(
        ::JsArtifactsDumpHandler
    )
}

/**
 * Configures handlers for JS box testing
 */
fun TestConfigurationBuilder.configureJsBoxHandlers(verifyJsAst: Boolean = true) {
    configureJsArtifactsHandlersStep {
        useHandlers(
            ::JsTypeScriptCompilationHandler,
            ::NodeJsGeneratorHandler,
            ::JsBoxRunner,
        )
        runIf(verifyJsAst) {
            useHandlers(::JsAstHandler)
        }
    }
}

/**
 * Sets up the base test configuration for JS backend tests, including
 * - global defaults
 * - environment configurators
 * - additional source providers
 */
fun TestConfigurationBuilder.commonServicesConfigurationForJsCodegenTest(
    customConfigurators: List<Constructor<AbstractEnvironmentConfigurator>>? = null,
) {
    globalDefaults {
        frontend = FrontendKinds.FIR
        targetPlatform = JsPlatforms.defaultJsPlatform
        dependencyKind = DependencyKind.Binary
    }

    defaultDirectives {
        +DiagnosticsDirectives.REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
    }

    if (customConfigurators == null) {
        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JsFirstStageEnvironmentConfigurator,
            ::JsSecondStageEnvironmentConfigurator,
        )
    } else {
        useConfigurators(*customConfigurators.toTypedArray())
    }

    useAdditionalSourceProviders(
        ::JsAdditionalSourceProvider,
        ::CoroutineHelpersSourceFilesProvider,
        ::AdditionalDiagnosticsSourceFilesProvider,
    )
}

/**
 * Sets up the pipeline for all JS backend tests
 *
 * Steps:
 * - FIR frontend
 * - FIR2IR
 * - JS pre-serialization lowerings
 * - IR serialization
 *
 * There are handler steps after each facade step.
 */
fun TestConfigurationBuilder.commonConfigurationForJsTest() {
    commonServicesConfigurationForJsCodegenTest()
    facadeStep(::FirCliWebFacade)
    firHandlersStep()

    facadeStep(::Fir2IrCliWebFacade)
    irHandlersStep()

    facadeStep(::JsIrPreSerializationLoweringFacade)
    loweredIrHandlersStep()

    facadeStep(::FirKlibSerializerCliWebFacade)
    klibArtifactsHandlersStep()
}

fun TestConfigurationBuilder.setupCommonHandlersForJsTest(
    customIgnoreDirective: ValueDirective<TargetBackend>? = null,
    additionalIgnoreDirectives: List<ValueDirective<TargetBackend>>? = null
) {
    configureFirHandlersStep {
        useHandlers(::FirDiagnosticsHandler)
    }

    configureIrHandlersStep {
        useHandlers(::FirJsKlibAbiDumpBeforeInliningSavingHandler)
        useHandlers(::NoIrCompilationErrorsHandler)
        useHandlers(::IrMangledNameAndSignatureDumpHandler)
        useHandlers(::IrDiagnosticsHandler)
    }

    configureLoweredIrHandlersStep {
        useHandlers(::NoIrCompilationErrorsHandler)
    }

    configureKlibArtifactsHandlersStep {
        useHandlers(::KlibBackendDiagnosticsHandler, ::KlibAbiDumpAfterInliningVerifyingHandler)
    }

    useAfterAnalysisCheckers(
        ::BlackBoxCodegenSuppressor.bind(customIgnoreDirective, additionalIgnoreDirectives),
    )

    enableMetaInfoHandler()
}
