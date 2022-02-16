// V8 fail: https://bugs.chromium.org/p/v8/issues/detail?id=12834
// IGNORE_BACKEND: WASM

// EXPECTED_REACHABLE_NODES: 1283
package foo

external class A(c: Int) {
    val c: Int

    companion object {
        val g: Int
        val c: String = definedExternally
    }
}

fun box(): String {
    if (A.g != 3) return "fail1"
    if (A.c != "hoooray") return "fail2"
    if (A(2).c != 2) return "fail3"

    return "OK"
}
