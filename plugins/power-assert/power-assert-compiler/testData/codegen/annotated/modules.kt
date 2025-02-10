// IGNORE_BACKEND_K1: ANY
// DUMP_KT_IR

// MODULE: lib
// FILE: A.kt

import kotlin.explain.*

@ExplainCall
fun describe(value: Any): String? {
    return ExplainCall.explanation?.toDefaultMessage()
}

// MODULE: main(lib)
// FILE: B.kt

fun box(): String {
    val reallyLongList = listOf("a", "b")
    return describe(reallyLongList.reversed() == emptyList<String>()) ?: "FAIL"
}
