/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.NullSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsOutputFileNamesTest {

    @ParameterizedTest
    @EnumSource(JsModuleKind::class)
    @NullSource
    fun `module kind set in target compiler options produces matching file names`(moduleKind: JsModuleKind?) {
        val (expectedJsExtension, expectedDtsExtension) = when (moduleKind) {
            JsModuleKind.MODULE_ES,
                -> "mjs" to "d.mts"
            JsModuleKind.MODULE_UMD,
            JsModuleKind.MODULE_COMMONJS,
            JsModuleKind.MODULE_AMD,
            JsModuleKind.MODULE_PLAIN,
            null,
                -> "js" to "d.ts"
        }
        val project = buildJsProject(moduleKind = moduleKind)
        assertOutputFileNames(project, expectedJsExtension, expectedDtsExtension)
    }

    @Test
    fun `es2015 target set in target compiler options produces mjs file names`() {
        val project = buildJsProject(target = "es2015")
        assertEsModuleOutputFileNames(project)
    }

    @Test
    fun `useEsModules produces mjs file names`() {
        val project = buildJsProject(useEsModules = true)
        assertEsModuleOutputFileNames(project)
    }

    @Test
    fun `KT-91228 - module kind set via free compiler args produces mjs file names`() {
        val project = buildJsProject(freeCompilerArgs = listOf("-module-kind", "es"))
        assertEsModuleOutputFileNames(project)
    }

    @Test
    fun `KT-91228 - es2015 target set via free compiler args produces mjs file names`() {
        val project = buildJsProject(freeCompilerArgs = listOf("-target", "es2015"))
        assertEsModuleOutputFileNames(project)
    }

    @Test
    fun `KT-91228 - module kind set via free compiler args takes precedence over the one in target compiler options`() {
        val project = buildJsProject(
            moduleKind = JsModuleKind.MODULE_UMD,
            freeCompilerArgs = listOf("-module-kind", "es"),
        )
        assertEsModuleOutputFileNames(project)
    }

    @ParameterizedTest
    @EnumSource(JsModuleKind::class, names = ["MODULE_ES", "MODULE_UMD"])
    fun `KT-91228 - module name set on the link tasks is used for the binaries' main file names`(moduleKind: JsModuleKind) {
        val expectedJsExtension = if (moduleKind == JsModuleKind.MODULE_ES) "mjs" else "js"
        val project = buildProjectWithMPP {
            kotlin {
                js {
                    nodejs()
                    binaries.executable()
                    compilerOptions.moduleKind.set(moduleKind)
                }
            }
            tasks.withType(KotlinJsIrLink::class.java).configureEach {
                it.compilerOptions.moduleName.set("custom-name")
            }
        }.evaluate()

        val binaries = project.multiplatformExtension.js().compilations.getByName(MAIN_COMPILATION_NAME)
            .binaries.withType(JsIrBinary::class.java)
        assertTrue(binaries.isNotEmpty(), "Expected JS binaries to be configured")

        assertAll(
            binaries.map { binary ->
                { assertEquals("custom-name.$expectedJsExtension", binary.mainFileName.orNull, "${binary.name} main file name") }
            }
        )
    }

    companion object {
        private fun buildJsProject(
            freeCompilerArgs: List<String> = emptyList(),
            useEsModules: Boolean = false,
            target: String? = null,
            moduleKind: JsModuleKind? = null,
        ): Project =
            buildProjectWithMPP {
                kotlin {
                    js {
                        nodejs()
                        binaries.executable()
                        generateTypeScriptDefinitions()
                        compilerOptions.freeCompilerArgs.addAll(freeCompilerArgs)
                        if (useEsModules) useEsModules()
                        if (target != null) compilerOptions.target.set(target)
                        if (moduleKind != null) compilerOptions.moduleKind.set(moduleKind)
                    }
                }
            }.evaluate()

        private fun assertEsModuleOutputFileNames(project: Project) =
            assertOutputFileNames(project, expectedJsExtension = "mjs", expectedDtsExtension = "d.mts")

        private fun assertOutputFileNames(project: Project, expectedJsExtension: String, expectedDtsExtension: String) {
            val compilation = project.multiplatformExtension.js().compilations.getByName(MAIN_COMPILATION_NAME)
            val npmProject = compilation.npmProject
            val moduleName = npmProject.name.orNull

            assertNotNull(moduleName, "Expected module name to be configured")

            val binaries = compilation.binaries.withType(JsIrBinary::class.java)
            assertTrue(binaries.isNotEmpty(), "Expected JS binaries to be configured")

            assertAll(
                buildList {
                    add { assertEquals("kotlin/$moduleName.$expectedJsExtension", npmProject.main.orNull, "package.json main") }
                    add { assertEquals("kotlin/$moduleName.$expectedDtsExtension", npmProject.typesFilePath.orNull, "package.json types") }
                    binaries.forEach { binary ->
                        add {
                            assertEquals("$moduleName.$expectedJsExtension", binary.mainFileName.orNull, "${binary.name} main file name")
                        }
                    }
                }
            )
        }
    }
}
