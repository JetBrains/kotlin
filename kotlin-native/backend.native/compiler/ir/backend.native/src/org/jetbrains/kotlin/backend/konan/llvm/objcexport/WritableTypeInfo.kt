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
import org.jetbrains.kotlin.backend.konan.llvm.Struct
import org.jetbrains.kotlin.backend.konan.llvm.bitcast
import org.jetbrains.kotlin.backend.konan.llvm.functionType
import org.jetbrains.kotlin.backend.konan.llvm.kObjHeaderPtr
import org.jetbrains.kotlin.backend.konan.llvm.llvmType
import org.jetbrains.kotlin.backend.konan.llvm.pointerType
import org.jetbrains.kotlin.backend.konan.llvm.replaceExternalWeakOrCommonGlobal
import org.jetbrains.kotlin.backend.konan.llvm.writableTypeInfoSymbolName
import org.jetbrains.kotlin.ir.declarations.IrClass

internal fun CodeGenerator.bindObjCExportConvertToRetainedTo(
        irClass: IrClass,
        convertToRetained: ConstPointer
) = setWritableTypeInfo(
        irClass,
        buildWritableTypeInfoValue(
                convertToRetained = convertToRetained,
                objCClass = null,
                typeAdapter = null,
        )
)

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
        val writeableTypeInfoGlobal = generationState.llvmDeclarations.forClass(irClass).writableTypeInfoGlobal!!
        writeableTypeInfoGlobal.setLinkage(LLVMLinkage.LLVMExternalLinkage)
        writeableTypeInfoGlobal.setInitializer(writableTypeInfoValue)
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