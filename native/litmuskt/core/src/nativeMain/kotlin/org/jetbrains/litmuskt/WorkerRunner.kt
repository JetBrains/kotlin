package org.jetbrains.litmuskt

class WorkerRunner : ThreadlikeRunner() {
    override fun threadlikeProducer(): Threadlike = WorkerThreadlike()
}
