/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.apple

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.lazyMapWithCC
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.GradleTestVersions
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.NativeGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.SwiftPMImportGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.TestVersions
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.testbase.withConfigurationCache
import kotlin.test.assertEquals

@SwiftPMImportGradlePluginTests
class LazyMapWithCCTest : KGPBaseTest() {

    @GradleTestVersions(TestVersions.Gradle.G_8_0)
    @GradleTest
    fun test(version: GradleVersion) {
        project("empty", version) {
            plugins {
                kotlin("multiplatform").apply(false)
            }
            val eagerResult = projectPath.resolve("eagerResult").toFile()
            buildScriptInjection {
                val eagerOutput = project.layout.projectDirectory.file("eager").asFile
                val eager = project.tasks.register("eager") {
                    it.doLast {
                        eagerOutput.writeText("eager")
                    }
                }.map {
                    eagerOutput.exists()
                }
                project.tasks.register("writeEager") {
                    it.dependsOn(eager)
                    it.doLast {
                        eagerResult.writeText(eager.get().toString())
                    }
                }
            }
            build("writeEager", buildOptions = defaultBuildOptions.withConfigurationCache)
            assertEquals("false", eagerResult.readText())

            val lazyResult = projectPath.resolve("lazyResult").toFile()
            buildScriptInjection {
                val lazyOutput = project.layout.projectDirectory.file("lazy").asFile
                val lazy = project.tasks.register("lazy") {
                    it.doLast {
                        lazyOutput.writeText("lazy")
                    }
                }.lazyMapWithCC {
                    lazyOutput.readText()
                }
                project.tasks.register("writeLazy") {
                    it.dependsOn(lazy)
                    it.doLast {
                        lazyResult.writeText(lazy.get().toString())
                    }
                }
            }
            build("writeLazy", buildOptions = defaultBuildOptions.withConfigurationCache)
            assertEquals("lazy", lazyResult.readText())
        }
    }
}