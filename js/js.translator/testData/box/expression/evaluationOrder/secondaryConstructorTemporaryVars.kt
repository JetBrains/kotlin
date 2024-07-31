// EXPECTED_REACHABLE_NODES: 1285
// HAS_NO_CAPTURED_VARS: function=A_init_$Init$ except=A;equals TARGET_BACKENDS=JS_IR
// HAS_NO_CAPTURED_VARS: function=new_A_z6ztw9_k$ except=equals TARGET_BACKENDS=JS_IR_ES6

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