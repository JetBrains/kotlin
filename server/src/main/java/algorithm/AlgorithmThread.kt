package algorithm

class AlgorithmThread(val algorithmImpl: AbstractAlgorithm) : Thread() {

    private var count = 0

    override fun run() {
        while (!algorithmImpl.isCompleted()) {
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
        println("algorithm is finished!")
    }

    fun setCount(count: Int) {
        this.count = count
    }

    fun cancel() {
        count = 0
    }
}