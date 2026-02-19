package foo

class A<T>(val someValue: T) {
    private fun <A> doSomething(a: A, b: T = someValue) =
        a.toString() + b.toString()

    fun <B> getResult(a: B) = doSomething(a)
    fun <B> getResult(a: B, b: T) = doSomething(a, b)
}

fun box(): String {
    val a = A("K")

    var result = a.getResult("O")
    if (result != "OK") return "Fail: unexpected result of calling get result with one argument: $result"

    result = a.getResult("TEST", "1")
    if (result != "TEST1") return "Fail: unexpected result of calling get result with two arguments: $result"

    return "OK"
}
