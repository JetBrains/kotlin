/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/* Associate compilations are not yet supported by the IDE. KT-34102 */
@file:Suppress("invisible_reference", "invisible_member", "FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle

import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.findSourceSetsDependingOn
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.plugin.sources.hasMetadataCompilation
import org.junit.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResolveKotlinSourceSetsTest {
    private lateinit var project: ProjectInternal
    private lateinit var kotlin: KotlinMultiplatformExtension

    @BeforeTest
    fun setup() {
        project = ProjectBuilder.builder().build() as ProjectInternal
        addBuildEventsListenerRegistryMock(project)
        project.plugins.apply("kotlin-multiplatform")
        kotlin = project.extensions.getByName("kotlin") as KotlinMultiplatformExtension

    }

    @Test
    fun resolveAllSourceSetsDependingOn() {
        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val nativeMain = kotlin.sourceSets.create("nativeMain")
        val linuxMain = kotlin.sourceSets.create("linuxMain")
        val macosMain = kotlin.sourceSets.create("macosMain")
        val jvmMain = kotlin.sourceSets.create("jvmMain")

        jvmMain.dependsOn(commonMain)
        nativeMain.dependsOn(commonMain)
        linuxMain.dependsOn(nativeMain)
        macosMain.dependsOn(nativeMain)

        assertEquals(
            setOf(nativeMain, linuxMain, macosMain, jvmMain),
            kotlin.findSourceSetsDependingOn(commonMain)
        )

        assertEquals(
            setOf(linuxMain, macosMain),
            kotlin.findSourceSetsDependingOn(nativeMain)
        )

        assertEquals(emptySet(), kotlin.findSourceSetsDependingOn(linuxMain))
        assertEquals(emptySet(), kotlin.findSourceSetsDependingOn(macosMain))
        assertEquals(emptySet(), kotlin.findSourceSetsDependingOn(jvmMain))
    }

    @Test
    fun resolveAllDependsOnSourceSets() {
        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val nativeMain = kotlin.sourceSets.create("nativeMain")
        val linuxMain = kotlin.sourceSets.create("linuxMain")
        val macosMain = kotlin.sourceSets.create("macosMain")
        val jvmMain = kotlin.sourceSets.create("jvmMain")

        jvmMain.dependsOn(commonMain)
        nativeMain.dependsOn(commonMain)
        linuxMain.dependsOn(nativeMain)
        macosMain.dependsOn(nativeMain)

        assertEquals(
            setOf(nativeMain, commonMain), linuxMain.internal.dependsOnClosure
        )

        assertEquals(
            setOf(commonMain), nativeMain.internal.dependsOnClosure
        )

        assertEquals(
            emptySet(), commonMain.internal.dependsOnClosure
        )

        assertEquals(
            setOf(commonMain), jvmMain.internal.dependsOnClosure
        )
    }
    
    @Test
    fun resolveAllSharedSourceSets() {
        with(kotlin) {
            jvm()
            jvm("jvm2")
            linuxX64("linux")
            macosX64("macos")
        }

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val nativeMain = kotlin.sourceSets.create("nativeMain")
        val linuxMain = kotlin.sourceSets.getByName("linuxMain")
        val macosMain = kotlin.sourceSets.getByName("macosMain")
        val jvmMain = kotlin.sourceSets.getByName("jvmMain")
        val jvm2Main = kotlin.sourceSets.getByName("jvm2Main")
        val allJvmMain = kotlin.sourceSets.create("allJvmMain")

        jvmMain.dependsOn(commonMain)
        nativeMain.dependsOn(commonMain)
        linuxMain.dependsOn(nativeMain)
        macosMain.dependsOn(nativeMain)
        allJvmMain.dependsOn(commonMain)
        jvmMain.dependsOn(allJvmMain)
        jvm2Main.dependsOn(allJvmMain)

        project.evaluate()

        fun assertItHasMetadataCompilation(sourceSet: KotlinSourceSet) =
            assertTrue("${sourceSet.name} is expected to be shared") { sourceSet.internal.hasMetadataCompilation }

        fun assertItDoesntHaveMetadataCompilation(sourceSet: KotlinSourceSet) =
            assertFalse("${sourceSet.name} is expected to be shared") { sourceSet.internal.hasMetadataCompilation }

        assertItHasMetadataCompilation(commonMain)
        assertItHasMetadataCompilation(nativeMain)

        assertItDoesntHaveMetadataCompilation(linuxMain)
        assertItDoesntHaveMetadataCompilation(macosMain)
        assertItDoesntHaveMetadataCompilation(jvmMain)
        assertItDoesntHaveMetadataCompilation(allJvmMain)
    }
}
