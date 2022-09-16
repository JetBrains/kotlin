/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.phases

import kotlinx.cinterop.CPointer
import llvm.LLVMDumpModule
import llvm.LLVMModuleRef
import llvm.LLVMOpaqueValue
import org.jetbrains.kotlin.backend.common.ErrorReportingContext
import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.backend.konan.ir.llvmSymbolOrigin
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.coverage.CoverageManager
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExport
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportCodegen
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamer
import org.jetbrains.kotlin.backend.konan.optimizations.DevirtualizationAnalysis
import org.jetbrains.kotlin.backend.konan.optimizations.ModuleDFG
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrLinker
import org.jetbrains.kotlin.backend.konan.serialization.SerializedClassFields
import org.jetbrains.kotlin.backend.konan.serialization.SerializedInlineFunctionReference
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.konan.CompiledKlibModuleOrigin
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.target.needSmallBinary
import org.jetbrains.kotlin.library.SerializedIrModule
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module

internal interface PhaseContext : LoggingContext, ErrorReportingContext {
    val config: KonanConfig
    val memoryModel get() = config.memoryModel

    val messageCollector: MessageCollector

    override fun report(element: IrElement?, irFile: IrFile?, message: String, isError: Boolean) {
        val location = element?.getCompilerMessageLocation(irFile ?: error("irFile should be not null for $element"))
        this.messageCollector.report(
                if (isError) CompilerMessageSeverity.ERROR else CompilerMessageSeverity.WARNING,
                message, location
        )
    }
}

internal interface BackendPhaseContext : PhaseContext, KonanBackendContext {
    override val configuration get() = config.configuration

    val isNativeLibrary: Boolean

    override fun report(element: IrElement?, irFile: IrFile?, message: String, isError: Boolean)

    val standardLlvmSymbolsOrigin: CompiledKlibModuleOrigin
        get() = stdlibModule.llvmSymbolOrigin

    val librariesWithDependencies: List<KonanLibrary>

    val llvmImports: LlvmImports
}

internal val KonanBackendContext.stdlibModule
    get() = this.builtIns.any.module

fun ConfigChecks(config: KonanConfig): ConfigChecks = object : ConfigChecks {
    override val config: KonanConfig = config
}

interface ConfigChecks {
    val config: KonanConfig

    fun shouldExportKDoc() = config.configuration.getBoolean(KonanConfigKeys.EXPORT_KDOC)

    fun shouldVerifyBitCode() = config.configuration.getBoolean(KonanConfigKeys.VERIFY_BITCODE)

    fun shouldPrintBitCode() = config.configuration.getBoolean(KonanConfigKeys.PRINT_BITCODE)

    fun shouldPrintFiles() = config.configuration.getBoolean(KonanConfigKeys.PRINT_FILES)

    fun shouldContainDebugInfo() = config.debug

    fun shouldContainLocationDebugInfo() = shouldContainDebugInfo() || config.lightDebug

    fun shouldContainAnyDebugInfo() = shouldContainDebugInfo() || shouldContainLocationDebugInfo()

    fun shouldUseDebugInfoFromNativeLibs() = shouldContainAnyDebugInfo() && config.useDebugInfoInNativeLibs

    fun shouldOptimize() = config.optimizationsEnabled

    fun shouldInlineSafepoints() = !config.target.needSmallBinary()

    fun useLazyFileInitializers() = config.propertyLazyInitialization
}

internal interface FrontendContext : PhaseContext {
    var environment: KotlinCoreEnvironment

    var moduleDescriptor: ModuleDescriptor

    var bindingContext: BindingContext

    var frontendServices: FrontendServices
}

// TODO: Consider component-based approach
internal interface PsiToIrContext :
        PhaseContext,
        LlvmModuleSpecificationComponent
{
    val reflectionTypes: KonanReflectionTypes

    val builtIns: KonanBuiltIns
}

// We don't need this interface if we have a proper phase system
internal interface ObjCExportContext : PsiToIrContext {
    var objCExport: ObjCExport
}

// We don't need this interface if we have a proper phase system
internal interface CExportContext : PsiToIrContext {
    var cAdapterGenerator: CAdapterGenerator
}

internal interface LlvmModuleSpecificationComponent {
    val llvmModuleSpecification: LlvmModuleSpecification

}

internal interface LlvmModuleContext : BackendPhaseContext {
    var llvmModule: LLVMModuleRef?

    fun verifyBitCode() {
        if (llvmModule == null) return
        verifyModule(llvmModule!!)
    }

    fun printBitCode() {
        if (llvmModule == null) return
        LLVMDumpModule(llvmModule!!)
    }
}

data class SerializationResult(
        val serializedMetadata: SerializedMetadata?,
        val serializedIr: SerializedIrModule?,
        val dataFlowGraph: ByteArray?,
        val neededLibraries: List<KonanLibrary>
)

internal interface BitcodegenContext :
        BackendPhaseContext,
        LayoutBuildingContext,
        BridgesAwareContext,
        LocalClassNameAwareContext,
        LlvmModuleContext,
        LlvmModuleSpecificationComponent,
        ConfigChecks,
        ClassITablePlacerContext,
        ClassFieldsLayoutContext,
        ClassIdComputerContext,
        ClassVTableEntriesContext,
        CacheContext {
    override val config: KonanConfig

    val irLinker: KonanIrLinker

    val irModule: IrModuleFragment?

    val coverage: CoverageManager

    val llvm: Llvm

    var necessaryLlvmParts: NecessaryLlvmParts

    val targetAbiInfo: TargetAbiInfo

    var llvmDeclarations: LlvmDeclarations

    val cAdapterGenerator: CAdapterGenerator?

    val debugInfo: DebugInfo

    val enumsSupport: EnumsSupport

    val interopBuiltIns: InteropBuiltIns

    val objCExport: ObjCExport?

    val globalHierarchyAnalysisResult: GlobalHierarchyAnalysisResult

    val referencedFunctions: Set<IrFunction>?

    val lifetimes: Map<IrElement, Lifetime>

    fun ghaEnabled(): Boolean

    fun objcExportCodegen(
            objCExport: ObjCExport?,
            codegen: CodeGenerator,
    ) {
        ObjCExportCodegen(this, objCExport, config).generate(codegen)
    }

    val contextUtils: ContextUtils
        get() = ContextUtils(this)

    fun ContextUtils.unique(kind: UniqueKind): ConstPointer {
        val descriptor = when (kind) {
            UniqueKind.UNIT -> ir.symbols.unit.owner
            UniqueKind.EMPTY_ARRAY -> ir.symbols.array.owner
        }
        return if (isExternal(descriptor)) {
            constPointer(importGlobal(
                    kind.llvmName, llvm.runtime.objHeaderType, origin = descriptor.llvmSymbolOrigin
            ))
        } else {
            llvmDeclarations.forUnique(kind).pointer
        }
    }
}

internal interface BridgesAwareContext : BackendPhaseContext {
    val bridgesSupport: BridgesSupport
}

internal interface LlvmCodegenContext : BackendPhaseContext, LlvmModuleSpecificationComponent, LlvmModuleContext {

    val llvm: Llvm

    val objCExportNamer: ObjCExportNamer?

    val cStubsManager: CStubsManager

    var bitcodeFileName: String

    val coverage: CoverageManager

    val runtimeAnnotationMap: Map<String, List<CPointer<LLVMOpaqueValue>>>
}

internal interface LayoutBuildingContext : BackendPhaseContext {
    fun getLayoutBuilder(irClass: IrClass): ClassLayoutBuilder
}

internal interface ClassVTableEntriesContext : BackendPhaseContext {
    fun getClassVTableEntries(irClass: IrClass): ClassVTableEntries
}

internal interface ClassITablePlacerContext : BackendPhaseContext {
    fun getClassITablePlacer(irClass: IrClass): ClassITablePlacer
}

internal interface ClassFieldsLayoutContext : BackendPhaseContext {
    fun getClassFieldLayout(irClass: IrClass): ClassFieldsLayout
}

internal interface ClassIdComputerContext : BackendPhaseContext {
    fun getClassId(irClass: IrClass): ClassIdComputer
}

internal interface IrLinkerComponent {
    var irLinker: KonanIrLinker
}

internal interface InnerClassesSupportComponent {
    val innerClassesSupport: InnerClassesSupport
}

internal interface LocalClassNameAwareContext : BackendPhaseContext {

    val localClassNames: MutableMap<IrAttributeContainer, String>

    fun getLocalClassName(container: IrAttributeContainer): String? = localClassNames[container.attributeOwnerId]

    fun putLocalClassName(container: IrAttributeContainer, name: String) {
        localClassNames[container.attributeOwnerId] = name
    }

    fun copyLocalClassName(source: IrAttributeContainer, destination: IrAttributeContainer) {
        getLocalClassName(source)?.let { name -> putLocalClassName(destination, name) }
    }
}

internal interface LtoContext :
        BackendPhaseContext,
        LayoutBuildingContext,
        ClassIdComputerContext,
        ClassVTableEntriesContext,
        ClassITablePlacerContext,
        BridgesAwareContext,
        ConfigChecks {
    val irModule: IrModuleFragment?

    var globalHierarchyAnalysisResult: GlobalHierarchyAnalysisResult

    fun ghaEnabled(): Boolean

    var devirtualizationAnalysisResult: DevirtualizationAnalysis.AnalysisResult?

    var moduleDFG: ModuleDFG?

    var referencedFunctions: Set<IrFunction>?

    val lifetimes: MutableMap<IrElement, Lifetime>
}

internal interface MiddleEndContext :
        PhaseContext,
        LlvmModuleSpecificationComponent,
        LocalClassNameAwareContext,
        BridgesAwareContext,
        CacheAwareContext,
        IrLinkerComponent {
    val irModule: IrModuleFragment?

    val interopBuiltIns: InteropBuiltIns

    val enumsSupport: EnumsSupport

    val cachesAbiSupport: CachesAbiSupport

    val inlineFunctionsSupport: InlineFunctionsSupport

    val cStubsManager: CStubsManager

    var functionReferenceCount: Int
    var coroutineCount: Int

    val testCasesToDump: MutableMap<ClassId, MutableCollection<String>>

    var moduleDescriptor: ModuleDescriptor

    var irModules: Map<String, IrModuleFragment>
}

internal interface ObjectFilesContext : PhaseContext {
    // Should be a phase output
    var compilerOutput: List<ObjectFile>
}

internal interface LinkerContext : PhaseContext {
    val llvmModuleSpecification: LlvmModuleSpecification

    val necessaryLlvmParts: NecessaryLlvmParts

    val coverage: CoverageManager
}

internal interface CacheContext : PhaseContext {
    val inlineFunctionBodies: MutableList<SerializedInlineFunctionReference>
    val classFields: MutableList<SerializedClassFields>
    val constructedFromExportedInlineFunctions: MutableSet<IrClass>
    val calledFromExportedInlineFunctions: MutableSet<IrFunction>
    val llvmImports: LlvmImports
}

internal interface CacheAwareContext : CacheContext, InnerClassesSupportComponent, BackendPhaseContext, ClassFieldsLayoutContext