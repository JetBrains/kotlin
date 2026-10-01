/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.runners

import org.jetbrains.kotlin.konan.test.*
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.createSimpleTestRunSettings
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CustomKlibs
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.*
import org.jetbrains.kotlin.test.backend.ir.IrBackendFacade
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.configuration.additionalK2ConfigurationForIrTextTest
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.configuration.commonIrHandlersForCodegenTest
import org.jetbrains.kotlin.test.configuration.setupDefaultDirectivesForIrTextTest
import org.jetbrains.kotlin.test.configuration.setupIrTextDumpHandlers
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.KlibAbiConsistencyDirectives.CHECK_SAME_ABI_AFTER_INLINING
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE
import org.jetbrains.kotlin.test.directives.NativeEnvironmentConfigurationDirectives.WITH_PLATFORM_LIBS
import org.jetbrains.kotlin.test.frontend.fir.FirFailingTestSuppressor
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractFirPsiDiagnosticTest
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.runners.UnspecifiedTargetBackend
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirLightTreeBlackBoxCodegenTest
import org.jetbrains.kotlin.test.services.PhasedPipelineChecker
import org.jetbrains.kotlin.test.services.TestPhase
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeSecondStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.utils.bind
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.RegisterExtension

open class AbstractAtomicfuJvmFirLightTreeTest : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureJvmArtifactsHandlersStep { useHandlers(::SMAPDumpHandler) }
        builder.configureForKotlinxAtomicfu()
    }
}

abstract class AbstractAtomicfuFirCheckerTest : AbstractFirPsiDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            configureForKotlinxAtomicfu()
            useFailureSuppressors(::FirFailingTestSuppressor)
        }
    }
}

@OptIn(UnspecifiedTargetBackend::class)
abstract class AbstractAtomicfuNativeIrTextTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.NATIVE) {
    private val frontend: FrontendKind<*> = FrontendKinds.FIR
    private val targetPlatform = NativePlatforms.unspecifiedNativePlatform
    private val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>> = ::FirCliNativeFacade
    private val converter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>> = ::Fir2IrCliNativeFacade
    private val preSerializerFacade: Constructor<IrPreSerializationLoweringFacade<IrBackendInput>> =
        ::NativePreSerializationLoweringCliFacade
    private val klibAbiDumpBeforeInliningSavingHandler: Constructor<AbstractKlibAbiDumpBeforeInliningSavingHandler> =
        ::FirNativeKlibAbiDumpBeforeInliningSavingHandler
    private val serializerFacade: Constructor<IrBackendFacade<BinaryArtifacts.KLib>> = ::KlibSerializerNativeCliFacade
    private val parser: FirParser = FirParser.LightTree

    private lateinit var extensionContext: ExtensionContext

    @RegisterExtension
    val extensionContextCaptor = BeforeEachCallback { context ->
        extensionContext = context
    }

    override fun configure(builder: TestConfigurationBuilder) {
        with(builder) {
            // AbstractNonJvmIrTextTest
            globalDefaults {
                frontend = this@AbstractAtomicfuNativeIrTextTest.frontend
                targetPlatform = this@AbstractAtomicfuNativeIrTextTest.targetPlatform
                targetBackend = this@AbstractAtomicfuNativeIrTextTest.targetBackend
                artifactKind = ArtifactKind.NoArtifact
                dependencyKind = DependencyKind.Binary
            }

            // <AbstractNativeIrTextTestBase.TestConfigurationBuilder.applyConfigurators()>
            useConfigurators(
                ::CommonEnvironmentConfigurator,
                ::NativeFirstStageEnvironmentConfigurator,
                ::NativeSecondStageEnvironmentConfigurator,
            )
            // </AbstractNativeIrTextTestBase.TestConfigurationBuilder.applyConfigurators()>

            useAdditionalSourceProviders(
                ::AdditionalDiagnosticsSourceFilesProvider,
                ::CoroutineHelpersSourceFilesProvider,
            )

            facadeStep(frontendFacade)
            firHandlersStep {
                commonFirHandlersForCodegenTest()
                useHandlers(
                    ::FirDiagnosticsHandler
                )
            }

            setupDefaultDirectivesForIrTextTest()
            useFailureSuppressors(
                ::BlackBoxCodegenSuppressor,
                ::PhasedPipelineChecker.bind(TestPhase.CODEGEN)
            )
            enableMetaInfoHandler()
            facadeStep(converter)
            irHandlersStep {
                commonIrHandlersForCodegenTest()
                setupIrTextDumpHandlers()
                useHandlers(klibAbiDumpBeforeInliningSavingHandler)
            }
            facadeStep(preSerializerFacade)

            loweredIrHandlersStep {
                useHandlers(::IrDiagnosticsHandler, { SerializedIrDumpHandler(it, isAfterDeserialization = false) })
            }

            facadeStep(serializerFacade)

            // AbstractNativeIrTextTestBase stuff
            additionalK2ConfigurationForIrTextTest(parser)
            with(builder) {
                defaultDirectives {
                    // Kotlin/Native does not have "minimal" stdlib(like other backends do), so full stdlib is needed to resolve
                    // `Any`, `String`, `println`, etc.
                    +ConfigurationDirectives.WITH_STDLIB
                    +CHECK_SAME_ABI_AFTER_INLINING
                    +ALLOW_KOTLIN_PACKAGE
                }
            }

            // Atomicfu stuff
            useConfigurators(
                ::AtomicfuExtensionRegistrarConfigurator,
                ::CommonEnvironmentConfigurator,
                ::NativeFirstStageEnvironmentConfigurator,
            )
            defaultDirectives {
                +WITH_PLATFORM_LIBS
            }

            useCustomRuntimeClasspathProviders(
                ::AtomicfuNativeRuntimeClasspathProvider.bind(
                    extensionContext.createSimpleTestRunSettings().get<CustomKlibs>()
                )
            )
        }
    }
}
