/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import llvm.LLVMTypeRef
import org.jetbrains.kotlin.backend.common.DefaultDelegateFactory
import org.jetbrains.kotlin.backend.common.DefaultMapping
import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.konan.descriptors.BridgeDirections
import org.jetbrains.kotlin.backend.konan.descriptors.ClassLayoutBuilder
import org.jetbrains.kotlin.backend.konan.descriptors.GlobalHierarchyAnalysisResult
import org.jetbrains.kotlin.backend.konan.ir.KonanIr
import org.jetbrains.kotlin.backend.konan.llvm.CodegenClassMetadata
import org.jetbrains.kotlin.backend.konan.llvm.Lifetime
import org.jetbrains.kotlin.backend.konan.llvm.coverage.CoverageManager
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExport
import org.jetbrains.kotlin.backend.konan.optimizations.DevirtualizationAnalysis
import org.jetbrains.kotlin.backend.konan.optimizations.ModuleDFG
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrLinker
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyClass
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.konan.library.KonanLibraryLayout
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.needSmallBinary
import org.jetbrains.kotlin.library.SerializedIrModule
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.reflect.KProperty

internal class NativeMapping : DefaultMapping() {
    data class BridgeKey(val target: IrSimpleFunction, val bridgeDirections: BridgeDirections)

    val outerThisFields = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrField>()
    val enumImplObjects = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrClass>()
    val enumValueGetters = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrFunction>()
    val enumEntriesMaps = mutableMapOf<IrClass, Map<Name, LoweredEnumEntryDescription>>()
    val bridges = mutableMapOf<BridgeKey, IrSimpleFunction>()
    val notLoweredInlineFunctions = mutableMapOf<IrFunctionSymbol, IrFunction>()
    val companionObjectCacheAccessors = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrSimpleFunction>()
    val outerThisCacheAccessors = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrSimpleFunction>()
    val lateinitPropertyCacheAccessors = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrProperty, IrSimpleFunction>()
    val enumValuesCacheAccessors = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrSimpleFunction>()
}

internal class Context(config: KonanConfig) : KonanBackendContext(config), ConfigChecks {
    lateinit var frontendServices: FrontendServices
    lateinit var environment: KotlinCoreEnvironment
    lateinit var bindingContext: BindingContext

    lateinit var moduleDescriptor: ModuleDescriptor

    lateinit var objCExport: ObjCExport

    lateinit var cAdapterGenerator: CAdapterGenerator

    lateinit var expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>

    override val builtIns: KonanBuiltIns by lazy(PUBLICATION) {
        moduleDescriptor.builtIns as KonanBuiltIns
    }

    override val configuration get() = config.configuration

    override val internalPackageFqn: FqName = RuntimeNames.kotlinNativeInternalPackageName

    override val optimizeLoopsOverUnsignedArrays = true

    lateinit var generationState: NativeGenerationState

    fun disposeGenerationState() {
        if (::generationState.isInitialized) generationState.dispose()
    }

    val phaseConfig = config.phaseConfig

    val innerClassesSupport by lazy { InnerClassesSupport(mapping, irFactory) }
    val bridgesSupport by lazy { BridgesSupport(mapping, irBuiltIns, irFactory) }
    val inlineFunctionsSupport by lazy { InlineFunctionsSupport(mapping) }
    val enumsSupport by lazy { EnumsSupport(mapping, ir.symbols, irBuiltIns, irFactory) }
    val cachesAbiSupport by lazy { CachesAbiSupport(mapping, ir.symbols, irFactory) }

    open class LazyMember<T>(val initializer: Context.() -> T) {
        operator fun getValue(thisRef: Context, property: KProperty<*>): T = thisRef.getValue(this)
    }

    class LazyVarMember<T>(initializer: Context.() -> T) : LazyMember<T>(initializer) {
        operator fun setValue(thisRef: Context, property: KProperty<*>, newValue: T) = thisRef.setValue(this, newValue)
    }

    companion object {
        fun <T> lazyMember(initializer: Context.() -> T) = LazyMember<T>(initializer)

        fun <K, V> lazyMapMember(initializer: Context.(K) -> V): LazyMember<(K) -> V> = lazyMember {
            val storage = mutableMapOf<K, V>()
            val result: (K) -> V = {
                storage.getOrPut(it, { initializer(it) })
            }
            result
        }

        fun <T> nullValue() = LazyVarMember<T?>({ null })
    }

    private val lazyValues = mutableMapOf<LazyMember<*>, Any?>()

    fun <T> getValue(member: LazyMember<T>): T =
            @Suppress("UNCHECKED_CAST") (lazyValues.getOrPut(member, { member.initializer(this) }) as T)

    fun <T> setValue(member: LazyVarMember<T>, newValue: T) {
        lazyValues[member] = newValue
    }

    val reflectionTypes: KonanReflectionTypes by lazy(PUBLICATION) {
        KonanReflectionTypes(moduleDescriptor)
    }

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

    // We serialize untouched descriptor tree and IR.
    // But we have to wait until the code generation phase,
    // to dump this information into generated file.
    var serializedMetadata: SerializedMetadata? = null
    var serializedIr: SerializedIrModule? = null
    var dataFlowGraph: ByteArray? = null

    val librariesWithDependencies by lazy {
        config.librariesWithDependencies(moduleDescriptor)
    }

    fun needGlobalInit(field: IrField): Boolean {
        if (field.descriptor.containingDeclaration !is PackageFragmentDescriptor) return false
        // TODO: add some smartness here. Maybe if package of the field is in never accessed
        // assume its global init can be actually omitted.
        return true
    }

    lateinit var irModules: Map<String, IrModuleFragment>

    // TODO: make lateinit?
    var irModule: IrModuleFragment? = null
        set(module) {
            if (field != null) {
                throw Error("Another IrModule in the context.")
            }
            field = module!!
            ir = KonanIr(this, module)
        }

    override lateinit var ir: KonanIr

    override val irBuiltIns
        get() = ir.irModule.irBuiltins

    override val typeSystem: IrTypeSystemContext
        get() = IrTypeSystemContextImpl(irBuiltIns)

    val interopBuiltIns by lazy {
        InteropBuiltIns(this.builtIns)
    }

    lateinit var bitcodeFileName: String
    lateinit var library: KonanLibraryLayout

    val coverage by lazy { CoverageManager(this) }

    fun separator(title: String) {
        println("\n\n--- ${title} ----------------------\n")
    }

    fun verifyBitCode() {
        if (::generationState.isInitialized)
            generationState.verifyBitCode()
    }

    fun printBitCode() {
        if (::generationState.isInitialized)
            generationState.printBitCode()
    }

    fun ghaEnabled() = ::globalHierarchyAnalysisResult.isInitialized

    val memoryModel = config.memoryModel

    override var inVerbosePhase = false
    override fun log(message: () -> String) {
        if (inVerbosePhase) {
            println(message())
        }
    }

    var moduleDFG: ModuleDFG? = null
    val lifetimes = mutableMapOf<IrElement, Lifetime>()
    var devirtualizationAnalysisResult: DevirtualizationAnalysis.AnalysisResult? = null

    var referencedFunctions: Set<IrFunction>? = null

    internal val stdlibModule
        get() = this.builtIns.any.module

    lateinit var compilerOutput: List<ObjectFile>

    val llvmModuleSpecification: LlvmModuleSpecification by lazy {
        when {
            config.produce.isCache ->
                CacheLlvmModuleSpecification(this, config.cachedLibraries, config.libraryToCache!!)
            else -> DefaultLlvmModuleSpecification(config.cachedLibraries)
        }
    }

    val declaredLocalArrays: MutableMap<String, LLVMTypeRef> = HashMap()

    lateinit var irLinker: KonanIrLinker

    val targetAbiInfo: TargetAbiInfo by lazy {
        when {
            config.target == KonanTarget.MINGW_X64 -> {
                WindowsX64TargetAbiInfo()
            }
            !config.target.family.isAppleFamily && config.target.architecture == Architecture.ARM64 -> {
                AAPCS64TargetAbiInfo()
            }
            else -> {
                DefaultTargetAbiInfo()
            }
        }
    }
}

internal class ContextLogger(val context: LoggingContext) {
    operator fun String.unaryPlus() = context.log { this }
}

internal fun LoggingContext.logMultiple(messageBuilder: ContextLogger.() -> Unit) {
    if (!inVerbosePhase) return
    with(ContextLogger(this)) { messageBuilder() }
}