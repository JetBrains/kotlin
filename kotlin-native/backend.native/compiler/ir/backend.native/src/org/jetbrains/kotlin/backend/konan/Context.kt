/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import llvm.LLVMTypeRef
import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.Mapping
import org.jetbrains.kotlin.backend.common.linkage.partial.createPartialLinkageSupportForLowerings
import org.jetbrains.kotlin.backend.common.lower.InnerClassesSupport
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterExportedElements
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.backend.konan.llvm.KonanMetadata
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportCodeSpec
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportedInterface
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrLinker
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import java.util.concurrent.ConcurrentHashMap

internal class NativeMapping : Mapping() {
    enum class AtomicFunctionType {
        COMPARE_AND_EXCHANGE, COMPARE_AND_SET, GET_AND_SET, GET_AND_ADD,
        ATOMIC_GET_ARRAY_ELEMENT, ATOMIC_SET_ARRAY_ELEMENT, COMPARE_AND_EXCHANGE_ARRAY_ELEMENT, COMPARE_AND_SET_ARRAY_ELEMENT, GET_AND_SET_ARRAY_ELEMENT, GET_AND_ADD_ARRAY_ELEMENT;
    }

    val outerThisFields: DeclarationMapping<IrClass, IrField> by AttributeBasedMappingDelegate()
    val enumValueGetters: DeclarationMapping<IrClass, IrFunction> by AttributeBasedMappingDelegate()
    val enumEntriesMaps: DeclarationMapping<IrClass, Map<Name, LoweredEnumEntryDescription>> by AttributeBasedMappingDelegate()
    val bridges: DeclarationMapping<IrSimpleFunction, MutableMap<BridgeDirections, IrSimpleFunction>> by AttributeBasedMappingDelegate()
    val loweredInlineFunctions: DeclarationMapping<IrFunction, IrFunction> by AttributeBasedMappingDelegate()
    val outerThisCacheAccessors: DeclarationMapping<IrClass, IrSimpleFunction> by AttributeBasedMappingDelegate()
    val lateinitPropertyCacheAccessors: DeclarationMapping<IrProperty, IrSimpleFunction> by AttributeBasedMappingDelegate()
    val topLevelFieldCacheAccessors: DeclarationMapping<IrField, IrSimpleFunction> by AttributeBasedMappingDelegate()
    val objectInstanceGetter: DeclarationMapping<IrClass, IrSimpleFunction> by AttributeBasedMappingDelegate()
    val boxFunctions: DeclarationMapping<IrClass, IrSimpleFunction> by AttributeBasedMappingDelegate()
    val unboxFunctions: DeclarationMapping<IrClass, IrSimpleFunction> by AttributeBasedMappingDelegate()
    val loweredInlineClassConstructors: DeclarationMapping<IrConstructor, IrSimpleFunction> by AttributeBasedMappingDelegate()
    val volatileFieldToAtomicFunctions: DeclarationMapping<IrField, MutableMap<AtomicFunctionType, IrSimpleFunction>> by AttributeBasedMappingDelegate()
    val functionToVolatileField: DeclarationMapping<IrSimpleFunction, IrField> by AttributeBasedMappingDelegate()
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

    override val innerClassesSupport: InnerClassesSupport by lazy { NativeInnerClassesSupport(mapping, irFactory) }
    val bridgesSupport by lazy { BridgesSupport(mapping, irBuiltIns, irFactory) }
    val inlineFunctionsSupport by lazy { InlineFunctionsSupport(mapping) }
    val enumsSupport by lazy { EnumsSupport(mapping, irBuiltIns, irFactory) }
    val cachesAbiSupport by lazy { CachesAbiSupport(mapping, irFactory) }

    // TODO: Remove after adding special <userData> property to IrDeclaration.
    private val layoutBuilders = ConcurrentHashMap<IrClass, ClassLayoutBuilder>()

    fun getLayoutBuilder(irClass: IrClass): ClassLayoutBuilder =
            (irClass.metadata as? KonanMetadata.Class)?.layoutBuilder
                    ?: layoutBuilders.getOrPut(irClass) { ClassLayoutBuilder(irClass, this) }

    lateinit var globalHierarchyAnalysisResult: GlobalHierarchyAnalysisResult

    override val typeSystem: IrTypeSystemContext
        get() = IrTypeSystemContextImpl(irBuiltIns)

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

    override val partialLinkageSupport = createPartialLinkageSupportForLowerings(
            config.partialLinkageConfig,
            irBuiltIns,
            configuration.messageCollector
    )
}

internal class ContextLogger(val context: LoggingContext) {
    operator fun String.unaryPlus() = context.log { this }
}

internal fun LoggingContext.logMultiple(messageBuilder: ContextLogger.() -> Unit) {
    if (!inVerbosePhase) return
    with(ContextLogger(this)) { messageBuilder() }
}
