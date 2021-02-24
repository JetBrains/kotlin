/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@JsName("arrayIterator")
internal fun arrayIterator(array: dynamic, type: String?) = when (type) {
    null -> {
        val arr: Array<dynamic> = array
        object : Iterator<dynamic> {
            var index = 0
            override fun hasNext() = index < arr.size
            override fun next() = if (index < arr.size) arr[index++] else throw NoSuchElementException("$index")
        }
    }
    "BooleanArray" -> booleanArrayIterator(array)
    "ByteArray" -> byteArrayIterator(array)
    "ShortArray" -> shortArrayIterator(array)
    "CharArray" -> charArrayIterator(array)
    "IntArray" -> intArrayIterator(array)
    "LongArray" -> longArrayIterator(array)
    "FloatArray" -> floatArrayIterator(array)
    "DoubleArray" -> doubleArrayIterator(array)
    else -> throw IllegalStateException("Unsupported type argument for arrayIterator: $type")
}

@JsName("booleanArrayIterator")
internal fun booleanArrayIterator(array: BooleanArray) = object : BooleanIterator() {
    var index = 0
    override fun hasNext() = index < array.size
    override fun nextBoolean() = if (index < array.size) array[index++] else throw NoSuchElementException("$index")
}

@JsName("byteArrayIterator")
internal fun byteArrayIterator(array: ByteArray) = object : ByteIterator() {
    var index = 0
    override fun hasNext() = index < array.size
    override fun nextByte() = if (index < array.size) array[index++] else throw NoSuchElementException("$index")
}

@JsName("shortArrayIterator")
internal fun shortArrayIterator(array: ShortArray) = object : ShortIterator() {
    var index = 0
    override fun hasNext() = index < array.size
    override fun nextShort() = if (index < array.size) array[index++] else throw NoSuchElementException("$index")
}

@JsName("charArrayIterator")
internal fun charArrayIterator(array: CharArray) = object : CharIterator() {
    var index = 0
    override fun hasNext() = index < array.size
    override fun nextChar() = if (index < array.size) array[index++] else throw NoSuchElementException("$index")
}

@JsName("intArrayIterator")
internal fun intArrayIterator(array: IntArray) = object : IntIterator() {
    var index = 0
    override fun hasNext() = index < array.size
    override fun nextInt() = if (index < array.size) array[index++] else throw NoSuchElementException("$index")
}

@JsName("floatArrayIterator")
internal fun floatArrayIterator(array: FloatArray) = object : FloatIterator() {
    var index = 0
    override fun hasNext() = index < array.size
    override fun nextFloat() = if (index < array.size) array[index++] else throw NoSuchElementException("$index")
}

@JsName("doubleArrayIterator")
internal fun doubleArrayIterator(array: DoubleArray) = object : DoubleIterator() {
    var index = 0
    override fun hasNext() = index < array.size
    override fun nextDouble() = if (index < array.size) array[index++] else throw NoSuchElementException("$index")
}

@JsName("longArrayIterator")
internal fun longArrayIterator(array: LongArray) = object : LongIterator() {
    var index = 0
    override fun hasNext() = index < array.size
    override fun nextLong() = if (index < array.size) array[index++] else throw NoSuchElementException("$index")
}

@JsName("PropertyMetadata")
internal class PropertyMetadata(@JsName("callableName") val name: String)

@JsName("noWhenBranchMatched")
internal fun noWhenBranchMatched(): Nothing = throw NoWhenBranchMatchedException()

@JsName("subSequence")
internal fun subSequence(c: CharSequence, startIndex: Int, endIndex: Int): CharSequence {
    if (c is String) {
        return c.substring(startIndex, endIndex)
    } else {
        return c.asDynamic().`subSequence_vux9f0$`(startIndex, endIndex)
    }
}

@JsName("captureStack")
internal fun captureStack(@Suppress("UNUSED_PARAMETER") baseClass: JsClass<in Throwable>, instance: Throwable) {
    if (js("Error").captureStackTrace) {
        // Using uncropped stack traces due to KT-37563.
        // Precise stack traces are implemented in JS IR compiler and stdlib
        js("Error").captureStackTrace(instance);
    } else {
        instance.asDynamic().stack = js("new Error()").stack;
    }
}

@JsName("newThrowable")
internal fun newThrowable(message: String?, cause: Throwable?): Throwable {
    val throwable = js("new Error()")
    throwable.message = if (jsTypeOf(message) == "undefined") {
        if (cause != null) cause.toString() else null
    } else {
        message
    }
    throwable.cause = cause
    throwable.name = "Throwable"
    return throwable
}

@JsName("BoxedChar")
internal class BoxedChar(val c: Int) : Comparable<Int> {
    override fun equals(other: Any?): Boolean {
        return other is BoxedChar && c == other.c
    }

    override fun hashCode(): Int {
        return c
    }

    override fun toString(): String {
        return js("this.c").unsafeCast<Char>().toString()
    }

    override fun compareTo(other: Int): Int {
        return js("this.c - other").unsafeCast<Int>()
    }

    @JsName("valueOf")
    public fun valueOf(): Int {
        return c
    }
}

@kotlin.internal.InlineOnly
internal inline fun <T> concat(args: Array<T>): T {
    val typed = js("Array")(args.size)
    for (i in args.indices) {
        val arr = args[i]
        if (arr !is Array<*>) {
            typed[i] = js("[]").slice.call(arr)
        } else {
            typed[i] = arr
        }
    }
    return js("[]").concat.apply(js("[]"), typed);
}

/** Concat regular Array's and TypedArray's into an Array.
 */
@PublishedApi
@JsName("arrayConcat")
@Suppress("UNUSED_PARAMETER")
internal fun <T> arrayConcat(a: T, b: T): T {
    return concat(js("arguments"))
}

/** Concat primitive arrays. Main use: prepare vararg arguments.
 *  For compatibility with 1.1.0 the arguments may be a mixture of Array's and TypedArray's.
 *
 *  If the first argument is TypedArray (Byte-, Short-, Char-, Int-, Float-, and DoubleArray) returns a TypedArray, otherwise an Array.
 *  If the first argument has the $type$ property (Boolean-, Char-, and LongArray) copy its value to result.$type$.
 *  If the first argument is a regular Array without the $type$ property default to arrayConcat.
 */
@PublishedApi
@JsName("primitiveArrayConcat")
@Suppress("UNUSED_PARAMETER")
internal fun <T> primitiveArrayConcat(a: T, b: T): T {
    val args: Array<T> = js("arguments")
    if (a is Array<*> && a.asDynamic().`$type$` === undefined) {
        return concat(args)
    } else {
        var size = 0
        for (i in args.indices) {
            size += args[i].asDynamic().length as Int
        }
        val result = js("new a.constructor(size)")
        kotlin.copyArrayType(a, result)
        size = 0
        for (i in args.indices) {
            val arr = args[i].asDynamic()
            for (j in 0 until arr.length) {
                result[size++] = arr[j]
            }
        }
        return result
    }
}

@JsName("booleanArrayOf")
internal fun booleanArrayOf() = withType("BooleanArray", js("[].slice.call(arguments)"))

@JsName("charArrayOf") // The arguments have to be slice'd here because of Rhino (see KT-16974)
internal fun charArrayOf() = withType("CharArray", js("new Uint16Array([].slice.call(arguments))"))

@JsName("longArrayOf")
internal fun longArrayOf() = withType("LongArray", js("[].slice.call(arguments)"))

@JsName("withType")
@kotlin.internal.InlineOnly
internal inline fun withType(type: String, array: dynamic): dynamic {
    array.`$type$` = type
    return array
}