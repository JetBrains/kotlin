/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ir

import org.jetbrains.kotlin.js.test.converters.JsIrBackendFacade
import org.jetbrains.kotlin.js.test.converters.JsKlibBackendFacade
import org.jetbrains.kotlin.js.test.handlers.JsBackendDiagnosticsHandler
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.classic.handlers.DeclarationsDumpHandler
import org.jetbrains.kotlin.test.frontend.classic.handlers.DynamicCallsDumpHandler
import org.jetbrains.kotlin.test.frontend.classic.handlers.OldNewInferenceMetaInfoProcessor
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator

abstract class AbstractDiagnosticsTestWithJsStdLib : AbstractKotlinCompilerTest() {
    protected open fun configureTestBuilder(builder: TestConfigurationBuilder) = builder.apply {
        globalDefaults {
            frontend = FrontendKinds.ClassicFrontend
            targetPlatform = JsPlatforms.defaultJsPlatform
            targetBackend = TargetBackend.JS_IR
            dependencyKind = DependencyKind.Source
        }

        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)

        defaultDirectives {
            +ConfigurationDirectives.WITH_STDLIB
            +JvmEnvironmentConfigurationDirectives.USE_PSI_CLASS_FILES_READING
        }

        useAdditionalService(::LibraryProvider)

        enableMetaInfoHandler()

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JsEnvironmentConfigurator,
        )

        useMetaInfoProcessors(::OldNewInferenceMetaInfoProcessor)
        useAdditionalSourceProviders(
            ::AdditionalDiagnosticsSourceFilesProvider,
            ::CoroutineHelpersSourceFilesProvider,
        )

        classicFrontendStep()
        classicFrontendHandlersStep {
            useHandlers(
                ::DeclarationsDumpHandler,
                ::ClassicDiagnosticsHandler,
                ::DynamicCallsDumpHandler,
            )
        }
    }

    final override fun TestConfigurationBuilder.configuration() {
        configureTestBuilder(this@configuration)
    }
}

abstract class AbstractDiagnosticsTestWithJsStdLibWithBackend : AbstractDiagnosticsTestWithJsStdLib() {
    override fun configureTestBuilder(builder: TestConfigurationBuilder) = builder.apply {
        super.configureTestBuilder(builder)

        psi2IrStep()
        facadeStep { JsKlibBackendFacade(it, true) }

        // TODO: Currently do not run lowerings, because they don't report anything;
        //      see KT-61881, KT-61882
        // facadeStep { JsIrBackendFacade(it, firstTimeCompilation = true) }

        klibArtifactsHandlersStep {
            useHandlers(::JsBackendDiagnosticsHandler)
        }
    }
}
