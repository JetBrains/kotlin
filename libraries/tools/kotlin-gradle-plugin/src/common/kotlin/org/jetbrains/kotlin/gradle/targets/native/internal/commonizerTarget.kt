/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.LeafCommonizerTarget
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.commonizer.allLeaves
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.hierarchy.awaitIsStrictSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.sources.awaitPlatformCompilations
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.utils.Future
import org.jetbrains.kotlin.gradle.utils.future
import org.jetbrains.kotlin.gradle.utils.extrasStoredFuture
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.tooling.core.UnsafeApi

internal val KotlinSourceSet.commonizerTarget: Future<CommonizerTarget?> by extrasStoredFuture {
    if (awaitIsStrictSourceSet()) {
        val names = defaultTargetHierarchyCommonizationTargetBySourceSet[this.name] ?: return@extrasStoredFuture inferCommonizerTarget(internal.awaitPlatformCompilations())
        if (names.isEmpty()) {
            null
        } else {
            CommonizerTarget(names.map { KonanTarget.predefinedTargets[it]!! })
        }
    } else {
        inferCommonizerTarget(internal.awaitPlatformCompilations())
    }
}

internal val KotlinSourceSet.sharedCommonizerTarget: Future<SharedCommonizerTarget?>
    get() = project.future { commonizerTarget.await() as? SharedCommonizerTarget }

internal val KotlinCompilation<*>.commonizerTarget: Future<CommonizerTarget?> by extrasStoredFuture {
    KotlinPluginLifecycle.Stage.AfterFinaliseRefinesEdges.await()
    @OptIn(UnsafeApi::class)
    inferCommonizerTarget(this)
}

@UnsafeApi("Use sourceSet.commonizerTarget instead")
internal fun inferCommonizerTarget(sourceSet: KotlinSourceSet): CommonizerTarget? {
    val platformCompilations = sourceSet.internal.compilations
        .filter { compilation -> compilation !is KotlinMetadataCompilation }
    return inferCommonizerTarget(platformCompilations)
}


private fun inferCommonizerTarget(compilations: Iterable<KotlinCompilation<*>>): CommonizerTarget? {
    @OptIn(UnsafeApi::class)
    val allCompilationLeafTargets = compilations
        .filter { compilation -> compilation !is KotlinMetadataCompilation }
        .map { compilation -> inferCommonizerTarget(compilation) ?: return null }
        .allLeaves()

    return when {
        allCompilationLeafTargets.isEmpty() -> null
        allCompilationLeafTargets.size == 1 -> allCompilationLeafTargets.single()
        else -> SharedCommonizerTarget(allCompilationLeafTargets)
    }
}

@UnsafeApi("Use compilation.commonizerTarget instead")
internal fun inferCommonizerTarget(compilation: KotlinCompilation<*>): CommonizerTarget? {
    if (compilation is KotlinNativeCompilation) {
        return LeafCommonizerTarget(compilation.konanTarget)
    }

    if (compilation is KotlinSharedNativeCompilation) {
        return inferCommonizerTarget(compilation.defaultSourceSet) ?: CommonizerTarget(compilation.konanTargets)
    }

    return null
}

internal val defaultTargetHierarchyCommonizationTargetBySourceSet: Map<String, List<String>> = mapOf(
    "appleMain" to listOf(
        "ios_arm64",
        "ios_simulator_arm64",
        "ios_x64",
        "macos_arm64",
        "tvos_arm64",
        "tvos_simulator_arm64",
        "watchos_arm64",
        "watchos_device_arm64",
        "watchos_simulator_arm64",
    ),
    "appleTest" to listOf(
        "ios_arm64",
        "ios_simulator_arm64",
        "ios_x64",
        "macos_arm64",
        "tvos_arm64",
        "tvos_simulator_arm64",
        "watchos_arm64",
        "watchos_device_arm64",
        "watchos_simulator_arm64",
    ),
    "commonMain" to listOf(
    ),
    "commonTest" to listOf(
    ),
    "iosMain" to listOf(
        "ios_arm64",
        "ios_simulator_arm64",
        "ios_x64",
    ),
    "iosTest" to listOf(
        "ios_arm64",
        "ios_simulator_arm64",
        "ios_x64",
    ),
    "linuxMain" to listOf(
        "linux_arm64",
        "linux_x64",
    ),
    "linuxTest" to listOf(
        "linux_arm64",
        "linux_x64",
    ),
    "nativeMain" to listOf(
        "ios_arm64",
        "ios_simulator_arm64",
        "ios_x64",
        "linux_arm64",
        "linux_x64",
        "macos_arm64",
        "mingw_x64",
        "tvos_arm64",
        "tvos_simulator_arm64",
        "watchos_arm64",
        "watchos_device_arm64",
        "watchos_simulator_arm64",
    ),
    "nativeTest" to listOf(
        "ios_arm64",
        "ios_simulator_arm64",
        "ios_x64",
        "linux_arm64",
        "linux_x64",
        "macos_arm64",
        "mingw_x64",
        "tvos_arm64",
        "tvos_simulator_arm64",
        "watchos_arm64",
        "watchos_device_arm64",
        "watchos_simulator_arm64",
    ),
    "tvosMain" to listOf(
        "tvos_arm64",
        "tvos_simulator_arm64",
    ),
    "tvosTest" to listOf(
        "tvos_arm64",
        "tvos_simulator_arm64",
    ),
    "watchosMain" to listOf(
        "watchos_arm64",
        "watchos_device_arm64",
        "watchos_simulator_arm64",
    ),
    "watchosTest" to listOf(
        "watchos_arm64",
        "watchos_device_arm64",
        "watchos_simulator_arm64",
    ),
    "webMain" to listOf(
    ),
    "webTest" to listOf(
    ),
)
