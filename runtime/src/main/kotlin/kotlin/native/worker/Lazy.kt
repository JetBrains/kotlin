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

internal class FreezeAwareLazyImpl<out T>(initializer: () -> T) : Lazy<T> {
    private var initializer_: (() -> T)? = initializer
    private var value_: Any? = UNINITIALIZED
    // Objects are not frozen by default on single-threaded targets, shall they?
    private val valueFrozen_ = AtomicReference<Any?>(UNINITIALIZED.freeze())

    override val value: T
        get() {
            var result: Any? = value_
            if (isFrozen) {
                if (result !== UNINITIALIZED) {
                    assert(result !== INITIALIZING)
                    @Suppress("UNCHECKED_CAST")
                    return result as T
                }
                // Barrier.
                // Note that recursive lazy initializers will hang here.
                do {
                    result = valueFrozen_.get()
                } while (result === INITIALIZING)

                if (result !== UNINITIALIZED) {
                    @Suppress("UNCHECKED_CAST")
                    return result as T
                }
                // TODO: maybe release initializer in frozen case.
                if (valueFrozen_.compareAndSwap(UNINITIALIZED, INITIALIZING) === UNINITIALIZED) {
                    result = initializer_!!().freeze()
                    val old = valueFrozen_.compareAndSwap(INITIALIZING, result)
                    assert(old === INITIALIZING)
                } else {
                    do {
                        result = valueFrozen_.get()
                    } while (result === INITIALIZING)
                }
                assert(result !== UNINITIALIZED && result !== INITIALIZING)
                @Suppress("UNCHECKED_CAST")
                return result as T
            } else {
                if (result === UNINITIALIZED) {
                    result = initializer_!!()
                    if (isFrozen)
                        throw InvalidMutabilityException(this)
                    value_ = result
                    initializer_ = null
                }
                @Suppress("UNCHECKED_CAST")
                return result as T
            }
        }

    // Racy!
    override fun isInitialized(): Boolean = (value_ !== UNINITIALIZED) ||  (valueFrozen_.get() !== UNINITIALIZED)

    override fun toString(): String = if (isInitialized())
        value.toString() else "Lazy value not initialized yet."
}