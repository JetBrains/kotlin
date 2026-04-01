// DUMP_KT_IR

// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

import kotlin.powerassert.*

@PowerAssert
expect fun explain(value: Any): String?

fun test1(): String {
    val reallyLongList = listOf("a", "b")
    return explain(reallyLongList.reversed() == emptyList<String>()) ?: "FAIL"
}

// MODULE: platform()()(common)
// FILE: platform.kt

import kotlin.powerassert.*

// TODO(KT-85237): Should report missing annotation here.
actual fun explain(value: Any): String? {
    return "OK"
}

fun box(): String = runAllOutput(
    "test1" to ::test1,
    "test2" to ::test2,
)

fun test2(): String {
    val reallyLongList = listOf("a", "b")
    return explain(reallyLongList.reversed() == emptyList<String>()) ?: "FAIL"
}
