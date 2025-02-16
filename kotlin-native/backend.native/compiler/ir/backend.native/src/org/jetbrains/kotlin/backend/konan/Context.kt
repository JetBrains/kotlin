/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import llvm.LLVMTypeRef
import org.jetbrains.kotlin.config.LoggingContext
import org.jetbrains.kotlin.backend.common.linkage.partial.createPartialLinkageSupportForLowerings
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterExportedElements
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.backend.konan.llvm.KonanMetadata
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportCodeSpec
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportedInterface
import org.jetbrains.kotlin.backend.konan.serialization.ExternalDeclarationFileNameProvider
import org.jetbrains.kotlin.backend.konan.serialization.ModuleDeserializerProvider
import org.jetbrains.kotlin.backend.konan.serialization.InlineFunctionDeserializer
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrLinker
import org.jetbrains.kotlin.backend.konan.serialization.KonanPartialModuleDeserializer
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import java.util.concurrent.ConcurrentHashMap

// TODO: Can be renamed or merged with KonanBackendContext
internal class Context(
        config: KonanConfig,
        val sourcesModules: Set<ModuleDescriptor>,
        override val builtIns: KonanBuiltIns,
        override val irBuiltIns: IrBuiltIns,
        val irModules: Map<String, IrModuleFragment>,
        val irLinker: KonanIrLinker,
        symbols: KonanSymbols,
        val symbolTable: ReferenceSymbolTable,
) : KonanBackendContext(config) {

    override val ir: KonanIr = KonanIr(symbols)

    override val configuration get() = config.configuration

    override val optimizeLoopsOverUnsignedArrays = true

    override val innerClassesSupport: NativeInnerClassesSupport by lazy { NativeInnerClassesSupport(irFactory) }
    val bridgesSupport by lazy { BridgesSupport(irBuiltIns, irFactory) }
    val enumsSupport by lazy { EnumsSupport(irBuiltIns, irFactory) }
    val cachesAbiSupport by lazy { CachesAbiSupport(mapping, irFactory) }

    val moduleDeserializerProvider by lazy {
        ModuleDeserializerProvider(config.libraryToCache, config.cachedLibraries, irLinker)
    }

    val externalDeclarationFileNameProvider by lazy {
        ExternalDeclarationFileNameProvider(moduleDeserializerProvider)
    }

    private val inlineFunctionDeserializers = ConcurrentHashMap<KonanPartialModuleDeserializer, InlineFunctionDeserializer>()

    fun getInlineFunctionDeserializer(function: IrFunction): InlineFunctionDeserializer {
        val deserializer = moduleDeserializerProvider.getDeserializerOrNull(function)
                ?: error("No module deserializer for ${function.render()}")
        return inlineFunctionDeserializers.getOrPut(deserializer) {
            InlineFunctionDeserializer(deserializer, config.cachedLibraries, irLinker)
        }
    }

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
