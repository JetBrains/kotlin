@file:JvmVersion
@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StandardKt")
package kotlin

import kotlin.jvm.internal.unsafe.*

/**
 * Executes the given function [block] while holding the monitor of the given object [lock].
 */
@kotlin.internal.InlineOnly
public inline fun <R> synchronized(lock: Any, block: () -> R): R {
    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE", "INVISIBLE_MEMBER")
    monitorEnter(lock)
    try {
        return block()
    }
    finally {
        @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE", "INVISIBLE_MEMBER")
        monitorExit(lock)
    }
}
