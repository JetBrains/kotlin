/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.util.zip.ZipFile
import kotlin.test.assertNotNull

// TODO: This suite is failing with deprecation error on Gradle <7.0 versions
// Should be fixed via planned fixes in Kotlin/JS plugin: https://youtrack.jetbrains.com/issue/KFC-252
@GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0)
@DisplayName("Kotlin/JS IR library")
@JsGradlePluginTests
class KotlinJsIrLibraryGradlePluginIT : KGPBaseTest() {

    override val defaultBuildOptions = super.defaultBuildOptions.copy(
        jsOptions = BuildOptions.JsOptions(
            useIrBackend = true,
            jsCompilerType = KotlinJsCompilerType.IR
        )
    )

    @DisplayName("simple binary library")
    @GradleTest
    fun testSimpleJsBinaryLibrary(gradleVersion: GradleVersion) {
        project("simple-js-library", gradleVersion) {
            build("build") {
                assertFileInProjectExists("build/productionLibrary/js-library.js")
                assertFileInProjectExists("build/productionLibrary/package.json")
                assertFileInProjectExists("build/productionLibrary/main.js")
            }
        }
    }

    @DisplayName("binary library and executable")
    @GradleTest
    fun testJsBinaryLibraryAndExecutable(gradleVersion: GradleVersion) {
        project("js-library-with-executable", gradleVersion) {
            build("build") {
                assertFileInProjectExists("build/productionLibrary/js-library.js")
                assertFileInProjectExists("build/productionLibrary/package.json")
                assertFileInProjectExists("build/productionLibrary/main.js")
            }
        }
    }

    @DisplayName("binary library and executable both for browser and nodejs")
    @GradleTest
    fun testJsBinaryLibraryAndExecutableForBrowserAndNodejs(gradleVersion: GradleVersion) {
        project("js-library-with-executable-browser-nodejs", gradleVersion) {
            build("build") {
                assertFileInProjectExists("build/productionLibrary/js-library.js")
                assertFileInProjectExists("build/productionLibrary/package.json")
                assertFileInProjectExists("build/productionLibrary/main.js")

                assertFileInProjectExists("build/distributions/js-library.js")
            }
        }
    }

    @DisplayName("publish sources jar task should also include dukat outputs")
    @GradleTest
    fun testPublishSourcesJarTaskShouldAlsoIncludeDukatTaskOutputs(gradleVersion: GradleVersion) {
        project("js-library-ir", gradleVersion) {
            build("sourcesJar") {
                val sourcesJarFilePath = "build/libs/js-library-ir-kotlin-sources.jar"
                assertFileInProjectExists(sourcesJarFilePath)
                ZipFile(projectPath.resolve(sourcesJarFilePath).toFile()).use {
                    assertNotNull(it.getEntry("jsMain/index.module_decamelize.kt"))
                }
            }
        }
    }
}