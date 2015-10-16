package templates

import templates.Family.*

fun specialJVM(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("plus(element: T)") {
        operator(true)

        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        returns("SELF")
        doc { "Returns an array containing all elements of the original array and then the given [element]." }
        body() {
            """
            val index = size()
            val result = Arrays.copyOf(this, index + 1)
            result[index] = element
            return result
            """
        }
    }

    templates add f("plus(collection: Collection<T>)") {
        operator(true)

        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        returns("SELF")
        doc { "Returns an array containing all elements of the original array and then all elements of the given [collection]." }
        body {
            """
            var index = size()
            val result = Arrays.copyOf(this, index + collection.size())
            for (element in collection) result[index++] = element
            return result
            """
        }
    }

    templates add f("plus(array: SELF)") {
        operator(true)

        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        customSignature(InvariantArraysOfObjects) { "plus(array: Array<out T>)" }
        doc { "Returns an array containing all elements of the original array and then all elements of the given [array]." }
        returns("SELF")
        body {
            """
            val thisSize = size()
            val arraySize = array.size()
            val result = Arrays.copyOf(this, thisSize + arraySize)
            System.arraycopy(array, 0, result, thisSize, arraySize)
            return result
            """
        }
    }


    templates add f("copyOfRange(fromIndex: Int, toIndex: Int)") {
        only(ArraysOfObjects, InvariantArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns new array which is a copy of range of original array." }
        returns("SELF")
        annotations(InvariantArraysOfObjects) { """@JvmName("mutableCopyOfRange")"""}
        body {
            "return Arrays.copyOfRange(this, fromIndex, toIndex)"
        }
    }

    templates add f("copyOf()") {
        only(ArraysOfObjects, InvariantArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns new array which is a copy of the original array." }
        returns("SELF")
        annotations(InvariantArraysOfObjects) { """@JvmName("mutableCopyOf")"""}
        body {
            "return Arrays.copyOf(this, size())"
        }
    }

    // This overload can cause nulls if array size is expanding, hence different return overload
    templates add f("copyOf(newSize: Int)") {
        only(ArraysOfObjects, InvariantArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns new array which is a copy of the original array." }
        returns("SELF")
        body {
            "return Arrays.copyOf(this, newSize)"
        }
        returns(ArraysOfObjects) { "Array<out T?>" }
        returns(InvariantArraysOfObjects) { "Array<T?>" }
        annotations(InvariantArraysOfObjects) { """@JvmName("mutableCopyOf")"""}
    }

    templates add f("fill(element: T, fromIndex: Int = 0, toIndex: Int = size())") {
        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        doc { "Fills original array with the provided value." }
        returns { "Unit" }
        body {
            """
            Arrays.fill(this, fromIndex, toIndex, element)
            """
        }
    }

    templates add f("binarySearch(element: T, fromIndex: Int = 0, toIndex: Int = size())") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        exclude(PrimitiveType.Boolean)
        doc { "Searches array or range of array for provided element index using binary search algorithm. Array is expected to be sorted." }
        returns("Int")
        body {
            "return Arrays.binarySearch(this, fromIndex, toIndex, element)"
        }
    }

    templates add f("binarySearch(element: T, comparator: Comparator<in T>, fromIndex: Int = 0, toIndex: Int = size())") {
        only(ArraysOfObjects)
        doc { "Searches array or range of array for provided element index using binary search algorithm. Array is expected to be sorted according to the specified [comparator]." }
        returns("Int")
        body {
            "return Arrays.binarySearch(this, fromIndex, toIndex, element, comparator)"
        }
    }

    templates add f("sort(fromIndex: Int = 0, toIndex: Int = size())") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        exclude(PrimitiveType.Boolean)
        doc { "Sorts array or range in array inplace." }
        returns("Unit")
        body {
            "Arrays.sort(this, fromIndex, toIndex)"
        }
    }

    templates add f("sortWith(comparator: Comparator<in T>, fromIndex: Int = 0, toIndex: Int = size())") {
        only(ArraysOfObjects)
        doc { "Sorts array or range in array inplace." }
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
        exclude(ArraysOfPrimitives, CharSequences)
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
        exclude(ArraysOfPrimitives, CharSequences)

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
        exclude(ArraysOfPrimitives, CharSequences)
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
        returns("List<R>")
        inline(true)
        receiverAsterisk(true)
        body {
            """
            return filterIsInstanceTo(ArrayList<R>())
            """
        }
        exclude(ArraysOfPrimitives, CharSequences)

        doc(Sequences) { "Returns a sequence containing all elements that are instances of specified type parameter R." }
        returns(Sequences) { "Sequence<R>" }
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

        body(ArraysOfPrimitives) {
            """
            return object : AbstractList<T>(), RandomAccess {
                override val size: Int get() = this@asList.size()
                override fun isEmpty(): Boolean = this@asList.isEmpty()
                override fun contains(o: T): Boolean = this@asList.contains(o)
                override fun iterator(): MutableIterator<T> = this@asList.iterator() as MutableIterator<T>
                override fun get(index: Int): T = this@asList[index]
                override fun indexOf(o: T): Int = this@asList.indexOf(o)
                override fun lastIndexOf(o: T): Int = this@asList.lastIndexOf(o)
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
            val result = arrayOfNulls<T>(size())
            for (index in indices)
                result[index] = this[index]
            return result as Array<T>
            """
        }
    }

    templates.forEach { it.jvmOnly(true) }

    return templates
}