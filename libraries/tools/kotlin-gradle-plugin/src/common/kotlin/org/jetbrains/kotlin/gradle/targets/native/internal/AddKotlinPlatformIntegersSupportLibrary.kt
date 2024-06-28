/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.internal.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.internal.PLATFORM_INTEGERS_SUPPORT_LIBRARY
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.sources.getVisibleSourceSetsFromAssociateCompilations
import org.jetbrains.kotlin.gradle.plugin.sources.internal

internal val AddKotlinPlatformIntegersSupportLibrary = KotlinProjectSetupCoroutine {
    if (!isPlatformIntegerCommonizationEnabled) return@KotlinProjectSetupCoroutine
    val kotlin = multiplatformExtensionOrNull ?: return@KotlinProjectSetupCoroutine
    val sourceSets = kotlin.awaitSourceSets()

    /**
     * Calculate roots of native source sets
     * Imagine following structure:
     *
     * ```
     *      common
     *   /   |        \
     *  jvm  linux     ios
     *       /   |     |     \
     *    lX64 lArm64  iosX64 iosArm64
     * ```
     *
     * In the structure above following source sets are native: linux, ios, lX64, lArm64, iosX64, iosArm64
     * But roots of the native source set hierarchies are only linux and ios. I.e. they don't depend on any other
     * native source set. The code below calculates exactly that:
     */
    val nativeSourceSets = sourceSets.filter { sourceSet -> sourceSet.internal.commonizerTarget.await() != null }
    val nativeSourceSetsRoots = nativeSourceSets.filter { sourceSet ->
        val allVisibleSourceSets = sourceSet.dependsOn + getVisibleSourceSetsFromAssociateCompilations(sourceSet)
        allVisibleSourceSets.none { dependency ->
            dependency in nativeSourceSets
        }
    }

    nativeSourceSetsRoots.forEach { sourceSet ->
        dependencies.add(
            sourceSet.implementationConfigurationName,
            "$KOTLIN_MODULE_GROUP:$PLATFORM_INTEGERS_SUPPORT_LIBRARY:${getKotlinPluginVersion()}"
        )
    }
}
