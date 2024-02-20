package komem.litmus.barriers

import barrier.CSpinBarrier
import barrier.barrier_wait
import barrier.create_barrier
import komem.litmus.Barrier
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
class CinteropSpinBarrier(threadCount: Int) : Barrier {

    private val barrierStruct: CPointer<CSpinBarrier>? = create_barrier(threadCount)

    override fun await() {
        barrier_wait(barrierStruct)
    }
}
