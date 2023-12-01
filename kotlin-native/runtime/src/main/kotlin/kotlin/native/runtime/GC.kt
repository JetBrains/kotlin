/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.runtime

import kotlin.time.*
import kotlin.time.Duration.Companion.microseconds
import kotlin.native.internal.GCUnsafeCall

/**
 * __Note__: this API is unstable and may change in any release.
 *
 * Kotlin/Native uses tracing garbage collector (GC) that is executed periodically to collect objects
 * that are not reachable from the "roots", like local and global variables.
 * See [documentation](https://kotlinlang.org/docs/native-memory-manager.html) to learn more about
 * Kotlin/Native memory management.
 *
 * This object provides a set of functions and properties that allows to tune garbage collector.
 *
 * __Legacy memory manager__
 *
 * Kotlin/Native relies upon reference counting for object management, however it could
 * not collect cyclical garbage, so we perform periodic garbage collection.
 * This may slow down application, so this interface provides control over how
 * garbage collector activates and runs.
 * Garbage collector can be in one of the following states:
 *  - running
 *  - suspended (so cycle candidates are collected, but GC is not performed until resume)
 *  - stopped (all cyclical garbage is hopelessly lost)
 * Immediately after startup GC is in running state.
 * Depending on application needs it may select to suspend GC for certain phases of
 * its lifetime, and resume it later on, or just completely turn it off, if GC pauses
 * are less desirable than cyclical garbage leaks.
 */
@NativeRuntimeApi
@SinceKotlin("1.9")
public object GC {
    /**
     * Trigger new collection and wait for its completion.
     *
     * Legacy MM: force garbage collection immediately, unless collector is stopped
     * with [stop] operation. Even if GC is suspended, [collect] still triggers collection.
     */
    @GCUnsafeCall("Kotlin_native_internal_GC_collect")
    public external fun collect()

    /**
     * Trigger new collection without waiting for its completion.
     */
    @GCUnsafeCall("Kotlin_native_internal_GC_schedule")
    public external fun schedule()

    /**
     * Deprecated and unused.
     *
     * Legacy MM: Request global cyclic collector, operation is async and just triggers the collection.
     */
    @GCUnsafeCall("Kotlin_native_internal_GC_collectCyclic")
    @Deprecated("No-op in modern GC implementation")
    public external fun collectCyclic()

    /**
     * Deprecated and unused.
     *
     * Legacy MM: Suspend garbage collection. Release candidates are still collected, but
     * GC algorithm is not executed.
     */
    @GCUnsafeCall("Kotlin_native_internal_GC_suspend")
    public external fun suspend()

    /**
     * Deprecated and unused.
     *
     * Legacy MM: Resume garbage collection. Can potentially lead to GC immediately.
     */
    @GCUnsafeCall("Kotlin_native_internal_GC_resume")
    @Deprecated("No-op in modern GC implementation")
    public external fun resume()

    /**
     * Deprecated and unused.
     *
     * Legacy MM: Stop garbage collection. Cyclical garbage is no longer collected.
     */
    @GCUnsafeCall("Kotlin_native_internal_GC_stop")
    @Deprecated("No-op in modern GC implementation")
    public external fun stop()

    /**
     * Deprecated and unused.
     *
     * Legacy MM: Start garbage collection. Cyclical garbage produced while GC was stopped
     * cannot be reclaimed, but all new garbage is collected.
     */
    @GCUnsafeCall("Kotlin_native_internal_GC_start")
    @Deprecated("No-op in modern GC implementation")
    public external fun start()

    /**
     * Deprecated and unused.
     *
     * Legacy MM: GC threshold, controlling how frequenly GC is activated, and how much time GC
     * takes. Bigger values lead to longer GC pauses, but less GCs.
     *
     * Default: 8 * 1024
     *
     * @throws [IllegalArgumentException] when value is not positive.
     */
    @Deprecated("No-op in modern GC implementation")
    public var threshold: Int
        get() = getThreshold()
        set(value) {
            require(value > 0) { "threshold must be positive: $value" }
            setThreshold(value)
        }

    /**
     * Deprecated and unused.
     *
     * Legacy MM: GC allocation threshold, controlling how frequenly GC collect cycles, and how much time
     * this process takes. Bigger values lead to longer GC pauses, but less GCs.
     *
     * Default: 8 * 1024
     *
     * @throws [IllegalArgumentException] when value is not positive.
     */
    @Deprecated("No-op in modern GC implementation")
    public var collectCyclesThreshold: Long
        get() = getCollectCyclesThreshold()
        set(value) {
            require(value > 0) { "collectCyclesThreshold must be positive: $value" }
            setCollectCyclesThreshold(value)
        }

    /**
     * Deprecated and unused.
     *
     * Legacy MM: GC allocation threshold, controlling how many bytes allocated since last
     * collection will trigger new GC.
     *
     * Default: 8 * 1024 * 1024
     *
     * @throws [IllegalArgumentException] when value is not positive.
     */
    public var thresholdAllocations: Long
        get() = getThresholdAllocations()
        set(value) {
            require(value > 0) { "thresholdAllocations must be positive: $value" }
            setThresholdAllocations(value)
        }

    /**
     * If true update targetHeapBytes after each collection.
     *
     * Legacy MM: If GC shall auto-tune thresholds, depending on how much time is spent in collection.
     *
     * Default: true
     */
    public var autotune: Boolean
        get() = getTuneThreshold()
        set(value) = setTuneThreshold(value)


    /**
     * Deprecated and unused.
     *
     * Legacy MM: If cyclic collector for atomic references to be deployed.
     */
    @Deprecated("No-op in modern GC implementation")
    public var cyclicCollectorEnabled: Boolean
        get() = getCyclicCollectorEnabled()
        set(value) = setCyclicCollectorEnabled(value)

    /**
     * When Kotlin code is not allocating enough to trigger GC, the GC scheduler uses timer to drive collection.
     * Timer-triggered collection will happen roughly in [regularGCInterval] .. 2 * [regularGCInterval] since
     * any previous collection.
     *
     * Default: 10 seconds
     *
     * Unused in legacy MM.
     *
     * @throws [IllegalArgumentException] when value is negative.
     */
    public var regularGCInterval: Duration
        get() = getRegularGCIntervalMicroseconds().microseconds
        set(value) {
            require(!value.isNegative()) { "regularGCInterval must not be negative: $value" }
            setRegularGCIntervalMicroseconds(value.inWholeMicroseconds)
        }

    /**
     * Total amount of heap available for Kotlin objects. The GC tries to schedule execution
     * so that Kotlin heap doesn't overflow this heap.
     * Automatically adjusts when [autotune] is true:
     * after each collection the [targetHeapBytes] is set to heapBytes / [targetHeapUtilization] and
     * capped between [minHeapBytes] and [maxHeapBytes], where heapBytes is heap usage after the garbage
     * is collected.
     * Note, that if after a collection heapBytes > [targetHeapBytes] (which may happen if [autotune] is false,
     * or [maxHeapBytes] is set too low), the next collection will be triggered almost immediately.
     *
     * Default: 100 MiB (10 MiB on watchOS)
     *
     * Unused in legacy MM.
     *
     * @throws [IllegalArgumentException] when value is negative.
     */
    public var targetHeapBytes: Long
        get() = getTargetHeapBytes()
        set(value) {
            require(value >= 0) { "targetHeapBytes must not be negative: $value" }
            setTargetHeapBytes(value)
        }

    /**
     * What fraction of the Kotlin heap should be populated.
     * Only used if [autotune] is true. See [targetHeapBytes] for more details.
     *
     * Default: 0.5
     *
     * Unused in legacy MM.
     *
     * @throws [IllegalArgumentException] when value is outside (0, 1] interval.
     */
    public var targetHeapUtilization: Double
        get() = getTargetHeapUtilization()
        set(value) {
            require(value > 0 && value <= 1) { "targetHeapUtilization must be in (0, 1] interval: $value" }
            setTargetHeapUtilization(value)
        }

    /**
     * The minimum value for [targetHeapBytes]
     * Only used if [autotune] is true. See [targetHeapBytes] for more details.
     *
     * Default: 5 MiB
     *
     * Unused in legacy MM.
     *
     * @throws [IllegalArgumentException] when value is negative.
     */
    public var minHeapBytes: Long
        get() = getMinHeapBytes()
        set(value) {
            require(value >= 0) { "minHeapBytes must not be negative: $value" }
            setMinHeapBytes(value)
        }

    /**
     * The maximum value for [targetHeapBytes].
     * Only used if [autotune] is true. See [targetHeapBytes] for more details.
     *
     * Default: [Long.MAX_VALUE]
     *
     * Unused in legacy MM.
     *
     * @throws [IllegalArgumentException] when value is negative.
     */
    public var maxHeapBytes: Long
        get() = getMaxHeapBytes()
        set(value) {
            require(value >= 0) { "maxHeapBytes must not be negative: $value" }
            setMaxHeapBytes(value)
        }

    /**
     * The GC is scheduled when Kotlin heap overflows [heapTriggerCoefficient] * [targetHeapBytes].
     *
     * Default: 0.9
     *
     * @throws [IllegalArgumentException] when value is outside (0, 1] interval.
     */
    public var heapTriggerCoefficient: Double
        get() = getHeapTriggerCoefficient()
        set(value) {
            require(value > 0 && value <= 1) { "heapTriggerCoefficient must be in (0, 1] interval: $value" }
            setHeapTriggerCoefficient(value)
        }

    /**
     * If true, the GC will pause Kotlin threads when Kotlin heap overflows [targetHeapBytes]
     * and will resume them only after current GC is done.
     *
     * Default: true, unless [autotune] is false or [maxHeapBytes] is less than [Long.MAX_VALUE].
     */
    public var pauseOnTargetHeapOverflow: Boolean
        get() = getPauseOnTargetHeapOverflow()
        set(value) = setPauseOnTargetHeapOverflow(value)

    /**
     * Deprecated and unused. Always returns null.
     *
     * Legacy MM: Detect cyclic references going via atomic references and return list of cycle-inducing objects
     * or `null` if the leak detector is not available. Use [Platform.isMemoryLeakCheckerActive] to check
     * leak detector availability.
     */
    @GCUnsafeCall("Kotlin_native_internal_GC_detectCycles")
    @Deprecated("No-op in modern GC implementation")
    public external fun detectCycles(): Array<Any>?

    /**
     * Returns statistics of the last finished garbage collection run.
     * This information is supposed to be used for testing and debugging purposes only
     *
     * Can return null, if there was no garbage collection runs yet.
     *
     * Legacy MM: Always returns null
     */
    @ExperimentalStdlibApi
    public val lastGCInfo: GCInfo?
        get() = GCInfo.lastGCInfo

    /**
     * Deprecated and unused. Always returns null.
     *
     * Legacy MM: Find a reference cycle including from the given object, `null` if no cycles detected.
     */
    @GCUnsafeCall("Kotlin_native_internal_GC_findCycle")
    @Deprecated("No-op in modern GC implementation")
    public external fun findCycle(root: Any): Array<Any>?

    /**
     * This objects allows to customize the behavior of the finalizer processor that runs on the main thread.
     *
     * On Apple platforms Kotlin/Native releases ObjC/Swift objects that were passed to Kotlin when it processes the finalizers after GC.
     * Kotlin/Native can also utilize main run loop to release objects that were passed to Kotlin on the main thread. This can be turned
     * off by setting `objcDisposeOnMain` binary option to `false`.
     * For more information, see [iOS integration](https://kotlinlang.org/docs/native-ios-integration.html#deinitializers).
     *
     * This finalizer processor works as follows:
     * - Finalizers that must be run on the main thread get scheduled by the GC after the main GC phase is finished
     * - A task will be posted on the main run loop in which the scheduled finalizers will start processing
     * - Finalizers will be processed inside an `autoreleasepool` in batches of size [batchSize]
     * - If at some point during task the processor detected that more than [maxTimeInTask] has passed, it will
     *   stop and post another task to the main thread to continue processing finalizers later. Note that if some
     *   finalizer takes a very long time, the task will still process the entire [batchSize] and may significantly overflow [maxTimeInTask]
     * - It's guaranteed that the time interval between tasks is at least [minTimeBetweenTasks].
     *
     * [maxTimeInTask] and [minTimeBetweenTasks] allow other tasks posted on the main thread (e.g. UI events) be processed without
     * significant delays.
     */
    public object MainThreadFinalizerProcessor {
        /**
         * `true` if Kotlin/Native will use [MainThreadFinalizerProcessor] to process finalizers.
         *
         * __Note__: at the very start of the program this will be `false`, but may turn `true` later, if Kotlin/Native determines that the
         * main run loop is being processed.
         */
        public val available: Boolean
            get() = isAvailable()

        /**
         * How much time can each task take.
         *
         * There is no guarantee that the task will be completed under this time, this is only a hint.
         *
         * Setting this value too high makes some other main thread tasks (e.g. UI events) be processed with high delays.
         * Setting this value too low makes ObjC/Swift objects be released with high delays which contributes to memory usage.
         *
         * Default: 5ms
         *
         * @throws [IllegalArgumentException] when value is negative.
         */
        public var maxTimeInTask: Duration
            get() = getMaxTimeInTask().microseconds
            set(value) {
                require(!value.isNegative()) { "mainThreadMaxTimeInTask must not be negative: $value" }
                setMaxTimeInTask(value.inWholeMicroseconds)
            }

        /**
         * The minimum interval between two tasks.
         *
         * Setting this value too high makes ObjC/Swift objects be released with high delays which contributes to memory usage.
         * Setting this value too low makes some other main thread tasks (e.g. UI events) be processed with high delays.
         *
         * Default: 10ms
         *
         * @throws [IllegalArgumentException] when value is negative.
         */
        public var minTimeBetweenTasks: Duration
            get() = getMinTimeBetweenTasks().microseconds
            set(value) {
                require(!value.isNegative()) { "mainThreadMinTimeBetweenTasks must not be negative: $value" }
                setMinTimeBetweenTasks(value.inWholeMicroseconds)
            }

        /**
         * How many finalizers will be processed inside a single `autoreleasepool`.
         *
         * Setting this value too high makes it more likely that [maxTimeInTask] will not be respected.
         * Setting this value too low makes each single finalizer take more time and so fewer finalizers will be processed in one task.
         *
         * Default: 100
         *
         * @throws [IllegalArgumentException] when value is 0.
         */
        public var batchSize: ULong
            get() = getBatchSize()
            set(value) {
                require(value > 0U) { "mainThreadBatchSize must not be 0" }
                setBatchSize(value)
            }

        @GCUnsafeCall("Kotlin_native_runtime_GC_MainThreadFinalizerProcessor_isAvailable")
        private external fun isAvailable(): Boolean

        @GCUnsafeCall("Kotlin_native_runtime_GC_MainThreadFinalizerProcessor_getMaxTimeInTask")
        private external fun getMaxTimeInTask(): Long

        @GCUnsafeCall("Kotlin_native_runtime_GC_MainThreadFinalizerProcessor_setMaxTimeInTask")
        private external fun setMaxTimeInTask(value: Long)

        @GCUnsafeCall("Kotlin_native_runtime_GC_MainThreadFinalizerProcessor_getMinTimeBetweenTasks")
        private external fun getMinTimeBetweenTasks(): Long

        @GCUnsafeCall("Kotlin_native_runtime_GC_MainThreadFinalizerProcessor_setMinTimeBetweenTasks")
        private external fun setMinTimeBetweenTasks(value: Long)

        @GCUnsafeCall("Kotlin_native_runtime_GC_MainThreadFinalizerProcessor_getBatchSize")
        private external fun getBatchSize(): ULong

        @GCUnsafeCall("Kotlin_native_runtime_GC_MainThreadFinalizerProcessor_setBatchSize")
        private external fun setBatchSize(value: ULong)
    }

    @GCUnsafeCall("Kotlin_native_internal_GC_getThreshold")
    private external fun getThreshold(): Int

    @GCUnsafeCall("Kotlin_native_internal_GC_setThreshold")
    private external fun setThreshold(value: Int)

    @GCUnsafeCall("Kotlin_native_internal_GC_getCollectCyclesThreshold")
    private external fun getCollectCyclesThreshold(): Long

    @GCUnsafeCall("Kotlin_native_internal_GC_setCollectCyclesThreshold")
    private external fun setCollectCyclesThreshold(value: Long)

    @GCUnsafeCall("Kotlin_native_internal_GC_getThresholdAllocations")
    private external fun getThresholdAllocations(): Long

    @GCUnsafeCall("Kotlin_native_internal_GC_setThresholdAllocations")
    private external fun setThresholdAllocations(value: Long)

    @GCUnsafeCall("Kotlin_native_internal_GC_getTuneThreshold")
    private external fun getTuneThreshold(): Boolean

    @GCUnsafeCall("Kotlin_native_internal_GC_setTuneThreshold")
    private external fun setTuneThreshold(value: Boolean)

    @GCUnsafeCall("Kotlin_native_internal_GC_getCyclicCollector")
    private external fun getCyclicCollectorEnabled(): Boolean

    @GCUnsafeCall("Kotlin_native_internal_GC_setCyclicCollector")
    private external fun setCyclicCollectorEnabled(value: Boolean)

    @GCUnsafeCall("Kotlin_native_internal_GC_getRegularGCIntervalMicroseconds")
    private external fun getRegularGCIntervalMicroseconds(): Long

    @GCUnsafeCall("Kotlin_native_internal_GC_setRegularGCIntervalMicroseconds")
    private external fun setRegularGCIntervalMicroseconds(value: Long)

    @GCUnsafeCall("Kotlin_native_internal_GC_getTargetHeapBytes")
    private external fun getTargetHeapBytes(): Long

    @GCUnsafeCall("Kotlin_native_internal_GC_setTargetHeapBytes")
    private external fun setTargetHeapBytes(value: Long)

    @GCUnsafeCall("Kotlin_native_internal_GC_getTargetHeapUtilization")
    private external fun getTargetHeapUtilization(): Double

    @GCUnsafeCall("Kotlin_native_internal_GC_setTargetHeapUtilization")
    private external fun setTargetHeapUtilization(value: Double)

    @GCUnsafeCall("Kotlin_native_internal_GC_getMinHeapBytes")
    private external fun getMinHeapBytes(): Long

    @GCUnsafeCall("Kotlin_native_internal_GC_setMinHeapBytes")
    private external fun setMinHeapBytes(value: Long)

    @GCUnsafeCall("Kotlin_native_internal_GC_getMaxHeapBytes")
    private external fun getMaxHeapBytes(): Long

    @GCUnsafeCall("Kotlin_native_internal_GC_setMaxHeapBytes")
    private external fun setMaxHeapBytes(value: Long)

    @GCUnsafeCall("Kotlin_native_internal_GC_getHeapTriggerCoefficient")
    private external fun getHeapTriggerCoefficient(): Double

    @GCUnsafeCall("Kotlin_native_internal_GC_setHeapTriggerCoefficient")
    private external fun setHeapTriggerCoefficient(value: Double)

    @GCUnsafeCall("Kotlin_native_internal_GC_getPauseOnTargetHeapOverflow")
    private external fun getPauseOnTargetHeapOverflow(): Boolean

    @GCUnsafeCall("Kotlin_native_internal_GC_setPauseOnTargetHeapOverflow")
    private external fun setPauseOnTargetHeapOverflow(value: Boolean)
}
