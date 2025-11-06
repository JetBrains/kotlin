/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.dependencyResolutionTests.tcs

import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.assertMatches
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.binaryCoordinates
import org.jetbrains.kotlin.gradle.idea.testFixtures.utils.*
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers.IdeTransformedMetadataDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImport
import org.jetbrains.kotlin.gradle.util.*
import org.junit.Test

class IdeTransformedMetadataDependencyResolverTest {

    @Test
    fun `test - MVIKotlin - with unavailable linuxArm64 target`() {
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
        val linuxMain = kotlin.sourceSets.getByName("linuxMain")

        // odd source set structure: linuxX64Main + jvmMain -> linuxX64AndJvmMain
        val linuxX64Main = kotlin.sourceSets.getByName("linuxX64Main")
        val jvmMain = kotlin.sourceSets.getByName("jvmMain")
        val linuxX64AndJvmMain = kotlin.sourceSets.create("linuxX64AndJvmMain")
        linuxX64AndJvmMain.dependsOn(commonMain)
        linuxX64Main.dependsOn(linuxX64AndJvmMain)
        jvmMain.dependsOn(linuxX64AndJvmMain)

        commonMain.dependencies {
            implementation("com.arkivanov.mvikotlin:mvikotlin:3.0.2")
        }

        project.evaluate()

        val unresolvedDependenciesDiagnosticMatcher = unresolvedDependenciesDiagnosticMatcher("com.arkivanov.mvikotlin:mvikotlin")

        // Expected to be unresolved in all intermediate main & test source sets
        IdeTransformedMetadataDependencyResolver.resolve(commonMain)
            .assertMatches(
                unresolvedDependenciesDiagnosticMatcher,
            )

        IdeTransformedMetadataDependencyResolver.resolve(commonTest)
            .assertMatches(
                unresolvedDependenciesDiagnosticMatcher,
            )

        IdeTransformedMetadataDependencyResolver.resolve(linuxMain)
            .assertMatches(
                unresolvedDependenciesDiagnosticMatcher
            )

        // And only linuxX64AndJvmMain can see symbols from mvi
        IdeTransformedMetadataDependencyResolver.resolve(linuxX64AndJvmMain)
            .assertMatches(
                binaryCoordinates("com.arkivanov.mvikotlin:mvikotlin:commonMain:3.0.2"),
                binaryCoordinates("com.arkivanov.essenty:lifecycle:commonMain:0.4.2"),
                binaryCoordinates("com.arkivanov.essenty:instance-keeper:commonMain:0.4.2"),
            )

    }

    @Test
    fun `test - MVIKotlin - with supported targets`() {
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
        kotlin.iosSimulatorArm64()
        kotlin.iosArm64()

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val iosMain = kotlin.sourceSets.getByName("iosMain")

        commonMain.dependencies {
            implementation("com.arkivanov.mvikotlin:mvikotlin:3.0.2")
        }

        project.evaluate()

        IdeTransformedMetadataDependencyResolver.resolve(commonMain)
            .assertMatches(
                binaryCoordinates("com.arkivanov.mvikotlin:mvikotlin:commonMain:3.0.2"),
                binaryCoordinates("com.arkivanov.essenty:lifecycle:commonMain:0.4.2"),
                binaryCoordinates("com.arkivanov.essenty:instance-keeper:commonMain:0.4.2"),
            )

        IdeTransformedMetadataDependencyResolver.resolve(commonTest)
            .assertMatches(
                binaryCoordinates("com.arkivanov.mvikotlin:mvikotlin:commonMain:3.0.2"),
                binaryCoordinates("com.arkivanov.essenty:lifecycle:commonMain:0.4.2"),
                binaryCoordinates("com.arkivanov.essenty:instance-keeper:commonMain:0.4.2"),
            )

        IdeTransformedMetadataDependencyResolver.resolve(iosMain)
            .assertMatches(
                binaryCoordinates("com.arkivanov.mvikotlin:mvikotlin:commonMain:3.0.2"),
                binaryCoordinates("com.arkivanov.mvikotlin:mvikotlin:jsNativeMain:3.0.2"),
                binaryCoordinates("com.arkivanov.essenty:lifecycle:commonMain:0.4.2"),
                binaryCoordinates("com.arkivanov.essenty:instance-keeper:commonMain:0.4.2"),
            )
    }

    @Test
    fun `test OKIO in JVM + Android project`() {
        val project = buildProject {
            enableDependencyVerification(false)
            applyMultiplatformPlugin()
            repositories.mavenLocal()
            repositories.mavenCentralCacheRedirector()
            repositories.google()
            androidLibrary {
                compileSdkVersion = "android-31"
            }
        }
        val kotlin = project.multiplatformExtension

        kotlin.jvm()
        @Suppress("DEPRECATION")
        kotlin.androidTarget()

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")

        commonMain.dependencies {
            implementation("com.squareup.okio:okio:3.2.0")
        }

        project.evaluate()

        fun KotlinSourceSet.binaryDependencies() =
            project.kotlinIdeMultiplatformImport.resolveDependencies(this).filterIsInstance<IdeaKotlinBinaryDependency>()

        val kgpVersion = project.getKotlinPluginVersion()
        commonMain.binaryDependencies().assertMatches(
            binaryCoordinates("com.squareup.okio:okio-jvm:3.2.0"),
            legacyStdlibJdkDependencies(),
            binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib:$kgpVersion"),
            binaryCoordinates("org.jetbrains:annotations:13.0"),
        )

        commonTest.binaryDependencies().assertMatches(
            binaryCoordinates("com.squareup.okio:okio-jvm:3.2.0"),
            legacyStdlibJdkDependencies(),
            binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib:$kgpVersion"),
            binaryCoordinates("org.jetbrains:annotations:13.0"),
        )
    }
}
