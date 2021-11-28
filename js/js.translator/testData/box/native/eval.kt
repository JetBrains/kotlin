// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: UNSUPPORTED_JS_INTEROP
// SKIP_MINIFICATION

val top = "TOP LEVEL"

fun box(): String {
    // Does't work in Rhino, but should.
    // val v = 1
    // assertEquals(3, eval("v + 2"))

    assertEquals(5, eval("3 + 2"))

    if (testUtils.isLegacyBackend()) {
        val PACKAGE = "main"
        assertEquals(top, eval("$PACKAGE.top"))
    }

    return "OK"
}