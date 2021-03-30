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
import org.jetbrains.kotlin.gradle.plugin.sources.resolveAllDependsOnSourceSets
import org.jetbrains.kotlin.gradle.plugin.sources.resolveAllSourceSetsDependingOn
import org.junit.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class ResolveKotlinSourceSetsTest {
    private lateinit var project: Project
    private lateinit var kotlin: KotlinMultiplatformExtension

    @BeforeTest
    fun setup() {
        project = ProjectBuilder.builder().build()
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
            kotlin.resolveAllSourceSetsDependingOn(commonMain)
        )

        assertEquals(
            setOf(linuxMain, macosMain),
            kotlin.resolveAllSourceSetsDependingOn(nativeMain)
        )

        assertEquals(emptySet(), kotlin.resolveAllSourceSetsDependingOn(linuxMain))
        assertEquals(emptySet(), kotlin.resolveAllSourceSetsDependingOn(macosMain))
        assertEquals(emptySet(), kotlin.resolveAllSourceSetsDependingOn(jvmMain))
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
            setOf(nativeMain, commonMain), linuxMain.resolveAllDependsOnSourceSets()
        )

        assertEquals(
            setOf(commonMain), nativeMain.resolveAllDependsOnSourceSets()
        )

        assertEquals(
            emptySet(), commonMain.resolveAllDependsOnSourceSets()
        )

        assertEquals(
            setOf(commonMain), jvmMain.resolveAllDependsOnSourceSets()
        )
    }
}
