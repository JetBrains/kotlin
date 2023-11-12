/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlin.native

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.TypedIntrinsic
import kotlin.native.internal.IntrinsicType

/**
 * Operating system family.
 */
@ExperimentalNativeApi
public enum class OsFamily {
    UNKNOWN,
    MACOSX,
    IOS,
    LINUX,
    WINDOWS,
    ANDROID,
    WASM,
    TVOS,
    WATCHOS
}

/**
 * Central Processor Unit architecture.
 */
@ExperimentalNativeApi
public enum class CpuArchitecture(public val bitness: Int) {
    UNKNOWN(-1),
    ARM32(32),
    ARM64(64),
    X86(32),
    X64(64),
    MIPS32(32),
    MIPSEL32(32),
    WASM32(32);
}

/**
 * Memory model.
 */
// NOTE: Must match `MemoryModel` in `Memory.h`
@ExperimentalNativeApi
public enum class MemoryModel {
    STRICT,
    RELAXED,
    EXPERIMENTAL,
}

/**
 * Object describing the current platform program executes upon.
 */
@ExperimentalNativeApi
public object Platform {
    /**
     * Check if current architecture allows unaligned access to wider than byte locations.
     */
    public val canAccessUnaligned: Boolean
        get() = Platform_canAccessUnaligned() != 0

    /**
     * Check if byte order of the current platform is least significant byte (LSB) first, aka little endian.
     */
    public val isLittleEndian: Boolean
        get() = Platform_isLittleEndian() != 0

    /**
     * Operating system family program executes upon.
     */
    public val osFamily: OsFamily
        get() = OsFamily.values()[Platform_getOsFamily()]

    /**
     * Architecture of the CPU program executes upon.
     */
    public val cpuArchitecture: CpuArchitecture
        get() = CpuArchitecture.values()[Platform_getCpuArchitecture()]

    /**
     * Memory model binary was compiled with.
     */
    public val memoryModel: MemoryModel
        get() = MemoryModel.values()[Platform_getMemoryModel()]

    /**
     * If binary was compiled in debug mode.
     */
    public val isDebugBinary: Boolean
        get() = Platform_isDebugBinary()

    /**
     * If freezing is enabled.
     *
     * This value would be false, only if binary option `freezing` is equal to `disabled`. This is default when
     * [memoryModel] is equal to [MemoryModel.EXPERIMENTAL].
     */
    public val isFreezingEnabled: Boolean
        get() = Platform_isFreezingEnabled()

    /**
     * If the memory leak checker is activated, by default `true` in debug mode, `false` in release.
     * When memory leak checker is activated, and leak is detected during last Kotlin context
     * deinitialization process - error message with leak information is printed and application
     * execution is aborted.
     *
     * @see isDebugBinary
     */
    public var isMemoryLeakCheckerActive: Boolean
        get() = Platform_getMemoryLeakChecker()
        set(value) = Platform_setMemoryLeakChecker(value)

    public var isCleanersLeakCheckerActive: Boolean
        get() = Platform_getCleanersLeakChecker()
        set(value) = Platform_setCleanersLeakChecker(value)

    /**
     * The number of logical processors available.
     *
     * Can be not equal to the number of processors in the system if some restrictions on processor usage were successfully detected.
     * Some kinds of processor usage restrictions are not detected, for now, e.g., CPU quotas in containers.
     *
     * The value is computed on each usage. It can change if some OS scheduler API restricts the process during runtime.
     * Also, value can differ on different threads if some thread-specific scheduler API was used.
     *
     * If one considers the value to be inaccurate and wants another one to be used, it can be overridden by
     * `KOTLIN_NATIVE_AVAILABLE_PROCESSORS` environment variable. When the variable is set and contains a value that is not
     * positive [Int], [IllegalStateException] will be thrown.
     */
    public fun getAvailableProcessors() : Int {
        val fromEnv = Platform_getAvailableProcessorsEnv()
        if (fromEnv == null) {
            return Platform_getAvailableProcessors()
        }
        return fromEnv.toIntOrNull()?.takeIf { it > 0 } ?:
            throw IllegalStateException("Available processors has incorrect environment override: $fromEnv")
    }
}

@GCUnsafeCall("Konan_Platform_canAccessUnaligned")
private external fun Platform_canAccessUnaligned(): Int

@GCUnsafeCall("Konan_Platform_isLittleEndian")
private external fun Platform_isLittleEndian(): Int

@GCUnsafeCall("Konan_Platform_getOsFamily")
private external fun Platform_getOsFamily(): Int

@GCUnsafeCall("Konan_Platform_getCpuArchitecture")
private external fun Platform_getCpuArchitecture(): Int

@GCUnsafeCall("Konan_Platform_getMemoryModel")
private external fun Platform_getMemoryModel(): Int

@GCUnsafeCall("Konan_Platform_isDebugBinary")
private external fun Platform_isDebugBinary(): Boolean

@GCUnsafeCall("Konan_Platform_isFreezingEnabled")
private external fun Platform_isFreezingEnabled(): Boolean

@GCUnsafeCall("Konan_Platform_getMemoryLeakChecker")
private external fun Platform_getMemoryLeakChecker(): Boolean

@GCUnsafeCall("Konan_Platform_setMemoryLeakChecker")
private external fun Platform_setMemoryLeakChecker(value: Boolean): Unit

@GCUnsafeCall("Konan_Platform_getCleanersLeakChecker")
private external fun Platform_getCleanersLeakChecker(): Boolean

@GCUnsafeCall("Konan_Platform_setCleanersLeakChecker")
private external fun Platform_setCleanersLeakChecker(value: Boolean): Unit

@GCUnsafeCall("Konan_Platform_getAvailableProcessorsEnv")
private external fun Platform_getAvailableProcessorsEnv(): String?

@GCUnsafeCall("Konan_Platform_getAvailableProcessors")
private external fun Platform_getAvailableProcessors(): Int


@TypedIntrinsic(IntrinsicType.IS_EXPERIMENTAL_MM)
@ExperimentalStdlibApi
public external fun isExperimentalMM(): Boolean
