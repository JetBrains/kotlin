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
