/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package kotlin
import konan.internal.ExportForCompiler
import konan.internal.InlineConstructor

// TODO: remove that, as RTTI shall be per instantiation.
@ExportTypeInfo("theArrayTypeInfo")
public final class Array<T> {
    // Constructors are handled with compiler magic.
    @InlineConstructor
    public constructor(size: Int, init: (Int) -> T): this(size) {
        var index = 0
        while (index < size) {
            this[index] = init(index)
            index++
        }
    }

    @ExportForCompiler
    internal constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}

    public val size: Int
        get() = getArrayLength()

    @SymbolName("Kotlin_Array_get")
    external public operator fun get(index: Int): T

    @SymbolName("Kotlin_Array_set")
    external public operator fun set(index: Int, value: T): Unit

    public operator fun iterator(): kotlin.collections.Iterator<T> {
        return IteratorImpl(this)
    }

    // Konan-specific.
    @SymbolName("Kotlin_Array_getArrayLength")
    external private fun getArrayLength(): Int
}

private class IteratorImpl<T>(val collection: Array<T>) : Iterator<T> {
    var index : Int = 0

    public override fun next(): T {
        if (!hasNext()) throw NoSuchElementException("$index")
        return collection[index++]
    }

    public override operator fun hasNext(): Boolean {
        return index < collection.size
    }
}


@kotlin.internal.InlineOnly
public inline operator fun <T> Array<T>.plus(elements: Array<T>): Array<T> {
    val result = copyOfUninitializedElements(this.size + elements.size)
    elements.copyRangeTo(result, 0, elements.size, this.size)
    return result
}
