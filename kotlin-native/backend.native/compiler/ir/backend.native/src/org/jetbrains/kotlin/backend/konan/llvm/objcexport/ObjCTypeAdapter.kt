/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objcexport

import llvm.LLVMTypeRef
import org.jetbrains.kotlin.backend.konan.ir.ClassLayoutBuilder
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.ConstPointer
import org.jetbrains.kotlin.backend.konan.llvm.ConstValue
import org.jetbrains.kotlin.backend.konan.llvm.LlvmCallable
import org.jetbrains.kotlin.backend.konan.llvm.RTTIGenerator
import org.jetbrains.kotlin.backend.konan.llvm.Struct
import org.jetbrains.kotlin.backend.konan.llvm.constPointer
import org.jetbrains.kotlin.ir.declarations.IrClass

internal class ObjCToKotlinMethodAdapter private constructor(type: LLVMTypeRef, vararg elements: ConstValue?) : Struct(type, *elements) {
    companion object {
        fun CodeGenerator.ObjCToKotlinMethodAdapter(
                selector: String,
                encoding: String,
                imp: LlvmCallable,
        ) = ObjCToKotlinMethodAdapter(
                type = llvm.runtime.objCToKotlinMethodAdapter,
                staticData.cStringLiteral(selector),
                staticData.cStringLiteral(encoding),
                imp.toConstPointer(),
        )
    }
}

internal class KotlinToObjCMethodAdapter private constructor(type: LLVMTypeRef, vararg elements: ConstValue?) : Struct(type, *elements) {
    companion object {
        fun CodeGenerator.KotlinToObjCMethodAdapter(
                selector: String,
                itablePlace: ClassLayoutBuilder.InterfaceTablePlace,
                vtableIndex: Int,
                kotlinImpl: ConstPointer,
        ) = KotlinToObjCMethodAdapter(
                type = llvm.runtime.kotlinToObjCMethodAdapter,
                staticData.cStringLiteral(selector),
                llvm.constInt32(itablePlace.interfaceId),
                llvm.constInt32(itablePlace.itableSize),
                llvm.constInt32(itablePlace.methodIndex),
                llvm.constInt32(vtableIndex),
                kotlinImpl,
        )
    }
}

internal class ObjCTypeAdapter private constructor(val irClass: IrClass?, val objCName: String, type: LLVMTypeRef, vararg elements: ConstValue?) : Struct(type, *elements) {
    companion object {
        fun CodeGenerator.ObjCTypeAdapter(
                irClass: IrClass?,
                objCName: String,
                vtable: ConstPointer?,
                vtableSize: Int,
                itable: List<RTTIGenerator.InterfaceTableRecord>,
                itableSize: Int,
                directAdapters: List<ObjCToKotlinMethodAdapter>,
                classAdapters: List<ObjCToKotlinMethodAdapter>,
                virtualAdapters: List<ObjCToKotlinMethodAdapter>,
                reverseAdapters: List<KotlinToObjCMethodAdapter>,
        ) = ObjCTypeAdapter(
                irClass = irClass,
                objCName = objCName,
                type = llvm.runtime.objCTypeAdapter,
                irClass?.let { constPointer(typeInfoValue(it)) },
                vtable,
                llvm.constInt32(vtableSize),
                llvm.staticData.placeGlobalConstArray("", llvm.runtime.interfaceTableRecordType, itable),
                llvm.constInt32(itableSize),
                llvm.staticData.cStringLiteral(objCName),
                llvm.staticData.placeGlobalConstArray("", llvm.runtime.objCToKotlinMethodAdapter, directAdapters),
                llvm.constInt32(directAdapters.size),
                llvm.staticData.placeGlobalConstArray("", llvm.runtime.objCToKotlinMethodAdapter, classAdapters),
                llvm.constInt32(classAdapters.size),
                llvm.staticData.placeGlobalConstArray("", llvm.runtime.objCToKotlinMethodAdapter, virtualAdapters),
                llvm.constInt32(virtualAdapters.size),
                llvm.staticData.placeGlobalConstArray("", llvm.runtime.kotlinToObjCMethodAdapter, reverseAdapters),
                llvm.constInt32(reverseAdapters.size),
        )

        fun CodeGenerator.ObjCTypeAdapterForBindClassToObjCName(
                irClass: IrClass?,
                objCName: String,
        ) = ObjCTypeAdapter(
                irClass = irClass,
                objCName = objCName,
                type = llvm.runtime.objCTypeAdapter,
                irClass?.let { constPointer(typeInfoValue(it)) },
                llvm.nullPointer, // vtable
                llvm.constInt32(0),
                llvm.nullPointer, // itable
                llvm.constInt32(0),
                llvm.staticData.cStringLiteral(objCName),
                llvm.nullPointer, // directAdapters
                llvm.constInt32(0),
                llvm.nullPointer, // classAdapters
                llvm.constInt32(0),
                llvm.nullPointer, // virtualAdapters
                llvm.constInt32(0),
                llvm.nullPointer, // reverseAdapters
                llvm.constInt32(0),
        )
    }
}

