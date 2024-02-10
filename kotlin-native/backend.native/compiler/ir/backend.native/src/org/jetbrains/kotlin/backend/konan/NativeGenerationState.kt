/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import llvm.*
import org.jetbrains.kotlin.backend.common.serialization.FingerprintHash
import org.jetbrains.kotlin.backend.common.serialization.Hash128Bits
import org.jetbrains.kotlin.backend.konan.driver.BasicPhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.utilities.BackendContextHolder
import org.jetbrains.kotlin.backend.konan.driver.utilities.LlvmIrHolder
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExport
import org.jetbrains.kotlin.backend.konan.serialization.SerializedClassFields
import org.jetbrains.kotlin.backend.konan.serialization.SerializedEagerInitializedFile
import org.jetbrains.kotlin.backend.konan.serialization.SerializedInlineFunctionReference
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrSuspensionPoint

internal class InlineFunctionOriginInfo(val irFunction: IrFunction, val irFile: IrFile, val startOffset: Int, val endOffset: Int)

internal class FileLowerState {
    private var functionReferenceCount = 0
    private var coroutineCount = 0
    private var cStubCount = 0

    fun getFunctionReferenceImplUniqueName(targetFunction: IrFunction): String =
            getFunctionReferenceImplUniqueName("${targetFunction.name}\$FUNCTION_REFERENCE\$")

    fun getCoroutineImplUniqueName(function: IrFunction): String =
            "${function.name}COROUTINE\$${coroutineCount++}"

    fun getFunctionReferenceImplUniqueName(prefix: String) =
            "$prefix${functionReferenceCount++}"

    fun getCStubUniqueName(prefix: String) =
            "$prefix${cStubCount++}"
}

internal interface BitcodePostProcessingContext : PhaseContext, LlvmIrHolder {
    val llvm: BasicLlvmHelpers
    val llvmContext: LLVMContextRef
}

internal class BitcodePostProcessingContextImpl(
        config: KonanConfig,
        override val llvmModule: LLVMModuleRef,
        override val llvmContext: LLVMContextRef
) : BitcodePostProcessingContext, BasicPhaseContext(config) {
    override val llvm: BasicLlvmHelpers = BasicLlvmHelpers(this, llvmModule)
}

internal class NativeGenerationState(
        config: KonanConfig,
        // TODO: Get rid of this property completely once transition to the dynamic driver is complete.
        //  It will reduce code coupling and make it easier to create NativeGenerationState instances.
        val context: Context,
        val cacheDeserializationStrategy: CacheDeserializationStrategy?,
        val dependenciesTracker: DependenciesTracker,
        val llvmModuleSpecification: LlvmModuleSpecification,
        val outputFiles: OutputFiles,
        val llvmModuleName: String,
) : BasicPhaseContext(config), BackendContextHolder<Context>, LlvmIrHolder, BitcodePostProcessingContext {
    val outputFile = outputFiles.mainFileName

    var klibHash: FingerprintHash = FingerprintHash(Hash128Bits(0U, 0U))

    val inlineFunctionBodies = mutableListOf<SerializedInlineFunctionReference>()
    val classFields = mutableListOf<SerializedClassFields>()
    val eagerInitializedFiles = mutableListOf<SerializedEagerInitializedFile>()
    val calledFromExportedInlineFunctions = mutableSetOf<IrFunction>()
    val constructedFromExportedInlineFunctions = mutableSetOf<IrClass>()
    val inlineFunctionOrigins = mutableMapOf<IrFunction, InlineFunctionOriginInfo>()
    val liveVariablesAtSuspensionPoints = mutableMapOf<IrSuspensionPoint, List<IrVariable>>()
    val visibleVariablesAtSuspensionPoints = mutableMapOf<IrSuspensionPoint, List<IrVariable>>()

    private val localClassNames = mutableMapOf<IrAttributeContainer, String>()
    fun getLocalClassName(container: IrAttributeContainer): String? = localClassNames[container.attributeOwnerId]
    fun putLocalClassName(container: IrAttributeContainer, name: String) {
        localClassNames[container.attributeOwnerId] = name
    }
    fun copyLocalClassName(source: IrAttributeContainer, destination: IrAttributeContainer) {
        getLocalClassName(source)?.let { name -> putLocalClassName(destination, name) }
    }

    lateinit var fileLowerState: FileLowerState

    val producedLlvmModuleContainsStdlib get() = llvmModuleSpecification.containsModule(context.stdlibModule)

    private val runtimeDelegate = lazy { Runtime(llvmContext, config.distribution.compilerInterface(config.target)) }
    private val llvmDelegate = lazy { CodegenLlvmHelpers(this, LLVMModuleCreateWithNameInContext(llvmModuleName, llvmContext)!!) }
    private val debugInfoDelegate = lazy { DebugInfo(this) }

    override val llvmContext = LLVMContextCreate()!!
    val runtime by runtimeDelegate
    override val llvm by llvmDelegate
    val debugInfo by debugInfoDelegate
    val cStubsManager = CStubsManager(config.target, this)
    lateinit var llvmDeclarations: LlvmDeclarations

    val virtualFunctionTrampolines = mutableMapOf<IrSimpleFunction, LlvmCallable>()

    lateinit var objCExport: ObjCExport

    fun hasDebugInfo() = debugInfoDelegate.isInitialized()

    private var isDisposed = false

    // Both NativeGenerationState and Context could be used for logging purposes.
    // Unfortunately, only NativeGenerationState is used as a PhaseContext, so logging in Context
    // will do nothing. Workaround that by setting inVerbosePhase of "parent" context.
    //
    // A proper solution would be decoupling of logging, error reporting, etc. into a separate (PhaseEnvironment?) object.
    override var inVerbosePhase: Boolean
        get() = super.inVerbosePhase
        set(value) {
            super.inVerbosePhase = value
            context.inVerbosePhase = value
        }

    override fun dispose() {
        if (isDisposed) return

        if (hasDebugInfo()) {
            LLVMDisposeDIBuilder(debugInfo.builder)
        }
        if (llvmDelegate.isInitialized()) {
            LLVMDisposeModule(llvm.module)
        }
        if (runtimeDelegate.isInitialized()) {
            LLVMDisposeTargetData(runtime.targetData)
            LLVMDisposeModule(runtime.llvmModule)
        }
        LLVMContextDispose(llvmContext)

        isDisposed = true
    }

    override val backendContext: Context
        get() = context

    override val llvmModule: LLVMModuleRef
        get() = llvm.module
}
