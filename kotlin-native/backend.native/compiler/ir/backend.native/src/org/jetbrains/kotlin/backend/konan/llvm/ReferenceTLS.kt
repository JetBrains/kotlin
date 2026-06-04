/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*


internal class ReferenceTLS(private val llvm: CodegenLlvmHelpers) {
    private sealed class State {
        /**
         * The TLS is being built. Entries can be added but not used.
         * Note: it is possible to support using entries in this state,
         * but this would complicate things and there is no need.
         */
        data class Building(var count: Int) : State()

        /**
         * The TLS is built and finalized, it is not empty. Entries can be used but not added.
         */
        data class Built(val count: Int, val key: LLVMValueRef) : State()

        /**
         * The TLS is built and finalized, it is empty. New entries can't be added.
         */
        data object BuiltEmpty : State()

        inline fun <reified T : State> expect(): T = this.let {
            check(it is T) { "Unexpected state: $it" }
            it
        }
    }

    private var state: State = State.Building(count = 0)

    fun add(alignment: Int): TLSAddressAccess {
        val state = this.state.expect<State.Building>()

        require(llvm.runtime.pointerAlignment % alignment == 0)
        val index = state.count++
        return TLSAddressAccess(this, index)
    }

    fun build() {
        val state = this.state.expect<State.Building>()
        if (state.count == 0) {
            this.state = State.BuiltEmpty
            return
        }

        val global = LLVMAddGlobal(llvm.module, llvm.pointerType, "__KonanTlsKey")!!
        LLVMSetLinkage(global, LLVMLinkage.LLVMInternalLinkage)
        LLVMSetInitializer(global, llvm.kNull)

        this.state = State.Built(state.count, key = global)
    }

    context(gen: FunctionGenerationContext)
    fun generateGetAddress(index: Int): LLVMValueRef {
        val state = this.state.expect<State.Built>()
        require(index < state.count) { "TLS index out of bounds: $index" }
        return gen.call(gen.llvm.lookupTLS, listOf(state.key, gen.llvm.int32(index)))
    }

    context(gen: FunctionGenerationContext)
    fun generateAllocate() {
        if (this.state is State.BuiltEmpty) return

        val state = this.state.expect<State.Built>()
        check(state.count > 0) { "Unexpected TLS count: ${state.count}" }
        val memory = gen.param(1)
        gen.call(gen.llvm.addTLSRecord, listOf(memory, state.key, gen.llvm.int32(state.count)))
    }
}

internal class TLSAddressAccess(private val tls: ReferenceTLS, private val index: Int) : AddressAccess() {
    override fun getAddress(generationContext: FunctionGenerationContext?): LLVMValueRef {
        return with(generationContext!!) {
            tls.generateGetAddress(index)
        }
    }
}