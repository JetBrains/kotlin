/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.dependencyResolutionTests.tcs

import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.extras.documentationClasspathKey
import org.jetbrains.kotlin.gradle.idea.tcs.extras.sourcesClasspath
import org.jetbrains.kotlin.gradle.idea.tcs.extras.sourcesClasspathKey
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.assertMatches
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.binaryCoordinates
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers.IdeNativeStdlibDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImport
import org.jetbrains.kotlin.gradle.util.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.enableDefaultStdlibDependency
import org.jetbrains.kotlin.gradle.util.enableDependencyVerification
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
        kotlin.applyDefaultHierarchyTemplate()
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
                .filter { it.sourcesClasspath.isNotEmpty() }

        /* Check commonMain&commonTest */
        run {
            val expectedDependencies = listOf(
                binaryCoordinates("com.arkivanov.mvikotlin:mvikotlin:commonMain:3.0.2"),
                binaryCoordinates("com.arkivanov.essenty:lifecycle:commonMain:0.4.2"),
                binaryCoordinates("com.arkivanov.essenty:instance-keeper:commonMain:0.4.2"),
            )

            val resolvedDependencies = resolveDependencySources(commonMain)
            resolvedDependencies.assertMatches(expectedDependencies)
            resolveDependencySources(commonTest).assertMatches(resolvedDependencies)
            resolvedDependencies.assertSourcesFilesEndWith("-sources.jar")
        }

        /* Check nativeMain&nativeTest */
        run {
            val expectedDependencies = listOf(
                binaryCoordinates("com.arkivanov.mvikotlin:mvikotlin:commonMain:3.0.2"),
                binaryCoordinates("com.arkivanov.mvikotlin:mvikotlin:jsNativeMain:3.0.2"),
                binaryCoordinates("com.arkivanov.essenty:lifecycle:commonMain:0.4.2"),
                binaryCoordinates("com.arkivanov.essenty:instance-keeper:commonMain:0.4.2"),
                IdeNativeStdlibDependencyResolver.nativeStdlibCoordinates(project),
                binaryCoordinates(Regex(".*stdlib.*")) /* KT-56278 */
            )

            val resolvedDependencies = resolveDependencySources(nativeMain)
            resolvedDependencies.assertMatches(expectedDependencies)
            resolveDependencySources(nativeTest).assertMatches(resolvedDependencies)
            resolvedDependencies.assertSourcesFilesEndWith("-sources.jar", "-sources.zip")
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
            resolveDependencySources(linuxX64Test).withSanitisedExtras().assertMatches(resolvedDependencies.withSanitisedExtras())
            resolvedDependencies.assertSourcesFilesEndWith("-sources.jar", "-sources.zip")
        }
    }
}

private fun Iterable<IdeaKotlinResolvedBinaryDependency>.withSanitisedExtras() = onEach { dependency ->
    val keysToKeep = setOf(sourcesClasspathKey, documentationClasspathKey)
    (dependency.extras.keys - keysToKeep).forEach { keyToRemove ->
        dependency.extras.remove(keyToRemove)
    }
}

private fun Iterable<IdeaKotlinResolvedBinaryDependency>.assertSourcesFilesEndWith(vararg suffixes: String) {
    forEach { dependency ->
        dependency.sourcesClasspath.forEach { sourcesFile ->
            if (suffixes.none { suffix -> sourcesFile.path.endsWith(suffix) }) {
                fail("Expected binaryFile to end with one of ${suffixes.toList()}. Found: ${sourcesFile}")
            }
        }
    }
}
