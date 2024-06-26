package org.jetbrains.litmuskt.barriers

import org.jetbrains.litmuskt.Barrier
import kotlin.concurrent.AtomicInt

class KNativeSpinBarrier(private val threadCount: Int) : Barrier {
    private val waitingCount = AtomicInt(0)
    private val passedBarriersCount = AtomicInt(0)

    override fun await() {
        val oldPassed = passedBarriersCount.value
        if (waitingCount.addAndGet(1) == threadCount) {
            waitingCount.value = 0
            passedBarriersCount.incrementAndGet()
        } else {
            while (passedBarriersCount.value == oldPassed) passedBarriersCount.value // spin
        }
    }
}
