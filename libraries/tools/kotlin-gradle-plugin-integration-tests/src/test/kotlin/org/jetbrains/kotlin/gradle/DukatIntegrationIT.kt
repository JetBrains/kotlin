/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectModules
import org.jetbrains.kotlin.gradle.targets.js.npm.fromSrcPackageJson
import org.jetbrains.kotlin.gradle.tasks.USING_JS_INCREMENTAL_COMPILATION_MESSAGE
import org.jetbrains.kotlin.gradle.tasks.USING_JS_IR_BACKEND_MESSAGE
import org.jetbrains.kotlin.gradle.util.*
import org.junit.Assume.assumeFalse
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DukatIntegrationIT : BaseGradleIT() {
    @Test
    fun testSeparateDukatKotlinDslRootDependencies() {
        testSeparateDukat(
            DslType.KOTLIN,
            DependenciesLocation.ROOT
        )
    }

    @Test
    fun testSeparateDukatKotlinDslExtDependencies() {
        testSeparateDukat(
            DslType.KOTLIN,
            DependenciesLocation.EXTENSION
        )
    }

    @Test
    fun testSeparateDukatGroovyDslRootDependencies() {
        testSeparateDukat(
            DslType.GROOVY,
            DependenciesLocation.ROOT
        )
    }

    @Test
    fun testSeparateDukatGroovyDslExtDependencies() {
        testSeparateDukat(
            DslType.GROOVY,
            DependenciesLocation.EXTENSION
        )
    }

    private fun testSeparateDukat(
        dslType: DslType,
        dependenciesLocation: DependenciesLocation
    ) {
        val project = Project(
            projectName = "${dslType.value}-${dependenciesLocation.value}",
            directoryPrefix = "dukat-integration"
        )
        project.setupWorkingDir()
        project.gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

        project.build("generateExternals") {
            assertSuccessful()
        }
    }

    private enum class DslType(
        val value: String
    ) {
        KOTLIN("kotlin-dsl"),
        GROOVY("groovy-dsl")
    }

    private enum class DependenciesLocation(
        val value: String
    ) {
        ROOT("root"),
        EXTENSION("ext")
    }
}
