/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.expressions.IrConst

/**
 * Provides utilities to create static data.
 */
internal class StaticData(override val context: Context): ContextUtils {

    /**
     * Represents the LLVM global variable.
     */
    class Global private constructor(val staticData: StaticData, val llvmGlobal: LLVMValueRef) {
        companion object {

            private fun createLlvmGlobal(module: LLVMModuleRef,
                                         type: LLVMTypeRef,
                                         name: String,
                                         isExported: Boolean
            ): LLVMValueRef {

                if (isExported && LLVMGetNamedGlobal(module, name) != null) {
                    throw IllegalArgumentException("Global '$name' already exists")
                }

                // Globals created with this API are *not* thread local.
                val llvmGlobal = LLVMAddGlobal(module, type, name)!!

                if (!isExported) {
                    LLVMSetLinkage(llvmGlobal, LLVMLinkage.LLVMInternalLinkage)
                }

                return llvmGlobal
            }

            fun create(staticData: StaticData, type: LLVMTypeRef, name: String, isExported: Boolean): Global {
                val module = staticData.context.llvmModule

                val isUnnamed = (name == "") // LLVM will select the unique index and represent the global as `@idx`.
                if (isUnnamed && isExported) {
                    throw IllegalArgumentException("unnamed global can't be exported")
                }

                val llvmGlobal = createLlvmGlobal(module!!, type, name, isExported)
                return Global(staticData, llvmGlobal)
            }
        }

        val type get() = getGlobalType(this.llvmGlobal)

        fun setInitializer(value: ConstValue) {
            LLVMSetInitializer(llvmGlobal, value.llvm)
        }

        fun setZeroInitializer() {
            LLVMSetInitializer(llvmGlobal, LLVMConstNull(this.type)!!)
        }

        fun setConstant(value: Boolean) {
            LLVMSetGlobalConstant(llvmGlobal, if (value) 1 else 0)
        }

        fun setLinkage(value: LLVMLinkage) {
            LLVMSetLinkage(llvmGlobal, value)
        }

        fun setAlignment(value: Int) {
            LLVMSetAlignment(llvmGlobal, value)
        }

        fun setSection(name: String) {
            LLVMSetSection(llvmGlobal, name)
        }

        val pointer = Pointer.to(this)
    }

    /**
     * Represents the pointer to static data.
     * It can be a pointer to either a global or any its element.
     *
     * TODO: this class is probably should be implemented more optimally
     */
    class Pointer private constructor(val global: Global,
                                      private val delegate: ConstPointer,
                                      val offsetInGlobal: Long) : ConstPointer by delegate {

        companion object {
            fun to(global: Global) = Pointer(global, constPointer(global.llvmGlobal), 0L)
        }

        private fun getElementOffset(index: Int): Long {
            val llvmTargetData = global.staticData.llvmTargetData
            val type = LLVMGetElementType(delegate.llvmType)
            return when (LLVMGetTypeKind(type)) {
                LLVMTypeKind.LLVMStructTypeKind -> LLVMOffsetOfElement(llvmTargetData, type, index)
                LLVMTypeKind.LLVMArrayTypeKind -> LLVMABISizeOfType(llvmTargetData, LLVMGetElementType(type)) * index
                else -> TODO()
            }
        }

        override fun getElementPtr(index: Int): Pointer {
            return Pointer(global, delegate.getElementPtr(index), offsetInGlobal + this.getElementOffset(index))
        }

        /**
         * @return the distance from other pointer to this.
         *
         * @throws UnsupportedOperationException if it is not possible to represent the distance as [Int] value
         */
        fun sub(other: Pointer): Int {
            if (this.global != other.global) {
                throw UnsupportedOperationException("pointers must belong to the same global")
            }

            val res = this.offsetInGlobal - other.offsetInGlobal
            if (res.toInt().toLong() != res) {
                throw UnsupportedOperationException("result doesn't fit into Int")
            }

            return res.toInt()
        }
    }

    /**
     * Creates [Global] with given type and name.
     *
     * It is external until explicitly initialized with [Global.setInitializer].
     */
    fun createGlobal(type: LLVMTypeRef, name: String, isExported: Boolean = false): Global {
        return Global.create(this, type, name, isExported)
    }

    /**
     * Creates [Global] with given name and value.
     */
    fun placeGlobal(name: String, initializer: ConstValue, isExported: Boolean = false): Global {
        val global = createGlobal(initializer.llvmType, name, isExported)
        global.setInitializer(initializer)
        return global
    }

    /**
     * Creates array-typed global with given name and value.
     */
    fun placeGlobalArray(name: String, elemType: LLVMTypeRef?, elements: List<ConstValue>, isExported: Boolean = false): Global {
        val initializer = ConstArray(elemType, elements)
        val global = placeGlobal(name, initializer, isExported)

        return global
    }

    private val stringLiterals = mutableMapOf<String, ConstPointer>()
    private val cStringLiterals = mutableMapOf<String, ConstPointer>()

    fun cStringLiteral(value: String) =
            cStringLiterals.getOrPut(value) { placeCStringLiteral(value) }

    fun kotlinStringLiteral(value: String) =
        stringLiterals.getOrPut(value) { createKotlinStringLiteral(value) }
}

/**
 * Creates static instance of `konan.ImmutableByteArray` with given values of elements.
 *
 * @param args data for constant creation.
 */
internal fun StaticData.createImmutableBlob(value: IrConst<String>): LLVMValueRef {
    val args = value.value.map { Int8(it.code.toByte()).llvm }
    return createConstKotlinArray(context.ir.symbols.immutableBlob.owner, args)
}