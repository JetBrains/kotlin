package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.linkage.partial.createPartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideChecker
import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializer
import org.jetbrains.kotlin.backend.konan.driver.phases.PsiToIrContext
import org.jetbrains.kotlin.backend.konan.driver.phases.PsiToIrInput
import org.jetbrains.kotlin.backend.konan.driver.phases.PsiToIrOutput
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.interop.IrProviderForCEnumAndCStructStubs
import org.jetbrains.kotlin.backend.konan.ir.konanLibrary
import org.jetbrains.kotlin.backend.konan.serialization.CInteropModuleDeserializerFactory
import org.jetbrains.kotlin.backend.konan.serialization.KonanInteropModuleDeserializer
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrLinker
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.backend.konan.serialization.isFromCInteropLibrary
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.DescriptorMetadataSource
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.linkage.partial.partialLinkageConfig
import org.jetbrains.kotlin.ir.objcinterop.IrObjCOverridabilityCondition
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.StubGeneratorExtensions
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isHeader
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.KlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.impl.isForwardDeclarationModule
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorImpl
import org.jetbrains.kotlin.utils.DFS

object KonanStubGeneratorExtensions : StubGeneratorExtensions() {
    override fun isPropertyWithPlatformField(descriptor: PropertyDescriptor): Boolean {
        return super.isPropertyWithPlatformField(descriptor) || descriptor.isLateInit
    }
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
internal fun PsiToIrContext.psiToIr(
        input: PsiToIrInput,
        useLinkerWhenProducingLibrary: Boolean
): PsiToIrOutput {
    val symbolTable = symbolTable!!
    val (moduleDescriptor, environment, isProducingLibrary) = input
    // Translate AST to high level IR.
    val messageCollector = config.configuration.messageCollector

    val partialLinkageConfig = config.configuration.partialLinkageConfig

    val translator = Psi2IrTranslator(
            config.configuration.languageVersionSettings,
            Psi2IrConfiguration(ignoreErrors = false, partialLinkageConfig.isEnabled),
            messageCollector::checkNoUnboundSymbols
    )
    val generatorContext = translator.createGeneratorContext(moduleDescriptor, bindingContext, symbolTable)

    val pluginExtensions = IrGenerationExtension.getInstances(config.project)

    val forwardDeclarationsModuleDescriptor = moduleDescriptor.allDependencyModules.firstOrNull { it.isForwardDeclarationModule }

    val libraryToCache = config.libraryToCache
    val libraryToCacheModule = libraryToCache?.klib?.let {
        moduleDescriptor.allDependencyModules.single { module -> module.konanLibrary == it }
    }

    val stdlibIsCached = stdlibModule.konanLibrary?.let { config.cachedLibraries.isLibraryCached(it) } == true
    val stdlibIsBeingCached = libraryToCacheModule == stdlibModule
    require(!(stdlibIsCached && stdlibIsBeingCached)) { "The cache for stdlib is already built" }
    val kFunctionImplIsBeingCached = stdlibIsBeingCached && libraryToCache?.strategy.containsKFunctionImpl
    val shouldUseLazyFunctionClasses = (stdlibIsCached || stdlibIsBeingCached) && !kFunctionImplIsBeingCached

    val stubGenerator = DeclarationStubGeneratorImpl(
            moduleDescriptor, symbolTable,
            generatorContext.irBuiltIns,
            DescriptorByIdSignatureFinderImpl(moduleDescriptor, KonanManglerDesc),
            KonanStubGeneratorExtensions
    )
    val irBuiltInsOverDescriptors = generatorContext.irBuiltIns as IrBuiltInsOverDescriptors
    val functionIrClassFactory: KonanIrAbstractDescriptorBasedFunctionFactory =
            if (shouldUseLazyFunctionClasses && config.lazyIrForCaches)
                LazyIrFunctionFactory(symbolTable, stubGenerator, irBuiltInsOverDescriptors, reflectionTypes)
            else
                BuiltInFictitiousFunctionIrClassFactory(symbolTable, irBuiltInsOverDescriptors, reflectionTypes)
    irBuiltInsOverDescriptors.functionFactory = functionIrClassFactory
    val symbols = KonanSymbols(
            this,
            generatorContext.irBuiltIns,
            this.config.configuration
    )

    val irDeserializer = if (isProducingLibrary && !useLinkerWhenProducingLibrary) {
        // Enable lazy IR generation for newly-created symbols inside BE
        stubGenerator.unboundSymbolGeneration = true

        object : IrDeserializer {
            override fun getDeclaration(symbol: IrSymbol) = functionIrClassFactory.getDeclaration(symbol)
                    ?: stubGenerator.getDeclaration(symbol)

            override fun resolveBySignatureInModule(signature: IdSignature, kind: IrDeserializer.TopLevelSymbolKind, moduleName: Name): IrSymbol {
                error("Should not be called")
            }

            override fun postProcess(inOrAfterLinkageStep: Boolean) = Unit
        }
    } else {
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
                        messageCollector = messageCollector
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
                    if (isFullyCachedLibrary && kotlinLibrary?.isHeader == true)
                        linker.deserializeHeadersWithInlineBodies(dependency, kotlinLibrary)
                    else if (isProducingLibrary || isFullyCachedLibrary)
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

    translator.addPostprocessingStep { module ->
        val pluginContext = IrPluginContextImpl(
                generatorContext.moduleDescriptor,
                generatorContext.bindingContext,
                generatorContext.languageVersionSettings,
                generatorContext.symbolTable,
                generatorContext.typeTranslator,
                generatorContext.irBuiltIns,
                linker = irDeserializer,
                messageCollector = messageCollector
        )
        pluginExtensions.forEach { extension ->
            extension.generate(module, pluginContext)
        }
    }

    val mainModule = translator.generateModuleFragment(generatorContext, environment.getSourceFiles(), listOf(irDeserializer))

    irDeserializer.postProcess(inOrAfterLinkageStep = true)

    // Enable lazy IR genration for newly-created symbols inside BE
    stubGenerator.unboundSymbolGeneration = true

    messageCollector.checkNoUnboundSymbols(symbolTable, "at the end of IR linkage process")

    val modules = if (isProducingLibrary) emptyMap() else (irDeserializer as KonanIrLinker).modules

    if (config.configuration.getBoolean(KonanConfigKeys.FAKE_OVERRIDE_VALIDATOR)) {
        val fakeOverrideChecker = FakeOverrideChecker(KonanManglerIr, KonanManglerDesc)
        modules.values.forEach { fakeOverrideChecker.check(it) }
    }
    // IR linker deserializes files in the order they lie on the disk, which might be inconvenient,
    // so to make the pipeline more deterministic, the files are to be sorted.
    // This concerns in the first place global initializers order for the eager initialization strategy,
    // where the files are being initialized in order one by one.
    modules.values.forEach { module -> module.files.sortBy { it.fileEntry.name } }

    if (!isProducingLibrary) {
        // TODO: find out what should be done in the new builtins/symbols about it
        if (stdlibIsBeingCached) {
            (functionIrClassFactory as? BuiltInFictitiousFunctionIrClassFactory)?.buildAllClasses()
        }
        (functionIrClassFactory as? BuiltInFictitiousFunctionIrClassFactory)?.module =
                (modules.values + mainModule).single { it.descriptor == this.stdlibModule }
    }

    mainModule.files.forEach { it.metadata = DescriptorMetadataSource.File(listOf(mainModule.descriptor)) }
    modules.values.forEach { module ->
        module.files.forEach { it.metadata = DescriptorMetadataSource.File(listOf(module.descriptor)) }
    }

    return if (isProducingLibrary) {
        PsiToIrOutput.ForKlib(mainModule, generatorContext.irBuiltIns, symbols)
    } else if (libraryToCache == null) {
        PsiToIrOutput.ForBackend(modules, mainModule, generatorContext.irBuiltIns, symbols, symbolTable, irDeserializer as KonanIrLinker)
    } else {
        val libraryName = libraryToCache.klib.libraryName
        val libraryModule = modules[libraryName] ?: error("No module for the library being cached: $libraryName")
        PsiToIrOutput.ForBackend(
                irModules = modules.filterKeys { it != libraryName },
                irModule = libraryModule,
                irBuiltIns = generatorContext.irBuiltIns,
                symbols = symbols,
                symbolTable = symbolTable,
                irLinker = irDeserializer as KonanIrLinker
        )
    }
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
