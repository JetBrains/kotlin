/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testNew

import org.jetbrains.kotlin.js.testNew.handlers.*
import org.jetbrains.kotlin.js.testNew.utils.JsIncrementalEnvironmentConfigurator
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.classicFrontendHandlersStep
import org.jetbrains.kotlin.test.builders.irHandlersStep
import org.jetbrains.kotlin.test.builders.jsArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticsHandler
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.runners.codegen.commonClassicFrontendHandlersForCodegenTest
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import java.lang.Boolean.getBoolean

abstract class AbstractJsBlackBoxCodegenTestBase<R : ResultingArtifact.FrontendOutput<R>, I : ResultingArtifact.BackendInput<I>>(
    val targetFrontend: FrontendKind<R>,
    targetBackend: TargetBackend,
    private val pathToTestDir: String,
    private val testGroupOutputDirPrefix: String,
    private val skipMinification: Boolean = getBoolean("kotlin.js.skipMinificationTest"),
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    abstract val frontendFacade: Constructor<FrontendFacade<R>>
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<R, I>>
    abstract val backendFacade: Constructor<BackendFacade<I, BinaryArtifacts.Js>>
    abstract val recompileFacade: Constructor<AbstractTestFacade<BinaryArtifacts.Js, BinaryArtifacts.Js>>

    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = targetFrontend
            targetPlatform = JsPlatforms.defaultJsPlatform
            dependencyKind = DependencyKind.Binary
        }

        val pathToRootOutputDir = System.getProperty("kotlin.js.test.root.out.dir") ?: error("'kotlin.js.test.root.out.dir' is not set")
        defaultDirectives {
            JsEnvironmentConfigurationDirectives.PATH_TO_ROOT_OUTPUT_DIR with pathToRootOutputDir
            JsEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR with pathToTestDir
            JsEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX with testGroupOutputDirPrefix
            +JsEnvironmentConfigurationDirectives.TYPED_ARRAYS
            +JsEnvironmentConfigurationDirectives.GENERATE_NODE_JS_RUNNER
            if (skipMinification) +JsEnvironmentConfigurationDirectives.SKIP_MINIFICATION
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JsEnvironmentConfigurator,
            ::JsIncrementalEnvironmentConfigurator
        )

        useAdditionalSourceProviders(
            ::JsAdditionalSourceProvider,
            ::CoroutineHelpersSourceFilesProvider,
        )

        useAfterAnalysisCheckers(
            ::JsFailingTestSuppressor,
            ::BlackBoxCodegenSuppressor,
        )

        facadeStep(frontendFacade)
        classicFrontendHandlersStep {
            commonClassicFrontendHandlersForCodegenTest()
            useHandlers(::ClassicDiagnosticsHandler)
        }

        facadeStep(frontendToBackendConverter)
        irHandlersStep()

        facadeStep(backendFacade)
        facadeStep(recompileFacade)
        jsArtifactsHandlersStep {
            useHandlers(
                ::JsBoxRunner,
                ::NodeJsGeneratorHandler,
                ::JsMinifierRunner,
                ::JsArtifactsDumpHandler,
                ::JsRecompiledArtifactsIdentityHandler
            )
        }
    }
}
