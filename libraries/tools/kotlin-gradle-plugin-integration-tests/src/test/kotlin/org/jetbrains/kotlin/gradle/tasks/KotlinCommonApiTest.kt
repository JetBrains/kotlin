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

@DisplayName("Common API validation")
class KotlinCommonApiTest : KGPBaseTest() {
    @DisplayName("Kotlin Metadata compilation can be set up using APIs")
    @MppGradlePluginTests
    @GradleTest
    internal fun kotlinCompilationShouldRunIfSetUpWithApi(gradleVersion: GradleVersion) {
        project(
            projectName = "kotlinProject",
            gradleVersion = gradleVersion
        ) {
            projectPath.resolve("src").let {
                it.deleteRecursively()
                it.resolve("commonMain/kotlin").createDirectories()
                it.resolve("commonMain/kotlin/foo.kt").writeText(
                    """
                    class Foo
                    """.trimIndent()
                )
            }

            buildGradle.writeText(
                """
                plugins {
                    id "org.jetbrains.kotlin.multiplatform" apply false
                }

                repositories {
                    mavenLocal()
                    mavenCentral()
                }

                import org.jetbrains.kotlin.gradle.plugin.KotlinBaseApiPlugin
                KotlinBaseApiPlugin apiPlugin = plugins.apply(KotlinBaseApiPlugin.class)

                apiPlugin.registerKotlinMetadataCompileTask("foo").configure {
                    it.source("src/commonMain/kotlin")
                    it.multiPlatformEnabled.set(false)
                    it.moduleName.set("main")
                    it.ownModuleName.set("main")
                    it.sourceSetName.set("main")
                    it.useModuleDetection.set(false)
                    it.destinationDirectory.fileValue(new File(project.buildDir, "fooOutput"))
                }
                """.trimIndent()
            )

            val expectedOutput = projectPath.resolve("build/fooOutput/default/manifest")

            build("foo") {
                assertFileExists(expectedOutput)
            }
        }
    }
}