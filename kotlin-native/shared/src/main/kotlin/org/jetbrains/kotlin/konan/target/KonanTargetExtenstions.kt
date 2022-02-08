package org.jetbrains.kotlin.konan.target

// TODO: This all needs to go to konan.properties

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
        else -> false // watchOS/tvOS/android_x86/android_arm32 aren't tested; linux_mips32/linux_mipsel32 need linking with libatomic.
    }

fun KonanTarget.supportsLibBacktrace(): Boolean =
        this.family.isAppleFamily ||
                // MIPS architectures have issues, see KT-48949
                (this.family == Family.LINUX && this.architecture !in listOf(Architecture.MIPS32, Architecture.MIPSEL32)) ||
                this.family == Family.ANDROID

fun KonanTarget.supportsCoreSymbolication(): Boolean =
        this in listOf(
                KonanTarget.MACOS_X64, KonanTarget.MACOS_ARM64, KonanTarget.IOS_X64,
                KonanTarget.IOS_SIMULATOR_ARM64, KonanTarget.TVOS_X64, KonanTarget.TVOS_SIMULATOR_ARM64,
                KonanTarget.WATCHOS_X86, KonanTarget.WATCHOS_X64, KonanTarget.WATCHOS_SIMULATOR_ARM64
        )


fun KonanTarget.supportsThreads(): Boolean =
     when(this) {
        is KonanTarget.WASM32 -> false
        is KonanTarget.ZEPHYR -> false
        else -> true
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
