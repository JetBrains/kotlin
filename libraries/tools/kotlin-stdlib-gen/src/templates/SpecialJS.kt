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

package templates

import templates.Family.*

fun specialJS(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()



    templates add f("plusElement(element: T)") {
        only(ArraysOfObjects)
        returns("SELF")
        returns(ArraysOfObjects) { "Array<T>" }
        inline(true)
        annotations("""@Suppress("NOTHING_TO_INLINE")""")
        doc { "Returns an array containing all elements of the original array and then the given [element]." }
        body() {
            """
            return this.asDynamic().concat(arrayOf(element))
            """
        }
    }

    templates add f("plus(element: T)") {
        operator(true)

        only(ArraysOfObjects, ArraysOfPrimitives)
        returns("SELF")
        returns(ArraysOfObjects) { "Array<T>" }
        inline(true)
        annotations("""@Suppress("NOTHING_TO_INLINE")""")
        doc { "Returns an array containing all elements of the original array and then the given [element]." }
        body() {
            """
            return this.asDynamic().concat(arrayOf(element))
            """
        }
    }

    templates add f("plus(elements: Collection<T>)") {
        operator(true)

        only(ArraysOfObjects, ArraysOfPrimitives)
        returns("SELF")
        returns(ArraysOfObjects) { "Array<T>" }
        doc { "Returns an array containing all elements of the original array and then all elements of the given [elements] collection." }
        body {
            """
            return arrayPlusCollection(this, elements)
            """
        }
    }

    // This overload can cause nulls if array size is expanding, hence different return overload
    templates add f("plus(elements: SELF)") {
        operator(true)

        only(ArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns an array containing all elements of the original array and then all elements of the given [elements] array." }
        inline(true)
        annotations("""@Suppress("NOTHING_TO_INLINE")""")
        returns("SELF")
        returns(ArraysOfObjects) { "Array<T>" }
        body {
            """
            return this.asDynamic().concat(elements)
            """
        }
    }

    templates add f("sort(noinline comparison: (T, T) -> Int)") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        exclude(PrimitiveType.Boolean)
        inline(true)
        returns("Unit")
        doc { "Sorts the array in-place according to the order specified by the given [comparison] function." }
        body { "asDynamic().sort(comparison)" }
    }

    return templates
}