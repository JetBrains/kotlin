/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.backend.ConstValueProviderImpl
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.FirMangler
import org.jetbrains.kotlin.fir.backend.extractFirDeclarations
import org.jetbrains.kotlin.fir.backend.native.FirNativeKotlinMangler
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.serialization.FirKLibSerializerExtension
import org.jetbrains.kotlin.fir.serialization.serializeSingleFirFile
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.KlibMetadataHeaderFlags
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.frontend.fir.AbstractFir2IrNonJvmResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.getAllNativeDependenciesPaths
import org.jetbrains.kotlin.test.frontend.fir.resolveLibraries
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class Fir2IrNativeResultsConverter(testServices: TestServices) : AbstractFir2IrNonJvmResultsConverter(testServices) {

    override fun createDescriptorMangler(): KotlinMangler.DescriptorMangler {
        return KonanManglerDesc
    }

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
        firFilesAndComponentsBySourceFile: Map<KtSourceFile, Pair<FirFile, Fir2IrComponents>>,
        sourceFiles: List<KtSourceFile>,
    ): IrBackendInput {
        val manglers = fir2IrResult.components.manglers
        return IrBackendInput.NativeBackendInput(
            fir2IrResult.irModuleFragment,
            fir2IrResult.pluginContext,
            diagnosticReporter = diagnosticReporter,
            hasErrors = inputArtifact.hasErrors,
            descriptorMangler = manglers.descriptorMangler,
            irMangler = manglers.irMangler,
            firMangler = manglers.firMangler,
        ) {
            val allowErrors = inputArtifact.partsForDependsOnModules.any { CodegenTestDirectives.IGNORE_ERRORS in it.module.directives }

            val actualizedFirDeclarations = fir2IrResult.irActualizedResult?.actualizedExpectDeclarations?.extractFirDeclarations()
            val metadataVersion = compilerConfiguration[CommonConfigurationKeys.METADATA_VERSION] as? KlibMetadataVersion
                ?: KlibMetadataVersion.INSTANCE

            val fragments = mutableMapOf<String, MutableList<ByteArray>>()

            firFilesAndComponentsBySourceFile.values.forEach { (firFile, fir2IrComponents) ->
                val fragmentProto = serializeSingleFirFile(
                    firFile,
                    fir2IrComponents.session,
                    fir2IrComponents.scopeSession,
                    actualizedFirDeclarations,
                    FirKLibSerializerExtension(
                        fir2IrComponents.session,
                        fir2IrComponents.firProvider,
                        metadataVersion,
                        ConstValueProviderImpl(fir2IrComponents),
                        allowErrorTypes = allowErrors,
                        exportKDoc = false,
                        additionalMetadataProvider = fir2IrComponents.annotationsFromPluginRegistrar.createAdditionalMetadataProvider()
                    ),
                    compilerConfiguration.languageVersionSettings,
                )
                fragments.getOrPut(firFile.packageFqName.asString()) { mutableListOf() } += fragmentProto.toByteArray()
            }

            val header = KlibMetadataProtoBuf.Header.newBuilder()
            header.moduleName = fir2IrResult.irModuleFragment.name.asString()
            if (compilerConfiguration.languageVersionSettings.isPreRelease())
                header.flags = KlibMetadataHeaderFlags.PRE_RELEASE

            val fragmentNames = mutableListOf<String>()
            val fragmentParts = mutableListOf<List<ByteArray>>()

            for ((fqName, fragment) in fragments.entries.sortedBy { it.key }) {
                fragmentNames += fqName
                fragmentParts += fragment
                header.addPackageFragmentName(fqName)
            }

            SerializedMetadata(
                module = header.build().toByteArray(),
                fragments = fragmentParts,
                fragmentNames = fragmentNames
            )
        }
    }
}
