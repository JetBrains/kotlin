// EXPECTED_REACHABLE_NODES: 495
var log = ""

fun foo() {
    fun local(x: String) {
        if (x.isEmpty()) return
        log += "foo(${x[0]});"
        local(x.substring(1))
    }
    local("OK")
}

fun bar() {
    fun local(x: String) {
        if (x.isEmpty()) return
        log += "bar(${x[0]});"
        local(x.substring(1))
    }
    local("OK")
}

fun baz(): Int {
    fun local(x: String) {
        if (x.isEmpty()) return
        log += "baz(${x[0]});"
        local(x.substring(1))
    }
    fun local(x: Int): Int = if (x == 0) 1 else x * local(x - 1)
    local("OK")
    return local(3)
}


fun box(): String {
    foo()
    bar()
    val result = baz()

    if (result != 6) return "fail1: $result"

    if (log != "foo(O);foo(K);bar(O);bar(K);baz(O);baz(K);") return "fail2: $result"

    return "OK"
}