/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

import kotlin.native.internal.ExportTypeInfo
import kotlin.native.internal.Frozen
import kotlin.native.internal.LeakDetectorCandidate
import kotlin.native.internal.NoReorderFields
import kotlin.native.SymbolName
import kotlinx.cinterop.NativePtr

/**
 * Atomic values and freezing: atomics [AtomicInt], [AtomicLong], [AtomicNativePtr] and [AtomicReference]
 * are unique types with regard to freezing. Namely, they provide mutating operations, while can participate
 * in frozen subgraphs. So shared frozen objects can have fields of atomic types.
 */
@Frozen
public class AtomicInt(private var value_: Int) {
    /**
     * The value being held by this class.
     */
    public var value: Int
            get() = getImpl()
            set(new) = setImpl(new)

    /**
     * Increments the value by [delta] and returns the new value.
     *
     * @param delta the value to add
     * @return the new value
     */
    @SymbolName("Kotlin_AtomicInt_addAndGet")
    external public fun addAndGet(delta: Int): Int

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     *
     * @param expected the expected value
     * @param new the new value
     * @return the old value
     */
    @SymbolName("Kotlin_AtomicInt_compareAndSwap")
    external public fun compareAndSwap(expected: Int, new: Int): Int

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     *
     * @param expected the expected value
     * @param new the new value
     * @return true if successful
     */
    @SymbolName("Kotlin_AtomicInt_compareAndSet")
    external public fun compareAndSet(expected: Int, new: Int): Boolean

    /**
     * Increments value by one.
     */
    public fun increment(): Unit {
        addAndGet(1)
    }

    /**
     * Decrements value by one.
     */
    public fun decrement(): Unit {
        addAndGet(-1)
    }

    /**
     * Returns the string representation of this object.
     *
     * @return the string representation
     */
    public override fun toString(): String = value.toString()

    // Implementation details.
    @SymbolName("Kotlin_AtomicInt_set")
    private external fun setImpl(new: Int): Unit

    @SymbolName("Kotlin_AtomicInt_get")
    private external fun getImpl(): Int
}

@Frozen
public class AtomicLong(private var value_: Long = 0)  {
    /**
     * The value being held by this class.
     */
    public var value: Long
        get() = getImpl()
        set(new) = setImpl(new)

    /**
     * Increments the value by [delta] and returns the new value.
     *
     * @param delta the value to add
     * @return the new value
     */
    @SymbolName("Kotlin_AtomicLong_addAndGet")
    external public fun addAndGet(delta: Long): Long

    /**
     * Increments the value by [delta] and returns the new value.
     *
     * @param delta the value to add
     * @return the new value
     */
    public fun addAndGet(delta: Int): Long = addAndGet(delta.toLong())

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     *
     * @param expected the expected value
     * @param new the new value
     * @return the old value
     */
    @SymbolName("Kotlin_AtomicLong_compareAndSwap")
    external public fun compareAndSwap(expected: Long, new: Long): Long

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     *
     * @param expected the expected value
     * @param new the new value
     * @return true if successful, false if state is unchanged
     */
    @SymbolName("Kotlin_AtomicLong_compareAndSet")
    external public fun compareAndSet(expected: Long, new: Long): Boolean

    /**
     * Increments value by one.
     */
    public fun increment(): Unit {
        addAndGet(1L)
    }

    /**
     * Decrements value by one.
     */
    fun decrement(): Unit {
        addAndGet(-1L)
    }

    /**
     * Returns the string representation of this object.
     *
     * @return the string representation of this object
     */
    public override fun toString(): String = value.toString()

    // Implementation details.
    @SymbolName("Kotlin_AtomicLong_set")
    private external fun setImpl(new: Long): Unit

    @SymbolName("Kotlin_AtomicLong_get")
    private external fun getImpl(): Long
}

@Frozen
public class AtomicNativePtr(private var value_: NativePtr) {
    /**
     * The value being held by this class.
     */
    public var value: NativePtr
        get() = getImpl()
        set(new) = setImpl(new)

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     * If [new] value is not null, it must be frozen or permanent object.
     *
     * @param expected the expected value
     * @param new the new value
     * @throws InvalidMutabilityException if [new] is not frozen or a permanent object
     * @return the old value
     */
    @SymbolName("Kotlin_AtomicNativePtr_compareAndSwap")
    external public fun compareAndSwap(expected: NativePtr, new: NativePtr): NativePtr

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     *
     * @param expected the expected value
     * @param new the new value
     * @return true if successful
     */
    @SymbolName("Kotlin_AtomicNativePtr_compareAndSet")
    external public fun compareAndSet(expected: NativePtr, new: NativePtr): Boolean

    /**
     * Returns the string representation of this object.
     *
     * @return string representation of this object
     */
    public override fun toString(): String = value.toString()

    // Implementation details.
    @SymbolName("Kotlin_AtomicNativePtr_set")
    private external fun setImpl(new: NativePtr): Unit

    @SymbolName("Kotlin_AtomicNativePtr_get")
    private external fun getImpl(): NativePtr
}


private fun idString(value: Any) = "${value.hashCode().toUInt().toString(16)}"

private fun debugString(value: Any?): String {
    if (value == null) return "null"
    return "${value::class.qualifiedName}: ${idString(value)}"
}

/**
 * An atomic reference to a frozen Kotlin object. Can be used in concurrent scenarious
 * but frequently shall be of nullable type and be zeroed out once no longer needed.
 * Otherwise memory leak could happen. To detect such leaks [kotlin.native.internal.GC.detectCycles]
 * in debug mode could be helpful.
 */
@Frozen
@LeakDetectorCandidate
@NoReorderFields
public class AtomicReference<T> {
    private var value_: T

    // A spinlock to fix potential ARC race.
    private var lock: Int = 0

    // Optimization for speeding up access.
    private var cookie: Int = 0

    /**
     * Creates a new atomic reference pointing to given [ref].
     * @throws InvalidMutabilityException if reference is not frozen.
     */
    constructor(value: T) {
        checkIfFrozen(value)
        value_ = value
    }

    /**
     * The referenced value.
     * Gets the value or sets the [new] value. If [new] value is not null,
     * it must be frozen or permanent object.
     *
     * @throws InvalidMutabilityException if the value is not frozen or a permanent object
     */
    public var value: T
        get() = @Suppress("UNCHECKED_CAST")(getImpl() as T)
        set(new) = setImpl(new)

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     * Note that comparison is identity-based, not value-based.
     * If [new] value is not null, it must be frozen or permanent object.
     *
     * @param expected the expected value
     * @param new the new value
     * @throws InvalidMutabilityException if the value is not frozen or a permanent object
     * @return the old value
     */
    @SymbolName("Kotlin_AtomicReference_compareAndSwap")
    external public fun compareAndSwap(expected: T, new: T): T

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     * Note that comparison is identity-based, not value-based.
     *
     * @param expected the expected value
     * @param new the new value
     * @return true if successful
     */
    @SymbolName("Kotlin_AtomicReference_compareAndSet")
    external public fun compareAndSet(expected: T, new: T): Boolean

    /**
     * Returns the string representation of this object.
     *
     * @return string representation of this object
     */
    public override fun toString(): String =
            "${debugString(this)} -> ${debugString(value)}"

    // Implementation details.
    @SymbolName("Kotlin_AtomicReference_set")
    private external fun setImpl(new: Any?): Unit

    @SymbolName("Kotlin_AtomicReference_get")
    private external fun getImpl(): Any?
}

/**
 * An atomic reference to a Kotlin object. Can be used in concurrent scenarious, but must be frozen first,
 * otherwise behaves as regular box for the value. If frozen, shall be zeroed out once no longer needed.
 * Otherwise memory leak could happen. To detect such leaks [kotlin.native.internal.GC.detectCycles]
 * in debug mode could be helpful.
 */
@NoReorderFields
@LeakDetectorCandidate
@ExportTypeInfo("theFreezableAtomicReferenceTypeInfo")
public class FreezableAtomicReference<T>(private var value_: T) {
    // A spinlock to fix potential ARC race.
    private var lock: Int = 0

    // Optimization for speeding up access.
    private var cookie: Int = 0

    /**
     * The referenced value.
     * Gets the value or sets the [new] value. If [new] value is not null,
     * and `this` is frozen - it must be frozen or permanent object.
     *
     * @throws InvalidMutabilityException if the value is not frozen or a permanent object
     */
    public var value: T
        get() = @Suppress("UNCHECKED_CAST")(getImpl() as T)
        set(new) {
            if (this.isFrozen)
                setImpl(new)
            else
                value_ = new
        }

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     * If [new] value is not null and object is frozen, it must be frozen or permanent object.
     *
     * @param expected the expected value
     * @param new the new value
     * @throws InvalidMutabilityException if the value is not frozen or a permanent object
     * @return the old value
     */
     public fun compareAndSwap(expected: T, new: T): T {
        return if (this.isFrozen) @Suppress("UNCHECKED_CAST")(compareAndSwapImpl(expected, new) as T) else {
            val old = value_
            if (old === expected) value_ = new
            old
        }
    }

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     * Note that comparison is identity-based, not value-based.
     *
     * @param expected the expected value
     * @param new the new value
     * @return true if successful
     */
    public fun compareAndSet(expected: T, new: T): Boolean {
        if (this.isFrozen) return compareAndSetImpl(expected, new)
        val old = value_
        if (old === expected) {
            value_ = new
            return true
        } else {
            return false
        }
    }

    /**
     * Returns the string representation of this object.
     *
     * @return string representation of this object
     */
    public override fun toString(): String =
            "${debugString(this)} -> ${debugString(value)}"

    // Implementation details.
    @SymbolName("Kotlin_AtomicReference_set")
    private external fun setImpl(new: Any?): Unit

    @SymbolName("Kotlin_AtomicReference_get")
    private external fun getImpl(): Any?

    @SymbolName("Kotlin_AtomicReference_compareAndSwap")
    private external fun compareAndSwapImpl(expected: Any?, new: Any?): Any?

    @SymbolName("Kotlin_AtomicReference_compareAndSet")
    private external fun compareAndSetImpl(expected: Any?, new: Any?): Boolean
}
