package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.linkage.IrDeserializer
import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.linkage.partial.partialLinkageConfig
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideChecker
import org.jetbrains.kotlin.backend.common.phaser.KotlinBackendIrHolder
import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
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
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorImpl
import org.jetbrains.kotlin.resolve.BindingContext

internal data class PsiToIrInput(
        val moduleDescriptor: ModuleDescriptor,
        val environment: KotlinCoreEnvironment,
)

internal class PsiToIrOutput(
        val irModule: IrModuleFragment,
        val irBuiltIns: IrBuiltIns,
        val symbols: KonanSymbols,
) : KotlinBackendIrHolder {

    override val kotlinIr: IrElement
        get() = irModule
}

internal interface PsiToIrContext : PhaseContext {
    val symbolTable: SymbolTable?

    val reflectionTypes: KonanReflectionTypes

    val builtIns: KonanBuiltIns

    val bindingContext: BindingContext
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
internal fun PsiToIrContext.psiToIr(
        input: PsiToIrInput,
): PsiToIrOutput {
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

    val pluginExtensions = IrGenerationExtension.getInstances(config.project)

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

    val modules = emptyMap<String, IrModuleFragment>()

    if (config.configuration.getBoolean(KonanConfigKeys.FAKE_OVERRIDE_VALIDATOR)) {
        val fakeOverrideChecker = FakeOverrideChecker(KonanManglerIr, KonanManglerDesc)
        modules.values.forEach { fakeOverrideChecker.check(it) }
    }
    // IR linker deserializes files in the order they lie on the disk, which might be inconvenient,
    // so to make the pipeline more deterministic, the files are to be sorted.
    // This concerns in the first place global initializers order for the eager initialization strategy,
    // where the files are being initialized in order one by one.
    modules.values.forEach { module -> module.files.sortBy { it.fileEntry.name } }

    mainModule.files.forEach { it.metadata = DescriptorMetadataSource.File(listOf(mainModule.descriptor)) }
    modules.values.forEach { module ->
        module.files.forEach { it.metadata = DescriptorMetadataSource.File(listOf(module.descriptor)) }
    }

    return PsiToIrOutput(mainModule, generatorContext.irBuiltIns, symbols)
}