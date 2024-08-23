/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.cValuesOf
import llvm.*
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrConst

private fun ConstPointer.addBits(llvm: CodegenLlvmHelpers, type: LLVMTypeRef, bits: Int): ConstPointer {
    val rawPtr = LLVMConstBitCast(this.llvm, llvm.int8PtrType)
    // Only pointer arithmetic via GEP works on constant pointers in LLVM.
    val withBits = LLVMConstGEP2(llvm.int8Type, rawPtr, cValuesOf(llvm.int32(bits)), 1)!!
    val withType = LLVMConstBitCast(withBits, type)!!
    return constPointer(withType)
}

internal class KotlinStaticData(override val generationState: NativeGenerationState, override val llvm: CodegenLlvmHelpers, module: LLVMModuleRef) : ContextUtils, StaticData(module, llvm) {
    private val stringLiterals = mutableMapOf<String, ConstPointer>()

    // Must match OBJECT_TAG_PERMANENT_CONTAINER in C++.
    private fun permanentTag(typeInfo: ConstPointer): ConstPointer {
        return typeInfo.addBits(llvm, kTypeInfoPtr, 1)
    }


    private fun objHeader(typeInfo: ConstPointer): Struct {
        return Struct(runtime.objHeaderType, permanentTag(typeInfo))
    }

    private fun arrayHeader(typeInfo: ConstPointer, length: Int): Struct {
        assert(length >= 0)
        return Struct(runtime.arrayHeaderType, permanentTag(typeInfo), llvm.constInt32(length))
    }

    private fun createConstant(value: ConstValue): ConstPointer {
        val global = placeGlobal("", value)
        global.setUnnamedAddr(true)
        global.setConstant(true)
        // value should be of struct type with first element having the object/array header layout
        return global.pointer.getElementPtr(llvm, global.type, 0).bitcast(kObjHeaderPtr)
    }

    private fun createKotlinStringLiteral(value: String): ConstPointer {
        val useLatin1 = value.all { it.code in 0..255 }
        val useHashcodeCache = value.length > (if (useLatin1) 16 else 8) // arbitrary bound...
        // See KString.h for the meanings and native equivalents of these constants.
        val encoding = if (useLatin1) (1 shl 12) or ((value.length % 2) shl 2) else 0
        val stringHeaderLength = if (useHashcodeCache) 4 else 1
        val stringHeader = if (useHashcodeCache)
            arrayOf(llvm.constInt16((encoding or 3).toShort()), llvm.constInt16(0), llvm.constInt32(value.hashCode()))
        else
            arrayOf(llvm.constInt16(encoding.toShort()))
        // Length should be in chars independent of encoding.
        val arrayLength = stringHeaderLength + (if (useLatin1) (value.length + 1) / 2 else value.length)
        val arrayHeader = arrayHeader(context.ir.symbols.string.owner.typeInfoPtr, arrayLength)
        val arrayValue = if (useLatin1) {
            val encoded = value.map { llvm.constInt8(it.code.toByte()) }
            val padding = List(value.length % 2) { llvm.constInt8(0) }
            ConstArray(llvm.int8Type, encoded + padding)
        } else {
            ConstArray(llvm.int16Type, value.map(llvm::constChar16))
        }
        // The header contains a pointer, use a packed struct to avoid padding the entire string
        // to a multiple of pointer size.
        return createConstant(llvm.struct(arrayHeader, *stringHeader, arrayValue, packed = true))
    }

    fun kotlinStringLiteral(value: String) = stringLiterals.getOrPut(value) { createKotlinStringLiteral(value) }

    fun createConstKotlinArray(arrayClass: IrClass, elements: List<LLVMValueRef>) =
            createConstKotlinArray(arrayClass, elements.map { constValue(it) }).llvm

    fun createConstKotlinArray(arrayClass: IrClass, elements: List<ConstValue>): ConstPointer {
        val arrayHeader = arrayHeader(arrayClass.typeInfoPtr, elements.size)
        // (use [0 x i8] as body if there are no elements)
        val arrayBody = ConstArray(elements.firstOrNull()?.llvmType ?: llvm.int8Type, elements)
        return createConstant(llvm.struct(arrayHeader, arrayBody))
    }

    fun createConstKotlinObject(type: IrClass, vararg fields: ConstValue): ConstPointer {
        return createConstant(createConstKotlinObjectBody(type, *fields))
    }

    fun createConstKotlinObjectBody(type: IrClass, vararg fields: ConstValue): ConstValue {
        // TODO: handle padding here
        return llvm.struct(objHeader(type.typeInfoPtr), *fields)
    }

    fun createUniqueInstance(
            kind: UniqueKind, bodyType: LLVMTypeRef, typeInfo: ConstPointer): ConstPointer {
        assert(getStructElements(bodyType).size == 1) // ObjHeader only.
        val objHeader = when (kind) {
            UniqueKind.UNIT -> objHeader(typeInfo)
            UniqueKind.EMPTY_ARRAY -> arrayHeader(typeInfo, 0)
        }
        val global = this.placeGlobal(kind.llvmName, objHeader, isExported = true)
        global.setConstant(true)
        return global.pointer
    }

    fun unique(kind: UniqueKind): ConstPointer {
        val descriptor = when (kind) {
            UniqueKind.UNIT -> context.ir.symbols.unit.owner
            UniqueKind.EMPTY_ARRAY -> context.ir.symbols.array.owner
        }
        return if (isExternal(descriptor)) {
            constPointer(importGlobal(kind.llvmName, runtime.objHeaderType, descriptor))
        } else {
            generationState.llvmDeclarations.forUnique(kind).pointer
        }
    }

    /**
     * Creates static instance of `konan.ImmutableByteArray` with given values of elements.
     *
     * @param args data for constant creation.
     */
    fun createImmutableBlob(value: IrConst): LLVMValueRef {
        val strValue = value.value as String
        val args = strValue.map { llvm.int8(it.code.toByte()) }
        return createConstKotlinArray(context.ir.symbols.immutableBlob.owner, args)
    }
}

internal val ContextUtils.theUnitInstanceRef: ConstPointer
    get() = staticData.unique(UniqueKind.UNIT)