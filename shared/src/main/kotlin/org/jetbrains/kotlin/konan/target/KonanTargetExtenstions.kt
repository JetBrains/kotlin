package org.jetbrains.kotlin.konan.target

fun KonanTarget.supportsCodeCoverage(): Boolean =
        this == KonanTarget.MINGW_X64 ||
        this == KonanTarget.LINUX_X64 ||
        this == KonanTarget.MACOS_X64 ||
        this == KonanTarget.IOS_X64

fun KonanTarget.supportsMimallocAllocator(): Boolean =
     when(this) {
        is KonanTarget.LINUX_X64 -> true
        is KonanTarget.MINGW_X86 -> true
        is KonanTarget.MINGW_X64 -> true
        is KonanTarget.MACOS_X64 -> true
        is KonanTarget.LINUX_ARM64 -> true
        is KonanTarget.LINUX_ARM32_HFP -> true
        is KonanTarget.ANDROID_X64 -> true
        is KonanTarget.ANDROID_ARM64 -> true
        is KonanTarget.IOS_ARM32 -> true
        is KonanTarget.IOS_ARM64 -> true
        is KonanTarget.IOS_X64 -> true
        else -> false // watchOS/tvOS/android_x86/android_arm32 aren't tested; linux_mips32/linux_mipsel32 need linking with libatomic.
    }