/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resourcesPublicationExtension
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.utils.androidExtension
import org.junit.Test
import java.io.File

class KotlinAndroidTargetResourcesPublicationTests {

    private val Project.expectedReleaseResourcePath
        get() = layout.buildDirectory.dir(
            "kotlin-multiplatform-resources/assemble-hierarchically/androidReleaseResources"
        ).get().asFile

    private val Project.expectedDebugResourcePath
        get() = layout.buildDirectory.dir(
            "kotlin-multiplatform-resources/assemble-hierarchically/androidDebugResources"
        ).get().asFile

    private val Project.expectedDemoReleaseResourcePath
        get() = layout.buildDirectory.dir(
            "kotlin-multiplatform-resources/assemble-hierarchically/androidDemoReleaseResources"
        ).get().asFile

    private val Project.expectedFullReleaseResourcePath
        get() = layout.buildDirectory.dir(
            "kotlin-multiplatform-resources/assemble-hierarchically/androidFullReleaseResources"
        ).get().asFile

    @Test
    fun `test publishing android resources - reflects in android resource directories`() {
        val project = mppProjectWithAndroidTarget()

        project.evaluate()
        project.publishFakeResources(project.multiplatformExtension.androidTarget())
        project.publishFakeAssets(project.multiplatformExtension.androidTarget())

        val androidVariantSourceSets = project.multiplatformExtension.androidTarget()
            .compilations.getByName("release")
            .androidVariant.sourceSets
        val actualResourcesDirectories = androidVariantSourceSets.flatMapTo(linkedSetOf()) { it.resourcesDirectories }

        assert(
            actualResourcesDirectories.contains(project.expectedReleaseResourcePath)
        ) { actualResourcesDirectories.dumpPaths() }
        assert(
            !actualResourcesDirectories.contains(project.expectedDebugResourcePath)
        ) { actualResourcesDirectories.dumpPaths() }
    }

    @Test
    fun `test publishing android resources with flavors - reflects in android resource directories`() {
        val project = mppProjectWithAndroidTarget {
            val dimension = "version"
            flavorDimensions += dimension
            productFlavors {
                with(create("demo")) { this.dimension = dimension }
                with(create("full")) { this.dimension = dimension }
            }
        }

        project.evaluate()
        project.publishFakeResources(project.multiplatformExtension.androidTarget())

        val androidVariantSourceSets = project.multiplatformExtension.androidTarget()
            .compilations.getByName("demoRelease")
            .androidVariant.sourceSets
        val actualResourcesDirectories = androidVariantSourceSets.flatMapTo(linkedSetOf()) {
            it.resourcesDirectories
        }

        assert(
            actualResourcesDirectories.contains(project.expectedDemoReleaseResourcePath)
        ) { actualResourcesDirectories.dumpPaths() }
        assert(
            !actualResourcesDirectories.contains(project.expectedFullReleaseResourcePath)
        ) { actualResourcesDirectories.dumpPaths() }
        assert(
            !actualResourcesDirectories.contains(project.expectedReleaseResourcePath)
        ) { actualResourcesDirectories.dumpPaths() }
    }

    @Test
    fun `test publishing android resources - doesn't show up in resources - when there is no publication`() {
        val project = mppProjectWithAndroidTarget()

        project.evaluate()

        val actualSourcesDirectories = project.multiplatformExtension.androidTarget()
            .compilations.getByName("release")
            .androidVariant.sourceSets.flatMapTo(linkedSetOf()) { it.resourcesDirectories }

        assert(
            !actualSourcesDirectories.contains(project.expectedReleaseResourcePath)
        ) { actualSourcesDirectories.dumpPaths() }
    }

    private fun mppProjectWithAndroidTarget(
        configure: LibraryExtension.(Unit) -> (Unit) = { },
    ): ProjectInternal {
        val project = buildProjectWithMPP {
            plugins.apply("com.android.library")
            kotlin {
                androidTarget()
            }
        }
        val libraryExt = (project.extensions.getByName("android") as LibraryExtension)
        libraryExt.compileSdk = 30
        libraryExt.configure(Unit)
        return project
    }

    private fun Project.publishFakeResources(target: KotlinTarget) {
        project.multiplatformExtension.resourcesPublicationExtension?.publishResourcesAsKotlinComponent(
            target,
            resourcePathForSourceSet = {
                KotlinTargetResourcesPublication.ResourceRoot(
                    project.provider { File(it.name) },
                    emptyList(),
                    emptyList(),
                )
            },
            relativeResourcePlacement = project.provider { File("test") },
        )
    }

    private fun Project.publishFakeAssets(target: KotlinAndroidTarget) {
        project.multiplatformExtension.resourcesPublicationExtension?.publishInAndroidAssets(
            target,
            resourcePathForSourceSet = {
                KotlinTargetResourcesPublication.ResourceRoot(
                    project.provider { File(it.name) },
                    emptyList(),
                    emptyList(),
                )
            },
            relativeResourcePlacement = project.provider { File("test") },
        )
    }

    private fun Collection<File>.dumpPaths(): String {
        return "Actual paths:" + if (isEmpty()) " <Empty>" else joinToString("") { "\n${it}" }
    }

}