package kotlin2

import java.util.ArrayList

public trait EagerTraversable<T>: Traversable<T> {
    /**
    * Returns a list containing all elements which match the given *predicate*
    *
    * @includeFunctionBody ../../test/CollectionTest.kt filter
    */
    public inline fun filter(predicate: (T) -> Boolean): List<T> = filterTo(ArrayList<T>(), predicate)

    /**
    * Returns a list containing all elements which do not match the given predicate
    *
    * @includeFunctionBody ../../test/CollectionTest.kt filterNot
    */
    public inline fun filterNot(predicate: (T)-> Boolean): List<T> = filterNotTo(ArrayList<T>(), predicate)

    /**
    * Returns a list containing all the non-*null* elements
    *
    * @includeFunctionBody ../../test/CollectionTest.kt filterNotNull
    */
    public inline fun filterNotNull(): List<T> = filterNotNullTo<T, ArrayList<T>>(java.util.ArrayList<T>())

    /**
    * Returns the result of transforming each element to one or more values which are concatenated together into a single collection
    *
    * @includeFunctionBody ../../test/CollectionTest.kt flatMap
    */
    public inline fun <R> flatMap(transform: (T)-> Collection<R>): Collection<R> = flatMapTo(ArrayList<R>(), transform)


    /**
    * Returns a list containing all the non-*null* elements, throwing an [[IllegalArgumentException]] if there are any null elements
    *
    * @includeFunctionBody ../../test/CollectionTest.kt requireNoNulls
    */
    public inline fun java.lang.Iterable<T?>?.requireNoNulls(): List<T> {
        val list = ArrayList<T>()
        for (element in this) {
            if (element == null) {
                throw IllegalArgumentException("null element found in $this")
            } else {
                list.add(element)
            }
        }
        return list
    }

    /**
    * Returns a list containing the first *n* elements
    *
    * @includeFunctionBody ../../test/CollectionTest.kt take
    */
    public inline fun take(n: Int): List<T> {
        return takeWhile(countTo(n))
    }

    /**
    * Returns a list containing the first elements that satisfy the given *predicate*
    *
    * @includeFunctionBody ../../test/CollectionTest.kt takeWhile
    */
    public inline fun takeWhile(predicate: (T) -> Boolean): List<T> = takeWhileTo(ArrayList<T>(), predicate)

    /**
    * Creates a copy of this collection as a [[List]] with the element added at the end
    *
    * @includeFunctionBody ../../test/CollectionTest.kt plus
    */
    public inline fun plus(element: T): List<in T> {
        val list = toCollection(ArrayList<T>())
        list.add(element)
        return list
    }


    /**
    * Creates a copy of this collection as a [[List]] with all the elements added at the end
    *
    * @includeFunctionBody ../../test/CollectionTest.kt plusCollection
    */
    public inline fun plus(elements: java.lang.Iterable<T>): List<T> {
        val list = toCollection(ArrayList<T>())
        list.addAll(elements.toCollection())
        return list
    }


}