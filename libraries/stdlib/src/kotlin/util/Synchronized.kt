@file:JvmVersion
@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StandardKt")
package kotlin

import kotlin.jvm.internal.unsafe.*

/**
 * Executes the given function [block] while holding the monitor of the given object [lock].
 */
public inline fun <R> synchronized(lock: Any, block: () -> R): R {
    monitorEnter(lock)
    try {
        return block()
    }
    finally {
        monitorExit(lock)
    }
}
