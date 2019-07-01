// IGNORE_BACKEND: JS
// EXPECTED_REACHABLE_NODES: 1298
package foo

fun check(e: Throwable, expectedString: String) {
    try {
        throw e
    }
    catch (e: Throwable) {
        assertEquals(expectedString, e.toString())
    }
}

var storage = ""

fun <T> sideEffect(v: String, m: T?): T? {
    storage += v
    return m
}

class MyException1(i1: String, i2: String, m: String? = null, t: Throwable? = null): Throwable(sideEffect(i2, sideEffect(i1, m)), t)
class MyException2(i1: String, i2: String, m: String? = null, t: Throwable? = null): Throwable(sideEffect(i1, m), sideEffect(i2, t))


fun box(): String {
    check(MyException1("1", "2"), "MyException1: null")
    check(MyException1("3", "4", "aaa"), "MyException1: aaa")
    check(MyException1("5", "6", t = Throwable("bbb")), "MyException1: Throwable: bbb")
    check(MyException2("7", "8"), "MyException2: null")
    check(MyException2("9", "0", "ccc"), "MyException2: ccc")
    check(MyException2("A", "B", t = Throwable("ddd")), "MyException2: Throwable: ddd")

    if (storage != "1234567890AB") return "FAIL $storage"

    return "OK"
}
