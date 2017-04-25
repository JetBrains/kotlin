// EXPECTED_REACHABLE_NODES: 501
package foo

open class Ex: Exception()

class Ex1: Ex()

fun box(): String {

    var s: String = ""

    try {
        throw Ex1()
    }
    catch (e: Ex) {
        s = "Ex"
    }
    catch (e: Ex1) {
        throw Exception("Control flow cannot get here")
    }

    assertEquals("Ex", s)

    return "OK"
}