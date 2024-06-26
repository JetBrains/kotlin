package org.jetbrains.litmuskt.barriers

import org.jetbrains.litmuskt.Barrier
import java.util.concurrent.atomic.AtomicInteger

class JvmSpinBarrier(private val threadCount: Int) : Barrier {
    private val waitingCount = AtomicInteger(0)
    private val passedBarriersCount = AtomicInteger(0)

    override fun await() {
        val oldPassed = passedBarriersCount.get()
        if (waitingCount.addAndGet(1) == threadCount) {
            waitingCount.set(0)
            passedBarriersCount.incrementAndGet()
        } else {
            while (passedBarriersCount.get() == oldPassed) passedBarriersCount.get() // spin
        }
    }
}
