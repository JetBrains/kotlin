/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.repository

import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.commonizer.konan.NativeLibrary
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.util.Logger
import org.jetbrains.kotlin.utils.associateByNotNull
import java.io.File

const val SUPPORT_LIB_PATH = "/Users/Nikolay.Lunyak/Documents/Projects/kotlin-worktrees/kotlin-platform-type-commonized-to-different-types/libraries/commonizer-support-library/build/classes/kotlin"

val SUPPORT_LIB_FILE = File(SUPPORT_LIB_PATH)
val SUPPORT_LIB_FILE_COMMON = SUPPORT_LIB_FILE.resolve("metadata")

val supportHierarchy = mapOf(
    "linuxArm64" to "linuxMain",
    "linuxX64" to "linuxMain",

    "iosX64" to "iosMain",
    "iosArm64" to "iosMain",
    "iosSimulatorArm64" to "iosMain",

    "macosX64" to "macosMain",
    "macosArm64" to "macosMain",

    "tvosX64" to "tvosMain",
    "tvosArm64" to "tvosMain",
    "tvosSimulatorArm64" to "tvosMain",

    "watchosX64" to "watchosMain",
    "watchosArm32" to "watchosMain",
    "watchosArm64" to "watchosMain",
    "watchosDeviceArm64" to "watchosMain",
    "watchosSimulatorArm64" to "watchosMain",

    "iosMain" to "appleMain",
    "macosMain" to "appleMain",
    "tvosMain" to "appleMain",
    "watchosMain" to "appleMain",

    "androidNativeX86" to "androidNativeMain",
    "androidNativeX64" to "androidNativeMain",
    "androidNativeArm32" to "androidNativeMain",
    "androidNativeArm64" to "androidNativeMain",

    "linuxMain" to "nativeMain",
    "appleMain" to "nativeMain",
    "mingwX64" to "nativeMain",
    "androidNativeMain" to "nativeMain",
)

class SupportHierarchyTarget(val name: String, var parent: SupportHierarchyTarget?, val targets: MutableList<SupportHierarchyTarget>) {
    override fun toString(): String = "SupportHierarchyTarget($name)"
}

fun buildSupportHierarchyTargets(): Map<String, SupportHierarchyTarget> {
    val supportHierarchyTargets = (supportHierarchy.keys.toSet() + supportHierarchy.values)
        .associateWith { SupportHierarchyTarget(it, null, mutableListOf()) }

    for ((key, value) in supportHierarchy) {
        supportHierarchyTargets[key]!!.parent = supportHierarchyTargets[value]!!
        supportHierarchyTargets[value]!!.targets.add(supportHierarchyTargets[key]!!)
    }

    return supportHierarchyTargets
}

fun Map<String, SupportHierarchyTarget>.toCommonizerTargets(): Map<SupportHierarchyTarget, CommonizerTarget> {
    val supportHierarchyTargetCache = mutableMapOf<SupportHierarchyTarget, CommonizerTarget>()
    val leafTargets = KonanTarget.predefinedTargets.mapKeys {
        val sanitized = it.key.replace("_", "").lowercase()

        when {
            sanitized.startsWith("android") -> sanitized.replace("android", "androidnative")
            else -> sanitized
        }
    }

    fun SupportHierarchyTarget.toCommonizerTarget(): CommonizerTarget {
        return supportHierarchyTargetCache.getOrPut(this) {
            val leaf = leafTargets[name.lowercase()]
            when {
                leaf != null -> LeafCommonizerTarget(leaf)
                else -> SharedCommonizerTarget(targets.flatMap { it.toCommonizerTarget().konanTargets })
            }
        }
    }
    return map { it.value to it.value.toCommonizerTarget() }.toMap()
}

internal fun loadSupportLibraries(logger: Logger): Map<String, NativeLibrary> {
    val supportLibSharedTargets = SUPPORT_LIB_FILE_COMMON.list { _, name -> name.endsWith("Main") } ?: return emptyMap()
    val supportNativeSharedLibraries = supportLibSharedTargets.associateWith {
        val file = SUPPORT_LIB_FILE_COMMON.resolve(it).resolve("klib").resolve("module1_$it")
        DefaultNativeLibraryLoader(logger).invoke(file)
    }

    val supportLibLeafTargets = SUPPORT_LIB_FILE.list { _, name ->
        name.endsWith("X64") || name.endsWith("Arm64") || name.endsWith("X32") || name.endsWith("Arm32") || name.endsWith("X86")
    } ?: return emptyMap()
    val supportNativeLeafLibraries = supportLibLeafTargets.associateWith {
        val file = SUPPORT_LIB_FILE.resolve(it).resolve("main").resolve("klib").resolve("module1")
        DefaultNativeLibraryLoader(logger).invoke(file)
    }

    return supportNativeSharedLibraries + supportNativeLeafLibraries
}

internal class CommonizerSupportLibraryRepository(val logger: Logger) : Repository {
    val libraries by lazy {
        val supportLibraries = loadSupportLibraries(logger)
        val supportHierarchyTargets = buildSupportHierarchyTargets()
        val supportHierarchyCommonizerTargets = supportHierarchyTargets.toCommonizerTargets()

        supportHierarchyTargets.entries.associateByNotNull(
            keySelector = { supportHierarchyCommonizerTargets[it.value] },
            valueTransform = { supportLibraries[it.key] },
        )
    }

    override fun getLibraries(target: CommonizerTarget): Set<NativeLibrary> =
        libraries.filterKeys { it.konanTargets.containsAll(target.konanTargets) }.values.toSet()
}
