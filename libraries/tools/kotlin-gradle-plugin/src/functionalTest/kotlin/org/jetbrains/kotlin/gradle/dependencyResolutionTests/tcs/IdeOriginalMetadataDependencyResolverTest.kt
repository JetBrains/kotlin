/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.dependencyResolutionTests.tcs

import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonTest
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.dependencies
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.assertMatches
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.binaryCoordinates
import org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers.IdeOriginalMetadataDependencyResolver
import org.jetbrains.kotlin.gradle.util.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.enableDefaultStdlibDependency
import org.jetbrains.kotlin.gradle.util.enableDependencyVerification
import org.junit.Test

class IdeOriginalMetadataDependencyResolverTest {

    @Test
    fun `test kotlin-test-common`() {
        val project = buildProject {
            enableDependencyVerification(false)
            enableDefaultStdlibDependency(false)
            applyMultiplatformPlugin()
            repositories.mavenLocal()
            repositories.mavenCentralCacheRedirector()
        }

        val kotlin = project.multiplatformExtension

        kotlin.jvm()
        kotlin.linuxX64()
        kotlin.linuxArm64()

        kotlin.sourceSets.commonTest.dependencies {
            implementation("org.jetbrains.kotlin:kotlin-test-common:${kotlin.coreLibrariesVersion}")
        }

        project.evaluate()

        IdeOriginalMetadataDependencyResolver.resolve(kotlin.sourceSets.commonTest.get()).assertMatches(
            binaryCoordinates("org.jetbrains.kotlin:kotlin-test-common:${kotlin.coreLibrariesVersion}")
        )
    }

    @Test
    fun `test legacy metadata dependency`() {
        val project = buildProject {
            enableDependencyVerification(false)
            enableDefaultStdlibDependency(false)
            applyMultiplatformPlugin()
            repositories.mavenLocal()
            repositories.mavenCentralCacheRedirector()
        }

        val kotlin = project.multiplatformExtension

        kotlin.jvm()
        kotlin.linuxX64()

        kotlin.applyDefaultHierarchyTemplate()

        val commonMain = kotlin.sourceSets.getByName("commonMain")

        commonMain.dependencies {
            implementation("io.ktor:ktor-client-core:1.0.1")
        }

        project.evaluate()

        IdeOriginalMetadataDependencyResolver.resolve(commonMain).assertMatches(
            binaryCoordinates("io.ktor:ktor-client-core:1.0.1"),
            binaryCoordinates("io.ktor:ktor-http:1.0.1"),
            binaryCoordinates("io.ktor:ktor-utils:1.0.1"),
            binaryCoordinates("org.jetbrains.kotlinx:kotlinx-coroutines-io:0.1.1"),
            binaryCoordinates("org.jetbrains.kotlinx:kotlinx-io:0.1.1"),
            binaryCoordinates("org.jetbrains.kotlinx:atomicfu-common:0.11.12"),
            binaryCoordinates("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:1.0.1"),
            binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib-common:1.3.10"),
        )
    }
}
