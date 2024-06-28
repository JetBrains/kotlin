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

class Runtime(private val llvmContext: LLVMContextRef, bitcodeFile: String) {
    val llvmModule: LLVMModuleRef = parseBitcodeFile(llvmContext, bitcodeFile)
    val calculatedLLVMTypes: MutableMap<IrType, LLVMTypeRef> = HashMap()
    val addedLLVMExternalFunctions: MutableMap<IrFunction, LlvmCallable> = HashMap()

    private fun getStructTypeOrNull(name: String) =
            LLVMGetTypeByName(llvmModule, "struct.$name")
                    ?: LLVMGetNamedGlobal(llvmModule, "touch$name")?.let(::LLVMGlobalGetValueType)

    private fun getStructType(name: String) = getStructTypeOrNull(name)
            ?: error("type $name is not found in the Runtime module.")

    private fun createStructType(name: String, vararg fieldTypes: LLVMTypeRef): LLVMTypeRef {
        val result = LLVMStructCreateNamed(llvmContext, name) ?: error("failed to create struct $name")
        LLVMStructSetBody(result, fieldTypes.toList().toCValues(), fieldTypes.size, 0)
        return result
    }

    private fun createOpaqueStructType(name: String): LLVMTypeRef =
            LLVMStructCreateNamed(llvmContext, name) ?: error("failed to create struct $name")

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

    val initNodeType = getStructType("InitNode")
    val memoryStateType = getStructTypeOrNull("MemoryState") ?: createOpaqueStructType("struct.MemoryState")!!

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

    val objCClassObjectType: LLVMTypeRef by lazy {
        val result = LLVMStructCreateNamed(llvmContext, "_class_t") ?: error("failed to create struct _class_t")
        val fieldTypes = listOf(
                pointerType(result),
                pointerType(result),
                pointerType(objCCache),
                pointerType(pointerType(functionType(i8Ptr, false, i8Ptr, i8Ptr))),
                pointerType(objCClassRoType)
        )
        LLVMStructSetBody(result, fieldTypes.toList().toCValues(), fieldTypes.size, 0)
        result
    }
    val objCCache by lazy { createOpaqueStructType("_objc_cache") }
    val objCClassRoType by lazy {
        createStructType(
                "_class_ro_t",
                i32,
                i32,
                i32,
                i8Ptr,
                i8Ptr,
                pointerType(objCMethodListType),
                pointerType(objCProtocolListType),
                pointerType(objCIVarListType),
                i8Ptr,
                pointerType(objCPropListType)
        )
    }
    val objCMethodType by lazy {
        createStructType("_objc_method", i8Ptr, i8Ptr, i8Ptr)
    }

    private val i32 = LLVMInt32TypeInContext(llvmContext)!!
    private val i8 = LLVMInt8TypeInContext(llvmContext)!!
    private val i8Ptr = pointerType(i8)

    val objCMethodListType by lazy { createOpaqueStructType("__method_list_t") }
    val objCProtocolListType by lazy { createOpaqueStructType("_objc_protocol_list") }
    val objCIVarListType by lazy { createOpaqueStructType("_ivar_list_t") }
    val objCPropListType by lazy { createOpaqueStructType("_prop_list_t") }

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
