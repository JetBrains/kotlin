/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

import kotlinx.cinterop.NativePtr
import kotlin.native.internal.*
import kotlin.reflect.*
import kotlin.concurrent.*

/**
 * Wrapper around [Int] with atomic synchronized operations.
 *
 * Legacy MM: Atomic values and freezing: this type is unique with regard to freezing.
 * Namely, it provides mutating operations, while can participate in frozen subgraphs.
 * So shared frozen objects can have mutable fields of [AtomicInt] type.
 */
@Frozen
@OptIn(FreezingIsDeprecated::class, ExperimentalStdlibApi::class)
public class AtomicInt(public @Volatile var value: Int) {
    /**
     * Increments the value by [delta] and returns the new value.
     *
     * @param delta the value to add
     * @return the new value
     */
    public fun addAndGet(delta: Int): Int = this::value.getAndAddField(delta) + delta

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     *
     * @param expected the expected value
     * @param new the new value
     * @return the old value
     */
    public fun compareAndSwap(expected: Int, new: Int): Int = this::value.compareAndSwapField(expected, new)

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     *
     * @param expected the expected value
     * @param new the new value
     * @return true if successful
     */
    public fun compareAndSet(expected: Int, new: Int): Boolean = this::value.compareAndSetField(expected, new)

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
}

/**
 * Wrapper around [Long] with atomic synchronized operations.
 *
 * Legacy MM: Atomic values and freezing: this type is unique with regard to freezing.
 * Namely, it provides mutating operations, while can participate in frozen subgraphs.
 * So shared frozen objects can have mutable fields of [AtomicLong] type.
 */
@Frozen
@OptIn(FreezingIsDeprecated::class, ExperimentalStdlibApi::class)
public class AtomicLong(public @Volatile var value: Long = 0)  {

    /**
     * Increments the value by [delta] and returns the new value.
     *
     * @param delta the value to add
     * @return the new value
     */
    public fun addAndGet(delta: Long): Long = this::value.getAndAddField(delta) + delta

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
    public fun compareAndSwap(expected: Long, new: Long): Long = this::value.compareAndSwapField(expected, new)

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     *
     * @param expected the expected value
     * @param new the new value
     * @return true if successful, false if state is unchanged
     */
    public fun compareAndSet(expected: Long, new: Long): Boolean = this::value.compareAndSetField(expected, new)

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
}

/**
 * Wrapper around [kotlinx.cinterop.NativePtr] with atomic synchronized operations.
 *
 * Legacy MM: Atomic values and freezing: this type is unique with regard to freezing.
 * Namely, it provides mutating operations, while can participate in frozen subgraphs.
 * So shared frozen objects can have mutable fields of [AtomicNativePtr] type.
 */
@Frozen
@OptIn(FreezingIsDeprecated::class, ExperimentalStdlibApi::class)
public class AtomicNativePtr(public @Volatile var value: NativePtr) {

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     *
     * @param expected the expected value
     * @param new the new value
     * @return the old value
     */
    public fun compareAndSwap(expected: NativePtr, new: NativePtr): NativePtr =
            this::value.compareAndSwapField(expected, new)

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     *
     * @param expected the expected value
     * @param new the new value
     * @return true if successful
     */
    public fun compareAndSet(expected: NativePtr, new: NativePtr): Boolean =
            this::value.compareAndSetField(expected, new)

    /**
     * Returns the string representation of this object.
     *
     * @return string representation of this object
     */
    public override fun toString(): String = value.toString()
}


private fun idString(value: Any) = "${value.hashCode().toUInt().toString(16)}"

private fun debugString(value: Any?): String {
    if (value == null) return "null"
    return "${value::class.qualifiedName}: ${idString(value)}"
}

/**
 * Wrapper around Kotlin object with atomic operations.
 *
 * Legacy MM: An atomic reference to a frozen Kotlin object. Can be used in concurrent scenarious
 * but frequently shall be of nullable type and be zeroed out once no longer needed.
 * Otherwise memory leak could happen. To detect such leaks [kotlin.native.runtime.GC.detectCycles]
 * in debug mode could be helpful.
 */
@FrozenLegacyMM
@LeakDetectorCandidate
@NoReorderFields
@OptIn(FreezingIsDeprecated::class)
public class AtomicReference<T> {
    private var value_: T

    // A spinlock to fix potential ARC race.
    private var lock: Int = 0

    // Optimization for speeding up access.
    private var cookie: Int = 0

    /**
     * Creates a new atomic reference pointing to given [ref].
     * @throws InvalidMutabilityException with legacy MM if reference is not frozen.
     */
    constructor(value: T) {
        if (this.isFrozen) {
            checkIfFrozen(value)
        }
        value_ = value
    }

    /**
     * The referenced value.
     * Gets the value or sets the [new] value.
     * Legacy MM: if [new] value is not null, it must be frozen or permanent object.
     *
     * @throws InvalidMutabilityException with legacy MM if the value is not frozen or a permanent object
     */
    public var value: T
        get() = @Suppress("UNCHECKED_CAST")(getImpl() as T)
        set(new) = setImpl(new)

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     * Note that comparison is identity-based, not value-based.
     *
     * Legacy MM: if [new] value is not null, it must be frozen or permanent object.
     *
     * @param expected the expected value
     * @param new the new value
     * @throws InvalidMutabilityException with legacy MM if the value is not frozen or a permanent object
     * @return the old value
     */
    @GCUnsafeCall("Kotlin_AtomicReference_compareAndSwap")
    external public fun compareAndSwap(expected: T, new: T): T

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     * Note that comparison is identity-based, not value-based.
     *
     * @param expected the expected value
     * @param new the new value
     * @return true if successful
     */
    @GCUnsafeCall("Kotlin_AtomicReference_compareAndSet")
    external public fun compareAndSet(expected: T, new: T): Boolean

    /**
     * Returns the string representation of this object.
     *
     * @return string representation of this object
     */
    public override fun toString(): String =
            "${debugString(this)} -> ${debugString(value)}"

    // TODO: Consider making this public.
    internal fun swap(new: T): T {
        while (true) {
            val old = value
            if (old === new) {
                return old
            }
            if (compareAndSet(old, new)) {
                return old
            }
        }
    }

    // Implementation details.
    @GCUnsafeCall("Kotlin_AtomicReference_set")
    private external fun setImpl(new: Any?): Unit

    @GCUnsafeCall("Kotlin_AtomicReference_get")
    private external fun getImpl(): Any?
}

/**
 * Note: this class is useful only with legacy memory manager. Please use [AtomicReference] instead.
 *
 * An atomic reference to a Kotlin object. Can be used in concurrent scenarious, but must be frozen first,
 * otherwise behaves as regular box for the value. If frozen, shall be zeroed out once no longer needed.
 * Otherwise memory leak could happen. To detect such leaks [kotlin.native.runtime.GC.detectCycles]
 * in debug mode could be helpful.
 */
@NoReorderFields
@LeakDetectorCandidate
@ExportTypeInfo("theFreezableAtomicReferenceTypeInfo")
@FreezingIsDeprecated
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
            if (this.isShareable())
                setImpl(new)
            else
                value_ = new
        }

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     * Legacy MM: If [new] value is not null and object is frozen, it must be frozen or permanent object.
     *
     * @param expected the expected value
     * @param new the new value
     * @throws InvalidMutabilityException with legacy MM if the value is not frozen or a permanent object
     * @return the old value
     */
     public fun compareAndSwap(expected: T, new: T): T {
        return if (this.isShareable()) {
            @Suppress("UNCHECKED_CAST")(compareAndSwapImpl(expected, new) as T)
        } else {
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
        if (this.isShareable())
            return compareAndSetImpl(expected, new)
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

    // TODO: Consider making this public.
    internal fun swap(new: T): T {
        while (true) {
            val old = value
            if (old === new) {
                return old
            }
            if (compareAndSet(old, new)) {
                return old
            }
        }
    }

    // Implementation details.
    @GCUnsafeCall("Kotlin_AtomicReference_set")
    private external fun setImpl(new: Any?): Unit

    @GCUnsafeCall("Kotlin_AtomicReference_get")
    private external fun getImpl(): Any?

    @GCUnsafeCall("Kotlin_AtomicReference_compareAndSwap")
    private external fun compareAndSwapImpl(expected: Any?, new: Any?): Any?

    @GCUnsafeCall("Kotlin_AtomicReference_compareAndSet")
    private external fun compareAndSetImpl(expected: Any?, new: Any?): Boolean
}


/**
 * Compares the value of the field referenced by [this] to [expectedValue], and if they are equal,
 * atomically replaces it with [newValue].
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * Comparison is done by reference or value depending on field representation.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 *
 * Returns true if the actual field value matched [expectedValue]
 *
 * Legacy MM: if [this] is a reference for a non-value represented field, [IllegalArgumentException] would be thrown.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.COMPARE_AND_SET_FIELD)
internal external fun <T> KMutableProperty0<T>.compareAndSetField(expectedValue: T, newValue: T): Boolean

/**
 * Compares the value of the field referenced by [this] to [expectedValue], and if they are equal,
 * atomically replaces it with [newValue].
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * Comparison is done by reference or value depending on field representation.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 *
 * Returns true if the actual field value before operation.
 *
 * Legacy MM: if [this] is a reference for a non-value represented field, [IllegalArgumentException] would be thrown.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.COMPARE_AND_SWAP_FIELD)
internal external fun <T> KMutableProperty0<T>.compareAndSwapField(expectedValue: T, newValue: T): T

/**
 * Atomically sets value of the field referenced by [this] to [newValue] and returns old field value.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 *
 * Legacy MM: if [this] is a reference for a non-value represented field, [IllegalArgumentException] would be thrown.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.GET_AND_SET_FIELD)
internal external fun <T> KMutableProperty0<T>.getAndSetField(newValue: T): T


/**
 * Atomically increments value of the field referenced by [this] by [delta] and returns old field value.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 *
 * Legacy MM: if [this] is a reference for a non-value represented field, [IllegalArgumentException] would be thrown.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_FIELD)
internal external fun KMutableProperty0<Short>.getAndAddField(delta: Short): Short

/**
 * Atomically increments value of the field referenced by [this] by [delta] and returns old field value.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 *
 * Legacy MM: if [this] is a reference for a non-value represented field, [IllegalArgumentException] would be thrown.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_FIELD)
internal external fun KMutableProperty0<Int>.getAndAddField(newValue: Int): Int

/**
 * Atomically increments value of the field referenced by [this] by [delta] and returns old field value.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 *
 * Legacy MM: if [this] is a reference for a non-value represented field, [IllegalArgumentException] would be thrown.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_FIELD)
internal external fun KMutableProperty0<Long>.getAndAddField(newValue: Long): Long

/**
 * Atomically increments value of the field referenced by [this] by [delta] and returns old field value.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 *
 * Legacy MM: if [this] is a reference for a non-value represented field, [IllegalArgumentException] would be thrown.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_FIELD)
internal external fun KMutableProperty0<Byte>.getAndAddField(newValue: Byte): Byte
