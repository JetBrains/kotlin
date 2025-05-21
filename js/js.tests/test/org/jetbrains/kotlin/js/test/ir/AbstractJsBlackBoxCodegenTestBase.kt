/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ir

import org.jetbrains.kotlin.js.test.JsAdditionalSourceProvider
import org.jetbrains.kotlin.js.test.JsFailingTestSuppressor
import org.jetbrains.kotlin.js.test.converters.JsIrDeserializerFacade
import org.jetbrains.kotlin.js.test.converters.JsIrPreSerializationLoweringFacade
import org.jetbrains.kotlin.js.test.converters.JsUnifiedIrDeserializerAndLoweringFacade
import org.jetbrains.kotlin.js.test.converters.incremental.RecompileModuleJsIrBackendFacade
import org.jetbrains.kotlin.js.test.handlers.*
import org.jetbrains.kotlin.js.test.ir.AbstractJsBlackBoxCodegenTestBase.JsBackendFacades
import org.jetbrains.kotlin.js.test.ir.AbstractJsBlackBoxCodegenTestBase.JsBackendFacades.WithRecompilation.deserializerAndLoweringFacade
import org.jetbrains.kotlin.js.test.ir.AbstractJsBlackBoxCodegenTestBase.JsBackendFacades.WithRecompilation.recompileFacade
import org.jetbrains.kotlin.js.test.ir.AbstractJsBlackBoxCodegenTestBase.JsBackendFacades.WithSeparatedDeserialization.postDeserializationHandler
import org.jetbrains.kotlin.js.test.ir.AbstractJsBlackBoxCodegenTestBase.JsBackendFacades.WithSeparatedDeserialization.preSerializationHandler
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.*
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.configuration.commonClassicFrontendHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.AbstractEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.utils.bind
import java.lang.Boolean.getBoolean

abstract class AbstractJsBlackBoxCodegenTestBase<FO : ResultingArtifact.FrontendOutput<FO>>(
    val targetFrontend: FrontendKind<FO>,
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

    abstract val frontendFacade: Constructor<FrontendFacade<FO>>
    abstract val frontendToIrConverter: Constructor<Frontend2BackendConverter<FO, IrBackendInput>>
    abstract val serializerFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
    abstract val backendFacades: JsBackendFacades

    protected open val customIgnoreDirective: ValueDirective<TargetBackend>?
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
            targetFrontend = targetFrontend,
            frontendFacade = frontendFacade,
            frontendToIrConverter = frontendToIrConverter,
            serializerFacade = serializerFacade,
            customIgnoreDirective = customIgnoreDirective
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

        forTestsMatching("compiler/testData/codegen/box/involvesIrInterpreter/*") {
            configureKlibArtifactsHandlersStep {
                useHandlers(::JsKlibInterpreterDumpHandler)
            }
            configureJsArtifactsHandlersStep {
                useHandlers(::JsIrInterpreterDumpHandler)
            }
        }

        forTestsMatching("compiler/testData/codegen/box/properties/backingField/*") {
            defaultDirectives {
                LANGUAGE with "+ExplicitBackingFields"
            }
        }
    }
}

/**
 * Sets up full configuration for the JS tests for the first compilation phase (compilation to KLib).
 * The configuration includes all minimally required services, handlers and defaults for such tests.
 */
fun <FO : ResultingArtifact.FrontendOutput<FO>> TestConfigurationBuilder.commonConfigurationForJsBackendFirstStageTest(
    targetFrontend: FrontendKind<FO>,
    frontendFacade: Constructor<FrontendFacade<FO>>,
    frontendToIrConverter: Constructor<Frontend2BackendConverter<FO, IrBackendInput>>,
    serializerFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>,
    customIgnoreDirective: ValueDirective<TargetBackend>? = null,
) {
    commonConfigurationForJsTest(targetFrontend, frontendFacade, frontendToIrConverter, serializerFacade)
    setupCommonHandlersForJsTest(customIgnoreDirective)
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
fun TestConfigurationBuilder.configureJsBoxHandlers() {
    configureJsArtifactsHandlersStep {
        useHandlers(
            ::NodeJsGeneratorHandler,
            ::JsBoxRunner,
            ::JsAstHandler
        )
    }
}

/**
 * Setups the base test configuration for JVM backend tests, including
 * - global defaults
 * - environment configurators
 * - additional source providers
 */
fun TestConfigurationBuilder.commonServicesConfigurationForJsCodegenTest(
    targetFrontend: FrontendKind<*>,
    customConfigurators: List<Constructor<AbstractEnvironmentConfigurator>>? = null,
) {
    globalDefaults {
        frontend = targetFrontend
        targetPlatform = JsPlatforms.defaultJsPlatform
        dependencyKind = DependencyKind.Binary
    }

    defaultDirectives {
        +DiagnosticsDirectives.REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
    }

    if (customConfigurators == null) {
        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JsEnvironmentConfigurator,
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
 * Setups the pipeline for all JVM backend tests
 *
 * Steps:
 * - FIR frontend
 * - FIR2IR
 * - JVM backend
 *
 * There are handler steps after each facade step.
 */
fun <FO : ResultingArtifact.FrontendOutput<FO>> TestConfigurationBuilder.commonConfigurationForJsTest(
    targetFrontend: FrontendKind<FO>,
    frontendFacade: Constructor<FrontendFacade<FO>>,
    frontendToIrConverter: Constructor<Frontend2BackendConverter<FO, IrBackendInput>>,
    serializerFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>,
) {
    commonServicesConfigurationForJsCodegenTest(targetFrontend)
    facadeStep(frontendFacade)
    classicFrontendHandlersStep()
    firHandlersStep()

    facadeStep(frontendToIrConverter)
    irHandlersStep()

    facadeStep(::JsIrPreSerializationLoweringFacade)
    loweredIrHandlersStep()

    facadeStep(serializerFacade)
    klibArtifactsHandlersStep()
}

fun TestConfigurationBuilder.setupCommonHandlersForJsTest(customIgnoreDirective: ValueDirective<TargetBackend>? = null) {
    configureClassicFrontendHandlersStep(skipMissingStep = true) {
        commonClassicFrontendHandlersForCodegenTest()
        useHandlers(::ClassicDiagnosticsHandler)
    }

    configureFirHandlersStep {
        useHandlers(::FirDiagnosticsHandler)
    }

    configureIrHandlersStep {
        useHandlers(::NoFir2IrCompilationErrorsHandler)
        useHandlers(::IrMangledNameAndSignatureDumpHandler)
    }

    configureLoweredIrHandlersStep {
        useHandlers(::NoFir2IrCompilationErrorsHandler)
    }

    configureKlibArtifactsHandlersStep {
        useHandlers(::KlibBackendDiagnosticsHandler)
    }

    useAfterAnalysisCheckers(
        ::JsFailingTestSuppressor,
        ::BlackBoxCodegenSuppressor.bind(customIgnoreDirective),
    )

    enableMetaInfoHandler()
}
