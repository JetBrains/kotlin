// EXPECTED_REACHABLE_NODES: 505
package foo

class MyException(m: String? = null): Exception(m)
class MyException2(m: String? = null): Throwable(m)

fun check(e: Throwable, expectedString: String) {
    try {
        throw e
    }
    catch (e: Throwable) {
        assertEquals(expectedString, e.toString())
    }
}

fun box(): String {
    check(Throwable(), "Throwable: null")
    check(Throwable("ccc"), "Throwable: ccc")
    check(Exception(), "Exception: null")
    check(Exception("bbb"), "Exception: bbb")
    check(MyException(), "MyException: null")
    check(MyException("aaa"), "MyException: aaa")
    check(MyException2(), "MyException2: null")
    check(MyException2("aaa"), "MyException2: aaa")

    return "OK"
}
