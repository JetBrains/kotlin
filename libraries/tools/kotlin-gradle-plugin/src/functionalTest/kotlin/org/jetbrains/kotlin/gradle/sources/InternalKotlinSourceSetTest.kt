/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.sources

import org.jetbrains.kotlin.gradle.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.InternalKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.reflections
import kotlin.reflect.full.isSubclassOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class InternalKotlinSourceSetTest {
    @Test
    fun `test - all implementations of KotlinSourceSet - implement InternalKotlinSourceSet`() {
        val subtypesOfKotlinSourceSet = reflections.getSubTypesOf(KotlinSourceSet::class.java)
        subtypesOfKotlinSourceSet
            .filter { subtype -> !subtype.isInterface }
            .forEach { implementation ->
                if (!implementation.kotlin.isSubclassOf(InternalKotlinSourceSet::class)) {
                    fail("$implementation does not implement ${InternalKotlinSourceSet::class}")
                }
            }
    }

    @Test
    fun `test - compilations - sample - 0`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension

        val jvm = kotlin.jvm()
        val linux = kotlin.linuxX64()
        val macos = kotlin.macosX64()

        val metadataCompilation = kotlin.metadata().compilations.getByName("main")
        val jvmCompilation = jvm.compilations.getByName("main")
        val linuxCompilation = linux.compilations.getByName("main")
        val macosCompilation = macos.compilations.getByName("main")

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val linuxX4Main = kotlin.sourceSets.getByName("linuxX64Main")
        val macosX64Main = kotlin.sourceSets.getByName("macosX64Main")

        val nativeMain = kotlin.sourceSets.create("nativeMain")
        nativeMain.dependsOn(commonMain)

        assertEquals<Set<KotlinCompilation<*>>>(
            setOf(metadataCompilation, jvmCompilation, linuxCompilation, macosCompilation),
            commonMain.internal.compilations
        )

        assertEquals(
            emptySet(),
            nativeMain.internal.compilations
        )

        linuxX4Main.dependsOn(nativeMain)
        assertEquals<Set<KotlinCompilation<*>>>(
            setOf(linuxCompilation),
            nativeMain.internal.compilations
        )

        macosX64Main.dependsOn(nativeMain)
        assertEquals<Set<KotlinCompilation<*>>>(
            setOf(linuxCompilation, macosCompilation),
            nativeMain.internal.compilations
        )
    }

    @Test
    fun `test - withDependsOnClosure - sample - 0`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension

        kotlin.linuxX64()

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val nativeMain = kotlin.sourceSets.create("nativeMain")
        val linuxMain = kotlin.sourceSets.create("linuxMain")
        val linuxX64Main = kotlin.sourceSets.getByName("linuxX64Main")

        assertEquals(
            setOf(commonMain, linuxX64Main),
            linuxX64Main.internal.withDependsOnClosure
        )

        linuxX64Main.dependsOn(linuxMain)
        assertEquals(
            setOf(commonMain, linuxMain, linuxX64Main),
            linuxX64Main.internal.withDependsOnClosure
        )

        linuxMain.dependsOn(nativeMain)
        assertEquals(
            setOf(commonMain, nativeMain, linuxMain, linuxX64Main),
            linuxX64Main.internal.withDependsOnClosure
        )

        nativeMain.dependsOn(commonMain)
        assertEquals(
            setOf(commonMain, nativeMain, linuxMain, linuxX64Main),
            linuxX64Main.internal.withDependsOnClosure
        )
    }
}
