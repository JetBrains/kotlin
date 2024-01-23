/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.getByName
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinCompilation
import org.jetbrains.kotlin.gradle.util.assertAllImplementationsAlsoImplement
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse

class InternalKotlinSourceSetTest {
    @Test
    fun `test - all implementations of KotlinCompilation - implement InternalKotlinCompilation`() {
        assertAllImplementationsAlsoImplement(KotlinCompilation::class, InternalKotlinCompilation::class)
    }

    @Test
    fun `test - allKotlinSourceSets - carries underlying KotlinSourceSet dependencies`() = testKotlinSourceSets(
        collectionOfKotlinSourceSetsFromCompilation = { allKotlinSourceSets },
        addDependenciesToSourceSetName = "commonMain"
    )

    @Test
    fun `test - kotlinSourceSets - carries underlying KotlinSourceSet dependencies`() = testKotlinSourceSets(
        collectionOfKotlinSourceSetsFromCompilation = { kotlinSourceSets },
        addDependenciesToSourceSetName = "jvmMain"
    )

    private fun testKotlinSourceSets(
        collectionOfKotlinSourceSetsFromCompilation: KotlinCompilation<*>.() -> NamedDomainObjectSet<KotlinSourceSet>,
        addDependenciesToSourceSetName: String
    ) {
        val project = buildProjectWithMPP {
            kotlin {
                jvm()
                linuxX64()
            }
        }

        val jvmMainSourceSets = project
            .multiplatformExtension
            .targets
            .getByName("jvm")
            .compilations
            .getByName("main")
            .collectionOfKotlinSourceSetsFromCompilation()

        // register task that depend on all source sets of jvm main compilation
        val testTask = project.tasks.register("taskThatDependOnJvmSourcesAndResources") {
            it.dependsOn(jvmMainSourceSets)
        }

        val kotlinSourceSet = project.multiplatformExtension.sourceSets.getByName(addDependenciesToSourceSetName)
        val generateSources = project.tasks.register("generateSources", Copy::class.java)
        val generateResources = project.tasks.register("generateResources", Copy::class.java)
        kotlinSourceSet.kotlin.srcDirs(generateSources)
        kotlinSourceSet.resources.srcDirs(generateResources)

        project.evaluate()

        val taskDependencies = testTask.get().taskDependencies.getDependencies(null).map { it.name }
        assertEquals(listOf("generateSources", "generateResources"), taskDependencies)
    }

    @Test
    fun `it is not possible to add source via kotlinSourceSets or allKotlinSourceSets api`() {
        val project = buildProjectWithMPP {
            kotlin {
                jvm()
                linuxX64()
            }
        }

        project.evaluate()

        val commonMain = project.multiplatformExtension.sourceSets.getByName("commonMain")
        val linuxMain = project.multiplatformExtension.sourceSets.getByName("linuxMain")

        val jvmMainCompilation = project
            .multiplatformExtension
            .targets
            .getByName("jvm")
            .compilations
            .getByName("main")

        assertFails { jvmMainCompilation.kotlinSourceSets.add(commonMain) }
        assertFails { jvmMainCompilation.allKotlinSourceSets.add(linuxMain) }

        // this call is possible because commonMain already in the collection
        assertFalse(jvmMainCompilation.allKotlinSourceSets.add(commonMain))
    }
}
