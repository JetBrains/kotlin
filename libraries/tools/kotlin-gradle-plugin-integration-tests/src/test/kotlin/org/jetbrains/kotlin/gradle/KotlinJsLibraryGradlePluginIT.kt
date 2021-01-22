/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test

class KotlinJsIrLibraryGradlePluginIT : BaseGradleIT() {
    override fun defaultBuildOptions(): BuildOptions =
        super.defaultBuildOptions().copy(
            jsIrBackend = true,
            jsCompilerType = KotlinJsCompilerType.IR
        )

    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.AtLeast("6.0")

    @Test
    fun testSimpleJsBinaryLibrary() {
        val project = Project("simple-js-library")

        project.setupWorkingDir()
        project.gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
        project.build("build") {
            assertSuccessful()

            assertFileExists("build/productionLibrary/js-library.js")
            assertFileExists("build/productionLibrary/package.json")
            assertFileExists("build/productionLibrary/main.js")
        }
    }

    @Test
    fun testJsBinaryLibraryAndExecutable() {
        val project = Project("js-library-with-executable")

        project.setupWorkingDir()
        project.gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
        project.build("build") {
            assertSuccessful()

            assertFileExists("build/productionLibrary/js-library.js")
            assertFileExists("build/productionLibrary/package.json")
            assertFileExists("build/productionLibrary/main.js")
        }
    }

    @Test
    fun testJsBinaryLibraryAndExecutableForBrowserAndNodejs() {
        val project = Project("js-library-with-executable-browser-nodejs")

        project.setupWorkingDir()
        project.gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
        project.build("build") {
            assertSuccessful()

            assertFileExists("build/productionLibrary/js-library.js")
            assertFileExists("build/productionLibrary/package.json")
            assertFileExists("build/productionLibrary/main.js")

            assertFileExists("build/distributions/js-library.js")
        }
    }
}