package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideChecker
import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.backend.common.serialization.linkerissues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.serialization.mangle.ManglerChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.Ir2DescriptorManglerAdapter
import org.jetbrains.kotlin.backend.konan.descriptors.isForwardDeclarationModule
import org.jetbrains.kotlin.backend.konan.descriptors.isFromInteropLibrary
import org.jetbrains.kotlin.backend.konan.driver.phases.PsiToIrContext
import org.jetbrains.kotlin.backend.konan.driver.phases.PsiToIrInput
import org.jetbrains.kotlin.backend.konan.driver.phases.PsiToIrOutput
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbolsOverDescriptors
import org.jetbrains.kotlin.backend.konan.ir.interop.IrProviderForCEnumAndCStructStubs
import org.jetbrains.kotlin.backend.konan.ir.konanLibrary
import org.jetbrains.kotlin.backend.konan.serialization.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.TranslationPluginContext
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.KlibModuleOrigin
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

internal fun PsiToIrContext.psiToIr(
        input: PsiToIrInput,
        useLinkerWhenProducingLibrary: Boolean
): PsiToIrOutput {
    val symbolTable = symbolTable!!
    val (moduleDescriptor, environment, isProducingLibrary) = input
    // Translate AST to high level IR.
    val expectActualLinker = config.configuration[CommonConfigurationKeys.EXPECT_ACTUAL_LINKER] ?: false
    val messageLogger = config.configuration.irMessageLogger

    val partialLinkageEnabled = config.configuration[KonanConfigKeys.PARTIAL_LINKAGE] ?: false

    val translator = Psi2IrTranslator(
            config.configuration.languageVersionSettings,
            Psi2IrConfiguration(ignoreErrors = false, partialLinkageEnabled),
            messageLogger::checkNoUnboundSymbols
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
    val descriptorsLookup = DescriptorsLookup(this.builtIns)
    val symbols = KonanSymbolsOverDescriptors(this, descriptorsLookup, generatorContext.irBuiltIns, symbolTable, symbolTable.lazyWrapper)

    val irDeserializer = if (isProducingLibrary && !useLinkerWhenProducingLibrary) {
        // Enable lazy IR generation for newly-created symbols inside BE
        stubGenerator.unboundSymbolGeneration = true

        object : IrDeserializer {
            override fun getDeclaration(symbol: IrSymbol) = functionIrClassFactory.getDeclaration(symbol)
                    ?: stubGenerator.getDeclaration(symbol)

            override fun resolveBySignatureInModule(signature: IdSignature, kind: IrDeserializer.TopLevelSymbolKind, moduleName: Name): IrSymbol {
                error("Should not be called")
            }
        }
    } else {
        val exportedDependencies = (moduleDescriptor.getExportedDependencies(config) + libraryToCacheModule?.let { listOf(it) }.orEmpty()).distinct()
        val irProviderForCEnumsAndCStructs =
                IrProviderForCEnumAndCStructStubs(generatorContext, interopBuiltIns, symbols)

        val translationContext = object : TranslationPluginContext {
            override val moduleDescriptor: ModuleDescriptor
                get() = generatorContext.moduleDescriptor
            override val symbolTable: ReferenceSymbolTable
                get() = symbolTable
            override val typeTranslator: TypeTranslator
                get() = generatorContext.typeTranslator
            override val irBuiltIns: IrBuiltIns
                get() = generatorContext.irBuiltIns
        }

        val friendModules = config.resolvedLibraries.getFullList()
                .filter { it.libraryFile in config.friendModuleFiles }
                .map { it.uniqueName }

        val friendModulesMap = (
                listOf(moduleDescriptor.name.asStringStripSpecialMarkers()) +
                        config.resolve.includedLibraries.map { it.uniqueName }
                ).associateWith { friendModules }

        KonanIrLinker(
                currentModule = moduleDescriptor,
                translationPluginContext = translationContext,
                messageLogger = messageLogger,
                builtIns = generatorContext.irBuiltIns,
                symbolTable = symbolTable,
                friendModules = friendModulesMap,
                forwardModuleDescriptor = forwardDeclarationsModuleDescriptor,
                stubGenerator = stubGenerator,
                cenumsProvider = irProviderForCEnumsAndCStructs,
                exportedDependencies = exportedDependencies,
                partialLinkageEnabled = partialLinkageEnabled,
                cachedLibraries = config.cachedLibraries,
                lazyIrForCaches = config.lazyIrForCaches,
                libraryBeingCached = config.libraryToCache,
                userVisibleIrModulesSupport = config.userVisibleIrModulesSupport
        ).also { linker ->

            // context.config.librariesWithDependencies could change at each iteration.
            var dependenciesCount = 0
            while (true) {
                // context.config.librariesWithDependencies could change at each iteration.
                val dependencies = moduleDescriptor.allDependencyModules.filter {
                    config.librariesWithDependencies().contains(it.konanLibrary)
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
                    if (isProducingLibrary || (config.lazyIrForCaches && isFullyCachedLibrary))
                        linker.deserializeOnlyHeaderModule(dependency, kotlinLibrary)
                    else if (isFullyCachedLibrary)
                        linker.deserializeHeadersWithInlineBodies(dependency, kotlinLibrary!!)
                    else
                        linker.deserializeIrModuleHeader(dependency, kotlinLibrary, dependency.name.asString())
                }
                if (dependencies.size == dependenciesCount) break
                dependenciesCount = dependencies.size
            }

            // We need to run `buildAllEnumsAndStructsFrom` before `generateModuleFragment` because it adds references to symbolTable
            // that should be bound.
            if (libraryToCacheModule?.isFromInteropLibrary() == true)
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
                diagnosticReporter = messageLogger
        )
        pluginExtensions.forEach { extension ->
            extension.generate(module, pluginContext)
        }
    }

    val expectDescriptorToSymbol = mutableMapOf<DeclarationDescriptor, IrSymbol>()
    val mainModule = translator.generateModuleFragment(
            generatorContext,
            environment.getSourceFiles(),
            irProviders = listOf(irDeserializer),
            linkerExtensions = pluginExtensions,
            // TODO: This is a hack to allow platform libs to build in reasonable time.
            // referenceExpectsForUsedActuals() appears to be quadratic in time because of
            // how ExpectedActualResolver is implemented.
            // Need to fix ExpectActualResolver to either cache expects or somehow reduce the member scope searches.
            expectDescriptorToSymbol = if (expectActualLinker) expectDescriptorToSymbol else null
    ).toKonanModule()

    irDeserializer.postProcess()

    // Enable lazy IR genration for newly-created symbols inside BE
    stubGenerator.unboundSymbolGeneration = true

    messageLogger.checkNoUnboundSymbols(symbolTable, "at the end of IR linkage process")

    mainModule.acceptVoid(ManglerChecker(KonanManglerIr, Ir2DescriptorManglerAdapter(KonanManglerDesc)))

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

    mainModule.files.forEach { it.metadata = KonanFileMetadataSource(mainModule) }
    modules.values.forEach { module ->
        module.files.forEach { it.metadata = KonanFileMetadataSource(module as KonanIrModuleFragmentImpl) }
    }

    return if (isProducingLibrary) {
        PsiToIrOutput.ForKlib(mainModule, symbols, expectDescriptorToSymbol)
    } else if (libraryToCache == null) {
        PsiToIrOutput.ForBackend(modules, mainModule, symbols, irDeserializer as KonanIrLinker)
    } else {
        val libraryName = libraryToCache.klib.libraryName
        val libraryModule = modules[libraryName] ?: error("No module for the library being cached: $libraryName")
        PsiToIrOutput.ForBackend(modules.filterKeys { it != libraryName }, libraryModule, symbols, irDeserializer as KonanIrLinker)
    }
}
