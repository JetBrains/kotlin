package org.jetbrains.litmuskt

import kaffinity.*
import org.jetbrains.litmuskt.AffinityManager
import org.jetbrains.litmuskt.syscallCheck
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.cpu_set_t
import platform.posix.pthread_t
import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.Worker

@OptIn(ExperimentalStdlibApi::class, ObsoleteWorkersApi::class)
actual fun getAffinityManager(): AffinityManager? = object : AffinityManager {
    override fun setAffinity(w: Worker, cpus: Set<Int>) {
        setAffinity(w.platformThreadId, cpus)
    }

    override fun getAffinity(w: Worker): Set<Int> {
        return getAffinity(w.platformThreadId)
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun setAffinity(thread: pthread_t, cpus: Set<Int>): Unit = memScoped {
        require(cpus.isNotEmpty())
        val set = alloc<cpu_set_t>()
        cpu_zero(set.ptr)
        for (cpu in cpus) cpu_set(cpu, set.ptr)
        set_affinity(thread, set.ptr).syscallCheck()
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun getAffinity(thread: pthread_t): Set<Int> = memScoped {
        val set = alloc<cpu_set_t>()
        get_affinity(thread, set.ptr).syscallCheck()
        return (0..<cpu_setsize())
            .filter { cpu_isset(it, set.ptr) != 0 }
            .toSet()
    }
}
