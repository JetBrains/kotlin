/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.konan.target

import org.jetbrains.kotlin.konan.target.KonanTarget.*
import org.jetbrains.kotlin.konan.util.Named

enum class Family(val exeSuffix:String, val dynamicPrefix: String, val dynamicSuffix: String) {
    OSX     ("kexe", "lib", "dylib"),
    IOS     ("kexe", "lib", "dylib"),
    LINUX   ("kexe", "lib", "so"   ),
    MINGW   ("exe" , ""   , "dll"  ),
    ANDROID ("so"  , "lib", "so"   ),
    WASM    ("wasm", ""   , "wasm" ),
    ZEPHYR  ("o"   , "lib", "a"    )
}

enum class Architecture(val bitness: Int) {
    X64(64),
    ARM64(64),
    ARM32(32),
    MIPS32(32),
    MIPSEL32(32),
    WASM32(32);
}

sealed class KonanTarget(override val name: String, val family: Family, val architecture: Architecture) : Named {
    object ANDROID_ARM32 :  KonanTarget( "android_arm32",   Family.ANDROID, Architecture.ARM32)
    object ANDROID_ARM64 :  KonanTarget( "android_arm64",   Family.ANDROID, Architecture.ARM64)
    object IOS_ARM64 :      KonanTarget( "ios_arm64",       Family.IOS,     Architecture.ARM64)
    object IOS_X64 :        KonanTarget( "ios_x64",         Family.IOS,     Architecture.X64)
    object LINUX_X64 :      KonanTarget( "linux_x64",       Family.LINUX,   Architecture.X64)
    object MINGW_X64 :      KonanTarget( "mingw_x64",       Family.MINGW,   Architecture.X64)
    object MACOS_X64 :      KonanTarget( "macos_x64",       Family.OSX,     Architecture.X64)
    object LINUX_ARM32_HFP :KonanTarget( "linux_arm32_hfp", Family.LINUX,   Architecture.ARM32)
    object LINUX_MIPS32 :   KonanTarget( "linux_mips32",    Family.LINUX,   Architecture.MIPS32)
    object LINUX_MIPSEL32 : KonanTarget( "linux_mipsel32",  Family.LINUX,   Architecture.MIPSEL32)
    object WASM32 :         KonanTarget( "wasm32",          Family.WASM,    Architecture.WASM32)

    // Tunable targets
    class ZEPHYR(val subName: String, val genericName: String = "zephyr") : KonanTarget("${genericName}_$subName", Family.ZEPHYR, Architecture.ARM32)
}

fun hostTargetSuffix(host: KonanTarget, target: KonanTarget) =
    if (target == host) host.name else "${host.name}-${target.name}"

enum class CompilerOutputKind {
    PROGRAM {
        override fun suffix(target: KonanTarget?) = ".${target!!.family.exeSuffix}"
    },
    DYNAMIC {
        override fun suffix(target: KonanTarget?) = ".${target!!.family.dynamicSuffix}"
        override fun prefix(target: KonanTarget?) = "${target!!.family.dynamicPrefix}"
    },
    FRAMEWORK {
        override fun suffix(target: KonanTarget?): String = ".framework"
    },
    LIBRARY {
        override fun suffix(target: KonanTarget?) = ".klib"
    },
    BITCODE {
        override fun suffix(target: KonanTarget?) = ".bc"
    };

    abstract fun suffix(target: KonanTarget? = null): String
    open fun prefix(target: KonanTarget? = null): String = ""
}

interface TargetManager {
    val target: KonanTarget
    val targetName : String
    fun list() : Unit
    val hostTargetSuffix: String
    val targetSuffix: String
}

private class TargetManagerImpl(val userRequest: String?, val hostManager: HostManager): TargetManager {
    override val target = determineCurrent()
    override val targetName
        get() = target.visibleName

    override fun list() {
        hostManager.enabled.forEach {
            val isDefault = if (it == target) "(default)" else ""
            val aliasList = HostManager.listAliases(it.visibleName).joinToString(", ")
            println(String.format("%1$-30s%2$-10s%3\$s", "${it.visibleName}:", "$isDefault", aliasList))
        }
    }

    fun determineCurrent(): KonanTarget {
        return if (userRequest == null || userRequest == "host") {
            HostManager.host
        } else {
            val resolvedAlias = HostManager.resolveAlias(userRequest)
            hostManager.targets[hostManager.known(resolvedAlias)]!!
        }
    }

    override val hostTargetSuffix get() = hostTargetSuffix(HostManager.host, target)
    override val targetSuffix get() = target.name
}

open class HostManager(protected val distribution: Distribution = Distribution()) {

    fun targetManager(userRequest: String? = null): TargetManager = TargetManagerImpl(userRequest, this)

    // TODO: need a better way to enumerated predefined targets.
    private val predefinedTargets = listOf(ANDROID_ARM32, ANDROID_ARM64, IOS_ARM64, IOS_X64, LINUX_X64, MINGW_X64, MACOS_X64, LINUX_ARM32_HFP, LINUX_MIPS32, LINUX_MIPSEL32, WASM32)

    private val zephyrSubtargets = distribution.availableSubTarget("zephyr").map { ZEPHYR(it) }

    private val configurableSubtargets = zephyrSubtargets

    val targetValues: List<KonanTarget> by lazy {
        predefinedTargets + configurableSubtargets
    }

    val targets = targetValues.associate{ it.visibleName to it }

    fun toKonanTargets(names: Iterable<String>): List<KonanTarget> {
        return names.map {
            if (it == "host") HostManager.host
            else targets[known(resolveAlias(it))]!!
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
        val target = targets[resolveAlias(name)]
        if (target == null) throw TargetSupportException("Unknown target name: $name")
        return target
    }

    val enabled: List<KonanTarget> by lazy { 
            when (host) {
                KonanTarget.LINUX_X64 -> listOf(
                    KonanTarget.LINUX_X64,
                    KonanTarget.LINUX_ARM32_HFP,
                    KonanTarget.LINUX_MIPS32,
                    KonanTarget.LINUX_MIPSEL32,
                    KonanTarget.ANDROID_ARM32,
                    KonanTarget.ANDROID_ARM64,
                    KonanTarget.WASM32
                ) + zephyrSubtargets
                KonanTarget.MINGW_X64 -> listOf(
                    KonanTarget.MINGW_X64,
                    KonanTarget.WASM32
                ) + zephyrSubtargets
                KonanTarget.MACOS_X64 -> listOf(
                    KonanTarget.MACOS_X64,
                    KonanTarget.IOS_ARM64,
                    KonanTarget.IOS_X64,
                    KonanTarget.ANDROID_ARM32,
                    KonanTarget.ANDROID_ARM64,
                    KonanTarget.WASM32
                ) + zephyrSubtargets
                else ->
                    throw TargetSupportException("Unknown host platform: $host")
            }        
    }

    fun isEnabled(target: KonanTarget) = enabled.contains(target)

    companion object {
        fun host_os(): String {
            val javaOsName = System.getProperty("os.name")
            return when {
                javaOsName == "Mac OS X" -> "osx"
                javaOsName == "Linux" -> "linux"
                javaOsName.startsWith("Windows") -> "windows"
                else -> throw TargetSupportException("Unknown operating system: ${javaOsName}")
            }
        }

        @JvmStatic
        fun simpleOsName(): String {
            val hostOs = host_os()
            return if (hostOs == "osx") "macos" else hostOs
        }

        val jniHostPlatformIncludeDir: String 
            get() = when(host) {
                KonanTarget.MACOS_X64 -> "darwin"
                KonanTarget.LINUX_X64 -> "linux"
                KonanTarget.MINGW_X64 ->"win32"
                else -> throw TargetSupportException("Unknown host: $host.")
            }

        fun host_arch(): String {
            val javaArch = System.getProperty("os.arch")
            return when (javaArch) {
                "x86_64" -> "x86_64"
                "amd64"  -> "x86_64"
                "arm64"  -> "arm64"
                else -> throw TargetSupportException("Unknown hardware platform: ${javaArch}")
            }
        }

        val host: KonanTarget = when (host_os()) {
            "osx"   -> KonanTarget.MACOS_X64
            "linux" -> KonanTarget.LINUX_X64
            "windows" -> KonanTarget.MINGW_X64
            else -> throw TargetSupportException("Unknown host target: ${host_os()} ${host_arch()}")
        }

        val hostIsMac   = (host == KonanTarget.MACOS_X64)
        val hostIsLinux = (host == KonanTarget.LINUX_X64)
        val hostIsMingw = (host == KonanTarget.MINGW_X64)

        val hostSuffix get() = host.name
        @JvmStatic
        val hostName get() = host.name

        val knownTargetTemplates = listOf("zephyr")

        private val targetAliasResolutions = mapOf(
                "linux"       to "linux_x64",
                "macbook"     to "macos_x64",
                "macos"       to "macos_x64",
                "imac"        to "macos_x64",
                "raspberrypi" to "linux_arm32_hfp",
                "iphone"      to "ios_arm64",
                "ipad"        to "ios_arm64",
                "ios"         to "ios_arm64",
                "iphone_sim"  to "ios_x64",
                "mingw"       to "mingw_x64"
        )

        private val targetAliases: Map<String, List<String>> by lazy {
            val result = mutableMapOf<String, MutableList<String>>()
            targetAliasResolutions.entries.forEach {
                result.getOrPut(it.value, { mutableListOf() } ).add(it.key)
            }
            result
        }

        fun resolveAlias(request: String): String = targetAliasResolutions[request] ?: request

        fun listAliases(target: String): List<String> = targetAliases[target] ?: emptyList()
    }
}

class TargetSupportException (message: String = "", cause: Throwable? = null) : Exception(message, cause)

