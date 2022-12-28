/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import llvm.LLVMTypeRef
import org.jetbrains.kotlin.backend.common.DefaultDelegateFactory
import org.jetbrains.kotlin.backend.common.DefaultMapping
import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterExportedElements
import org.jetbrains.kotlin.backend.konan.descriptors.BridgeDirections
import org.jetbrains.kotlin.backend.konan.descriptors.ClassLayoutBuilder
import org.jetbrains.kotlin.backend.konan.descriptors.GlobalHierarchyAnalysisResult
import org.jetbrains.kotlin.backend.konan.driver.phases.PsiToIrContext
import org.jetbrains.kotlin.backend.konan.driver.phases.PsiToIrOutput
import org.jetbrains.kotlin.backend.konan.ir.KonanIr
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.llvm.CodegenClassMetadata
import org.jetbrains.kotlin.backend.konan.llvm.Lifetime
import org.jetbrains.kotlin.backend.konan.llvm.coverage.CoverageManager
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportCodeSpec
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportedInterface
import org.jetbrains.kotlin.backend.konan.optimizations.DevirtualizationAnalysis
import org.jetbrains.kotlin.backend.konan.optimizations.ModuleDFG
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrLinker
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyClass
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.konan.library.KonanLibraryLayout
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import kotlin.LazyThreadSafetyMode.PUBLICATION

internal class NativeMapping : DefaultMapping() {
    data class BridgeKey(val target: IrSimpleFunction, val bridgeDirections: BridgeDirections)
    enum class AtomicFunctionType {
        COMPARE_AND_SWAP, COMPARE_AND_SET, GET_AND_SET, GET_AND_ADD;
    }
    data class AtomicFunctionKey(val field: IrField, val type: AtomicFunctionType)

    val outerThisFields = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrField>()
    val enumValueGetters = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrFunction>()
    val enumEntriesMaps = mutableMapOf<IrClass, Map<Name, LoweredEnumEntryDescription>>()
    val bridges = mutableMapOf<BridgeKey, IrSimpleFunction>()
    val notLoweredInlineFunctions = mutableMapOf<IrFunctionSymbol, IrFunction>()
    val outerThisCacheAccessors = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrSimpleFunction>()
    val lateinitPropertyCacheAccessors = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrProperty, IrSimpleFunction>()
    val objectInstanceGetter = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrSimpleFunction>()
    val boxFunctions = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrSimpleFunction>()
    val unboxFunctions = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrSimpleFunction>()
    val loweredInlineClassConstructors = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrConstructor, IrSimpleFunction>()
    val volatileFieldToAtomicFunction = mutableMapOf<AtomicFunctionKey, IrSimpleFunction>()
    val functionToVolatileField = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrSimpleFunction, IrField>()
}

// TODO: Can be renamed or merged with KonanBackendContext
internal class Context(
        config: KonanConfig,
        val sourcesModules: Set<ModuleDescriptor>,
        override val builtIns: KonanBuiltIns,
        override val irBuiltIns: IrBuiltIns,
        val irModules: Map<String, IrModuleFragment>,
        val irLinker: KonanIrLinker,
        symbols: KonanSymbols,
) : KonanBackendContext(config) {

    override val ir: KonanIr = KonanIr(this, symbols)

    override val configuration get() = config.configuration

    override val internalPackageFqn: FqName = RuntimeNames.kotlinNativeInternalPackageName

    override val optimizeLoopsOverUnsignedArrays = true

    val innerClassesSupport by lazy { InnerClassesSupport(mapping, irFactory) }
    val bridgesSupport by lazy { BridgesSupport(mapping, irBuiltIns, irFactory) }
    val inlineFunctionsSupport by lazy { InlineFunctionsSupport(mapping) }
    val enumsSupport by lazy { EnumsSupport(mapping, irBuiltIns, irFactory) }
    val cachesAbiSupport by lazy { CachesAbiSupport(mapping, irFactory) }

    // TODO: Remove after adding special <userData> property to IrDeclaration.
    private val layoutBuilders = mutableMapOf<IrClass, ClassLayoutBuilder>()

    fun getLayoutBuilder(irClass: IrClass): ClassLayoutBuilder {
        if (irClass is IrLazyClass)
            return layoutBuilders.getOrPut(irClass) {
                ClassLayoutBuilder(irClass, this)
            }
        val metadata = irClass.metadata as? CodegenClassMetadata
                ?: CodegenClassMetadata(irClass).also { irClass.metadata = it }
        metadata.layoutBuilder?.let { return it }
        val layoutBuilder = ClassLayoutBuilder(irClass, this)
        metadata.layoutBuilder = layoutBuilder
        return layoutBuilder
    }

    lateinit var globalHierarchyAnalysisResult: GlobalHierarchyAnalysisResult

    override val typeSystem: IrTypeSystemContext
        get() = IrTypeSystemContextImpl(irBuiltIns)

    val interopBuiltIns by lazy {
        InteropBuiltIns(this.builtIns)
    }

    var cAdapterExportedElements: CAdapterExportedElements? = null
    var objCExportedInterface: ObjCExportedInterface? = null
    var objCExportCodeSpec: ObjCExportCodeSpec? = null

    fun ghaEnabled() = ::globalHierarchyAnalysisResult.isInitialized

    val stdlibModule
        get() = this.builtIns.any.module

    val declaredLocalArrays: MutableMap<String, LLVMTypeRef> = HashMap()

    val targetAbiInfo = config.target.abiInfo

    val memoryModel = config.memoryModel

    override fun dispose() {}
}

internal class ContextLogger(val context: LoggingContext) {
    operator fun String.unaryPlus() = context.log { this }
}

internal fun LoggingContext.logMultiple(messageBuilder: ContextLogger.() -> Unit) {
    if (!inVerbosePhase) return
    with(ContextLogger(this)) { messageBuilder() }
}