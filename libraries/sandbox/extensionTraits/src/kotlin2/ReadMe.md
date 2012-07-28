## Experimental use of traits to define extension methods

This module provides a port of the standard kotlin library into traits to more easily understand the shapes of functions and how they are grouped together, overridden and applied in different circumstances.

* [Traversable](https://github.com/JetBrains/kotlin/blob/master/libraries/sandbox/extensionTraits/src/kotlin2/Traversable.kt#L5) applies to all kinds of object which provide some kind of way to traverse values; so all objects like Iterable, Iterator, Collection, Array etc.
* [TraversableWithSize](https://github.com/JetBrains/kotlin/blob/master/libraries/sandbox/extensionTraits/src/kotlin2/TraversableWithSize.kt#L5) for non-stream based collections where its fast to calculate the size
* [EagerTraversable](https://github.com/JetBrains/kotlin/blob/master/libraries/sandbox/extensionTraits/src/kotlin2/EagerTraversable.kt#L7) for typical collections like List, Set, Arrays where operations like filter() / flatMap() / map() are calculated eagerly for simplicity returning a List<T>
* [LazyTraversable](https://github.com/JetBrains/kotlin/blob/master/libraries/sandbox/extensionTraits/src/kotlin2/LazyTraversable.kt#L3) for Iterator and streams where operations like filter() / flatMap() / map() are performed lazily returning Iterator<T>
* [CollectionLike](https://github.com/JetBrains/kotlin/blob/master/libraries/sandbox/extensionTraits/src/kotlin2/CollectionLike.kt#L3) for things like Sets where we can just iterate and know the size but cannot access by index
* [ListLike](https://github.com/JetBrains/kotlin/blob/master/libraries/sandbox/extensionTraits/src/kotlin2/ListLike.kt#L3) for things like List and Arrays where we can access items in a collection using random access by index

The missing bit is then how to take these traits (which have no state) and turn them into sets of extension functions on different source types.

As an experiment, we've used annotated classes (with the extension annotation) to bind the traits to a real objects using the binding file

* [BindExensionFunctions](https://github.com/JetBrains/kotlin/blob/master/libraries/sandbox/extensionTraits/src/kotlin2/BindExtensionFunctions.kt#L11)

The idea here is we use an annotated class with a single field (like the 'this' in an extension function), such that all the functions on the extension class can be turned into the equivalent of an extension function, replacing the 'that' field in the class with the 'this' value in the extension function.

So we'd transform...

    extension open class CollectionExtensions<T>(private val that: java.util.Collection<T>): CollectionLike<T> {
      ...
    }

to be this (for each concrete method)...

    public inline fun java.util.Collection<T>.all(predicate: (T) -> Boolean): Boolean {
      for (element in this) if (!predicate(element)) return false
      return true
    }

