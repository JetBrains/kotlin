/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.signExtend
import llvm.LLVMGetEnumAttributeKindForName
import org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi


data class LLVMAttributeKindId(val value: Int)

interface LlvmAttribute {
    fun asAttributeKindId(): LLVMAttributeKindId
}


// We use sealed class instead of enum because there are attributes with parameters
// that we might want to use later. For example, align(<n>).
sealed class LlvmParameterAttribute(private val llvmAttributeName: String) : LlvmAttribute {

    override fun asAttributeKindId(): LLVMAttributeKindId = llvmAttributeKindIdCache.getOrPut(this) {
        getLlvmAttributeKindId(llvmAttributeName)
    }

    companion object {
        private val llvmAttributeKindIdCache = mutableMapOf<LlvmParameterAttribute, LLVMAttributeKindId>()
    }

    object SignExt : LlvmParameterAttribute("signext")
    object ZeroExt : LlvmParameterAttribute("zeroext")
}

sealed class LlvmFunctionAttribute(private val llvmAttributeName: String) : LlvmAttribute {

    override fun asAttributeKindId(): LLVMAttributeKindId = llvmAttributeKindIdCache.getOrPut(this) {
        getLlvmAttributeKindId(llvmAttributeName)
    }

    companion object {
        private val llvmAttributeKindIdCache = mutableMapOf<LlvmFunctionAttribute, LLVMAttributeKindId>()
    }

    object NoUnwind : LlvmFunctionAttribute("nounwind")
    object NoReturn : LlvmFunctionAttribute("noreturn")
    object NoInline : LlvmFunctionAttribute("noinline")
    object SanitizeThread : LlvmFunctionAttribute("sanitize_thread")
}

@InternalKotlinNativeApi
fun getLlvmAttributeKindId(attributeName: String): LLVMAttributeKindId {
    val attrKindId = LLVMGetEnumAttributeKindForName(attributeName, attributeName.length.signExtend())
    if (attrKindId == 0) {
        throw Error("Unable to find '$attributeName' attribute kind id")
    }
    return LLVMAttributeKindId(attrKindId)
}