/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.test.Fir2IrNativeResultsConverter
import org.jetbrains.kotlin.konan.test.NativeKlibSerializerFacade
import org.jetbrains.kotlin.konan.test.blackbox.support.RegularKotlinNativeClassLoader
import org.jetbrains.kotlin.konan.test.blackbox.support.copyNativeHomeProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.util.dumpIr
import org.jetbrains.kotlin.konan.test.blackbox.support.util.dumpMetadata
import org.jetbrains.kotlin.konan.test.converters.NativePreSerializationLoweringFacade
import org.jetbrains.kotlin.konan.test.klib.ManifestWritingTest.Companion.readManifestAndSanitize
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.KlibArtifactHandler
import org.jetbrains.kotlin.test.backend.handlers.NoFir2IrCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.builders.irHandlersStep
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.builders.loweredIrHandlersStep
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.NativeEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.utils.bind
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import java.io.File

/**
 * This test works in the following way:
 *
 * - a klib for darwin target is compiled from sources
 * - its metadata dump, IR dump and manifest are compared against the golden-files
 *
 * The main idea is that the test is launched on all hosts (Linux, Macos, Win) and therefore
 * indirectly asserts that the generated klib is "identical" across these hosts
 */
@Tag("klib")
open class AbstractKlibCrossCompilationIdentityTest : AbstractFirKlibCrossCompilationIdentityTestBase("")
@Tag("klib")
open class AbstractKlibCrossCompilationIdentityWithPreSerializationLoweringTest :
    AbstractFirKlibCrossCompilationIdentityTestBase(".lowered") {

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                LANGUAGE with listOf(
                    "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                    "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
                )
            }
        }
    }
}

open class AbstractFirKlibCrossCompilationIdentityTestBase(val irFileSuffix: String = "") :
    AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.NATIVE) {

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = NativePlatforms.unspecifiedNativePlatform
            artifactKind = ArtifactKind.NoArtifact
            dependencyKind = DependencyKind.Binary
        }

        defaultDirectives {
            // Kotlin/Native does not have "minimal" stdlib(like other backends do), so full stdlib is needed to resolve
            // `Any`, `String`, `println`, etc.
            +ConfigurationDirectives.WITH_STDLIB

            // Some tests require declarations only available in platform libraries.
            +NativeEnvironmentConfigurationDirectives.WITH_PLATFORM_LIBS

            // Fix the Kotlin/Native target used in this test. Ignore the target passed by the CI server.
            NativeEnvironmentConfigurationDirectives.WITH_FIXED_TARGET with KonanTarget.MACOS_ARM64.name

            FirDiagnosticsDirectives.FIR_PARSER with FirParser.LightTree

            DiagnosticsDirectives.DIAGNOSTICS with "-warnings"

            LANGUAGE with listOf(
                "-${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                "-${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
            )
        }

        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)
        enableMetaInfoHandler()
        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::NativeEnvironmentConfigurator,
        )
        useAdditionalService(::LibraryProvider)

        facadeStep(::FirFrontendFacade)
        firHandlersStep {
            useHandlers(::NoFirCompilationErrorsHandler)
            useHandlers(::FirDiagnosticsHandler)
        }
        facadeStep(::Fir2IrNativeResultsConverter)
        irHandlersStep {
            useHandlers(
                ::IrDiagnosticsHandler,
                ::NoFir2IrCompilationErrorsHandler,
            )
        }
        facadeStep(::NativePreSerializationLoweringFacade)
        loweredIrHandlersStep{
            useHandlers(
                ::IrDiagnosticsHandler,
                ::NoFir2IrCompilationErrorsHandler,
            )
        }
        facadeStep(::NativeKlibSerializerFacade)
        klibArtifactsHandlersStep {
            useHandlers(::NativeKlibCrossCompilationIdentityHandler.bind(irFileSuffix))
        }
    }

    override fun runTest(filePath: String) {
        assumeTrue(isCrossDistAvailable(), "Kotlin/Native cross-distribution is not available. Test is muted.")
        super.runTest(filePath)
    }

    companion object {
        private fun isCrossDistAvailable(): Boolean =
            HostManager.hostIsMac || System.getProperty(FULL_CROSS_DIST_ENABLED_PROPERTY)?.toBoolean() ?: false

        // If you rename/change it, adjust native/native.tests/build.gradle.kts as well
        private const val FULL_CROSS_DIST_ENABLED_PROPERTY = "kotlin.native.internal.fullCrossDistEnabled"
    }
}

private class NativeKlibCrossCompilationIdentityHandler(testServices: TestServices, val irFileSuffix: String) : KlibArtifactHandler(testServices) {
    private val metadataDumper = newDumper()
    private val irDumper = newDumper()
    private val manifestDumper = newDumper()

    private val kotlinNativeClassLoader by lazy {
        copyNativeHomeProperty()
        RegularKotlinNativeClassLoader.kotlinNativeClassLoader.classLoader
    }

    override fun processModule(module: TestModule, info: BinaryArtifacts.KLib) {
        val klibFile = info.outputFile

        metadataDumper[module] += klibFile.dumpMetadata(kotlinNativeClassLoader, printSignatures = false, signatureVersion = null)
        irDumper[module] += klibFile.dumpIr(kotlinNativeClassLoader)
        manifestDumper[module] += readManifestAndSanitize(klibFile, singleTargetInManifestToBeReplacedByTheAlias = null)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val (testDataDir: File, baseName: String) = testServices
            .moduleStructure
            .originalTestDataFiles
            .first()
            .let { it.parentFile to it.nameWithoutExtension }

        fun MultiModuleInfoDumper.checkGoldenData(goldenDataFileExtension: String): () -> Unit = {
            assertions.assertEqualsToFile(
                expectedFile = testDataDir.resolve("$baseName.$goldenDataFileExtension"),
                actual = generateResultingDump()
            )
        }

        assertions.assertAll(
            listOf(
                metadataDumper.checkGoldenData(goldenDataFileExtension = "metadata.txt"),
                irDumper.checkGoldenData(goldenDataFileExtension = "ir$irFileSuffix.txt"),
                manifestDumper.checkGoldenData(goldenDataFileExtension = "manifest")
            )
        )
    }

    companion object {
        // a shortcut for more clean code
        private fun newDumper() = MultiModuleInfoDumper("// MODULE: %s")

        // a shortcut for more clean code
        private operator fun MultiModuleInfoDumper.get(module: TestModule): StringBuilder {
            return builderForModule(module)
        }

        // a shortcut for more clean code
        private operator fun StringBuilder.plusAssign(text: String) {
            appendLine(text)
        }
    }
}

