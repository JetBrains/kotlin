/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.internal.concurrent

import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalNativeApi
import kotlin.math.max
import kotlin.native.internal.concurrent.Monitor.MonitoredSection
import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.NativePtr
import kotlin.native.internal.escapeAnalysis.Escapes
import kotlin.native.ref.Cleaner
import kotlin.native.ref.createCleaner
import kotlin.time.Duration
import kotlin.time.TimeMark

@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS")
@ExperimentalForeignApi
internal value class Monitor private constructor(private val impl: NativePtr) {

    @PublishedApi
    @GCUnsafeCall("Kotlin_Monitor_enter")
    internal external fun enter()

    @PublishedApi
    @GCUnsafeCall("Kotlin_Monitor_leave")
    internal external fun leave()

    @PublishedApi
    @GCUnsafeCall("Kotlin_Monitor_wait")
    internal external fun wait(timeoutMillis: Long)

    @PublishedApi
    @GCUnsafeCall("Kotlin_Monitor_notify")
    internal external fun notify()

    @PublishedApi
    @GCUnsafeCall("Kotlin_Monitor_notifyAll")
    internal external fun notifyAll()

    @GCUnsafeCall("Kotlin_Monitor_destroy")
    internal external fun destroy()

    @OptIn(ExperimentalNativeApi::class)
    internal fun cleaner(): Cleaner = createCleaner(this) {
        it.destroy()
    }

    @Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS", "NOTHING_TO_INLINE")
    internal value class MonitoredSection @PublishedApi internal constructor(internal val monitor: Monitor) : AutoCloseable {
        override fun close() {
            monitor.leave()
        }

        internal inline fun wait() {
            monitor.wait(0)
        }

        internal inline fun wait(timeout: Duration) {
            // FIXME maybe just not wait at all if timeout <= 0?
            // wait at least 1ms, 0ms is reserved for untimed wait
            val millis = max(timeout.inWholeMilliseconds, 1L)
            monitor.wait(millis)
        }

        internal inline fun waitUntil(timeMark: TimeMark) {
            wait(-timeMark.elapsedNow())
        }

        internal inline fun notify() {
            monitor.notify()
        }

        internal inline fun notifyAll() {
            monitor.notifyAll()
        }
    }

    internal companion object {
        @Escapes.Nothing
        @GCUnsafeCall("Kotlin_Monitor_allocate")
        internal external fun allocate(): Monitor
    }
}

@ExperimentalForeignApi
internal inline fun <T> synchronized(monitor: Monitor, block: MonitoredSection.() -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    monitor.enter()
    return MonitoredSection(monitor).use {
        it.block()
    }
}

@ExperimentalNativeApi
@ExperimentalForeignApi
internal open class Synchronizable {
    @PublishedApi
    internal val monitor: Monitor = Monitor.allocate()

    @Suppress("unused")
    private val monitorCleaner = monitor.cleaner()

    @IgnorableReturnValue
    internal inline fun <T> synchronized(block: MonitoredSection.() -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return synchronized(monitor, block)
    }
}
