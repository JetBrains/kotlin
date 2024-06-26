package org.jetbrains.litmuskt

interface Barrier {
    fun await()
}

typealias BarrierProducer = (Int) -> Barrier
