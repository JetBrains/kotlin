package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.backend.common.IrBuiltInsForLinker
import org.jetbrains.kotlin.backend.common.IrModuleDependencies
import org.jetbrains.kotlin.backend.common.LoadedNativeKlibs
import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.phaser.KotlinBackendIrHolder
import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
import org.jetbrains.kotlin.backend.common.serialization.kotlinLibrary
import org.jetbrains.kotlin.backend.konan.driver.NativeBackendPhaseContext
import org.jetbrains.kotlin.backend.konan.ir.BackendNativeSymbols
import org.jetbrains.kotlin.backend.konan.library.KlibDAG
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
import org.jetbrains.kotlin.library.metadata.*
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.resolve.CommonCompilerDeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.utils.newHashMapWithExpectedSize

internal interface LinkKlibsContext : NativeBackendPhaseContext {
    val symbolTable: SymbolTable?

    @OptIn(K1Deprecation::class)
    val builtIns: KonanBuiltIns
}

data class LinkKlibsInput(
        val moduleDescriptor: ModuleDescriptor,
)

/**
 * @property irModules The list of IR module fragments in the reverse topological order. This list only contains IR modules
 *   that are treated as "useful", i.e. each of them either was explicitly passed via CLI argument to the compiler or was loaded
 *   from the Kotlin/Native distribution implicitly and has at least one declaration that has been loaded/linked from it.
 */
internal class LinkKlibsOutput(
        val irModules: List<IrModuleFragment>,
        val irBuiltIns: IrBuiltIns,
        val symbols: BackendNativeSymbols,
        val symbolTable: ReferenceSymbolTable,
        val irLinker: KonanIrLinker,
        val irModulesToCompile: List<IrModuleFragment>,
) : KotlinBackendIrHolder {

    override val kotlinIr: IrElement
        get() = irModulesToCompile.first()
}


internal fun LinkKlibsContext.linkKlibs(
        input: LinkKlibsInput
): LinkKlibsOutput {
    val symbolTable = symbolTable!!
    val moduleDescriptor = input.moduleDescriptor

    val stdlibToCache = config.librariesToCache.keys.firstOrNull { it.isNativeStdlib }
    require(stdlibToCache == null || !config.cachedLibraries.isLibraryCached(stdlibToCache)) { "The cache for stdlib is already built" }

    val irLinker = createIrLinker(moduleDescriptor)

    scheduleDependenciesForDeserialization(
            loadedKlibs = config.loadedKlibs,
            moduleDescriptors = moduleDescriptor.allDependencyModules,
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
    val sortedUsefulModuleDependencies = usefulModuleDependencies.reverseTopoSortUsingKlibDAG(config.klibDag)

    // IR linker deserializes files in the order they lie on the disk, which might be inconvenient,
    // so to make the pipeline more deterministic, the files are to be sorted.
    // This concerns in the first place global initializers order for the eager initialization strategy,
    // where the files are being initialized in order one by one.
    sortedUsefulModuleDependencies.sortFilesAndDeclarationsToKeepPipelineDeterministic()

    if (stdlibToCache != null) {
        val maxArity = 255 // See [BuiltInFictitiousFunctionClassFactory].
        (0..maxArity).forEach { arity ->
            irBuiltIns.functionN(arity)
            irBuiltIns.suspendFunctionN(arity)
            irBuiltIns.kFunctionN(arity)
            irBuiltIns.kSuspendFunctionN(arity)
        }
    }

    val irModulesForLinkKlibsOutput: List<IrModuleFragment> = sortedUsefulModuleDependencies.allDependencies
            .filter { it.name != FORWARD_DECLARATIONS_MODULE_NAME }

    return if (config.librariesToCache.isEmpty()) {
        val mainModule = IrModuleFragmentImpl(moduleDescriptor)
        LinkKlibsOutput(
                irModules = irModulesForLinkKlibsOutput,
                irBuiltIns = irBuiltIns,
                symbols = symbols,
                symbolTable = symbolTable,
                irLinker = irLinker,
                irModulesToCompile = listOf(mainModule),
        )
    } else {
        val [libraryModules, otherModules] = irModulesForLinkKlibsOutput.partition { it.kotlinLibrary in config.librariesToCache }
        check(libraryModules.size == config.librariesToCache.size) { "Missing modules for libraries being cached" }
        LinkKlibsOutput(
                irModules = otherModules, // TODO(KT-88867): Keep the full list of all IR module fragments in `irModules`
                irBuiltIns = irBuiltIns,
                symbols = symbols,
                symbolTable = symbolTable,
                irLinker = irLinker,
                irModulesToCompile = libraryModules,
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
            configuration = config.configuration,
            symbolTable = symbolTable,
            friendModules = friendModulesMap,
            cInteropModuleDeserializerFactory = cInteropModuleDeserializerFactory,
            irDiagnosticReporter = irDiagnosticReporter,
            librariesBeingCached = config.librariesToCache,
            externalOverridabilityConditions = listOf(IrObjCOverridabilityCondition),
            partialLinkageIssueSink = config.partialLinkageIssues,
    )
}

private fun LinkKlibsContext.scheduleDependenciesForDeserialization(
        loadedKlibs: LoadedNativeKlibs,
        moduleDescriptors: List<ModuleDescriptor>,
        linker: KonanIrLinker,
) {
    val exportedDependencies: Set<KotlinLibrary> = (config.loadedKlibs.exported + config.loadedKlibs.included + config.librariesToCache.keys).toSet()

    val libraryToModuleDescriptor: Map<KotlinLibrary, ModuleDescriptor> = moduleDescriptors
            .filterNot {
                // The forward declarations module and the current (source-based) modules do not have
                // associated KLIBs. Also, the current module is not supposed to ever participate in the deserialization process.
                it.isForwardDeclarationModule || it.klibModuleOrigin is CurrentKlibModuleOrigin
            }
            .associateBy { (it.klibModuleOrigin as DeserializedKlibModuleOrigin).library }

    // First, schedule all the dependencies for the deserialization using the CLI-order.
    for (library in loadedKlibs.all) {
        val dependencyModuleDescriptor: ModuleDescriptor = libraryToModuleDescriptor[library]
                ?: error("Could not resolve module descriptor for $library")

        val isFullyCachedLibrary = config.cachedLibraries.isLibraryCached(library) && library !in config.librariesToCache

        val deserializationStrategy = when {
            isFullyCachedLibrary && library.isHeader -> DeserializationStrategy.WITH_INLINE_BODIES
            isFullyCachedLibrary -> DeserializationStrategy.ONLY_DECLARATION_HEADERS
            library in exportedDependencies -> DeserializationStrategy.ALL
            else -> DeserializationStrategy.EXPLICITLY_EXPORTED
        }

        linker.createAndRegisterModuleDeserializer(dependencyModuleDescriptor, library, deserializationStrategy)
    }

    // Make sure the library-to-be-cached is also scheduled for deserialization.
    // Note: a batched build never contains C-interop libraries, so it has nothing to load here.
    ensureCStructsAndEnumsAreLoadedForCaching(linker, config.librariesToCache.keys.singleOrNull())

    // Finally, add the forward declarations module (if there is any). It does not have any associated KLIB.
    moduleDescriptors.firstOrNull { it.isForwardDeclarationModule }?.let {
        linker.createAndRegisterModuleDeserializer(it, kotlinLibrary = null, DeserializationStrategy.ALL)
    }
}

private fun ensureCStructsAndEnumsAreLoadedForCaching(linker: KonanIrLinker, libraryToCache: KotlinLibrary?) {
    // Unlike other declarations from C-interop Klibs, we generate synthetic implementation for C structs and enums, which is then
    // being lowered, and eventually ends up being compiled into assembly code, much like regular Kotlin classes.
    // Normally it's only for the classes actually used from the lib/app being compiled, but if instead we're building a cache for
    // a C-interop library, we want to load, process and cache everything. The consumer of the cached library will then have all the
    // resulting assembly code for the C structs and enums already available, without a need for any special processing.
    if (libraryToCache?.isCInteropLibrary() != true) return
    val interopModuleDeserializer = linker.allModuleDeserializers.single { it.moduleFragment.kotlinLibrary == libraryToCache }
    (interopModuleDeserializer as? KonanInteropModuleDeserializer)?.deserializeAllCStructsAndEnums()
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

/**
 * In general, the IR linker is capable to track the dependencies between modules through the deserialized IR. But for a cached library
 * only the header is deserialized, so some of the dependencies might be missed, and the order computed by the IR linker might be wrong.
 * To be safe and consistent, we have to sort the modules according to [KlibDAG], which is computed without the involvement of IR linker.
 */
private fun IrModuleDependencies.reverseTopoSortUsingKlibDAG(klibDag: KlibDAG): IrModuleDependencies {
    val libraryToModule = newHashMapWithExpectedSize<KotlinLibrary, IrModuleFragment>(allDependencies.size)
    val modulesWithoutLibraryInCliOrder = ArrayList<IrModuleFragment>(2)

    for (module in allDependencies) {
        when (val library = module.kotlinLibrary) {
            null -> modulesWithoutLibraryInCliOrder += module
            else -> libraryToModule[library] = module
        }
    }

    val sortedModules = ArrayList<IrModuleFragment>(allDependencies.size)

    klibDag.librariesReverseTopoSorted.mapNotNullTo(sortedModules, libraryToModule::get)
    sortedModules += modulesWithoutLibraryInCliOrder

    return copy(allDependencies = sortedModules)
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
