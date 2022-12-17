/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

@DisplayName("JS API validation")
class KotlinJsApiTest : KGPBaseTest() {
    @DisplayName("Kotlin compilation can be set up using APIs")
    @JsGradlePluginTests
    @GradleTest
    internal fun kotlinCompilationShouldRunIfSetUpWithApi(gradleVersion: GradleVersion) {
        project(
            projectName = "kotlinProject",
            gradleVersion = gradleVersion
        ) {
            projectPath.resolve("src").let {
                it.deleteRecursively()
                it.resolve("main/kotlin").createDirectories()
                it.resolve("main/kotlin/foo.kt").writeText(
                    """
                    class Foo
                    """.trimIndent()
                )
            }

            buildGradle.writeText(
                """
                plugins {
                    id "org.jetbrains.kotlin.js" apply false
                }

                repositories {
                    mavenLocal()
                    mavenCentral()
                }

                import org.jetbrains.kotlin.gradle.plugin.KotlinBaseApiPlugin
                KotlinBaseApiPlugin apiPlugin = plugins.apply(KotlinBaseApiPlugin.class)

                apiPlugin.registerKotlinJsCompileTask("foo").configure {
                    it.source("src/main/kotlin")
                    it.multiPlatformEnabled.set(false)
                    it.moduleName.set("main")
                    it.ownModuleName.set("main")
                    it.sourceSetName.set("main")
                    it.useModuleDetection.set(false)
                    it.destinationDirectory.fileValue(new File(project.buildDir, "fooOutput"))
                    it.compilerOptions.moduleName.set("main")
                }
                """.trimIndent()
            )

            val expectedOutput = projectPath.resolve("build/fooOutput/main.js")

            build("foo") {
                assertFileExists(expectedOutput)
                assertFileContains(expectedOutput, "function Foo()")
            }
        }
    }
}