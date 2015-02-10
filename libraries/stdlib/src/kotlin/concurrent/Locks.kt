package kotlin.concurrent

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.concurrent.CountDownLatch

/**
 * Executes the given calculation under this lock.
 * @return result of the calculation.
 */
public inline fun <T> Lock.withLock(action: () -> T): T {
    lock()
    try {
        return action()
    } finally {
        unlock();
    }
}

/**
 * Executes the given calculation under the read lock of this lock.
 * @return result of the calculation.
 */
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
 * Executes the given calculation under the write lock of this lock.
 * The method does upgrade from read to write lock if needed
 * If such write has been initiated by checking some condition, the condition must be rechecked inside the action to avoid possible races
 * @return result of the calculation.
 */
public inline fun <T> ReentrantReadWriteLock.write(action: () -> T): T {
    val rl = readLock()

    val readCount = if (getWriteHoldCount() == 0) getReadHoldCount() else 0
    readCount times { rl.unlock() }

    val wl = writeLock()
    wl.lock()
    try {
        return action()
    } finally {
        readCount times { rl.lock() }
        wl.unlock()
    }
}

/**
 * Execute the given calculation and await for CountDownLatch
 * @return result of the calculation.
 */
public fun <T> Int.latch(operation: CountDownLatch.() -> T): T {
    val latch = CountDownLatch(this)
    val result = latch.operation()
    latch.await()
    return result
}
