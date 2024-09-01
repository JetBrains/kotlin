/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalForeignApi::class)

package kotlin.native.concurrent

import kotlin.native.internal.*
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * A shared reference to a Kotlin object that doesn't freeze the referred object when it gets frozen itself.
 *
 * After freezing can be safely passed between workers, but [value] can only be accessed on
 * the worker [WorkerBoundReference] was created on, unless the referred object is frozen too.
 *
 * Note: Garbage collector currently cannot free any reference cycles with frozen [WorkerBoundReference] in them.
 * To resolve such cycles consider using [AtomicReference]`<WorkerBoundReference?>` which can be explicitly
 * nulled out.
 */
@FreezingIsDeprecated
@ObsoleteWorkersApi
public class WorkerBoundReference<out T : Any>(
        /**
         * The referenced value.
         */
        public val value: T
) {
    /**
     * The referenced value. Never null.
     */
    public val valueOrNull: T?
        get() = value

    /**
     * Worker that [value] is bound to.
     */
    public val worker: Worker = Worker.current
}
