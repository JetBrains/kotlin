/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.backend.common.IrSpecialAnnotationsProvider
import org.jetbrains.kotlin.backend.common.actualizer.IrExtraActualDeclarationExtractor
import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.Fir2IrVisibilityConverter
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.Fir2KlibMetadataSerializer
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.konan.library.KLIB_INTEROP_IR_PROVIDER_IDENTIFIER
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

    override fun createIrMangler(): KotlinMangler.IrMangler = KonanManglerIr
    override fun createFir2IrExtensions(compilerConfiguration: CompilerConfiguration): Fir2IrExtensions = Fir2IrExtensions.Default
    override fun createFir2IrVisibilityConverter(): Fir2IrVisibilityConverter = Fir2IrVisibilityConverter.Default
    override fun createTypeSystemContextProvider(): (IrBuiltIns) -> IrTypeSystemContext = ::IrTypeSystemContextImpl
    override fun createSpecialAnnotationsProvider(): IrSpecialAnnotationsProvider? = null
    override fun createExtraActualDeclarationExtractorInitializer(): (Fir2IrComponents) -> IrExtraActualDeclarationExtractor? = { null }

    override fun resolveLibraries(module: TestModule, compilerConfiguration: CompilerConfiguration): List<KotlinResolvedLibrary> {
        return resolveLibraries(
            compilerConfiguration,
            getAllNativeDependenciesPaths(module, testServices),
            knownIrProviders = listOf(KLIB_INTEROP_IR_PROVIDER_IDENTIFIER)
        )
    }

    override val klibFactories: KlibMetadataFactories = KlibMetadataFactories(::KonanBuiltIns, DynamicTypeDeserializer)

    override fun createBackendInput(
        compilerConfiguration: CompilerConfiguration,
        diagnosticReporter: BaseDiagnosticsCollector,
        inputArtifact: FirOutputArtifact,
        fir2IrResult: Fir2IrActualizedResult,
        fir2KlibMetadataSerializer: Fir2KlibMetadataSerializer,
    ): IrBackendInput {
        return IrBackendInput.NativeBackendInput(
            fir2IrResult.irModuleFragment,
            fir2IrResult.pluginContext,
            diagnosticReporter = diagnosticReporter,
            descriptorMangler = null,
            irMangler = fir2IrResult.components.irMangler,
            metadataSerializer = fir2KlibMetadataSerializer
        )
    }
}
