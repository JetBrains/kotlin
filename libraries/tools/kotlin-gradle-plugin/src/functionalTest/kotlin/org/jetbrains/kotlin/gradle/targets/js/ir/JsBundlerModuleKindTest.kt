/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Project
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.Companion.lenient
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.swc.GenerateSwcConfig
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.propertiesExtension
import org.jetbrains.kotlin.js.config.ModuleKind
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checks that webpack and SWC are configured for the module system the compiler generates, and that the link tasks
 * request the module kind the rest of the build expects when transpilation is delegated to SWC,
 * including when the module kind or target are set via free compiler arguments.
 */
class JsBundlerModuleKindTest {

    @Test
    fun `webpack uses ES modules when the module kind is set in target compiler options`() {
        val project = buildJsProject(moduleKind = JsModuleKind.MODULE_ES)
        assertWebpackEsModules(project, expected = true)
    }

    @Test
    fun `webpack doesn't use ES modules by default`() {
        val project = buildJsProject()
        assertWebpackEsModules(project, expected = false)
    }

    @Test
    fun `KT-91228 - webpack uses ES modules when the module kind is set via free compiler args`() {
        val project = buildJsProject(freeCompilerArgs = listOf("-module-kind", "es"))
        assertWebpackEsModules(project, expected = true)
    }

    @Test
    fun `KT-91228 - webpack uses ES modules when the es2015 target is set via free compiler args`() {
        val project = buildJsProject(freeCompilerArgs = listOf("-target", "es2015"))
        assertWebpackEsModules(project, expected = true)
    }

    @Test
    fun `KT-91228 - webpack module kind set via free compiler args takes precedence over the one in target compiler options`() {
        val project = buildJsProject(
            moduleKind = JsModuleKind.MODULE_ES,
            freeCompilerArgs = listOf("-module-kind", "umd"),
        )
        assertWebpackEsModules(project, expected = false)
    }

    @Test
    fun `SWC uses ES modules when the module kind is set in target compiler options`() {
        val project = buildJsProject(moduleKind = JsModuleKind.MODULE_ES, delegatedTranspilation = true)
        assertSwcConfig(project, expectedModuleKind = ModuleKind.ES, expectedTarget = "es5")
    }

    @Test
    fun `SWC uses UMD by default`() {
        val project = buildJsProject(delegatedTranspilation = true)
        assertSwcConfig(project, expectedModuleKind = ModuleKind.UMD, expectedTarget = "es5")
    }

    @Test
    fun `KT-91228 - SWC uses ES modules when the module kind is set via free compiler args`() {
        val project = buildJsProject(freeCompilerArgs = listOf("-module-kind", "es"), delegatedTranspilation = true)
        assertSwcConfig(project, expectedModuleKind = ModuleKind.ES, expectedTarget = "es5")
    }

    @Test
    fun `KT-91228 - SWC uses the target and ES modules when the es2015 target is set via free compiler args`() {
        val project = buildJsProject(freeCompilerArgs = listOf("-target", "es2015"), delegatedTranspilation = true)
        assertSwcConfig(project, expectedModuleKind = ModuleKind.ES, expectedTarget = "es2015")
    }

    @Test
    fun `KT-91228 - SWC module kind set via free compiler args takes precedence over the one in target compiler options`() {
        val project = buildJsProject(
            moduleKind = JsModuleKind.MODULE_ES,
            freeCompilerArgs = listOf("-module-kind", "umd"),
            delegatedTranspilation = true,
        )
        assertSwcConfig(project, expectedModuleKind = ModuleKind.UMD, expectedTarget = "es5")
    }

    @Test
    fun `delegated transpilation link uses ES modules when the es2015 target is set in target compiler options`() {
        val project = buildJsProject(target = "es2015", delegatedTranspilation = true)
        assertLinkModuleKind(project, expected = JsModuleKind.MODULE_ES)
    }

    @Test
    fun `delegated transpilation link uses UMD by default`() {
        val project = buildJsProject(delegatedTranspilation = true)
        assertLinkModuleKind(project, expected = JsModuleKind.MODULE_UMD)
    }

    @Test
    fun `KT-91228 - delegated transpilation link uses ES modules when the es2015 target is set via free compiler args`() {
        val project = buildJsProject(freeCompilerArgs = listOf("-target", "es2015"), delegatedTranspilation = true)
        assertLinkModuleKind(project, expected = JsModuleKind.MODULE_ES)
    }

    // Setting the module kind on the link tasks is deprecated, but it keeps working for everything except package.json
    @Test
    fun `KT-91228 - module kind set on the link tasks is used for the binaries and bundlers`() {
        val project = buildJsProject(linkTaskModuleKind = JsModuleKind.MODULE_ES, delegatedTranspilation = true)
        val compilation = project.multiplatformExtension.js().compilations.getByName(MAIN_COMPILATION_NAME)
        val moduleName = compilation.npmProject.name.get()
        val binaries = compilation.binaries.withType(JsIrBinary::class.java).toList()
        assertTrue(binaries.isNotEmpty(), "Expected JS binaries to be configured")

        assertAll(
            buildList {
                binaries.forEach { binary ->
                    add { assertEquals("$moduleName.mjs", binary.mainFileName.get(), "${binary.name} main file name") }
                }
                add { assertWebpackEsModules(project, expected = true) }
                add { assertSwcConfig(project, expectedModuleKind = ModuleKind.ES, expectedTarget = "es5") }
                // package.json follows the compilation, which doesn't set the module kind
                add { assertEquals("kotlin/$moduleName.js", compilation.npmProject.main.get(), "package.json main") }
            }
        )
    }

    companion object {
        private fun buildJsProject(
            freeCompilerArgs: List<String> = emptyList(),
            moduleKind: JsModuleKind? = null,
            target: String? = null,
            linkTaskModuleKind: JsModuleKind? = null,
            delegatedTranspilation: Boolean = false,
        ): Project =
            buildProjectWithMPP(
                preApplyCode = {
                    if (delegatedTranspilation) propertiesExtension.set("kotlin.js.delegated.transpilation", "true")
                }
            ) {
                kotlin {
                    js {
                        browser()
                        binaries.executable()
                        compilerOptions.freeCompilerArgs.addAll(freeCompilerArgs)
                        if (moduleKind != null) compilerOptions.moduleKind.set(moduleKind)
                        if (target != null) compilerOptions.target.set(target)
                    }
                }
                if (linkTaskModuleKind != null) {
                    tasks.withType(KotlinJsIrLink::class.java).configureEach { task ->
                        task.compilerOptions.moduleKind.set(linkTaskModuleKind)
                    }
                }
            }.evaluate()

        private fun assertWebpackEsModules(project: Project, expected: Boolean) {
            val webpackTasks = project.tasks.withType(KotlinWebpack::class.java).toList()
            assertTrue(webpackTasks.isNotEmpty(), "Expected webpack tasks to be configured")

            assertAll(webpackTasks.map { task ->
                { assertEquals(expected, task.esModules.get(), "${task.name} esModules") }
            })
        }

        private fun assertSwcConfig(project: Project, expectedModuleKind: ModuleKind, expectedTarget: String) {
            val swcConfigTasks = project.tasks.withType(GenerateSwcConfig::class.java).toList()
            assertTrue(swcConfigTasks.isNotEmpty(), "Expected SWC config tasks to be configured")

            assertAll(swcConfigTasks.flatMap { task ->
                listOf(
                    { assertEquals(expectedModuleKind, task.moduleSystemToUse.get(), "${task.name} module system") },
                    { assertEquals(expectedTarget, task.esTarget.get(), "${task.name} target") },
                )
            })
        }

        /**
         * Checks the module kind the compiler gets from the link tasks: their arguments are serialized as on the command line
         * (with the free compiler arguments last) and parsed with the compiler's parser.
         */
        private fun assertLinkModuleKind(project: Project, expected: JsModuleKind) {
            val linkTasks = project.tasks.withType(KotlinJsIrLink::class.java).toList()
            assertTrue(linkTasks.isNotEmpty(), "Expected link tasks to be configured")

            assertAll(linkTasks.map { task ->
                {
                    val commandLine = task.createCompilerArguments(lenient).toArgumentStrings()
                    val compilerArguments = parseCommandLineArguments<K2JSCompilerArguments>(commandLine)
                    assertEquals(expected.kind, compilerArguments.moduleKind, "${task.name} module kind")
                }
            })
        }
    }
}
