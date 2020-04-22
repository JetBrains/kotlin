/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.LibraryVariant
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.api.UnitTestVariant
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/**
 * Builds an additional adjustment of dependsOn-relation for Android source-sets in HMPP
 *
 * If returned map contains a mapping (from, to), then a source-set with name 'from'
 * is considered to have an additional dependsOn-edge to source-set with name 'to'.
 *
 * The idea of this adjustment is to provide at least some dependsOn-edges from Android
 * source-sets to Kotlin source-sets. See https://youtrack.jetbrains.com/issue/KT-33809
 * for detailed explanation.
 */
@Suppress("UNUSED") // used via reflection in KotlinMppModelBuilder
internal fun buildDependsOnAdjustmentForAndroidSourceSets(project: Project): Map<String, String> {
    if ((project.findProperty("kotlin.mpp.enableGranularSourceSetsMetadata") as? String)?.toBoolean() != true) return emptyMap()

    val kotlinMppExtension = project.multiplatformExtensionOrNull ?: return emptyMap()
    val androidTarget = kotlinMppExtension.targets.singleOrNull { it.platformType == KotlinPlatformType.androidJvm } as KotlinAndroidTarget?
        ?: return emptyMap()

    val mainCompilations: MutableList<KotlinJvmAndroidCompilation> = mutableListOf()
    val unitTestCompilations: MutableList<KotlinJvmAndroidCompilation> = mutableListOf()
    val androidTestCompilations: MutableList<KotlinJvmAndroidCompilation> = mutableListOf()

    androidTarget.compilations.forEach {
        when (it.androidVariant) {
            is LibraryVariant, is ApplicationVariant -> mainCompilations += it
            is TestVariant -> androidTestCompilations += it
            is UnitTestVariant -> unitTestCompilations += it
        }
    }

    val rootMainSourceSet = mainCompilations.findRootSourceSets().singleOrLogError(mainCompilations) ?: return emptyMap()
    val rootUnitTestSourceSet = unitTestCompilations.findRootSourceSets().singleOrLogError(unitTestCompilations) ?: return emptyMap()

    // Android-tests are a little bit tricker: they don't have a release-compilation, so normally two source-sets
    // will participate in all compilations: androidTest and androidTestDebug
    val rootAndroidTestSourceSets = androidTestCompilations.findRootSourceSets().toList()
    if (rootAndroidTestSourceSets.size != 2) {
        rootAndroidTestSourceSets.logAmbigousRootSourceSets(androidTestCompilations)
        return emptyMap()
    }

    // Now we want to chose one -- it should be androidTest
    // Solution: assume that the name of less specific source-set is always a substring of a more specific one
    // (that's a hack, but more or less reasonable one)
    val firstRootSourceSet = rootAndroidTestSourceSets[0].name
    val secondRootSourceSet = rootAndroidTestSourceSets[1].name
    val actualRootAndroidTestSourceSetName = when {
        firstRootSourceSet in secondRootSourceSet -> firstRootSourceSet
        secondRootSourceSet in firstRootSourceSet -> secondRootSourceSet
        else -> {
            rootAndroidTestSourceSets.logAmbigousRootSourceSets(androidTestCompilations)
            return emptyMap()
        }
    }

    return mapOf(
        rootMainSourceSet.name to KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME,
        rootUnitTestSourceSet.name to KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME,
        actualRootAndroidTestSourceSetName to KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME
    )
}

// Finds non-trivial (i.e. excluding commonMain/commonTest) source-sets which participate in each passed compilation
private fun Collection<KotlinJvmAndroidCompilation>.findRootSourceSets(): Set<KotlinSourceSet> {
    return map { it.allKotlinSourceSets.toSet() }
        .intersectSets()
        .filterTo(mutableSetOf()) {
            it.name != KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME && it.name != KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME
        }
}

private fun Set<KotlinSourceSet>.singleOrLogError(resultedFromCompilations: Collection<KotlinJvmAndroidCompilation>): KotlinSourceSet? =
    singleOrNull().also { if (it == null) logAmbigousRootSourceSets(resultedFromCompilations) }

private fun Collection<KotlinSourceSet>.logAmbigousRootSourceSets(resultedFromCompilations: Collection<KotlinJvmAndroidCompilation>) {
    val logger = Logging.getLogger(KotlinAndroidTarget::class.java)
    logger.kotlinDebug(
        "ERROR. Couldn't find a single source-set, instead got: $this\n" +
                "Resulted from:\n" +
                resultedFromCompilations.joinToString(separator = "\n") {
                    it.toString() + ", source-sets = " + it.allKotlinSourceSets.joinToString(prefix = "[", postfix = "]", separator = ", ")
                }
    )
}

private fun <T> Collection<Set<T>>.intersectSets(): Set<T> {
    if (isEmpty()) return emptySet()
    if (size == 1) return single()
    return reduce { acc, other -> acc.intersect(other) }
}