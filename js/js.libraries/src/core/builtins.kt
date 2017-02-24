/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

@JsName("arrayIterator")
internal fun arrayIterator(array: dynamic): MutableIterator<dynamic> {
    return object : MutableIterator<dynamic> {
        var index = 0;

        override fun hasNext(): Boolean {
            val length: Int = array.length
            return index < length
        }

        override fun next() = array[index++]

        override fun remove() {
            array.splice(--index, 1)
        }
    }
}

@JsName("PropertyMetadata")
internal class PropertyMetadata(@JsName("callableName") val name: String)

@JsName("noWhenBranchMatched")
internal fun noWhenBranchMatched(): Nothing = throw NoWhenBranchMatchedException()

@JsName("subSequence")
internal fun subSequence(c: CharSequence, startIndex: Int, endIndex: Int): CharSequence {
    if (c is String) {
        return c.substring(startIndex, endIndex)
    }
    else {
        return c.asDynamic().`subSequence_vux9f0$`(startIndex, endIndex)
    }
}

@JsName("captureStack")
internal fun captureStack(baseClass: JsClass<in Throwable>, instance: Throwable) {
    if (js("Error").captureStackTrace) {
        js("Error").captureStackTrace(instance, instance::class.js);
    }
    else {
        instance.asDynamic().stack = js("new Error()").stack;
    }
}

@JsName("newThrowable")
internal fun newThrowable(message: String?, cause: Throwable?): Throwable {
    val throwable = js("new Error()")
    throwable.message = if (jsTypeOf(message) == "undefined") {
        if (cause != null) cause.toString() else null
    }
    else {
        message
    }
    throwable.cause = cause
    throwable.name = "Throwable"
    return throwable
}

@JsName("BoxedChar")
internal class BoxedChar(val c: Char) : Comparable<Char> {
    override fun equals(other: Any?): Boolean {
        return other is BoxedChar && c == other.c
    }

    override fun hashCode(): Int {
        return c.toInt()
    }

    override fun toString(): String {
        return c.toString()
    }

    override fun compareTo(other: Char): Int {
        return c - other
    }

    @JsName("valueOf")
    public fun valueOf(): Int {
        return js("this.c")
    }
}

/* For future binary compatibility with TypedArrays
 * TODO: concat normal Array's and TypedArrays into an Array
 */
@PublishedApi
@JsName("arrayConcat")
internal fun <T> arrayConcat(a: T, b: T): T {
    return a.asDynamic().concat.apply(js("[]"), js("arguments"));
}

/* For future binary compatibility with TypedArrays
 * TODO: concat primitive arrays.
 *       For Byte-, Short-, Int-, Float-, and DoubleArray concat result into a TypedArray.
 *       For Boolean-, Char-, and LongArray return an Array with corresponding type property.
 *       Default to Array.prototype.concat for compatibility.
 */
@PublishedApi
@JsName("primitiveArrayConcat")
internal fun <T> primitiveArrayConcat(a: T, b: T): T {
    return a.asDynamic().concat.apply(js("[]"), js("arguments"));
}