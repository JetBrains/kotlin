/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.phases

import kotlinx.cinterop.CPointer
import llvm.LLVMGetInitializer
import llvm.LLVMGetOperand
import llvm.LLVMModuleRef
import llvm.LLVMOpaqueValue
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.backend.konan.ir.KonanIr
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.coverage.CoverageManager
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExport
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamer
import org.jetbrains.kotlin.backend.konan.optimizations.DevirtualizationAnalysis
import org.jetbrains.kotlin.backend.konan.optimizations.ModuleDFG
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrLinker
import org.jetbrains.kotlin.backend.konan.serialization.SerializedClassFields
import org.jetbrains.kotlin.backend.konan.serialization.SerializedInlineFunctionReference
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.BindingContext

internal data class BasicPhaseContextPayload(
        override val config: KonanConfig,
        override val builtIns: KonanBuiltIns,
        val irModuleFragment: IrModuleFragment,
        override val lazyValues: MutableMap<LazyMember<*>, Any?> = mutableMapOf(),
        val librariesWithDependencies: List<KonanLibrary>
) : AbstractKonanBackendContext(config) {
    override val configuration: CompilerConfiguration
        get() = config.configuration

    override val ir = KonanIr(this, irModuleFragment)

    override val typeSystem: IrTypeSystemContext = IrTypeSystemContextImpl(irModuleFragment.irBuiltins)

    override val irBuiltIns: IrBuiltIns
        get() = irModuleFragment.irBuiltins
}

internal open class BasicBackendPhaseContext(
        val payload: BasicPhaseContextPayload,
) : AbstractKonanBackendContext(payload.config), BackendPhaseContext {
    override val irBuiltIns: IrBuiltIns
        get() = payload.irBuiltIns
    override val typeSystem: IrTypeSystemContext
        get() = payload.typeSystem
    override val configuration: CompilerConfiguration
        get() = payload.configuration
    override val lazyValues: MutableMap<LazyMember<*>, Any?>
        get() = payload.lazyValues
    override val builtIns: KonanBuiltIns
        get() = payload.builtIns
    override val ir: KonanIr
        get() = payload.ir

    override val isNativeLibrary: Boolean by lazy {
        val kind = config.configuration.get(KonanConfigKeys.PRODUCE)
        kind == CompilerOutputKind.DYNAMIC || kind == CompilerOutputKind.STATIC
    }

    override val llvmImports: LlvmImports by lazy { Llvm.ImportsImpl(this.librariesWithDependencies) }

    override val librariesWithDependencies: List<KonanLibrary>
        get() = payload.librariesWithDependencies
}

internal open class BasicPhaseContext(
        override val config: KonanConfig,
) : PhaseContext {
    override var inVerbosePhase = false
    override fun log(message: () -> String) {
        if (inVerbosePhase) {
            println(message())
        }
    }

    override val messageCollector: MessageCollector
        get() = config.configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
}

internal class FrontendContextImpl(
        config: KonanConfig,
        override var environment: KotlinCoreEnvironment,
) : FrontendContext, BasicPhaseContext(config) {
    override lateinit var moduleDescriptor: ModuleDescriptor
    override lateinit var bindingContext: BindingContext
    override lateinit var frontendServices: FrontendServices
}

internal class PsiToIrContextImpl(
        override val config: KonanConfig,
        moduleDescriptor: ModuleDescriptor,
) : BasicPhaseContext(config), PsiToIrContext {
    override val reflectionTypes: KonanReflectionTypes by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KonanReflectionTypes(moduleDescriptor)
    }

    override val builtIns: KonanBuiltIns by lazy(LazyThreadSafetyMode.PUBLICATION) {
        moduleDescriptor.builtIns as KonanBuiltIns
    }

    override val llvmModuleSpecification: LlvmModuleSpecification by lazy {
        when {
            config.produce.isCache ->
                CacheLlvmModuleSpecification(config.cachedLibraries, config.libraryToCache!!.klib)

            else -> DefaultLlvmModuleSpecification(config.cachedLibraries)
        }
    }
}

internal class KlibProducingContextImpl(
        payload: BasicPhaseContextPayload
) : BasicBackendPhaseContext(payload), PhaseContext

internal class LlvmCodegenContextImpl(
        payload: BasicPhaseContextPayload,
        override val cStubsManager: CStubsManager,
        override val objCExportNamer: ObjCExportNamer?,
        override val llvm: Llvm,
        override val coverage: CoverageManager,
        override var llvmModule: LLVMModuleRef?,
        override val llvmModuleSpecification: LlvmModuleSpecification
) : BasicBackendPhaseContext(payload), LlvmCodegenContext {
    override var bitcodeFileName: String = ""

    override val runtimeAnnotationMap: Map<String, List<CPointer<LLVMOpaqueValue>>> by lazy {
        llvm.staticData.getGlobal("llvm.global.annotations")
                ?.getInitializer()
                ?.let { getOperands(it) }
                ?.groupBy(
                        { LLVMGetInitializer(LLVMGetOperand(LLVMGetOperand(it, 1), 0))?.getAsCString() ?: "" },
                        { LLVMGetOperand(LLVMGetOperand(it, 0), 0)!! }
                )
                ?.filterKeys { it != "" }
                ?: emptyMap()
    }
}

internal class ObjectFilesContextImpl(
        config: KonanConfig,
) : BasicPhaseContext(config), ObjectFilesContext {
    override var compilerOutput: List<ObjectFile> = emptyList()
}

internal class LinkerContextImpl(
        config: KonanConfig,
        override val necessaryLlvmParts: NecessaryLlvmParts,
        override val coverage: CoverageManager,
        override val llvmModuleSpecification: LlvmModuleSpecification,
) : BasicPhaseContext(config), LinkerContext

internal class CacheContextImpl(
        config: KonanConfig,
        override val inlineFunctionBodies: MutableList<SerializedInlineFunctionReference>,
        override val classFields: MutableList<SerializedClassFields>,
        override val llvmImports: LlvmImports,
        override val constructedFromExportedInlineFunctions: MutableSet<IrClass>,
        override val calledFromExportedInlineFunctions: MutableSet<IrFunction>,
) : BasicPhaseContext(config), CacheContext

internal fun CacheContextImpl(config: KonanConfig, previous: CacheContext) = CacheContextImpl(
        config,
        previous.inlineFunctionBodies,
        previous.classFields,
        previous.llvmImports,
        previous.constructedFromExportedInlineFunctions,
        previous.calledFromExportedInlineFunctions
)

internal class LtoContextImpl(
        payload: BasicPhaseContextPayload,
        override val bridgesSupport: BridgesSupport,
        override val irModule: IrModuleFragment,
        private val classIdComputerHolder: SmartHolder<ClassIdComputer>,
        private val classITablePlacer: SmartHolder<ClassITablePlacer>,
        private val classVTableEntries: SmartHolder<ClassVTableEntries>,
        private val layoutBuildersHolder: SmartHolder<ClassLayoutBuilder>,
) : BasicBackendPhaseContext(payload), LtoContext {

    override fun getLayoutBuilder(irClass: IrClass): ClassLayoutBuilder {
        return layoutBuildersHolder.get(irClass)
    }

    override fun getClassVTableEntries(irClass: IrClass): ClassVTableEntries =
            classVTableEntries.get(irClass)

    override fun getClassITablePlacer(irClass: IrClass): ClassITablePlacer =
            classITablePlacer.get(irClass)

    override fun getClassId(irClass: IrClass): ClassIdComputer =
            classIdComputerHolder.get(irClass)

    override var globalHierarchyAnalysisResult: GlobalHierarchyAnalysisResult = GlobalHierarchyAnalysisResult(-1)

    override fun ghaEnabled(): Boolean =
            globalHierarchyAnalysisResult.valid()

    override var devirtualizationAnalysisResult: DevirtualizationAnalysis.AnalysisResult? = null

    override var moduleDFG: ModuleDFG? = null
    override var referencedFunctions: Set<IrFunction>? = null
    override var lifetimes: MutableMap<IrElement, Lifetime> = mutableMapOf()
}

internal class LtoResults(
        val globalHierarchyAnalysisResult: GlobalHierarchyAnalysisResult,
        val ghaEnabled: Boolean,
        val devirtualizationAnalysisResult: DevirtualizationAnalysis.AnalysisResult?,
        val moduleDFG: ModuleDFG?,
        val referencedFunctions: Set<IrFunction>?,
        val lifetimes: Map<IrElement, Lifetime>,
) {
    companion object {
        val EMPTY = LtoResults(GlobalHierarchyAnalysisResult(-1), false, null, null, null, mapOf())
    }
}

internal class SmartHoldersCollection(
        val classIdComputerHolder: SmartHolder<ClassIdComputer>,
        val classITablePlacer: SmartHolder<ClassITablePlacer>,
        val classVTableEntries: SmartHolder<ClassVTableEntries>,
        val classFieldsLayout: SmartHolder<ClassFieldsLayout>,
        val layoutBuildersHolder: SmartHolder<ClassLayoutBuilder>,
)

internal class BitcodegenContextImpl(
        payload: BasicPhaseContextPayload,
        override val llvmModuleSpecification: LlvmModuleSpecification,
        override var llvmModule: LLVMModuleRef?,
        override val irModule: IrModuleFragment,
        override val irLinker: KonanIrLinker,
        override val coverage: CoverageManager,
        override val llvm: Llvm,
        private val smartHoldersCollection: SmartHoldersCollection,
        private val ltoResults: LtoResults,
        override val cAdapterGenerator: CAdapterGenerator? = null,
        override val objCExport: ObjCExport? = null,
        override val debugInfo: DebugInfo,
        override val localClassNames: MutableMap<IrAttributeContainer, String>,
        override val bridgesSupport: BridgesSupport,
        override val enumsSupport: EnumsSupport,
        private val cacheContext: CacheContext,
) : BasicBackendPhaseContext(payload), BitcodegenContext {

    override var necessaryLlvmParts: NecessaryLlvmParts = NecessaryLlvmParts(emptySet(), emptySet(), emptySet())

    override var llvmDeclarations: LlvmDeclarations = LlvmDeclarations(emptyMap())

    override val interopBuiltIns: InteropBuiltIns by lazy {
        InteropBuiltIns(this.builtIns)
    }

    override val inlineFunctionBodies: MutableList<SerializedInlineFunctionReference>
        get() = cacheContext.inlineFunctionBodies
    override val classFields: MutableList<SerializedClassFields>
        get() = cacheContext.classFields
    override val constructedFromExportedInlineFunctions: MutableSet<IrClass>
        get() = cacheContext.constructedFromExportedInlineFunctions
    override val calledFromExportedInlineFunctions: MutableSet<IrFunction>
        get() = cacheContext.calledFromExportedInlineFunctions

    override fun getLayoutBuilder(irClass: IrClass): ClassLayoutBuilder =
            smartHoldersCollection.layoutBuildersHolder.get(irClass)

    override fun getClassVTableEntries(irClass: IrClass): ClassVTableEntries =
            smartHoldersCollection.classVTableEntries.get(irClass)

    override fun getClassITablePlacer(irClass: IrClass): ClassITablePlacer =
            smartHoldersCollection.classITablePlacer.get(irClass)

    override fun getClassFieldLayout(irClass: IrClass): ClassFieldsLayout =
            smartHoldersCollection.classFieldsLayout.get(irClass)

    override fun getClassId(irClass: IrClass): ClassIdComputer =
            smartHoldersCollection.classIdComputerHolder.get(irClass)

    override val globalHierarchyAnalysisResult: GlobalHierarchyAnalysisResult =
            ltoResults.globalHierarchyAnalysisResult

    override val lifetimes: Map<IrElement, Lifetime> =
            ltoResults.lifetimes

    override val contextUtils: ContextUtils by lazy {
        ContextUtils(this)
    }

    override fun ghaEnabled(): Boolean =
            ltoResults.ghaEnabled

    override val referencedFunctions: Set<IrFunction>?
        get() = ltoResults.referencedFunctions

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

internal class MiddleEndContextImpl(
        payload: BasicPhaseContextPayload,
        psiToIrResult: PsiToIrResult,
) : BasicBackendPhaseContext(payload), MiddleEndContext {
    override val llvmModuleSpecification: LlvmModuleSpecification
        get() = TODO("Not yet implemented")
    override val bridgesSupport: BridgesSupport
        get() = TODO("Not yet implemented")

    override fun getClassFieldLayout(irClass: IrClass): ClassFieldsLayout {
        TODO("Not yet implemented")
    }

    override var irLinker: KonanIrLinker
        get() = TODO("Not yet implemented")
        set(value) {}
    override val innerClassesSupport: InnerClassesSupport
        get() = TODO("Not yet implemented")
    override val localClassNames: MutableMap<IrAttributeContainer, String>
        get() = TODO("Not yet implemented")
    override val irModule: IrModuleFragment?
        get() = TODO("Not yet implemented")
    override val interopBuiltIns: InteropBuiltIns
        get() = TODO("Not yet implemented")
    override val enumsSupport: EnumsSupport
        get() = TODO("Not yet implemented")
    override val cachesAbiSupport: CachesAbiSupport
        get() = TODO("Not yet implemented")
    override val inlineFunctionsSupport: InlineFunctionsSupport
        get() = TODO("Not yet implemented")
    override val cStubsManager: CStubsManager
        get() = TODO("Not yet implemented")
    override var functionReferenceCount: Int
        get() = TODO("Not yet implemented")
        set(value) {}
    override var coroutineCount: Int
        get() = TODO("Not yet implemented")
        set(value) {}
    override val testCasesToDump: MutableMap<ClassId, MutableCollection<String>>
        get() = TODO("Not yet implemented")
    override var moduleDescriptor: ModuleDescriptor
        get() = TODO("Not yet implemented")
        set(value) {}
    override var irModules: Map<String, IrModuleFragment>
        get() = TODO("Not yet implemented")
        set(value) {}
    override val inlineFunctionBodies: MutableList<SerializedInlineFunctionReference>
        get() = TODO("Not yet implemented")
    override val classFields: MutableList<SerializedClassFields>
        get() = TODO("Not yet implemented")
    override val constructedFromExportedInlineFunctions: MutableSet<IrClass>
        get() = TODO("Not yet implemented")
    override val calledFromExportedInlineFunctions: MutableSet<IrFunction>
        get() = TODO("Not yet implemented")
}