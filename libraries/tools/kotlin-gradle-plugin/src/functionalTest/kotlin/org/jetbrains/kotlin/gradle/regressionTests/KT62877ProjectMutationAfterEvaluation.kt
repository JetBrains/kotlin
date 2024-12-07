/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImport
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.enableCInteropCommonization
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test
import kotlin.test.assertEquals

class KT62877ProjectMutationAfterEvaluation {

    @Test
    fun `test multiplatform project's collections can't be mutated after evaluation`() {
        val project = buildProjectWithMPP {
            kotlin {
                jvm()
                linuxX64(); linuxArm64()
                iosX64(); iosArm64()

                targets.filterIsInstance<KotlinNativeTarget>().forEach { target ->
                    target.compilations.getByName("main").cinterops.create("foo")
                }
            }
        }

        project.enableCInteropCommonization()
        project.repositories.mavenLocal()
        project.repositories.mavenCentralCacheRedirector()

        project.evaluate()

        val domainObjectCollectionsToCheck = listOf(
            project.configurations,
            project.tasks,
            project.components,
        )

        // Collect names before task initialization
        val namesBefore = domainObjectCollectionsToCheck.associateWith { it.names.toSet() }

        // Now initialize tasks
        val taskNames = project.tasks.names.toMutableSet()
        taskNames.remove("init") // initializing the init task isn't possible due to Gradle reasons
        taskNames.map { project.tasks.getByName(it) }

        // And resolve IDE dependencies
        project.kotlinIdeMultiplatformImport.resolveDependencies("commonMain")

        // Compare names after
        val namesAfter = domainObjectCollectionsToCheck.associateWith { it.names.toSet() }

        assertEquals(namesBefore, namesAfter)
    }
}