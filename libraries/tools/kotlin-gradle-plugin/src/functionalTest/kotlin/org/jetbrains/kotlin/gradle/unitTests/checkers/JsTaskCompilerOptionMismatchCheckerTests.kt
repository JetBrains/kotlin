/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.checkers

import org.gradle.api.Project
import org.gradle.api.Task
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import org.jetbrains.kotlin.gradle.targets.js.swc.GenerateSwcConfig
import org.jetbrains.kotlin.gradle.targets.js.typescript.KotlinJsDtsGenerationTask
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class JsTaskCompilerOptionMismatchCheckerTests {

    @Test
    fun `module kind set on the link tasks differently from the compilation - emits diagnostic`() {
        val project = buildJsProject {
            configureLinkTasks { it.compilerOptions.moduleKind.set(JsModuleKind.MODULE_ES) }
        }
        // The TypeScript declarations generation and webpack tasks follow the link tasks
        project.assertMismatches(linkTasksModuleKindEsMismatches)
    }

    @Test
    fun `module kind set via free compiler args of the link tasks differently from the compilation - emits diagnostic`() {
        val project = buildJsProject {
            configureLinkTasks { it.compilerOptions.freeCompilerArgs.addAll("-module-kind", "es") }
        }
        // The webpack tasks only follow the typed options of the link tasks
        project.assertMismatches(
            listOf(
                mismatch(":compileDevelopmentExecutableKotlinJs", "moduleKind", "es", "umd"),
                mismatch(":compileProductionExecutableKotlinJs", "moduleKind", "es", "umd"),
                mismatch(":compileTestDevelopmentExecutableKotlinJs", "moduleKind", "es", "umd"),
                mismatch(":compileTestProductionExecutableKotlinJs", "moduleKind", "es", "umd"),
                mismatch(":jsDevelopmentExecutableGenerateTypeScriptDefinitions", "moduleKind", "es", "umd"),
                mismatch(":jsProductionExecutableGenerateTypeScriptDefinitions", "moduleKind", "es", "umd"),
                mismatch(":jsTestTestDevelopmentExecutableGenerateTypeScriptDefinitions", "moduleKind", "es", "umd"),
                mismatch(":jsTestTestProductionExecutableGenerateTypeScriptDefinitions", "moduleKind", "es", "umd"),
            )
        )
    }

    @Test
    fun `target set on the link tasks differently from the compilation - emits diagnostic`() {
        val project = buildJsProject {
            configureLinkTasks { it.compilerOptions.target.set("es2015") }
        }
        // The es2015 target implies ES modules
        project.assertMismatches(
            listOf(
                mismatch(":compileDevelopmentExecutableKotlinJs", "moduleKind", "es", "umd"),
                mismatch(":compileDevelopmentExecutableKotlinJs", "target", "es2015", "es5"),
                mismatch(":compileProductionExecutableKotlinJs", "moduleKind", "es", "umd"),
                mismatch(":compileProductionExecutableKotlinJs", "target", "es2015", "es5"),
                mismatch(":compileTestDevelopmentExecutableKotlinJs", "moduleKind", "es", "umd"),
                mismatch(":compileTestDevelopmentExecutableKotlinJs", "target", "es2015", "es5"),
                mismatch(":compileTestProductionExecutableKotlinJs", "moduleKind", "es", "umd"),
                mismatch(":compileTestProductionExecutableKotlinJs", "target", "es2015", "es5"),
                mismatch(":jsBrowserDevelopmentRun", "esModules", "true", "false"),
                mismatch(":jsBrowserDevelopmentWebpack", "esModules", "true", "false"),
                mismatch(":jsBrowserProductionRun", "esModules", "true", "false"),
                mismatch(":jsBrowserProductionWebpack", "esModules", "true", "false"),
                mismatch(":jsDevelopmentExecutableGenerateTypeScriptDefinitions", "moduleKind", "es", "umd"),
                mismatch(":jsDevelopmentExecutableGenerateTypeScriptDefinitions", "target", "es2015", "es5"),
                mismatch(":jsProductionExecutableGenerateTypeScriptDefinitions", "moduleKind", "es", "umd"),
                mismatch(":jsProductionExecutableGenerateTypeScriptDefinitions", "target", "es2015", "es5"),
                mismatch(":jsTestTestDevelopmentExecutableGenerateTypeScriptDefinitions", "moduleKind", "es", "umd"),
                mismatch(":jsTestTestDevelopmentExecutableGenerateTypeScriptDefinitions", "target", "es2015", "es5"),
                mismatch(":jsTestTestProductionExecutableGenerateTypeScriptDefinitions", "moduleKind", "es", "umd"),
                mismatch(":jsTestTestProductionExecutableGenerateTypeScriptDefinitions", "target", "es2015", "es5"),
            )
        )
    }

    @Test
    fun `module kind set only on the compile task - emits diagnostic`() {
        val project = buildJsProject {
            kotlin {
                js().compilations.getByName(MAIN_COMPILATION_NAME).compileTaskProvider.configure {
                    it.compilerOptions.moduleKind.set(JsModuleKind.MODULE_ES)
                }
            }
        }
        // Only the main compilation uses ES modules, its link tasks still use the target's module kind
        project.assertMismatches(
            listOf(
                mismatch(":compileDevelopmentExecutableKotlinJs", "moduleKind", "umd", "es"),
                mismatch(":compileProductionExecutableKotlinJs", "moduleKind", "umd", "es"),
                mismatch(":jsBrowserDevelopmentRun", "esModules", "false", "true"),
                mismatch(":jsBrowserDevelopmentWebpack", "esModules", "false", "true"),
                mismatch(":jsBrowserProductionRun", "esModules", "false", "true"),
                mismatch(":jsBrowserProductionWebpack", "esModules", "false", "true"),
                mismatch(":jsDevelopmentExecutableGenerateTypeScriptDefinitions", "moduleKind", "umd", "es"),
                mismatch(":jsProductionExecutableGenerateTypeScriptDefinitions", "moduleKind", "umd", "es"),
            )
        )
    }

    @Test
    fun `module kind set on the TypeScript declarations generation tasks differently from the compilation - emits diagnostic`() {
        val project = buildJsProject {
            tasks.withType(KotlinJsDtsGenerationTask::class.java).configureEach {
                it.linkCompilerOptions.moduleKind.set(JsModuleKind.MODULE_ES)
            }
        }
        project.assertMismatches(
            listOf(
                mismatch(":jsDevelopmentExecutableGenerateTypeScriptDefinitions", "moduleKind", "es", "umd"),
                mismatch(":jsProductionExecutableGenerateTypeScriptDefinitions", "moduleKind", "es", "umd"),
                mismatch(":jsTestTestDevelopmentExecutableGenerateTypeScriptDefinitions", "moduleKind", "es", "umd"),
                mismatch(":jsTestTestProductionExecutableGenerateTypeScriptDefinitions", "moduleKind", "es", "umd"),
            )
        )
    }

    @Test
    fun `ES modules set on the webpack tasks differently from the compilation - emits diagnostic`() {
        val project = buildJsProject {
            tasks.withType(KotlinWebpack::class.java).configureEach { it.esModules.set(true) }
        }
        project.assertMismatches(
            listOf(
                mismatch(":jsBrowserDevelopmentRun", "esModules", "true", "false"),
                mismatch(":jsBrowserDevelopmentWebpack", "esModules", "true", "false"),
                mismatch(":jsBrowserProductionRun", "esModules", "true", "false"),
                mismatch(":jsBrowserProductionWebpack", "esModules", "true", "false"),
            )
        )
    }

    @Test
    fun `module kind set on the SWC config tasks differently from the compilation - emits diagnostic`() {
        val project = buildJsProject(delegatedTranspilation = true) {
            tasks.withType(GenerateSwcConfig::class.java).configureEach { it.moduleKind.set(JsModuleKind.MODULE_ES) }
        }
        project.assertMismatches(
            listOf(
                mismatch(":jsBrowserDevelopmentGenerateSwcConfig", "moduleKind", "es", "umd"),
                mismatch(":jsBrowserProductionGenerateSwcConfig", "moduleKind", "es", "umd"),
                mismatch(":jsBrowserTestDevelopmentGenerateSwcConfig", "moduleKind", "es", "umd"),
                mismatch(":jsBrowserTestProductionGenerateSwcConfig", "moduleKind", "es", "umd"),
            )
        )
    }

    @Test
    fun `target set on the SWC config tasks differently from the compilation - emits diagnostic`() {
        val project = buildJsProject(delegatedTranspilation = true) {
            tasks.withType(GenerateSwcConfig::class.java).configureEach { it.esTarget.set("es2015") }
        }
        // Without a module kind, SWC uses the one implied by the target
        project.assertMismatches(
            listOf(
                mismatch(":jsBrowserDevelopmentGenerateSwcConfig", "moduleKind", "es", "umd"),
                mismatch(":jsBrowserDevelopmentGenerateSwcConfig", "target", "es2015", "es5"),
                mismatch(":jsBrowserProductionGenerateSwcConfig", "moduleKind", "es", "umd"),
                mismatch(":jsBrowserProductionGenerateSwcConfig", "target", "es2015", "es5"),
                mismatch(":jsBrowserTestDevelopmentGenerateSwcConfig", "moduleKind", "es", "umd"),
                mismatch(":jsBrowserTestDevelopmentGenerateSwcConfig", "target", "es2015", "es5"),
                mismatch(":jsBrowserTestProductionGenerateSwcConfig", "moduleKind", "es", "umd"),
                mismatch(":jsBrowserTestProductionGenerateSwcConfig", "target", "es2015", "es5"),
            )
        )
    }

    @Test
    fun `default configuration - no diagnostic`() {
        val project = buildJsProject(delegatedTranspilation = true) { }
        project.assertNoDiagnostics(KotlinToolingDiagnostics.JsTaskCompilerOptionMismatch)
    }

    @Test
    fun `module kind set in the DSL - no diagnostic`() {
        val project = buildJsProject(delegatedTranspilation = true) {
            kotlin { js { compilerOptions.moduleKind.set(JsModuleKind.MODULE_ES) } }
        }
        project.assertNoDiagnostics(KotlinToolingDiagnostics.JsTaskCompilerOptionMismatch)
    }

    @Test
    fun `useEsModules - no diagnostic`() {
        val project = buildJsProject(delegatedTranspilation = true) {
            kotlin { js { useEsModules() } }
        }
        project.assertNoDiagnostics(KotlinToolingDiagnostics.JsTaskCompilerOptionMismatch)
    }

    @Test
    fun `same module kind set on the link tasks and in the DSL - no diagnostic`() {
        val project = buildJsProject {
            kotlin { js { compilerOptions.moduleKind.set(JsModuleKind.MODULE_ES) } }
            configureLinkTasks { it.compilerOptions.moduleKind.set(JsModuleKind.MODULE_ES) }
        }
        project.assertNoDiagnostics(KotlinToolingDiagnostics.JsTaskCompilerOptionMismatch)
    }

    @Test
    fun `different module kinds in two JS targets - no diagnostic`() {
        // Both targets have compilations with the same names, which mustn't be compared with each other's tasks
        val project = buildJsProject(delegatedTranspilation = true) {
            kotlin {
                js("esModules") {
                    browser()
                    binaries.executable()
                    useEsModules()
                }
            }
        }
        project.assertNoDiagnostics(KotlinToolingDiagnostics.JsTaskCompilerOptionMismatch)
    }

    private fun buildJsProject(delegatedTranspilation: Boolean = false, configure: Project.() -> Unit): Project =
        buildProjectWithMPP(
            preApplyCode = {
                if (delegatedTranspilation) propertiesExtension.set("kotlin.js.delegated.transpilation", "true")
            }
        ) {
            // The webpack tasks depend on the npm dependencies
            configureRepositoriesForTests()
            kotlin {
                js {
                    browser()
                    binaries.executable()
                    generateTypeScriptDefinitions()
                }
            }
            configure()
        }.also { project ->
            project.evaluate()
            // The checker runs once the task graph is ready, as in a build running all the checked tasks
            val checkedTasks = listOf(
                KotlinJsIrLink::class.java,
                KotlinJsDtsGenerationTask::class.java,
                KotlinWebpack::class.java,
                GenerateSwcConfig::class.java,
            ).flatMap<Class<out Task>, Task> { project.tasks.withType(it) }
            project.populateTaskGraph(*checkedTasks.toTypedArray())
        }

    private fun Project.configureLinkTasks(action: (KotlinJsIrLink) -> Unit) {
        tasks.withType(KotlinJsIrLink::class.java).configureEach(action)
    }

    /**
     * Asserts that all the mismatches are reported in a single diagnostic.
     */
    private fun Project.assertMismatches(expected: List<KotlinToolingDiagnostics.JsTaskCompilerOptionMismatch.Mismatch>) {
        val diagnostic = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(path)
            .assertContainsSingleDiagnostic(KotlinToolingDiagnostics.JsTaskCompilerOptionMismatch)
        assertEquals(KotlinToolingDiagnostics.JsTaskCompilerOptionMismatch(expected), diagnostic)
    }

    private fun mismatch(taskPath: String, optionName: String, taskValue: String, compilationValue: String) =
        KotlinToolingDiagnostics.JsTaskCompilerOptionMismatch.Mismatch(taskPath, optionName, taskValue, compilationValue)

    private val linkTasksModuleKindEsMismatches = listOf(
        mismatch(":compileDevelopmentExecutableKotlinJs", "moduleKind", "es", "umd"),
        mismatch(":compileProductionExecutableKotlinJs", "moduleKind", "es", "umd"),
        mismatch(":compileTestDevelopmentExecutableKotlinJs", "moduleKind", "es", "umd"),
        mismatch(":compileTestProductionExecutableKotlinJs", "moduleKind", "es", "umd"),
        mismatch(":jsBrowserDevelopmentRun", "esModules", "true", "false"),
        mismatch(":jsBrowserDevelopmentWebpack", "esModules", "true", "false"),
        mismatch(":jsBrowserProductionRun", "esModules", "true", "false"),
        mismatch(":jsBrowserProductionWebpack", "esModules", "true", "false"),
        mismatch(":jsDevelopmentExecutableGenerateTypeScriptDefinitions", "moduleKind", "es", "umd"),
        mismatch(":jsProductionExecutableGenerateTypeScriptDefinitions", "moduleKind", "es", "umd"),
        mismatch(":jsTestTestDevelopmentExecutableGenerateTypeScriptDefinitions", "moduleKind", "es", "umd"),
        mismatch(":jsTestTestProductionExecutableGenerateTypeScriptDefinitions", "moduleKind", "es", "umd"),
    )
}
