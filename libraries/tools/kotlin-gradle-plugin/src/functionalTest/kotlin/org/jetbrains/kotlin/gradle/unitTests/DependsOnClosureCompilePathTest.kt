/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.iosMain
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.linuxMain
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.targets.metadata.dependsOnClosureCompilePath
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import kotlin.test.Test
import kotlin.test.assertEquals

class DependsOnClosureCompilePathTest {
    private val project = buildProjectWithMPP()
    private val kotlin = project.multiplatformExtension

    @Test
    fun `test - default hierarchy - iosMain`() {
        kotlin.iosArm64()
        kotlin.iosX64()
        kotlin.iosSimulatorArm64()
        project.evaluate()

        assertEquals(
            listOf("test_appleMain.klib", "test_nativeMain.klib", "test_commonMain.klib"),
            kotlin.sourceSets.iosMain.get().dependsOnClosureCompilePath.toList().map { it.name }
        )
    }

    /**
     * ```
     *           ┌─► commonMain  ◄──┐
     *           │        ▲         │
     *           │        │         │
     *           │   ┌──► b ◄────┐  │
     *           │   │           │  │
     *           │   │           │  │
     *           └───c──────────►a──┘
     *               ▲
     *               │
     *           linuxMain
     *               ▲
     *               │
     *         ┌─────┴─────┐
     *         │           │
     * linuxX64main    linuxArm64Main
     * ```
     */
    @Test
    fun `test - custom dependsOn order`() {
        val linuxX64Main = kotlin.linuxX64().compilations.getByName("main").defaultSourceSet
        val linuxArm64Main = kotlin.linuxArm64().compilations.getByName("main").defaultSourceSet

        val commonMain = kotlin.sourceSets.commonMain.get()
        val a = kotlin.sourceSets.create("a")
        val b = kotlin.sourceSets.create("b")
        val c = kotlin.sourceSets.create("c")

        a.dependsOn(commonMain)
        b.dependsOn(commonMain)
        c.dependsOn(commonMain)

        linuxX64Main.dependsOn(c)
        linuxArm64Main.dependsOn(c)

        c.dependsOn(b)
        c.dependsOn(a)
        a.dependsOn(b)

        kotlin.sourceSets.linuxMain.get().dependsOn(c)

        project.evaluate()

        assertEquals(
            listOf("test_c.klib", "test_a.klib", "test_b.klib", "test_commonMain.klib"),
            kotlin.sourceSets.linuxMain.get().dependsOnClosureCompilePath.toList().map { it.name }
        )
    }

    /**
     * ```
     *  ┌────► commonMain ◄───┐
     *  │                     │
     *  │                     │
     *  │                     │
     * left                right
     *  ▲                     ▲
     *  │                     │
     *  │                     │
     *  └───────bottom────────┘
     *             ▲
     *             │
     *             │
     *             │
     *         linuxMain
     * ```
     */
    @Test
    fun `test - diamond`() {
        val linuxX64Main = kotlin.linuxX64().compilations.getByName("main").defaultSourceSet
        val linuxArm64Main = kotlin.linuxArm64().compilations.getByName("main").defaultSourceSet

        val commonMain = kotlin.sourceSets.commonMain.get()
        val left = kotlin.sourceSets.create("left")
        val right = kotlin.sourceSets.create("right")
        val bottom = kotlin.sourceSets.create("bottom")
        val linuxMain = kotlin.sourceSets.linuxMain.get()

        left.dependsOn(commonMain)
        right.dependsOn(commonMain)
        bottom.dependsOn(left)
        bottom.dependsOn(right)
        linuxMain.dependsOn(bottom)

        linuxX64Main.dependsOn(linuxMain)
        linuxArm64Main.dependsOn(linuxMain)

        project.evaluate()

        /*
        ⚠️ We expect 'left' to be listed before 'right' as this reflects
        the order of 'dependsOn()' calls.

        If the order changed, please investigate the root cause; Do not update the assertion!
         */
        assertEquals(
            listOf("test_bottom.klib", "test_left.klib", "test_right.klib", "test_commonMain.klib"),
            kotlin.sourceSets.linuxMain.get().dependsOnClosureCompilePath.toList().map { it.name }
        )
    }
}
