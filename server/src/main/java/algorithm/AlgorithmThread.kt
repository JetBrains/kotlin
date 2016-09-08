package algorithm

class AlgorithmThread() : Thread() {

    var algorithmImpl: AbstractAlgorithm? = null
    private var count = 0

    override fun run() {
        val currentAlgorithm = algorithmImpl ?: return
        while (!currentAlgorithm.isCompleted()) {
            try {
                if (count > 0) {
                    currentAlgorithm.iterate()
                    count--
                } else {
                    Thread.sleep(1000)
                }
            } catch (e: InterruptedException) {
                println("algorithm thread is interrupted")
                break
            }
        }
        println("algorithm is completed")
    }

    fun setCount(count: Int) {
        this.count = count
    }
}
