/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:OptIn(ExperimentalAtomicApi::class)

package kotlin.native.concurrent

import kotlin.experimental.ExperimentalNativeApi
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.decrementAndFetch
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@ThreadLocal
private object CurrentThread {
    val id = Any()
}

@OptIn(ExperimentalNativeApi::class)
internal class Lock {
    private val locker_ = AtomicInt(0)
    private val reenterCount_ = AtomicInt(0)

    // TODO: make it properly reschedule instead of spinning.
    @OptIn(ExperimentalStdlibApi::class)
    fun lock() {
        val lockData = CurrentThread.id.hashCode()
        loop@ do {
            val old = locker_.compareAndExchange(0, lockData)
            when (old) {
                lockData -> {
                    // Was locked by us already.
                    reenterCount_.incrementAndFetch()
                    break@loop
                }
                0 -> {
                    // We just got the lock.
                    assert(reenterCount_.load() == 0)
                    break@loop
                }
            }
        } while (true)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun unlock() {
        if (reenterCount_.load() > 0) {
            reenterCount_.decrementAndFetch()
        } else {
            val lockData = CurrentThread.id.hashCode()
            val old = locker_.compareAndExchange(lockData, 0)
            assert(old == lockData)
        }
    }
}

internal inline fun <R> locked(lock: Lock, block: () -> R): R {
    lock.lock()
    try {
        return block()
    } finally {
        lock.unlock()
    }
}
