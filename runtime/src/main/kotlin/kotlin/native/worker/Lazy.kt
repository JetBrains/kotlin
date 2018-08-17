/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package kotlin.native.worker

import kotlin.native.internal.NoReorderFields

@SymbolName("Konan_ensureAcyclicAndSet")
private external fun ensureAcyclicAndSet(where: Any, index: Int, what: Any?): Boolean

@NoReorderFields
internal class FreezeAwareLazyImpl<out T>(initializer: () -> T) : Lazy<T> {
    // IMPORTANT: due to simplified ensureAcyclicAndSet() semantics fields here must be ordered like this,
    // as an ordinal is used to refer a field.
    private var value_: Any? = UNINITIALIZED
    private var initializer_: (() -> T)? = initializer
    private val lock_ = Lock()

    override val value: T
        get() {
            if (isFrozen) {
                locked(lock_) {
                    var result: Any? = value_
                    if (result !== UNINITIALIZED) {
                        assert(result !== INITIALIZING)
                        @Suppress("UNCHECKED_CAST")
                        return result as T
                    }
                    // Set value_ to INITIALIZING.
                    ensureAcyclicAndSet(this, 0, INITIALIZING)
                    result = initializer_!!().freeze()
                    // Set value_.
                    if (!ensureAcyclicAndSet(this, 0, result)) {
                        throw InvalidMutabilityException("Setting cyclic data via lazy in $this: $result")
                    }
                    // Clear initializer_ reference.
                    ensureAcyclicAndSet(this, 1, null)
                    @Suppress("UNCHECKED_CAST")
                    return result as T
                }
            } else {
                var result: Any? = value_
                if (result === UNINITIALIZED) {
                    result = initializer_!!()
                    if (isFrozen)
                        throw InvalidMutabilityException("$this got frozen during lazy evaluation" )
                    value_ = result
                    initializer_ = null
                }
                @Suppress("UNCHECKED_CAST")
                return result as T
            }
        }

    // Racy!
    override fun isInitialized(): Boolean = (value_ !== UNINITIALIZED) && (value_ !== INITIALIZING)

    override fun toString(): String = if (isInitialized())
        value.toString() else "Lazy value not initialized yet."
}