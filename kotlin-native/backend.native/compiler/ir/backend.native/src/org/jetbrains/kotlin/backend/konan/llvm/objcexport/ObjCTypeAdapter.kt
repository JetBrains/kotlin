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
import org.jetbrains.kotlin.backend.konan.llvm.KonanBinaryInterface.classHierarchyIdsSymbolName
import org.jetbrains.kotlin.backend.konan.llvm.LlvmCallable
import org.jetbrains.kotlin.backend.konan.llvm.RTTIGenerator
import org.jetbrains.kotlin.backend.konan.llvm.Struct
import org.jetbrains.kotlin.backend.konan.llvm.constPointer
import org.jetbrains.kotlin.backend.konan.llvm.crossModuleName
import org.jetbrains.kotlin.backend.konan.llvm.hierarchyWillBeLaidOutByFinalLink
import org.jetbrains.kotlin.backend.konan.llvm.importGlobal
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
                interfaceName: String? = null,
        ): KotlinToObjCMethodAdapter {
            val interfaceIdPtr = if (context.hierarchyWillBeLaidOutByFinalLink) {
                // `interfaceName` names the interface this method is dispatched through where `itablePlace` cannot:
                // the places rebuilt from a cache carry no IR.
                val crossModuleInterfaceName = interfaceName ?: itablePlace.irInterface?.crossModuleName()
                if (crossModuleInterfaceName != null) {
                    // The id itself is not known here - the final link assigns it - so the adapter carries the
                    // address of the interface's `ClassHierarchyIds` record, which that link fills in, and the
                    // runtime reads the id out of it. [ClassLayoutBuilder.InterfaceTablePlace.interfaceId] is no
                    // use for this: it is the id of the compilation that computed the place, not of the program.
                    constPointer(importGlobal(classHierarchyIdsSymbolName(crossModuleInterfaceName), runtime.classHierarchyIdsType))
                } else llvm.nullPointer
            } else {
                val id = llvm.constInt32(itablePlace.interfaceId)
                staticData.placeGlobal("", Struct(runtime.classHierarchyIdsType, id, id))
                        .also { it.setConstant(true) }.pointer
            }
            return KotlinToObjCMethodAdapter(
                    type = llvm.runtime.kotlinToObjCMethodAdapter,
                    staticData.cStringLiteral(selector),
                    interfaceIdPtr,
                    llvm.constInt32(itablePlace.itableSize),
                    llvm.constInt32(itablePlace.methodIndex),
                    llvm.constInt32(vtableIndex),
                    kotlinImpl,
            )
        }
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
                vtableSize: Int,
                itableSize: Int = 0,
                reverseAdapters: List<KotlinToObjCMethodAdapter>,
                typeInfo: ConstPointer? = irClass?.let { constPointer(typeInfoValue(it)) },
        ) = ObjCTypeAdapter(
                irClass = irClass,
                objCName = objCName,
                type = llvm.runtime.objCTypeAdapter,
                typeInfo,
                llvm.nullPointer, // vtable
                llvm.constInt32(vtableSize),
                llvm.nullPointer, // itable
                llvm.constInt32(itableSize),
                llvm.staticData.cStringLiteral(objCName),
                llvm.nullPointer, // directAdapters
                llvm.constInt32(0),
                llvm.nullPointer, // classAdapters
                llvm.constInt32(0),
                llvm.nullPointer, // virtualAdapters
                llvm.constInt32(0),
                if (reverseAdapters.isEmpty()) llvm.nullPointer
                else llvm.staticData.placeGlobalConstArray("", llvm.runtime.kotlinToObjCMethodAdapter, reverseAdapters),
                llvm.constInt32(reverseAdapters.size),
        )
    }
}
