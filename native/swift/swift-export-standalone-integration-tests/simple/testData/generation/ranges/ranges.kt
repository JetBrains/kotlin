fun foo(): IntRange = 2 ..< 6

fun bar(): ClosedRange<Int> {
    return 2 .. 3
}

fun baz(): OpenEndRange<Long> {
    return 5L ..< 10L
}

fun mapper(arg: List<LongRange>): List<IntRange> =
    arg.map { it.start.toInt() .. it.endInclusive.toInt() }

fun total(list: List<IntRange>): IntRange = list.map { it.start }.min() .. list.map { it.endInclusive }.max()

fun accept(range: IntRange): LongRange = range.start.toLong() .. range.endInclusive.toLong()

fun acceptNullable(range: IntRange?): IntRange? =
    if (range == null || range.isEmpty()) null else range

fun acceptClosed(range: ClosedRange<Int>): OpenEndRange<Int> = range.start ..< range.endInclusive

fun unsupported(): ClosedRange<String> = "alpha" .. "omega"
