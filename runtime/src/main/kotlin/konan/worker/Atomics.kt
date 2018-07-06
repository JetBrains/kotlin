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

package konan.worker

import konan.internal.Frozen
import konan.SymbolName
import kotlinx.cinterop.NativePtr

@Frozen
class AtomicInt(private var value: Int = 0) {

    /**
     * Increments the value by [delta] and returns the new value.
     */
    @SymbolName("Kotlin_AtomicInt_addAndGet")
    external fun addAndGet(delta: Int): Int

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     * Returns the old value.
     */
    @SymbolName("Kotlin_AtomicInt_compareAndSwap")
    external fun compareAndSwap(expected: Int, new: Int): Int

    /**
     * Increments value by one.
     */
    fun increment(): Int = addAndGet(1)

    /**
     * Decrements value by one.
     */
    fun decrement(): Int = addAndGet(-1)

    /**
     * Returns the current value.
     */
    fun get(): Int = value

    /**
     * Returns the string representation of this object.
     */
    public override fun toString(): String = "AtomicInt $value"
}

@Frozen
class AtomicLong(private var value: Long = 0)  {

    /**
     * Increments the value by [delta] and returns the new value.
     */
    @SymbolName("Kotlin_AtomicLong_addAndGet")
    external fun addAndGet(delta: Long): Long

    /**
     * Increments the value by [delta] and returns the new value.
     */
    fun addAndGet(delta: Int): Long = addAndGet(delta.toLong())

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     * Returns the old value.
     */
    @SymbolName("Kotlin_AtomicLong_compareAndSwap")
    external fun compareAndSwap(expected: Long, new: Long): Long

    /**
     * Increments value by one.
     */
    fun increment(): Long = addAndGet(1L)

    /**
     * Decrements value by one.
     */
    fun decrement(): Long = addAndGet(-1L)

    /**
     * Returns the current value.
     */
    fun get(): Long = value

    /**
     * Returns the string representation of this object.
     */
    public override fun toString(): String = "AtomicLong $value"
}

@Frozen
class AtomicNativePtr(private var value: NativePtr) {
    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     * Returns the old value.
     */
    @SymbolName("Kotlin_AtomicNativePtr_compareAndSwap")
    external fun compareAndSwap(expected: NativePtr, new: NativePtr): NativePtr

    /**
     * Returns the current value.
     */
    fun get(): NativePtr = value
}

@SymbolName("Kotlin_AtomicReference_checkIfFrozen")
external private fun checkIfFrozen(ref: Any?)

/**
 * An atomic reference to a frozen Kotlin object. Can be used in concurrent scenarious
 * and must be zeroed out (with `compareAndSwap(get(), null)`) once no longer needed.
 * Otherwise memory leak could happen.
 */
@Frozen
class AtomicReference<T>(private var value: T? = null) {
    // A spinlock to fix potential ARC race. Not an AtomicInt just for the effeciency sake.
    private var lock: Int = 0

    /**
     * Creates a new atomic reference pointing to given [ref]. If reference is not frozen,
     * @InvalidMutabilityException is thrown.
     */
    init {
        checkIfFrozen(value)
    }

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     * If [new] value is not null, it must be frozen or permanent object, otherwise an
     * @InvalidMutabilityException is thrown.
     * Returns the old value.
     */
    @SymbolName("Kotlin_AtomicReference_compareAndSwap")
    external public fun compareAndSwap(expected: T?, new: T?): T?

    /**
     * Returns the current value.
     */
    @SymbolName("Kotlin_AtomicReference_get")
    external public fun get(): T?
}

internal object UNINITIALIZED

internal object INITIALIZING

@Frozen
internal class AtomicLazyImpl<out T>(initializer: () -> T) : Lazy<T> {
    private val initializer_ = AtomicReference<Function0<T>?>(initializer.freeze())
    private val value_ = AtomicReference<Any?>(UNINITIALIZED)

    override val value: T
        get() {
            if (value_.compareAndSwap(UNINITIALIZED, INITIALIZING) === UNINITIALIZED) {
                // We execute exclusively here.
                val ctor = initializer_.get()
                if (ctor != null && initializer_.compareAndSwap(ctor, null) === ctor) {
                    value_.compareAndSwap(INITIALIZING, ctor().freeze())
                } else {
                    // Something wrong.
                    assert(false)
                }
            }
            var result: Any?
            do {
                result = value_.get()
            } while (result === INITIALIZING)

            assert(result !== UNINITIALIZED && result != INITIALIZING)
            @Suppress("UNCHECKED_CAST")
            return result as T
        }

    // Racy!
    override fun isInitialized(): Boolean = value_.get() !== UNINITIALIZED

    override fun toString(): String = if (isInitialized())
        value_.get().toString() else "Lazy value not initialized yet."
}

/**
 * Atomic lazy initializer, could be used in frozen objects, freezes initializing lambda,
 * so use very carefully. Also, as with other uses of an @AtomicReference may potentially
 * leak memory, so it is recommended to use `atomicLazy` in cases of objects living forever,
 * such as object signletons, or in cases where it's guaranteed not to have cyclical garbage.
 */
public fun <T> atomicLazy(initializer: () -> T): Lazy<T> = AtomicLazyImpl(initializer)