/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.ide

import org.jetbrains.kotlin.gradle.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.buildProject
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.enableDefaultStdlibDependency
import org.jetbrains.kotlin.gradle.enableDependencyVerification
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.assertMatches
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.binaryCoordinates
import org.jetbrains.kotlin.gradle.kpm.idea.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers.IdePlatformBinaryDependencyResolver
import kotlin.test.Test

class IdePlatformBinaryDependencyResolverTest {

    @Test
    fun `test - MVIKotlin - on jvm and linux platform source sets`() {
        val project = buildProject {
            enableDefaultStdlibDependency(false)
            enableDependencyVerification(false)
            applyMultiplatformPlugin()
            repositories.mavenCentralCacheRedirector()
        }

        val kotlin = project.multiplatformExtension
        kotlin.targetHierarchy.default()

        kotlin.jvm()
        kotlin.linuxX64()

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val jvmMain = kotlin.sourceSets.getByName("jvmMain")
        val linuxX64Main = kotlin.sourceSets.getByName("linuxX64Main")
        val jvmTest = kotlin.sourceSets.getByName("jvmTest")
        val linuxX64Test = kotlin.sourceSets.getByName("linuxX64Test")

        commonMain.dependencies {
            implementation("com.arkivanov.mvikotlin:mvikotlin:3.0.2")
        }

        /* This resolver shall refuse to resolve for dependencies for metadata based dependencies */
        IdePlatformBinaryDependencyResolver().resolve(commonMain).assertMatches()

        val jvmDependencies = listOf(
            binaryCoordinates("com.arkivanov.mvikotlin:mvikotlin-jvm:3.0.2"),
            binaryCoordinates("com.arkivanov.essenty:lifecycle-jvm:0.4.2"),
            binaryCoordinates("com.arkivanov.essenty:instance-keeper-jvm:0.4.2"),
            binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.10"),
            binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.7.10"),
            binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib:1.7.10"),
            binaryCoordinates("org.jetbrains:annotations:13.0")
        )

        val linuxDependencies = listOf(
            binaryCoordinates("com.arkivanov.mvikotlin:mvikotlin-linuxx64:3.0.2"),
            binaryCoordinates("com.arkivanov.essenty:lifecycle-linuxx64:0.4.2"),
            binaryCoordinates("com.arkivanov.essenty:instance-keeper-linuxx64:0.4.2"),
            binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib:1.7.10"),
            binaryCoordinates("org.jetbrains:annotations:13.0"),

            /* Special in K/N: There are no 'runtimeOnly' dependencies */
            binaryCoordinates("com.arkivanov.mvikotlin:rx-internal-linuxx64:3.0.2"),
            binaryCoordinates("com.arkivanov.mvikotlin:utils-internal-linuxx64:3.0.2"),
            binaryCoordinates("com.arkivanov.mvikotlin:rx-linuxx64:3.0.2"),
            binaryCoordinates("com.arkivanov.essenty:utils-internal-linuxx64:0.4.2"),
        )

        IdePlatformBinaryDependencyResolver().resolve(jvmMain).assertMatches(jvmDependencies)
        IdePlatformBinaryDependencyResolver().resolve(jvmTest).assertMatches(jvmDependencies)
        IdePlatformBinaryDependencyResolver().resolve(linuxX64Main).assertMatches(linuxDependencies)
        IdePlatformBinaryDependencyResolver().resolve(linuxX64Test).assertMatches(linuxDependencies)
    }
}
