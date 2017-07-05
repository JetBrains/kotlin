// EXPECTED_REACHABLE_NODES: 1375
package foo

external fun returnFalse(): Boolean = definedExternally

fun box() = if (!returnFalse()) "OK" else "fail"
