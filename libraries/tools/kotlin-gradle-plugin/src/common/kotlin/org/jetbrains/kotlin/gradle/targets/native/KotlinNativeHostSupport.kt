/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// The package of the other Kotlin/Native target helpers
@file:Suppress("PackageDirectoryMismatch")

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.buildtools.nativehost.DelicateKotlinNativeHostApi
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHost
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostArchitecture
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeHostOs
import org.jetbrains.kotlin.buildtools.nativehost.KotlinNativeTargetSupport
import org.jetbrains.kotlin.gradle.dsl.canLinkBinaries
import org.jetbrains.kotlin.gradle.dsl.konanTarget
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.KonanTarget

/**
 * What Kotlin/Native supports, as the plugin assumes it while it configures the build.
 *
 * It is the data of the Kotlin/Native version the plugin was released with. The plugin loads the Build Tools API
 * implementation only when tasks run, so the data of the version the build selected is not available here.
 */
@OptIn(DelicateKotlinNativeHostApi::class)
internal val bundledNativeTargetSupport: KotlinNativeTargetSupport = KotlinNativeTargetSupport.bundled()

/** Whether a Kotlin/Native distribution exists for this host. */
internal val KotlinNativeHost.isSupported: Boolean
    get() = this in bundledNativeTargetSupport.supportedHosts

/** The name of the Kotlin/Native target of this host, or `null` if the host is not supported. */
internal val KotlinNativeHost.targetName: String?
    get() = bundledNativeTargetSupport.target(this)?.stringValue

/** The Kotlin/Native target of this host, or `null` if the host is not supported. */
internal val KotlinNativeHost.konanTarget: KonanTarget?
    get() = bundledNativeTargetSupport.konanTarget(this)

/** Whether this host can link binaries for the [target]. */
internal fun KotlinNativeHost.canLinkBinaries(target: KonanTarget): Boolean =
    bundledNativeTargetSupport.canLinkBinaries(this, target)

internal val KotlinNativeHost.isMacOs: Boolean
    get() = os == KotlinNativeHostOs.MACOS

internal val KotlinNativeHost.isWindows: Boolean
    get() = os == KotlinNativeHostOs.WINDOWS

/**
 * `true` on a macOS host with a Kotlin/Native distribution.
 *
 * The Apple tooling of the plugin needs linked Kotlin/Native binaries, so it checks this and not [isMacOs].
 * An Intel Mac is macOS but unsupported, and its tasks stay skipped as they were with `HostManager.hostIsMac`.
 */
internal val KotlinNativeHost.isSupportedMacOs: Boolean
    get() = isMacOs && isSupported

/** Whether tests of the simulator [target] can run on this host: a supported Mac with the architecture of the target. */
internal fun KotlinNativeHost.canRunSimulatorTests(target: KonanTarget): Boolean =
    isSupportedMacOs && architecture == target.architecture.toHostArchitecture()

private fun Architecture.toHostArchitecture(): KotlinNativeHostArchitecture = when (this) {
    Architecture.X64 -> KotlinNativeHostArchitecture.X64
    Architecture.X86 -> KotlinNativeHostArchitecture.X86
    Architecture.ARM64 -> KotlinNativeHostArchitecture.ARM64
    Architecture.ARM32 -> KotlinNativeHostArchitecture.ARM32
}

/** Human-readable names of the supported hosts for diagnostics, e.g. `macOS (arm64)`. */
internal val supportedNativeHosts: List<String> = bundledNativeTargetSupport.supportedHosts.map { it.displayName }

internal val KotlinNativeHost.displayName: String
    get() {
        val osName = when (os) {
            KotlinNativeHostOs.MACOS -> "macOS"
            KotlinNativeHostOs.LINUX -> "Linux"
            KotlinNativeHostOs.WINDOWS -> "Windows"
            KotlinNativeHostOs.FREEBSD -> "FreeBSD"
            else -> "unknown OS"
        }
        val archName = when (architecture) {
            KotlinNativeHostArchitecture.X64 -> "x86_64"
            KotlinNativeHostArchitecture.ARM64 -> "arm64"
            KotlinNativeHostArchitecture.UNKNOWN -> "unknown architecture"
            else -> architecture.name.lowercase()
        }
        return "$osName ($archName)"
    }
