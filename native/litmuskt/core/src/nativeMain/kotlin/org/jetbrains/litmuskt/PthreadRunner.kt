package org.jetbrains.litmuskt

class PthreadRunner : ThreadlikeRunner() {
    override fun threadlikeProducer(): Threadlike = PthreadThreadlike()
}
