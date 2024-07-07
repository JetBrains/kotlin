package org.jetbrains.litmuskt

import kotlin.random.Random

/**
 * An interface for managing the CPU affinity of a [Threadlike] object.
 */
interface AffinityManager {
    /**
     * Binds a [Threadlike] to a certain set of CPU cores.
     *
     * @return `true` on success, `false` if setting affinity for [threadlike] is not supported
     * @throws IllegalStateException if setting affinity is supported, but failed
     */
    fun setAffinity(threadlike: Threadlike, cpus: Set<Int>): Boolean

    /**
     * Gets the CPU cores this [threadlike] is bound to.
     *
     * @return the set of CPU cores on success, `null` if getting affinity for [threadlike] is not supported
     * @throws IllegalStateException if getting affinity is supported, but failed
     */
    fun getAffinity(threadlike: Threadlike): Set<Int>?
}

/**
 * Get the [AffinityManager] or null if it is not supported on this platform.
 */
expect val affinityManager: AffinityManager?

/**
 * Sets [threadlike]'s affinity to [cpus] and ensures that it worked.
 *
 * @return `true` on success, `false` if setting affinity for [threadlike] is not supported.
 * @throws IllegalStateException if setting affinity is supported, but failed
 */
fun AffinityManager.setAffinityAndCheck(threadlike: Threadlike, cpus: Set<Int>): Boolean {
    val set = setAffinity(threadlike, cpus)
    if (set) {
        getAffinity(threadlike)?.let { result ->
            if (result == cpus) return true
            error("affinity failed to set: expected $cpus, got $result")
        }
    }
    return false
}

/**
 * When there are multiple threads, this interface manages which threads go to which CPU cores, so that
 * we can achieve some "interesting" arrangements and thread interactions. Each thread should be assigned
 * its own index, and with that index [allowedCores] provides the cores this thread should run on.
 *
 * We can then bind the thread to these cores using the [AffinityManager] interface.
 */
fun interface AffinityMap {
    fun allowedCores(threadIndex: Int): Set<Int>
}

/**
 * Creates a map where each thread is assigned to one core, and each core
 * is shifted by [shift] from the previous one. If cores overlap, the next free one is used.
 *
 * Example: with [shift]=2 and 6 cores in total the resulting map is:
 *
 * `[ {0}, {2}, {4}, {1}, {3}, {5} ]`
 */
fun AffinityManager.newShiftMap(shift: Int): AffinityMap = object : AffinityMap {
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

/**
 * Create a map where each thread is assigned to one random core, without collisions.
 */
fun AffinityManager.newRandomMap(random: Random = Random): AffinityMap = object : AffinityMap {
    private val cpus = (0..<cpuCount()).shuffled(random).map { setOf(it) }
    override fun allowedCores(threadIndex: Int) = cpus[threadIndex]
}

/**
 * A short list of some "reasonable" maps, where "reasonable" means they check some "interesting"
 * arrangements of threads.
 *
 * Intended for using with test params variation.
 */
fun AffinityManager.presetShort(): List<AffinityMap> = listOf(
    newShiftMap(1),
    newShiftMap(2),
    newShiftMap(4),
)

/**
 * A longer list of some "reasonable" maps. Again, intended for using with test params variation.
 */
fun AffinityManager.presetLong(): List<AffinityMap> = List(cpuCount()) { newShiftMap(it) } + listOf(
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
