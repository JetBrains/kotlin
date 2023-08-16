/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.irtext

import org.jetbrains.kotlin.cli.js.klib.generateIrForKlibSerialization
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.konan.blackboxtest.support.CastCompatibleKotlinNativeClassLoader
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*


class ClassicFrontend2NativeIrConverter(
    testServices: TestServices,
) : Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>(
    testServices,
    FrontendKinds.ClassicFrontend,
    BackendKinds.IrBackend
) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::LibraryProvider))

    override fun transform(module: TestModule, inputArtifact: ClassicFrontendOutputArtifact): IrBackendInput {
        return when (module.targetBackend) {
            TargetBackend.NATIVE -> transformToNativeIr(module, inputArtifact)
            else -> testServices.assertions.fail { "Target backend ${module.targetBackend} is not supported for transformation into IR" }
        }
    }

    private fun transformToNativeIr(module: TestModule, inputArtifact: ClassicFrontendOutputArtifact): IrBackendInput {
        val (psiFiles, analysisResult, project, _) = inputArtifact

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val verifySignatures = true

        val sourceFiles = psiFiles.values.toList()
        val icData = configuration.incrementalDataProvider?.getSerializedData(sourceFiles) ?: emptyList()
        val expectDescriptorToSymbol = mutableMapOf<DeclarationDescriptor, IrSymbol>()

        val (moduleFragment, pluginContext) = generateIrForKlibSerialization(
            project,
            sourceFiles,
            configuration,
            analysisResult,
            listOf(),
            icData,
            expectDescriptorToSymbol,
            IrFactoryImpl,
            verifySignatures
        ) {
            testServices.libraryProvider.getDescriptorByCompiledLibrary(it)
        }

        // KT-61248: TODO Replace reflection for direct import of mangler object, when it would extracted out of `backend.native` module
        val konanManglerIrClass = Class.forName(
            "org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr",
            true,
            CastCompatibleKotlinNativeClassLoader.kotlinNativeClassLoader.classLoader
        )
        val konanIrMangler = konanManglerIrClass.kotlin.objectInstance as KotlinMangler.IrMangler

        return IrBackendInput.NativeBackendInput(
            moduleFragment,
            dependentIrModuleFragments = emptyList(),
            pluginContext,
            diagnosticReporter = DiagnosticReporterFactory.createReporter(),
            descriptorMangler = (pluginContext.symbolTable as SymbolTable).signaturer.mangler,
            irMangler = konanIrMangler,
            firMangler = null,
        )
    }
}