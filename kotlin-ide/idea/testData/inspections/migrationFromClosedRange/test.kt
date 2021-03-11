package fromClosedRange

import kotlin.ranges.IntProgression.Companion.fromClosedRange

fun testInt(rangeStart: Int, rangeEnd: Int, step: Int) {
    fromClosedRange(12, 143, step) // report
    fromClosedRange(12, 143, Int.MIN_VALUE) // report

    fromClosedRange(step = 12, rangeEnd = rangeEnd, rangeStart = rangeStart) // do not report
    fromClosedRange(12, 143, 2) // do not report
}

fun testLong(rangeStart: Long, rangeEnd: Long, step: Long) {
    LongProgression.fromClosedRange(12, 143, step) // report
    LongProgression.fromClosedRange(12, 143, Long.MIN_VALUE) // report

    LongProgression.fromClosedRange(12, 143, 2.toLong()) // do no report
    LongProgression.fromClosedRange(step = 12, rangeEnd = rangeEnd, rangeStart = rangeStart) // do not report
    LongProgression.fromClosedRange(12, 143, 2) // do not report
    LongProgression.fromClosedRange(12, 143, 2L) // do not report
    LongProgression.fromClosedRange(12, 143, Int.MIN_VALUE) // do not report
}