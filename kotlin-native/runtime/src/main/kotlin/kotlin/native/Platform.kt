/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlin.native

import kotlin.native.internal.GCCritical

/**
 * Operating system family.
 */
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
public enum class CpuArchitecture(val bitness: Int) {
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
public enum class MemoryModel {
    STRICT,
    RELAXED,
    EXPERIMENTAL,
}

/**
 * Object describing the current platform program executes upon.
 */
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
     * Architechture of the CPU program executes upon.
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
}

@SymbolName("Konan_Platform_canAccessUnaligned")
@GCCritical
private external fun Platform_canAccessUnaligned(): Int

@SymbolName("Konan_Platform_isLittleEndian")
@GCCritical
private external fun Platform_isLittleEndian(): Int

@SymbolName("Konan_Platform_getOsFamily")
@GCCritical
private external fun Platform_getOsFamily(): Int

@SymbolName("Konan_Platform_getCpuArchitecture")
@GCCritical
private external fun Platform_getCpuArchitecture(): Int

@SymbolName("Konan_Platform_getMemoryModel")
@GCCritical
private external fun Platform_getMemoryModel(): Int

@SymbolName("Konan_Platform_isDebugBinary")
@GCCritical
private external fun Platform_isDebugBinary(): Boolean

@SymbolName("Konan_Platform_getMemoryLeakChecker")
@GCCritical
private external fun Platform_getMemoryLeakChecker(): Boolean

@SymbolName("Konan_Platform_setMemoryLeakChecker")
@GCCritical
private external fun Platform_setMemoryLeakChecker(value: Boolean): Unit

@SymbolName("Konan_Platform_getCleanersLeakChecker")
@GCCritical
private external fun Platform_getCleanersLeakChecker(): Boolean

@SymbolName("Konan_Platform_setCleanersLeakChecker")
@GCCritical
private external fun Platform_setCleanersLeakChecker(value: Boolean): Unit
