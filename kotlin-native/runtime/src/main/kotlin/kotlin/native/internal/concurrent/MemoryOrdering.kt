/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.internal.concurrent

import kotlin.native.internal.InternalForKotlinNative

/**
 * Memory ordering for atomic operations.
 *
 * See also [LLVM Atomic Memory Ordering Constraints](https://llvm.org/docs/LangRef.html#atomic-memory-ordering-constraints)
 */
@InternalForKotlinNative
public enum class MemoryOrdering(
        /** Mapping for the runtime. **/
        // Must match MemoryOrdering.hpp
        internal val value: Int
) {
    /**
     * Guarantees single total order only on the address of the operand.
     *
     * Corresponds to LLVM `monotonic` memory ordering. (C++'s `memory_order_relaxed`)
     */
    RELAXED(0),

    /**
     * Can be paired with [RELEASE] and [ACQUIRE_RELEASE] operations to form happens-before relationship.
     *
     * Corresponds to LLVM `acquire` memory ordering. (C++'s `memory_order_acquire`)
     */
    ACQUIRE(1),

    /**
     * Can be paired with [ACQUIRE] and [ACQUIRE_RELEASE] operations to form happens-before relationship.
     *
     * Corresponds to LLVM `release` memory ordering. (C++'s `memory_order_release`)
     */
    RELEASE(2),

    /**
     * Can be paired with [ACQUIRE] and [RELEASE] operations to form happens-before relationship.
     *
     * Corresponds to LLVM `acq_rel` memory ordering. (C++'s `memory_order_acq_rel`)
     */
    ACQUIRE_RELEASE(3),

    /**
     * Guarantees a global total order of all [SEQUENTIALLY_CONSISTENT] operations on all addresses.
     *
     * Corresponds to LLVM `seq_cst` memory ordering. (C++'s `memory_order_seq_cst`; or Java's `volatile`)
     */
    SEQUENTIALLY_CONSISTENT(4);
}