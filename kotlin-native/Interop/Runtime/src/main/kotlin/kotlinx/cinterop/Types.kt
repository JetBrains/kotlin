/*
 * Copyright 2010-2019 JetBrains s.r.o.
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

package kotlinx.cinterop

/**
 * The entity which has an associated native pointer.
 * Subtypes are supposed to represent interpretations of the pointed data or code.
 *
 * This interface is likely to be handled by compiler magic and shouldn't be subtyped by arbitrary classes.
 *
 * TODO: the behavior of [equals], [hashCode] and [toString] differs on Native and JVM backends.
 */
public open class NativePointed internal constructor(rawPtr: NonNullNativePtr) {
    var rawPtr = rawPtr.toNativePtr()
        internal set
}

// `null` value of `NativePointed?` is mapped to `nativeNullPtr`.
public val NativePointed?.rawPtr: NativePtr
    get() = if (this != null) this.rawPtr else nativeNullPtr

/**
 * Returns interpretation of entity with given pointer.
 *
 * @param T must not be abstract
 */
public inline fun <reified T : NativePointed> interpretPointed(ptr: NativePtr): T = interpretNullablePointed<T>(ptr)!!

private class OpaqueNativePointed(rawPtr: NativePtr) : NativePointed(rawPtr.toNonNull())

public fun interpretOpaquePointed(ptr: NativePtr): NativePointed = interpretPointed<OpaqueNativePointed>(ptr)
public fun interpretNullableOpaquePointed(ptr: NativePtr): NativePointed? = interpretNullablePointed<OpaqueNativePointed>(ptr)

/**
 * Changes the interpretation of the pointed data or code.
 */
public inline fun <reified T : NativePointed> NativePointed.reinterpret(): T = interpretPointed(this.rawPtr)

/**
 * C data or code.
 */
public abstract class CPointed(rawPtr: NativePtr) : NativePointed(rawPtr.toNonNull())

/**
 * Represents a reference to (possibly empty) sequence of C values.
 * It can be either a stable pointer [CPointer] or a sequence of immutable values [CValues].
 *
 * [CValuesRef] is designed to be used as Kotlin representation of pointer-typed parameters of C functions.
 * When passing [CPointer] as [CValuesRef] to the Kotlin binding method, the C function receives exactly this pointer.
 * Passing [CValues] has nearly the same semantics as passing by value: the C function receives
 * the pointer to the temporary copy of these values, and the caller can't observe the modifications to this copy.
 * The copy is valid until the C function returns.
 * There are also other implementations of [CValuesRef] that provide temporary pointer,
 * e.g. Kotlin Native specific [refTo] functions to pass primitive arrays directly to native.
 */
public abstract class CValuesRef<T : CPointed> {
    /**
     * If this reference is [CPointer], returns this pointer, otherwise
     * allocate storage value in the scope and return it.
     */
    public abstract fun getPointer(scope: AutofreeScope): CPointer<T>
}

/**
 * The (possibly empty) sequence of immutable C values.
 * It is self-contained and doesn't depend on native memory.
 */
public abstract class CValues<T : CVariable> : CValuesRef<T>() {
    /**
     * Copies the values to [placement] and returns the pointer to the copy.
     */
    public override fun getPointer(scope: AutofreeScope): CPointer<T> {
        return place(interpretCPointer(scope.alloc(size, align).rawPtr)!!)
    }

    // TODO: optimize
    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CValues<*>) return false

        val thisBytes = this.getBytes()
        val otherBytes = other.getBytes()

        if (thisBytes.size != otherBytes.size) {
            return false
        }

        for (index in 0 .. thisBytes.size - 1) {
            if (thisBytes[index] != otherBytes[index]) {
                return false
            }
        }

        return true
    }

    public override fun hashCode(): Int {
        var result = 0
        for (byte in this.getBytes()) {
            result = result * 31 + byte
        }
        return result
    }

    public abstract val size: Int

    public abstract val align: Int

    /**
     * Copy the referenced values to [placement] and return placement pointer.
     */
    public abstract fun place(placement: CPointer<T>): CPointer<T>
}

public fun <T : CVariable> CValues<T>.placeTo(scope: AutofreeScope) = this.getPointer(scope)

/**
 * The single immutable C value.
 * It is self-contained and doesn't depend on native memory.
 *
 * TODO: consider providing an adapter instead of subtyping [CValues].
 */
public abstract class CValue<T : CVariable> : CValues<T>()

/**
 * C pointer.
 */
public class CPointer<T : CPointed> internal constructor(@PublishedApi internal val value: NonNullNativePtr) : CValuesRef<T>() {

    // TODO: replace by [value].
    @Suppress("NOTHING_TO_INLINE")
    public inline val rawValue: NativePtr get() = value.toNativePtr()

    public override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true // fast path
        }

        return (other is CPointer<*>) && (rawValue == other.rawValue)
    }

    public override fun hashCode(): Int {
        return rawValue.hashCode()
    }

    public override fun toString() = this.cPointerToString()

    public override fun getPointer(scope: AutofreeScope) = this
}

/**
 * Returns the pointer to this data or code.
 */
public val <T : CPointed> T.ptr: CPointer<T>
    get() = interpretCPointer(this.rawPtr)!!

/**
 * Returns the corresponding [CPointed].
 *
 * @param T must not be abstract
 */
public inline val <reified T : CPointed> CPointer<T>.pointed: T
    get() = interpretPointed<T>(this.rawValue)

// `null` value of `CPointer?` is mapped to `nativeNullPtr`
public val CPointer<*>?.rawValue: NativePtr
    get() = if (this != null) this.rawValue else nativeNullPtr

public fun <T : CPointed> CPointer<*>.reinterpret(): CPointer<T> = interpretCPointer(this.rawValue)!!

public fun <T : CPointed> CPointer<T>?.toLong() = this.rawValue.toLong()

public fun <T : CPointed> Long.toCPointer(): CPointer<T>? = interpretCPointer(nativeNullPtr + this)

/**
 * The [CPointed] without any specified interpretation.
 */
public abstract class COpaque(rawPtr: NativePtr) : CPointed(rawPtr) // TODO: should it correspond to COpaquePointer?

/**
 * The pointer with an opaque type.
 */
public typealias COpaquePointer = CPointer<out CPointed> // FIXME

/**
 * The variable containing a [COpaquePointer].
 */
public typealias COpaquePointerVar = CPointerVarOf<COpaquePointer>

/**
 * The C data variable located in memory.
 *
 * The non-abstract subclasses should represent the (complete) C data type and thus specify size and alignment.
 * Each such subclass must have a companion object which is a [Type].
 */
public abstract class CVariable(rawPtr: NativePtr) : CPointed(rawPtr) {

    /**
     * The (complete) C data type.
     *
     * @param size the size in bytes of data of this type
     * @param align the alignments in bytes that is enough for this data type.
     * It may be greater than actually required for simplicity.
     */
    @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
    public open class Type(val size: Long, val align: Int) {

        init {
            require(size % align == 0L)
        }

    }
}

@Suppress("DEPRECATION")
public inline fun <reified T : CVariable> sizeOf() = typeOf<T>().size

@Suppress("DEPRECATION")
public inline fun <reified T : CVariable> alignOf() = typeOf<T>().align

/**
 * Returns the member of this [CStructVar] which is located by given offset in bytes.
 */
public inline fun <reified T : CPointed> CStructVar.memberAt(offset: Long): T {
    return interpretPointed<T>(this.rawPtr + offset)
}

public inline fun <reified T : CVariable> CStructVar.arrayMemberAt(offset: Long): CArrayPointer<T> {
    return interpretCPointer<T>(this.rawPtr + offset)!!
}

/**
 * The C struct-typed variable located in memory.
 */
public abstract class CStructVar(rawPtr: NativePtr) : CVariable(rawPtr) {
    @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
    @Suppress("DEPRECATION")
    open class Type(size: Long, align: Int) : CVariable.Type(size, align)
}

/**
 * The C primitive-typed variable located in memory.
 */
sealed class CPrimitiveVar(rawPtr: NativePtr) : CVariable(rawPtr) {
    // aligning by size is obviously enough
    @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
    @Suppress("DEPRECATION")
    open class Type(size: Int) : CVariable.Type(size.toLong(), align = size)
}

@Deprecated("Will be removed.")
public interface CEnum {
    public val value: Any
}

public abstract class CEnumVar(rawPtr: NativePtr) : CPrimitiveVar(rawPtr)

// generics below are used for typedef support
// these classes are not supposed to be used directly, instead the typealiases are provided.

@Suppress("FINAL_UPPER_BOUND")
public class BooleanVarOf<T : Boolean>(rawPtr: NativePtr) : CPrimitiveVar(rawPtr) {
    @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
    @Suppress("DEPRECATION")
    companion object : Type(1)
}

@Suppress("FINAL_UPPER_BOUND")
public class ByteVarOf<T : Byte>(rawPtr: NativePtr) : CPrimitiveVar(rawPtr) {
    @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
    @Suppress("DEPRECATION")
    companion object : Type(1)
}

@Suppress("FINAL_UPPER_BOUND")
public class ShortVarOf<T : Short>(rawPtr: NativePtr) : CPrimitiveVar(rawPtr) {
    @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
    @Suppress("DEPRECATION")
    companion object : Type(2)
}

@Suppress("FINAL_UPPER_BOUND")
public class IntVarOf<T : Int>(rawPtr: NativePtr) : CPrimitiveVar(rawPtr) {
    @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
    @Suppress("DEPRECATION")
    companion object : Type(4)
}

@Suppress("FINAL_UPPER_BOUND")
public class LongVarOf<T : Long>(rawPtr: NativePtr) : CPrimitiveVar(rawPtr) {
    @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
    @Suppress("DEPRECATION")
    companion object : Type(8)
}

@Suppress("FINAL_UPPER_BOUND")
public class UByteVarOf<T : UByte>(rawPtr: NativePtr) : CPrimitiveVar(rawPtr) {
    @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
    @Suppress("DEPRECATION")
    companion object : Type(1)
}

@Suppress("FINAL_UPPER_BOUND")
public class UShortVarOf<T : UShort>(rawPtr: NativePtr) : CPrimitiveVar(rawPtr) {
    @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
    @Suppress("DEPRECATION")
    companion object : Type(2)
}

@Suppress("FINAL_UPPER_BOUND")
public class UIntVarOf<T : UInt>(rawPtr: NativePtr) : CPrimitiveVar(rawPtr) {
    @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
    @Suppress("DEPRECATION")
    companion object : Type(4)
}

@Suppress("FINAL_UPPER_BOUND")
public class ULongVarOf<T : ULong>(rawPtr: NativePtr) : CPrimitiveVar(rawPtr) {
    @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
    @Suppress("DEPRECATION")
    companion object : Type(8)
}

@Suppress("FINAL_UPPER_BOUND")
public class FloatVarOf<T : Float>(rawPtr: NativePtr) : CPrimitiveVar(rawPtr) {
    @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
    @Suppress("DEPRECATION")
    companion object : Type(4)
}

@Suppress("FINAL_UPPER_BOUND")
public class DoubleVarOf<T : Double>(rawPtr: NativePtr) : CPrimitiveVar(rawPtr) {
    @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
    @Suppress("DEPRECATION")
    companion object : Type(8)
}

public typealias BooleanVar = BooleanVarOf<Boolean>
public typealias ByteVar = ByteVarOf<Byte>
public typealias ShortVar = ShortVarOf<Short>
public typealias IntVar = IntVarOf<Int>
public typealias LongVar = LongVarOf<Long>
public typealias UByteVar = UByteVarOf<UByte>
public typealias UShortVar = UShortVarOf<UShort>
public typealias UIntVar = UIntVarOf<UInt>
public typealias ULongVar = ULongVarOf<ULong>
public typealias FloatVar = FloatVarOf<Float>
public typealias DoubleVar = DoubleVarOf<Double>

@Suppress("FINAL_UPPER_BOUND", "UNCHECKED_CAST")
public var <T : Boolean> BooleanVarOf<T>.value: T
    get() {
        val byte = nativeMemUtils.getByte(this)
        return byte.toBoolean() as T
    }
    set(value) = nativeMemUtils.putByte(this, value.toByte())

@Suppress("NOTHING_TO_INLINE")
public inline fun Boolean.toByte(): Byte = if (this) 1 else 0

@Suppress("NOTHING_TO_INLINE")
public inline fun Byte.toBoolean() = (this.toInt() != 0)

@Suppress("FINAL_UPPER_BOUND", "UNCHECKED_CAST")
public var <T : Byte> ByteVarOf<T>.value: T
    get() = nativeMemUtils.getByte(this) as T
    set(value) = nativeMemUtils.putByte(this, value)

@Suppress("FINAL_UPPER_BOUND", "UNCHECKED_CAST")
public var <T : Short> ShortVarOf<T>.value: T
    get() = nativeMemUtils.getShort(this) as T
    set(value) = nativeMemUtils.putShort(this, value)

@Suppress("FINAL_UPPER_BOUND", "UNCHECKED_CAST")
public var <T : Int> IntVarOf<T>.value: T
    get() = nativeMemUtils.getInt(this) as T
    set(value) = nativeMemUtils.putInt(this, value)

@Suppress("FINAL_UPPER_BOUND", "UNCHECKED_CAST")
public var <T : Long> LongVarOf<T>.value: T
    get() = nativeMemUtils.getLong(this) as T
    set(value) = nativeMemUtils.putLong(this, value)

@Suppress("FINAL_UPPER_BOUND", "UNCHECKED_CAST")
public var <T : UByte> UByteVarOf<T>.value: T
    get() = nativeMemUtils.getByte(this).toUByte() as T
    set(value) = nativeMemUtils.putByte(this, value.toByte())

@Suppress("FINAL_UPPER_BOUND", "UNCHECKED_CAST")
public var <T : UShort> UShortVarOf<T>.value: T
    get() = nativeMemUtils.getShort(this).toUShort() as T
    set(value) = nativeMemUtils.putShort(this, value.toShort())

@Suppress("FINAL_UPPER_BOUND", "UNCHECKED_CAST")
public var <T : UInt> UIntVarOf<T>.value: T
    get() = nativeMemUtils.getInt(this).toUInt() as T
    set(value) = nativeMemUtils.putInt(this, value.toInt())

@Suppress("FINAL_UPPER_BOUND", "UNCHECKED_CAST")
public var <T : ULong> ULongVarOf<T>.value: T
    get() = nativeMemUtils.getLong(this).toULong() as T
    set(value) = nativeMemUtils.putLong(this, value.toLong())

// TODO: ensure native floats have the appropriate binary representation

@Suppress("FINAL_UPPER_BOUND", "UNCHECKED_CAST")
public var <T : Float> FloatVarOf<T>.value: T
    get() = nativeMemUtils.getFloat(this) as T
    set(value) = nativeMemUtils.putFloat(this, value)

@Suppress("FINAL_UPPER_BOUND", "UNCHECKED_CAST")
public var <T : Double> DoubleVarOf<T>.value: T
    get() = nativeMemUtils.getDouble(this) as T
    set(value) = nativeMemUtils.putDouble(this, value)


public class CPointerVarOf<T : CPointer<*>>(rawPtr: NativePtr) : CVariable(rawPtr) {
    @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
    @Suppress("DEPRECATION")
    companion object : CVariable.Type(pointerSize.toLong(), pointerSize)
}

/**
 * The C data variable containing the pointer to `T`.
 */
public typealias CPointerVar<T> = CPointerVarOf<CPointer<T>>

/**
 * The value of this variable.
 */
@Suppress("UNCHECKED_CAST")
public inline var <P : CPointer<*>> CPointerVarOf<P>.value: P?
    get() = interpretCPointer<CPointed>(nativeMemUtils.getNativePtr(this)) as P?
    set(value) = nativeMemUtils.putNativePtr(this, value.rawValue)

/**
 * The code or data pointed by the value of this variable.
 * 
 * @param T must not be abstract
 */
public inline var <reified T : CPointed, reified P : CPointer<T>> CPointerVarOf<P>.pointed: T?
    get() = this.value?.pointed
    set(value) {
        this.value = value?.ptr as P?
    }

public inline operator fun <reified T : CVariable> CPointer<T>.get(index: Long): T {
    val offset = if (index == 0L) {
        0L // optimization for JVM impl which uses reflection for now.
    } else {
        index * sizeOf<T>()
    }
    return interpretPointed(this.rawValue + offset)
}

public inline operator fun <reified T : CVariable> CPointer<T>.get(index: Int): T = this.get(index.toLong())

@Suppress("NOTHING_TO_INLINE")
@JvmName("plus\$CPointer")
public inline operator fun <T : CPointerVarOf<*>> CPointer<T>?.plus(index: Long): CPointer<T>? =
        interpretCPointer(this.rawValue + index * pointerSize)

@Suppress("NOTHING_TO_INLINE")
@JvmName("plus\$CPointer")
public inline operator fun <T : CPointerVarOf<*>> CPointer<T>?.plus(index: Int): CPointer<T>? =
        this + index.toLong()

@Suppress("NOTHING_TO_INLINE")
public inline operator fun <T : CPointer<*>> CPointer<CPointerVarOf<T>>.get(index: Int): T? =
        (this + index)!!.pointed.value

@Suppress("NOTHING_TO_INLINE")
public inline operator fun <T : CPointer<*>> CPointer<CPointerVarOf<T>>.set(index: Int, value: T?) {
    (this + index)!!.pointed.value = value
}

@Suppress("NOTHING_TO_INLINE")
public inline operator fun <T : CPointer<*>> CPointer<CPointerVarOf<T>>.get(index: Long): T? =
        (this + index)!!.pointed.value

@Suppress("NOTHING_TO_INLINE")
public inline operator fun <T : CPointer<*>> CPointer<CPointerVarOf<T>>.set(index: Long, value: T?) {
    (this + index)!!.pointed.value = value
}

public typealias CArrayPointer<T> = CPointer<T>
public typealias CArrayPointerVar<T> = CPointerVar<T>

/**
 * The C function.
 */
public class CFunction<T : Function<*>>(rawPtr: NativePtr) : CPointed(rawPtr)
