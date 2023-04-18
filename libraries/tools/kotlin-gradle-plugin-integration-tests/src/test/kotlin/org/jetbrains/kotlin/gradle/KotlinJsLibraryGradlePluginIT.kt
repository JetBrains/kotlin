/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.targets.js.dsl.Distribution.Companion.DIST
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.reader
import kotlin.test.assertNotNull

// TODO: This suite is failing with deprecation error on Gradle <7.0 versions
// Should be fixed via planned fixes in Kotlin/JS plugin: https://youtrack.jetbrains.com/issue/KFC-252
abstract class KotlinJsIrLibraryGradlePluginITBase : KGPBaseTest() {

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
                assertFileInProjectExists("build/$DIST/js/productionLibrary/js-library.js")
                assertFileInProjectExists("build/$DIST/js/productionLibrary/package.json")
                assertFileInProjectExists("build/$DIST/js/productionLibrary/main.js")
                projectPath.resolve("build/$DIST/js/productionLibrary/package.json").reader()
                    .use { Gson().fromJson(it, JsonObject::class.java) }
                    .getAsJsonObject("dependencies")
                    ?.entrySet()?.associate { (k, v) -> k to v.asString }
                    .let { dependencies ->
                        assertNotNull(dependencies?.get("kotlin")) { "Direct npm dependency missing in package.json" }
                        assertNotNull(dependencies?.get("@js-joda/core")) { "Transitive npm dependency missing in package.json" }
                    }
            }
        }
    }

    @DisplayName("binary library and executable")
    @GradleTest
    fun testJsBinaryLibraryAndExecutable(gradleVersion: GradleVersion) {
        project("js-library-with-executable", gradleVersion) {
            build("build") {
                assertFileInProjectExists("build/$DIST/js/productionLibrary/js-library.js")
                assertFileInProjectExists("build/$DIST/js/productionLibrary/package.json")
                assertFileInProjectExists("build/$DIST/js/productionLibrary/main.js")
            }
        }
    }

    @DisplayName("binary library and executable both for browser and nodejs")
    @GradleTest
    fun testJsBinaryLibraryAndExecutableForBrowserAndNodejs(gradleVersion: GradleVersion) {
        project("js-library-with-executable-browser-nodejs", gradleVersion) {
            build("build") {
                assertFileInProjectExists("build/$DIST/js/productionLibrary/js-library.js")
                assertFileInProjectExists("build/$DIST/js/productionLibrary/package.json")
                assertFileInProjectExists("build/$DIST/js/productionLibrary/main.js")

                assertFileInProjectExists("build/$DIST/js/productionExecutable/js-library.js")
            }
        }
    }
}

@GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0)
@DisplayName("Kotlin/JS K1 IR library")
@JsGradlePluginTests
class KotlinK1JsIrLibraryGradlePluginIT : KotlinJsIrLibraryGradlePluginITBase() {
    override val defaultBuildOptions = super.defaultBuildOptions.copy(languageVersion = null)
}

@GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0)
@DisplayName("Kotlin/JS K2 IR library")
@JsGradlePluginTests
class KotlinK2JsIrLibraryGradlePluginIT : KotlinJsIrLibraryGradlePluginITBase() {
    override val defaultBuildOptions = super.defaultBuildOptions.copy(languageVersion = "2.0")
}
