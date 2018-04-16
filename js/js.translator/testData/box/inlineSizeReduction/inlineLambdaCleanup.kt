// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1109
// CHECK_NOT_CALLED: produceOK except=box

fun produceOK() = "OK"

private inline fun <T> block(f: () -> T) = f()

fun box(): String = block { produceOK() }