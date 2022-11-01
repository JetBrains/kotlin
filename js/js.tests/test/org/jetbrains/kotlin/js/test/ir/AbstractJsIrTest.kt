/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ir

import org.jetbrains.kotlin.js.test.AbstractJsBlackBoxCodegenTestBase
import org.jetbrains.kotlin.js.test.JsAdditionalSourceProvider
import org.jetbrains.kotlin.js.test.JsSteppingTestAdditionalSourceProvider
import org.jetbrains.kotlin.js.test.converters.JsIrBackendFacade
import org.jetbrains.kotlin.js.test.converters.JsKlibBackendFacade
import org.jetbrains.kotlin.js.test.converters.incremental.RecompileModuleJsIrBackendFacade
import org.jetbrains.kotlin.js.test.handlers.*
import org.jetbrains.kotlin.parsing.parseBoolean
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.*
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.*
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.JsLibraryProvider
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.utils.isDirectiveDefined
import java.lang.Boolean.getBoolean

abstract class AbstractJsIrTest(
    pathToTestDir: String,
    testGroupOutputDirPrefix: String,
) : AbstractJsBlackBoxCodegenTestBase<ClassicFrontendOutputArtifact, IrBackendInput, BinaryArtifacts.KLib>(
    FrontendKinds.ClassicFrontend, TargetBackend.JS_IR, pathToTestDir, testGroupOutputDirPrefix, skipMinification = true
) {
    override val frontendFacade: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade

    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>>
        get() = ::ClassicFrontend2IrConverter

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>
        get() = ::JsKlibBackendFacade

    override val afterBackendFacade: Constructor<AbstractTestFacade<BinaryArtifacts.KLib, BinaryArtifacts.Js>>?
        get() = ::JsIrBackendFacade

    override val recompileFacade: Constructor<AbstractTestFacade<BinaryArtifacts.Js, BinaryArtifacts.Js>>
        get() = { RecompileModuleJsIrBackendFacade(it) }

    private fun getBoolean(s: String, default: Boolean) = System.getProperty(s)?.let { parseBoolean(it) } ?: default

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                val runIc = getBoolean("kotlin.js.ir.icMode")
                if (runIc) +JsEnvironmentConfigurationDirectives.RUN_IC
                if (getBoolean("kotlin.js.ir.klibMainModule")) +JsEnvironmentConfigurationDirectives.KLIB_MAIN_MODULE
                if (getBoolean("kotlin.js.ir.perModule", true)) +JsEnvironmentConfigurationDirectives.PER_MODULE
                if (getBoolean("kotlin.js.ir.dce", true)) +JsEnvironmentConfigurationDirectives.RUN_IR_DCE
                -JsEnvironmentConfigurationDirectives.GENERATE_NODE_JS_RUNNER
            }

            configureJsArtifactsHandlersStep {
                useHandlers(
                    ::JsIrRecompiledArtifactsIdentityHandler,
                )
            }

            forTestsMatching("${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/closure/inlineAnonymousFunctions/*") {
                defaultDirectives {
                    +JsEnvironmentConfigurationDirectives.GENERATE_INLINE_ANONYMOUS_FUNCTIONS
                }
            }
        }
    }
}

open class AbstractIrBoxJsTest : AbstractJsIrTest(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/",
    testGroupOutputDirPrefix = "irBox/"
)

open class AbstractIrJsCodegenBoxTest : AbstractJsIrTest(
    pathToTestDir = "compiler/testData/codegen/box/",
    testGroupOutputDirPrefix = "codegen/irBox/"
)

open class AbstractIrJsCodegenBoxErrorTest : AbstractJsIrTest(
    pathToTestDir = "compiler/testData/codegen/boxError/",
    testGroupOutputDirPrefix = "codegen/irBoxError/"
)

open class AbstractIrJsCodegenInlineTest : AbstractJsIrTest(
    pathToTestDir = "compiler/testData/codegen/boxInline/",
    testGroupOutputDirPrefix = "codegen/irBoxInline/"
)

open class AbstractIrJsTypeScriptExportTest : AbstractJsIrTest(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/typescript-export/",
    testGroupOutputDirPrefix = "typescript-export/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +JsEnvironmentConfigurationDirectives.GENERATE_DTS
                if (getBoolean("kotlin.js.updateReferenceDtsFiles")) +JsEnvironmentConfigurationDirectives.UPDATE_REFERENCE_DTS_FILES
            }

            configureJsArtifactsHandlersStep {
                useHandlers(
                    ::JsDtsHandler
                )
            }
        }
    }
}

open class AbstractJsIrLineNumberTest : AbstractJsIrTest(
    pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/lineNumbers/",
    testGroupOutputDirPrefix = "irLineNumbers/"
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +JsEnvironmentConfigurationDirectives.KJS_WITH_FULL_RUNTIME
                +JsEnvironmentConfigurationDirectives.NO_COMMON_FILES
                -JsEnvironmentConfigurationDirectives.GENERATE_NODE_JS_RUNNER
                JsEnvironmentConfigurationDirectives.DONT_RUN_GENERATED_CODE.with(listOf("JS", "JS_IR", "JS_IR_ES6"))
            }
            configureJsArtifactsHandlersStep {
                useHandlers(::JsLineNumberHandler)
            }
        }
    }
}

open class AbstractIrJsSteppingTest : AbstractJsIrTest(
    pathToTestDir = "compiler/testData/debug/stepping/",
    testGroupOutputDirPrefix = "debug/stepping/"
) {
    override fun TestConfigurationBuilder.configuration() {
        commonConfigurationForJsBlackBoxCodegenTest()
        defaultDirectives {
            +JsEnvironmentConfigurationDirectives.NO_COMMON_FILES
        }
        useAdditionalSourceProviders(::JsSteppingTestAdditionalSourceProvider)
        jsArtifactsHandlersStep {
            useHandlers({ JsDebugRunner(it, localVariables = false) })
        }
    }
}

open class AbstractIrJsLocalVariableTest : AbstractJsIrTest(
    pathToTestDir = "compiler/testData/debug/localVariables/",
    testGroupOutputDirPrefix = "debug/localVariables/"
) {
    override fun TestConfigurationBuilder.configuration() {
        commonConfigurationForJsBlackBoxCodegenTest()
        defaultDirectives {
            +JsEnvironmentConfigurationDirectives.NO_COMMON_FILES
        }
        useAdditionalSourceProviders(::JsSteppingTestAdditionalSourceProvider)
        jsArtifactsHandlersStep {
            useHandlers({ JsDebugRunner(it, localVariables = true) })
        }
    }
}

open class AbstractIrCodegenWasmJsInteropJsTest : AbstractJsIrTest(
    pathToTestDir = "compiler/testData/codegen/wasmJsInterop",
    testGroupOutputDirPrefix = "codegen/wasmJsInteropJs"
)

private class SkipMultiModuleTestsMetaConfigurator(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun shouldSkipTest(): Boolean {
        return testServices.moduleStructure.originalTestDataFiles.any {
            it.isDirectiveDefined("// IGNORE_FIR")
        }
    }
}

open class AbstractFirJsTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JS_IR) {
    private val pathToTestDir = "${JsEnvironmentConfigurator.TEST_DATA_DIR_PATH}/box/"
    private val testGroupOutputDirPrefix = "box/"

    val targetFrontend = FrontendKinds.FIR
    private val skipMinification: Boolean = getBoolean("kotlin.js.skipMinificationTest")

    val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade

    private val frontendToBackendConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrResultsConverter

    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = targetFrontend
            targetPlatform = JsPlatforms.defaultJsPlatform
            dependencyKind = DependencyKind.Binary
        }

        val pathToRootOutputDir = System.getProperty("kotlin.js.test.root.out.dir") ?: error("'kotlin.js.test.root.out.dir' is not set")
        defaultDirectives {
            +DiagnosticsDirectives.REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
            JsEnvironmentConfigurationDirectives.PATH_TO_ROOT_OUTPUT_DIR with pathToRootOutputDir
            JsEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR with pathToTestDir
            JsEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX with testGroupOutputDirPrefix
            +JsEnvironmentConfigurationDirectives.TYPED_ARRAYS
            if (skipMinification) +JsEnvironmentConfigurationDirectives.SKIP_MINIFICATION
            if (getBoolean("kotlin.js.ir.skipRegularMode")) +JsEnvironmentConfigurationDirectives.SKIP_REGULAR_MODE
            +ConfigurationDirectives.WITH_STDLIB
            +LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE
        }

        useMetaTestConfigurators(::SkipMultiModuleTestsMetaConfigurator)

        forTestsNotMatching("compiler/testData/codegen/box/diagnostics/functions/tailRecursion/*") {
            defaultDirectives {
                DiagnosticsDirectives.DIAGNOSTICS with "-warnings"
            }
        }

        forTestsNotMatching("compiler/testData/codegen/boxError/*") {
            enableMetaInfoHandler()
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JsEnvironmentConfigurator,
        )

        useAdditionalSourceProviders(
            ::JsAdditionalSourceProvider,
            ::CoroutineHelpersSourceFilesProvider,
        )

        useAdditionalService(::JsLibraryProvider)

        facadeStep(frontendFacade)

        firHandlersStep {
            useHandlers(
                ::FirDiagnosticsHandler,
                ::FirDumpHandler,
                ::FirCfgDumpHandler,
                ::FirCfgConsistencyHandler,
                ::FirResolvedTypesVerifier,
            )
        }

        // There were some problems not covered by
        // the FIR handlers above
        facadeStep(frontendToBackendConverter)
    }
}
