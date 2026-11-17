/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Project
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.js.targetVersion
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptionsHelper
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.TEST_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.Companion.lenient
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.swc.GenerateSwcConfig
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.propertiesExtension
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.js.config.EcmaVersion
import org.jetbrains.kotlin.js.config.supportsEsModules
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Records, for every way of configuring the JS module kind and target, what the compiler gets from each binary's link task
 * and TypeScript declarations generation task, and what the rest of the build (package.json, the binary's main file, webpack, SWC)
 * expects. All the binaries are checked: executables and libraries, development and production, of the main and test compilations.
 *
 * The expectations document the current behaviour, including the cases where the build disagrees with the compiler
 * (marked with "Inconsistent"), so that any change in behaviour fails a test.
 *
 * KT-91228
 */
class JsModuleKindConsistencyTest {

    @TestFactory
    fun `module kind and target as seen by the compiler and the rest of the build`(): List<DynamicNode> =
        Scenario.entries.map { scenario ->
            dynamicContainer(
                scenario.description,
                scenario.testedModuleKinds.map { moduleKind ->
                    dynamicContainer(
                        "moduleKind: ${moduleKind.kind}",
                        listOf(false, true).map { delegated ->
                            dynamicContainer(
                                "delegated transpilation $delegated",
                                outcomeTests(scenario, moduleKind, delegated)
                            )
                        }
                    )
                })
        }

    /**
     * A test for each binary. The project is built and observed by the first test that runs, and shared by the others.
     */
    private fun outcomeTests(
        scenario: Scenario,
        moduleKind: JsModuleKind,
        delegated: Boolean,
    ): List<DynamicTest> {
        val expected = if (delegated) {
            scenario.expectedWithDelegatedTranspilation(moduleKind.kind)
        } else {
            scenario.expected(moduleKind.kind)
        }
        val defaultExpected = if (delegated) {
            Scenario.Default.expectedWithDelegatedTranspilation(moduleKind.kind)
        } else {
            Scenario.Default.expected(moduleKind.kind)
        }
        val observed by lazy { observe( scenario, moduleKind, delegated) }

        return buildList {
            add(dynamicTest("binaries") {
                assertEquals(allBinaries, observed.keys.map { it.toString() }.toSet())
            })
            allBinaries.forEach { binaryName ->
                add(dynamicTest(binaryName) {
                    val (binary, outcome) = assertNotNull(
                        observed.entries.find { it.key.toString() == binaryName },
                        "Expected the binary $binaryName to be configured",
                    )
                    val expectedForCompilation =
                        if (binary.compilation == TEST_COMPILATION_NAME && !scenario.affectsTestCompilation) {
                            defaultExpected
                        } else {
                            expected
                        }
                    // Only the executables of the main compilation are bundled with webpack
                    val expectedForBinary = expectedForCompilation.copy(
                        webpackEsModules = expectedForCompilation.webpackEsModules.takeIf { binary.hasWebpack }
                    )
                    assertEquals(expectedForBinary, outcome)
                })
            }
        }
    }

    private data class BinaryId(val compilation: String, val binary: String, val hasWebpack: Boolean) {
        override fun toString(): String = "$compilation $binary"
    }

    private val allBinaries = setOf(
        "main developmentExecutable",
        "main productionExecutable",
        "main developmentLibrary",
        "main productionLibrary",
        "test testDevelopmentExecutable",
        "test testProductionExecutable",
    )

    /**
     * What the compiler gets and what the rest of the build expects.
     * File names are reduced to their extensions. SWC is only configured with delegated transpilation.
     */
    private data class Outcome(
        val compilerModuleKind: String?,
        val compilerTarget: String?,
        val dtsModuleKind: String?,
        val dtsTarget: String?,
        val swcModuleKind: String?,
        val swcTarget: String?,
        val packageJsonMain: String?,
        val packageJsonTypes: String?,
        val mainFileName: String?,
        val webpackEsModules: Boolean?,
    ) {
        override fun toString(): String =
            buildString {
                appendLine("compilerModuleKind: $compilerModuleKind")
                appendLine("compilerTarget: $compilerTarget")
                appendLine("dtsModuleKind: $dtsModuleKind")
                appendLine("dtsTarget: $dtsTarget")
                appendLine("swcModuleKind: $swcModuleKind")
                appendLine("swcTarget: $swcTarget")
                appendLine("packageJsonMain: $packageJsonMain")
                appendLine("packageJsonTypes: $packageJsonTypes")
                appendLine("mainFileName: $mainFileName")
                appendLine("webpackEsModules: $webpackEsModules")
            }
    }

    private enum class Scenario(
        val description: String,
        val testedModuleKinds: List<JsModuleKind> = listOf(JsModuleKind.MODULE_UMD),
        // Otherwise, the test compilation behaves as by default
        val affectsTestCompilation: Boolean = true,
    ) {
        Default("default") {
            override fun Project.configure(moduleKind: JsModuleKind) {}
            override fun expected(kind: String) =
                Outcome("umd", "es5", "umd", "es5", null, null, "js", "d.ts", "js", false)

            override fun expectedWithDelegatedTranspilation(kind: String) =
                Outcome("umd", "es2020", "umd", "es5", "umd", "es5", "js", "d.ts", "js", false)
        },

        DslTarget("DSL target = es2015") {
            override fun Project.configure(moduleKind: JsModuleKind) {
                kotlin { js { compilerOptions.target.set("es2015") } }
            }

            override fun expected(kind: String) =
                Outcome("es", "es2015", "es", "es2015", null, null, "mjs", "d.mts", "mjs", true)

            override fun expectedWithDelegatedTranspilation(kind: String) =
                Outcome("es", "es2020", "es", "es2015", "es", "es2015", "mjs", "d.mts", "mjs", true)
        },

        DslUseEsModules("DSL useEsModules()") {
            override fun Project.configure(moduleKind: JsModuleKind) {
                kotlin { js { useEsModules() } }
            }

            override fun expected(kind: String) =
                Outcome("es", "es5", "es", "es5", null, null, "mjs", "d.mts", "mjs", true)

            override fun expectedWithDelegatedTranspilation(kind: String) =
                Outcome("es", "es2020", "es", "es5", "es", "es5", "mjs", "d.mts", "mjs", true)
        },

        DslUseCommonJs("DSL useCommonJs()") {
            override fun Project.configure(moduleKind: JsModuleKind) {
                kotlin { js { useCommonJs() } }
            }

            override fun expected(kind: String) =
                Outcome("commonjs", "es5", "commonjs", "es5", null, null, "js", "d.ts", "js", false)

            override fun expectedWithDelegatedTranspilation(kind: String) =
                Outcome("commonjs", "es2020", "commonjs", "es5", "commonjs", "es5", "js", "d.ts", "js", false)
        },

        DslModuleKind("DSL moduleKind =", testedModuleKinds = JsModuleKind.entries) {
            override fun Project.configure(moduleKind: JsModuleKind) {
                kotlin { js { compilerOptions.moduleKind.set(moduleKind) } }
            }

            override fun expected(kind: String) =
                Outcome(kind, "es5", kind, "es5", null, null, jsExtension(kind), dtsExtension(kind), jsExtension(kind), isEs(kind))

            override fun expectedWithDelegatedTranspilation(kind: String) =
                Outcome(kind, "es2020", kind, "es5", kind, "es5", jsExtension(kind), dtsExtension(kind), jsExtension(kind), isEs(kind))
        },

        DslFreeCompilerArgsModuleKind("DSL freeCompilerArgs -module-kind", testedModuleKinds = JsModuleKind.entries) {
            override fun Project.configure(moduleKind: JsModuleKind) {
                kotlin { js { compilerOptions.freeCompilerArgs.addAll("-module-kind", moduleKind.kind) } }
            }

            override fun expected(kind: String) =
                Outcome(kind, "es5", kind, "es5", null, null, jsExtension(kind), dtsExtension(kind), jsExtension(kind), isEs(kind))

            override fun expectedWithDelegatedTranspilation(kind: String) =
                Outcome(kind, "es2020", kind, "es5", kind, "es5", jsExtension(kind), dtsExtension(kind), jsExtension(kind), isEs(kind))
        },

        DslFreeCompilerArgsTarget("DSL freeCompilerArgs -target es2015") {
            override fun Project.configure(moduleKind: JsModuleKind) {
                kotlin { js { compilerOptions.freeCompilerArgs.addAll("-target", "es2015") } }
            }

            override fun expected(kind: String) =
                Outcome("es", "es2015", "es", "es2015", null, null, "mjs", "d.mts", "mjs", true)

            // The free compiler arguments come last, so they override the latest target requested by KGP
            override fun expectedWithDelegatedTranspilation(kind: String) =
                Outcome("es", "es2015", "es", "es2015", "es", "es2015", "mjs", "d.mts", "mjs", true)
        },

        DslModuleKindAndFreeCompilerArgsModuleKind("DSL moduleKind = es, freeCompilerArgs -module-kind umd") {
            override fun Project.configure(moduleKind: JsModuleKind) {
                kotlin {
                    js {
                        compilerOptions.moduleKind.set(JsModuleKind.MODULE_ES)
                        compilerOptions.freeCompilerArgs.addAll("-module-kind", "umd")
                    }
                }
            }

            override fun expected(kind: String) =
                Outcome("umd", "es5", "umd", "es5", null, null, "js", "d.ts", "js", false)

            override fun expectedWithDelegatedTranspilation(kind: String) =
                Outcome("umd", "es2020", "umd", "es5", "umd", "es5", "js", "d.ts", "js", false)
        },

        LinkModuleKind("link moduleKind =", testedModuleKinds = JsModuleKind.entries) {
            override fun Project.configure(moduleKind: JsModuleKind) {
                configureLinkTasks { it.compilerOptions.moduleKind.set(moduleKind) }
            }

            // Inconsistent unless UMD: package.json expects UMD
            override fun expected(kind: String) =
                Outcome(kind, "es5", kind, "es5", null, null, "js", "d.ts", jsExtension(kind), isEs(kind))

            // Inconsistent unless UMD: package.json expects UMD
            override fun expectedWithDelegatedTranspilation(kind: String) =
                Outcome(kind, "es2020", kind, "es5", kind, "es5", "js", "d.ts", jsExtension(kind), isEs(kind))
        },

        LinkTarget("link target = es2015") {
            override fun Project.configure(moduleKind: JsModuleKind) {
                configureLinkTasks { it.compilerOptions.target.set("es2015") }
            }

            // Inconsistent: package.json expects UMD
            override fun expected(kind: String) =
                Outcome("es", "es2015", "es", "es2015", null, null, "js", "d.ts", "mjs", true)

            // Inconsistent: package.json expects UMD
            override fun expectedWithDelegatedTranspilation(kind: String) =
                Outcome("es", "es2020", "es", "es2015", "es", "es2015", "js", "d.ts", "mjs", true)
        },

        LinkFreeCompilerArgsModuleKind("link freeCompilerArgs -module-kind", testedModuleKinds = JsModuleKind.entries) {
            override fun Project.configure(moduleKind: JsModuleKind) {
                configureLinkTasks { it.compilerOptions.freeCompilerArgs.addAll("-module-kind", moduleKind.kind) }
            }

            // Inconsistent unless UMD: package.json expects UMD
            override fun expected(kind: String) =
                Outcome(kind, "es5", kind, "es5", null, null, "js", "d.ts", jsExtension(kind), isEs(kind))

            // Inconsistent unless UMD: package.json expects UMD
            override fun expectedWithDelegatedTranspilation(kind: String) =
                Outcome(kind, "es2020", kind, "es5", kind, "es5", "js", "d.ts", jsExtension(kind), isEs(kind))
        },

        LinkFreeCompilerArgsTarget("link freeCompilerArgs -target es2015") {
            override fun Project.configure(moduleKind: JsModuleKind) {
                configureLinkTasks { it.compilerOptions.freeCompilerArgs.addAll("-target", "es2015") }
            }

            // Inconsistent: package.json expects UMD
            override fun expected(kind: String) =
                Outcome("es", "es2015", "es", "es2015", null, null, "js", "d.ts", "mjs", true)

            // Inconsistent: package.json expects UMD.
            // The free compiler arguments come last, so they override the latest target requested by KGP
            override fun expectedWithDelegatedTranspilation(kind: String) =
                Outcome("es", "es2015", "es", "es2015", "es", "es2015", "js", "d.ts", "mjs", true)
        },

        DslModuleKindAndLinkModuleKind("DSL moduleKind = es, link moduleKind = umd") {
            override fun Project.configure(moduleKind: JsModuleKind) {
                kotlin { js { compilerOptions.moduleKind.set(JsModuleKind.MODULE_ES) } }
                configureLinkTasks { it.compilerOptions.moduleKind.set(JsModuleKind.MODULE_UMD) }
            }

            // Inconsistent: package.json expects ES modules
            override fun expected(kind: String) =
                Outcome("umd", "es5", "umd", "es5", null, null, "mjs", "d.mts", "js", false)

            // Inconsistent: package.json expects ES modules
            override fun expectedWithDelegatedTranspilation(kind: String) =
                Outcome("umd", "es2020", "umd", "es5", "umd", "es5", "mjs", "d.mts", "js", false)
        },

        CompileTaskModuleKind("compile task moduleKind =", testedModuleKinds = JsModuleKind.entries, affectsTestCompilation = false) {
            override fun Project.configure(moduleKind: JsModuleKind) {
                kotlin {
                    js().compilations.getByName(MAIN_COMPILATION_NAME).compileTaskProvider.configure {
                        it.compilerOptions.moduleKind.set(moduleKind)
                    }
                }
            }

            // Inconsistent unless UMD: package.json expects the compile task's module kind
            override fun expected(kind: String) =
                Outcome("umd", "es5", "umd", "es5", null, null, jsExtension(kind), dtsExtension(kind), "js", false)

            // Inconsistent unless UMD: package.json expects the compile task's module kind
            override fun expectedWithDelegatedTranspilation(kind: String) =
                Outcome("umd", "es2020", "umd", "es5", "umd", "es5", jsExtension(kind), dtsExtension(kind), "js", false)
        },
        ;

        abstract fun Project.configure(moduleKind: JsModuleKind)
        abstract fun expected(kind: String): Outcome
        abstract fun expectedWithDelegatedTranspilation(kind: String): Outcome
    }

    private fun observe(
        scenario: Scenario,
        moduleKind: JsModuleKind,
        delegatedTranspilation: Boolean,
    ): Map<BinaryId, Outcome> {
        val project = buildProjectWithMPP(
            preApplyCode = {
                propertiesExtension.set("kotlin.js.delegated.transpilation", delegatedTranspilation)
            },
        ) {
            kotlin {
                js {
                    browser()
                    binaries.executable()
                    binaries.library()
                    generateTypeScriptDefinitions()
                }
            }
            with(scenario) { configure(moduleKind) }
        }.evaluate()
        // Eagerly configure all tasks, so that their configuration actions are applied
        project.tasks.all {}

        return project.multiplatformExtension.js().compilations.flatMap { compilation ->
            compilation.binaries.withType(JsIrBinary::class.java).map { binary ->
                val hasWebpack = compilation.name == MAIN_COMPILATION_NAME && binary is Executable
                BinaryId(compilation.name, binary.name, hasWebpack) to observe(project, compilation, binary)
            }
        }.toMap()
    }

    private fun observe(project: Project, compilation: KotlinJsIrCompilation, binary: JsIrBinary): Outcome {
        val compilerArguments = binary.linkTask.orNull?.let { linkTask ->
            parseCommandLineArguments<K2JSCompilerArguments>(linkTask.createCompilerArguments(lenient).toArgumentStrings())
        }
        // The arguments the TypeScript declarations generation task passes to the compiler
        val dtsArguments = binary.dtsGenerationTask?.orNull?.let { dtsTask ->
            val arguments = K2JSCompilerArguments()
            KotlinJsCompilerOptionsHelper.fillCompilerArguments(dtsTask.linkCompilerOptions, arguments)
            parseCommandLineArguments<K2JSCompilerArguments>(arguments.toArgumentStrings())
        }

        // The SWC config tasks are named after the binaries
        val swcConfigTaskBaseName = (binary as? Executable)?.executeTaskBaseName ?: binary.name
        val swcConfig = project.tasks.withType(GenerateSwcConfig::class.java)
            .findByName(lowerCamelCaseName("jsBrowser", swcConfigTaskBaseName, "generateSwcConfig"))

        // The webpack bundle and run tasks of the binary
        val webpackEsModules = project.tasks.withType(KotlinWebpack::class.java)
            .filter { binary is Executable && it.compilation == compilation && it.mode.name == binary.mode.name }
            .map { it.esModules.orNull }
            .distinct()
        assertTrue(webpackEsModules.size <= 1, "Expected the webpack tasks of ${binary.name} to agree, but got $webpackEsModules")

        return Outcome(
            compilerModuleKind = compilerArguments?.effectiveModuleKind,
            compilerTarget = compilerArguments?.effectiveTarget,
            dtsModuleKind = dtsArguments?.effectiveModuleKind,
            dtsTarget = dtsArguments?.effectiveTarget,
            swcModuleKind = swcConfig?.moduleSystemToUse?.orNull?.type,
            swcTarget = swcConfig?.esTarget?.orNull,
            packageJsonMain = compilation.npmProject.main.orNull?.fileExtension,
            packageJsonTypes = compilation.npmProject.typesFilePath.orNull?.fileExtension,
            mainFileName = binary.mainFileName.orNull?.fileExtension,
            webpackEsModules = webpackEsModules.singleOrNull(),
        )
    }

    // The compiler's own rules: the explicit module kind, otherwise ES modules for an ES2015+ target
    private val K2JSCompilerArguments.effectiveModuleKind: String
        get() = moduleKind ?: if (targetVersion.supportsEsModules) JsModuleKind.MODULE_ES.kind else JsModuleKind.MODULE_UMD.kind

    private val K2JSCompilerArguments.effectiveTarget: String
        get() = target ?: EcmaVersion.defaultVersion().name

    private val String.fileExtension: String
        get() = substringAfterLast('/').substringAfter('.')
}

private fun isEs(kind: String) = kind == JsModuleKind.MODULE_ES.kind
private fun jsExtension(kind: String) = if (isEs(kind)) "mjs" else "js"
private fun dtsExtension(kind: String) = if (isEs(kind)) "d.mts" else "d.ts"

private fun Project.configureLinkTasks(action: (KotlinJsIrLink) -> Unit) {
    tasks.withType(KotlinJsIrLink::class.java).configureEach(action)
}
