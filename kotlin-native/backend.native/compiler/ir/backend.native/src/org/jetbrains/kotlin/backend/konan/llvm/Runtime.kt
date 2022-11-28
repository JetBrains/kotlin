/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.declarations.IrFunction

interface RuntimeAware {
    val runtime: Runtime
}

class Runtime(llvmContext: LLVMContextRef, bitcodeFile: String) {
    val llvmModule: LLVMModuleRef = parseBitcodeFile(llvmContext, bitcodeFile)
    val calculatedLLVMTypes: MutableMap<IrType, LLVMTypeRef> = HashMap()
    val addedLLVMExternalFunctions: MutableMap<IrFunction, LlvmCallable> = HashMap()

    private fun getStructTypeOrNull(name: String) = LLVMGetTypeByName(llvmModule, "struct.$name")
    private fun getStructType(name: String) = getStructTypeOrNull(name)
            ?: error("struct.$name is not found in the Runtime module.")

    val typeInfoType = getStructType("TypeInfo")
    val extendedTypeInfoType = getStructType("ExtendedTypeInfo")
    val writableTypeInfoType = getStructTypeOrNull("WritableTypeInfo")
    val interfaceTableRecordType = getStructType("InterfaceTableRecord")
    val associatedObjectTableRecordType = getStructType("AssociatedObjectTableRecord")

    val objHeaderType = getStructType("ObjHeader")
    val objHeaderPtrType = pointerType(objHeaderType)
    val objHeaderPtrPtrType = pointerType(objHeaderType)
    val arrayHeaderType = getStructType("ArrayHeader")

    val frameOverlayType = getStructType("FrameOverlay")

    val target = LLVMGetTarget(llvmModule)!!.toKString()

    val dataLayout = LLVMGetDataLayout(llvmModule)!!.toKString()

    val targetData = LLVMCreateTargetData(dataLayout)!!

    val kotlinObjCClassData by lazy { getStructType("KotlinObjCClassData") }
    val kotlinObjCClassInfo by lazy { getStructType("KotlinObjCClassInfo") }
    val objCMethodDescription by lazy { getStructType("ObjCMethodDescription") }
    val objCTypeAdapter by lazy { getStructType("ObjCTypeAdapter") }
    val objCToKotlinMethodAdapter by lazy { getStructType("ObjCToKotlinMethodAdapter") }
    val kotlinToObjCMethodAdapter by lazy { getStructType("KotlinToObjCMethodAdapter") }
    val typeInfoObjCExportAddition by lazy { getStructType("TypeInfoObjCExportAddition") }

    val objCClassObjectType by lazy { getStructType("_class_t") }
    val objCCache by lazy { getStructType("_objc_cache") }
    val objCClassRoType by lazy { getStructType("_class_ro_t") }
    val objCMethodType by lazy { getStructType("_objc_method") }
    val objCMethodListType by lazy { getStructType("__method_list_t") }
    val objCProtocolListType by lazy { getStructType("_objc_protocol_list") }
    val objCIVarListType by lazy { getStructType("_ivar_list_t") }
    val objCPropListType by lazy { getStructType("_prop_list_t") }

    val kRefSharedHolderType by lazy { LLVMGetTypeByName(llvmModule, "class.KRefSharedHolder")!! }
    val blockLiteralType by lazy { getStructType("Block_literal_1") }
    val blockDescriptorType by lazy { getStructType("Block_descriptor_1") }

    val pointerSize: Int by lazy {
        LLVMABISizeOfType(targetData, objHeaderPtrType).toInt()
    }

    val pointerAlignment: Int by lazy {
        LLVMABIAlignmentOfType(targetData, objHeaderPtrType)
    }

    // Must match kObjectAlignment in runtime
    val objectAlignment = 8
}
