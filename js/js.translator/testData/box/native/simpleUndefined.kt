// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1109
package foo

external val c: Any? = definedExternally

fun box(): String {
    if (c != null) return "fail1"
    return if (c == null) "OK" else "fail2"
}
