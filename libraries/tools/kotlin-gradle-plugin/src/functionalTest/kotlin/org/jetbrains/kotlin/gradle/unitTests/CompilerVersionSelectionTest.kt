/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalDependency
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.internal.KOTLIN_BUILD_TOOLS_API_IMPL
import org.jetbrains.kotlin.gradle.internal.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.plugin.BUILD_TOOLS_API_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Configuration-phase counterpart of the former KGP `CompilerVersionChooseIT`: the Build Tools API classpath
 * configuration must pin the compiler implementation to the version chosen via `kotlin.compilerVersion` (the
 * Kotlin plugin version by default). Verified by inspecting the configuration's declared dependency without
 * resolving artifacts. Scenarios that need real dependency resolution (e.g. that a manually added
 * `kotlin-compiler-embeddable` does not override the chosen version) stay in the integration test.
 */
class CompilerVersionSelectionTest {

    @Test
    fun testDefaultCompilerVersionIsThePluginVersion() {
        val project = buildProjectWithJvm()
        project.evaluate()

        assertEquals(
            project.getKotlinPluginVersion(),
            project.buildToolsApiImplStrictVersion(),
        )
    }

    @Test
    fun testCompilerVersionCanBeChangedViaExtension() {
        val project = buildProjectWithJvm()
        project.kotlinJvmExtension.compilerVersion.set("2.0.0")
        project.evaluate()

        assertEquals(
            "2.0.0",
            project.buildToolsApiImplStrictVersion(),
        )
    }

    @Test
    fun testCompilerVersionRespectsTheLastValueSet() {
        val project = buildProjectWithJvm()
        project.kotlinJvmExtension.compilerVersion.set(project.getKotlinPluginVersion())
        project.kotlinJvmExtension.compilerVersion.set("2.0.0")
        project.evaluate()

        assertEquals(
            "2.0.0",
            project.buildToolsApiImplStrictVersion(),
        )
    }

    private fun Project.buildToolsApiImplStrictVersion(): String {
        val implDependency = configurations
            .getByName(BUILD_TOOLS_API_CLASSPATH_CONFIGURATION_NAME)
            .incoming
            .dependencies
            .single { it.group == KOTLIN_MODULE_GROUP && it.name == KOTLIN_BUILD_TOOLS_API_IMPL }
        return (implDependency as ExternalDependency).versionConstraint.strictVersion
    }
}
