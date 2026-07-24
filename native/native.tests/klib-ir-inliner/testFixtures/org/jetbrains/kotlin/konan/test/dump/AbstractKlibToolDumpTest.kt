/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.dump

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.konan.test.Fir2IrCliNativeFacade
import org.jetbrains.kotlin.konan.test.FirCliNativeFacade
import org.jetbrains.kotlin.konan.test.KlibSerializerNativeCliFacade
import org.jetbrains.kotlin.konan.test.NativePreSerializationLoweringCliFacade
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeCoreTest
import org.jetbrains.kotlin.konan.test.services.CInteropTestSkipper
import org.jetbrains.kotlin.konan.test.suppressors.NativeTestsSuppressor
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.test.builders.loweredIrHandlersStep
import org.jetbrains.kotlin.test.configuration.commonIrHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.objcinterop.ObjCInteropFacade
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.junit.jupiter.api.Tag
import java.io.File

/**
 * An abstract test runner for one of the KLIB tool's `dump-XXX` commands.
 */
@Tag("klib")
abstract class AbstractKlibToolDumpTest : AbstractNativeCoreTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)
        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::NativeFirstStageEnvironmentConfigurator,
        )
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = NativePlatforms.unspecifiedNativePlatform
            targetBackend = TargetBackend.NATIVE
            artifactKind = ArtifactKind.NoArtifact
            dependencyKind = DependencyKind.Binary
        }
        defaultDirectives {
            +WITH_STDLIB
            LANGUAGE with listOf(
                "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}",
            )
        }

        useMetaTestConfigurators(::CInteropTestSkipper)
        useFailureSuppressors(::NativeTestsSuppressor)

        facadeStep(::ObjCInteropFacade)

        configureFirParser(FirParser.LightTree)
        facadeStep(::FirCliNativeFacade)
        firHandlersStep {
            useHandlers(::FirDiagnosticsHandler)
        }

        facadeStep(::Fir2IrCliNativeFacade)
        facadeStep(::NativePreSerializationLoweringCliFacade)

        loweredIrHandlersStep {
            commonIrHandlersForCodegenTest()
            useHandlers(::IrDiagnosticsHandler)
        }

        facadeStep(::KlibSerializerNativeCliFacade)

        klibArtifactsHandlersStep {
            useHandlers(getDumpHandlers())
        }
    }

    protected abstract fun getDumpHandlers(): List<Constructor<AbstractKlibToolDumpHandler<*>>>
}

/**
 * Implementations of this interface are supposed to keep various arguments that influence the way how
 * a certain kind of KLIB dump is performed. See inheritors for examples.
 */
interface KlibToolDumpHandlerVariation {
    val dumpFileSuffix: String
}

/**
 * An abstract test handler for one of the KLIB tool's `dump-XXX` commands.
 */
abstract class AbstractKlibToolDumpHandler<V : KlibToolDumpHandlerVariation>(
    testServices: TestServices,
) : BinaryArtifactHandler<BinaryArtifacts.KLib>(
    testServices = testServices,
    artifactKind = ArtifactKinds.KLib,
    failureDisablesNextSteps = false,
    doNotRunIfThereWerePreviousFailures = false,
) {
    protected abstract val variation: V

    private val dumper: MultiModuleInfoDumper = MultiModuleInfoDumper()

    final override fun processModule(module: TestModule, info: BinaryArtifacts.KLib) {
        val dump = makeDump(info.outputFile, module) ?: return
        dumper.builderForModule(module.name).appendLine(dump)
    }

    final override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val originalTestDataFile = testServices.moduleStructure.originalTestDataFiles.first().absolutePath

        val dumpFile = buildString {
            append(originalTestDataFile.removeSuffix(".kt").removeSuffix(".def"))
            append(variation.dumpFileSuffix)
            append(".txt")
        }

        assertions.assertEqualsToFile(
            File(dumpFile),
            dumper.generateResultingDump().trim().ifEmpty { "/* empty dump */" }
        )
    }

    /** Returns the text of the dump, if dump is available for the given type of KLIB. */
    protected abstract fun makeDump(klib: File, module: TestModule): String?
}
