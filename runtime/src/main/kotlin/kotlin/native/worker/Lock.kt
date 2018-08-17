/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.native.worker

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
                    assert(reenterCount_.get() == 0)
                    break@loop
                }
            }
        } while (true)
    }

    fun unlock() {
        if (reenterCount_.get() > 0) {
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