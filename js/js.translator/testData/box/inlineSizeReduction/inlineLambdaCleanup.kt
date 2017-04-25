// EXPECTED_REACHABLE_NODES: 488
// CHECK_NOT_CALLED: produceOK except=box

fun produceOK() = "OK"

private inline fun <T> block(f: () -> T) = f()

fun box(): String = block { produceOK() }