// EXPECTED_REACHABLE_NODES: 1287
interface I

class MyException: Exception(), I

fun box(): String {
    var e: Any = MyException()
    if (e !is I) return "fail1"

    e = Exception()
    if (e is I) return "fail2"

    return "OK"
}