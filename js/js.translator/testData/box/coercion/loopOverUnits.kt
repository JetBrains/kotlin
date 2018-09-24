// EXPECTED_REACHABLE_NODES: 1289
class A {
    operator fun iterator() = B()
}

class B() {
    private var count = 0

    operator fun next() {
        count++
    }

    operator fun hasNext() = count < 5
}

fun box(): String {
    var i = 0
    for (x: Any in A()) {
        if (x != Unit) return "fail1: $x"
        i++
    }
    if (i != 5) return "fail2: $i"

    return "OK"
}