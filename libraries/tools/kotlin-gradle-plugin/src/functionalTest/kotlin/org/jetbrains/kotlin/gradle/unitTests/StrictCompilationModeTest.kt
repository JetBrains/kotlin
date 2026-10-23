/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.commonizer.konanTargets
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.mpp.GranularMetadataTransformation
import org.jetbrains.kotlin.gradle.plugin.sources.getVisibleSourceSetsFromAssociateCompilations
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.plugin.sources.isSharedSourceSet
import org.jetbrains.kotlin.gradle.targets.metadata.isNativeSourceSet
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizerTarget
import org.jetbrains.kotlin.gradle.targets.native.internal.defaultTargetHierarchyCommonizationTargetBySourceSet
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.utils.future
import org.jetbrains.kotlin.konan.target.KonanTarget
import kotlin.test.Test
import kotlin.test.assertEquals

class StrictCompilationModeTest {

//    @Test
//    fun `strict compilation mode - coroutines transformation`() {
//        val a = buildProjectWithMPP(preApplyCode = {
//            propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_KMP_STRICT_COMPILATION, "true")
//        }) {
//            multiplatformExtension.apply {
//                iosSimulatorArm64()
//                iosArm64()
//
//                sourceSets.commonMain.dependencies {
//                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
//                }
//            }
//        }.evaluate().multiplatformExtension.iosSimulatorArm64().compilations.getByName("main")
//            .compileTaskProvider.get().multiplatformStructure
//            .fragments.get()
//    }

    @Test
    fun `precomputed SS exclusion map and commonization targets - are up to date with all targets`() {
        // FIXME: disable strict compilation
        buildProjectWithMPP {
            plugins.apply("com.android.kotlin.multiplatform.library")
            multiplatformExtension.apply {
                targets.withType(KotlinMultiplatformAndroidLibraryTarget::class.java).configureEach { target ->
                    target.compileSdk = 34
                    target.namespace = "org.foo"
                }

                val excludedTargets = setOf("androidTarget")
                val nonDeprecatedTargetConstructors = multiplatformExtension::class.java.methods.filter {
                    it.parameterCount == 0
                            && KotlinTarget::class.java.isAssignableFrom(it.returnType)
                            // Maybe?
                            && it.getAnnotationsByType(Deprecated::class.java).isEmpty()
                            && it.name !in excludedTargets
                }
                assertEquals(
                    setOf(
                        "iosArm64",
                        "iosSimulatorArm64",
                        "iosX64",
                        "js",
                        "jvm",
                        "linuxArm64",
                        "linuxX64",
                        "macosArm64",
                        "mingwX64",
                        "tvosArm64",
                        "tvosSimulatorArm64",
                        "wasmJs",
                        "wasmWasi",
                        "watchosArm64",
                        "watchosDeviceArm64",
                        "watchosSimulatorArm64",
                    ).prettyPrinted,
                    nonDeprecatedTargetConstructors.map { it.name }.toSet().prettyPrinted
                )
                nonDeprecatedTargetConstructors.forEach {
                    it(multiplatformExtension)
                }

                val exclusionMap = project.future {
                    computeExclusionMap()
                }
                val commonizerTargetMap = project.future {
                    computeCommonizerTargets()
                }
                (project as ProjectInternal).evaluate()
                assertEquals(
                    exclusionMap.getOrThrow().map {
                        it.key.name to it.value.map { it.name }.toSet()
                    }.toMap().prettyPrinted,
                    GranularMetadataTransformation.defaultTargetHierarchyExcludedSourceSetVisibilityBySourceSet.prettyPrinted,
                )
                assertEquals(
                    commonizerTargetMap.getOrThrow().map {
                        it.key.name to it.value.map { it.name }
                    }.toMap().prettyPrinted,
                    defaultTargetHierarchyCommonizationTargetBySourceSet.prettyPrinted,
                )
            }
        }
    }

    suspend fun KotlinMultiplatformExtension.computeExclusionMap(): Map<KotlinSourceSet, Set<KotlinSourceSet>> {
        KotlinPluginLifecycle.Stage.ReadyForExecution.await()
        val extension = this
        val excludedSourceSetVisibilityBySourceSet = mutableMapOf<KotlinSourceSet, MutableSet<KotlinSourceSet>>()

        extension.sourceSets.forEach { sourceSet ->
            sourceSet.internal.dependsOnClosure.forEach { dependencySourceSet ->
                excludedSourceSetVisibilityBySourceSet.getOrPut(dependencySourceSet, { mutableSetOf() }).add(sourceSet)
            }
        }

        /**
         * Use [getVisibleSourceSetsFromAssociateCompilations] as a crude heuristic to see which SS are test
         */
        extension.sourceSets.filter { it.internal.isSharedSourceSet() }.forEach { potentialTestSourceSet ->
            val associatedVisibleSourceSets = getVisibleSourceSetsFromAssociateCompilations(potentialTestSourceSet)
            if (associatedVisibleSourceSets.isNotEmpty()) {
                excludedSourceSetVisibilityBySourceSet[potentialTestSourceSet] = associatedVisibleSourceSets.fold(
                    excludedSourceSetVisibilityBySourceSet[associatedVisibleSourceSets.first()]!! as Set<KotlinSourceSet>
                ) { acc, sourceSet -> acc.intersect(excludedSourceSetVisibilityBySourceSet[sourceSet]!!) }.toMutableSet()
            }
        }
        return excludedSourceSetVisibilityBySourceSet
    }

    suspend fun KotlinMultiplatformExtension.computeCommonizerTargets(): Map<KotlinSourceSet, Set<KonanTarget>> {
        KotlinPluginLifecycle.Stage.ReadyForExecution.await()
        return sourceSets.filter { it.internal.isSharedSourceSet() }.map {
            if (it.isNativeSourceSet.getOrThrow()) {
                it to it.commonizerTarget.getOrThrow()!!.konanTargets
            } else {
                // E.g. make sure commonMain doesn't have a commonization target
                it to emptySet()
            }
        }.toMap()
    }

}
