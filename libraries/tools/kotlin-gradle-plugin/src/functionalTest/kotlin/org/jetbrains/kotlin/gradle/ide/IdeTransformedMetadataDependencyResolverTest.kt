/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.ide

import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.assertMatches
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.binaryCoordinates
import org.jetbrains.kotlin.gradle.kpm.idea.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers.IdeTransformedMetadataDependencyResolver
import org.junit.Test

class IdeTransformedMetadataDependencyResolverTest {

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
        val linuxMain = kotlin.sourceSets.getByName("linuxMain")

        commonMain.dependencies {
            implementation("com.arkivanov.mvikotlin:mvikotlin:3.0.2")
            implementation("com.arkivanov.essenty:lifecycle:0.4.2")
            implementation("com.arkivanov.essenty:instance-keeper:0.4.2")
        }

        project.evaluate()

        IdeTransformedMetadataDependencyResolver.resolve(commonMain)
            .assertMatches(
                binaryCoordinates("com.arkivanov.mvikotlin:mvikotlin:3.0.2:commonMain"),
                binaryCoordinates("com.arkivanov.essenty:lifecycle:0.4.2:commonMain"),
                binaryCoordinates("com.arkivanov.essenty:instance-keeper:0.4.2:commonMain")
            )

        IdeTransformedMetadataDependencyResolver.resolve(linuxMain)
            .assertMatches(
                binaryCoordinates("com.arkivanov.mvikotlin:mvikotlin:3.0.2:commonMain"),
                binaryCoordinates("com.arkivanov.mvikotlin:mvikotlin:3.0.2:jsNativeMain"),
                binaryCoordinates("com.arkivanov.essenty:lifecycle:0.4.2:commonMain"),
                binaryCoordinates("com.arkivanov.essenty:instance-keeper:0.4.2:commonMain")
            )
    }
}