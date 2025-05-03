package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.linkage.partial.createPartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.linkage.partial.partialLinkageConfig
import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializer
import org.jetbrains.kotlin.backend.konan.driver.phases.PsiToIrContext
import org.jetbrains.kotlin.backend.konan.driver.phases.PsiToIrInput
import org.jetbrains.kotlin.backend.konan.driver.phases.PsiToIrOutput
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.interop.IrProviderForCEnumAndCStructStubs
import org.jetbrains.kotlin.backend.konan.ir.konanLibrary
import org.jetbrains.kotlin.backend.konan.serialization.*
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.DescriptorMetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.objcinterop.IrObjCOverridabilityCondition
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
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
import org.jetbrains.kotlin.psi2ir.generators.ModuleGenerator
import org.jetbrains.kotlin.utils.DFS

@OptIn(ObsoleteDescriptorBasedAPI::class)
internal fun PsiToIrContext.runIrLinker(
        input: PsiToIrInput,
): PsiToIrOutput {
    val symbolTable = symbolTable!!
    val (moduleDescriptor, _, isProducingLibrary) = input
    require(!isProducingLibrary)
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
    val functionIrClassFactory: KonanIrAbstractDescriptorBasedFunctionFactory =
            BuiltInFictitiousFunctionIrClassFactory(symbolTable, irBuiltInsOverDescriptors, reflectionTypes)
    irBuiltInsOverDescriptors.functionFactory = functionIrClassFactory
    val symbols = KonanSymbols(
            this,
            generatorContext.irBuiltIns,
            this.config.configuration
    )

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

    val linker = KonanIrLinker(
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
                    messageCollector = messageCollector
            ),
            libraryBeingCached = config.libraryToCache,
            userVisibleIrModulesSupport = config.userVisibleIrModulesSupport,
            externalOverridabilityConditions = listOf(IrObjCOverridabilityCondition)
    )
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

    val irProviders = listOf(linker)
    val moduleGenerator = ModuleGenerator(generatorContext)
    val irModule = IrModuleFragmentImpl(moduleGenerator.context.moduleDescriptor)
    linker.init(irModule)
    ExternalDependenciesGenerator(moduleGenerator.context.symbolTable, irProviders).generateUnboundSymbolsAsDependencies()
    linker.postProcess(inOrAfterLinkageStep = true)
    if (!partialLinkageConfig.isEnabled) {
        messageCollector.checkNoUnboundSymbols(symbolTable, "after generation of IR module ${irModule.name.asString()}")
    }
    irProviderForCEnumsAndCStructs.generateBodies()
    val mainModule = irModule

    linker.postProcess(inOrAfterLinkageStep = true)

    // Enable lazy IR genration for newly-created symbols inside BE
    stubGenerator.unboundSymbolGeneration = true

    messageCollector.checkNoUnboundSymbols(symbolTable, "at the end of IR linkage process")

    val modules = linker.modules
    // IR linker deserializes files in the order they lie on the disk, which might be inconvenient,
    // so to make the pipeline more deterministic, the files are to be sorted.
    // This concerns in the first place global initializers order for the eager initialization strategy,
    // where the files are being initialized in order one by one.
    modules.values.forEach { module -> module.files.sortBy { it.fileEntry.name } }

    // TODO: find out what should be done in the new builtins/symbols about it
    if (stdlibIsBeingCached) {
        (functionIrClassFactory as? BuiltInFictitiousFunctionIrClassFactory)?.buildAllClasses()
    }
    (functionIrClassFactory as? BuiltInFictitiousFunctionIrClassFactory)?.module =
            (modules.values + mainModule).single { it.descriptor == this.stdlibModule }

    mainModule.files.forEach { it.metadata = DescriptorMetadataSource.File(listOf(mainModule.descriptor)) }
    modules.values.forEach { module ->
        module.files.forEach { it.metadata = DescriptorMetadataSource.File(listOf(module.descriptor)) }
    }

    val libraryName = libraryToCache?.klib?.libraryName
    return PsiToIrOutput.ForBackend(
            irModules = modules.filterKeys { it != libraryName },
            irModule = if (libraryName == null) {
                mainModule
            } else {
                modules[libraryName] ?: error("No module for the library being cached: $libraryName")
            },
            irBuiltIns = generatorContext.irBuiltIns,
            symbols = symbols,
            symbolTable = symbolTable,
            irLinker = linker
    )
}

internal class KonanCInteropModuleDeserializerFactory(
        private val cachedLibraries: CachedLibraries,
        private val cenumsProvider: IrProviderForCEnumAndCStructStubs,
        private val stubGenerator: DeclarationStubGenerator,
): CInteropModuleDeserializerFactory {
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