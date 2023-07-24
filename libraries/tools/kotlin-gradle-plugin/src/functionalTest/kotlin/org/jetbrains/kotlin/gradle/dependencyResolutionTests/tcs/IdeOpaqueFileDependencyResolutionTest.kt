/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.dependencyResolutionTests.tcs

import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.jvmMain
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.extras.isOpaqueFileDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.*
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImport
import org.jetbrains.kotlin.gradle.util.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.enableDefaultStdlibDependency
import org.jetbrains.kotlin.gradle.util.enableDependencyVerification
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class IdeOpaqueFileDependencyResolutionTest {
    @Test
    fun `test - simple jar file`() {
        val project = buildProject {
            enableDefaultStdlibDependency(false)
            enableDependencyVerification(false)
            applyMultiplatformPlugin()
            repositories.mavenCentralCacheRedirector()

            val kotlin = project.multiplatformExtension
            kotlin.jvm()

            /*
            Setup opaque file dependency to 'foo.jar'
             */
            kotlin.sourceSets.jvmMain.dependencies {
                implementation(project.files("libs/foo.jar"))
            }
        }

        project.evaluate()

        /* Check dependencies on jvmMain */
        val fooJarMatcher = binaryCoordinates(Regex(""".*:libs[/\\]foo\.jar"""))

        val fooJarDependency = project.kotlinIdeMultiplatformImport.resolveDependencies("jvmMain").assertMatches(
            dependsOnDependency(":/commonMain"),
            fooJarMatcher
        ).getOrFail(fooJarMatcher)

        if (fooJarDependency !is IdeaKotlinResolvedBinaryDependency) {
            fail("Expected 'foo.jar' to be resolved. Found $fooJarDependency")
        }

        assertTrue(
            fooJarDependency.isOpaqueFileDependency,
            "Expected 'foo.jar' to be marked as 'opaque file dependency'"
        )

        /* Check dependencies on jvmTest */
        project.kotlinIdeMultiplatformImport.resolveDependencies("jvmTest").assertMatches(
            friendSourceDependency(":/commonMain"),
            friendSourceDependency(":/jvmMain"),
            dependsOnDependency(":/commonTest"),
            fooJarMatcher
        )
    }
}