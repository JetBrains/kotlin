// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Ranges
// FILE: Ranges.kt

fun distance(some: IntRange): Int = some.endInclusive - some.start

fun materialize(): IntRange = 4 .. 11

fun simple(some: IntRange): IntRange = some.start - 1 .. some.endInclusive + 1

fun nullable(some: IntRange?): IntRange? = some?.let { simple(it) }

//fun total(list: List<IntRange>): IntRange = list.map { it.start }.min() .. list.map { it.endInclusive }.max()
