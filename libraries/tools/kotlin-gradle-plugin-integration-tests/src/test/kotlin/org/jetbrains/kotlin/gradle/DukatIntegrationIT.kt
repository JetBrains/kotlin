/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test

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
            projectName = projectName(dslType, dependenciesLocation),
            directoryPrefix = "dukat-integration"
        )
        project.setupWorkingDir()
        project.gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

        project.build("generateExternals") {
            assertSuccessful()
        }
    }

    @Test
    fun testIntegratedDukatKotlinDslRootDependencies() {
        testIntegratedDukat(
            DslType.KOTLIN,
            DependenciesLocation.ROOT
        )
    }

    @Test
    fun testIntegratedDukatKotlinDslExtDependencies() {
        testIntegratedDukat(
            DslType.KOTLIN,
            DependenciesLocation.EXTENSION
        )
    }

    @Test
    fun testIntegratedDukatGroovyDslRootDependencies() {
        testIntegratedDukat(
            DslType.GROOVY,
            DependenciesLocation.ROOT
        )
    }

    @Test
    fun testIntegratedDukatGroovyDslExtDependencies() {
        testIntegratedDukat(
            DslType.GROOVY,
            DependenciesLocation.EXTENSION
        )
    }

    private fun testIntegratedDukat(
        dslType: DslType,
        dependenciesLocation: DependenciesLocation
    ) {
        val projectName = projectName(dslType, dependenciesLocation)
        val project = Project(
            projectName = projectName,
            directoryPrefix = "dukat-integration"
        )
        project.setupWorkingDir()
        project.gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

        val externalSrcs = "build/externals/$projectName/src"
        project.build("generateExternalsIntegrated") {
            assertSuccessful()

            assertSingleFileExists(externalSrcs, "index.module_decamelize.kt")
        }
    }

    @Test
    fun testIntegratedDukatWithChangeKotlinDslRootDependencies() {
        testIntegratedDukatWithChange(
            DslType.KOTLIN,
            DependenciesLocation.ROOT
        )
    }

    @Test
    fun testIntegratedDukatWithChangeKotlinDslExtDependencies() {
        testIntegratedDukatWithChange(
            DslType.KOTLIN,
            DependenciesLocation.EXTENSION
        )
    }

    @Test
    fun testIntegratedDukatWithChangeGroovyDslRootDependencies() {
        testIntegratedDukatWithChange(
            DslType.GROOVY,
            DependenciesLocation.ROOT
        )
    }

    @Test
    fun testIntegratedDukatWithChangeGroovyDslExtDependencies() {
        testIntegratedDukatWithChange(
            DslType.GROOVY,
            DependenciesLocation.EXTENSION
        )
    }

    private fun testIntegratedDukatWithChange(
        dslType: DslType,
        dependenciesLocation: DependenciesLocation
    ) {
        val projectName = projectName(dslType, dependenciesLocation)
        val project = Project(
            projectName = projectName,
            directoryPrefix = "dukat-integration"
        )
        project.setupWorkingDir()
        project.gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

        val externalSrcs = "build/externals/$projectName/src"
        project.build("generateExternalsIntegrated") {
            assertSuccessful()

            assertSingleFileExists(externalSrcs, "index.module_decamelize.kt")
        }

        project.gradleBuildScript().modify { buildScript ->
            buildScript
                .replace(
                    """implementation(npm("left-pad", "1.3.0"))""",
                    """implementation(npm("left-pad", "1.3.0", true))"""
                )
                .replace(
                    """implementation(npm("decamelize", "4.0.0", true))""",
                    """implementation(npm("decamelize", "4.0.0"))"""
                )
        }

        project.build("generateExternalsIntegrated") {
            assertSuccessful()

            assertSingleFileExists(externalSrcs, "index.module_left-pad.kt")
        }
    }

    @Test
    fun testIntegratedDukatWithFalseDefaultKotlinDslRootDependencies() {
        testIntegratedDukatWithFalseDefault(
            DslType.KOTLIN,
            DependenciesLocation.ROOT
        )
    }

    @Test
    fun testIntegratedDukatWithFalseDefaultKotlinDslExtDependencies() {
        testIntegratedDukatWithFalseDefault(
            DslType.KOTLIN,
            DependenciesLocation.EXTENSION
        )
    }

    @Test
    fun testIntegratedDukatWithFalseDefaultGroovyDslRootDependencies() {
        testIntegratedDukatWithFalseDefault(
            DslType.GROOVY,
            DependenciesLocation.ROOT
        )
    }

    @Test
    fun testIntegratedDukatWithFalseDefaultGroovyDslExtDependencies() {
        testIntegratedDukatWithFalseDefault(
            DslType.GROOVY,
            DependenciesLocation.EXTENSION
        )
    }

    private fun testIntegratedDukatWithFalseDefault(
        dslType: DslType,
        dependenciesLocation: DependenciesLocation
    ) {
        val projectName = projectName(dslType, dependenciesLocation)
        val project = Project(
            projectName = projectName,
            directoryPrefix = "dukat-integration"
        )
        project.setupWorkingDir()
        project.gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
        project.gradleProperties().modify {
            "kotlin.js.generate.externals=true"
        }

        project.gradleBuildScript().modify { buildScript ->
            buildScript
                .replace(
                    """implementation(npm("decamelize", "4.0.0", true))""",
                    """implementation(npm("decamelize", "4.0.0", false))"""
                )
        }

        val externalSrcs = "build/externals/$projectName/src"
        project.build("generateExternalsIntegrated") {
            assertSuccessful()

            assertSingleFileExists(externalSrcs, "index.module_left-pad.kt")
        }
    }

    private fun projectName(
        dslType: DslType,
        dependenciesLocation: DependenciesLocation
    ): String =
        "${dslType.value}-${dependenciesLocation.value}"

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
