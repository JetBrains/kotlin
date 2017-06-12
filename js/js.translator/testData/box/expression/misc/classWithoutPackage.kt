// EXPECTED_REACHABLE_NODES: 489
class A(var a: Int) {
    init {
        a = 3
    }
}

fun box(): String {
    val result = A(1).a
    if (result != 3) return "fail: $result"
    return "OK"
}
