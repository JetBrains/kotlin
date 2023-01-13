/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.dependencyResolutionTests.tcs

import org.jetbrains.kotlin.compilerRunner.konanVersion
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.assertMatches
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.binaryCoordinates
import org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers.IdeNativeStdlibDependencyResolver
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.junit.Test

class IdeNativeStdlibResolverTest {


    @Test
    fun `test single linux target`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension

        kotlin.linuxX64()

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val linuxX64Main = kotlin.sourceSets.getByName("linuxX64Main")
        val linuxX64Test = kotlin.sourceSets.getByName("linuxX64Test")

        val stdlibCoordinates = binaryCoordinates("org.jetbrains.kotlin.native:stdlib:${project.konanVersion}")

        IdeNativeStdlibDependencyResolver.resolve(commonMain).assertMatches(stdlibCoordinates)
        IdeNativeStdlibDependencyResolver.resolve(commonTest).assertMatches(stdlibCoordinates)
        IdeNativeStdlibDependencyResolver.resolve(linuxX64Main).assertMatches(stdlibCoordinates)
        IdeNativeStdlibDependencyResolver.resolve(linuxX64Test).assertMatches(stdlibCoordinates)
    }

    @Test
    fun `test shared non native target`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension

        kotlin.linuxX64()
        kotlin.jvm()

        val linuxX64Main = kotlin.sourceSets.getByName("linuxX64Main")
        val linuxX64Test = kotlin.sourceSets.getByName("linuxX64Test")

        val stdlibCoordinates = binaryCoordinates("org.jetbrains.kotlin.native:stdlib:${project.konanVersion}")

        IdeNativeStdlibDependencyResolver.resolve(linuxX64Main).assertMatches(stdlibCoordinates)
        IdeNativeStdlibDependencyResolver.resolve(linuxX64Test).assertMatches(stdlibCoordinates)
    }

    @Test
    fun `test shared native target`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension

        kotlin.linuxX64()
        kotlin.linuxArm64()
        kotlin.jvm()

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val linuxX64Main = kotlin.sourceSets.getByName("linuxX64Main")
        val linuxX64Test = kotlin.sourceSets.getByName("linuxX64Test")
        val linuxArm64Main = kotlin.sourceSets.getByName("linuxArm64Main")
        val linuxArm64Test = kotlin.sourceSets.getByName("linuxArm64Test")
        val linuxMain = kotlin.sourceSets.create("linuxMain") { linuxMain ->
            linuxMain.dependsOn(commonMain)
            linuxArm64Main.dependsOn(linuxMain)
            linuxX64Main.dependsOn(linuxMain)
        }
        val linuxTest = kotlin.sourceSets.create("linuxTest") { linuxTest ->
            linuxTest.dependsOn(commonTest)
            linuxArm64Main.dependsOn(linuxTest)
            linuxX64Main.dependsOn(linuxTest)
        }

        val stdlibCoordinates = binaryCoordinates("org.jetbrains.kotlin.native:stdlib:${project.konanVersion}")

        IdeNativeStdlibDependencyResolver.resolve(linuxX64Main).assertMatches(stdlibCoordinates)
        IdeNativeStdlibDependencyResolver.resolve(linuxX64Test).assertMatches(stdlibCoordinates)
        IdeNativeStdlibDependencyResolver.resolve(linuxArm64Main).assertMatches(stdlibCoordinates)
        IdeNativeStdlibDependencyResolver.resolve(linuxArm64Test).assertMatches(stdlibCoordinates)
        IdeNativeStdlibDependencyResolver.resolve(linuxMain).assertMatches(stdlibCoordinates)
        IdeNativeStdlibDependencyResolver.resolve(linuxTest).assertMatches(stdlibCoordinates)
    }
}
