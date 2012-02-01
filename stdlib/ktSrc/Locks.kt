package std.concurrent

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock

/*
Executes given calculation under lock
Returns result of the calculation
*/
inline fun <erased T> Lock.withLock(action: ()->T) : T {
    lock()
    try {
        return action()
    }
    finally {
        unlock();
    }
}

/*
Executes given calculation under read lock
Returns result of the calculation
*/
inline fun <erased T> ReentrantReadWriteLock.read(action: ()->T) : T {
    val rl = readLock().sure()
    rl.lock()
    try {
        return action()
    }
    finally {
        rl.unlock()
    }
}

/*
Executes given calculation under write lock.
The method does upgrade from read to write lock if needed
If such write has been initiated by checking some condition, the condition must be rechecked inside the action to avoid possible races
Returns result of the calculation
*/
inline fun <erased T> ReentrantReadWriteLock.write(action: ()->T) : T {
    val rl = readLock().sure()
    var upgradeCount = getWriteHoldCount()
    if(upgradeCount == 0) {
        upgradeCount = getReadHoldCount()
        if(upgradeCount > 0)
            rl.unlock(upgradeCount)
    }
    else {
        upgradeCount = 0
    }

    val wl = writeLock().sure()
    wl.lock()
    try {
        return action()
    }
    finally {
        if(upgradeCount > 0) {
            rl.lock(upgradeCount)
        }
        wl.unlock()
    }
}

inline private fun ReadLock.lock(readCount: Int) {
    if(readCount > 0) {
        for(j in 1..readCount) {
            lock()
        }
    }
}

inline private fun ReadLock.unlock(readCount: Int) {
    if(readCount > 0) {
        for(j in 1..readCount) {
            unlock()
        }
    }
}
