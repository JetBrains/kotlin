package foo

import java.util.ArrayList


data class Holder<T>(val v: T)

data class D(val start: String, val end: String)

class O(val start: String, val end: String)


fun assertEquals<T>(expected: T, actual: T) {
    if (expected != actual) throw Exception("expected: $expected, actual, $actual")
}

fun assertSomeNotEqual<T>(c: Iterable<T>) {
    val it = c.iterator()
    val first = it.next()
    while (it.hasNext()) {
        val item: T = it.next()
        if (item != first) {
            return;
        }
    }
    throw Exception("All elements are the same: $first")
}
fun assertAllEqual<T>(c: Iterable<out T>) {
    val it = c.iterator()
    val first = it.next()
    while (it.hasNext()) {
        val item: T = it.next()
        assertEquals(first, item)
    }
}

// This is described in documenctation at http://confluence.jetbrains.com/display/Kotlin/Functions
// but can't seem to work:
//
// fun asList<T>(vararg<ArrayList<T>> ts : T) : List<T> = ts


// Two duplicated functions, because vararg Array does not implement Iterable.
fun listOf<T>(vararg a: T): ArrayList<T> = makeList(a)
    val list = ArrayList<T>();
    for (e in a) {
        list.add(e)
    }
    return list
}

fun makeList<T>(i: Iterable<T>): List<T> {
    val result = ArrayList<T>()
    for (element in i) {
        result.add(element)
    }
    return result
}


fun map<T, S>(c : Iterable<T>, transform : (T) -> S) : Iterable<S> {
  return object : Iterable<S> {
      public override fun iterator() : Iterator<S> {
          val it = c.iterator()
          return object : Iterator<S> {
              public override fun next(): S = transform(it.next())
              public override fun hasNext(): Boolean = it.hasNext()
          }
      }
  }
}

// Unclear how to refer regular 'fun' as a function value.
//fun hashCoder(o : Any) : Int = o.hashCode()
val hashCoder: (o: Any) -> Int = { o -> o.hashCode() }

// Unclear how to create generic function value.
// val wrapInH: <T> (t: T) -> Holder<T> = { t -> Holder(t) }
fun wrapInHBuilder<T>(): (t: T) -> Holder<T> = { t -> Holder(t) }

fun box(): String {

    // Check that same D's have the same hashcode.
    val sameDs: Collection<D> = listOf(D("a", "b"), D("a", "b"))
    assertAllEqual(map(sameDs, hashCoder))

    // Check that different D's have different hashcodes (at least some of them).
    val differentDs: Collection<D> = listOf(D("a", "b"), D("a", "c"), D("a", "d"))
    assertSomeNotEqual(map(differentDs, hashCoder))

    // Check the same on O's, which should be always different and with different hashcodes.
    // Blocked since we cannot actually call hashCode on Any.
    val sameOs: Collection<O> = listOf(O("a", "b"), O("a", "b"), O("a", "b"))
    val differentOs: Collection<O> = listOf(O("a", "b"), O("a", "b"), O("a", "b"))

    if (false) {
        // O's are always different.
        assertSomeNotEqual(map(sameOs, hashCoder))

        assertSomeNotEqual(map(differentOs, hashCoder))
    }

    // Both D's and O's wrapped as Holder should retain their hashcode relations.
    val sameHDs = makeList(map(sameDs, wrapInHBuilder<D>()))
    assertAllEqual(map(sameHDs, hashCoder))
    val differentHDs = makeList(map(differentDs, wrapInHBuilder<D>()))
    assertSomeNotEqual(map(differentHDs, hashCoder))

    val sameHOs = makeList(map(sameOs, wrapInHBuilder<O>()))
    assertSomeNotEqual(map(sameHOs, hashCoder))
    val differentHOs = makeList(map(differentOs, wrapInHBuilder<O>()))
    assertSomeNotEqual(map(differentHOs, hashCoder))

    return "OK"
}
