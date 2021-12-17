/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import org.jetbrains.kotlin.js.test.handlers.*
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.irHandlersStep
import org.jetbrains.kotlin.test.builders.jsArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.JsLibraryProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import java.lang.Boolean.getBoolean

abstract class AbstractJsBlackBoxCodegenTestBase<R : ResultingArtifact.FrontendOutput<R>, I : ResultingArtifact.BackendInput<I>, A : ResultingArtifact.Binary<A>>(
    val targetFrontend: FrontendKind<R>,
    targetBackend: TargetBackend,
    private val pathToTestDir: String,
    private val testGroupOutputDirPrefix: String,
    private val skipMinification: Boolean = getBoolean("kotlin.js.skipMinificationTest"),
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    abstract val frontendFacade: Constructor<FrontendFacade<R>>
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<R, I>>
    abstract val backendFacade: Constructor<BackendFacade<I, A>>
    abstract val afterBackendFacade: Constructor<AbstractTestFacade<A, BinaryArtifacts.Js>>?
    abstract val recompileFacade: Constructor<AbstractTestFacade<BinaryArtifacts.Js, BinaryArtifacts.Js>>

    open fun TestConfigurationBuilder.configureFrontendHandlers() {}

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
            +JsEnvironmentConfigurationDirectives.GENERATE_NODE_JS_RUNNER
            if (skipMinification) +JsEnvironmentConfigurationDirectives.SKIP_MINIFICATION
            if (getBoolean("kotlin.js.ir.skipRegularMode")) +JsEnvironmentConfigurationDirectives.SKIP_REGULAR_MODE
        }

        forTestsNotMatching("compiler/testData/codegen/box/diagnostics/functions/tailRecursion/*") {
            defaultDirectives {
                DIAGNOSTICS with "-warnings"
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

        useAfterAnalysisCheckers(
            ::JsFailingTestSuppressor,
            ::BlackBoxCodegenSuppressor,
            ::JsArtifactsDumpHandler
        )

        facadeStep(frontendFacade)
        configureFrontendHandlers()

        facadeStep(frontendToBackendConverter)
        irHandlersStep()

        facadeStep(backendFacade)
        afterBackendFacade?.let { facadeStep(it) }
        facadeStep(recompileFacade)
        jsArtifactsHandlersStep {
            useHandlers(
                ::NodeJsGeneratorHandler,
                ::JsBoxRunner,
                ::JsMinifierRunner,
                ::JsAstHandler
            )
        }
    }
}
