// KIND: STANDALONE
// MODULE: simple
// FILE: simple.kt

fun foo(): IntRange = 2 ..< 6

fun bar(): ClosedRange<Int> {
    return 2 .. 3
}

fun baz(): OpenEndRange<Long> {
    return 5L ..< 10L
}

fun accept(range: IntRange): LongRange = range.start.toLong() .. range.endInclusive.toLong()

fun acceptClosed(range: ClosedRange<Int>): OpenEndRange<Int> = range.start ..< range.endInclusive

fun unsupported(): ClosedRange<String> = "alpha" .. "omega"

// MODULE: another
// FILE: another.kt

package some

fun foo(): IntRange = 2 ..< 6
