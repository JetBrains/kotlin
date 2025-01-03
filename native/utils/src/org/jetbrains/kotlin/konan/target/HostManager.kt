/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

import org.jetbrains.kotlin.konan.target.KonanTarget.*
import java.lang.Exception

@Suppress("DEPRECATION") // Uses deprecated SubTargetProvider in other deprecated APIs
open class HostManager() {
    fun targetManager(userRequest: String? = null): TargetManager = TargetManagerImpl(userRequest, this)

    @Suppress("UNUSED_PARAMETER")
    @Deprecated(level = DeprecationLevel.HIDDEN, message = "Kept for binary compatibility of Gradle plugins")
    constructor(
        subTargetProvider: SubTargetProvider = SubTargetProvider.NoSubTargets,
        experimental: Boolean = false
    ) : this()

    @Suppress("UNUSED_PARAMETER")
    @Deprecated(level = DeprecationLevel.HIDDEN, message = "Kept for binary compatibility of Gradle plugins")
    constructor(
        distribution: Distribution,
        experimental: Boolean = false
    ) : this()

    val targetValues: List<KonanTarget> by lazy { KonanTarget.predefinedTargets.values.toList() }

    val targets = targetValues.associateBy { it.visibleName }

    fun toKonanTargets(names: Iterable<String>): List<KonanTarget> {
        return names.map {
            if (it == "host") host else targets.getValue(known(resolveAlias(it)))
        }
    }

    fun known(name: String): String {
        if (targets[name] == null) {
            throw TargetSupportException("Unknown target: $name. Use -list_targets to see the list of available targets")
        }
        return name
    }

    fun targetByName(name: String): KonanTarget {
        if (name == "host") return host
        return targets[resolveAlias(name)] ?: throw TargetSupportException("Unknown target name: $name")
    }

    private val commonTargets = setOf(
        LINUX_X64,
        LINUX_ARM32_HFP,
        LINUX_ARM64,
        MINGW_X64,
        ANDROID_X86,
        ANDROID_X64,
        ANDROID_ARM32,
        ANDROID_ARM64,
    )

    private val appleTargets = setOf(
        MACOS_X64,
        MACOS_ARM64,
        IOS_ARM64,
        IOS_X64,
        IOS_SIMULATOR_ARM64,
        WATCHOS_ARM32,
        WATCHOS_ARM64,
        WATCHOS_X64,
        WATCHOS_SIMULATOR_ARM64,
        WATCHOS_DEVICE_ARM64,
        TVOS_ARM64,
        TVOS_X64,
        TVOS_SIMULATOR_ARM64,
    )

    val enabledByHost: Map<KonanTarget, Set<KonanTarget>> = mapOf(
        LINUX_X64 to commonTargets,
        LINUX_ARM64 to commonTargets,
        MINGW_X64 to commonTargets,
        MACOS_X64 to commonTargets + appleTargets,
        MACOS_ARM64 to commonTargets + appleTargets
    )

    val enabled: List<KonanTarget>
        get() = enabledByHost[host]?.toList() ?: throw TargetSupportException("Unknown host platform: $host")

    fun isEnabled(target: KonanTarget) = enabled.contains(target)

    companion object {
        @Deprecated("Use `hostOs` instead", ReplaceWith("HostManager.hostOs()"))
        fun host_os(): String =
            hostOs()

        fun hostOs(): String {
            val javaOsName = System.getProperty("os.name")
            return when {
                javaOsName == "Mac OS X" -> "osx"
                javaOsName == "Linux" -> "linux"
                javaOsName.startsWith("Windows") -> "windows"
                else -> throw TargetSupportException("Unknown operating system: $javaOsName")
            }
        }

        @JvmStatic
        fun simpleOsName(): String {
            val hostOs = hostOs()
            return if (hostOs == "osx") "macos" else hostOs
        }

        @JvmStatic
        fun platformName(): String {
            val hostOs = hostOs()
            val arch = hostArch()
            return when (hostOs) {
                "osx" -> "macos-$arch"
                else -> "$hostOs-$arch"
            }
        }

        val jniHostPlatformIncludeDir: String
            get() = when (host) {
                MACOS_X64,
                MACOS_ARM64 -> "darwin"
                LINUX_X64 -> "linux"
                MINGW_X64 -> "win32"
                else -> throw TargetSupportException("Unknown host: $host.")
            }

        @Deprecated("Use `hostArch` instead", ReplaceWith("HostManager.hostArch()"))
        fun host_arch(): String =
            hostArch()

        fun hostArch(): String =
            hostArchOrNull()
                ?: throw TargetSupportException("Unknown hardware platform: ${System.getProperty("os.arch")}")

        fun hostArchOrNull(): String? =
            when (System.getProperty("os.arch")) {
                "x86_64" -> "x86_64"
                "amd64" -> "x86_64"
                "arm64" -> "aarch64"
                "aarch64" -> "aarch64"
                else -> null
            }

        private val hostMapping: Map<Pair<String, String>, KonanTarget> = mapOf(
            Pair("osx", "x86_64") to MACOS_X64,
            Pair("osx", "aarch64") to MACOS_ARM64,
            Pair("linux", "x86_64") to LINUX_X64,
            Pair("linux", "aarch64") to LINUX_ARM64,
            Pair("windows", "x86_64") to MINGW_X64
        )

        val host: KonanTarget = determineHost(hostOs(), hostArchOrNull())

        private fun determineHost(os: String, arch: String?): KonanTarget {
            hostMapping[os to arch]?.let {
                return it
            }
            // https://youtrack.jetbrains.com/issue/KT-48566.
            // Workaround for unsupported host architectures.
            // It is obviously incorrect, but makes Gradle plugin work.
            hostMapping.entries.firstOrNull { (host, _) -> host.first == os }?.let {
                return it.value
            }
            throw TargetSupportException("Unknown host target: $os $arch")
        }

        // Note Hotspot-specific VM option enforcing C1-only, critical for decent compilation speed.
        val defaultJvmArgs = listOf("-XX:TieredStopAtLevel=1", "-ea", "-Dfile.encoding=UTF-8")
        val regularJvmArgs = defaultJvmArgs + "-Xmx3G"

        val hostIsMac = (host.family == Family.OSX)
        val hostIsLinux = (host.family == Family.LINUX)
        val hostIsMingw = (host.family == Family.MINGW)

        @JvmStatic
        val hostName: String
            get() = host.name

        private val targetAliasResolutions = mapOf(
            "linux" to "linux_x64",
            "macbook" to "macos_x64",
            "macos" to "macos_x64",
            "imac" to "macos_x64",
            "raspberrypi" to "linux_arm32_hfp",
            "iphone32" to "ios_arm32",
            "iphone" to "ios_arm64",
            "ipad" to "ios_arm64",
            "ios" to "ios_arm64",
            "iphone_sim" to "ios_x64",
            "mingw" to "mingw_x64"
        )

        private val targetAliases: Map<String, List<String>> by lazy {
            val result = mutableMapOf<String, MutableList<String>>()
            targetAliasResolutions.entries.forEach {
                result.getOrPut(it.value, { mutableListOf() }).add(it.key)
            }
            result
        }

        fun resolveAlias(request: String): String = targetAliasResolutions[request] ?: request

        fun listAliases(target: String): List<String> = targetAliases[target] ?: emptyList()
    }
}

class TargetSupportException(message: String = "", cause: Throwable? = null) : Exception(message, cause)
