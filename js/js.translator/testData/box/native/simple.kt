// EXPECTED_REACHABLE_NODES: 991
package foo

external fun returnFalse(): Boolean = definedExternally

fun box() = if (!returnFalse()) "OK" else "fail"
