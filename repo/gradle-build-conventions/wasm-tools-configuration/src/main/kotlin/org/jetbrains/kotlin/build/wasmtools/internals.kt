/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Copy from KGP
// Remove when there will be default build platform detection
// KT-91862
package org.jetbrains.kotlin.build.wasmtools

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsBuildPlatform

@OptIn(ExperimentalWasmDsl::class)
internal fun ProviderFactory.detectWasmToolsPlatform(): Provider<WasmToolsBuildPlatform> {
    return currentHostPlatform().zip(systemProperty("os.arch")) { osType, osArch ->
        val platform = parsePlatform(osType, osArch)
        WasmToolsBuildPlatform(platform.name, platform.arch)
    }
}

private fun ProviderFactory.currentHostPlatform(): Provider<OsType> = systemProperty("os.name").map {
    parseOsType(it)
}

private fun parseOsType(type: String): OsType {
    val name = type.lowercase()
    return when {
        name.contains("windows") -> OsType.WINDOWS
        name.contains("mac") -> OsType.MAC
        name.contains("linux") -> OsType.LINUX
        name.contains("freebsd") -> OsType.FREEBSD
        else -> error("Unsupported OS: $name")
    }
}

private enum class OsType(val osName: String) {
    WINDOWS("win"),
    MAC("darwin"),
    LINUX("linux"),
    FREEBSD("linux"), // https://github.com/node-gradle/gradle-node-plugin/issues/178
}

private fun parsePlatform(osType: OsType, arch: String): Platform {
    val osArch = if (osType == OsType.WINDOWS) parseWindowsArch(arch.lowercase())
    else parseOsArch(arch.lowercase())

    return Platform(
        osType.osName,
        osArch
    )
}

private fun parseWindowsArch(arch: String): String {
    return when {
        // Node.js publishes no 32-bit ARM Windows build, so any ARM JVM is mapped to arm64.
        arch.startsWith("aarch") || arch.startsWith("arm") -> "arm64"
        arch.contains("64") -> "x64"
        else -> "x86"
    }
}

private fun parseOsArch(arch: String): String {
    return when {
        // Different JVMs name 64-bit ARM "aarch64" or "arm64".
        arch == "aarch64" || arch == "arm64" -> "arm64"
        // Java reports just "arm" (or "aarch32") for every 32-bit ARM variant and we no longer exec `uname` to tell
        // them apart. Node.js ships a single 32-bit ARM distribution, "armv7l", so we always use it. This is also
        // correct for 32-bit userland on an armv8l kernel (previously mapped to "arm64", which cannot run there).
        // armv6l is not distinguished: nodejs.org publishes no official armv6l builds.
        arch == "arm" || arch == "aarch32" -> "armv7l"
        arch == "ppc64le" -> "ppc64le"
        arch == "s390x" -> "s390x"
        arch.contains("64") -> "x64"
        else -> "x86"
    }
}

private data class Platform(
    val name: String,
    val arch: String,
) {
    fun isWindows(): Boolean {
        return name == "win"
    }
}
