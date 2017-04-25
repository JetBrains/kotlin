// EXPECTED_REACHABLE_NODES: 508
package foo

class A {
    val a = 12
    var b = 1

    fun boo(c: Int) = c

    fun litlit() {
        val testName = "litlit"
        myRun {
            myRun {
                assertEquals(12, a, testName)

                assertEquals(1, b, testName)
                b = 23
                assertEquals(23, b, testName)

                assertEquals(34, boo(34), testName)
            }
        }
    }

    fun funfun() {
        val testName = "funfun"
        fun foo() {
            fun bar() {
                assertEquals(12, a, testName)

                assertEquals(1, b, testName)
                b = 23
                assertEquals(23, b, testName)

                assertEquals(34, boo(34), testName)
            }
            bar()
        }
        foo()
    }

    fun litfun() {
        val testName = "litfun"
        myRun {
            fun bar() {
                assertEquals(12, a, testName)

                assertEquals(1, b, testName)
                b = 23
                assertEquals(23, b, testName)

                assertEquals(34, boo(34), testName)
            }
            bar()
        }
    }

    fun funlit() {
        val testName = "funlit"
        fun foo() {
            myRun {
                assertEquals(12, a, testName)

                assertEquals(1, b, testName)
                b = 23
                assertEquals(23, b, testName)

                assertEquals(34, boo(34), testName)
            }
        }
        foo()
    }
}

fun box(): String {
    A().litlit()
    A().funfun()
    A().litfun()
    A().funlit()

    return "OK"
}
