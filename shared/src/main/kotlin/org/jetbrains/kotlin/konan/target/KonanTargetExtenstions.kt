package org.jetbrains.kotlin.konan.target

fun KonanTarget.supportsCodeCoverage(): Boolean =
        this == KonanTarget.MINGW_X64 ||
        this == KonanTarget.LINUX_X64 ||
        this == KonanTarget.MACOS_X64 ||
        this == KonanTarget.IOS_X64