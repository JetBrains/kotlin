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
import org.jetbrains.kotlin.backend.konan.ir.KonanIr
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.lower.BridgesSupport
import org.jetbrains.kotlin.backend.konan.lower.EnumsSupport
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrLinker
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.konan.CompiledKlibModuleOrigin
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

internal interface PhaseContext : KonanBackendContextI

internal typealias ErrorReportingContext = KonanBackendContextI

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

    val config: KonanConfig

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

    override val llvmModuleSpecification: LlvmModuleSpecification

    override val builtIns: KonanBuiltIns

    val reflectionTypes: KonanReflectionTypes

    var irModules: Map<String, IrModuleFragment>

    // TODO: make lateinit?
    var irModule: IrModuleFragment?

    var irLinker: KonanIrLinker

    override var ir: KonanIr

    var expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>
}

internal interface ObjCExportContext : PsiToIrContext {
    var objCExport: ObjCExport
}

internal interface TopDownAnalyzerContext : PhaseContext, ConfigChecks {
    var frontendServices: FrontendServices
}

internal interface LlvmModuleContext : PhaseContext {
    var llvmModule: LLVMModuleRef?

    var llvm: Llvm

    var debugInfo: DebugInfo

    fun verifyBitCode() {
        if (llvmModule == null) return
        verifyModule(llvmModule!!)
    }

    fun printBitCode() {
        if (llvmModule == null) return
        LLVMDumpModule(llvmModule!!)
    }

}

internal interface LlvmModuleSpecificationContext : PhaseContext {

    val config: KonanConfig

    val llvmModuleSpecification: LlvmModuleSpecification

}

internal interface KlibProducingContext : PhaseContext {
    val config: KonanConfig

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
        LlvmModuleSpecificationContext,
        LtoAwareContext {
    override val config: KonanConfig

    val llvmModule: LLVMModuleRef?

    val llvm: Llvm

    val targetAbiInfo: TargetAbiInfo

    val llvmDeclarations: LlvmDeclarations

    val irLinker: KonanIrLinker

    override val llvmModuleSpecification: LlvmModuleSpecification

    val debugInfo: DebugInfo

    val standardLlvmSymbolsOrigin: CompiledKlibModuleOrigin

    override val ir: KonanIr

    val llvmImports: LlvmImports

    val librariesWithDependencies: List<KonanLibrary>

    val constructedFromExportedInlineFunctions: Set<IrClass>
    val calledFromExportedInlineFunctions: Set<IrFunction>

    val enumsSupport: EnumsSupport

    val referencedFunctions: Set<IrFunction>?

    val declaredLocalArrays: MutableMap<String, LLVMTypeRef>

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

internal interface LlvmCodegenContext : PhaseContext, LlvmModuleSpecificationContext {

    val llvmModule: LLVMModuleRef?

    val llvm: Llvm

    val objCExportNamer: ObjCExportNamer
}

internal interface LayoutBuildingContext : PhaseContext {
    val config: KonanConfig

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

internal interface LtoAwareContext : PhaseContext {
    var globalHierarchyAnalysisResult: GlobalHierarchyAnalysisResult

    fun ghaEnabled(): Boolean
}