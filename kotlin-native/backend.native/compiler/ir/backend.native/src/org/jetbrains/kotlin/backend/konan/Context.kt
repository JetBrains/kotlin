/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import llvm.*
import org.jetbrains.kotlin.backend.common.DefaultDelegateFactory
import org.jetbrains.kotlin.backend.common.DefaultMapping
import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.konan.descriptors.BridgeDirections
import org.jetbrains.kotlin.backend.konan.descriptors.ClassLayoutBuilder
import org.jetbrains.kotlin.backend.konan.descriptors.GlobalHierarchyAnalysisResult
import org.jetbrains.kotlin.backend.konan.ir.KonanIr
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.coverage.CoverageManager
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExport
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamer
import org.jetbrains.kotlin.backend.konan.optimizations.DevirtualizationAnalysis
import org.jetbrains.kotlin.backend.konan.optimizations.ModuleDFG
import org.jetbrains.kotlin.backend.konan.phases.*
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrLinker
import org.jetbrains.kotlin.backend.konan.serialization.SerializedClassFields
import org.jetbrains.kotlin.backend.konan.serialization.SerializedInlineFunctionReference
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.konan.CompiledKlibModuleOrigin
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
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
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.SerializedIrModule
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.reflect.KProperty

internal class InlineFunctionOriginInfo(val irFunction: IrFunction, val irFile: IrFile, val startOffset: Int, val endOffset: Int)

internal class NativeMapping() : DefaultMapping() {
    data class BridgeKey(val target: IrSimpleFunction, val bridgeDirections: BridgeDirections)

    val outerThisFields = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrField>()
    val enumImplObjects = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrClass>()
    val enumValueGetters = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrFunction>()
    val enumEntriesMaps = mutableMapOf<IrClass, Map<Name, LoweredEnumEntryDescription>>()
    val bridges = mutableMapOf<BridgeKey, IrSimpleFunction>()
    val notLoweredInlineFunctions = mutableMapOf<IrFunctionSymbol, IrFunction>()
    val loweredInlineFunctions = mutableMapOf<IrFunction, InlineFunctionOriginInfo>()
    val companionObjectCacheAccessors = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrSimpleFunction>()
    val outerThisCacheAccessors = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrSimpleFunction>()
    val lateinitPropertyCacheAccessors = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrProperty, IrSimpleFunction>()
    val enumValuesCacheAccessors = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrSimpleFunction>()
}

internal class Context(
        config: KonanConfig,
        override var objCExport: ObjCExport? = null,
) : AbstractKonanBackendContext(config),
        ConfigChecks,
        FrontendContext,
        PsiToIrContext,
        LlvmModuleSpecificationComponent,
        KlibProducingContext,
        BitcodegenContext,
        LlvmCodegenContext,
        BridgesAwareContext,
        LocalClassNameAwareContext,
        LtoContext,
        ObjCExportContext,
        MiddleEndContext,
        ObjectFilesContext,
        CacheAwareContext,
        CExportContext,
        LinkerContext,
        Component
{
    // TopDownAnalyzer Context
    override lateinit var frontendServices: FrontendServices

    override val container: ComponentContainer = ComponentContainer(setOf(this))

    // Frontend Context
    override lateinit var environment: KotlinCoreEnvironment
    override lateinit var bindingContext: BindingContext
    override lateinit var moduleDescriptor: ModuleDescriptor

    fun populateFromFrontend(frontendContext: FrontendContext) {
        environment = frontendContext.environment
        bindingContext = frontendContext.bindingContext
        moduleDescriptor = frontendContext.moduleDescriptor
        frontendServices = frontendContext.frontendServices
    }

    // Psi To IR context
    override var symbolTable: SymbolTable? = null

    override val isNativeLibrary: Boolean by lazy {
        val kind = config.configuration.get(KonanConfigKeys.PRODUCE)
        kind == CompilerOutputKind.DYNAMIC || kind == CompilerOutputKind.STATIC
    }

    override val objCExportNamer: ObjCExportNamer?
        get() = objCExport?.namer

    override lateinit var cAdapterGenerator: CAdapterGenerator
    override lateinit var expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>

    override val builtIns: KonanBuiltIns by lazy(PUBLICATION) {
        moduleDescriptor.builtIns as KonanBuiltIns
    }

    override val internalPackageFqn: FqName = RuntimeNames.kotlinNativeInternalPackageName

    override val optimizeLoopsOverUnsignedArrays = true

    override val innerClassesSupport by lazy { InnerClassesSupport(mapping, irFactory) }
    override val bridgesSupport: BridgesSupport by lazy { BridgesSupport(mapping, irBuiltIns, irFactory) }
    override val inlineFunctionsSupport: InlineFunctionsSupport by lazy { InlineFunctionsSupport(mapping) }
    override val enumsSupport by lazy { EnumsSupport(mapping, ir.symbols, irBuiltIns, irFactory) }
    override val cachesAbiSupport: CachesAbiSupport by lazy { CachesAbiSupport(mapping, ir.symbols, irFactory) }

    override val lazyValues = mutableMapOf<LazyMember<*>, Any?>()

    override val localClassNames = mutableMapOf<IrAttributeContainer, String>()

    /* test suite class -> test function names */
    override val testCasesToDump = mutableMapOf<ClassId, MutableCollection<String>>()

    override val reflectionTypes: KonanReflectionTypes by lazy(PUBLICATION) {
        KonanReflectionTypes(moduleDescriptor)
    }

    // TODO: Remove after adding special <userData> property to IrDeclaration.
    private val layoutBuilders = mutableMapOf<IrClass, ClassLayoutBuilder>()

    override fun getLayoutBuilder(irClass: IrClass): ClassLayoutBuilder {
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

    override lateinit var globalHierarchyAnalysisResult: GlobalHierarchyAnalysisResult

    // We serialize untouched descriptor tree and IR.
    // But we have to wait until the code generation phase,
    // to dump this information into generated file.
    override var serializedMetadata: SerializedMetadata? = null
    override var serializedIr: SerializedIrModule? = null
    override var dataFlowGraph: ByteArray? = null

    override val standardLlvmSymbolsOrigin: CompiledKlibModuleOrigin
        get() = stdlibModule.llvmSymbolOrigin
    override val librariesWithDependencies by lazy {
        config.librariesWithDependencies(moduleDescriptor)
    }

    override var functionReferenceCount = 0
    override var coroutineCount = 0

    override lateinit var irModules: Map<String, IrModuleFragment>

    // TODO: make lateinit?
    override var irModule: IrModuleFragment? = null
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

    override val interopBuiltIns: InteropBuiltIns by lazy {
        InteropBuiltIns(this.builtIns)
    }

    override var llvmModule: LLVMModuleRef? = null
        set(module) {
            if (field != null) {
                throw Error("Another LLVMModule in the context.")
            }
            field = module!!

            llvm = Llvm(this, config, module)
            debugInfo = DebugInfo(this)
        }

    override lateinit var llvm: Llvm
    override val llvmImports: LlvmImports by lazy { Llvm.ImportsImpl(this.librariesWithDependencies) }
    override lateinit var llvmDeclarations: LlvmDeclarations
    override lateinit var bitcodeFileName: String
    lateinit var library: KonanLibraryLayout

    private var llvmDisposed = false

    override lateinit var necessaryLlvmParts: NecessaryLlvmParts

    fun disposeLlvm() {
        if (llvmDisposed) return
        if (::debugInfo.isInitialized)
            LLVMDisposeDIBuilder(debugInfo.builder)
        if (llvmModule != null)
            LLVMDisposeModule(llvmModule)
        if (::llvm.isInitialized) {
            LLVMDisposeTargetData(llvm.runtime.targetData)
            LLVMDisposeModule(llvm.runtime.llvmModule)
        }
        tryDisposeLLVMContext()
        llvmDisposed = true
    }

    override val cStubsManager = CStubsManager(config.target)

    override val coverage: CoverageManager = CoverageManager(this, config)

    override fun ghaEnabled(): Boolean = ::globalHierarchyAnalysisResult.isInitialized

    override lateinit var debugInfo: DebugInfo
    override var moduleDFG: ModuleDFG? = null
    override lateinit var lifetimes: MutableMap<IrElement, Lifetime>
    override lateinit var codegenVisitor: CodeGeneratorVisitor
    override var devirtualizationAnalysisResult: DevirtualizationAnalysis.AnalysisResult? = null

    override var referencedFunctions: Set<IrFunction>? = null
    override lateinit var compilerOutput: List<ObjectFile>

    override val llvmModuleSpecification: LlvmModuleSpecification by lazy {
        when {
            config.produce.isCache ->
                CacheLlvmModuleSpecification(config.cachedLibraries, config.libraryToCache!!.klib)

            else -> DefaultLlvmModuleSpecification(config.cachedLibraries)
        }
    }

    override val declaredLocalArrays: MutableMap<String, LLVMTypeRef> = HashMap()

    override lateinit var irLinker: KonanIrLinker

    override val inlineFunctionBodies = mutableListOf<SerializedInlineFunctionReference>()
    override val classFields = mutableListOf<SerializedClassFields>()
    override val calledFromExportedInlineFunctions = mutableSetOf<IrFunction>()
    override val constructedFromExportedInlineFunctions = mutableSetOf<IrClass>()

    override val targetAbiInfo: TargetAbiInfo by lazy {
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

internal class DescriptorsLookup(private val builtIns: KonanBuiltIns) {
    private val packageScope by lazy { builtIns.builtInsModule.getPackage(KonanFqNames.internalPackageName).memberScope }

    val nativePtr by lazy { packageScope.getContributedClassifier(NATIVE_PTR_NAME) as ClassDescriptor }
    val getNativeNullPtr by lazy { packageScope.getContributedFunctions("getNativeNullPtr").single() }
    val immutableBlobOf by lazy {
        builtIns.builtInsModule.getPackage(KonanFqNames.packageName).memberScope.getContributedFunctions("immutableBlobOf").single()
    }

    val interopBuiltIns by lazy {
        InteropBuiltIns(this.builtIns)
    }
}

private fun MemberScope.getContributedClassifier(name: String) =
        this.getContributedClassifier(Name.identifier(name), NoLookupLocation.FROM_BUILTINS)

private fun MemberScope.getContributedFunctions(name: String) =
        this.getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BUILTINS)

internal class ContextLogger(val context: LoggingContext) {
    operator fun String.unaryPlus() = context.log { this }
}

internal fun LoggingContext.logMultiple(messageBuilder: ContextLogger.() -> Unit) {
    if (!inVerbosePhase) return
    with(ContextLogger(this)) { messageBuilder() }
}

internal open class LazyMember<T>(val initializer: KonanBackendContext.() -> T) {
    operator fun getValue(thisRef: KonanBackendContext, property: KProperty<*>): T = thisRef.getValue(this)
}

internal class LazyVarMember<T>(initializer: KonanBackendContext.() -> T) : LazyMember<T>(initializer) {
    operator fun setValue(thisRef: KonanBackendContext, property: KProperty<*>, newValue: T) = thisRef.setValue(this, newValue)
}

internal object LazyMap {
    fun <T> lazyMember(initializer: KonanBackendContext.() -> T) = LazyMember<T>(initializer)

    fun <K, V> lazyMapMember(initializer: KonanBackendContext.(K) -> V): LazyMember<(K) -> V> = lazyMember {
        val storage = mutableMapOf<K, V>()
        val result: (K) -> V = {
            storage.getOrPut(it, { initializer(it) })
        }
        result
    }
}