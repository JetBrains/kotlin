// EXPECTED_REACHABLE_NODES: 493
// HAS_NO_CAPTURED_VARS: function=A_init except=Kotlin;A

class A() {
    var y: String? = null
    var z: Any? = null

    constructor(x: Any) : this() {
        y = if (x == "foo") "!!!" else { z = x; ">>>" }
    }
}

fun box(): String {
    val a = A("foo")
    if (a.y != "!!!") return "fail1: ${a.y}"
    if (a.z != null) return "fail2: ${a.z}"

    val b = A(23)
    if (b.y != ">>>") return "fail3: ${b.y}"
    if (b.z != 23) return "fail4: ${b.z}"

    return "OK"
}