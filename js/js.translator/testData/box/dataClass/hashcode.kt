// EXPECTED_REACHABLE_NODES: 916
package foo


data class Holder<T>(val v: T)

data class Dat(val start: String, val end: String)

class Obj(val start: String, val end: String)

fun <T> assertSomeNotEqual(c: Iterable<T>) {
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

fun <T> assertAllEqual(c: Iterable<out T>) {
    val it = c.iterator()
    val first = it.next()
    while (it.hasNext()) {
        val item: T = it.next()
        assertEquals(first, item)
    }
}

val hashCoder: (o: Any) -> Int = { o -> o.hashCode() }

fun <T> wrapInH(t: T) = Holder(t)

fun box(): String {

    // Check that same Dat's have the same hashcode.
    val sameDs = listOf(Dat("a", "b"), Dat("a", "b"))
    assertAllEqual(sameDs.map(hashCoder))

    // Check that different Dat's have different hashcodes (at least some of them).
    val differentDs = listOf(Dat("a", "b"), Dat("a", "c"), Dat("a", "d"))
    assertSomeNotEqual(differentDs.map(hashCoder))

    // Check the same on Obj's, which should be always different and with different hashcodes.
    val sameOs = listOf(Obj("a", "b"), Obj("a", "b"), Obj("a", "b"))
    val differentOs = listOf(Obj("a", "b"), Obj("a", "b"), Obj("a", "b"))

    // Obj's are always different.
    assertSomeNotEqual(sameOs.map(hashCoder))
    assertSomeNotEqual(differentOs.map(hashCoder))

    // Both Dat's and Obj's wrapped as Holder should retain their hashcode relations.
    val sameHDs = sameDs.map { wrapInH(it) }
    assertAllEqual(sameHDs.map(hashCoder))
    val differentHDs = differentDs.map { wrapInH(it) }
    assertSomeNotEqual(differentHDs.map(hashCoder))

    val sameHOs = sameOs.map { wrapInH(it) }
    assertSomeNotEqual(sameHOs.map(hashCoder))
    val differentHOs = differentOs.map { wrapInH(it) }
    assertSomeNotEqual(differentHOs.map(hashCoder))

    return "OK"
}
