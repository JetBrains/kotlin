package komem.litmus

fun interface AffinityMap {
    fun allowedCores(threadIndex: Int): Set<Int>
}
