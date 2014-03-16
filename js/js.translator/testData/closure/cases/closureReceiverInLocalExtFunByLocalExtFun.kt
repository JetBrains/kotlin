package foo

fun assertEquals<T>(expected: T, actual: T, message: String) {
    if (expected != actual) throw Exception("Failed when $message, expected = $expected, actual = $actual")
}

// workaround for Rhino
var n = 0
class A {
    val i = ++n
}

fun box(): String {

    fun A.foo() {
        fun A.bar() {
            assertEquals(2, this.i, "check this.i in A.bar()")
            assertEquals(1, this@foo.i, "check this@foo.i in A.bar()")
        }
        val b = { assertEquals(1, this.i, "check this.i in b") }

        assertEquals(1, this.i, "check this.i in A.foo()")
        A().bar()
        b()
    }

    A().foo()

    return "OK"
}
