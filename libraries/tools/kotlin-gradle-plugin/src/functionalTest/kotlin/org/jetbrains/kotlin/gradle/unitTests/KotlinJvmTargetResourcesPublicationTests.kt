/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resourcesPublicationExtension
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.Test
import java.io.File

class KotlinJvmTargetResourcesPublicationTests {

    private val Project.expectedResourcePath get() = layout.buildDirectory.dir(
        "kotlin-multiplatform-resources/assemble-hierarchically/jvm"
    ).get().asFile

    @Test
    fun `test publishing jvm resources - reflects in jvm resource directories`() {
        val project = buildProjectWithMPP {
            kotlin {
                jvm()
            }
        }

        project.evaluate()
        project.publishFakeResources(
            project.multiplatformExtension.jvm()
        )

        val actualSourcesDirectories = project.multiplatformExtension.jvm()
            .compilations.getByName("main")
            .defaultSourceSet.resources.sourceDirectories

        assert(
            actualSourcesDirectories.contains(project.expectedResourcePath)
        ) { actualSourcesDirectories.dumpPaths() }
    }

    @Test
    fun `test publishing jvm resources - doesn't show up in resources - when there is no publication`() {
        val project = buildProjectWithMPP {
            kotlin {
                jvm()
            }
        }

        project.evaluate()

        val actualSourcesDirectories = project.multiplatformExtension.jvm()
            .compilations.getByName("main")
            .defaultSourceSet.resources.sourceDirectories

        assert(
            !actualSourcesDirectories.contains(project.expectedResourcePath)
        ) { actualSourcesDirectories.dumpPaths() }
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

    private fun FileCollection.dumpPaths(): String {
        return "Actual paths:" + if (isEmpty) " <Empty>" else joinToString("") { "\n${it.path}" }
    }

}