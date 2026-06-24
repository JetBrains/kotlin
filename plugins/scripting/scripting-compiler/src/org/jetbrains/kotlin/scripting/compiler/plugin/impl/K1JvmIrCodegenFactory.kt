/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.ir.ExpectSymbolTransformer
import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.backend.jvm.JvmBackendExtension
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory.BackendInput
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory.IdeCodegenSettings
import org.jetbrains.kotlin.backend.jvm.JvmIrTypeSystemContext
import org.jetbrains.kotlin.backend.jvm.SymbolTableWithBuiltInsDeduplication
import org.jetbrains.kotlin.backend.jvm.serialization.DisabledIdSignatureDescriptor
import org.jetbrains.kotlin.backend.jvm.serialization.JvmIdSignatureDescriptor
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmDescriptorMangler
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrLinker
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.KlibModuleOrigin
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorForNotFoundClasses
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorImpl
import org.jetbrains.kotlin.psi2ir.preprocessing.SourceDeclarationsPreprocessor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CleanableBindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.multiplatform.findCompatibleActualsForExpected

class K1JvmIrCodegenFactory(
    private val configuration: CompilerConfiguration,
    private val externalMangler: JvmDescriptorMangler? = null,
    private val externalSymbolTable: SymbolTable? = null,
    private val jvmGeneratorExtensions: JvmGeneratorExtensionsImpl = JvmGeneratorExtensionsImpl(configuration),
    private val ideCodegenSettings: IdeCodegenSettings = IdeCodegenSettings(),
) {
    val normalFactory: JvmIrCodegenFactory = JvmIrCodegenFactory(
        configuration,
        ideCodegenSettings
    )

    @K1Deprecation
    fun convertToIr(state: GenerationState, files: Collection<KtFile>, bindingContext: BindingContext): BackendInput = with(state) {
        convertToIr(
            files, configuration, module, diagnosticReporter, bindingContext, config.languageVersionSettings, ignoreErrors,
            skipBodies = !classBuilderMode.generateBodies
        )
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    @K1Deprecation
    fun convertToIr(
        files: Collection<KtFile>,
        configuration: CompilerConfiguration,
        module: ModuleDescriptor,
        diagnosticReporter: DiagnosticReporter,
        bindingContext: BindingContext,
        languageVersionSettings: LanguageVersionSettings,
        ignoreErrors: Boolean,
        skipBodies: Boolean,
    ): BackendInput {
        val enableIdSignatures =
            configuration.getBoolean(JVMConfigurationKeys.LINK_VIA_SIGNATURES) ||
                    configuration[JVMConfigurationKeys.KLIB_PATHS, emptyList()].isNotEmpty()
        val [mangler, symbolTable] =
            if (externalSymbolTable != null) externalMangler!! to externalSymbolTable
            else {
                val mangler = JvmDescriptorMangler(MainFunctionDetector(bindingContext, languageVersionSettings))
                val signaturer =
                    if (enableIdSignatures) JvmIdSignatureDescriptor(mangler)
                    else DisabledIdSignatureDescriptor
                val symbolTable = SymbolTable(signaturer, IrFactoryImpl)
                mangler to symbolTable
            }
        val psi2ir = Psi2IrTranslator(
            languageVersionSettings,
            Psi2IrConfiguration(ignoreErrors, skipBodies),
            configuration::checkNoUnboundSymbols
        )
        val psi2irContext = psi2ir.createGeneratorContext(
            module,
            bindingContext,
            configuration,
            symbolTable,
            jvmGeneratorExtensions,
            fragmentContext = null,
        )

        // Built-ins deduplication must be enabled immediately so that there is no chance for duplicate built-in symbols to occur. For
        // example, the creation of `IrPluginContextImpl` might already lead to duplicate built-in symbols via `BuiltinSymbolsBase`.
        if (symbolTable is SymbolTableWithBuiltInsDeduplication) {
            symbolTable.bindBuiltIns(psi2irContext.moduleDescriptor.builtIns)
        }

        val stubGenerator =
            DeclarationStubGeneratorImpl(
                psi2irContext.moduleDescriptor, symbolTable, psi2irContext.irBuiltIns,
                DescriptorByIdSignatureFinderImpl(psi2irContext.moduleDescriptor, mangler),
                jvmGeneratorExtensions
            )

        val irProvider = if (enableIdSignatures) {
            JvmIrLinker(
                psi2irContext.moduleDescriptor,
                configuration,
                JvmIrTypeSystemContext(psi2irContext.irBuiltIns),
                symbolTable,
                stubGenerator,
                mangler,
            )
        } else {
            stubGenerator
        }

        SourceDeclarationsPreprocessor(psi2irContext).run(files)

        // The plugin context contains unbound symbols right after construction and has to be
        // instantiated before we resolve unbound symbols and invoke any postprocessing steps.
        val pluginContext = IrPluginContextImpl(
            psi2irContext.moduleDescriptor,
            psi2irContext.languageVersionSettings,
            symbolTable,
            psi2irContext.irBuiltIns,
            irProvider,
            @OptIn(MessageCollectorAccess::class) // deprecated in IrPluginContext
            configuration.messageCollector,
            diagnosticReporter
        )
        for (extension in configuration.filteredExtensions) {
            if (!psi2irContext.configuration.generateBodies &&
                !@OptIn(FirIncompatiblePluginAPI::class) extension.shouldAlsoBeAppliedInKaptStubGenerationMode
            ) continue

            psi2ir.addPostprocessingStep { module ->
                val old = stubGenerator.unboundSymbolGeneration
                try {
                    stubGenerator.unboundSymbolGeneration = true
                    extension.generate(module, pluginContext)
                } finally {
                    stubGenerator.unboundSymbolGeneration = old
                }
            }
        }

        val dependencies = if (irProvider !is KotlinIrLinker) {
            emptyList()
        } else {
            psi2irContext.moduleDescriptor.collectAllDependencyModulesTransitively().map {
                val kotlinLibrary = (it.getCapability(KlibModuleOrigin.CAPABILITY) as? DeserializedKlibModuleOrigin)?.library
                irProvider.deserializeIrModuleHeader(it, kotlinLibrary, _moduleName = it.name.asString())
            }
        }

        val irProviders = if (ideCodegenSettings.shouldStubAndNotLinkUnboundSymbols) {
            listOf(stubGenerator)
        } else {
            val stubGeneratorForMissingClasses = DeclarationStubGeneratorForNotFoundClasses(stubGenerator)
            listOf(irProvider, stubGeneratorForMissingClasses)
        }

        val irModuleFragment = psi2ir.generateModuleFragment(psi2irContext, files, irProviders)

        if (irProvider is KotlinIrLinker) {
            irProvider.postProcess(psi2irContext.irBuiltIns, inOrAfterLinkageStep = true)
            irProvider.clear()
        }

        stubGenerator.unboundSymbolGeneration = true

        // We need to compile all files we reference in Klibs
        irModuleFragment.files.addAll(dependencies.flatMap { it.files })

        if (ideCodegenSettings.shouldStubOrphanedExpectSymbols) {
            irModuleFragment.stubOrphanedExpectSymbols(stubGenerator)
        }

        if (!configuration.getBoolean(JVMConfigurationKeys.DO_NOT_CLEAR_BINDING_CONTEXT) && files.none { it.isScript() }) {
            if (bindingContext !is CleanableBindingContext) {
                error("BindingContext should be cleanable in JVM IR to avoid leaking memory: $bindingContext")
            }
            bindingContext.clear()
        }
        return BackendInput(
            irModuleFragment,
            psi2irContext.irBuiltIns,
            symbolTable,
            irProviders,
            jvmGeneratorExtensions,
            JvmBackendExtension.Default,
            pluginContext,
        )
    }

    private fun ModuleDescriptor.collectAllDependencyModulesTransitively(): List<ModuleDescriptor> {
        val result = LinkedHashSet<ModuleDescriptor>()
        fun collectImpl(descriptor: ModuleDescriptor) {
            val dependencies = descriptor.allDependencyModules
            dependencies.forEach { if (result.add(it)) collectImpl(it) }
        }
        collectImpl(this)
        return result.toList()
    }

    private val CompilerConfiguration.filteredExtensions: List<IrGenerationExtension>
        get() = this.getCompilerExtensions(IrGenerationExtension)
}

/**
 * Replaces `expect` symbols for which no `actual` counterpart exists with an `actual` stub in all files of the [IrModuleFragment]. The
 * implementation keeps track of generated stubs and only generates a single stub for each unique `expect` symbol.
 *
 * [stubOrphanedExpectSymbols] is used by the IDE bytecode tool window to allow compiling source files with `expect` declarations for which
 * the compiled module has no `actual` declaration. (The `actual` declaration would be defined in a module dependent on the compiled
 * module, but choosing this module is non-trivial due to possibly multiple implementations of the same `expect` symbol. In addition, when
 * generating bytecode for a single source file, the number of source files to compile should be kept low. Stubbing helps with that.)
 */
private fun IrModuleFragment.stubOrphanedExpectSymbols(stubGenerator: DeclarationStubGenerator) {
    val transformer = StubOrphanedExpectSymbolTransformer(stubGenerator)
    files.forEach(transformer::visitFile)
}

private class StubOrphanedExpectSymbolTransformer(val stubGenerator: DeclarationStubGenerator) : ExpectSymbolTransformer() {

    private val stubbedClasses = mutableMapOf<ClassDescriptor, IrClassSymbol>()
    private val stubbedProperties = mutableMapOf<PropertyDescriptor, ActualPropertyResult>()
    private val stubbedConstructors = mutableMapOf<ClassConstructorDescriptor, IrConstructorSymbol>()
    private val stubbedFunctions = mutableMapOf<FunctionDescriptor, IrSimpleFunctionSymbol>()

    override fun getActualClass(descriptor: ClassDescriptor): IrClassSymbol? {
        if (!descriptor.isOrphanedExpect()) return null

        return stubbedClasses.getOrPut(descriptor) {
            stubGenerator.generateClassStub(FakeActualClassDescriptor(descriptor)).symbol
        }
    }

    override fun getActualProperty(descriptor: PropertyDescriptor): ActualPropertyResult? {
        if (!descriptor.isOrphanedExpect()) return null

        return stubbedProperties.getOrPut(descriptor) {
            val irProperty =
                stubGenerator.generatePropertyStub(FakeActualPropertyDescriptor(descriptor)).apply { ensureClassParent(descriptor) }
            val irGetter = descriptor.getter?.let(::getActualFunction)
            val irSetter = descriptor.setter?.let(::getActualFunction)
            ActualPropertyResult(irProperty.symbol, irGetter, irSetter)
        }
    }

    override fun getActualConstructor(descriptor: ClassConstructorDescriptor): IrConstructorSymbol? {
        if (!descriptor.isOrphanedExpect()) return null

        return stubbedConstructors.getOrPut(descriptor) {
            stubGenerator.generateConstructorStub(FakeActualClassConstructorDescriptor(descriptor)).symbol
        }
    }

    override fun getActualFunction(descriptor: FunctionDescriptor): IrSimpleFunctionSymbol? {
        if (!descriptor.isOrphanedExpect()) return null

        return stubbedFunctions.getOrPut(descriptor) {
            stubGenerator
                .generateFunctionStub(FakeActualFunctionDescriptor(descriptor), createPropertyIfNeeded = false)
                .apply { ensureClassParent(descriptor) }
                .symbol
        }
    }

    /**
     * Property getters and setters are not marked as `isExpect` even if the corresponding property is. However, we still need to stub such
     * getters and setters, so [isTargetDeclaration] allows it.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun isTargetDeclaration(declaration: IrDeclaration): Boolean =
        super.isTargetDeclaration(declaration) ||
                declaration is IrSimpleFunction && declaration.correspondingPropertySymbol?.owner?.isExpect == true

    /**
     * If an `actual` symbol exists, we shouldn't stub the `expect` symbol. This will be performed by
     * [org.jetbrains.kotlin.backend.common.lower.ExpectDeclarationsRemoveLowering] during lowering.
     */
    @OptIn(K1Deprecation::class)
    private fun MemberDescriptor.isOrphanedExpect(): Boolean = findCompatibleActualsForExpected(module).isEmpty()

    /**
     * [descriptor] should be the original descriptor, because the copied `actual` descriptor has no source.
     */
    private fun IrDeclaration.ensureClassParent(descriptor: MemberDescriptor) {
        if (parent !is IrClass) {
            parent = stubGenerator.generateOrGetFacadeClass(descriptor) ?: return
        }
    }

}

private class FakeActualClassDescriptor(original: ClassDescriptor) : ClassDescriptor by original {
    override fun isActual(): Boolean = true
    override fun isExpect(): Boolean = false

    override fun getSource(): SourceElement = SourceElement.NO_SOURCE
    override fun getOriginal(): ClassDescriptor = this
}

private class FakeActualPropertyDescriptor(original: PropertyDescriptor) : PropertyDescriptor by original {
    override fun isActual(): Boolean = true
    override fun isExpect(): Boolean = false

    override fun getSource(): SourceElement = SourceElement.NO_SOURCE
    override fun getOriginal(): PropertyDescriptor = this
}

private class FakeActualClassConstructorDescriptor(original: ClassConstructorDescriptor) : ClassConstructorDescriptor by original {
    override fun isActual(): Boolean = true
    override fun isExpect(): Boolean = false

    override fun getSource(): SourceElement = SourceElement.NO_SOURCE
    override fun getOriginal(): ClassConstructorDescriptor = this
}

private class FakeActualFunctionDescriptor(original: FunctionDescriptor) : FunctionDescriptor by original {
    override fun isActual(): Boolean = true
    override fun isExpect(): Boolean = false

    // `actual` functions are stubbed without providing a body. Hence, they may not be inlined, even if the `expect` function is marked as
    // `inline`. Given that inlining requires meaningful bodies (assuming the generated bytecode is of interest), it does not suffice to
    // just supply an empty body stub.
    override fun isInline(): Boolean = false

    override fun getSource(): SourceElement = SourceElement.NO_SOURCE
    override fun getOriginal(): FunctionDescriptor = this
}
