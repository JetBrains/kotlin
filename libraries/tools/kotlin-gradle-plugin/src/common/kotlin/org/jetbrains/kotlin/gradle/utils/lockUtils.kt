/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import java.util.concurrent.locks.Lock
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Executes [block] while holding [this] lock.
 *
 * The lock is acquired before [block] is executed, and is always released after the block completes,
 * regardless of whether the block executes successfully or throws an exception.
 */
@OptIn(ExperimentalContracts::class)
internal inline fun Lock.use(
    block: () -> Unit,
) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    lock()
    try {
        block()
    } finally {
        unlock()
    }
}


/**
 * Executes [block] while holding [this] lock, and returns a result.
 *
 * The lock is acquired before [block] is executed, and is always released after the block completes,
 * regardless of whether the block executes successfully or throws an exception.
 */
@OptIn(ExperimentalContracts::class)
internal inline fun <R> Lock.use(
    block: () -> R,
): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    lock()
    try {
        return block()
    } finally {
        unlock()
    }
}
