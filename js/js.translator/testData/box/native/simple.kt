// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1109
package foo

external fun returnFalse(): Boolean = definedExternally

fun box() = if (!returnFalse()) "OK" else "fail"
