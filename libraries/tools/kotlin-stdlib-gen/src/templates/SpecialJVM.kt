package templates

import templates.Family.*

fun specialJVM(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("plusElement(element: T)") {
        inline(Inline.Only)

        only(InvariantArraysOfObjects)
        returns("SELF")
        doc { "Returns an array containing all elements of the original array and then the given [element]." }
        body { "return plus(element)" }
    }

    templates add f("plus(element: T)") {
        operator(true)

        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        returns("SELF")
        doc { "Returns an array containing all elements of the original array and then the given [element]." }
        body() {
            """
            val index = size
            val result = Arrays.copyOf(this, index + 1)
            result[index] = element
            return result
            """
        }
    }

    templates add f("plus(elements: Collection<T>)") {
        operator(true)

        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        returns("SELF")
        doc { "Returns an array containing all elements of the original array and then all elements of the given [elements] collection." }
        body {
            """
            var index = size
            val result = Arrays.copyOf(this, index + elements.size)
            for (element in elements) result[index++] = element
            return result
            """
        }
    }

    templates add f("plus(elements: SELF)") {
        operator(true)

        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        customSignature(InvariantArraysOfObjects) { "plus(elements: Array<out T>)" }
        doc { "Returns an array containing all elements of the original array and then all elements of the given [elements] array." }
        returns("SELF")
        body {
            """
            val thisSize = size
            val arraySize = elements.size
            val result = Arrays.copyOf(this, thisSize + arraySize)
            System.arraycopy(elements, 0, result, thisSize, arraySize)
            return result
            """
        }
    }


    templates add f("copyOfRange(fromIndex: Int, toIndex: Int)") {
        inline(Inline.Only)

        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns new array which is a copy of range of original array." }
        returns("SELF")
        body {
            "return Arrays.copyOfRange(this, fromIndex, toIndex)"
        }
    }

    templates add f("copyOf()") {
        inline(Inline.Only)

        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns new array which is a copy of the original array." }
        returns("SELF")
        body {
            "return Arrays.copyOf(this, size)"
        }
    }

    // This overload can cause nulls if array size is expanding, hence different return overload
    templates add f("copyOf(newSize: Int)") {
        inline(Inline.Only)

        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns new array which is a copy of the original array." }
        returns("SELF")
        returns(InvariantArraysOfObjects) { "Array<T?>" }
        body {
            "return Arrays.copyOf(this, newSize)"
        }
    }

    templates add f("fill(element: T, fromIndex: Int = 0, toIndex: Int = size)") {
        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        doc { "Fills original array with the provided value." }
        returns { "Unit" }
        body {
            """
            Arrays.fill(this, fromIndex, toIndex, element)
            """
        }
    }

    templates add f("binarySearch(element: T, fromIndex: Int = 0, toIndex: Int = size)") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        exclude(PrimitiveType.Boolean)
        doc {
            """
            Searches the array or the range of the array for the provided [element] using the binary search algorithm.
            The array is expected to be sorted, otherwise the result is undefined.

            If the array contains multiple elements equal to the specified [element], there is no guarantee which one will be found.

            @return the index of the element, if it is contained in the array within the specified range;
            otherwise, the inverted insertion point `(-insertion point - 1)`.
            The insertion point is defined as the index at which the element should be inserted,
            so that the array (or the specified subrange of array) still remains sorted.
            """
        }
        returns("Int")
        body {
            "return Arrays.binarySearch(this, fromIndex, toIndex, element)"
        }
    }

    templates add f("binarySearch(element: T, comparator: Comparator<in T>, fromIndex: Int = 0, toIndex: Int = size)") {
        only(ArraysOfObjects)
        doc {
            """
            Searches the array or the range of the array for the provided [element] using the binary search algorithm.
            The array is expected to be sorted according to the specified [comparator], otherwise the result is undefined.

            If the array contains multiple elements equal to the specified [element], there is no guarantee which one will be found.

            @return the index of the element, if it is contained in the array within the specified range;
            otherwise, the inverted insertion point `(-insertion point - 1)`.
            The insertion point is defined as the index at which the element should be inserted,
            so that the array (or the specified subrange of array) still remains sorted according to the specified [comparator].
            """
        }
        returns("Int")
        body {
            "return Arrays.binarySearch(this, fromIndex, toIndex, element, comparator)"
        }
    }


    templates add f("sort()") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        exclude(PrimitiveType.Boolean)
        doc { "Sorts the array in-place." }
        returns("Unit")
        body {
            "if (size > 1) Arrays.sort(this)"
        }
    }

    templates add f("sort(fromIndex: Int = 0, toIndex: Int = size)") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        exclude(PrimitiveType.Boolean)
        doc { "Sorts a range in the array in-place." }
        returns("Unit")
        body {
            "Arrays.sort(this, fromIndex, toIndex)"
        }
    }

    templates add f("sortWith(comparator: Comparator<in T>)") {
        only(ArraysOfObjects)
        doc { "Sorts the array in-place with the given [comparator]." }
        returns("Unit")
        body {
            "if (size > 1) Arrays.sort(this, comparator)"
        }
    }

    templates add f("sortWith(comparator: Comparator<in T>, fromIndex: Int = 0, toIndex: Int = size)") {
        only(ArraysOfObjects)
        doc { "Sorts a range in the array in-place with the given [comparator]." }
        returns("Unit")
        body {
            "Arrays.sort(this, fromIndex, toIndex, comparator)"
        }
    }

    templates add f("filterIsInstanceTo(destination: C, klass: Class<R>)") {
        doc { "Appends all elements that are instances of specified class to the given [destination]." }
        receiverAsterisk(true)
        typeParam("C : MutableCollection<in R>")
        typeParam("R")
        returns("C")
        exclude(ArraysOfPrimitives, Strings)
        body {
            """
            for (element in this) if (klass.isInstance(element)) destination.add(element as R)
            return destination
            """
        }
    }

    templates add f("filterIsInstance(klass: Class<R>)") {
        doc { "Returns a list containing all elements that are instances of specified class." }
        receiverAsterisk(true)
        typeParam("R")
        returns("List<R>")
        body {
            """
            return filterIsInstanceTo(ArrayList<R>(), klass)
            """
        }
        exclude(ArraysOfPrimitives, Strings)

        doc(Sequences) { "Returns a sequence containing all elements that are instances of specified class." }
        returns(Sequences) { "Sequence<R>" }
        body(Sequences) {
            """
            return filter { klass.isInstance(it) } as Sequence<R>
            """
        }
    }

    templates add f("filterIsInstanceTo(destination: C)") {
        doc { "Appends all elements that are instances of specified type parameter R to the given [destination]." }
        typeParam("reified R")
        typeParam("C : MutableCollection<in R>")
        inline(true)
        receiverAsterisk(true)
        returns("C")
        exclude(ArraysOfPrimitives, Strings)
        body {
            """
            for (element in this) if (element is R) destination.add(element)
            return destination
            """
        }
    }

    templates add f("filterIsInstance()") {
        doc { "Returns a list containing all elements that are instances of specified type parameter R." }
        typeParam("reified R")
        returns("List<@kotlin.internal.NoInfer R>")
        inline(true)
        receiverAsterisk(true)
        body {
            """
            return filterIsInstanceTo(ArrayList<R>())
            """
        }
        exclude(ArraysOfPrimitives, Strings)

        doc(Sequences) { "Returns a sequence containing all elements that are instances of specified type parameter R." }
        returns(Sequences) { "Sequence<@kotlin.internal.NoInfer R>" }
        inline(true)
        receiverAsterisk(true)
        body(Sequences) {
            """
            return filter { it is R } as Sequence<R>
            """
        }
    }

    templates add f("asList()") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns a [List] that wraps the original array." }
        returns("List<T>")
        body(ArraysOfObjects) {
            """
            return ArraysUtilJVM.asList(this)
            """
        }

        // TODO: Use own readonly kotlin.AbstractList
        body(ArraysOfPrimitives) {
            """
            return object : AbstractList<T>(), RandomAccess {
                override val size: Int get() = this@asList.size
                override fun isEmpty(): Boolean = this@asList.isEmpty()
                override fun contains(element: T): Boolean = this@asList.contains(element)
                override fun get(index: Int): T = this@asList[index]
                override fun indexOf(element: T): Int = this@asList.indexOf(element)
                override fun lastIndexOf(element: T): Int = this@asList.lastIndexOf(element)
            }
            """
        }
    }

    templates add f("toTypedArray()") {
        only(ArraysOfPrimitives)
        returns("Array<T>")
        doc {
            """
            Returns a *typed* object array containing all of the elements of this primitive array.
            """
        }
        body {
            """
            val result = arrayOfNulls<T>(size)
            for (index in indices)
                result[index] = this[index]
            return result as Array<T>
            """
        }
    }

    templates.forEach { it.apply { jvmOnly(true) } }

    return templates
}