/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import llvm.LLVMTypeRef
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.InlineClassesUtils
import org.jetbrains.kotlin.backend.common.ir.KlibSharedVariablesManager
import org.jetbrains.kotlin.config.LoggingContext
import org.jetbrains.kotlin.backend.common.linkage.partial.createPartialLinkageSupportForLowerings
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterExportedElements
import org.jetbrains.kotlin.backend.konan.driver.BasicNativeBackendPhaseContext
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
import org.jetbrains.kotlin.backend.konan.serialization.TrivialGettersDeserializer
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.ValueClassBackendAgnosticApi
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.ir.util.isTrivialGetter
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.utils.addToStdlib.getOrSetIfNull
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

private var IrClass.layoutBuilder: ClassLayoutBuilder? by irAttribute(copyByDefault = false)

internal class Context(
        config: NativeSecondStageCompilationConfig,
        val sourcesModules: Set<ModuleDescriptor>,
        @OptIn(K1Deprecation::class)
        val builtIns: KonanBuiltIns,
        override val irBuiltIns: IrBuiltIns,
        val irModules: Map<Path, IrModuleFragment>,
        val irLinker: KonanIrLinker,
        override val symbols: BackendNativeSymbols,
        val symbolTable: ReferenceSymbolTable,
) : BasicNativeBackendPhaseContext(config), CommonBackendContext {
    override val configuration get() = config.configuration

    override val irFactory: IrFactory = IrFactoryImpl

    override val optimizeLoopsOverUnsignedArrays = true

    @OptIn(ValueClassBackendAgnosticApi::class)
    override val inlineClassesUtils: InlineClassesUtils = object : InlineClassesUtils {
        override fun isClassInlineLike(klass: IrClass): Boolean =
                klass.isInlineClass(treatCompatibleFullValueClassesAsInline = true)
    }
    override val innerClassesSupport: NativeInnerClassesSupport by lazy { NativeInnerClassesSupport(irFactory) }
    val bridgesSupport by lazy { BridgesSupport(irBuiltIns, symbols, irFactory) }
    val enumsSupport by lazy { EnumsSupport(irBuiltIns, irFactory) }
    val cachesAbiSupport by lazy { CachesAbiSupport(irFactory) }

    override val sharedVariablesManager by lazy {
        // Creating lazily because builtIns module seems to be incomplete during `link` test;
        // TODO: investigate this.
        KlibSharedVariablesManager(symbols)
    }

    override fun log(message: String) {
        super<BasicNativeBackendPhaseContext>.log(message)
    }

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
            InlineFunctionDeserializer(irBuiltIns, deserializer, config.cachedLibraries, irLinker)
        }
    }

    private val trivialGettersDeserializers = mutableMapOf<KonanPartialModuleDeserializer, TrivialGettersDeserializer>()

    /**
     * Cache-aware version of [isTrivialGetter] specialized for `val` properties.
     *
     * For getters declared in the current module we just delegate to [isTrivialGetter].
     * For getters declared in a cached dependency, the IR body may be empty, so we instead consult
     * the cache built when that dependency was first compiled (see `CacheInfoBuilder` and `TrivialGetterSerializer`).
     */
    fun isTrivialGetter(function: IrSimpleFunction): Boolean {
        val deserializer = moduleDeserializerProvider.getDeserializerOrNull(function)
                ?: return function.isTrivialGetter // Function from current module.

        val signature = function.symbol.signature ?: return false
        val trivialGettersDeserializer = trivialGettersDeserializers.getOrPut(deserializer) {
            TrivialGettersDeserializer(config.cachedLibraries, deserializer)
        }
        return signature in trivialGettersDeserializer.trivialGetterSignatures
    }

    fun getLayoutBuilder(irClass: IrClass): ClassLayoutBuilder {
        (irClass.metadata as? KonanMetadata.Class)?.layoutBuilder?.let {
            return it
        }
        synchronized(irClass) {
            return irClass::layoutBuilder.getOrSetIfNull { ClassLayoutBuilder(irClass, this) }
        }
    }

    lateinit var globalHierarchyAnalysisResult: GlobalHierarchyAnalysisResult

    override val typeSystem: IrTypeSystemContext
        get() = IrTypeSystemContextImpl(irBuiltIns)

    var cAdapterExportedElements: CAdapterExportedElements? = null
    var objCExportedInterface: ObjCExportedInterface? = null
    var objCExportCodeSpec: ObjCExportCodeSpec? = null

    fun ghaEnabled() = ::globalHierarchyAnalysisResult.isInitialized

    @OptIn(K1Deprecation::class)
    val stdlibModule
        get() = this.builtIns.any.module

    val declaredLocalArrays: MutableMap<String, LLVMTypeRef> = HashMap()

    val targetAbiInfo = config.target.abiInfo

    override fun dispose() {}

    override val partialLinkageSupport = createPartialLinkageSupportForLowerings(
            config.partialLinkageConfig,
            KtDiagnosticReporterWithImplicitIrBasedContext(
                    configuration.diagnosticsCollector,
                    config.languageVersionSettings,
            )
    )
}

internal class ContextLogger(val context: LoggingContext) {
    operator fun String.unaryPlus() = context.log { this }
}

internal fun LoggingContext.logMultiple(messageBuilder: ContextLogger.() -> Unit) {
    if (!inVerbosePhase) return
    with(ContextLogger(this)) { messageBuilder() }
}
