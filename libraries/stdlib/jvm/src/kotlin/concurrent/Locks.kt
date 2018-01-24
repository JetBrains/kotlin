/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

@file:JvmVersion
@file:JvmName("LocksKt")
package kotlin.concurrent

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.concurrent.CountDownLatch

/**
 * Executes the given [action] under this lock.
 * @return the return value of the action.
 */
@kotlin.internal.InlineOnly
public inline fun <T> Lock.withLock(action: () -> T): T {
    lock()
    try {
        return action()
    } finally {
        unlock()
    }
}

/**
 * Executes the given [action] under the read lock of this lock.
 * @return the return value of the action.
 */
@kotlin.internal.InlineOnly
public inline fun <T> ReentrantReadWriteLock.read(action: () -> T): T {
    val rl = readLock()
    rl.lock()
    try {
        return action()
    } finally {
        rl.unlock()
    }
}

/**
 * Executes the given [action] under the write lock of this lock.
 *
 * The function does upgrade from read to write lock if needed, but this upgrade is not atomic
 * as such upgrade is not supported by [ReentrantReadWriteLock].
 * In order to do such upgrade this function first releases all read locks held by this thread,
 * then acquires write lock, and after releasing it acquires read locks back again.
 *
 * Therefore if the [action] inside write lock has been initiated by checking some condition,
 * the condition must be rechecked inside the [action] to avoid possible races.
 *
 * @return the return value of the action.
 */
@kotlin.internal.InlineOnly
public inline fun <T> ReentrantReadWriteLock.write(action: () -> T): T {
    val rl = readLock()

    val readCount = if (writeHoldCount == 0) readHoldCount else 0
    repeat(readCount) { rl.unlock() }

    val wl = writeLock()
    wl.lock()
    try {
        return action()
    } finally {
        repeat(readCount) { rl.lock() }
        wl.unlock()
    }
}
