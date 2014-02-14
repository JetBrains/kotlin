package foo

fun run<T>(f: () -> T) = f()

class Fail(val message: String) : RuntimeException(message) {
    val isFail = true // workaround for exception handling
}


class A {
    var testName = ""
    fun assertEquals(actual: Int, expected: Int, message: String) =
            if (actual != expected) throw Fail("$message in $testName test.")

    val a = 12
    var b = 1

    fun boo(c: Int) = c

    fun litlit() {
        testName = "litlit"
        run {
            run {
                assertEquals(a, 12, "a != 12")

                assertEquals(b, 1, "b != 1")
                b = 23
                assertEquals(b, 23, "b != 23")

                assertEquals(boo(34), 34, "boo(34) != 34")
            }
        }
    }

    fun funfun() {
        testName = "funfun"
        fun foo() {
            fun bar() {
                assertEquals(a, 12, "a != 12")

                assertEquals(b, 1, "b != 1")
                b = 23
                assertEquals(b, 23, "b != 23")

                assertEquals(boo(34), 34, "boo(34) != 34")
            }
            bar()
        }
        foo()
    }

    fun litfun() {
        testName = "litfun"
        run {
            fun bar() {
                assertEquals(a, 12, "a != 12")

                assertEquals(b, 1, "b != 1")
                b = 23
                assertEquals(b, 23, "b != 23")

                assertEquals(boo(34), 34, "boo(34) != 34")
            }
            bar()
        }
    }

    fun funlit() {
        testName = "funlit"
        fun foo() {
            run {
                assertEquals(a, 12, "a != 12")

                assertEquals(b, 1, "b != 1")
                b = 23
                assertEquals(b, 23, "b != 23")

                assertEquals(boo(34), 34, "boo(34) != 34")
            }
        }
        foo()
    }
}

fun box(): String {
    try {
        A().litlit()
        A().funfun()
        A().litfun()
        A().funlit()
    }
    catch(f: Fail) {
        if (!f.isFail) throw f
        return f.message
    }

    return "OK"
}