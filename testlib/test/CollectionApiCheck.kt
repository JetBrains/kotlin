namespace test.apicheck

import std.util.*
import java.util.*

trait Traversable<T> {

  /** Returns true if any elements in the collection match the given predicate */
  fun any(predicate: fun(T): Boolean) : Boolean

  /** Returns true if all elements in the collection match the given predicate */
  fun all(predicate: fun(T): Boolean) : Boolean

  /** Returns the first item in the collection which matches the given predicate or null if none matched */
  fun find(predicate: fun(T): Boolean) : T?

  /** Returns a new collection containing all elements in this collection which match the given predicate */
  // TODO using: Collection<T> for the return type - I wonder if this exact type could be
  // deduced somewhat from the This.Type; e.g. returning Set on a Set, Array on Array etc
  fun filter(predicate: fun(T): Boolean) : Collection<T>

  /** Performs the given operation on each element inside the collection */
  fun foreach(operation: fun(element: T) : Unit)

  /** Returns a new collection containing the results of applying the given function to each element in this collection */
  fun <T, R> java.lang.Iterable<T>.map(transform : fun(T) : R) : Collection<R>
}

/**
TODO try use delegation here to make sure we implement all the methods in the Traversable API

class ListImpl<T>(coll: ArrayList<out T>) : Traversable<T> by coll {
}

*/