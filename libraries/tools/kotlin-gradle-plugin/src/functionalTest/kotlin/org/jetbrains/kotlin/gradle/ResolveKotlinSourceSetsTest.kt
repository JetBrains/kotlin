/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/* Associate compilations are not yet supported by the IDE. KT-34102 */
@file:Suppress("invisible_reference", "invisible_member", "FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.sources.dependsOnClosure
import org.jetbrains.kotlin.gradle.plugin.sources.findSourceSetsDependingOn
import org.junit.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class ResolveKotlinSourceSetsTest {
    private lateinit var project: Project
    private lateinit var kotlin: KotlinMultiplatformExtension

    @BeforeTest
    fun setup() {
        project = ProjectBuilder.builder().build()
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
            setOf(nativeMain, commonMain), linuxMain.dependsOnClosure
        )

        assertEquals(
            setOf(commonMain), nativeMain.dependsOnClosure
        )

        assertEquals(
            emptySet(), commonMain.dependsOnClosure
        )

        assertEquals(
            setOf(commonMain), jvmMain.dependsOnClosure
        )
    }
}
