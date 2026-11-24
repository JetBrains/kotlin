/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle

import com.google.gson.Gson
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInJs
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.*
import kotlin.test.assertEquals

/**
 * Kotlin/JS IR: TypeScript declarations, ES targets and delegated transpilation.
 */
@JsGradlePluginTests
@MustRunOnChangesInGradle
class Kotlin2JsIrOutputIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @TestMetadata("kotlin2JsIrDtsGeneration")
    @MustRunOnChangesInJs
    @DisplayName("TS type declarations are generated")
    @GradleTest
    fun generateDts(gradleVersion: GradleVersion) {
        project("kotlin2JsIrDtsGeneration", gradleVersion) {
            gradleProperties.appendText("\nkotlin.js.generateRichTypeScriptDeclarations=false\n")
            build("build") {
                assertOutputDoesNotContain("GenerateTypeScriptDefinitions")
                assertFileInProjectExists("build/js/packages/kotlin2JsIrDtsGeneration/kotlin/kotlin2JsIrDtsGeneration.js")
                val dts = projectPath.resolve("build/js/packages/kotlin2JsIrDtsGeneration/kotlin/kotlin2JsIrDtsGeneration.d.ts")
                val packageJson = projectPath.resolve("build/js/packages/kotlin2JsIrDtsGeneration/")
                    .resolve(NpmProject.PACKAGE_JSON)
                    .let { Gson().fromJson(it.readText(), PackageJson::class.java) }

                assertFileExists(dts)
                assertEquals("kotlin/kotlin2JsIrDtsGeneration.d.ts", packageJson.types)
                assertFileContains(dts, "function bar(): string")
                assertFileDoesNotContain(dts, "A declaration generated from KLIB metadata.")
            }
        }
    }

    @TestMetadata("kotlin2JsIrDtsGeneration")
    @MustRunOnChangesInJs
    @DisplayName("TS type declarations are generated using the rich TypeScript schema")
    @GradleTest
    fun generateDtsWithRichTypeScriptSchema(gradleVersion: GradleVersion) {
        project("kotlin2JsIrDtsGeneration", gradleVersion) {
            gradleProperties.appendText("\nkotlin.js.generateRichTypeScriptDeclarations=true\n")
            buildScriptInjection {
                kotlinMultiplatform.js {
                    compilerOptions {
                        outputModuleName.set("MyCustomLib")
                        freeCompilerArgs.set(listOf("-Xexport-kdoc"))
                    }
                }
            }
            build("assemble") {
                val dts = projectPath.resolve("build/js/packages/MyCustomLib/kotlin/MyCustomLib.d.ts")

                assertTasksExecuted(":jsProductionExecutableGenerateTypeScriptDefinitions")

                assertFileExists(dts)
                assertFileContains(dts, "function bar(): string")
                assertFileContains(dts, "A declaration generated from KLIB metadata.")
                assertFileContains(dts, "export as namespace kotlin2JsIrDtsGeneration;")
            }
            buildScriptInjection {
                kotlinMultiplatform.js {
                    compilerOptions {
                        outputModuleName.set("MyCustomLib2")
                        freeCompilerArgs.set(listOf("-XXLanguage:+ExportKDocDocumentationToKlib"))
                    }
                }
            }
            build("assemble") {
                val dts = projectPath.resolve("build/js/packages/MyCustomLib2/kotlin/MyCustomLib2.d.ts")

                assertTasksExecuted(":jsProductionExecutableGenerateTypeScriptDefinitions")

                assertFileExists(dts)
                assertFileNotExists(projectPath.resolve("build/js/packages/MyCustomLib2/kotlin/MyCustomLib.d.ts"))
                assertFileContains(dts, "function bar(): string")
                assertFileContains(dts, "A declaration generated from KLIB metadata.")
                assertFileContains(dts, "export as namespace kotlin2JsIrDtsGeneration;")
            }
            buildScriptInjection {
                kotlinMultiplatform.js {
                    compilerOptions.freeCompilerArgs.set(listOf("-Xexport-kdoc"))
                }
                project.tasks.withType(KotlinJsIrLink::class.java).configureEach {
                    it.compilerOptions.moduleName.set("CustomModuleName")
                }
            }
            build("assemble") {
                val dts = projectPath.resolve("build/js/packages/MyCustomLib2/kotlin/CustomModuleName.d.ts")

                assertTasksExecuted(":jsProductionExecutableGenerateTypeScriptDefinitions")

                assertFileExists(dts)
                assertFileNotExists(projectPath.resolve("build/js/packages/MyCustomLib2/kotlin/MyCustomLib2.d.ts"))
                assertFileContains(dts, "function bar(): string")
                assertFileContains(dts, "A declaration generated from KLIB metadata.")
                assertFileContains(dts, "export as namespace kotlin2JsIrDtsGeneration;")
            }
            buildScriptInjection {
                project.tasks.withType(KotlinJsIrLink::class.java).configureEach {
                    it.compilerOptions.freeCompilerArgs.add("-Xir-module-name=MyLib")
                }
            }
            build("assemble") {
                val dts = projectPath.resolve("build/js/packages/MyCustomLib2/kotlin/CustomModuleName.d.ts")

                assertTasksExecuted(":jsProductionExecutableGenerateTypeScriptDefinitions")

                assertFileExists(dts)
                assertFileContains(dts, "function bar(): string")
                assertFileContains(dts, "A declaration generated from KLIB metadata.")
                assertFileContains(dts, "export as namespace MyLib;")
            }
        }
    }

    @TestMetadata("kotlin2JsIrDtsGeneration")
    @MustRunOnChangesInJs
    @DisplayName("Rich TS export resolves language features from -XXLanguage")
    @GradleTest
    fun richDtsResolvesLanguageFeaturesFromXXLanguage(gradleVersion: GradleVersion) {
        project("kotlin2JsIrDtsGeneration", gradleVersion) {
            gradleProperties.appendText("\nkotlin.js.generateRichTypeScriptDeclarations=true\n")
            kotlinSourcesDir("jsMain").resolve("Test.kt").writeText(
                """
                |package foo
                |
                |@JsExport
                |interface Greeter {
                |    fun greet(): String
                |}
                """.trimMargin()
            )

            // These features have no sinceVersion, so -language-version cannot enable them by default.
            val dts = projectPath.resolve(
                "build/compileSync/js/main/productionExecutable/typescript-definitions/kotlin2JsIrDtsGeneration.d.ts"
            )
            build("jsProductionExecutableGenerateTypeScriptDefinitions") {
                assertTasksExecuted(":jsProductionExecutableGenerateTypeScriptDefinitions")

                assertFileContains(dts, "__doNotUseOrImplementIt")
                assertFileDoesNotContain(dts, "readonly [foo.Greeter.Symbol]: true;")
                assertFileDoesNotContain(dts, "const Symbol: unique symbol;")
            }

            buildScriptInjection {
                kotlinMultiplatform.js {
                    compilerOptions {
                        freeCompilerArgs.add("-XXLanguage:+JsExportInterfacesInImplementableWay")
                    }
                }
            }
            build("jsProductionExecutableGenerateTypeScriptDefinitions") {
                assertTasksExecuted(":jsProductionExecutableGenerateTypeScriptDefinitions")

                assertFileDoesNotContain(dts, "__doNotUseOrImplementIt")
                assertFileContains(dts, "readonly [foo.Greeter.Symbol]: true;")
                assertFileContains(dts, "const Symbol: unique symbol;")
            }
        }
    }

    @TestMetadata("kotlin2JsIrDtsGeneration")
    @MustRunOnChangesInJs
    @DisplayName("Rich TS export emit diagnostics")
    @GradleTest
    fun richDtsEmitDiagnostics(gradleVersion: GradleVersion) {
        project("kotlin2JsIrDtsGeneration", gradleVersion) {
            gradleProperties.appendText("\nkotlin.js.generateRichTypeScriptDeclarations=true\n")
            buildScriptInjection {
                kotlinMultiplatform.js {
                    compilerOptions {
                        freeCompilerArgs.add("-Xsuspend-lambda-exporting")
                    }
                }
            }
            kotlinSourcesDir("jsMain").resolve("Test.kt").writeText(
                """
                |package foo
                |
                |@JsExport
                |fun produceSuspendLambda(): suspend (Int) -> Int = { value -> value * 2 }
                """.trimMargin()
            )
            buildAndFail("jsProductionExecutableGenerateTypeScriptDefinitions") {
                assertOutputContains("Suspend lambdas cannot be exported without using generators")
            }

            kotlinSourcesDir("jsMain").resolve("Test.kt").writeText(
                """
                |package foo
                |
                |@JsExport
                |fun produceLong(): Long = 42L
                """.trimMargin()
            )
            buildAndFail("jsProductionExecutableGenerateTypeScriptDefinitions") {
                assertOutputContains("Long cannot be exported without using the bigint type.")
            }
        }
    }

    @TestMetadata("kotlin2JsIrDtsGeneration")
    @MustRunOnChangesInJs
    @DisplayName("TS type declarations are generated with the right extension for ES modules")
    @GradleTest
    fun generateDtsWithEsModules(gradleVersion: GradleVersion) {
        project("kotlin2JsIrDtsGeneration", gradleVersion) {
            buildScriptInjection {
                kotlinMultiplatform.js {
                    useEsModules()
                }
            }
            build("build") {
                assertFileInProjectExists("build/js/packages/kotlin2JsIrDtsGeneration/kotlin/kotlin2JsIrDtsGeneration.mjs")
                val dts = projectPath.resolve("build/js/packages/kotlin2JsIrDtsGeneration/kotlin/kotlin2JsIrDtsGeneration.d.mts")
                val packageJson = projectPath.resolve("build/js/packages/kotlin2JsIrDtsGeneration/")
                    .resolve(NpmProject.PACKAGE_JSON)
                    .let { Gson().fromJson(it.readText(), PackageJson::class.java) }

                assertFileExists(dts)
                assertEquals("kotlin/kotlin2JsIrDtsGeneration.d.mts", packageJson.types)
                assertFileContains(dts, "function bar(): string")
            }
        }
    }

    @TestMetadata("simple-js-executable")
    @MustRunOnChangesInJs
    @DisplayName("es2020 target produces ES modules and compiles Long as bigint by default")
    @GradleTest
    fun testEs2020TargetProducesEsModulesWithBigIntLongs(gradleVersion: GradleVersion) {
        project("simple-js-executable", gradleVersion) {
            projectPath.resolve("src/jsMain/kotlin/Main.kt").writeText(
                """
                |package com.example
                |
                |@JsExport
                |val value: Long = 1234567890123L
                |
                |fun main() {
                |    println(value.toString())
                |}
                """.trimMargin()
            )
            buildScriptInjection {
                kotlinMultiplatform.js {
                    compilerOptions {
                        target.set("es2020")
                        generateTypeScriptDefinitions()
                    }
                }
            }
            build("build") {
                // es2020 implies ES modules, just like es2015, so the output uses the `.mjs` extension.
                val outputDir = projectPath.resolve("build/js/packages/js-executable/kotlin/")
                val outputFile = outputDir.resolve("js-executable.mjs")

                assertFileExists(outputFile)
                // es2020 enables `-Xes-long-as-bigint` by default, so Long literals are emitted as bigint (`<value>n`).
                assertFileContains(outputFile, "1234567890123n")

                val packageJson = projectPath.resolve("build/js/packages/js-executable/")
                    .resolve(NpmProject.PACKAGE_JSON)
                    .let { Gson().fromJson(it.readText(), PackageJson::class.java) }

                assertEquals("kotlin/js-executable.mjs", packageJson.main)

                val outputDtsFile = outputDir.resolve("js-executable.d.mts")
                assertFileContains(
                    outputDtsFile,
                    """
                    |export declare const value: {
                    |    get(): bigint;
                    |};
                    """.trimMargin()
                )
            }
        }
    }

    @TestMetadata("kotlin-js-multiplatform-app-project")
    @DisplayName("delegated transpilation works for app project if it's turned on")
    @GradleTest
    fun testAppProjectWorkWithTheDelegatedTranspilation(gradleVersion: GradleVersion) {
        project("kotlin-js-multiplatform-app-project", gradleVersion) {
            gradleProperties.appendText(
                """
                |
                |kotlin.js.delegated.transpilation=true
                |kotlin.js.generateRichTypeScriptDeclarations=true
                """.trimMargin()
            )

            build("jsNodeTest") {
                assertTasksExecuted(":kotlinSwcSetup", ":jsNodeTestDevelopmentGenerateSwcConfig", ":jsNodeTestDevelopmentTranspileWithSwc")
                assertFileContains(
                    projectPath.resolve("build/js/packages/kotlin-js-multiplatform-app-project-test/.swcrc"),
                    "\"target\": \"es5\"",
                    "\"isModule\": false",
                    "\"loose\": false",
                    "\"sourceMaps\": true",
                )
                assertFileDoesNotContain(
                    projectPath.resolve("build/js/packages/kotlin-js-multiplatform-app-project-test/.swcrc"),
                    "\"module\"",
                )
            }

            build("jsNodeDevelopmentRun") {
                assertOutputContains("Hello, World!")
                assertTasksUpToDate(":kotlinSwcSetup")
                assertTasksExecuted(":jsNodeDevelopmentGenerateSwcConfig", ":jsNodeDevelopmentTranspileWithSwc")

                assertFileContains(
                    projectPath.resolve("build/js/packages/kotlin-js-multiplatform-app-project/.swcrc"),
                    "\"target\": \"es5\"",
                    "\"isModule\": false",
                    "\"loose\": false",
                    "\"sourceMaps\": true",
                )
                assertFileDoesNotContain(
                    projectPath.resolve("build/js/packages/kotlin-js-multiplatform-app-project/.swcrc"),
                    "\"module\"",
                )
                assertFileContains(
                    projectPath.resolve("build/js/packages/kotlin-js-multiplatform-app-project/kotlin/kotlin-js-multiplatform-app-project.d.ts"),
                    "function best(): number",
                )

                assertOutputContains("Long: 9007199254740993")
                // The es5 output must not contain BigInt literals, even if the compiler itself produces the latest JS version
                assertFileDoesNotContain(
                    projectPath.resolve("build/js/packages/kotlin-js-multiplatform-app-project/kotlin/kotlin-js-multiplatform-app-project.js"),
                    "9007199254740993n",
                )
            }

            build("jsNodeProductionRun") {
                assertOutputContains("Hello, World!")
                assertTasksUpToDate(":kotlinSwcSetup")
                assertTasksExecuted(":jsNodeProductionGenerateSwcConfig", ":jsNodeProductionTranspileWithSwc")

                assertFileContains(
                    projectPath.resolve("build/js/packages/kotlin-js-multiplatform-app-project/.swcrc"),
                    "\"target\": \"es5\"",
                    "\"isModule\": false",
                    "\"loose\": false",
                    "\"sourceMaps\": true",
                )
                assertFileDoesNotContain(
                    projectPath.resolve("build/js/packages/kotlin-js-multiplatform-app-project/.swcrc"),
                    "\"module\"",
                )

                assertOutputContains("Long: 9007199254740993")
                assertFileDoesNotContain(
                    projectPath.resolve("build/js/packages/kotlin-js-multiplatform-app-project/kotlin/kotlin-js-multiplatform-app-project.js"),
                    "9007199254740993n",
                )
            }

            gradleProperties.modify {
                it.replace(
                    "kotlin.js.generateRichTypeScriptDeclarations=true",
                    "kotlin.js.generateRichTypeScriptDeclarations=false",
                )
            }

            buildScriptInjection {
                kotlinMultiplatform.js {
                    compilerOptions.target.set("es2015")
                }
            }

            build("jsNodeDevelopmentRun") {
                assertOutputContains("Hello, World!")
                assertFileContains(
                    projectPath.resolve(
                        "build/js/packages/kotlin-js-multiplatform-app-project/kotlin/kotlin-js-multiplatform-app-project.d.mts"
                    ),
                    "function best(): number",
                )
            }

            build("jsNodeProductionRun") {
                assertOutputContains("Hello, World!")
                assertFileContains(
                    projectPath.resolve(
                        "build/js/packages/kotlin-js-multiplatform-app-project/kotlin/kotlin-js-multiplatform-app-project.d.mts"
                    ),
                    "function best(): number",
                )
            }
        }
    }

    @TestMetadata("kotlin-js-multiplatform-app-project")
    @DisplayName("delegated transpilation works without test sources")
    @GradleTest
    fun testDelegatedTranspilationWithoutTestSources(gradleVersion: GradleVersion) {
        project("kotlin-js-multiplatform-app-project", gradleVersion) {
            gradleProperties.appendText(
                """
                |
                |kotlin.js.delegated.transpilation=true
                """.trimMargin()
            )
            projectPath.resolve("src/jsTest").deleteRecursively()

            build("jsNodeTest") {
                assertTasksNoSource(
                    ":compileTestDevelopmentExecutableKotlinJs",
                    ":jsNodeTestDevelopmentTranspileWithSwc",
                )
                assertTasksSkipped(":jsNodeTest")
            }
        }
    }

    @TestMetadata("kotlin-js-multiplatform-library-project")
    @DisplayName("legacy TS declarations are synced once with delegated transpilation and es2020 target")
    @GradleTest
    fun testLibraryLegacyDtsSyncedOnceWithDelegatedTranspilationAndEs2020Target(gradleVersion: GradleVersion) {
        testLibraryDtsSyncedOnceWithDelegatedTranspilationAndEs2020Target(gradleVersion, richTypeScriptDeclarations = false)
    }

    @TestMetadata("kotlin-js-multiplatform-library-project")
    @DisplayName("rich TS declarations are synced once with delegated transpilation and es2020 target")
    @GradleTest
    fun testLibraryRichDtsSyncedOnceWithDelegatedTranspilationAndEs2020Target(gradleVersion: GradleVersion) {
        testLibraryDtsSyncedOnceWithDelegatedTranspilationAndEs2020Target(gradleVersion, richTypeScriptDeclarations = true)
    }

    private fun testLibraryDtsSyncedOnceWithDelegatedTranspilationAndEs2020Target(
        gradleVersion: GradleVersion,
        richTypeScriptDeclarations: Boolean,
    ) {
        project("kotlin-js-multiplatform-library-project", gradleVersion) {
            gradleProperties.appendText(
                """
                |
                |kotlin.js.delegated.transpilation=true
                |kotlin.js.generateRichTypeScriptDeclarations=$richTypeScriptDeclarations
                """.trimMargin()
            )
            buildScriptInjection {
                kotlinMultiplatform.js {
                    compilerOptions.target.set("es2020")
                    generateTypeScriptDefinitions()
                }
            }

            build("jsBrowserDevelopmentLibraryDistribution") {
                assertOutputDoesNotContain("has already been copied there")

                val dts = projectPath.resolve("build/dist/js/developmentLibrary/kotlin-js-multiplatform-library-project.d.mts")
                assertFileExists(dts)
                assertFileContains(dts, "function best(): number")
            }
        }
    }

    @TestMetadata("kotlin-js-multiplatform-library-project")
    @DisplayName("delegated transpilation works for library project if it's turned on")
    @GradleTest
    fun testLibraryProjectWorkWithTheDelegatedTranspilation(gradleVersion: GradleVersion) {
        project("kotlin-js-multiplatform-library-project", gradleVersion) {
            gradleProperties.appendText(
                """
                |
                |kotlin.js.delegated.transpilation=true
                """.trimMargin()
            )

            build("jsBrowserDevelopmentDistribution") {
                assertTasksExecuted(
                    ":kotlinSwcSetup",
                    ":jsBrowserDevelopmentLibraryGenerateSwcConfig",
                    ":jsBrowserDevelopmentLibraryTranspileWithSwc"
                )

                assertFileContains(
                    projectPath.resolve("build/js/packages/kotlin-js-multiplatform-library-project/.swcrc"),
                    "\"target\": \"es5\"",
                    "\"isModule\": false",
                    "\"loose\": false",
                )
                assertFileDoesNotContain(
                    projectPath.resolve("build/js/packages/kotlin-js-multiplatform-library-project/.swcrc"),
                    "\"module\"",
                )
            }

            build("jsBrowserProductionDistribution") {
                assertTasksUpToDate(":kotlinSwcSetup")
                assertTasksExecuted(":jsBrowserProductionLibraryGenerateSwcConfig", ":jsBrowserProductionLibraryTranspileWithSwc")

                assertFileContains(
                    projectPath.resolve("build/js/packages/kotlin-js-multiplatform-library-project/.swcrc"),
                    "\"target\": \"es5\"",
                    "\"isModule\": false",
                    "\"loose\": false",
                )
                assertFileDoesNotContain(
                    projectPath.resolve("build/js/packages/kotlin-js-multiplatform-library-project/.swcrc"),
                    "\"module\"",
                )
            }
        }
    }

    @TestMetadata("kotlin-js-multiplatform-library-project")
    @DisplayName("delegated transpilation configured differently for library project if compiler options are changed")
    @GradleTest
    fun testLibraryProjectWorkWithTheDelegatedTranspilationAndDifferentCompilerConfiguration(gradleVersion: GradleVersion) {
        project("kotlin-js-multiplatform-library-project", gradleVersion) {
            gradleProperties.appendText(
                """
                |
                |kotlin.js.delegated.transpilation=true
                """.trimMargin()
            )

            buildScriptInjection {
                kotlinMultiplatform.js {
                    useEsModules()
                }
            }

            build("jsBrowserDevelopmentDistribution") {
                assertTasksExecuted(
                    ":kotlinSwcSetup",
                    ":jsBrowserDevelopmentLibraryGenerateSwcConfig",
                    ":jsBrowserDevelopmentLibraryTranspileWithSwc"
                )

                assertFileContains(
                    projectPath.resolve("build/js/packages/kotlin-js-multiplatform-library-project/.swcrc"),
                    "\"target\": \"es5\"",
                    "\"type\": \"nodenext\"",
                    "\"outFileExtension\": \"mjs\"",
                    "\"loose\": false",
                )
            }

            build("jsBrowserProductionDistribution") {
                assertTasksUpToDate(":kotlinSwcSetup")
                assertTasksExecuted(":jsBrowserProductionLibraryGenerateSwcConfig", ":jsBrowserProductionLibraryTranspileWithSwc")

                assertFileContains(
                    projectPath.resolve("build/js/packages/kotlin-js-multiplatform-library-project/.swcrc"),
                    "\"target\": \"es5\"",
                    "\"type\": \"nodenext\"",
                    "\"outFileExtension\": \"mjs\"",
                    "\"loose\": false",
                )
            }
        }
    }

    @TestMetadata("kotlin-js-multiplatform-app-project")
    @DisplayName("delegated transpilation configured differently for app project if compiler options are changed")
    @GradleTest
    fun testAppProjectWorkWithTheDelegatedTranspilationAndDifferentCompilerConfiguration(gradleVersion: GradleVersion) {
        project("kotlin-js-multiplatform-app-project", gradleVersion) {
            gradleProperties.appendText(
                """
                |
                |kotlin.js.delegated.transpilation=true
                """.trimMargin()
            )

            buildScriptInjection {
                kotlinMultiplatform.js {
                    useEsModules()
                }
            }

            build("jsNodeTest") {
                assertTasksExecuted(":kotlinSwcSetup", ":jsNodeTestDevelopmentGenerateSwcConfig", ":jsNodeTestDevelopmentTranspileWithSwc")
                assertFileContains(
                    projectPath.resolve("build/js/packages/kotlin-js-multiplatform-app-project-test/.swcrc"),
                    "\"target\": \"es5\"",
                    "\"type\": \"nodenext\"",
                    "\"outFileExtension\": \"mjs\"",
                    "\"loose\": false",
                )
            }

            build("jsNodeDevelopmentRun") {
                assertOutputContains("Hello, World!")
                assertTasksUpToDate(":kotlinSwcSetup")
                assertTasksExecuted(":jsNodeDevelopmentGenerateSwcConfig", ":jsNodeDevelopmentTranspileWithSwc")

                assertFileContains(
                    projectPath.resolve("build/js/packages/kotlin-js-multiplatform-app-project/.swcrc"),
                    "\"target\": \"es5\"",
                    "\"type\": \"nodenext\"",
                    "\"outFileExtension\": \"mjs\"",
                    "\"loose\": false",
                )
            }

            build("jsNodeProductionRun") {
                assertOutputContains("Hello, World!")
                assertTasksUpToDate(":kotlinSwcSetup")
                assertTasksExecuted(":jsNodeProductionGenerateSwcConfig", ":jsNodeProductionTranspileWithSwc")

                assertFileContains(
                    projectPath.resolve("build/js/packages/kotlin-js-multiplatform-app-project/.swcrc"),
                    "\"target\": \"es5\"",
                    "\"type\": \"nodenext\"",
                    "\"outFileExtension\": \"mjs\"",
                    "\"loose\": false",
                )
            }
        }
    }

    @TestMetadata("kotlin-js-multiplatform-library-project")
    @DisplayName("delegated transpilation is turned off by default for library")
    @GradleTest
    fun testLibraryProjectWorkWithoutTheDelegatedTranspilation(gradleVersion: GradleVersion) {
        project("kotlin-js-multiplatform-library-project", gradleVersion) {
            build("jsBrowserDevelopmentDistribution") {
                assertTasksAreNotInTaskGraph(
                    ":kotlinSwcSetup",
                    ":jsBrowserDevelopmentLibraryGenerateSwcConfig",
                    ":jsBrowserDevelopmentLibraryTranspileWithSwc"
                )
            }

            build("jsBrowserProductionDistribution") {
                assertTasksAreNotInTaskGraph(
                    ":kotlinSwcSetup",
                    ":jsBrowserProductionLibraryGenerateSwcConfig",
                    ":jsBrowserProductionLibraryTranspileWithSwc"
                )
            }
        }
    }

    @TestMetadata("kotlin-js-multiplatform-app-project")
    @DisplayName("delegated transpilation is turned off by default for app")
    @GradleTest
    fun testAppProjectWorkWithoutTheDelegatedTranspilation(gradleVersion: GradleVersion) {
        project("kotlin-js-multiplatform-app-project", gradleVersion) {
            build("jsNodeTest") {
                assertTasksAreNotInTaskGraph(
                    ":kotlinSwcSetup",
                    ":jsNodeTestDevelopmentGenerateSwcConfig",
                    ":jsNodeTestDevelopmentTranspileWithSwc"
                )
            }

            build("jsNodeDevelopmentRun") {
                assertOutputContains("Hello, World!")
                assertTasksAreNotInTaskGraph(
                    ":kotlinSwcSetup",
                    ":jsNodeTestDevelopmentGenerateSwcConfig",
                    ":jsNodeTestDevelopmentTranspileWithSwc"
                )
            }

            build("jsNodeProductionRun") {
                assertOutputContains("Hello, World!")
                assertTasksAreNotInTaskGraph(
                    ":kotlinSwcSetup",
                    ":jsNodeTestDevelopmentGenerateSwcConfig",
                    ":jsNodeTestDevelopmentTranspileWithSwc"
                )
            }
        }
    }

    @TestMetadata("js-ir-validate-ts")
    @DisplayName("test TypeScript declarations are not generated if DSL wasn't applied")
    @GradleTest
    fun testNotGeneratedTypeScriptDeclarations(gradleVersion: GradleVersion) {
        project("js-ir-validate-ts", gradleVersion) {
            build("jsBrowserDevelopmentLibraryDistribution") {
                assertFileInProjectExists("build/dist/js/developmentLibrary/js-ir-validate-ts.js")
                assertFileInProjectNotExists("build/dist/js/developmentLibrary/js-ir-validate-ts.d.ts")

                val distributionPackageJson = projectPath.resolve("build/dist/js/developmentLibrary")
                    .resolve(NpmProject.PACKAGE_JSON)
                    .let { Gson().fromJson(it.readText(), PackageJson::class.java) }

                assertEquals("js-ir-validate-ts.js", distributionPackageJson.main)
                assertEquals(null, distributionPackageJson.types)

                assertFileInProjectExists("build/js/packages/js-ir-validate-ts/kotlin/js-ir-validate-ts.js")
                assertFileInProjectNotExists("build/js/packages/js-ir-validate-ts/kotlin/js-ir-validate-ts.d.ts")

                val internalPackageJson = projectPath.resolve("build/js/packages/js-ir-validate-ts")
                    .resolve(NpmProject.PACKAGE_JSON)
                    .let { Gson().fromJson(it.readText(), PackageJson::class.java) }

                assertEquals("kotlin/js-ir-validate-ts.js", internalPackageJson.main)
                assertEquals(null, internalPackageJson.types)
            }

            build("jsBrowserProductionLibraryDistribution") {
                assertFileInProjectExists("build/dist/js/productionLibrary/js-ir-validate-ts.js")
                assertFileInProjectNotExists("build/dist/js/productionLibrary/js-ir-validate-ts.d.ts")

                val packageJson = projectPath.resolve("build/dist/js/productionLibrary")
                    .resolve(NpmProject.PACKAGE_JSON)
                    .let { Gson().fromJson(it.readText(), PackageJson::class.java) }

                assertEquals("js-ir-validate-ts.js", packageJson.main)
                assertEquals(null, packageJson.types)

                assertFileInProjectExists("build/js/packages/js-ir-validate-ts/kotlin/js-ir-validate-ts.js")
                assertFileInProjectNotExists("build/js/packages/js-ir-validate-ts/kotlin/js-ir-validate-ts.d.ts")

                val internalPackageJson = projectPath.resolve("build/js/packages/js-ir-validate-ts")
                    .resolve(NpmProject.PACKAGE_JSON)
                    .let { Gson().fromJson(it.readText(), PackageJson::class.java) }

                assertEquals("kotlin/js-ir-validate-ts.js", internalPackageJson.main)
                assertEquals(null, internalPackageJson.types)
            }
        }
    }

    @TestMetadata("js-ir-validate-ts")
    @MustRunOnChangesInJs
    @DisplayName("test TypeScript declarations generated after DSL applied")
    @GradleTest
    fun testGeneratedTypeScriptDeclarations(gradleVersion: GradleVersion) {
        project("js-ir-validate-ts", gradleVersion) {
            buildScriptInjection {
                kotlinMultiplatform.js {
                    generateTypeScriptDefinitions()
                }
            }

            build("jsBrowserDevelopmentLibraryDistribution") {
                assertFileInProjectExists("build/dist/js/developmentLibrary/js-ir-validate-ts.js")
                assertFileInProjectExists("build/dist/js/developmentLibrary/js-ir-validate-ts.d.ts")

                val distributionPackageJson = projectPath.resolve("build/dist/js/developmentLibrary")
                    .resolve(NpmProject.PACKAGE_JSON)
                    .let { Gson().fromJson(it.readText(), PackageJson::class.java) }

                assertEquals("js-ir-validate-ts.js", distributionPackageJson.main)
                assertEquals("js-ir-validate-ts.d.ts", distributionPackageJson.types)

                assertFileInProjectExists("build/js/packages/js-ir-validate-ts/kotlin/js-ir-validate-ts.js")
                assertFileInProjectExists("build/js/packages/js-ir-validate-ts/kotlin/js-ir-validate-ts.d.ts")

                val internalPackageJson = projectPath.resolve("build/js/packages/js-ir-validate-ts/")
                    .resolve(NpmProject.PACKAGE_JSON)
                    .let { Gson().fromJson(it.readText(), PackageJson::class.java) }

                assertEquals("kotlin/js-ir-validate-ts.js", internalPackageJson.main)
                assertEquals("kotlin/js-ir-validate-ts.d.ts", internalPackageJson.types)
            }

            build("jsBrowserProductionLibraryDistribution") {
                assertFileInProjectExists("build/dist/js/productionLibrary/js-ir-validate-ts.js")
                assertFileInProjectExists("build/dist/js/productionLibrary/js-ir-validate-ts.d.ts")

                val packageJson = projectPath.resolve("build/dist/js/productionLibrary")
                    .resolve(NpmProject.PACKAGE_JSON)
                    .let { Gson().fromJson(it.readText(), PackageJson::class.java) }

                assertEquals("js-ir-validate-ts.js", packageJson.main)
                assertEquals("js-ir-validate-ts.d.ts", packageJson.types)

                assertFileInProjectExists("build/js/packages/js-ir-validate-ts/kotlin/js-ir-validate-ts.js")
                assertFileInProjectExists("build/js/packages/js-ir-validate-ts/kotlin/js-ir-validate-ts.d.ts")

                val internalPackageJson = projectPath.resolve("build/js/packages/js-ir-validate-ts/")
                    .resolve(NpmProject.PACKAGE_JSON)
                    .let { Gson().fromJson(it.readText(), PackageJson::class.java) }

                assertEquals("kotlin/js-ir-validate-ts.js", internalPackageJson.main)
                assertEquals("kotlin/js-ir-validate-ts.d.ts", internalPackageJson.types)
            }
        }
    }

    @TestMetadata("js-ir-validate-ts")
    @MustRunOnChangesInJs
    @DisplayName("generated typescript declarations validation")
    @GradleTest
    fun testGeneratedTypeScriptDeclarationsValidation(gradleVersion: GradleVersion) {
        project("js-ir-validate-ts", gradleVersion) {
            buildScriptInjection {
                kotlinMultiplatform.js {
                    generateTypeScriptDefinitions()

                    compilations.all {
                        it.binaries.withType(JsIrBinary::class.java).all { binary ->
                            @Suppress("INVISIBLE_REFERENCE")
                            binary.dtsGenerationTask?.configure { task ->
                                task.doLast {
                                    val dts = task.outputDirectory.asFile.get().resolve("js-ir-validate-ts.d.ts")
                                    dts.appendText("\nlet invalidCode: unique symbol = Symbol()")
                                }
                            }
                        }
                    }
                }
            }

            buildAndFail("jsBrowserDevelopmentLibraryDistribution") {
                assertTasksFailed(":jsDevelopmentLibraryValidateGeneratedByCompilerTypeScript")
                assertFileInProjectExists("build/js/packages/js-ir-validate-ts/kotlin/js-ir-validate-ts.js")
                assertFileInProjectExists("build/js/packages/js-ir-validate-ts/kotlin/js-ir-validate-ts.d.ts")
            }

            build("jsBrowserProductionLibraryDistribution") {
                assertTasksExecuted(":jsProductionLibraryValidateGeneratedByCompilerTypeScript")
                assertFileInProjectExists("build/dist/js/productionLibrary/js-ir-validate-ts.js")
                assertFileInProjectExists("build/dist/js/productionLibrary/js-ir-validate-ts.d.ts")
            }

            build("assemble") {
                assertTasksExecuted(":jsProductionLibraryValidateGeneratedByCompilerTypeScript")
                assertFileInProjectExists("build/dist/js/productionLibrary/js-ir-validate-ts.js")
                assertFileInProjectExists("build/dist/js/productionLibrary/js-ir-validate-ts.d.ts")
            }
        }
    }

    @MustRunOnChangesInJs
    @DisplayName("klib compilation with the declarations name clash")
    @GradleTest
    @TestMetadata("kotlin-js-invalid-project-with-exported-clash")
    fun testProjectWithExportedNamesClash(gradleVersion: GradleVersion) {
        project("kotlin-js-invalid-project-with-exported-clash", gradleVersion) {
            build("compileKotlinJs") {
                assertOutputContains("Exporting name 'best' in ES modules may clash")
            }
        }
    }

    @DisplayName("Rich TS type declarations are generated without running the JS link task")
    @GradleTest
    fun generateRichDtsWithoutLinkTask(gradleVersion: GradleVersion) {
        project("kotlin2JsIrDtsGeneration", gradleVersion) {
            gradleProperties.appendText("\nkotlin.js.generateRichTypeScriptDeclarations=true\n")
            build(":jsProductionExecutableGenerateTypeScriptDefinitions") {
                assertTasksExecuted(":compileKotlinJs", ":jsProductionExecutableGenerateTypeScriptDefinitions")
                assertTasksAreNotInTaskGraph(":compileProductionExecutableKotlinJs")
            }
        }
    }
}
