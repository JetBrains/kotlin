package komem.litmus

interface Barrier {
    fun await()
}

typealias BarrierProducer = (Int) -> Barrier
