/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.dsl.metadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCommonCompilation
import org.jetbrains.kotlin.gradle.targets.metadata.awaitMetadataCompilationsCreated
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class KotlinMetadataTargetCompilationsTest {
    @Test
    fun `test - metadata compilations created for shared source sets`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension

        kotlin.jvm()
        kotlin.linuxX64()
        kotlin.linuxArm64()
        kotlin.applyDefaultHierarchyTemplate()

        project.runLifecycleAwareTest {
            val target = kotlin.metadataTarget
            val metadataCompilations = target.awaitMetadataCompilationsCreated()
            metadataCompilations.assertExists("commonMain")
            metadataCompilations.assertExists("nativeMain")
            metadataCompilations.assertExists("linuxMain")

            metadataCompilations.assertExistsNot("commonTest")
            metadataCompilations.assertExistsNot("nativeTest")
            metadataCompilations.assertExistsNot("linuxTest")
        }
    }

    @Test
    fun `test - metadata compilations - single target project`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension
        kotlin.jvm()
        kotlin.applyDefaultHierarchyTemplate()

        project.runLifecycleAwareTest {
            val target = kotlin.metadataTarget
            val compilations = target.awaitMetadataCompilationsCreated()
            compilations.assertExistsNot("commonMain")
        }
    }

    @Test
    fun `test - metadata compilations - isKlibCompilation`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension
        kotlin.jvm()
        kotlin.linuxX64()
        kotlin.linuxArm64()

        project.runLifecycleAwareTest {
            val target = kotlin.metadataTarget
            val metadataCompilations = target.awaitMetadataCompilationsCreated() // returns common + shared native
                .filterIsInstance<KotlinCommonCompilation>()
                .associate { it.name to it.isKlibCompilation }

            val expectedCommonMetadataCompilations = mapOf(
                "main" to false,
                "commonMain" to true
            )

            assertEquals(expectedCommonMetadataCompilations, metadataCompilations)
        }
    }

    private fun NamedDomainObjectContainer<KotlinCompilation<*>>.assertExists(name: String) {
        if (name !in names) fail("Missing '$name' compilation")
    }

    private fun NamedDomainObjectContainer<KotlinCompilation<*>>.assertExistsNot(name: String) {
        if (name in names) fail("Unexpected '$name' compilation")
    }
}