package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.ManglerChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.Ir2DescriptorManglerAdapter
import org.jetbrains.kotlin.backend.konan.descriptors.isForwardDeclarationModule
import org.jetbrains.kotlin.backend.konan.descriptors.isFromInteropLibrary
import org.jetbrains.kotlin.backend.konan.ir.konanLibrary
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.interop.IrProviderForCEnumAndCStructStubs
import org.jetbrains.kotlin.backend.konan.serialization.*
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrLinker
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.konan.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.KlibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.ir.builders.TranslationPluginContext
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CleanableBindingContext
import org.jetbrains.kotlin.utils.DFS

internal fun Context.psiToIr(
        symbolTable: SymbolTable,
        isProducingLibrary: Boolean,
        useLinkerWhenProducingLibrary: Boolean
) {
    // Translate AST to high level IR.
    val expectActualLinker = config.configuration.get(CommonConfigurationKeys.EXPECT_ACTUAL_LINKER)?:false
    val messageLogger = config.configuration.get(IrMessageLogger.IR_MESSAGE_LOGGER) ?: IrMessageLogger.None

    val translator = Psi2IrTranslator(config.configuration.languageVersionSettings, Psi2IrConfiguration(false))
    val generatorContext = translator.createGeneratorContext(moduleDescriptor, bindingContext, symbolTable)

    val pluginExtensions = IrGenerationExtension.getInstances(config.project)

    val forwardDeclarationsModuleDescriptor = moduleDescriptor.allDependencyModules.firstOrNull { it.isForwardDeclarationModule }

    val modulesWithoutDCE = moduleDescriptor.allDependencyModules
            .filter { !llvmModuleSpecification.isFinal && llvmModuleSpecification.containsModule(it) }

    // Note: using [llvmModuleSpecification] since this phase produces IR for generating single LLVM module.

    val exportedDependencies = (getExportedDependencies() + modulesWithoutDCE).distinct()
    val functionIrClassFactory = BuiltInFictitiousFunctionIrClassFactory(
            symbolTable, generatorContext.irBuiltIns, reflectionTypes)
    generatorContext.irBuiltIns.functionFactory = functionIrClassFactory
    val stubGenerator = DeclarationStubGenerator(
            moduleDescriptor, symbolTable,
            config.configuration.languageVersionSettings
    )
    val symbols = KonanSymbols(this, generatorContext.irBuiltIns, symbolTable, symbolTable.lazyWrapper, functionIrClassFactory)

    val irDeserializer = if (isProducingLibrary && !useLinkerWhenProducingLibrary) {
        // Enable lazy IR generation for newly-created symbols inside BE
        stubGenerator.unboundSymbolGeneration = true

        object : IrDeserializer {
            override fun getDeclaration(symbol: IrSymbol) = stubGenerator.getDeclaration(symbol)
            override fun resolveBySignatureInModule(signature: IdSignature, kind: IrDeserializer.TopLevelSymbolKind, moduleName: Name): IrSymbol {
                error("Should not be called")
            }
        }
    } else {
        val irProviderForCEnumsAndCStructs =
                IrProviderForCEnumAndCStructStubs(generatorContext, interopBuiltIns, symbols)

        val translationContext = object : TranslationPluginContext {
            override val moduleDescriptor: ModuleDescriptor
                get() = generatorContext.moduleDescriptor
            override val bindingContext: BindingContext
                get() = generatorContext.bindingContext
            override val symbolTable: ReferenceSymbolTable
                get() = symbolTable
            override val typeTranslator: TypeTranslator
                get() = generatorContext.typeTranslator
            override val irBuiltIns: IrBuiltIns
                get() = generatorContext.irBuiltIns
        }

        KonanIrLinker(
                moduleDescriptor,
                functionIrClassFactory,
                translationContext,
                messageLogger,
                generatorContext.irBuiltIns,
                symbolTable,
                forwardDeclarationsModuleDescriptor,
                stubGenerator,
                irProviderForCEnumsAndCStructs,
                exportedDependencies,
                config.cachedLibraries
        ).also { linker ->

            // context.config.librariesWithDependencies could change at each iteration.
            var dependenciesCount = 0
            while (true) {
                // context.config.librariesWithDependencies could change at each iteration.
                val dependencies = moduleDescriptor.allDependencyModules.filter {
                    config.librariesWithDependencies(moduleDescriptor).contains(it.konanLibrary)
                }

                fun sortDependencies(dependencies: List<ModuleDescriptor>): Collection<ModuleDescriptor> {
                    return DFS.topologicalOrder(dependencies) {
                        it.allDependencyModules
                    }.reversed()
                }

                for (dependency in sortDependencies(dependencies).filter { it != moduleDescriptor }) {
                    val kotlinLibrary = dependency.getCapability(KlibModuleOrigin.CAPABILITY)?.let {
                        (it as? DeserializedKlibModuleOrigin)?.library
                    }
                    when {
                        isProducingLibrary -> linker.deserializeOnlyHeaderModule(dependency, kotlinLibrary)
                        kotlinLibrary != null && config.cachedLibraries.isLibraryCached(kotlinLibrary) ->
                            linker.deserializeHeadersWithInlineBodies(dependency, kotlinLibrary)
                        else -> linker.deserializeIrModuleHeader(dependency, kotlinLibrary)
                    }
                }
                if (dependencies.size == dependenciesCount) break
                dependenciesCount = dependencies.size
            }

            // We need to run `buildAllEnumsAndStructsFrom` before `generateModuleFragment` because it adds references to symbolTable
            // that should be bound.
            modulesWithoutDCE
                    .filter(ModuleDescriptor::isFromInteropLibrary)
                    .forEach(irProviderForCEnumsAndCStructs::referenceAllEnumsAndStructsFrom)


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

    expectDescriptorToSymbol = mutableMapOf()
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

    symbolTable.noUnboundLeft("Unbound symbols left after linker")

    mainModule.acceptVoid(ManglerChecker(KonanManglerIr, Ir2DescriptorManglerAdapter(KonanManglerDesc)))

    val modules = if (isProducingLibrary) emptyMap() else (irDeserializer as KonanIrLinker).modules

    if (config.configuration.getBoolean(KonanConfigKeys.FAKE_OVERRIDE_VALIDATOR)) {
        val fakeOverrideChecker = FakeOverrideChecker(KonanManglerIr, KonanManglerDesc)
        modules.values.forEach { fakeOverrideChecker.check(it) }
    }

    irModule = mainModule

    // Note: coupled with [shouldLower] below.
    irModules = modules.filterValues { llvmModuleSpecification.containsModule(it) }

    ir.symbols = symbols

    if (!isProducingLibrary) {
        if (this.stdlibModule in modulesWithoutDCE)
            functionIrClassFactory.buildAllClasses()
        internalAbi.init(irModules.values + irModule!!)
        functionIrClassFactory.module = (modules.values + irModule!!).single { it.descriptor.isNativeStdlib() }
    }

    mainModule.files.forEach { it.metadata = KonanFileMetadataSource(mainModule) }
    modules.values.forEach { module ->
        module.files.forEach { it.metadata = KonanFileMetadataSource(module as KonanIrModuleFragmentImpl) }
    }

    val originalBindingContext = bindingContext as? CleanableBindingContext
            ?: error("BindingContext should be cleanable in K/N IR to avoid leaking memory: $bindingContext")
    originalBindingContext.clear()

    this.bindingContext = BindingContext.EMPTY
}
