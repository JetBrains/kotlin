package org.jetbrains.litmuskt

fun interface AffinityMap {
    fun allowedCores(threadIndex: Int): Set<Int>
}
