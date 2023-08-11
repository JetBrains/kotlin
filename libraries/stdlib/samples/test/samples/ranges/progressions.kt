package samples.ranges

import samples.*
import java.sql.Date
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class Progressions {
    @Sample
    fun fromClosedRangeChar() {
        val progression = CharProgression.fromClosedRange('a', 'e', 2)
        val charsInProgression = progression.toList().toString()
        assertPrints(charsInProgression, "[a, c, e]")

        assertEquals(listOf('a', 'c'), progression.take(2).toList())
        assertTrue(progression.any { it == 'c' })
        assertFalse(progression.all { it == 'a' })
    }

    @Sample
    fun fromClosedRangeInt() {
        val progression = IntProgression.fromClosedRange(1, 5, 2)
        val intsInProgression = progression.toList().toString()
        assertPrints(intsInProgression, "[1, 3, 5]")

        assertEquals(listOf(1, 3), progression.take(2).toList())
        assertTrue(progression.any { it == 3 })
        assertFalse(progression.all { it == 1 })
    }

    @Sample
    fun fromClosedRangeLong() {
        val progression = LongProgression.fromClosedRange(1L, 5L, 2)
        val longsInProgression = progression.toList().toString()
        assertPrints(longsInProgression, "[1, 3, 5]")
        assertEquals(listOf(1L, 3L), progression.take(2).toList())
        assertTrue(progression.any { it == 3L })
        assertFalse(progression.all { it == 1L })
    }
}
