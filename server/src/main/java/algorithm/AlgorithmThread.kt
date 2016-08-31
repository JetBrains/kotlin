package algorithm

import kotlin.concurrent.thread

class AlgorithmThread(val algorithmImpl: AbstractAlgorithm) : Thread() {

    private var count = 0

    override fun run() {
        while (true) {//todo while !alrightmImpl.isDone(), but need check it
            try {
                if (count > 0) {
                    algorithmImpl.iterate()
                    count--
                } else {
                    Thread.sleep(1000)
                }
            } catch (e: InterruptedException) {

            }
        }
    }

    fun setCount(count: Int) {
        this.count = count
    }

    fun cancel() {
        count = 0
    }
}