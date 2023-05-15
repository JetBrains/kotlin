/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.await

internal suspend fun Project.setupDefaultKotlinTargetHierarchy() {
    KotlinPluginLifecycle.Stage.FinaliseRefinesEdges.await()
    val extension = project.multiplatformExtensionOrNull ?: return

    /* User configured a target hierarchy explicitly: No need for our defaults here */
    if (extension.internalKotlinTargetHierarchy.appliedDescriptors.isNotEmpty()) return

    /*
    User manually added a .dependsOn:
    We fall back to the old behaviour and add the commonMain/commonTest default edges
     */
    if (extension.sourceSets.any { sourceSet -> sourceSet.dependsOn.isNotEmpty() }) {
        setupPreMultiplatformStableDefaultDependsOnEdges()
        return
    }


    /*
    Using a group of the 'defaultTargetHierarchy' as 'target name' will potentially lead to conflicts
    e.g.

    linuxX64("linux") or macosX64("native") can lead to confusion of for 'linuxMain' and 'nativeMain' SourceSets
     */
    if (!targetNamesAreCompatibleWithDefaultHierarchy()) {
        setupPreMultiplatformStableDefaultDependsOnEdges()
        return
    }

    extension.targetHierarchy.default()
}

private suspend fun Project.targetNamesAreCompatibleWithDefaultHierarchy(): Boolean {
    val targets = multiplatformExtension.awaitTargets()
    val targetNames = targets.map { it.name }.toSet()
    return multiplatformExtension.awaitTargets().flatMap { it.compilations }.none { compilation ->
        val hierarchy = defaultKotlinTargetHierarchy.buildKotlinTargetHierarchy(compilation) ?: return@none false
        val nodeNames = hierarchy.childrenClosure.mapNotNull { it.node as? KotlinTargetHierarchyTree.Node.Group }.map { it.name }.toSet()
        nodeNames.intersect(targetNames).isNotEmpty()
    }
}

/**
 * Before 1.9.20 (and without any targetHierarchy applied), we just added default dependsOn
 * edges from 'main' compilations defaultSourceSets to 'commonMain' and
 * edges from 'test' compilations defaultSourceSets to 'commonTest
 */
private fun Project.setupPreMultiplatformStableDefaultDependsOnEdges() {
    val extension = multiplatformExtension
    val commonMain = extension.sourceSets.getByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME)
    val commonTest = extension.sourceSets.getByName(KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME)

    extension.targets.all { target ->
        target.compilations.findByName(KotlinCompilation.MAIN_COMPILATION_NAME)?.let { mainCompilation ->
            mainCompilation.defaultSourceSet.takeIf { it != commonMain }?.dependsOn(commonMain)
        }

        target.compilations.findByName(KotlinCompilation.TEST_COMPILATION_NAME)?.let { testCompilation ->
            testCompilation.defaultSourceSet.takeIf { it != commonTest }?.dependsOn(commonTest)
        }
    }
}
