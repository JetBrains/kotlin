// DUMP_KT_IR

import kotlin.explain.*

fun box(): String {
    val reallyLongList = listOf("a", "b")
    return explain(reallyLongList.reversed() == emptyList<String>()) ?: "FAIL"
}

@ExplainCall
fun explain(value: Any): String? {
    return ExplainCall.explanation?.toDefaultMessage()
}
