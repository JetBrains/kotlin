/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.cinterop

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@ExperimentalForeignApi
public interface NativePlacement {

    public fun alloc(size: Long, align: Int): NativePointed

    public fun alloc(size: Int, align: Int): NativePointed = alloc(size.toLong(), align)
}

@ExperimentalForeignApi
public interface NativeFreeablePlacement : NativePlacement {
    public fun free(mem: NativePtr)
}

@ExperimentalForeignApi
public fun NativeFreeablePlacement.free(pointer: CPointer<*>): Unit = this.free(pointer.rawValue)
@ExperimentalForeignApi
public fun NativeFreeablePlacement.free(pointed: NativePointed): Unit = this.free(pointed.rawPtr)

@ExperimentalForeignApi
public object nativeHeap : NativeFreeablePlacement {
    override fun alloc(size: Long, align: Int): NativePointed = nativeMemUtils.alloc(size, align)

    override fun free(mem: NativePtr): Unit = nativeMemUtils.free(mem)
}

@ExperimentalForeignApi
private typealias Deferred = () -> Unit

@ExperimentalForeignApi
public open class DeferScope {

    @PublishedApi
    internal var topDeferred: Deferred? = null

    internal fun executeAllDeferred() {
        topDeferred?.let {
            it.invoke()
            topDeferred = null
        }
    }

    public inline fun defer(crossinline block: () -> Unit) {
        val currentTop = topDeferred
        topDeferred = {
            try {
                block()
            } finally {
                // TODO: it is possible to implement chaining without recursion,
                // but it would require using an anonymous object here
                // which is not yet supported in Kotlin Native inliner.
                currentTop?.invoke()
            }
        }
    }
}

@ExperimentalForeignApi
public abstract class AutofreeScope : DeferScope(), NativePlacement {
    abstract override fun alloc(size: Long, align: Int): NativePointed
}

@ExperimentalForeignApi
public open class ArenaBase(private val parent: NativeFreeablePlacement = nativeHeap) : AutofreeScope() {

    private var lastChunk: NativePointed? = null

    final override fun alloc(size: Long, align: Int): NativePointed {
        // Reserve space for a pointer:
        val gapForPointer = maxOf(pointerSize, align)

        val chunk = parent.alloc(size = gapForPointer + size, align = gapForPointer)
        nativeMemUtils.putNativePtr(chunk, lastChunk.rawPtr)
        lastChunk = chunk
        return interpretOpaquePointed(chunk.rawPtr + gapForPointer.toLong())
    }

    @PublishedApi
    internal fun clearImpl() {
        this.executeAllDeferred()

        var chunk = lastChunk
        while (chunk != null) {
            val nextChunk = nativeMemUtils.getNativePtr(chunk)
            parent.free(chunk)
            chunk = interpretNullableOpaquePointed(nextChunk)
        }
    }

}

@ExperimentalForeignApi
public class Arena(parent: NativeFreeablePlacement = nativeHeap) : ArenaBase(parent) {
    public fun clear(): Unit = this.clearImpl()
}

/**
 * Allocates variable of given type.
 *
 * @param T must not be abstract
 */
@ExperimentalForeignApi
public inline fun <reified T : CVariable> NativePlacement.alloc(): T =
        @Suppress("DEPRECATION")
        alloc(typeOf<T>()).reinterpret()

@PublishedApi
@Suppress("DEPRECATION")
@ExperimentalForeignApi
internal fun NativePlacement.alloc(type: CVariable.Type): NativePointed =
        alloc(type.size, type.align)

/**
 * Allocates variable of given type and initializes it applying given block.
 *
 * @param T must not be abstract
 */
@ExperimentalForeignApi
public inline fun <reified T : CVariable> NativePlacement.alloc(initialize: T.() -> Unit): T =
        alloc<T>().also { it.initialize() }

/**
 * Allocates C array of given elements type and length.
 *
 * @param T must not be abstract
 */
@ExperimentalForeignApi
public inline fun <reified T : CVariable> NativePlacement.allocArray(length: Long): CArrayPointer<T> =
        alloc(sizeOf<T>() * length, alignOf<T>()).reinterpret<T>().ptr

/**
 * Allocates C array of given elements type and length.
 *
 * @param T must not be abstract
 */
@ExperimentalForeignApi
public inline fun <reified T : CVariable> NativePlacement.allocArray(length: Int): CArrayPointer<T> =
        allocArray(length.toLong())

/**
 * Allocates C array of given elements type and length, and initializes its elements applying given block.
 *
 * @param T must not be abstract
 */
@ExperimentalForeignApi
public inline fun <reified T : CVariable> NativePlacement.allocArray(length: Long,
                                                              initializer: T.(index: Long)->Unit): CArrayPointer<T> {
    val res = allocArray<T>(length)

    (0 .. length - 1).forEach { index ->
        res[index].initializer(index)
    }

    return res
}

/**
 * Allocates C array of given elements type and length, and initializes its elements applying given block.
 *
 * @param T must not be abstract
 */
@ExperimentalForeignApi
public inline fun <reified T : CVariable> NativePlacement.allocArray(
        length: Int, initializer: T.(index: Int)->Unit): CArrayPointer<T> = allocArray(length.toLong()) { index ->
            this.initializer(index.toInt())
        }


/**
 * Allocates C array of pointers to given elements.
 */
@ExperimentalForeignApi
public fun <T : CPointed> NativePlacement.allocArrayOfPointersTo(elements: List<T?>): CArrayPointer<CPointerVar<T>> {
    val res = allocArray<CPointerVar<T>>(elements.size)
    elements.forEachIndexed { index, value ->
        res[index] = value?.ptr
    }
    return res
}

/**
 * Allocates C array of pointers to given elements.
 */
@ExperimentalForeignApi
public fun <T : CPointed> NativePlacement.allocArrayOfPointersTo(vararg elements: T?): CArrayPointer<CPointerVar<T>> =
        allocArrayOfPointersTo(listOf(*elements))

/**
 * Allocates C array of given values.
 */
@ExperimentalForeignApi
public inline fun <reified T : CPointer<*>>
        NativePlacement.allocArrayOf(vararg elements: T?): CArrayPointer<CPointerVarOf<T>> {
    return allocArrayOf(listOf(*elements))
}

/**
 * Allocates C array of given values.
 */
@ExperimentalForeignApi
public inline fun <reified T : CPointer<*>>
        NativePlacement.allocArrayOf(elements: List<T?>): CArrayPointer<CPointerVarOf<T>> {

    val res = allocArray<CPointerVarOf<T>>(elements.size)
    var index = 0
    while (index < elements.size) {
        res[index] = elements[index]
        ++index
    }
    return res
}

@ExperimentalForeignApi
public fun NativePlacement.allocArrayOf(elements: ByteArray): CArrayPointer<ByteVar> {
    val result = allocArray<ByteVar>(elements.size)
    nativeMemUtils.putByteArray(elements, result.pointed, elements.size)
    return result
}

@ExperimentalForeignApi
public fun NativePlacement.allocArrayOf(vararg elements: Float): CArrayPointer<FloatVar> {
    val res = allocArray<FloatVar>(elements.size)
    var index = 0
    while (index < elements.size) {
        res[index] = elements[index]
        ++index
    }
    return res
}

@ExperimentalForeignApi
public fun <T : CPointed> NativePlacement.allocPointerTo(): CPointerVar<T> = alloc<CPointerVar<T>>()

@PublishedApi
@ExperimentalForeignApi
internal class ZeroValue<T : CVariable>(private val sizeBytes: Int, private val alignBytes: Int) : CValue<T>() {
    // Optimization to avoid unneeded virtual calls in base class implementation.
    override fun getPointer(scope: AutofreeScope): CPointer<T> {
        return place(interpretCPointer(scope.alloc(size, align).rawPtr)!!)
    }

    override fun place(placement: CPointer<T>): CPointer<T> {
        nativeMemUtils.zeroMemory(interpretPointed(placement.rawValue), sizeBytes)
        return placement
    }
    override val size get() = sizeBytes

    override val align get() = alignBytes

}
@Suppress("NOTHING_TO_INLINE")
@ExperimentalForeignApi
public inline fun <T : CVariable> zeroValue(size: Int, align: Int): CValue<T> = ZeroValue(size, align)

@ExperimentalForeignApi
public inline fun <reified T : CVariable> zeroValue(): CValue<T> = zeroValue<T>(sizeOf<T>().toInt(), alignOf<T>())

@ExperimentalForeignApi
public inline fun <reified T : CVariable> cValue(): CValue<T> = zeroValue<T>()

@ExperimentalForeignApi
public fun <T : CVariable> CPointed.readValues(size: Int, align: Int): CValues<T> {
    val bytes = ByteArray(size)
    nativeMemUtils.getByteArray(this, bytes, size)

    return object : CValue<T>() {
        // Optimization to avoid unneeded virtual calls in base class implementation.
        override fun getPointer(scope: AutofreeScope): CPointer<T> {
            return place(interpretCPointer(scope.alloc(size, align).rawPtr)!!)
        }
        override fun place(placement: CPointer<T>): CPointer<T> {
            nativeMemUtils.putByteArray(bytes, interpretPointed(placement.rawValue), bytes.size)
            return placement
        }
        override val size get() = size
        override val align get() = align
    }
}

@ExperimentalForeignApi
public inline fun <reified T : CVariable> T.readValues(count: Int): CValues<T> =
        this.readValues<T>(size = count * sizeOf<T>().toInt(), align = alignOf<T>())

@ExperimentalForeignApi
public fun <T : CVariable> CPointed.readValue(size: Long, align: Int): CValue<T> {
    val bytes = ByteArray(size.toInt())
    nativeMemUtils.getByteArray(this, bytes, size.toInt())

    return object : CValue<T>() {
        override fun place(placement: CPointer<T>): CPointer<T> {
            nativeMemUtils.putByteArray(bytes, interpretPointed(placement.rawValue), bytes.size)
            return placement
        }
        // Optimization to avoid unneeded virtual calls in base class implementation.
        public override fun getPointer(scope: AutofreeScope): CPointer<T> {
            return place(interpretCPointer(scope.alloc(size, align).rawPtr)!!)
        }
        override val size get() = size.toInt()
        override val align get() = align
    }
}

@Suppress("DEPRECATION")
@PublishedApi
@ExperimentalForeignApi
internal fun <T : CVariable> CPointed.readValue(type: CVariable.Type): CValue<T> =
        readValue(type.size, type.align)

// Note: can't be declared as property due to possible clash with a struct field.
// TODO: find better name.
@Suppress("DEPRECATION")
@ExperimentalForeignApi
public inline fun <reified T : CStructVar> T.readValue(): CValue<T> = this.readValue(typeOf<T>())

@ExperimentalForeignApi
public fun <T : CVariable> CValue<T>.write(location: NativePtr) {
    this.place(interpretCPointer(location)!!)
}

// TODO: optimize
@ExperimentalForeignApi
public fun <T : CVariable> CValues<T>.getBytes(): ByteArray = memScoped {
    val result = ByteArray(size)
    nativeMemUtils.getByteArray(
            source = this@getBytes.placeTo(memScope).reinterpret<ByteVar>().pointed,
            dest = result,
            length = result.size
    )
    result
}

/**
 * Calls the [block] with temporary copy of this value as receiver.
 */
@ExperimentalForeignApi
@OptIn(kotlin.contracts.ExperimentalContracts::class)
public inline fun <reified T : CStructVar, R> CValue<T>.useContents(block: T.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return memScoped {
        this@useContents.placeTo(memScope).pointed.block()
    }
}

@ExperimentalForeignApi
public inline fun <reified T : CStructVar> CValue<T>.copy(modify: T.() -> Unit): CValue<T> = useContents {
    this.modify()
    this.readValue()
}

@ExperimentalForeignApi
public inline fun <reified T : CStructVar> cValue(initialize: T.() -> Unit): CValue<T> =
    zeroValue<T>().copy(modify = initialize)

@ExperimentalForeignApi
public inline fun <reified T : CVariable> createValues(count: Int, initializer: T.(index: Int) -> Unit): CValues<T> = memScoped {
    val array = allocArray<T>(count, initializer)
    array[0].readValues(count)
}

// TODO: optimize other [cValuesOf] methods:
/**
 * Returns sequence of immutable values [CValues] to pass them to C code.
 */
@ExperimentalForeignApi
public fun cValuesOf(vararg elements: Byte): CValues<ByteVar> = object : CValues<ByteVar>() {
    // Optimization to avoid unneeded virtual calls in base class implementation.
    override fun getPointer(scope: AutofreeScope): CPointer<ByteVar> {
        return place(interpretCPointer(scope.alloc(size, align).rawPtr)!!)
    }
    override fun place(placement: CPointer<ByteVar>): CPointer<ByteVar> {
        nativeMemUtils.putByteArray(elements, interpretPointed(placement.rawValue), elements.size)
        return placement
    }

    override val size get() = 1 * elements.size
    override val align get() = 1
}

@ExperimentalForeignApi
public fun cValuesOf(vararg elements: Short): CValues<ShortVar> =
        createValues(elements.size) { index -> this.value = elements[index] }

@ExperimentalForeignApi
public fun cValuesOf(vararg elements: Int): CValues<IntVar> =
        createValues(elements.size) { index -> this.value = elements[index] }

@ExperimentalForeignApi
public fun cValuesOf(vararg elements: Long): CValues<LongVar> =
        createValues(elements.size) { index -> this.value = elements[index] }

@ExperimentalForeignApi
public fun cValuesOf(vararg elements: Float): CValues<FloatVar> =
        createValues(elements.size) { index -> this.value = elements[index] }

@ExperimentalForeignApi
public fun cValuesOf(vararg elements: Double): CValues<DoubleVar> =
        createValues(elements.size) { index -> this.value = elements[index] }

@ExperimentalForeignApi
public fun <T : CPointed> cValuesOf(vararg elements: CPointer<T>?): CValues<CPointerVar<T>> =
        createValues(elements.size) { index -> this.value = elements[index] }

@ExperimentalForeignApi
public fun ByteArray.toCValues(): CValues<ByteVar> = cValuesOf(*this)
@ExperimentalForeignApi
public fun ShortArray.toCValues(): CValues<ShortVar> = cValuesOf(*this)
@ExperimentalForeignApi
public fun IntArray.toCValues(): CValues<IntVar> = cValuesOf(*this)
@ExperimentalForeignApi
public fun LongArray.toCValues(): CValues<LongVar> = cValuesOf(*this)
@ExperimentalForeignApi
public fun FloatArray.toCValues(): CValues<FloatVar> = cValuesOf(*this)
@ExperimentalForeignApi
public fun DoubleArray.toCValues(): CValues<DoubleVar> = cValuesOf(*this)
@ExperimentalForeignApi
public fun <T : CPointed> Array<CPointer<T>?>.toCValues(): CValues<CPointerVar<T>> = cValuesOf(*this)
@ExperimentalForeignApi
public fun <T : CPointed> List<CPointer<T>?>.toCValues(): CValues<CPointerVar<T>> = this.toTypedArray().toCValues()

@ExperimentalForeignApi
private class CString(val bytes: ByteArray) : CValues<ByteVar>() {
    override val size get() = bytes.size + 1
    override val align get() = 1

    // Optimization to avoid unneeded virtual calls in base class implementation.
    override fun getPointer(scope: AutofreeScope): CPointer<ByteVar> {
        return place(interpretCPointer(scope.alloc(size, align).rawPtr)!!)
    }
    override fun place(placement: CPointer<ByteVar>): CPointer<ByteVar> {
        nativeMemUtils.putByteArray(bytes, placement.pointed, bytes.size)
        placement[bytes.size] = 0.toByte()
        return placement
    }
}

@ExperimentalForeignApi
private object EmptyCString : CValues<ByteVar>() {
    override val size get() = 1
    override val align get() = 1

    private val placement =
            interpretCPointer<ByteVar>(nativeMemUtils.allocRaw(1, 1))!!.also {
                it[0] = 0.toByte()
            }

    override fun getPointer(scope: AutofreeScope): CPointer<ByteVar> {
        return placement
    }

    override fun place(placement: CPointer<ByteVar>): CPointer<ByteVar> {
        placement[0] = 0.toByte()
        return placement
    }
}

/**
 * @return the value of zero-terminated UTF-8-encoded C string constructed from given [kotlin.String].
 */
@ExperimentalForeignApi
public val String.cstr: CValues<ByteVar>
    get() = if (isEmpty()) EmptyCString else CString(encodeToUtf8(this))

/**
 * @return the value of zero-terminated UTF-8-encoded C string constructed from given [kotlin.String].
 */
@ExperimentalForeignApi
public val String.utf8: CValues<ByteVar>
    get() = CString(encodeToUtf8(this))

/**
 * Convert this list of Kotlin strings to C array of C strings,
 * allocating memory for the array and C strings with given [AutofreeScope].
 */
@ExperimentalForeignApi
public fun List<String>.toCStringArray(autofreeScope: AutofreeScope): CPointer<CPointerVar<ByteVar>> =
        autofreeScope.allocArrayOf(this.map { it.cstr.getPointer(autofreeScope) })

/**
 * Convert this array of Kotlin strings to C array of C strings,
 * allocating memory for the array and C strings with given [AutofreeScope].
 */
@ExperimentalForeignApi
public fun Array<String>.toCStringArray(autofreeScope: AutofreeScope): CPointer<CPointerVar<ByteVar>> =
        autofreeScope.allocArrayOf(this.map { it.cstr.getPointer(autofreeScope) })


@ExperimentalForeignApi
private class U16CString(val chars: CharArray): CValues<UShortVar>() {
    override val size get() = 2 * (chars.size + 1)

    override val align get() = 2

    // Optimization to avoid unneeded virtual calls in base class implementation.
    override fun getPointer(scope: AutofreeScope): CPointer<UShortVar> {
        return place(interpretCPointer(scope.alloc(size, align).rawPtr)!!)
    }

    override fun place(placement: CPointer<UShortVar>): CPointer<UShortVar> {
        nativeMemUtils.putCharArray(chars, placement.pointed, chars.size)
        // TODO: fix, after KT-29627 is fixed.
        nativeMemUtils.putShort((placement + chars.size)!!.pointed, 0)
        return placement
    }
}

/**
 * @return the value of zero-terminated UTF-16-encoded C string constructed from given [kotlin.String].
 */
@ExperimentalForeignApi
public val String.wcstr: CValues<UShortVar>
    get() = U16CString(this.toCharArray())

/**
 * @return the value of zero-terminated UTF-16-encoded C string constructed from given [kotlin.String].
 */
@ExperimentalForeignApi
public val String.utf16: CValues<UShortVar>
    get() = U16CString(this.toCharArray())

@ExperimentalForeignApi
private class U32CString(val chars: CharArray) : CValues<IntVar>() {
    override val size get() = 4 * (chars.size + 1)

    override val align get() = 4

    // Optimization to avoid unneeded virtual calls in base class implementation.
    override fun getPointer(scope: AutofreeScope): CPointer<IntVar> {
        return place(interpretCPointer(scope.alloc(size, align).rawPtr)!!)
    }

    override fun place(placement: CPointer<IntVar>): CPointer<IntVar> {
        var indexIn = 0
        var indexOut = 0
        while (indexIn < chars.size) {
            var value = chars[indexIn++].code
            if (value >= 0xd800 && value < 0xdc00) {
                // Surrogate pair.
                if (indexIn >= chars.size - 1) throw IllegalArgumentException()
                indexIn++
                val next = chars[indexIn].code
                if (next < 0xdc00 || next >= 0xe000) throw IllegalArgumentException()
                value = 0x10000 + ((value and 0x3ff) shl 10) + (next and 0x3ff)
            }
            nativeMemUtils.putInt((placement + indexOut)!!.pointed, value)
            indexOut++
        }
        nativeMemUtils.putInt((placement + indexOut)!!.pointed, 0)
        return placement
    }
}

/**
 * @return the value of zero-terminated UTF-32-encoded C string constructed from given [kotlin.String].
 */
@ExperimentalForeignApi
public val String.utf32: CValues<IntVar>
    get() = U32CString(this.toCharArray())


// TODO: optimize
/**
 * @return the [kotlin.String] decoded from given zero-terminated UTF-8-encoded C string.
 */
@ExperimentalForeignApi
public fun CPointer<ByteVar>.toKStringFromUtf8(): String = this.toKStringFromUtf8Impl()

/**
 * @return the [kotlin.String] decoded from given zero-terminated UTF-8-encoded C string.
 */
@ExperimentalForeignApi
public fun CPointer<ByteVar>.toKString(): String = this.toKStringFromUtf8()

/**
 * @return the [kotlin.String] decoded from given zero-terminated UTF-16-encoded C string.
 */
@ExperimentalForeignApi
public fun CPointer<ShortVar>.toKStringFromUtf16(): String {
    val nativeBytes = this

    var length = 0
    while (nativeBytes[length] != 0.toShort()) {
        ++length
    }
    val chars = CharArray(length)
    var index = 0
    while (index < length) {
        chars[index] = nativeBytes[index].toInt().toChar()
        ++index
    }
    return chars.concatToString()
}

/**
 * @return the [kotlin.String] decoded from given zero-terminated UTF-32-encoded C string.
 */
@ExperimentalForeignApi
public fun CPointer<IntVar>.toKStringFromUtf32(): String {
    val nativeBytes = this

    var fromIndex = 0
    var toIndex = 0
    while (true) {
        val value = nativeBytes[fromIndex++]
        if (value == 0) break
        toIndex++
        if (value >= 0x10000 && value <= 0x10ffff) {
            toIndex++
        }
    }
    val length = toIndex
    val chars = CharArray(length)
    fromIndex = 0
    toIndex = 0
    while (toIndex < length) {
        var value = nativeBytes[fromIndex++]
        if (value >= 0x10000 && value <= 0x10ffff) {
            chars[toIndex++] = (((value - 0x10000) shr 10) or 0xd800).toChar()
            chars[toIndex++] = (((value - 0x10000) and 0x3ff) or 0xdc00).toChar()
        } else {
            chars[toIndex++] = value.toChar()
        }
    }
    return chars.concatToString()
}


/**
 * Decodes a string from the bytes in UTF-8 encoding in this array.
 * Bytes following the first occurrence of `0` byte, if it occurs, are not decoded.
 *
 * Malformed byte sequences are replaced by the replacement char `\uFFFD`.
 */
@SinceKotlin("1.3")
@ExperimentalForeignApi
public fun ByteArray.toKString() : String {
    val realEndIndex = realEndIndex(this, 0, this.size)
    return decodeToString(0, realEndIndex)
}

/**
 * Decodes a string from the bytes in UTF-8 encoding in this array or its subrange.
 * Bytes following the first occurrence of `0` byte, if it occurs, are not decoded.
 *
 * @param startIndex the beginning (inclusive) of the subrange to decode, 0 by default.
 * @param endIndex the end (exclusive) of the subrange to decode, size of this array by default.
 * @param throwOnInvalidSequence specifies whether to throw an exception on malformed byte sequence or replace it by the replacement char `\uFFFD`.
 *
 * @throws IndexOutOfBoundsException if [startIndex] is less than zero or [endIndex] is greater than the size of this array.
 * @throws IllegalArgumentException if [startIndex] is greater than [endIndex].
 * @throws CharacterCodingException if the byte array contains malformed UTF-8 byte sequence and [throwOnInvalidSequence] is true.
 */
@SinceKotlin("1.3")
@ExperimentalForeignApi
public fun ByteArray.toKString(
        startIndex: Int = 0,
        endIndex: Int = this.size,
        throwOnInvalidSequence: Boolean = false
) : String {
    checkBoundsIndexes(startIndex, endIndex, this.size)
    val realEndIndex = realEndIndex(this, startIndex, endIndex)
    return decodeToString(startIndex, realEndIndex, throwOnInvalidSequence)
}

private fun realEndIndex(byteArray: ByteArray, startIndex: Int, endIndex: Int): Int {
    var index = startIndex
    while (index < endIndex && byteArray[index] != 0.toByte()) {
        index++
    }
    return index
}

private fun checkBoundsIndexes(startIndex: Int, endIndex: Int, size: Int) {
    if (startIndex < 0 || endIndex > size) {
        throw IndexOutOfBoundsException("startIndex: $startIndex, endIndex: $endIndex, size: $size")
    }
    if (startIndex > endIndex) {
        throw IllegalArgumentException("startIndex: $startIndex > endIndex: $endIndex")
    }
}

@ExperimentalForeignApi
public class MemScope : ArenaBase() {

    public val memScope: MemScope
        get() = this

    public val <T: CVariable> CValues<T>.ptr: CPointer<T>
        get() = this@ptr.getPointer(this@MemScope)
}

// TODO: consider renaming `memScoped` because it now supports `defer`.

/**
 * Runs given [block] providing allocation of memory
 * which will be automatically disposed at the end of this scope.
 */
@ExperimentalForeignApi
@OptIn(kotlin.contracts.ExperimentalContracts::class)
public inline fun <R> memScoped(block: MemScope.()->R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val memScope = MemScope()
    try {
        return memScope.block()
    } finally {
        memScope.clearImpl()
    }
}

@ExperimentalForeignApi
public fun COpaquePointer.readBytes(count: Int): ByteArray {
    val result = ByteArray(count)
    nativeMemUtils.getByteArray(this.reinterpret<ByteVar>().pointed, result, count)
    return result
}
