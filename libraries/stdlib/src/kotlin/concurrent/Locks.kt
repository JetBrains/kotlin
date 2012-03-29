package kotlin.concurrent

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock

/**
Executes given calculation under lock
Returns result of the calculation
*/
public inline fun <erased T> Lock.withLock(action: ()->T) : T {
    lock()
    try {
        return action()
    }
    finally {
        unlock();
    }
}

/**
Executes given calculation under read lock
Returns result of the calculation
*/
public inline fun <erased T> ReentrantReadWriteLock.read(action: ()->T) : T {
    val rl = readLock().sure()
    rl.lock()
    try {
        return action()
    }
    finally {
        rl.unlock()
    }
}

/**
Executes given calculation under write lock.
The method does upgrade from read to write lock if needed
If such write has been initiated by checking some condition, the condition must be rechecked inside the action to avoid possible races
Returns result of the calculation
*/
public inline fun <erased T> ReentrantReadWriteLock.write(action: ()->T) : T {
    val rl = readLock().sure()

    val readCount = if (getWriteHoldCount() == 0) getReadHoldCount() else 0
    readCount times { rl.unlock() }

    val wl = writeLock().sure()
    wl.lock()
    try {
        return action()
    }
    finally {
        readCount times { rl.lock() }
        wl.unlock()
    }
}
