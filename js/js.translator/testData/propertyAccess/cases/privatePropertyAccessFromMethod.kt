package foo

object A {
    private val foo = 23

    fun bar(): Int {
        // Erase object so that we ensure that access to 'foo' property is performed via 'this'
        // It's no crucial from runtime standpoind, but JS code looks more concise and natural this way.
        erase()
        return foo
    }

    fun erase() {
        js("_.foo.A = null")
    }
}

fun box(): String {
    var result = A.bar()
    if (result != 23) return "failed: ${result}"
    return "OK"
}