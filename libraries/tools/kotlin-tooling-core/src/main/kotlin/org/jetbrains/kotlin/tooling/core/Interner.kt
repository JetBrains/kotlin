/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.tooling.core

import org.jetbrains.kotlin.tooling.core.InternerImpl.Store.Strong
import org.jetbrains.kotlin.tooling.core.InternerImpl.Store.Weak
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.locks.Lock
import kotlin.concurrent.withLock

fun WeakInterner(lock: Lock? = null): Interner = InternerImpl(Weak(), lock)

fun Interner(lock: Lock? = null): Interner = InternerImpl(Strong(), lock)

interface Interner {
    fun <T : Any> getOrPut(value: T): T
    fun clear()
}

private class InternerImpl(
    private val store: Store,
    private val lock: Lock? = null
) : Interner {

    interface Store {
        fun <T : Any> getOrPut(value: T): T
        fun clear()

        class Weak : Store {
            private val references = WeakHashMap<Any, WeakReference<Any>>()
            override fun <T : Any> getOrPut(value: T): T {
                @Suppress("unchecked_cast")
                return (references.getOrPut(value) { WeakReference(value) }.get() ?: run {
                    references[value] = WeakReference(value)
                    value
                }) as T
            }

            override fun clear() {
                return references.clear()
            }
        }

        @Suppress("UNCHECKED_CAST")
        class Strong : Store {
            private val references = hashMapOf<Any, Any>()
            override fun <T : Any> getOrPut(value: T): T {
                return references.getOrPut(value) { value } as T
            }

            override fun clear() {
                return references.clear()
            }
        }
    }

    override fun <T : Any> getOrPut(value: T): T {
        return withLockIfAny { store.getOrPut(value) }
    }

    override fun clear() {
        return withLockIfAny { store.clear() }
    }

    private inline fun <T> withLockIfAny(action: () -> T): T {
        return if (lock != null) lock.withLock(action) else action()
    }
}
