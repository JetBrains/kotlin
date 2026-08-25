package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.backend.common.IrBuiltInsForLinker
import org.jetbrains.kotlin.backend.common.IrModuleDependencies
import org.jetbrains.kotlin.backend.common.LoadedNativeKlibs
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
import org.jetbrains.kotlin.konan.library.isImplicitlyLoadedFromKotlinNativeDistribution
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isHeader
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.metadata.impl.KlibResolvedModuleDescriptorsFactoryImpl
import org.jetbrains.kotlin.library.metadata.CurrentKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.impl.isForwardDeclarationModule
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.library.metadata.klibModuleOrigin
import org.jetbrains.kotlin.library.metadata.kotlinLibrary
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

    val libraryToCache = config.libraryToCache
    val libraryToCacheModule = libraryToCache?.klib?.let {
        moduleDescriptor.allDependencyModules.single { module -> module.konanLibrary == it }
    }

    val stdlibIsCached = stdlibModule.konanLibrary?.let { config.cachedLibraries.isLibraryCached(it) } == true
    val stdlibIsBeingCached = libraryToCacheModule == stdlibModule
    require(!(stdlibIsCached && stdlibIsBeingCached)) { "The cache for stdlib is already built" }

    val forwardDeclarationsModule = moduleDescriptor.allDependencyModules.firstOrNull { it.isForwardDeclarationModule }

    val irLinker = createIrLinker(moduleDescriptor, forwardDeclarationsModule, libraryToCacheModule)

    scheduleDependenciesForDeserialization(
            loadedKlibs = config.loadedKlibs,
            moduleDescriptor = moduleDescriptor,
            forwardDeclarationsModule = forwardDeclarationsModule,
            libraryToCacheModule = libraryToCacheModule,
            linker = irLinker
    )

    // Get the list of all dependencies (including potentially unused platform libraries).
    val originalModuleDependencies = IrModuleDependencies(irLinker.allModuleFragments)

    @OptIn(InternalSymbolFinderAPI::class)
    val irBuiltIns = IrBuiltInsForLinker(irLinker, config.configuration.languageVersionSettings)
    val symbols = BackendNativeSymbols(this, irBuiltIns, config.configuration)

    ExternalDependenciesGenerator(irLinker.symbolTable, listOf(irLinker)).generateUnboundSymbolsAsDependencies()
    irLinker.postProcess(irBuiltIns, inOrAfterLinkageStep = true)

    // Drop those platform library modules which remain unused (untouched) during the deserialization.
    val usefulModuleDependencies = originalModuleDependencies.filterOutUnusedPlatformLibraryModules(irLinker)

    // Generate stubs only for useful modules.
    generateImplForCStructsAndEnums(usefulModuleDependencies, irBuiltIns, symbols)

    config.configuration.checkNoUnboundSymbols(symbolTable, "at the end of IR linkage process")

    // Also, sort modules in RTO according to their actual dependencies DAG.
    val sortedUsefulModuleDependencies = usefulModuleDependencies.reverseTopoOrder(irLinker)

    // IR linker deserializes files in the order they lie on the disk, which might be inconvenient,
    // so to make the pipeline more deterministic, the files are to be sorted.
    // This concerns in the first place global initializers order for the eager initialization strategy,
    // where the files are being initialized in order one by one.
    sortedUsefulModuleDependencies.sortFilesAndDeclarationsToKeepPipelineDeterministic()

    if (stdlibIsBeingCached) {
        val maxArity = 255 // See [BuiltInFictitiousFunctionClassFactory].
        (0..maxArity).forEach { arity ->
            irBuiltIns.functionN(arity)
            irBuiltIns.suspendFunctionN(arity)
            irBuiltIns.kFunctionN(arity)
            irBuiltIns.kSuspendFunctionN(arity)
        }
    }

    val irModulesForLinkKlibsOutput: Map<Path, IrModuleFragment> = sortedUsefulModuleDependencies.allDependencies
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
        val libraryPath: Path = libraryToCache.klib.path
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

private fun LinkKlibsContext.createIrLinker(
        moduleDescriptor: ModuleDescriptor,
        forwardDeclarationsModule: ModuleDescriptor?,
        libraryToCacheModule: ModuleDescriptor?,
): KonanIrLinker {
    val symbolTable = symbolTable!!
    val exportedDependencies = (moduleDescriptor.getExportedDependencies(config) + libraryToCacheModule?.let { listOf(it) }.orEmpty()).distinct()

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
            forwardModuleDescriptor = forwardDeclarationsModule,
            cInteropModuleDeserializerFactory = cInteropModuleDeserializerFactory,
            exportedDependencies = exportedDependencies,
            partialLinkageConfig = config.configuration.partialLinkageConfig,
            irDiagnosticReporter = irDiagnosticReporter,
            libraryBeingCached = config.libraryToCache,
            externalOverridabilityConditions = listOf(IrObjCOverridabilityCondition),
    )
}

private fun LinkKlibsContext.scheduleDependenciesForDeserialization(
        loadedKlibs: LoadedNativeKlibs,
        moduleDescriptor: ModuleDescriptor,
        forwardDeclarationsModule: ModuleDescriptor?,
        libraryToCacheModule: ModuleDescriptor?,
        linker: KonanIrLinker,
) {
    val libraryToModuleDescriptor: Map<KotlinLibrary, ModuleDescriptor> = moduleDescriptor.allDependencyModules
            .filterNot {
                // The forward declarations module and the current (source-based) modules do not have
                // associated KLIBs. Also, the current module is not supposed to ever participate in the deserialization process.
                it == forwardDeclarationsModule || it.klibModuleOrigin is CurrentKlibModuleOrigin
            }
            .associateBy { it.kotlinLibrary }

    // First, schedule all the dependencies for the deserialization using the CLI-order.
    for (library in loadedKlibs.all) {
        val moduleDescriptor: ModuleDescriptor = libraryToModuleDescriptor[library]
                ?: error("Could not resolve module descriptor for $library")

        val isFullyCachedLibrary = config.cachedLibraries.isLibraryCached(library) && library != config.libraryToCache?.klib

        when {
            isFullyCachedLibrary && library.isHeader -> linker.deserializeHeadersWithInlineBodies(moduleDescriptor, library)
            isFullyCachedLibrary -> linker.deserializeOnlyHeaderModule(moduleDescriptor, library)
            else -> linker.deserializeIrModuleHeader(moduleDescriptor, library, moduleDescriptor.name.asString())
        }
    }

    // Make sure the library-to-be-cached is also scheduled for deserialization.
    ensureCStructsAndEnumsAreLoadedForCaching(linker, libraryToCacheModule)

    // Finally, add the forward declarations module (if there is any). It does not have any associated KLIB.
    forwardDeclarationsModule?.let {
        linker.deserializeIrModuleHeader(it, kotlinLibrary = null, it.name.asString())
    }
}

private fun ensureCStructsAndEnumsAreLoadedForCaching(linker: KonanIrLinker, libraryToCacheModule: ModuleDescriptor?) {
    // Unlike other declarations from C-interop Klibs, we generate synthetic implementation for C structs and enums, which is then
    // being lowered, and eventually ends up being compiled into assembly code, much like regular Kotlin classes.
    // Normally it's only for the classes actually used from the lib/app being compiled, but if instead we're building a cache for
    // a C-interop library, we want to load, process and cache everything. The consumer of the cached library will then have all the
    // resulting assembly code for the C structs and enums already available, without a need for any special processing.
    if (libraryToCacheModule?.kotlinLibrary?.isCInteropLibrary() == true) {
        val interopModuleDeserializer = linker.getOrCreateDeserializerForModule(libraryToCacheModule, libraryToCacheModule.kotlinLibrary,
                { DeserializationStrategy.ONLY_REFERENCED }, libraryToCacheModule.name.asString())
        (interopModuleDeserializer as? KonanInteropModuleDeserializer)?.deserializeAllCStructsAndEnums()
    }
}

private fun generateImplForCStructsAndEnums(
        usefulModuleDependencies: IrModuleDependencies,
        builtIns: IrBuiltIns,
        symbols: BackendNativeSymbols,
) {
    val implGen = IrImplementationGeneratorForCStructsAndEnums(builtIns, symbols)
    for (module in usefulModuleDependencies.allDependencies) {
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

private fun IrModuleDependencies.filterOutUnusedPlatformLibraryModules(linker: KonanIrLinker): IrModuleDependencies {
    val omittedModuleFragments: Set<IrModuleFragment> = linker.allModuleDeserializers.asSequence()
            .filter { it is CInteropModuleDeserializer && !it.hasAnyLinkedIrDeclarations() }
            .map { it.moduleFragment }
            .filter { it.kotlinLibrary?.isImplicitlyLoadedFromKotlinNativeDistribution == true }
            .toSet()

    return copy(allDependencies = allDependencies - omittedModuleFragments)
}

private fun IrModuleDependencies.reverseTopoOrder(linker: KonanIrLinker): IrModuleDependencies {
    return linker.moduleDependencyTracker.reverseTopoOrder(this)
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
