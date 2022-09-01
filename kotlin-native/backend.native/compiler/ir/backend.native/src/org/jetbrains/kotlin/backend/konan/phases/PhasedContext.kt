/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.phases

import llvm.LLVMDumpModule
import llvm.LLVMModuleRef
import llvm.LLVMTypeRef
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.ClassLayoutBuilder
import org.jetbrains.kotlin.backend.konan.descriptors.GlobalHierarchyAnalysisResult
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.coverage.CoverageManager
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.backend.konan.optimizations.DevirtualizationAnalysis
import org.jetbrains.kotlin.backend.konan.optimizations.ModuleDFG
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrLinker
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.konan.CompiledKlibModuleOrigin
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.target.needSmallBinary
import org.jetbrains.kotlin.library.SerializedIrModule
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module

internal interface PhaseContext : KonanBackendContext {
    val config: KonanConfig
}

internal typealias ErrorReportingContext = KonanBackendContext

internal val PhaseContext.stdlibModule
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
        FrontendContext,
        LlvmModuleSpecificationContext {
    var symbolTable: SymbolTable?

    val reflectionTypes: KonanReflectionTypes

    var irModules: Map<String, IrModuleFragment>

    // TODO: make lateinit?
    var irModule: IrModuleFragment?

    var irLinker: KonanIrLinker

    var expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>
}

internal interface ObjCExportContext : PsiToIrContext {
    var objCExport: ObjCExport
}

internal interface TopDownAnalyzerContext : PhaseContext, ConfigChecks {
    var frontendServices: FrontendServices
}

internal interface LlvmModuleSpecificationContext : PhaseContext {
    val llvmModuleSpecification: LlvmModuleSpecification

}

internal interface LlvmModuleContext : PhaseContext {
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

internal interface KlibProducingContext : PhaseContext {
    val moduleDescriptor: ModuleDescriptor

    val irModule: IrModuleFragment?

    val expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>

    // We serialize untouched descriptor tree and IR.
    // But we have to wait until the code generation phase,
    // to dump this information into generated file.
    var serializedMetadata: SerializedMetadata?
    var serializedIr: SerializedIrModule?
    var dataFlowGraph: ByteArray?

    val librariesWithDependencies: List<KonanLibrary>
}

internal interface BitcodegenContext :
        PhaseContext,
        LayoutBuildingContext,
        BridgesAwareContext,
        LocalClassNameAwareContext,
        LlvmModuleContext,
        LlvmModuleSpecificationContext,
        LtoContext {
    override val config: KonanConfig

    val llvm: Llvm

    val targetAbiInfo: TargetAbiInfo

    var llvmDeclarations: LlvmDeclarations

    val irLinker: KonanIrLinker

    val debugInfo: DebugInfo

    val standardLlvmSymbolsOrigin: CompiledKlibModuleOrigin

    val llvmImports: LlvmImports

    val librariesWithDependencies: List<KonanLibrary>

    val constructedFromExportedInlineFunctions: Set<IrClass>
    val calledFromExportedInlineFunctions: Set<IrFunction>

    val enumsSupport: EnumsSupport

    val declaredLocalArrays: MutableMap<String, LLVMTypeRef>

    var codegenVisitor: CodeGeneratorVisitor

    val interopBuiltIns: InteropBuiltIns

    fun objcExportCodegen(
            exportedInterface: ObjCExportedInterface,
            codeSpec: ObjCExportCodeSpec,
            codegen: CodeGenerator,
    ) {
        ObjCExportCodegen(this, exportedInterface, codeSpec, config).generate(codegen)
    }
}

internal interface BridgesAwareContext : PhaseContext {
    val bridgesSupport: BridgesSupport
}

internal interface LlvmCodegenContext : PhaseContext, LlvmModuleSpecificationContext, LlvmModuleContext {

    val llvm: Llvm

    val objCExportNamer: ObjCExportNamer

    val cStubsManager: CStubsManager

    val coverage: CoverageManager
}

internal interface LayoutBuildingContext : PhaseContext {
    fun getLayoutBuilder(irClass: IrClass): ClassLayoutBuilder
}

internal interface LocalClassNameAwareContext : PhaseContext {

    val localClassNames: MutableMap<IrAttributeContainer, String>

    fun getLocalClassName(container: IrAttributeContainer): String? = localClassNames[container.attributeOwnerId]

    fun putLocalClassName(container: IrAttributeContainer, name: String) {
        localClassNames[container.attributeOwnerId] = name
    }

    fun copyLocalClassName(source: IrAttributeContainer, destination: IrAttributeContainer) {
        getLocalClassName(source)?.let { name -> putLocalClassName(destination, name) }
    }
}

internal interface LtoContext : PhaseContext, LayoutBuildingContext, IrModuleContext {
    var globalHierarchyAnalysisResult: GlobalHierarchyAnalysisResult

    fun ghaEnabled(): Boolean

    var devirtualizationAnalysisResult: DevirtualizationAnalysis.AnalysisResult?

    var moduleDFG: ModuleDFG?

    var referencedFunctions: Set<IrFunction>?

    var lifetimes: MutableMap<IrElement, Lifetime>
}

internal interface IrModuleContext : PhaseContext {
    val irModule: IrModuleFragment?
}

internal interface MiddleEndContext :
        PhaseContext,
        LlvmModuleSpecificationContext,
        LocalClassNameAwareContext,
        BridgesAwareContext,
        IrModuleContext
{
    override val config: KonanConfig

    val interopBuiltIns: InteropBuiltIns

    val enumsSupport: EnumsSupport

    val cachesAbiSupport: CachesAbiSupport

    val inlineFunctionsSupport: InlineFunctionsSupport

    val innerClassesSupport: InnerClassesSupport

    val llvmImports: LlvmImports

    val cStubsManager: CStubsManager

    var functionReferenceCount: Int
    var coroutineCount: Int
}