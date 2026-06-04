/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*


internal class ReferenceTLS(private val llvm: CodegenLlvmHelpers) {
    private var count = 0

    private val key by lazy {
        val global = LLVMAddGlobal(llvm.module, llvm.pointerType, "__KonanTlsKey")!!
        LLVMSetLinkage(global, LLVMLinkage.LLVMInternalLinkage)
        LLVMSetInitializer(global, llvm.kNull)
        global
    }

    fun add(alignment: Int): TLSAddressAccess {
        require(llvm.runtime.pointerAlignment % alignment == 0)
        val index = count++
        return TLSAddressAccess(this, index)
    }

    context(gen: FunctionGenerationContext)
    fun generateGetAddress(index: Int): LLVMValueRef {
        return gen.call(gen.llvm.lookupTLS, listOf(key, gen.llvm.int32(index)))
    }

    context(gen: FunctionGenerationContext)
    fun generateAllocate() {
        if (count > 0) {
            val memory = gen.param(1)
            gen.call(gen.llvm.addTLSRecord, listOf(memory, key, gen.llvm.int32(count)))
        }
    }
}

internal class TLSAddressAccess(private val tls: ReferenceTLS, private val index: Int) : AddressAccess() {
    override fun getAddress(generationContext: FunctionGenerationContext?): LLVMValueRef {
        return with(generationContext!!) {
            tls.generateGetAddress(index)
        }
    }
}