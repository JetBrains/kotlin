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
    fun hasNext(): Boolean = enumerator.hasMoreElements()

    fun next() = enumerator.getNext()
}

fun SimpleEnumerator.iterator(): SimpleEnumeratorWrapper {
    return SimpleEnumeratorWrapper(this)
}

fun box(): Boolean {
    var o = ""
    val enumerator = SimpleEnumerator()
    for (s in enumerator) {
        o += s;
    }

    return o == "1"
}