/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.backend.FirMangler
import org.jetbrains.kotlin.fir.backend.native.FirNativeKotlinMangler
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.Fir2KlibMetadataSerializer
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.fir.AbstractFir2IrNonJvmResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.getAllNativeDependenciesPaths
import org.jetbrains.kotlin.test.frontend.fir.resolveLibraries
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class Fir2IrNativeResultsConverter(testServices: TestServices) : AbstractFir2IrNonJvmResultsConverter(testServices) {

    override fun createIrMangler(): KotlinMangler.IrMangler {
        return KonanManglerIr
    }

    override fun createFirMangler(): FirMangler {
        return FirNativeKotlinMangler
    }

    override fun resolveLibraries(module: TestModule, compilerConfiguration: CompilerConfiguration): List<KotlinResolvedLibrary> {
        return resolveLibraries(compilerConfiguration, getAllNativeDependenciesPaths(module, testServices))
    }

    override val klibFactories: KlibMetadataFactories = KlibMetadataFactories(::KonanBuiltIns, DynamicTypeDeserializer)

    override fun createBackendInput(
        compilerConfiguration: CompilerConfiguration,
        diagnosticReporter: BaseDiagnosticsCollector,
        inputArtifact: FirOutputArtifact,
        fir2IrResult: Fir2IrActualizedResult,
        fir2KlibMetadataSerializer: Fir2KlibMetadataSerializer,
    ): IrBackendInput {
        val fir2IrComponents = fir2IrResult.components
        val manglers = fir2IrComponents.manglers
        return IrBackendInput.NativeBackendInput(
            fir2IrResult.irModuleFragment,
            fir2IrResult.pluginContext,
            diagnosticReporter = diagnosticReporter,
            descriptorMangler = null,
            irMangler = manglers.irMangler,
            firMangler = manglers.firMangler,
        )
    }
}
