/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

/**
 *  ## Cycle garbage collector interface.
 *
 * Konan relies upon reference counting for object management, however it could
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
object GC {
    /**
     * To force garbage collection immediately, unless collector is stopped
     * with [stop] operation. Even if GC is suspended, [collect] still triggers collection.
     */
    @SymbolName("Kotlin_native_internal_GC_collect")
    external fun collect()

    /**
     * Request global cyclic collector, operation is async and just triggers the collection.
     */
    @SymbolName("Kotlin_native_internal_GC_collectCyclic")
    external fun collectCyclic()

    /**
     * Suspend garbage collection. Release candidates are still collected, but
     * GC algorithm is not executed.
     */
    @SymbolName("Kotlin_native_internal_GC_suspend")
    external fun suspend()

    /**
     * Resume garbage collection. Can potentially lead to GC immediately.
     */
    @SymbolName("Kotlin_native_internal_GC_resume")
    external fun resume()

    /**
     * Stop garbage collection. Cyclical garbage is no longer collected.
     */
    @SymbolName("Kotlin_native_internal_GC_stop")
    external fun stop()

    /**
     * Start garbage collection. Cyclical garbage produced while GC was stopped
     * cannot be reclaimed, but all new garbage is collected.
     */
    @SymbolName("Kotlin_native_internal_GC_start")
    external fun start()

    /**
     * GC threshold, controlling how frequenly GC is activated, and how much time GC
     * takes. Bigger values lead to longer GC pauses, but less GCs.
     */
    var threshold: Int
        get() = getThreshold()
        set(value) = setThreshold(value)

    /**
     * GC allocation threshold, controlling how frequenly GC collect cycles, and how much time
     * this process takes. Bigger values lead to longer GC pauses, but less GCs.
     */
    var collectCyclesThreshold: Long
        get() = getCollectCyclesThreshold()
        set(value) = setCollectCyclesThreshold(value)

    /**
     * GC allocation threshold, controlling how many bytes allocated since last
     * collection will trigger new GC.
     */
    var thresholdAllocations: Long
        get() = getThresholdAllocations()
        set(value) = setThresholdAllocations(value)

    /**
     * If GC shall auto-tune thresholds, depending on how much time is spent in collection.
     */
    var autotune: Boolean
        get() = getTuneThreshold()
        set(value) = setTuneThreshold(value)


    /**
     * If cyclic collector for atomic references to be deployed.
     */
    var cyclicCollectorEnabled: Boolean
        get() = getCyclicCollectorEnabled()
        set(value) = setCyclicCollectorEnabled(value)

    /**
     * Detect cyclic references going via atomic references and return list of cycle-inducing objects
     * or `null` if the leak detector is not available. Use [Platform.isMemoryLeakCheckerActive] to check
     * leak detector availability.
     */
    @SymbolName("Kotlin_native_internal_GC_detectCycles")
    external fun detectCycles(): Array<Any>?

    /**
     * Find a reference cycle including from the given object, `null` if no cycles detected.
     */
    @SymbolName("Kotlin_native_internal_GC_findCycle")
    external fun findCycle(root: Any): Array<Any>?

    @SymbolName("Kotlin_native_internal_GC_getThreshold")
    private external fun getThreshold(): Int

    @SymbolName("Kotlin_native_internal_GC_setThreshold")
    private external fun setThreshold(value: Int)

    @SymbolName("Kotlin_native_internal_GC_getCollectCyclesThreshold")
    private external fun getCollectCyclesThreshold(): Long

    @SymbolName("Kotlin_native_internal_GC_setCollectCyclesThreshold")
    private external fun setCollectCyclesThreshold(value: Long)

    @SymbolName("Kotlin_native_internal_GC_getThresholdAllocations")
    private external fun getThresholdAllocations(): Long

    @SymbolName("Kotlin_native_internal_GC_setThresholdAllocations")
    private external fun setThresholdAllocations(value: Long)

    @SymbolName("Kotlin_native_internal_GC_getTuneThreshold")
    private external fun getTuneThreshold(): Boolean

    @SymbolName("Kotlin_native_internal_GC_setTuneThreshold")
    private external fun setTuneThreshold(value: Boolean)

    @SymbolName("Kotlin_native_internal_GC_getCyclicCollector")
    private external fun getCyclicCollectorEnabled(): Boolean

    @SymbolName("Kotlin_native_internal_GC_setCyclicCollector")
    private external fun setCyclicCollectorEnabled(value: Boolean)
}
