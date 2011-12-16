namespace std.util

import java.util.*

/** Returns the size of the collection */
val Collection<*>.size : Int
    get() = size()

/** Returns true if this collection is empty */
val Collection<*>.empty : Boolean
    get() = isEmpty()

/** Returns a new ArrayList with a variable number of initial elements */
fun arrayList<T>(vararg values: T) : ArrayList<T> {
  val answer = ArrayList<T>()
  for (v in values)
    answer.add(v)
  return answer;
}

/** Returns a new HashSet with a variable number of initial elements */
fun hashSet<T>(vararg values: T) : HashSet<T> {
  val answer = HashSet<T>()
  for (v in values)
    answer.add(v)
  return answer;
}

/** Returns a new collection for the results of a helper method */
protected fun <T,R> java.lang.Iterable<T>.create(defaultSize: Int? = null) : Collection<R> {
  if (defaultSize != null) {
    return ArrayList<R>(defaultSize)
  } else {
    return ArrayList<R>()
  }
}

protected fun <T,R> Set<T>.create(defaultSize: Int? = null) : Set<R> {
  if (defaultSize != null) {
    return HashSet<R>(defaultSize)
  } else {
    return HashSet<R>()
  }
}



/** Returns true if any elements in the collection match the given predicate */
fun <T> java.lang.Iterable<T>.any(predicate: fun(T): Boolean) : Boolean {
  for (elem in this) {
    if (predicate(elem)) {
      return true
    }
  }
  return false
}

/** Returns true if all elements in the collection match the given predicate */
fun <T> java.lang.Iterable<T>.all(predicate: fun(T): Boolean) : Boolean {
  for (elem in this) {
    if (!predicate(elem)) {
      return false
    }
  }
  return true
}

/** Returns the first item in the collection which matches the given predicate or null if none matched */
fun <T> java.lang.Iterable<T>.find(predicate: fun(T): Boolean) : T? {
  for (elem in this) {
    if (predicate(elem))
      return elem
  }
  return null
}

/** Returns a new collection containing all elements in this collection which match the given predicate */
// TODO using: Collection<T> for the return type - I wonder if this exact type could be
// deduced somewhat from the This.Type; e.g. returning Set on a Set, Array on Array etc
fun <T> java.lang.Iterable<T>.filter(predicate: fun(T): Boolean) : Collection<T> {
  val result = this.create<T,T>()
  for (elem in this) {
    if (predicate(elem))
      result.add(elem)
  }
  return result
}

/** Returns a new collection containing the results of applying the given function to each element in this collection */
fun <T, R> java.lang.Iterable<T>.map(transform : fun(T) : R) : Collection<R> {
  val result = this.create<T,R>()
  for (item in this)
    result.add(transform(item))
  return result
}

/** Returns a new collection containing the results of applying the given function to each element in this collection */
fun <T, R> java.util.Collection<T>.map(transform : fun(T) : R) : Collection<R> {
  val result = this.create<T,R>(this.size)
  for (item in this)
    result.add(transform(item))
  return result
}