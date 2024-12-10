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
import org.jetbrains.kotlin.cli.common.messages.getLogger
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.isEmpty
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.backend.DelicateDeclarationStorageApi
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrConfiguration
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.Fir2IrVisibilityConverter
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
import org.jetbrains.kotlin.konan.library.KonanLibraryProperResolver
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.kotlinLibrary
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.library.metadata.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.library.metadata.resolver.impl.libraryResolver
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.NativeForwardDeclarationKind
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.fir.*
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
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
        return resolveKotlinLibrariesWithProperDefaults(module, testServices)
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
                if (!library.isDefault || moduleDescriptor.wasReallyUsed())
                    return@mapNotNull library
            }
            null
        }

        return IrBackendInput.NativeAfterFrontendBackendInput(
            fir2IrResult.irModuleFragment,
            fir2IrResult.pluginContext,
            diagnosticReporter = diagnosticReporter,
            descriptorMangler = null,
            irMangler = fir2IrResult.components.irMangler,
            metadataSerializer = fir2KlibMetadataSerializer,
            usedLibrariesForManifest = usedLibrariesForManifest,
        )
    }

    companion object {
        /**
         * Unlike [org.jetbrains.kotlin.test.frontend.fir.resolveLibraries], which does not distinguish
         * "default" (i.e., [KotlinLibrary.isDefault]) from "non-default" libraries, this function does that kind of distinction:
         * - "default" libraries is anything implicitly added by the Kotlin/Native compiler. For example, stdlib & platform libraries.
         * - "non-default" libraries are the libraries that are explicitly passed to the compiler via compiler CLI arguments.
         *   In tests, these are the libraries that were explicitly specified by `// MODULE` test directives in test data.
         *
         * This distinction is necessary to treat "default" and "non-default" libraries in a different way to compute the
         * list of dependencies to be written in manifest's `depends=` property:
         * - Any "default" library is written to manifest only if it was actually used during the compilation.
         * - Any "non-default" library is written always unconditionally.
         */
        private fun resolveKotlinLibrariesWithProperDefaults(module: TestModule, testServices: TestServices): List<KotlinLibrary> {
            val directDependencies = getTransitivesAndFriendsPaths(module, testServices)

            val nativeEnvironmentConfigurator = testServices.nativeEnvironmentConfigurator

            val nativeTarget = nativeEnvironmentConfigurator.getNativeTarget(module)
            val nativeDistributionKlibPath = nativeEnvironmentConfigurator.distributionKlibPath().absolutePath
            val logger = testServices.compilerConfigurationProvider.getCompilerConfiguration(module).getLogger(treatWarningsAsErrors = true)

            val libraryResolver = KonanLibraryProperResolver(
                directLibs = directDependencies, // Load all direct dependencies as non-default libraries.
                target = nativeTarget,
                distributionKlib = nativeDistributionKlibPath,
                skipCurrentDir = true,
                logger = logger
            ).libraryResolver()

            val resolveResult = libraryResolver.resolveWithDependencies(
                unresolvedLibraries = emptyList(),
                noStdLib = false, // Load stdlib as a default library.
                noDefaultLibs = false, // Load platform libraries for the `nativeTarget` as default libraries.
                noEndorsedLibs = true
            )

            val topologicallyOrderedLibraries = resolveResult.getFullResolvedList(TopologicalLibraryOrder)

            return topologicallyOrderedLibraries.map(KotlinResolvedLibrary::library)
        }

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
