/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objcexport.split

import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.constPointer
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.ObjCExportConverterConstants
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.buildWritableTypeInfoValue
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.writableTypeInfoSymbolName

private const val OBJC_EXPORT_CONVERTERS_MODULE_NAME: String = "objc_export_converters"

private fun LLVMContextRef.createNamedStructWithBody(name: String, vararg fieldTypes: LLVMTypeRef): LLVMTypeRef {
    return LLVMStructCreateNamed(this, name)!!.also {
        memScoped {
            val fields = allocArrayOf(*fieldTypes)
            LLVMStructSetBody(it, fields, fieldTypes.size, 0)
        }
    }
}

internal fun createObjCExportConvertersModule(llvmContext: LLVMContextRef): LLVMModuleRef {
    // TODO(Gabriele): It'd be nice reusing [CodegenLlvmHelpers], however it is highly coupled with [NativeGenerationState].
    // TODO(Gabriele): We should refactor this function when the split is complete.

    // TODO(Gabriele): Since we're reusing the existing LLVMContext, wouldn't we have some clashes with
    // TODO(Gabriele): `createdNamedStructWithBody`?

    val module = LLVMModuleCreateWithNameInContext(OBJC_EXPORT_CONVERTERS_MODULE_NAME, llvmContext)!!
    val pointerType = LLVMPointerTypeInContext(llvmContext, 0)!!

    // Unfortunately, most of the ObjC-codegen module is coupled, so functions cannot be reused.
    // We need to recreate structs manually.
    val objCExportAdditionType = llvmContext.createNamedStructWithBody("struct.TypeInfoObjCExportAddition", pointerType, pointerType, pointerType, pointerType)
    val writableTypeInfoType = llvmContext.createNamedStructWithBody("struct.WritableTypeInfo", objCExportAdditionType)

    // Converters have this C-shape: char*(*)(char*)
    val converterFunctionType = memScoped {
        val paramTypes = allocArrayOf(pointerType)
        LLVMFunctionType(pointerType, paramTypes, 1, 0)!!
    }

    for (entry in ObjCExportConverterConstants.standardConverters) {
        val converterFunc = LLVMAddFunction(module, entry.converterFunctionName, converterFunctionType)!!
        val converterPtr = constPointer(LLVMConstBitCast(converterFunc, pointerType)!!)

        val value = buildWritableTypeInfoValue(writableTypeInfoType, objCExportAdditionType, convertToRetained = converterPtr, llvmPointerType = pointerType)

        val global = LLVMAddGlobal(module, writableTypeInfoType, entry.writableTypeInfoSymbolName)!!
        LLVMSetInitializer(global, value.llvm)
        LLVMSetLinkage(global, LLVMLinkage.LLVMExternalLinkage)
    }

    return module
}