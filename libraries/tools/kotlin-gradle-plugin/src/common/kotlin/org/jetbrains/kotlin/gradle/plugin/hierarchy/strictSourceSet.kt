/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.hierarchy

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.kotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.sources.getVisibleSourceSetsFromAssociateCompilations
import org.jetbrains.kotlin.gradle.plugin.sources.internal

internal suspend fun KotlinSourceSet.awaitIsStrictSourceSet(): Boolean {
    project.multiplatformExtension.awaitSourceSets()
    return isStrictSourceSet()
}

/**
 * Compute if the source set is strict. A strict source set can see only "expected" fragments from dependencies and commonization result:
 *   - expected fragments are those we can see per GMT restricted further by default hierarchy heuristic. E.g. if this source set is
 *   commonMain, then it should only see commonMain from a dependency (even if it could also see nativeMain)
 *   - expected commonization result is that only nativeMain and its refiners can see commonization of platform libs, and they also see
 *   commonization as if all native targets for that source set were enabled. E.g. nativeMain sees the strictest possible commonization result
 *
 * A source set that is not in the default hierarchy is not strict and source sets that can see a non-strict source set in the same project
 * are also not strict.
 *
 * All checks are done by name, we don't check that the shape of the source set graph matches the default target hierarchy.
 */
internal fun KotlinSourceSet.isStrictSourceSet(): Boolean {
    return isStrictSourceSetWithoutAssociatedSourceSets() && getVisibleSourceSetsFromAssociateCompilations(this).all {
        it.isStrictSourceSetWithoutAssociatedSourceSets()
    }
}

internal fun KotlinSourceSet.isStrictSourceSetWithoutAssociatedSourceSets(): Boolean {
    if (!project.kotlinPropertiesProvider.strictKmpCompilation) return false
    if (name !in defaultTargetHierarchySourceSets) return false
    check(project.kotlinPluginLifecycle.stage >= KotlinPluginLifecycle.Stage.AfterFinaliseRefinesEdges)
    return internal.dependsOnClosure.all { it.name in defaultTargetHierarchySourceSets }
}

internal val defaultTargetHierarchySourceSets = setOf(
    "androidMain",
    "appleMain",
    "appleTest",
    "commonMain",
    "commonTest",
    "iosArm64Main",
    "iosArm64Test",
    "iosMain",
    "iosSimulatorArm64Main",
    "iosSimulatorArm64Test",
    "iosTest",
    "iosX64Main",
    "iosX64Test",
    "jsMain",
    "jsTest",
    "jvmMain",
    "jvmTest",
    "linuxArm64Main",
    "linuxArm64Test",
    "linuxMain",
    "linuxTest",
    "linuxX64Main",
    "linuxX64Test",
    "macosArm64Main",
    "macosArm64Test",
    "macosMain",
    "macosTest",
    "mingwMain",
    "mingwTest",
    "mingwX64Main",
    "mingwX64Test",
    "nativeMain",
    "nativeTest",
    "tvosArm64Main",
    "tvosArm64Test",
    "tvosMain",
    "tvosSimulatorArm64Main",
    "tvosSimulatorArm64Test",
    "tvosTest",
    "wasmJsMain",
    "wasmJsTest",
    "wasmWasiMain",
    "wasmWasiTest",
    "watchosArm64Main",
    "watchosArm64Test",
    "watchosDeviceArm64Main",
    "watchosDeviceArm64Test",
    "watchosMain",
    "watchosSimulatorArm64Main",
    "watchosSimulatorArm64Test",
    "watchosTest",
    "webMain",
    "webTest",
)
