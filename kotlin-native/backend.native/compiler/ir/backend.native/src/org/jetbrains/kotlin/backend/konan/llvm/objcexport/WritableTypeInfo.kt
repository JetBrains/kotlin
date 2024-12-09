/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objcexport

import kotlinx.cinterop.toKString
import llvm.LLVMLinkage
import llvm.LLVMPrintTypeToString
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.ConstPointer
import org.jetbrains.kotlin.backend.konan.llvm.ConstValue
import org.jetbrains.kotlin.backend.konan.llvm.ContextUtils
import org.jetbrains.kotlin.backend.konan.llvm.StaticData
import org.jetbrains.kotlin.backend.konan.llvm.Struct
import org.jetbrains.kotlin.backend.konan.llvm.bitcast
import org.jetbrains.kotlin.backend.konan.llvm.functionType
import org.jetbrains.kotlin.backend.konan.llvm.isExported
import org.jetbrains.kotlin.backend.konan.llvm.kObjHeaderPtr
import org.jetbrains.kotlin.backend.konan.llvm.llvmType
import org.jetbrains.kotlin.backend.konan.llvm.pointerType
import org.jetbrains.kotlin.backend.konan.llvm.replaceExternalWeakOrCommonGlobal
import org.jetbrains.kotlin.backend.konan.llvm.writableTypeInfoSymbolName
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.kotlinFqName

internal sealed interface WritableTypeInfoPointer : ConstPointer

private class FixedWritableTypeInfo(global: StaticData.Global) : WritableTypeInfoPointer, ConstPointer by global.pointer

private class OverridableWritableTypeInfo(private val global: StaticData.Global) : WritableTypeInfoPointer, ConstPointer by global.pointer {
    private var replaced = false

    fun tryReplaceWith(value: ConstValue): Boolean {
        if (replaced) {
            return false
        }
        replaced = true
        global.setLinkage(LLVMLinkage.LLVMExternalLinkage)
        global.setInitializer(value)
        return true
    }
}

/**
 * Generate [WritableTypeInfoPointer] for [irClass] synthetic interface.
 */
internal fun ContextUtils.generateWritableTypeInfoForSyntheticInterface(irClass: IrClass): WritableTypeInfoPointer? = runtime.writableTypeInfoType?.let { type ->
    require(irClass.isInterface)

    FixedWritableTypeInfo(staticData.createGlobal(type, "").apply {
        setZeroInitializer()
    })
}

/**
 * Generate [WritableTypeInfoPointer] for [irClass] class.
 * If [irClass] is exported, its [WritableTypeInfoPointer] can later be overridden once.
 */
internal fun ContextUtils.generateWritableTypeInfoForClass(irClass: IrClass): WritableTypeInfoPointer? = runtime.writableTypeInfoType?.let { type ->
    if (!irClass.isExported()) {
        // If the class not exported, its WritableTypeInfo cannot be replaced
        FixedWritableTypeInfo(staticData.createGlobal(type, "").apply {
            setZeroInitializer()
        })
    } else {
        OverridableWritableTypeInfo(staticData.createGlobal(type, irClass.writableTypeInfoSymbolName, isExported = true).apply {
            setLinkage(LLVMLinkage.LLVMCommonLinkage) // Allows to be replaced by other bitcode module.
            setZeroInitializer()
        })
    }
}

internal class WritableTypeInfoOverrideError(val irClass: IrClass, val reason: Reason) : IllegalStateException("Cannot override WritableTypeInfo for ${irClass.kotlinFqName}: $reason") {
    enum class Reason {
        NON_OVERRIDABLE,
        ALREADY_OVERRIDDEN,
    }
}

/**
 * @throws WritableTypeInfoOverrideError if [convertToRetained] cannot be bound to [irClass]
 */
internal fun CodeGenerator.bindObjCExportConvertToRetained(
        irClass: IrClass,
        convertToRetained: ConstPointer,
) = setWritableTypeInfo(
        irClass,
        buildWritableTypeInfoValue(
                convertToRetained = convertToRetained,
                objCClass = null,
                typeAdapter = null,
        )
)

/**
 * @throws WritableTypeInfoOverrideError if [typeAdapter] cannot be bound to [irClass]
 */
internal fun CodeGenerator.bindObjCExportTypeAdapterTo(
        irClass: IrClass,
        typeAdapter: ConstPointer
) = setWritableTypeInfo(
        irClass,
        buildWritableTypeInfoValue(
                convertToRetained = null,
                objCClass = null,
                typeAdapter = typeAdapter,
        )
)

private fun CodeGenerator.setWritableTypeInfo(
        irClass: IrClass,
        writableTypeInfoValue: Struct,
) {
    if (isExternal(irClass)) {
        // Note: this global replaces the external one with common linkage.
        replaceExternalWeakOrCommonGlobal(
                irClass.writableTypeInfoSymbolName,
                writableTypeInfoValue,
                irClass
        )
    } else {
        val writeableTypeInfoGlobal = generationState.llvmDeclarations.forClass(irClass).writableTypeInfoGlobal
        if (writeableTypeInfoGlobal !is OverridableWritableTypeInfo) {
            throw WritableTypeInfoOverrideError(irClass, WritableTypeInfoOverrideError.Reason.NON_OVERRIDABLE)
        }
        if (!writeableTypeInfoGlobal.tryReplaceWith(writableTypeInfoValue)) {
            throw WritableTypeInfoOverrideError(irClass, WritableTypeInfoOverrideError.Reason.ALREADY_OVERRIDDEN)
        }
    }
}

private fun CodeGenerator.buildWritableTypeInfoValue(
        convertToRetained: ConstPointer?,
        objCClass: ConstPointer?,
        typeAdapter: ConstPointer?
): Struct {
    if (convertToRetained != null) {
        val expectedType = pointerType(functionType(llvm.int8PtrType, false, kObjHeaderPtr))
        assert(convertToRetained.llvmType == expectedType) {
            "Expected: ${LLVMPrintTypeToString(expectedType)!!.toKString()} " +
                    "found: ${LLVMPrintTypeToString(convertToRetained.llvmType)!!.toKString()}"
        }
    }

    val objCExportAddition = Struct(
            runtime.typeInfoObjCExportAddition,
            convertToRetained?.bitcast(llvm.int8PtrType),
            objCClass,
            typeAdapter
    )

    val writableTypeInfoType = runtime.writableTypeInfoType!!
    return Struct(writableTypeInfoType, objCExportAddition)
}