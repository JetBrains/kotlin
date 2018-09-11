// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1282
fun box(): String {
    val r1 = trueFun() > falseFun()
    if (!r1) return "fail1"

    if (falseFun() > trueFun()) return "fail2"

    if (trueFun() > trueFun()) return "fail3"

    if (trueFun() < falseFun()) return "fail4"

    val x: Comparable<Boolean> = trueFun()
    val y: Comparable<Boolean> = falseFun()

    if (x.compareTo(false) <= 0) return "fail5"
    if (y.compareTo(true) >= 0) return "fail6"
    if (y.compareTo(false) != 0) return "fail7"

    if ((true).compareTo(false) <= 0) return "fail8"

    return "OK"
}

fun trueFun() = true

fun falseFun() = false