/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nativeDistribution

import org.gradle.api.Project
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.isDirectory

private const val ROOT_PROPERTY_NAME = "kotlin.native.llvm"
private val supportedHosts = listOf(KonanTarget.LINUX_X64, KonanTarget.MACOS_ARM64, KonanTarget.MACOS_X64, KonanTarget.MINGW_X64)

/**
 * Which components are contained in LLVM distribution:
 * - [DEV] - almost every component;
 * - [ESSENTIALS] - only necessary for the Native distribution
 *
 * *NOTE*: `kotlin-native/konan/konan.properties` uses [DEV], because it's required to build the Native compiler itself.
 */
enum class LLVMDistributionKind {
    /**
     * Includes (almost) every possible component.
     *
     * Suitable for building/testing Native compiler itself.
     */
    DEV,

    /**
     * Includes only necessary components.
     *
     * Suitable for the Native distribution.
     */
    ESSENTIALS,
}

data class RemoteLLVMDistribution(val host: KonanTarget, val kind: LLVMDistributionKind, val address: String, val version: String) {
    val name: String
        get() = address.substringAfterLast("/")

    val dir: String
        get() = address.substringBeforeLast("/")
}

sealed interface LLVMDistributionSource {
    class Default(val distributions: List<RemoteLLVMDistribution>) : LLVMDistributionSource
    class Next(val distributions: List<RemoteLLVMDistribution>) : LLVMDistributionSource
    class Local(val path: Path, val version: String) : LLVMDistributionSource
}

private fun Project.getRemoteLLVMDistribution(source: String, kind: LLVMDistributionKind, host: KonanTarget): RemoteLLVMDistribution? {
    val addressPropertyName = "${ROOT_PROPERTY_NAME}.${source}.${host.name.lowercase()}.${kind.name.lowercase()}"
    val address = findProperty(addressPropertyName) as String?
    if (address == null) {
        logger.warn("Missing the $source LLVM distribution ($kind) for $host. Specify it with $addressPropertyName")
        return null
    }
    val versionPropertyName = "${ROOT_PROPERTY_NAME}.${source}.${host.name.lowercase()}.version"
    val version = findProperty(versionPropertyName) as String?
    if (version == null) {
        logger.warn("Missing the $source LLVM distribution version for $host. Specify it with $versionPropertyName")
        return null
    }
    return RemoteLLVMDistribution(host, kind, address, version)
}

private fun Project.getRemoteLLVMDistributions(source: String): List<RemoteLLVMDistribution> {
    return supportedHosts.flatMap { host ->
        LLVMDistributionKind.values().mapNotNull { kind ->
            getRemoteLLVMDistribution(source, kind, host)
        }
    }
}

/**
 * Reads `kotlin.native.llvm` property to determine which LLVM to use.
 * Possible options:
 * - `default`
 * - `next`
 * - absolute path to the locally built LLVM distribution
 */
val Project.llvmDistributionSource: LLVMDistributionSource
    get() {
        val llvmProperty = property(ROOT_PROPERTY_NAME) as String
        return when (llvmProperty) {
            "default" -> LLVMDistributionSource.Default(getRemoteLLVMDistributions("default"))
            "next" -> LLVMDistributionSource.Next(getRemoteLLVMDistributions("next"))
            else -> {
                val path = Paths.get(llvmProperty)
                check(path.isAbsolute) {
                    "Path to the local LLVM distribution must be absolute. $ROOT_PROPERTY_NAME=`$path`"
                }
                check(path.isDirectory()) {
                    "The local LLVM distribution must be a directory. $ROOT_PROPERTY_NAME=`$path`"
                }
                val version = property("${ROOT_PROPERTY_NAME}.next.${HostManager.host.name}.version") as String
                LLVMDistributionSource.Local(path, version)
            }
        }
    }

val LLVMDistributionKind.nameForProperties: String
    get() = when (this) {
        LLVMDistributionKind.DEV -> "dev"
        LLVMDistributionKind.ESSENTIALS -> "user"
    }

private val List<RemoteLLVMDistribution>.remoteAsProperties: Map<String, String>
    get() = buildMap {
        put("predefinedLlvmDistributions", this@remoteAsProperties.joinToString(separator = " ") { it.name })
        val versionPerHost = mutableMapOf<String, String>()
        this@remoteAsProperties.forEach {
            put("${it.name}.default", it.dir)
            put("llvm.${it.host}.${it.kind.nameForProperties}", it.name)
            versionPerHost[it.host.name] = it.version
        }
        versionPerHost.forEach { (host, version) ->
            put("llvmVersion.$host", version)
        }
    }

private val LLVMDistributionSource.Local.localAsProperties: Map<String, String>
    get() = buildMap {
        val host = HostManager.host
        val absolutePath = path.absolutePathString()
        LLVMDistributionKind.values().forEach { kind ->
            put("llvm.$host.${kind.nameForProperties}", absolutePath)
        }
        put("llvmVersion.$host", version)
    }

/**
 * Convert the source to [Map] compatible with `konan.properties`
 */
val LLVMDistributionSource.asProperties: Map<String, String>
    get() = when (this) {
        is LLVMDistributionSource.Local -> this.localAsProperties
        is LLVMDistributionSource.Default -> this.distributions.remoteAsProperties
        is LLVMDistributionSource.Next -> this.distributions.remoteAsProperties
    }.toMap() // toMap() is required to work with Gradle/Java serialization for some reason.