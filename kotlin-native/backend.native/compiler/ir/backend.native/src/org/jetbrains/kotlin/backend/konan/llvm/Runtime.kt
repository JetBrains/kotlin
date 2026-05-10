/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.driver.NativeBackendPhaseContext
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.declarations.IrFunction

internal interface RuntimeAware {
    val runtime: Runtime
}

internal class Runtime private constructor(
        private val llvmContext: LLVMContextRef,
        val llvmModule: LLVMModuleRef,
        val kind: Kind,
) {
    /**
     * What this [Runtime] is being used for.
     *
     * - [NativeBinary]: full host runtime loaded from a per-target `runtime.bc`. Every member
     *   of this class is usable.
     * - [CudaDevice]: minimal in-memory NVPTX-targeted module carrying only NVVM intrinsic
     *   declarations and the right target triple/datalayout for PTX emission. Host-runtime-
     *   specific members (objHeaderType, frameOverlayType, ObjC types, etc.) are lazy and will
     *   crash when accessed because their underlying struct types do not exist in the device
     *   module. Subset validation in lowering ensures device codegen never reaches those paths.
     */
    enum class Kind { NativeBinary, CudaDevice }

    companion object {
        fun forNativeBinary(
                phaseContext: NativeBackendPhaseContext,
                llvmContext: LLVMContextRef,
                bitcodeFile: String,
        ): Runtime {
            val module = parseBitcodeFile(phaseContext, phaseContext.diagnosticReporter, llvmContext, bitcodeFile)
            return Runtime(llvmContext, module, Kind.NativeBinary)
        }

        fun forCudaDevice(llvmContext: LLVMContextRef): Runtime {
            val module = LLVMModuleCreateWithNameInContext("cuda_device_runtime", llvmContext)!!
            LLVMSetTarget(module, NVPTX64_TARGET_TRIPLE)
            LLVMSetDataLayout(module, NVPTX64_DATA_LAYOUT)
            declareNvvmIntrinsics(llvmContext, module)
            return Runtime(llvmContext, module, Kind.CudaDevice)
        }

        // NVPTX 64-bit target triple and standard datalayout per LLVM upstream NVPTXTargetMachine.
        const val NVPTX64_TARGET_TRIPLE = "nvptx64-nvidia-cuda"
        const val NVPTX64_DATA_LAYOUT = "e-i64:64-i128:128-v16:16-v32:32-n16:32:64"

        // Mirrors the @GCUnsafeCall names referenced from kotlin.native.cuda.cuda.kt.
        private val NVVM_I32_INTRINSICS = listOf(
                "llvm.nvvm.read.ptx.sreg.tid.x",
                "llvm.nvvm.read.ptx.sreg.tid.y",
                "llvm.nvvm.read.ptx.sreg.tid.z",
                "llvm.nvvm.read.ptx.sreg.ctaid.x",
                "llvm.nvvm.read.ptx.sreg.ctaid.y",
                "llvm.nvvm.read.ptx.sreg.ctaid.z",
        )

        private fun declareNvvmIntrinsics(llvmContext: LLVMContextRef, module: LLVMModuleRef) {
            val i32 = LLVMInt32TypeInContext(llvmContext)!!
            val voidType = LLVMVoidTypeInContext(llvmContext)!!
            val i32FromNothing = functionType(i32)
            val voidFromNothing = functionType(voidType)
            for (name in NVVM_I32_INTRINSICS) {
                LLVMAddFunction(module, name, i32FromNothing)
            }
            LLVMAddFunction(module, "llvm.nvvm.barrier0", voidFromNothing)
        }
    }

    val calculatedLLVMTypes: MutableMap<IrType, LLVMTypeRef> = HashMap()
    val addedLLVMExternalFunctions: MutableMap<IrFunction, LlvmCallable> = HashMap()

    private fun getStructTypeOrNull(name: String, isClass: Boolean = false) =
            LLVMGetTypeByName(llvmModule, "${if (isClass) "class" else "struct"}.$name")
                    ?: LLVMGetNamedGlobal(llvmModule, "touch$name")?.let(::LLVMGlobalGetValueType)

    private fun getStructType(name: String, isClass: Boolean = false) = getStructTypeOrNull(name, isClass)
            ?: error("type $name is not found in the Runtime module.")

    private fun createStructType(name: String, vararg fieldTypes: LLVMTypeRef): LLVMTypeRef {
        val result = LLVMStructCreateNamed(llvmContext, name) ?: error("failed to create struct $name")
        LLVMStructSetBody(result, fieldTypes.toList().toCValues(), fieldTypes.size, 0)
        return result
    }

    private fun createOpaqueStructType(name: String): LLVMTypeRef =
            LLVMStructCreateNamed(llvmContext, name) ?: error("failed to create struct $name")

    val pointerType = LLVMPointerTypeInContext(llvmContext, 0)!!

    // Host-runtime-specific struct types — lazy so they don't initialize on CudaDevice
    // runtimes (whose LLVM module lacks these definitions). Subset validation in lowering
    // ensures device codegen never accesses them.
    val typeInfoType by lazy { getStructType("TypeInfo") }
    val extendedTypeInfoType by lazy { getStructType("ExtendedTypeInfo") }
    val writableTypeInfoType by lazy { getStructTypeOrNull("WritableTypeInfo") }
    val interfaceTableRecordType by lazy { getStructType("InterfaceTableRecord") }
    val associatedObjectTableRecordType by lazy { getStructType("AssociatedObjectTableRecord") }

    val objHeaderType by lazy { getStructType("ObjHeader") }
    val arrayHeaderType by lazy { getStructType("ArrayHeader") }
    val stringHeaderType by lazy { getStructType("StringHeader") }

    val frameOverlayType by lazy { getStructType("FrameOverlay") }

    val initNodeType by lazy { getStructType("InitNode") }

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
        createStructType(
                "_class_t",
                pointerType, // _class_t*
                pointerType, // _class_t*
                pointerType, // _objc_cache*
                pointerType, // char* (*)(char*, char*)
                pointerType, // _class_ro_t*
        )
    }
    val objCCache by lazy { createOpaqueStructType("_objc_cache") }
    val objCClassRoType by lazy {
        createStructType(
                "_class_ro_t",
                i32,
                i32,
                i32,
                pointerType, // char*
                pointerType, // char*
                pointerType, // __method_list_t*
                pointerType, // _objc_protocol_list*
                pointerType, // _ivar_list_t*
                pointerType, // char*
                pointerType, // _prop_list_t*
        )
    }
    val objCMethodType by lazy {
        createStructType("_objc_method", pointerType, pointerType, pointerType)
    }

    private val i32 = LLVMInt32TypeInContext(llvmContext)!!

    val blockLiteralType by lazy { getStructType("Block_literal_1") }
    val blockDescriptorType by lazy { getStructType("Block_descriptor_1") }

    fun sizeOf(type: LLVMTypeRef) = LLVMABISizeOfType(targetData, type).toInt()
    fun alignOf(type: LLVMTypeRef) = LLVMABIAlignmentOfType(targetData, type)
    fun offsetOf(type: LLVMTypeRef, index: Int) = LLVMOffsetOfElement(targetData, type, index).toInt()

    val pointerSize: Int by lazy { sizeOf(pointerType) }
    val pointerAlignment: Int by lazy { alignOf(pointerType) }

    val stringHeaderExtraSize: Int by lazy {
        offsetOf(stringHeaderType, LLVMCountStructElementTypes(stringHeaderType) - 1) - sizeOf(arrayHeaderType)
    }

    // Must match kObjectAlignment in runtime
    val objectAlignment = 8

    val isBigEndian: Boolean by lazy { LLVMByteOrder(targetData) == LLVMByteOrdering.LLVMBigEndian }
}
