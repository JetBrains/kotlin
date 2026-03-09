// RENDER_IR_DIAGNOSTICS_FULL_TEXT

// MODULE: lib
// DISABLE_PLUGIN
// FILE: A.kt

import kotlinx.powerassert.*

@PowerAssert
fun describe(value: Any): String? {
    return PowerAssert.explanation?.toDefaultMessage()
}

// MODULE: main(lib)
// FILE: B.kt

fun box(): String {
    try {
        describe("")
        return "FAIL"
    } catch (_: NotImplementedError) {
        return "OK"
    }
}
