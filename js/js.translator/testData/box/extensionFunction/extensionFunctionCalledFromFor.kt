// EXPECTED_REACHABLE_NODES: 613
package foo

class SimpleEnumerator {
    private var counter = 0

    fun getNext(): String {
        counter++;
        return counter.toString()
    }

    fun hasMoreElements(): Boolean = counter < 1
}

class SimpleEnumeratorWrapper(private val enumerator: SimpleEnumerator) {
    operator fun hasNext(): Boolean = enumerator.hasMoreElements()

    operator fun next() = enumerator.getNext()
}

operator fun SimpleEnumerator.iterator(): SimpleEnumeratorWrapper {
    return SimpleEnumeratorWrapper(this)
}

fun box(): String {
    var o = ""
    val enumerator = SimpleEnumerator()
    for (s in enumerator) {
        o += s;
    }

    if (o != "1") return "fail: $o"

    return "OK"
}