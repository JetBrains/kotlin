package org.jetbrains.litmuskt

import kaffinity.*
import kotlinx.cinterop.*
import platform.posix.cpu_set_t
import platform.posix.pthread_t
import kotlin.native.concurrent.ObsoleteWorkersApi

@OptIn(ExperimentalStdlibApi::class, ObsoleteWorkersApi::class, ExperimentalForeignApi::class)
actual val affinityManager: AffinityManager? = object : AffinityManager {

    override fun setAffinity(threadlike: Threadlike, cpus: Set<Int>): Boolean {
        when (threadlike) {
            is WorkerThreadlike -> memScoped {
                val pthreadPtr = alloc<pthread_t>(threadlike.worker.platformThreadId).ptr
                setPthreadAffinity(pthreadPtr, cpus)
            }
            is PthreadThreadlike -> setPthreadAffinity(threadlike.pthreadPtr, cpus)
            else -> return false
        }
        return true
    }

    override fun getAffinity(threadlike: Threadlike): Set<Int>? = when (threadlike) {
        is WorkerThreadlike -> memScoped {
            val pthreadPtr = alloc<pthread_t>(threadlike.worker.platformThreadId).ptr
            getPthreadAffinity(pthreadPtr)
        }
        is PthreadThreadlike -> getPthreadAffinity(threadlike.pthreadPtr)
        else -> null
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun setPthreadAffinity(threadPtr: CPointer<*>, cpus: Set<Int>): Unit = memScoped {
        require(cpus.isNotEmpty())
        val set = alloc<cpu_set_t>()
        cpu_zero(set.ptr)
        for (cpu in cpus) cpu_set(cpu, set.ptr)
        set_affinity(threadPtr, set.ptr).syscallCheck()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun getPthreadAffinity(threadPtr: CPointer<*>): Set<Int> = memScoped {
        val set = alloc<cpu_set_t>()
        get_affinity(threadPtr, set.ptr).syscallCheck()
        return (0..<cpu_setsize())
            .filter { cpu_isset(it, set.ptr) != 0 }
            .toSet()
    }
}
