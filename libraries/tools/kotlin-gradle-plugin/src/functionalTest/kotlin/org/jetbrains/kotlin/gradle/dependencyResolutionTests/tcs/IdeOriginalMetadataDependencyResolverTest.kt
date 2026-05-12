/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.dependencyResolutionTests.tcs

import org.jetbrains.kotlin.gradle.dependencyResolutionTests.kotlinBuildDeps
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.assertMatches
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.binaryCoordinates
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonTest
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.dependencies
import org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers.IdeOriginalMetadataDependencyResolver
import org.jetbrains.kotlin.gradle.util.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.enableDefaultStdlibDependency
import org.jetbrains.kotlin.gradle.util.enableDependencyVerification
import kotlin.test.Test

class IdeOriginalMetadataDependencyResolverTest {
    @Test
    fun `test legacy metadata dependency`() {
        val project = buildProject {
            enableDependencyVerification(false)
            enableDefaultStdlibDependency(false)
            applyMultiplatformPlugin()
            repositories.kotlinBuildDeps()
            repositories.mavenCentralCacheRedirector()
        }

        val kotlin = project.multiplatformExtension

        kotlin.jvm()
        kotlin.linuxX64()

        kotlin.applyDefaultHierarchyTemplate()

        val commonMain = kotlin.sourceSets.getByName("commonMain")

        commonMain.dependencies {
            implementation("org.test:mock-jvm-lib-a:1.0")
        }

        project.evaluate()

        IdeOriginalMetadataDependencyResolver.resolve(commonMain).assertMatches(
            binaryCoordinates("org.test:mock-jvm-lib-a:1.0"),
            binaryCoordinates("org.test:mock-jvm-lib-b:1.0"),
            binaryCoordinates("org.test:mock-jvm-lib-c:1.0"),
            binaryCoordinates("org.test:mock-coroutines-io:1.0"),
            binaryCoordinates("org.test:mock-kotlinx-io:1.0"),
            binaryCoordinates("org.test:mock-atomicfu-common:1.0"),
            binaryCoordinates("org.test:mock-coroutines-common:1.0"),
            binaryCoordinates("org.jetbrains.kotlin:kotlin-stdlib-common:1.3.10"),
        )
    }
}
