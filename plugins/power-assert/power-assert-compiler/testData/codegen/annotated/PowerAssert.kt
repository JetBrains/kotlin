// DUMP_KT_IR

import kotlinx.powerassert.*

fun box(): String {
    val reallyLongList = listOf("a", "b")
    return explain(reallyLongList.reversed() == emptyList<String>()) ?: "FAIL"
}

@PowerAssert
fun explain(value: Any): String? {
    return PowerAssert.explanation?.toDefaultMessage()
}
