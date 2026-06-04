/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import hair.compilation.FunctionCompilation
import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.ir.isUnconditional
import org.jetbrains.kotlin.backend.common.lower.coroutines.getOrCreateFunctionWithContinuationStub
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterCodegen
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterExportedElements
import org.jetbrains.kotlin.backend.konan.cgen.CBridgeOrigin
import org.jetbrains.kotlin.backend.konan.hair.HairToBitcode
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.backend.konan.llvm.objc.processBindClassToObjCNameAnnotations
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.lower.ReifiedFunctionLowering.Companion.isReifiedInline
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.config.nativeBinaryOptions.AndroidProgramType
import org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptions
import org.jetbrains.kotlin.config.nativeBinaryOptions.SourceInfoType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.objcinterop.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.konan.ForeignExceptionMode
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

internal fun NativeGenerationState.generateRuntimeConstantsModule() : LLVMModuleRef {
    val llvmModule = LLVMModuleCreateWithNameInContext("constants", llvmContext)!!
    LLVMSetDataLayout(llvmModule, runtime.dataLayout)
    val static = StaticData(llvmModule, llvm)

    fun setRuntimeConstGlobal(name: String, value: ConstValue) {
        val global = static.placeGlobal(name, value)
        global.setConstant(true)
        global.setLinkage(LLVMLinkage.LLVMExternalLinkage)
    }

    setRuntimeConstGlobal("Kotlin_needDebugInfo", llvm.constInt32(if (shouldContainDebugInfo()) 1 else 0))
    setRuntimeConstGlobal("Kotlin_runtimeAssertsMode", llvm.constInt32(config.runtimeAssertsMode.value))
    setRuntimeConstGlobal("Kotlin_disableMmap", llvm.constInt32(if (config.disableMmap) 1 else 0))

    val runtimeLogs = ConstArray(llvm.int32Type, LoggingTag.entries.sortedBy { it.ord }.map {
        config.runtimeLogs[it]!!.ord.let { llvm.constInt32(it) }
    })
    setRuntimeConstGlobal("Kotlin_runtimeLogs", runtimeLogs)
    setRuntimeConstGlobal("Kotlin_concurrentWeakSweep", llvm.constInt32(if (context.config.concurrentWeakSweep) 1 else 0))
    setRuntimeConstGlobal("Kotlin_gcMarkSingleThreaded", llvm.constInt32(if (config.gcMarkSingleThreaded) 1 else 0))
    setRuntimeConstGlobal("Kotlin_fixedBlockPageSize", llvm.constInt32(config.fixedBlockPageSize.toInt()))
    setRuntimeConstGlobal("Kotlin_pagedAllocator", llvm.constInt32(if (config.pagedAllocator) 1 else 0))

    return llvmModule
}
