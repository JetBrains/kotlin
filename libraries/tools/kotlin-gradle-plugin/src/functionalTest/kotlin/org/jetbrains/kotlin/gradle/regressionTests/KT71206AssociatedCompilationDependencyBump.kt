/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.regressionTests

import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonTest
import org.jetbrains.kotlin.gradle.internal.dsl.KotlinMultiplatformSourceSetConventionsImpl.dependencies
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.util.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.enableDependencyVerification
import org.jetbrains.kotlin.test.MuteableTestRule
import org.junit.Rule
import kotlin.test.Test

class KT71206AssociatedCompilationDependencyBump {
    @get:Rule val muteableTestRule = MuteableTestRule()

    @Test
    fun nativeDependencyBumpInTest() {
        val project = buildProject {
            enableDependencyVerification(false)
            repositories.mavenCentralCacheRedirector()
            repositories.mavenLocal()
            applyMultiplatformPlugin()
        }

        val kotlin = project.multiplatformExtension
        val target = kotlin.linuxX64()

        kotlin.sourceSets.commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
        }
        kotlin.sourceSets.commonTest.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
        }
        project.evaluate()

        fun assertCoroutinesExactlyOnceWithVersion(compilationName: String, version: String) {
            val coroutinesArtifact =
                target.compilations.getByName(compilationName).compileTaskProvider.get().libraries.singleOrNull { it.name.contains("kotlinx-coroutines-core") }
                    ?: error(
                        """
                        |Expected to see kotlinx-coroutines-core exactly once in $compilationName, but was: 
                        |${target.compilations.getByName(compilationName).compileTaskProvider.get().libraries.joinToString("\n")}
                        """.trimMargin()
                    )
            assert(version in coroutinesArtifact.path) {
                "Expected $coroutinesArtifact to have version $version"
            }
        }
        assertCoroutinesExactlyOnceWithVersion(KotlinCompilation.MAIN_COMPILATION_NAME, "1.6.1")
        assertCoroutinesExactlyOnceWithVersion(KotlinCompilation.TEST_COMPILATION_NAME, "1.6.4")
    }
}