package kotlin2

public trait LazyTraversable<T>: Traversable<T> {

    /**
    * Returns an iterator which invokes the function to calculate the next value on each iteration until the function returns *null*
    *
    * @includeFunctionBody ../../test/iterators/IteratorsTest.kt fibonacci
    */
    public fun iterate(nextFunction: () -> T?): Iterator<T> = FunctionIterator(nextFunction)

    /**
    * Returns an iterator over elements which match the given *predicate*
    *
    * @includeFunctionBody ../../test/iterators/IteratorsTest.kt filterAndTakeWhileExtractTheElementsWithinRange
    */
    public fun filter(predicate: (T) -> Boolean): Iterator<T> = FilterIterator<T>(iterator(), predicate)

    /** Returns an iterator over elements which do not match the given *predicate* */
    public fun filterNot(predicate: (T) -> Boolean): Iterator<T> = filter { !predicate(it) }

    /** Returns an iterator over non-*null* elements */
    public fun filterNotNull(): Iterator<T> = FilterNotNullIterator(iterator())

    /**
    * Returns an iterator obtained by applying *transform*, a function transforming an object of type *T* into an object of type *R*
    *
    * @includeFunctionBody ../../test/iterators/IteratorsTest.kt mapAndTakeWhileExtractTheTransformedElements
    */
    public fun <R> map(transform: (T) -> R): Iterator<R> = MapIterator<T, R>(iterator(), transform)

    /**
    * Returns an iterator over the concatenated results of transforming each element to one or more values
    *
    * @includeFunctionBody ../../test/iterators/IteratorsTest.kt flatMapAndTakeExtractTheTransformedElements
    */
    public fun <R> flatMap(transform: (T) -> Iterator<R>): Iterator<R> = FlatMapIterator<T, R>(iterator(), transform)

    /**
    * Creates an [[Iterator]] which iterates over this iterator then the given element at the end
    *
    * @includeFunctionBody ../../test/iterators/IteratorsTest.kt plus
    */
    public fun plus(element: T): Iterator<T> {
        return CompositeIterator<T>(iterator(), SingleIterator(element))
    }


    /**
    * Creates an [[Iterator]] which iterates over this iterator then the following iterator
    *
    * @includeFunctionBody ../../test/iterators/IteratorsTest.kt plusCollection
    */
    public fun plus(iterator: Iterator<T>): Iterator<T> {
        return CompositeIterator<T>(iterator(), iterator)
    }

    /**
    * Creates an [[Iterator]] which iterates over this iterator then the following collection
    *
    * @includeFunctionBody ../../test/iterators/IteratorsTest.kt plusCollection
    */
    public fun plus(collection: Iterable<T>): Iterator<T> = plus(collection.iterator())

    /**
    * Returns an iterator containing all the non-*null* elements, lazily throwing an [[IllegalArgumentException]]
    if there are any null elements
    *
    * @includeFunctionBody ../../test/iterators/IteratorsTest.kt requireNoNulls
    */
    public fun requireNoNulls(): Iterator<T> {
        // TODO since using Iterator<T> then T can't be null ;)
        return map<T>{
            if (it == null) throw IllegalArgumentException("null element in iterator $this") else it
        }
    }


    /**
    * Returns an iterator restricted to the first *n* elements
    *
    * @includeFunctionBody ../../test/iterators/IteratorsTest.kt takeExtractsTheFirstNElements
    */
    public fun take(n: Int): Iterator<T> {
        var count = n
        return takeWhile{ --count >= 0 }
    }

    /**
    * Returns an iterator restricted to the first elements that match the given *predicate*
    *
    * @includeFunctionBody ../../test/iterators/IteratorsTest.kt filterAndTakeWhileExtractTheElementsWithinRange
    */
    public fun takeWhile(predicate: (T) -> Boolean): Iterator<T> = TakeWhileIterator<T>(iterator(), predicate)

}
