/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

import kotlin.native.internal.Frozen

@ThreadLocal
private object CurrentThread {
    val id = Any().freeze()
}

@Frozen
internal class Lock {
    private val locker_ = AtomicInt(0)
    private val reenterCount_ = AtomicInt(0)

    // TODO: make it properly reschedule instead of spinning.
    fun lock() {
        val lockData = CurrentThread.id.hashCode()
        loop@ do {
            val old = locker_.compareAndSwap(0, lockData)
            when (old) {
                lockData -> {
                    // Was locked by us already.
                    reenterCount_.increment()
                    break@loop
                }
                0 -> {
                    // We just got the lock.
                    assert(reenterCount_.value == 0)
                    break@loop
                }
            }
        } while (true)
    }

    fun unlock() {
        if (reenterCount_.value > 0) {
            reenterCount_.decrement()
        } else {
            val lockData = CurrentThread.id.hashCode()
            val old = locker_.compareAndSwap(lockData, 0)
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