/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlin.native.internal

@InternalForKotlinNative
public object MemoryUsageInfo {
    // An estimate of how much memory was committed by the process at its peak:
    // * RSS on Linux
    // * Memory Footprint on macOS
    // * Working Set Size on Windows
    // May return 0 if unimplemented on some platform, or in case of an error.
    public val peakResidentSetSizeBytes: Long
        get() = MemoryUsageInfo_getPeakResidentSetSizeBytes()
}

@GCUnsafeCall("Kotlin_MemoryUsageInfo_getPeakResidentSetSizeBytes")
private external fun MemoryUsageInfo_getPeakResidentSetSizeBytes(): Long
