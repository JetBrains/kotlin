/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.backend.common.IrSpecialAnnotationsProvider
import org.jetbrains.kotlin.backend.common.actualizer.IrExtraActualDeclarationExtractor
import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.backend.konan.serialization.loadNativeKlibsInTestPipeline
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.isEmpty
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.Fir2KlibMetadataSerializer
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.konan.library.isFromKotlinNativeDistribution
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.kotlinLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.NativeForwardDeclarationKind
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.fir.AbstractFir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.getTransitivesAndFriendsPaths
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.nativeEnvironmentConfigurator

class Fir2IrNativeResultsConverter(testServices: TestServices) : AbstractFir2IrResultsConverter(testServices) {

    override fun createIrMangler(): KotlinMangler.IrMangler = KonanManglerIr
    override fun createFir2IrExtensions(compilerConfiguration: CompilerConfiguration): Fir2IrExtensions = Fir2IrExtensions.Default
    override fun createFir2IrVisibilityConverter(): Fir2IrVisibilityConverter = Fir2IrVisibilityConverter.Default
    override fun createTypeSystemContextProvider(): (IrBuiltIns) -> IrTypeSystemContext = ::IrTypeSystemContextImpl
    override fun createSpecialAnnotationsProvider(): IrSpecialAnnotationsProvider? = null
    override fun createExtraActualDeclarationExtractorInitializer(): (Fir2IrComponents) -> List<IrExtraActualDeclarationExtractor> =
        { emptyList() }

    override fun resolveLibraries(module: TestModule, compilerConfiguration: CompilerConfiguration): List<KotlinLibrary> {
        val nativeEnvironmentConfigurator = testServices.nativeEnvironmentConfigurator

        val runtimeDependencies = nativeEnvironmentConfigurator.getRuntimePathsForModule(module)
        val transitiveDependencies = getTransitivesAndFriendsPaths(module, testServices)

        return loadNativeKlibsInTestPipeline(
            configuration = compilerConfiguration,
            libraryPaths = runtimeDependencies + transitiveDependencies,
            nativeTarget = nativeEnvironmentConfigurator.getNativeTarget(module)
        ).all
    }

    override val klibFactories: KlibMetadataFactories = KlibMetadataFactories(::KonanBuiltIns, DynamicTypeDeserializer)

    override fun createFir2IrConfiguration(
        compilerConfiguration: CompilerConfiguration,
        diagnosticReporter: BaseDiagnosticsCollector,
    ): Fir2IrConfiguration {
        return Fir2IrConfiguration.forKlibCompilation(compilerConfiguration, diagnosticReporter)
    }

    override fun createBackendInput(
        module: TestModule,
        compilerConfiguration: CompilerConfiguration,
        diagnosticReporter: BaseDiagnosticsCollector,
        inputArtifact: FirOutputArtifact,
        fir2IrResult: Fir2IrActualizedResult,
        fir2KlibMetadataSerializer: Fir2KlibMetadataSerializer,
    ): IrBackendInput {
        val usedPackages: List<FqName> = fir2IrResult.components.computeUsedPackages()

        fun ModuleDescriptorImpl.wasReallyUsed(): Boolean {
            return usedPackages.any { usedPackage -> !packageFragmentProviderForModuleContentWithoutDependencies.isEmpty(usedPackage) }
        }

        val usedLibrariesForManifest = fir2IrResult.irModuleFragment.descriptor.allDependencyModules.mapNotNull { moduleDescriptor ->
            if (moduleDescriptor is ModuleDescriptorImpl) {
                val library = moduleDescriptor.kotlinLibrary
                if (!library.isFromKotlinNativeDistribution || moduleDescriptor.wasReallyUsed())
                    return@mapNotNull library
            }
            null
        }

        return IrBackendInput.NativeAfterFrontendBackendInput(
            fir2IrResult.irModuleFragment,
            fir2IrResult.irBuiltIns,
            diagnosticReporter = diagnosticReporter,
            descriptorMangler = null,
            irMangler = fir2IrResult.components.irMangler,
            metadataSerializer = fir2KlibMetadataSerializer,
            usedLibrariesForManifest = usedLibrariesForManifest,
        )
    }

    companion object {
        /**
         * Collects [FqName]s of all used packages from [Fir2IrComponents].
         *
         * This information is further used to filter out "default" dependency libraries which were not used
         * during the compilation, and thus should not be recorded in KLIB manifest's `depends=` property.
         */
        @OptIn(DelicateDeclarationStorageApi::class, UnsafeDuringIrConstructionAPI::class)
        private fun Fir2IrComponents.computeUsedPackages(): List<FqName> = buildSet {
            fun addExternalPackage(it: IrSymbol) {
                // FIXME(KT-64742): Fir2IrDeclarationStorage caches may contain unbound IR symbols, so we filter them out.
                val p = it.takeIf { it.isBound }?.owner as? IrDeclaration ?: return
                val fragment = (p.getPackageFragment() as? IrExternalPackageFragment) ?: return
                add(fragment.packageFqName)
            }
            declarationStorage.forEachCachedDeclarationSymbol(::addExternalPackage)
            classifierStorage.forEachCachedDeclarationSymbol(::addExternalPackage)

            // These packages exist in all platform libraries, but can contain only synthetic declarations.
            // These declarations are not really located in klib, so we don't need to depend on klib to use them.
            removeAll(NativeForwardDeclarationKind.entries.map { it.packageFqName }.toSet())
        }.toList()
    }
}
