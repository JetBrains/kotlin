package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.backend.common.IrBuiltInsForLinker
import org.jetbrains.kotlin.backend.common.IrModuleDependencies
import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.linkage.partial.partialLinkageConfig
import org.jetbrains.kotlin.backend.common.phaser.KotlinBackendIrHolder
import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
import org.jetbrains.kotlin.backend.common.serialization.kotlinLibrary
import org.jetbrains.kotlin.backend.konan.driver.NativeBackendPhaseContext
import org.jetbrains.kotlin.backend.konan.ir.BackendNativeSymbols
import org.jetbrains.kotlin.backend.konan.ir.konanLibrary
import org.jetbrains.kotlin.backend.konan.serialization.*
import org.jetbrains.kotlin.backend.konan.util.sortDeclarationsInFunctionInterfaceFile
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.IrBasedFunctionFactory.Companion.isFunctionInterfaceFile
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.objcinterop.IrObjCOverridabilityCondition
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isHeader
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.metadata.CurrentKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.SyntheticModulesOrigin
import org.jetbrains.kotlin.library.metadata.impl.KlibResolvedModuleDescriptorsFactoryImpl
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.library.metadata.klibModuleOrigin
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.resolve.CommonCompilerDeserializationConfiguration
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import java.nio.file.Path

internal interface LinkKlibsContext : NativeBackendPhaseContext {
    val symbolTable: SymbolTable?

    @OptIn(K1Deprecation::class)
    val builtIns: KonanBuiltIns

    @OptIn(K1Deprecation::class)
    val stdlibModule: ModuleDescriptor
        get() = this.builtIns.any.module
}

data class LinkKlibsInput(
        val moduleDescriptor: ModuleDescriptor,
)

internal class LinkKlibsOutput(
        val irModules: Map<Path, IrModuleFragment>,
        val irModule: IrModuleFragment,
        val irBuiltIns: IrBuiltIns,
        val symbols: BackendNativeSymbols,
        val symbolTable: ReferenceSymbolTable,
        val irLinker: KonanIrLinker,
) : KotlinBackendIrHolder {

    override val kotlinIr: IrElement
        get() = irModule
}


internal fun LinkKlibsContext.linkKlibs(
        input: LinkKlibsInput
): LinkKlibsOutput {
    val symbolTable = symbolTable!!
    val moduleDescriptor = input.moduleDescriptor
    val libraryToCache = config.libraryToCache?.klib

    val stdlibIsCached = stdlibModule.konanLibrary?.let { config.cachedLibraries.isLibraryCached(it) } == true
    val stdlibIsBeingCached = libraryToCache != null && libraryToCache == stdlibModule.konanLibrary
    require(!(stdlibIsCached && stdlibIsBeingCached)) { "The cache for stdlib is already built" }

    val irLinker = createIrLinker(moduleDescriptor)
    deserializeDependencies(moduleDescriptor, irLinker)
    ensureCStructsAndEnumsAreLoadedForCaching(irLinker, libraryToCache)

    // Get the list of all libraries registered with the linker.
    val originalModuleDependencies = IrModuleDependencies(irLinker.allModuleFragments)

    @OptIn(InternalSymbolFinderAPI::class)
    val irBuiltIns = IrBuiltInsForLinker(irLinker, config.configuration.languageVersionSettings)
    val symbols = BackendNativeSymbols(this, irBuiltIns, config.configuration)

    ExternalDependenciesGenerator(irLinker.symbolTable, listOf(irLinker)).generateUnboundSymbolsAsDependencies()
    irLinker.postProcess(irBuiltIns, inOrAfterLinkageStep = true)

    generateImplForCStructsAndEnums(irLinker, irBuiltIns, symbols)

    config.configuration.checkNoUnboundSymbols(symbolTable, "at the end of IR linkage process")

    // IR linker deserializes files in the order they lie on the disk, which might be inconvenient,
    // so to make the pipeline more deterministic, the files are to be sorted.
    // This concerns in the first place global initializers order for the eager initialization strategy,
    // where the files are being initialized in order one by one.
    originalModuleDependencies.sortFilesAndDeclarationsToKeepPipelineDeterministic()

    if (stdlibIsBeingCached) {
        val maxArity = 255 // See [BuiltInFictitiousFunctionClassFactory].
        (0..maxArity).forEach { arity ->
            irBuiltIns.functionN(arity)
            irBuiltIns.suspendFunctionN(arity)
            irBuiltIns.kFunctionN(arity)
            irBuiltIns.kSuspendFunctionN(arity)
        }
    }

    val irModulesForLinkKlibsOutput: Map<Path, IrModuleFragment> = originalModuleDependencies.allDependencies
            .filter { it.name != KlibResolvedModuleDescriptorsFactoryImpl.FORWARD_DECLARATIONS_MODULE_NAME && it.descriptor !== moduleDescriptor }
            .associateBy { it.kotlinLibrary!!.path }

    return if (libraryToCache == null) {
        val mainModule = IrModuleFragmentImpl(moduleDescriptor)
        LinkKlibsOutput(
                irModules = irModulesForLinkKlibsOutput,
                irModule = mainModule,
                irBuiltIns = irBuiltIns,
                symbols = symbols,
                symbolTable = symbolTable,
                irLinker = irLinker
        )
    } else {
        val libraryPath: Path = libraryToCache.path
        val libraryModule = irModulesForLinkKlibsOutput[libraryPath] ?: error("No module for the library being cached: $libraryPath")
        LinkKlibsOutput(
                irModules = irModulesForLinkKlibsOutput.filterKeys { it != libraryPath },
                irModule = libraryModule,
                irBuiltIns = irBuiltIns,
                symbols = symbols,
                symbolTable = symbolTable,
                irLinker = irLinker
        )
    }
}

private fun LinkKlibsContext.createIrLinker(moduleDescriptor: ModuleDescriptor): KonanIrLinker {
    val symbolTable = symbolTable!!

    val deserializationConfiguration = CommonCompilerDeserializationConfiguration(config.configuration.languageVersionSettings)
    val cInteropModuleDeserializerFactory = KonanCInteropModuleDeserializerFactory(
            deserializationConfiguration = deserializationConfiguration,
            cachedLibraries = config.cachedLibraries,
    )

    val friendModuleUniqueNames = config.loadedKlibs.friends.map { it.uniqueName }
    val includedModuleUniqueNames = config.loadedKlibs.included.map { it.uniqueName }

    val friendModulesMap: Map<String, List<String>> =
            (listOf(moduleDescriptor.name.asStringStripSpecialMarkers()) + includedModuleUniqueNames).associateWith { friendModuleUniqueNames }

    val irDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
            config.configuration.diagnosticsCollector,
            config.languageVersionSettings,
    )

    return KonanIrLinker(
            currentModule = moduleDescriptor,
            configuration = config.configuration,
            symbolTable = symbolTable,
            friendModules = friendModulesMap,
            cInteropModuleDeserializerFactory = cInteropModuleDeserializerFactory,
            partialLinkageConfig = config.configuration.partialLinkageConfig,
            irDiagnosticReporter = irDiagnosticReporter,
            libraryBeingCached = config.libraryToCache,
            externalOverridabilityConditions = listOf(IrObjCOverridabilityCondition),
    )
}

private fun LinkKlibsContext.deserializeDependencies(moduleDescriptor: ModuleDescriptor, linker: KonanIrLinker) {
    // The set of libraries to deserialize and their order come from `config.librariesWithDependencies()`.
    // The module descriptors are needed only as a per-library input for the IR linker.
    val moduleByLibrary = mutableMapOf<KotlinLibrary, ModuleDescriptor>()
    for (module in moduleDescriptor.allDependencyModules) {
        when (val origin = module.klibModuleOrigin) {
            is DeserializedKlibModuleOrigin -> moduleByLibrary[origin.library] = module
            // The forward-declarations module is synthesized by the frontend and is not backed by any klib,
            // so the library iteration below cannot discover it.
            SyntheticModulesOrigin ->
                linker.createAndRegisterModuleDeserializer(module, kotlinLibrary = null, { DeserializationStrategy.ALL })
            CurrentKlibModuleOrigin -> error("Unexpected kind of module dependency $module")
        }
    }

    val libraryToCache = config.libraryToCache?.klib
    // Only the IR of these libraries is deserialized in full.
    val fullyDeserializedLibraries = config.exportedAndIncludedLibraries + listOfNotNull(libraryToCache)

    for (library in config.librariesWithDependencies().reversed()) {
        val module = moduleByLibrary.getValue(library)
        val isFullyCachedLibrary = config.cachedLibraries.isLibraryCached(library) && library != libraryToCache
        when {
            isFullyCachedLibrary && library.isHeader -> linker.deserializeHeadersWithInlineBodies(module, library)
            isFullyCachedLibrary -> linker.deserializeOnlyHeaderModule(module, library)
            library in fullyDeserializedLibraries -> linker.deserializeFullModule(module, library)
            // Every other library has its declarations deserialized lazily, as they are referenced, except for
            // the explicitly exported ones (e.g. top-level property initializers), which are deserialized eagerly.
            // TODO: consider skip deserializing explicitly exported declarations for libraries.
            // Now it's not valid because of all dependencies that must be computed.
            else -> linker.deserializeExplicitlyExportedModule(module, library)
        }
    }
}

private fun ensureCStructsAndEnumsAreLoadedForCaching(linker: KonanIrLinker, libraryToCache: KotlinLibrary?) {
    // Unlike other declarations from C-interop Klibs, we generate synthetic implementation for C structs and enums, which is then
    // being lowered, and eventually ends up being compiled into assembly code, much like regular Kotlin classes.
    // Normally it's only for the classes actually used from the lib/app being compiled, but if instead we're building a cache for
    // a C-interop library, we want to load, process and cache everything. The consumer of the cached library will then have all the
    // resulting assembly code for the C structs and enums already available, without a need for any special processing.
    if (libraryToCache?.isCInteropLibrary() == true) {
        // The library being cached is always registered by `deserializeDependencies`.
        val interopModuleDeserializer = linker.allModuleDeserializers.single { it.moduleFragment.kotlinLibrary == libraryToCache }
        (interopModuleDeserializer as? KonanInteropModuleDeserializer)?.deserializeAllCStructsAndEnums()
    }
}

private fun generateImplForCStructsAndEnums(linker: KonanIrLinker, builtIns: IrBuiltIns, symbols: BackendNativeSymbols) {
    val implGen = IrImplementationGeneratorForCStructsAndEnums(builtIns, symbols)
    for (deserializer in linker.allModuleDeserializers) {
        val module = deserializer.moduleFragment
        if (module.kotlinLibrary?.isCInteropLibrary() == true) {
            for (file in module.files) {
                for (declaration in file.declarations) {
                    if (declaration is IrClass) {
                        implGen.generateImplIfCStructOrEnum(declaration)
                    }
                }
            }
        }
    }
}

private fun IrModuleDependencies.sortFilesAndDeclarationsToKeepPipelineDeterministic() {
    allDependencies.forEach { module ->
        module.files.sortBy { file -> file.fileEntry.name }

        // Sort also synthetic `*Function` classes is special function interface files inside the standard library.
        // They might be generated and added to files on demand and in the different order (based on the order or
        // the deserialization queue).
        if (module.kotlinLibrary?.isNativeStdlib == true) {
            module.files.forEach { file ->
                if (file.isFunctionInterfaceFile) sortDeclarationsInFunctionInterfaceFile(file)
            }
        }
    }
}

internal class KonanCInteropModuleDeserializerFactory(
        private val cachedLibraries: CachedLibraries,
        private val deserializationConfiguration: DeserializationConfiguration,
) : CInteropModuleDeserializerFactory<KonanInteropModuleDeserializer> {
    override fun createIrModuleDeserializer(
            moduleFragment: IrModuleFragment,
            klib: KotlinLibrary,
            linker: KonanIrLinker,
    ) = KonanInteropModuleDeserializer(
            deserializationConfiguration,
            moduleFragment,
            klib,
            cachedLibraries.isLibraryCached(klib),
            linker,
    )
}
