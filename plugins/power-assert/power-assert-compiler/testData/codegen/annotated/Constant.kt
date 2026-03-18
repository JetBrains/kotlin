// DUMP_KT_IR

import kotlinx.powerassert.*

fun box(): String {
    return explain("OK") ?: "FAIL"
}

@PowerAssert
fun explain(value: Any): String? {
    return PowerAssert.explanation?.toDefaultMessage()
}
