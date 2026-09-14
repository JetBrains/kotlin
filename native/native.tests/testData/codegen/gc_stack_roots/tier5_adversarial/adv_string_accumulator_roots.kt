// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
package gc_stack_roots.tier5_adversarial

private fun concatAll(parts: List<String>): String {
    var string: String = ""
    for (it in parts) string += it
    return string
}

fun box(): String {
    val parts = (0 until 10000).map { "value_$it" }
    val expected = parts.sumOf { it.length }
    repeat(5) { round ->
        val result = concatAll(parts)
        if (result.length != expected) return "FAIL: round $round, length ${result.length} != $expected"
        if (!result.startsWith("value_0value_1") || !result.endsWith("value_9999")) return "FAIL: round $round, wrong contents"
    }
    return "OK"
}
