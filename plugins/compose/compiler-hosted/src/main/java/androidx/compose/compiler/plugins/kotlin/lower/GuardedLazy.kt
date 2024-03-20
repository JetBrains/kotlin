/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.lower

import kotlin.reflect.KProperty
class GuardedLazy<out T>(initializer: () -> T) {
    private var _value: Any? = UNINITIALIZED_VALUE
    private var _initializer: (() -> T)? = initializer

    fun value(name: String): T {
        if (_value === UNINITIALIZED_VALUE) {
            try {
                _value = _initializer!!()
                _initializer = null
            } catch (e: Throwable) {
                throw java.lang.IllegalStateException("Error initializing $name", e)
            }
        }
        @Suppress("UNCHECKED_CAST")
        return _value as T
    }
}

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> GuardedLazy<T>.getValue(thisRef: Any?, property: KProperty<*>) =
    value(property.name)

fun <T> guardedLazy(initializer: () -> T) = GuardedLazy<T>(initializer)
