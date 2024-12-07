/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NO_EXPLICIT_VISIBILITY_IN_API_MODE") // Scheduled for eventual removal

package kotlin.native.internal.gc

/**
 * This class represents statistics of memory usage in one memory pool.
 *
 * @property objectsCount The number of allocated objects.
 * @property totalObjectsSizeBytes The total size of allocated objects. System allocator overhead is not included,
 *                                 so it can not perfectly match the value received by os tools.
 *                                 All alignment and auxiliary object headers are included.
 */
@Deprecated("Use kotlin.native.runtime.MemoryUsage instead.", ReplaceWith("MemoryUsage", "kotlin.native.runtime.MemoryUsage"))
@DeprecatedSinceKotlin(warningSince = "1.9", errorSince = "2.1")
@ExperimentalStdlibApi
public class MemoryUsage(
        val objectsCount: Long,
        val totalObjectsSizeBytes: Long,
)

/**
 * This class represents statistics of the root set for garbage collector run, separated by root set pools.
 * These nodes are assumed to be used, even if there are no references for them.
 *
 * @property threadLocalReferences The number of objects in global variables with `@ThreadLocal` annotation.
 *                                 Object is counted once per each thread it was initialized in.
 * @property stackReferences The number of objects referenced from the stack of any thread.
 *                           These are function local variables and different temporary values, as function call arguments and
 *                           return values. They would be automatically removed from the root set when a corresponding function
 *                           call is finished.
 * @property globalReferences The number of objects in global variables. The object is counted only if the variable is initialized.
 * @property stableReferences The number of objects referenced by [kotlinx.cinterop.StableRef]. It includes both explicit usage
 *                            of this API, and internal usages, e.g. inside interop and Worker API.
 */
@ExperimentalStdlibApi
@Deprecated("Use kotlin.native.runtime.RootSetStatistics instead.", ReplaceWith("RootSetStatistics", "kotlin.native.runtime.RootSetStatistics"))
@DeprecatedSinceKotlin(warningSince = "1.9", errorSince = "2.1")
public class RootSetStatistics(
        val threadLocalReferences: Long,
        val stackReferences: Long,
        val globalReferences: Long,
        val stableReferences: Long
)

/**
 * This class represents statistics about the single run of the garbage collector.
 * It is supposed to be used for testing and debugging purposes only.
 *
 * @property epoch ID of garbage collector run.
 * @property startTimeNs Time, when garbage collector run is started, meausered by [kotlin.system.getTimeNanos].
 * @property endTimeNs Time, when garbage collector run is ended, measured by [kotlin.system.getTimeNanos].
 *                     After this point, most of the memory is reclaimed, and a new garbage collector run can start.
 * @property firstPauseRequestTimeNs Time, when the garbage collector thread requested suspension of mutator threads for the first time,
 *                                   mesured by [kotlin.system.getTimeNanos].
 * @property firstPauseStartTimeNs Time, when mutator threads are suspended for the first time, mesured by [kotlin.system.getTimeNanos].
 * @property firstPauseEndTimeNs Time, when mutator threads are unsuspended for the first time, mesured by [kotlin.system.getTimeNanos].
 * @property secondPauseRequestTimeNs Time, when the garbage collector thread requested suspension of mutator threads for the second time,
 *                                    mesured by [kotlin.system.getTimeNanos].
 * @property secondPauseStartTimeNs Time, when mutator threads are suspended for the second time, mesured by [kotlin.system.getTimeNanos].
 * @property secondPauseEndTimeNs Time, when mutator threads are unsuspended for the second time, mesured by [kotlin.system.getTimeNanos].
 * @property postGcCleanupTimeNs Time, when all memory is reclaimed, measured by [kotlin.system.getTimeNanos].
 *                               If null, memory reclamation is still in progress.
 * @property rootSet The number of objects in each root set pool. Check [RootSetStatistics] doc for details.
 * @property memoryUsageAfter Memory usage at the start of garbage collector run, separated by memory pools.
 *                            The set of memory pools depends on the collector implementation.
 *                            Can be empty, of colelction is in progress.
 * @property memoryUsageBefore Memory usage at the end of garbage collector run, separated by memory pools.
 *                            The set of memory pools depends on the collector implementation.
 *                            Can be empty, of colelction is in progress.
 */
@ExperimentalStdlibApi
@Deprecated("Use kotlin.native.runtime.GCInfo instead.", ReplaceWith("GCInfo", "kotlin.native.runtime.GCInfo"))
@DeprecatedSinceKotlin(warningSince = "1.9", errorSince = "2.1")
@OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
@Suppress("DEPRECATION_ERROR")
public class GCInfo(
        val epoch: Long,
        val startTimeNs: Long,
        val endTimeNs: Long,
        val firstPauseRequestTimeNs: Long,
        val firstPauseStartTimeNs: Long,
        val firstPauseEndTimeNs: Long,
        val secondPauseRequestTimeNs: Long?,
        val secondPauseStartTimeNs: Long?,
        val secondPauseEndTimeNs: Long?,
        val postGcCleanupTimeNs: Long?,
        val rootSet: RootSetStatistics,
        val memoryUsageBefore: Map<String, MemoryUsage>,
        val memoryUsageAfter: Map<String, MemoryUsage>,
) {
    internal companion object {
        val lastGCInfo: GCInfo?
            get() {
                val info = kotlin.native.runtime.GCInfo.lastGCInfo ?: return null
                return GCInfo(
                        info.epoch,
                        info.startTimeNs,
                        info.endTimeNs,
                        info.firstPauseRequestTimeNs,
                        info.firstPauseStartTimeNs,
                        info.firstPauseEndTimeNs,
                        info.secondPauseRequestTimeNs,
                        info.secondPauseStartTimeNs,
                        info.secondPauseEndTimeNs,
                        info.postGcCleanupTimeNs,
                        info.rootSet.let {
                            RootSetStatistics(
                                    it.threadLocalReferences,
                                    it.stackReferences,
                                    it.globalReferences,
                                    it.stableReferences
                            )
                        },
                        info.memoryUsageBefore.mapValues { (_, v) ->
                            MemoryUsage(
                                    0L,
                                    v.totalObjectsSizeBytes,
                            )
                        },
                        info.memoryUsageAfter.mapValues { (_, v) ->
                            MemoryUsage(
                                    0L,
                                    v.totalObjectsSizeBytes,
                            )
                        }
                )
            }
    }
}
