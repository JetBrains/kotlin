package komem.litmus

import org.jetbrains.litmuskt.LitmusOutcomeStats
import org.jetbrains.litmuskt.LitmusOutcomeType
import org.jetbrains.litmuskt.mergeResults
import kotlin.test.Test
import kotlin.test.assertEquals

class LitmusOutcomeTest {
    @Test
    fun mergeStatsTest() {
        val stats = listOf(
            listOf(
                LitmusOutcomeStats(1, 10, LitmusOutcomeType.ACCEPTED),
                LitmusOutcomeStats(0, 10, LitmusOutcomeType.ACCEPTED),
            ),
            listOf(
                LitmusOutcomeStats(1, 20, LitmusOutcomeType.ACCEPTED),
                LitmusOutcomeStats(0, 10, LitmusOutcomeType.ACCEPTED),
            ),
            listOf(
                LitmusOutcomeStats(0, 10, LitmusOutcomeType.ACCEPTED),
                LitmusOutcomeStats(2, 1, LitmusOutcomeType.INTERESTING),
            )
        )
        assertEquals(
            setOf(
                LitmusOutcomeStats(1, 30, LitmusOutcomeType.ACCEPTED),
                LitmusOutcomeStats(0, 30, LitmusOutcomeType.ACCEPTED),
                LitmusOutcomeStats(2, 1, LitmusOutcomeType.INTERESTING),
            ),
            stats.mergeResults().toSet()
        )
    }
}



