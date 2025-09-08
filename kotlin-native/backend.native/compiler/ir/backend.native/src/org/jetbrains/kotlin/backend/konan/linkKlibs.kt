package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.linkage.partial.createPartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.linkage.partial.partialLinkageConfig
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideChecker
import org.jetbrains.kotlin.backend.common.phaser.KotlinBackendIrHolder
import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializer
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.interop.IrProviderForCEnumAndCStructStubs
import org.jetbrains.kotlin.backend.konan.ir.konanLibrary
import org.jetbrains.kotlin.backend.konan.serialization.CInteropModuleDeserializerFactory
import org.jetbrains.kotlin.backend.konan.serialization.KonanInteropModuleDeserializer
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrLinker
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.backend.konan.serialization.isFromCInteropLibrary
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.DescriptorMetadataSource
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.objcinterop.IrObjCOverridabilityCondition
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isHeader
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.KlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.impl.isForwardDeclarationModule
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorImpl
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.utils.DFS

internal interface LinkKlibsContext : PhaseContext {
    val symbolTable: SymbolTable?

    val reflectionTypes: KonanReflectionTypes

    val builtIns: KonanBuiltIns

    val bindingContext: BindingContext

    val stdlibModule: ModuleDescriptor
        get() = this.builtIns.any.module
}

data class LinkKlibsInput(
        val moduleDescriptor: ModuleDescriptor,
        val environment: KotlinCoreEnvironment,
)

internal class LinkKlibsOutput(
        val irModules: Map<String, IrModuleFragment>,
        val irModule: IrModuleFragment,
        val irBuiltIns: IrBuiltIns,
        val symbols: KonanSymbols,
        val symbolTable: ReferenceSymbolTable,
        val irLinker: KonanIrLinker,
) : KotlinBackendIrHolder {

    override val kotlinIr: IrElement
        get() = irModule
}


@OptIn(ObsoleteDescriptorBasedAPI::class)
internal fun LinkKlibsContext.linkKlibs(
        input: LinkKlibsInput
): LinkKlibsOutput {
    val symbolTable = symbolTable!!
    val (moduleDescriptor, environment) = input
    // Translate AST to high level IR.
    val messageCollector = config.configuration.messageCollector
    val partialLinkageConfig = config.configuration.partialLinkageConfig

    val translator = Psi2IrTranslator(
            config.configuration.languageVersionSettings,
            Psi2IrConfiguration(ignoreErrors = false, partialLinkageConfig.isEnabled),
            messageCollector::checkNoUnboundSymbols
    )
    val generatorContext = translator.createGeneratorContext(moduleDescriptor, bindingContext, symbolTable)

    val forwardDeclarationsModuleDescriptor = moduleDescriptor.allDependencyModules.firstOrNull { it.isForwardDeclarationModule }

    val libraryToCache = config.libraryToCache
    val libraryToCacheModule = libraryToCache?.klib?.let {
        moduleDescriptor.allDependencyModules.single { module -> module.konanLibrary == it }
    }

    val stdlibIsCached = stdlibModule.konanLibrary?.let { config.cachedLibraries.isLibraryCached(it) } == true
    val stdlibIsBeingCached = libraryToCacheModule == stdlibModule
    require(!(stdlibIsCached && stdlibIsBeingCached)) { "The cache for stdlib is already built" }

    val stubGenerator = DeclarationStubGeneratorImpl(
            moduleDescriptor, symbolTable,
            generatorContext.irBuiltIns,
            DescriptorByIdSignatureFinderImpl(moduleDescriptor, KonanManglerDesc),
            KonanStubGeneratorExtensions
    )
    val irBuiltInsOverDescriptors = generatorContext.irBuiltIns as IrBuiltInsOverDescriptors
    val functionIrClassFactory = BuiltInFictitiousFunctionIrClassFactory(symbolTable, irBuiltInsOverDescriptors, reflectionTypes)
    irBuiltInsOverDescriptors.functionFactory = functionIrClassFactory
    val symbols = KonanSymbols(
            this,
            generatorContext.irBuiltIns,
            this.config.configuration
    )

    val irDeserializer = run {
        val exportedDependencies = (moduleDescriptor.getExportedDependencies(config) + libraryToCacheModule?.let { listOf(it) }.orEmpty()).distinct()
        val irProviderForCEnumsAndCStructs =
                IrProviderForCEnumAndCStructStubs(generatorContext, symbols)
        val cInteropModuleDeserializerFactory = KonanCInteropModuleDeserializerFactory(
                cachedLibraries = config.cachedLibraries,
                cenumsProvider = irProviderForCEnumsAndCStructs,
                stubGenerator = stubGenerator,
        )

        val friendModules = config.resolvedLibraries.getFullList()
                .filter { it.libraryFile in config.friendModuleFiles }
                .map { it.uniqueName }

        val friendModulesMap = (
                listOf(moduleDescriptor.name.asStringStripSpecialMarkers()) +
                        config.resolve.includedLibraries.map { it.uniqueName }
                ).associateWith { friendModules }

        KonanIrLinker(
                currentModule = moduleDescriptor,
                messageCollector = messageCollector,
                builtIns = generatorContext.irBuiltIns,
                symbolTable = symbolTable,
                friendModules = friendModulesMap,
                forwardModuleDescriptor = forwardDeclarationsModuleDescriptor,
                stubGenerator = stubGenerator,
                cInteropModuleDeserializerFactory = cInteropModuleDeserializerFactory,
                exportedDependencies = exportedDependencies,
                partialLinkageSupport = createPartialLinkageSupportForLinker(
                        partialLinkageConfig = partialLinkageConfig,
                        builtIns = generatorContext.irBuiltIns,
                        messageCollector = messageCollector,
                ),
                libraryBeingCached = config.libraryToCache,
                userVisibleIrModulesSupport = config.userVisibleIrModulesSupport,
                externalOverridabilityConditions = listOf(IrObjCOverridabilityCondition)
        ).also { linker ->

            // context.config.librariesWithDependencies could change at each iteration.
            var dependenciesCount = 0
            while (true) {
                // context.config.librariesWithDependencies could change at each iteration.
                val libsWithDeps = config.librariesWithDependencies().toSet()
                val dependencies = moduleDescriptor.allDependencyModules.filter {
                    libsWithDeps.contains(it.konanLibrary)
                }

                fun sortDependencies(dependencies: List<ModuleDescriptor>): Collection<ModuleDescriptor> {
                    return DFS.topologicalOrder(dependencies) {
                        it.allDependencyModules
                    }.reversed()
                }

                for (dependency in sortDependencies(dependencies).filter { it != moduleDescriptor }) {
                    val kotlinLibrary = (dependency.getCapability(KlibModuleOrigin.CAPABILITY) as? DeserializedKlibModuleOrigin)?.library
                    val isFullyCachedLibrary = kotlinLibrary != null &&
                            config.cachedLibraries.isLibraryCached(kotlinLibrary) && kotlinLibrary != config.libraryToCache?.klib
                    if (isFullyCachedLibrary && kotlinLibrary.isHeader)
                        linker.deserializeHeadersWithInlineBodies(dependency, kotlinLibrary)
                    else if (isFullyCachedLibrary)
                        linker.deserializeOnlyHeaderModule(dependency, kotlinLibrary)
                    else
                        linker.deserializeIrModuleHeader(dependency, kotlinLibrary, dependency.name.asString())
                }
                if (dependencies.size == dependenciesCount) break
                dependenciesCount = dependencies.size
            }

            // We need to run `buildAllEnumsAndStructsFrom` before `generateModuleFragment` because it adds references to symbolTable
            // that should be bound.
            if (libraryToCacheModule?.isFromCInteropLibrary() == true)
                irProviderForCEnumsAndCStructs.referenceAllEnumsAndStructsFrom(libraryToCacheModule)

            translator.addPostprocessingStep {
                irProviderForCEnumsAndCStructs.generateBodies()
            }
        }
    }
    val mainModule = translator.generateModuleFragment(generatorContext, environment.getSourceFiles(), listOf(irDeserializer))

    irDeserializer.postProcess(inOrAfterLinkageStep = true)

    // Enable lazy IR genration for newly-created symbols inside BE
    stubGenerator.unboundSymbolGeneration = true

    messageCollector.checkNoUnboundSymbols(symbolTable, "at the end of IR linkage process")

    val modules = irDeserializer.modules

    if (config.configuration.getBoolean(KonanConfigKeys.FAKE_OVERRIDE_VALIDATOR)) {
        val fakeOverrideChecker = FakeOverrideChecker(KonanManglerIr, KonanManglerDesc)
        modules.values.forEach { fakeOverrideChecker.check(it) }
    }
    // IR linker deserializes files in the order they lie on the disk, which might be inconvenient,
    // so to make the pipeline more deterministic, the files are to be sorted.
    // This concerns in the first place global initializers order for the eager initialization strategy,
    // where the files are being initialized in order one by one.
    modules.values.forEach { module -> module.files.sortBy { it.fileEntry.name } }

    // TODO: find out what should be done in the new builtins/symbols about it
    if (stdlibIsBeingCached) {
        functionIrClassFactory.buildAllClasses()
    }
    functionIrClassFactory.module =
            (modules.values + mainModule).single { it.descriptor == this.stdlibModule }

    mainModule.files.forEach { it.metadata = DescriptorMetadataSource.File(listOf(mainModule.descriptor)) }
    modules.values.forEach { module ->
        module.files.forEach { it.metadata = DescriptorMetadataSource.File(listOf(module.descriptor)) }
    }

    return if (libraryToCache == null) {
        LinkKlibsOutput(modules, mainModule, generatorContext.irBuiltIns, symbols, symbolTable, irDeserializer)
    } else {
        val libraryName = libraryToCache.klib.libraryName
        val libraryModule = modules[libraryName] ?: error("No module for the library being cached: $libraryName")
        LinkKlibsOutput(
                irModules = modules.filterKeys { it != libraryName },
                irModule = libraryModule,
                irBuiltIns = generatorContext.irBuiltIns,
                symbols = symbols,
                symbolTable = symbolTable,
                irLinker = irDeserializer
        )
    }
}

internal class KonanCInteropModuleDeserializerFactory(
        private val cachedLibraries: CachedLibraries,
        private val cenumsProvider: IrProviderForCEnumAndCStructStubs,
        private val stubGenerator: DeclarationStubGenerator,
) : CInteropModuleDeserializerFactory {
    override fun createIrModuleDeserializer(
            moduleDescriptor: ModuleDescriptor,
            klib: KotlinLibrary,
            moduleDependencies: Collection<IrModuleDeserializer>,
    ): IrModuleDeserializer = KonanInteropModuleDeserializer(
            moduleDescriptor,
            klib,
            moduleDependencies,
            cachedLibraries.isLibraryCached(klib),
            cenumsProvider,
            stubGenerator,
    )
}