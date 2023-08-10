/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

enum class BinaryFormat {
    ELF,
    PE_COFF,
    MACH_O
}

fun KonanTarget.binaryFormat() = when (family) {
    Family.WATCHOS -> BinaryFormat.MACH_O
    Family.IOS -> BinaryFormat.MACH_O
    Family.TVOS -> BinaryFormat.MACH_O
    Family.OSX -> BinaryFormat.MACH_O
    Family.ANDROID -> BinaryFormat.ELF
    Family.LINUX -> BinaryFormat.ELF
    Family.MINGW -> BinaryFormat.PE_COFF
    Family.WASM, Family.ZEPHYR -> null
}

fun KonanTarget.pointerBits() = when (architecture) {
    Architecture.X64 -> 64
    Architecture.X86 -> 32
    Architecture.ARM64 -> if (this == KonanTarget.WATCHOS_ARM64) 32 else 64
    Architecture.ARM32 -> 32
    Architecture.MIPS32 -> 32
    Architecture.MIPSEL32 -> 32
    Architecture.WASM32 -> 32
}


fun KonanTarget.supportsCodeCoverage(): Boolean =
        // TODO: Disabled for now, because we don't support
        //  coverage format from LLVM 11.
        false
//        this == KonanTarget.MINGW_X64 ||
//        this == KonanTarget.LINUX_X64 ||
//        this == KonanTarget.MACOS_X64 ||
//        this == KonanTarget.IOS_X64

fun KonanTarget.supportsMimallocAllocator(): Boolean =
     when(this) {
        is KonanTarget.LINUX_X64 -> true
        is KonanTarget.MINGW_X86 -> true
        is KonanTarget.MINGW_X64 -> true
        is KonanTarget.MACOS_X64 -> true
        is KonanTarget.MACOS_ARM64 -> true
        is KonanTarget.LINUX_ARM64 -> true
        is KonanTarget.LINUX_ARM32_HFP -> true
        is KonanTarget.ANDROID_X64 -> true
        is KonanTarget.ANDROID_ARM64 -> true
        is KonanTarget.IOS_ARM32 -> true
        is KonanTarget.IOS_ARM64 -> true
        is KonanTarget.IOS_X64 -> true
        is KonanTarget.IOS_SIMULATOR_ARM64 -> true
        is KonanTarget.WATCHOS_ARM32, is KonanTarget.WATCHOS_ARM64,
        is KonanTarget.WATCHOS_SIMULATOR_ARM64, is KonanTarget.WATCHOS_X64, is KonanTarget.WATCHOS_X86,
        is KonanTarget.TVOS_ARM64, is KonanTarget.TVOS_SIMULATOR_ARM64, is KonanTarget.TVOS_X64,
        is KonanTarget.ANDROID_X86, is KonanTarget.ANDROID_ARM32 -> false // aren't tested.
        is KonanTarget.LINUX_MIPS32, is KonanTarget.LINUX_MIPSEL32 -> false // need linking with libatomic.
        is KonanTarget.WASM32, is KonanTarget.ZEPHYR -> false // likely not supported
        // Funny thing is we can neither access WATCHOS_DEVICE_ARM64, nor omit it explicitly due to the
        // build's quirks. Workaround by using else clause.
        // TODO: Add explicit WATCHOS_DEVICE_ARM64 after compiler update.
        else -> false
    }

fun KonanTarget.supportsLibBacktrace(): Boolean =
        this.family.isAppleFamily ||
                // MIPS architectures have issues, see KT-48949
                (this.family == Family.LINUX && this.architecture !in listOf(Architecture.MIPS32, Architecture.MIPSEL32)) ||
                this.family == Family.ANDROID

// TODO: Add explicit WATCHOS_DEVICE_ARM64 after compiler update.
fun KonanTarget.supportsCoreSymbolication(): Boolean =
        this in listOf(
                KonanTarget.MACOS_X64, KonanTarget.MACOS_ARM64, KonanTarget.IOS_X64,
                KonanTarget.IOS_SIMULATOR_ARM64, KonanTarget.TVOS_X64, KonanTarget.TVOS_SIMULATOR_ARM64,
                KonanTarget.WATCHOS_X86, KonanTarget.WATCHOS_X64, KonanTarget.WATCHOS_SIMULATOR_ARM64
        )

fun KonanTarget.supportsGccUnwind(): Boolean = family == Family.ANDROID || family == Family.LINUX || this is KonanTarget.MINGW_X86
// MINGW_X64 target does not support GCC unwind, since its sysroot contains libgcc version < 12 having misfeature, see KT-49240
fun KonanTarget.supportsWinAPIUnwind(): Boolean = this is KonanTarget.MINGW_X64

fun KonanTarget.supportsThreads(): Boolean = when(this) {
    is KonanTarget.WASM32 -> false
    is KonanTarget.ZEPHYR -> false
    else -> true
}

fun KonanTarget.supportsExceptions(): Boolean = when(this) {
    is KonanTarget.WASM32 -> false
    is KonanTarget.ZEPHYR -> false
    else -> true
}

fun KonanTarget.suportsMemMem(): Boolean = when (this) {
    is KonanTarget.WASM32 -> false
    is KonanTarget.MINGW_X86 -> false
    is KonanTarget.MINGW_X64 -> false
    is KonanTarget.ZEPHYR -> false
    else -> true
}

fun KonanTarget.supportsObjcInterop(): Boolean = family.isAppleFamily
fun KonanTarget.hasFoundationFramework(): Boolean = family.isAppleFamily
fun KonanTarget.hasUIKitFramework(): Boolean = family == Family.IOS || family == Family.TVOS
fun KonanTarget.supports64BitMulOverflow(): Boolean = when (this) {
    is KonanTarget.MINGW_X86 -> false
    is KonanTarget.LINUX_ARM32_HFP -> false
    is KonanTarget.LINUX_MIPS32 -> false
    is KonanTarget.LINUX_MIPSEL32 -> false
    is KonanTarget.WASM32 -> false
    is KonanTarget.ZEPHYR -> false
    is KonanTarget.ANDROID_ARM32 -> false
    is KonanTarget.ANDROID_X86 -> false
    else -> true
}

// TODO: Add explicit WATCHOS_DEVICE_ARM64 after compiler update.
fun KonanTarget.supportsIosCrashLog(): Boolean = when (this) {
    KonanTarget.IOS_ARM32 -> true
    KonanTarget.IOS_ARM64 -> true
    KonanTarget.WATCHOS_ARM32 -> true
    KonanTarget.WATCHOS_ARM64 -> true
    KonanTarget.TVOS_ARM64 -> true
    else -> false
}

/*
 * While not 100% correct here, using atomic ops on iOS armv7 requires 8 byte alignment,
 * and general ABI requires 4-byte alignment on 64-bit long fields as mentioned in
 * https://developer.apple.com/library/archive/documentation/Xcode/Conceptual/iPhoneOSABIReference/Articles/ARMv6FunctionCallingConventions.html#//apple_ref/doc/uid/TP40009021-SW1
 * See https://github.com/ktorio/ktor/issues/941 for the context.
 * TODO: reconsider once target MIPS can do proper 64-bit load/store/CAS.
 */
fun KonanTarget.supports64BitAtomics(): Boolean = when (architecture) {
    Architecture.ARM32, Architecture.WASM32, Architecture.MIPS32, Architecture.MIPSEL32 -> false
    Architecture.X86, Architecture.ARM64, Architecture.X64 -> true
} && this != KonanTarget.WATCHOS_ARM64 && this != KonanTarget.WATCHOS_X86

fun KonanTarget.supportsUnalignedAccess(): Boolean = when (architecture) {
    Architecture.ARM32, Architecture.WASM32, Architecture.MIPS32, Architecture.MIPSEL32 -> false
    Architecture.X86, Architecture.ARM64, Architecture.X64 -> true
} && this != KonanTarget.WATCHOS_ARM64

fun KonanTarget.needSmallBinary() = when {
    family == Family.WATCHOS -> true
    family.isAppleFamily -> architecture == Architecture.ARM32
    else -> false
}

fun KonanTarget.supportedSanitizers(): List<SanitizerKind> =
    when(this) {
        is KonanTarget.LINUX_X64 -> listOf(SanitizerKind.ADDRESS)
        is KonanTarget.MACOS_X64 -> listOf(SanitizerKind.THREAD)
        // TODO: Enable ASAN on macOS. Currently there's an incompatibility between clang frontend version and clang_rt.asan version.
        // TODO: Enable TSAN on linux. Currently there's a link error between clang_rt.tsan and libstdc++.
        // TODO: Consider supporting mingw.
        // TODO: Support macOS arm64
        else -> listOf()
    }

fun KonanTarget.hasAddressDependencyInMemoryModel(): Boolean =
     when (this.architecture) {
         Architecture.X86, Architecture.X64, Architecture.ARM32, Architecture.ARM64 -> true
         Architecture.MIPS32, Architecture.MIPSEL32, Architecture.WASM32 -> false
     }

val KonanTarget.supportsGrandCentralDispatch
    get() = when(family) {
        Family.WATCHOS, Family.IOS, Family.TVOS, Family.OSX -> true
        else -> false
    }
