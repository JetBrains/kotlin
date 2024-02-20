@file:OptIn(kotlin.native.concurrent.ObsoleteWorkersApi::class)

package komem.litmus

// TODO: add documentation

import platform.posix.pthread_t
import kotlin.native.concurrent.Worker
import kotlin.random.Random

interface AffinityManager {
    fun setAffinity(w: Worker, cpus: Set<Int>)
    fun getAffinity(w: Worker): Set<Int>

    fun setAffinity(thread: pthread_t, cpus: Set<Int>)
    fun getAffinity(thread: pthread_t): Set<Int>

    fun newShiftMap(shift: Int): AffinityMap = object : AffinityMap {
        private val cpus: List<Set<Int>>

        init {
            val tmp = MutableList(cpuCount()) { setOf<Int>() }
            var i = 0
            repeat(tmp.size) {
                tmp[i] = setOf(i)
                i = (i + shift) % tmp.size
                if (tmp[i].isNotEmpty()) i++
            }
            cpus = tmp
        }

        override fun allowedCores(threadIndex: Int) = cpus[threadIndex]
    }

    fun newRandomMap(random: Random = Random): AffinityMap = object : AffinityMap {
        private val cpus = (0..<cpuCount()).shuffled(random).map { setOf(it) }
        override fun allowedCores(threadIndex: Int) = cpus[threadIndex]
    }

    fun presetShort(): List<AffinityMap> = listOf(
        newShiftMap(1),
        newShiftMap(2),
        newShiftMap(4),
    )

    fun presetLong(): List<AffinityMap> = List(cpuCount()) { newShiftMap(it) } + listOf(
        object : AffinityMap {
            override fun allowedCores(threadIndex: Int) = setOf(0, 1)
        },
        object : AffinityMap {
            override fun allowedCores(threadIndex: Int) = setOf(1, 2)
        },
        object : AffinityMap {
            override fun allowedCores(threadIndex: Int) = setOf(
                (threadIndex * 2) % cpuCount(),
                (threadIndex * 2 + 1) % cpuCount()
            )
        }
    )
}

expect fun getAffinityManager(): AffinityManager?