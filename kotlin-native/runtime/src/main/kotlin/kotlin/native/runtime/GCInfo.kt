/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class)
package kotlin.native.runtime

import kotlin.native.internal.*
import kotlin.native.internal.NativePtr
import kotlin.native.concurrent.*
import kotlin.time.*
import kotlin.time.Duration.Companion.nanoseconds
import kotlinx.cinterop.*
import kotlin.system.*

/**
 * This class represents statistics of memory usage in one memory pool.
 *
 * @property totalObjectsSizeBytes The total size of allocated objects. System allocator overhead is not included,
 *                                 so it can not perfectly match the value received by os tools.
 *                                 All alignment and auxiliary object headers are included.
 */
@NativeRuntimeApi
@SinceKotlin("1.9")
public class MemoryUsage(
        val totalObjectsSizeBytes: Long,
)

/**
 * This class represents statistics of sweeping in one memory pool.
 *
 * @property sweptCount The of objects that were freed.
 * @property keptCount The number of objects that were processed but kept alive.
 */
@NativeRuntimeApi
@SinceKotlin("1.9")
public class SweepStatistics(
    val sweptCount: Long,
    val keptCount: Long,
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
@NativeRuntimeApi
@SinceKotlin("1.9")
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
 *                                If null, memory reclamation is still in progress.
 * @property rootSet The number of objects in each root set pool. Check [RootSetStatistics] doc for details.
 * @property markedCount How many objects were processed during marking phase.
 * @property sweepStatistics Sweeping statistics separated by memory pools.
 *                           The set of memory pools depends on the collector implementation.
 *                           Can be empty, if collection is in progress.
 * @property memoryUsageAfter Memory usage at the start of garbage collector run, separated by memory pools.
 *                            The set of memory pools depends on the collector implementation.
 *                            Can be empty, if collection is in progress.
 * @property memoryUsageBefore Memory usage at the end of garbage collector run, separated by memory pools.
 *                            The set of memory pools depends on the collector implementation.
 *                            Can be empty, if collection is in progress.
 */
@NativeRuntimeApi
@SinceKotlin("1.9")
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
        val markedCount: Long,
        val sweepStatistics: Map<String, SweepStatistics>,
        val memoryUsageBefore: Map<String, MemoryUsage>,
        val memoryUsageAfter: Map<String, MemoryUsage>,
) {
    internal companion object {
        val lastGCInfo: GCInfo?
            get() = getGcInfo(0)

        private fun getGcInfo(id: Int) = GCInfoBuilder().apply { fill(id) }.build();
    }
}


@NativeRuntimeApi
private class GCInfoBuilder() {
    var epoch: Long? = null
    var startTimeNs: Long? = null
    var endTimeNs: Long? = null
    var firstPauseRequestTimeNs: Long? = null
    var firstPauseStartTimeNs: Long? = null
    var firstPauseEndTimeNs: Long? = null
    var secondPauseRequestTimeNs: Long? = null
    var secondPauseStartTimeNs: Long? = null
    var secondPauseEndTimeNs: Long? = null
    var postGcCleanupTimeNs: Long? = null
    var rootSet: RootSetStatistics? = null
    var markedCount: Long? = null
    var sweepStatistics = mutableMapOf<String, SweepStatistics>()
    var memoryUsageBefore = mutableMapOf<String, MemoryUsage>()
    var memoryUsageAfter = mutableMapOf<String, MemoryUsage>()

    @ExportForCppRuntime("Kotlin_Internal_GC_GCInfoBuilder_setEpoch")
    private fun setEpoch(value: Long) {
        epoch = value
    }

    @ExportForCppRuntime("Kotlin_Internal_GC_GCInfoBuilder_setStartTime")
    private fun setStartTime(value: Long) {
        startTimeNs = value
    }

    @ExportForCppRuntime("Kotlin_Internal_GC_GCInfoBuilder_setEndTime")
    private fun setEndTime(value: Long) {
        endTimeNs = value
    }

    @ExportForCppRuntime("Kotlin_Internal_GC_GCInfoBuilder_setFirstPauseRequestTime")
    private fun setFirstPauseRequestTime(value: Long) {
        firstPauseRequestTimeNs = value
    }

    @ExportForCppRuntime("Kotlin_Internal_GC_GCInfoBuilder_setFirstPauseStartTime")
    private fun setFirstPauseStartTime(value: Long) {
        firstPauseStartTimeNs = value
    }

    @ExportForCppRuntime("Kotlin_Internal_GC_GCInfoBuilder_setFirstPauseEndTime")
    private fun setFirstPauseEndTime(value: Long) {
        firstPauseEndTimeNs = value
    }

    @ExportForCppRuntime("Kotlin_Internal_GC_GCInfoBuilder_setSecondPauseRequestTime")
    private fun setSecondPauseRequestTime(value: Long) {
        secondPauseRequestTimeNs = value
    }

    @ExportForCppRuntime("Kotlin_Internal_GC_GCInfoBuilder_setSecondPauseStartTime")
    private fun setSecondPauseStartTime(value: Long) {
        secondPauseStartTimeNs = value
    }

    @ExportForCppRuntime("Kotlin_Internal_GC_GCInfoBuilder_setSecondPauseEndTime")
    private fun setSecondPauseEndTime(value: Long) {
        secondPauseEndTimeNs = value
    }

    @ExportForCppRuntime("Kotlin_Internal_GC_GCInfoBuilder_setPostGcCleanupTime")
    private fun setFinalizersDoneTime(value: Long) {
        postGcCleanupTimeNs = value
    }

    @ExportForCppRuntime("Kotlin_Internal_GC_GCInfoBuilder_setRootSet")
    private fun setRootSet(threadLocalReferences: Long, stackReferences: Long, globalReferences: Long, stableReferences: Long) {
        rootSet = RootSetStatistics(threadLocalReferences, stackReferences, globalReferences, stableReferences)
    }

    @ExportForCppRuntime("Kotlin_Internal_GC_GCInfoBuilder_setMarkStats")
    private fun setMarkStats(markedCount: Long) {
        this.markedCount = markedCount
    }

    @ExportForCppRuntime("Kotlin_Internal_GC_GCInfoBuilder_setSweepStats")
    private fun setSweepStats(name: NativePtr, keptCount: Long, sweptCount: Long) {
        val nameString = interpretCPointer<ByteVar>(name)!!.toKString()
        val stats = SweepStatistics(sweptCount, keptCount)
        sweepStatistics[nameString] = stats
    }

    @ExportForCppRuntime("Kotlin_Internal_GC_GCInfoBuilder_setMemoryUsageBefore")
    private fun setMemoryUsageBefore(name: NativePtr, totalObjectsSize: Long) {
        val nameString = interpretCPointer<ByteVar>(name)!!.toKString()
        val memoryUsage = MemoryUsage(totalObjectsSize)
        memoryUsageBefore[nameString] = memoryUsage
    }

    @ExportForCppRuntime("Kotlin_Internal_GC_GCInfoBuilder_setMemoryUsageAfter")
    private fun setMemoryUsageAfter(name: NativePtr, totalObjectsSize: Long) {
        val nameString = interpretCPointer<ByteVar>(name)!!.toKString()
        val memoryUsage = MemoryUsage(totalObjectsSize)
        memoryUsageAfter[nameString] = memoryUsage
    }

    fun build(): GCInfo? {
        return GCInfo(
                epoch ?: return null,
                startTimeNs ?: return null,
                endTimeNs ?: return null,
                firstPauseRequestTimeNs ?: return null,
                firstPauseStartTimeNs ?: return null,
                firstPauseEndTimeNs ?: return null,
                secondPauseRequestTimeNs,
                secondPauseStartTimeNs,
                secondPauseEndTimeNs,
                postGcCleanupTimeNs,
                rootSet ?: return null,
                markedCount ?: return null,
                sweepStatistics.toMap(),
                memoryUsageBefore.toMap(),
                memoryUsageAfter.toMap()
        )
    }

    @GCUnsafeCall("Kotlin_Internal_GC_GCInfoBuilder_Fill")
    external fun fill(id: Int)
}
