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
// We benchmark both new and legacy MM.
@file:OptIn(FreezingIsDeprecated::class)

package org.jetbrains.ring

import kotlin.concurrent.AtomicReference as KAtomicRef
import kotlin.native.concurrent.isFrozen
import kotlin.native.concurrent.freeze

public actual class AtomicRef<T> constructor(@PublishedApi internal val a: KAtomicRef<T>) {
    public actual inline var value: T
        get() = a.value
        set(value) {
            if (a.isFrozen) value.freeze()
            a.value = value
        }

    public actual inline fun lazySet(value: T) {
        if (a.isFrozen) value.freeze()
        a.value = value
    }

    public actual inline fun compareAndSet(expect: T, update: T): Boolean {
        if (a.isFrozen) update.freeze()
        return a.compareAndSet(expect, update)
    }

    public actual fun getAndSet(value: T): T {
        if (a.isFrozen) value.freeze()
        while (true) {
            val cur = a.value
            if (cur === value) return cur
            if (a.compareAndExchange(cur, value) === cur) return cur
        }
    }

    override fun toString(): String = value.toString()
}

public actual fun <T> atomic(initial: T): AtomicRef<T> = AtomicRef<T>(KAtomicRef(initial))
