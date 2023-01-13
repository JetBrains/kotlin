/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import llvm.*
import org.jetbrains.kotlin.backend.konan.driver.BasicPhaseContext
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.coverage.CoverageManager
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExport
import org.jetbrains.kotlin.backend.konan.serialization.SerializedClassFields
import org.jetbrains.kotlin.backend.konan.serialization.SerializedEagerInitializedFile
import org.jetbrains.kotlin.backend.konan.serialization.SerializedInlineFunctionReference
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.konan.TempFiles
import org.jetbrains.kotlin.konan.file.File

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

internal class NativeGenerationState(
        config: KonanConfig,
        // TODO: Get rid of this property completely once transition to the dynamic driver is complete.
        //  It will reduce code coupling and make it easier to create NativeGenerationState instances.
        val context: Context,
        val cacheDeserializationStrategy: CacheDeserializationStrategy?
) : BasicPhaseContext(config) {
    private val outputPath = config.cacheSupport.tryGetImplicitOutput(cacheDeserializationStrategy) ?: config.outputPath
    val outputFiles = OutputFiles(outputPath, config.target, config.produce)
    val tempFiles = run {
        val pathToTempDir = config.configuration.get(KonanConfigKeys.TEMPORARY_FILES_DIR)?.let {
            val singleFileStrategy = cacheDeserializationStrategy as? CacheDeserializationStrategy.SingleFile
            if (singleFileStrategy == null)
                it
            else File(it, CacheSupport.cacheFileId(singleFileStrategy.fqName, singleFileStrategy.filePath)).path
        }
        TempFiles(outputFiles.outputName, pathToTempDir)
    }
    val outputFile = outputFiles.mainFileName

    val inlineFunctionBodies = mutableListOf<SerializedInlineFunctionReference>()
    val classFields = mutableListOf<SerializedClassFields>()
    val eagerInitializedFiles = mutableListOf<SerializedEagerInitializedFile>()
    val calledFromExportedInlineFunctions = mutableSetOf<IrFunction>()
    val constructedFromExportedInlineFunctions = mutableSetOf<IrClass>()
    val loweredInlineFunctions = mutableMapOf<IrFunction, InlineFunctionOriginInfo>()

    private val localClassNames = mutableMapOf<IrAttributeContainer, String>()
    fun getLocalClassName(container: IrAttributeContainer): String? = localClassNames[container.attributeOwnerId]
    fun putLocalClassName(container: IrAttributeContainer, name: String) {
        localClassNames[container.attributeOwnerId] = name
    }
    fun copyLocalClassName(source: IrAttributeContainer, destination: IrAttributeContainer) {
        getLocalClassName(source)?.let { name -> putLocalClassName(destination, name) }
    }

    lateinit var fileLowerState: FileLowerState

    val llvmModuleSpecification by lazy {
        if (config.produce.isCache)
            CacheLlvmModuleSpecification(this, config.cachedLibraries,
                    PartialCacheInfo(config.libraryToCache!!.klib, cacheDeserializationStrategy!!))
        else DefaultLlvmModuleSpecification(config.cachedLibraries)
    }

    val producedLlvmModuleContainsStdlib get() = llvmModuleSpecification.containsModule(context.stdlibModule)

    val dependenciesTracker: DependenciesTracker = DependenciesTrackerImpl(this)

    private val runtimeDelegate = lazy { Runtime(llvmContext, config.distribution.compilerInterface(config.target)) }
    private val llvmDelegate = lazy { Llvm(this, LLVMModuleCreateWithNameInContext("out", llvmContext)!!) }
    private val debugInfoDelegate = lazy { DebugInfo(this) }

    val llvmContext = LLVMContextCreate()!!
    val runtime by runtimeDelegate
    val llvm by llvmDelegate
    val debugInfo by debugInfoDelegate
    val cStubsManager = CStubsManager(config.target, this)
    lateinit var llvmDeclarations: LlvmDeclarations

    lateinit var bitcodeFileName: String

    lateinit var compilerOutput: List<ObjectFile>

    val coverage by lazy { CoverageManager(this) }

    lateinit var objCExport: ObjCExport

    fun hasDebugInfo() = debugInfoDelegate.isInitialized()

    fun verifyBitCode() {
        if (!llvmDelegate.isInitialized()) return
        verifyModule(llvm.module)
    }

    // TODO: Do we need this function?
    fun printBitCode() {
        if (!llvmDelegate.isInitialized()) return
        separator("BitCode:")
        LLVMDumpModule(llvm.module)
    }

    private fun separator(title: String) {
        println("\n\n--- ${title} ----------------------\n")
    }

    private var isDisposed = false

    // A little hack to make logging work when executing this phase in its parent context.
    // TODO: A better solution would be a separate logger object or something like that.
    //  PhaseContext should not be responsible for logging.
    override var inVerbosePhase: Boolean
        get() = context.inVerbosePhase
        set(value) {
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
        tempFiles.dispose()

        isDisposed = true
    }
}