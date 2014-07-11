package foo

import java.util.ArrayList


data class Holder<T>(val v: T)

data class Dat(val start: String, val end: String)

class Obj(val start: String, val end: String)

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

val hashCoder: (o: Any) -> Int = { o -> o.hashCode() }

val <T> wrapInH = { (t: T) -> Holder(t) }

fun box(): String {

    // Check that same Dat's have the same hashcode.
    val sameDs = listOf(Dat("a", "b"), Dat("a", "b"))
    assertAllEqual(map(sameDs, hashCoder))

    // Check that different Dat's have different hashcodes (at least some of them).
    val differentDs = listOf(Dat("a", "b"), Dat("a", "c"), Dat("a", "d"))
    assertSomeNotEqual(map(differentDs, hashCoder))

    // Check the same on Obj's, which should be always different and with different hashcodes.
    val sameOs = listOf(Obj("a", "b"), Obj("a", "b"), Obj("a", "b"))
    val differentOs = listOf(Obj("a", "b"), Obj("a", "b"), Obj("a", "b"))

    // Obj's are always different.
    assertSomeNotEqual(map(sameOs, hashCoder))
    assertSomeNotEqual(map(differentOs, hashCoder))

    // Both Dat's and Obj's wrapped as Holder should retain their hashcode relations.
    val sameHDs = map(sameDs, wrapInH)
    assertAllEqual(map(sameHDs, hashCoder))
    val differentHDs = map(differentDs, wrapInH)
    assertSomeNotEqual(map(differentHDs, hashCoder))

    val sameHOs = map(sameOs, wrapInH)
    assertSomeNotEqual(map(sameHOs, hashCoder))
    val differentHOs = map(differentOs, wrapInH)
    assertSomeNotEqual(map(differentHOs, hashCoder))

    return "OK"
}

fun listOf<T>(vararg a: T): List<T> {
    val list = ArrayList<T>()
    for (e in a) {
        list.add(e)
    }
    return list
}

fun map<T, S>(c : Iterable<T>, transform : (T) -> S) : List<S> {
    val list = ArrayList<S>()
    for (t in c) {
        list.add(transform(t))
    }
    return list
}
