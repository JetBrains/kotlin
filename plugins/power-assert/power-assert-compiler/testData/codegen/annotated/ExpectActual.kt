// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

import kotlin.powerassert.*

@PowerAssert
expect fun explain(value: Any): String?

interface Explainer {
    @PowerAssert
    fun explain(value: Any): String?
}

expect class ExplainerImpl : Explainer {
    override fun explain(value: Any): String?
}

// MODULE: platform()()(common)
// FILE: platform.kt

import kotlin.powerassert.*

@PowerAssert
actual fun explain(value: Any): String? {
    return PowerAssert.explanation?.toDefaultMessage()
}

@PowerAssert.Ignore
actual class ExplainerImpl : Explainer {
    actual override fun explain(value: Any): String? {
        return PowerAssert.explanation?.toDefaultMessage()
    }
}

fun box(): String = runAllOutput(
    "test1" to ::test1,
    "test2" to ::test2,
)

fun test1(): String {
    val reallyLongList = listOf("a", "b")
    return explain(reallyLongList.reversed() == emptyList<String>()) ?: "FAIL"
}

fun test2(): String {
    val explainer = ExplainerImpl()
    val reallyLongList = listOf("a", "b")
    return explainer.explain(reallyLongList.reversed() == emptyList<String>()) ?: "FAIL"
}
