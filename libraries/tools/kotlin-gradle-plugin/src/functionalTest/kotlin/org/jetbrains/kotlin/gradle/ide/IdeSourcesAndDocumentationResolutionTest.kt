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
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.assertMatches
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.binaryCoordinates
import org.jetbrains.kotlin.gradle.kpm.idea.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers.IdeNativeStdlibDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImport
import kotlin.test.Test
import kotlin.test.fail
import kotlin.text.Regex.Companion.escape

class IdeSourcesAndDocumentationResolutionTest {

    @Test
    fun `test - MVIKotlin`() {
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
        kotlin.linuxArm64()

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val nativeMain = kotlin.sourceSets.getByName("nativeMain")
        val nativeTest = kotlin.sourceSets.getByName("nativeTest")
        val linuxX64Main = kotlin.sourceSets.getByName("linuxX64Main")
        val linuxX64Test = kotlin.sourceSets.getByName("linuxX64Test")

        commonMain.dependencies {
            implementation("com.arkivanov.mvikotlin:mvikotlin:3.0.2")
        }

        project.evaluate()

        fun resolveDependencySources(sourceSet: KotlinSourceSet): List<IdeaKotlinResolvedBinaryDependency> =
            project.kotlinIdeMultiplatformImport.resolveDependencies(sourceSet)
                .filterIsInstance<IdeaKotlinResolvedBinaryDependency>()
                .filter { it.binaryType == IdeaKotlinDependency.SOURCES_BINARY_TYPE }


        /* Check commonMain&commonTest */
        run {
            val expectedDependencies = listOf(
                binaryCoordinates("com.arkivanov.mvikotlin:mvikotlin:3.0.2"),
                binaryCoordinates("com.arkivanov.essenty:lifecycle:0.4.2"),
                binaryCoordinates("com.arkivanov.essenty:instance-keeper:0.4.2"),
            )

            val resolvedDependencies = resolveDependencySources(commonMain)
            resolvedDependencies.assertMatches(expectedDependencies)
            resolveDependencySources(commonTest).assertMatches(resolvedDependencies)
            resolvedDependencies.assertFilesEndWith("-sources.jar")
        }

        /* Check nativeMain&nativeTest */
        run {
            val expectedDependencies = listOf(
                binaryCoordinates("com.arkivanov.mvikotlin:mvikotlin:3.0.2"),
                binaryCoordinates("com.arkivanov.essenty:lifecycle:0.4.2"),
                binaryCoordinates("com.arkivanov.essenty:instance-keeper:0.4.2"),
                IdeNativeStdlibDependencyResolver.nativeStdlibCoordinates(project)
            )

            val resolvedDependencies = resolveDependencySources(nativeMain)
            resolvedDependencies.assertMatches(expectedDependencies)
            resolveDependencySources(nativeTest).assertMatches(resolvedDependencies)
            resolvedDependencies.assertFilesEndWith("-sources.jar", "-sources.zip")
        }

        /* Check linuxX64Main and linuxX64Test */
        run {
            val expectedDependencies = listOf(

                /* Required dependencies */
                listOf(
                    binaryCoordinates("com.arkivanov.mvikotlin:rx-internal-linuxx64:3.0.2"),
                    binaryCoordinates("com.arkivanov.mvikotlin:rx-linuxx64:3.0.2"),
                    binaryCoordinates("com.arkivanov.mvikotlin:utils-internal-linuxx64:3.0.2"),
                    binaryCoordinates("com.arkivanov.mvikotlin:mvikotlin-linuxx64:3.0.2"),
                    binaryCoordinates("com.arkivanov.essenty:utils-internal-linuxx64:0.4.2"),
                    binaryCoordinates("com.arkivanov.essenty:lifecycle-linuxx64:0.4.2"),
                    binaryCoordinates("com.arkivanov.essenty:instance-keeper-linuxx64:0.4.2")
                ),

                /* Stdlib */
                listOf(
                    binaryCoordinates("org.jetbrains:annotations:13.0"),
                    IdeNativeStdlibDependencyResolver.nativeStdlibCoordinates(project),
                    binaryCoordinates(Regex(escape("org.jetbrains.kotlin:kotlin-stdlib") + ".*"))
                )
            )

            val resolvedDependencies = resolveDependencySources(linuxX64Main)
            resolvedDependencies.assertMatches(expectedDependencies)
            resolveDependencySources(linuxX64Test).assertMatches(resolvedDependencies)
            resolvedDependencies.assertFilesEndWith("-sources.jar", "-sources.zip")
        }
    }
}

private fun Iterable<IdeaKotlinResolvedBinaryDependency>.assertFilesEndWith(vararg suffixes: String) {
    forEach { dependency ->
        if (suffixes.none { suffix -> dependency.binaryFile.path.endsWith(suffix) }) {
            fail("Expected binaryFile to end with one of ${suffixes.toList()}. Found: ${dependency.binaryFile}")
        }
    }
}