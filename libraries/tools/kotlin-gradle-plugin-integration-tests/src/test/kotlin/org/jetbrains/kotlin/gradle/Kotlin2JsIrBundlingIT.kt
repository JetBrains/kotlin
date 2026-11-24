/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.js.dsl.Distribution
import org.jetbrains.kotlin.gradle.targets.js.ir.Library
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInJs
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertAll
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * Kotlin/JS IR: webpack, browser distribution and source maps.
 */
@JsGradlePluginTests
@MustRunOnChangesInGradle
class Kotlin2JsIrBundlingIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @TestMetadata("kotlin2JsProjectWithSourceMap")
    @MustRunOnChangesInJs
    @DisplayName("fully qualified names can be used in the sourcemap")
    @GradleTest
    fun testKotlinJsSourceMapGenerateFqNames(gradleVersion: GradleVersion) {
        project("kotlin2JsProjectWithSourceMap", gradleVersion) {
            buildGradleKts.appendText(
                """
                |allprojects {
                |   tasks.withType<KotlinJsCompile> {
                |        compilerOptions.sourceMapNamesPolicy.set(
                |            org.jetbrains.kotlin.gradle.dsl.JsSourceMapNamesPolicy.SOURCE_MAP_NAMES_POLICY_FQ_NAMES
                |        )
                |   }
                |}
                |
                """.trimMargin()
            )
            build("compileDevelopmentExecutableKotlinJs") {
                assertFileContains(
                    subProject("app").projectPath
                        .resolve("build/compileSync/js/main/developmentExecutable/kotlin/$projectName-app.js.map"),
                    "app.C.somewhereOverTheRainbow"
                )
            }
        }
    }

    @TestMetadata("kotlin2JsProjectWithSourceMap")
    @MustRunOnChangesInJs
    @DisplayName("simple names can be used in the sourcemap")
    @GradleTest
    fun testKotlinJsSourceMapGenerateSimpleNames(gradleVersion: GradleVersion) {
        project("kotlin2JsProjectWithSourceMap", gradleVersion) {
            buildGradleKts.appendText(
                """
                |allprojects {
                |   tasks.withType<KotlinJsCompile> {
                |        compilerOptions.sourceMapNamesPolicy.set(
                |            org.jetbrains.kotlin.gradle.dsl.JsSourceMapNamesPolicy.SOURCE_MAP_NAMES_POLICY_SIMPLE_NAMES
                |        )
                |   }
                |}
                |
                """.trimMargin()
            )
            build("compileDevelopmentExecutableKotlinJs") {
                val sourceMap = subProject("app").projectPath
                    .resolve("build/compileSync/js/main/developmentExecutable/kotlin/$projectName-app.js.map")
                assertFileContains(sourceMap, "somewhereOverTheRainbow")
                assertFileDoesNotContain(sourceMap, "app.C.somewhereOverTheRainbow")
            }
        }
    }

    @TestMetadata("kotlin2JsProjectWithSourceMap")
    @MustRunOnChangesInJs
    @DisplayName("don't generate names in the sourcemap")
    @GradleTest
    fun testKotlinJsSourceMapGenerateNoNames(gradleVersion: GradleVersion) {
        project("kotlin2JsProjectWithSourceMap", gradleVersion) {
            buildGradleKts.appendText(
                """
                |allprojects {
                |   tasks.withType<KotlinJsCompile> {
                |        compilerOptions.sourceMapNamesPolicy.set(
                |            org.jetbrains.kotlin.gradle.dsl.JsSourceMapNamesPolicy.SOURCE_MAP_NAMES_POLICY_NO
                |        )
                |   }
                |}
                |
                """.trimMargin()
            )
            build("compileDevelopmentExecutableKotlinJs") {
                val sourceMap = subProject("app").projectPath
                    .resolve("build/compileSync/js/main/developmentExecutable/kotlin/$projectName-app.js.map")
                assertFileContains(sourceMap, "\"names\":[]")
                assertFileDoesNotContain(sourceMap, "app.C.somewhereOverTheRainbow")
            }
        }
    }

    @DisplayName("webpack must consider changes in dependencies in up-to-date")
    @GradleTest
    @TestMetadata("kotlin-js-browser-project")
    fun testWebpackConsiderChangesInDependencies(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {

            projectPath.resolve("app/src/jsMain/kotlin/App.kt").modify {
                it.replace("require(\"css/main.css\")", "")
            }

            build("runWebpackResult") {
                assertOutputContains("Sheldon: 73")
            }

            projectPath.resolve("base/src/jsMain/kotlin/Base.kt").modify {
                it.replace("73", "37")
            }

            build("runWebpackResult") {
                assertOutputContains("Sheldon: 37")
            }
        }
    }

    @DisplayName("Webpack works with ES modules")
    @GradleTest
    @TestMetadata("kotlin-js-browser-project")
    fun testWebpackWorksWithEsModules(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            subProject("app").buildGradleKts.modify { originalScript ->
                buildString {
                    append(originalScript)
                    append(
                        """
                        |
                        |tasks.named<org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink>("compileDevelopmentExecutableKotlinJs") {
                        |   compilerOptions {
                        |       moduleKind.set(org.jetbrains.kotlin.gradle.dsl.JsModuleKind.MODULE_ES)
                        |   }
                        |}
                        |
                        |tasks.named<org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink>("compileProductionExecutableKotlinJs") {
                        |   compilerOptions {
                        |       moduleKind.set(org.jetbrains.kotlin.gradle.dsl.JsModuleKind.MODULE_ES)
                        |   }
                        |}
                        |
                        """.trimMargin()
                    )
                }
            }

            build("jsBrowserDistribution") {
                assertTasksExecuted(":app:jsBrowserProductionWebpack")
                assertFileExists(subProject("app").projectPath.resolve("build/${Distribution.DIST}/js/productionExecutable/app.js"))
            }
        }
    }

    @TestMetadata("js-library-with-executable")
    @DisplayName("Webpack config block works after task configured")
    @GradleTest
    fun testWebpackConfigWorksAfterTaskConfigured(gradleVersion: GradleVersion) {
        project("js-library-with-executable", gradleVersion) {

            buildGradleKts.modify { originalScript ->
                buildString {
                    append(originalScript)
                    append(
                        """
                        |
                        |kotlin {
                        |   js {
                        |       browser {
                        |       }
                        |   }
                        |}
                        |
                        |tasks.all {
		                |       // do nothing
	                    |}
                        |
                        |kotlin {
                        |   js {
                        |       browser {
                        |          commonWebpackConfig {
                        |               outputFileName = "CORRECT_NAME.js"
	                    |           }
                        |       }
                        |   }
                        |}
                        |
                        """.trimMargin()
                    )
                }
            }

            build("jsBrowserDistribution") {
                assertTasksExecuted(":jsBrowserProductionWebpack")
                assertFileExists(projectPath.resolve("build/${Distribution.DIST}/js/productionExecutable/CORRECT_NAME.js"))
            }
        }
    }

    @TestMetadata("kotlin2JsProjectWithSourceMap")
    @MustRunOnChangesInJs
    @DisplayName("source map is generated")
    @GradleTest
    fun testKotlinJsSourceMap(gradleVersion: GradleVersion) {
        project("kotlin2JsProjectWithSourceMap", gradleVersion) {
            build("jsDevelopmentExecutableCompileSync") {
                val appSourceMap = subProject("app").projectPath
                    .resolve("build/compileSync/js/main/developmentExecutable/kotlin/$projectName-app.js.map")
                assertSourceMapSourcesResolveTo(
                    appSourceMap,
                    projectPath.resolve("app/src/jsMain/kotlin/main.kt"),
                    projectPath.resolve("lib/src/jsMain/kotlin/foo.kt"),
                )
                assertFileContains(appSourceMap, "\"sourcesContent\":[null")

                // The default should be generating simple names.
                assertFileContains(appSourceMap, "somewhereOverTheRainbow")
                assertFileDoesNotContain(appSourceMap, "\"names\":[]")
                assertFileDoesNotContain(appSourceMap, "app.C.somewhereOverTheRainbow")

                val libSourceMap = subProject("app").projectPath
                    .resolve("build/compileSync/js/main/developmentExecutable/kotlin/$projectName-lib.js.map")
                assertSourceMapSourcesResolveTo(libSourceMap, projectPath.resolve("lib/src/jsMain/kotlin/foo.kt"))
                assertFileContains(libSourceMap, "\"sourcesContent\":[null")

                // The default should be generating simple names.
                assertFileDoesNotContain(libSourceMap, "\"names\":[]")

                val libSourceMap2 = projectPath
                    .resolve("build/js/packages/$projectName-app/kotlin/$projectName-lib.js.map")
                assertSourceMapSourcesResolveTo(libSourceMap2, projectPath.resolve("lib/src/jsMain/kotlin/foo.kt"))
                assertFileContains(libSourceMap2, "\"sourcesContent\":[null")

                // The default should be generating simple names.
                assertFileDoesNotContain(libSourceMap2, "\"names\":[]")

                val appSourceMap2 = projectPath
                    .resolve("build/js/packages/$projectName-app/kotlin/$projectName-app.js.map")
                assertSourceMapSourcesResolveTo(
                    appSourceMap2,
                    projectPath.resolve("app/src/jsMain/kotlin/main.kt"),
                    projectPath.resolve("lib/src/jsMain/kotlin/foo.kt"),
                )
                assertFileContains(appSourceMap2, "\"sourcesContent\":[null")
            }
        }
    }

    @MustRunOnChangesInJs
    @DisplayName("Test correct sources path with per-file granularity ('KT-72833')")
    @GradleTest
    @TestMetadata("js-per-file")
    fun testCorrectSourcesPathInPerFile(gradleVersion: GradleVersion) {
        project("js-per-file", gradleVersion) {
            build("jsDevelopmentExecutableCompileSync") {
                val mainSource = projectPath.resolve("src/jsMain/kotlin/Main.kt")
                val someModuleSource = projectPath.resolve("src/jsMain/kotlin/SomeModule.kt")

                // In the sub-project build
                var mainSourceMap = projectPath
                    .resolve("build/compileSync/js/main/developmentExecutable/kotlin/$projectName/org/jetbrains/kotlin/testData/Main.mjs.map")

                assertSourceMapSourcesResolveTo(mainSourceMap, mainSource)
                assertFileContains(mainSourceMap, "\"sourcesContent\":[null")

                // In the root build
                mainSourceMap = projectPath
                    .resolve("build/js/packages/$projectName/kotlin/$projectName/org/jetbrains/kotlin/testData/Main.mjs.map")

                assertSourceMapSourcesResolveTo(mainSourceMap, mainSource)
                assertFileContains(mainSourceMap, "\"sourcesContent\":[null")

                // In the sub-project build
                var someModuleSourceMap = projectPath
                    .resolve("build/compileSync/js/main/developmentExecutable/kotlin/$projectName/SomeModule.mjs.map")

                assertSourceMapSourcesResolveTo(someModuleSourceMap, someModuleSource)
                assertFileContains(someModuleSourceMap, "\"sourcesContent\":[null")

                // In the root build
                someModuleSourceMap = projectPath
                    .resolve("build/js/packages/$projectName/kotlin/$projectName/SomeModule.mjs.map")

                assertSourceMapSourcesResolveTo(someModuleSourceMap, someModuleSource)
                assertFileContains(someModuleSourceMap, "\"sourcesContent\":[null")
            }
        }
    }

    @TestMetadata("kotlin2JsProjectWithSourceMap")
    @MustRunOnChangesInJs
    @DisplayName("path in source maps are remapped for custom outputFile in executable")
    @GradleTest
    fun testKotlinJsSourceMapCustomOutputFileInExecutable(gradleVersion: GradleVersion) {
        project("kotlin2JsProjectWithSourceMap", gradleVersion) {
            val taskSelector = "named<KotlinJsIrLink>(\"compileDevelopmentExecutableKotlinJs\")"
            buildGradleKts.appendText(
                """
                |project("app") {
                |   tasks.$taskSelector {
                |       destinationDirectory.set(file("${'$'}{buildDir}/kotlin2js"))
                |       compilerOptions.moduleName.set("app")
                |   }
                |}
                |
                """.trimMargin()
            )
            build("compileDevelopmentExecutableKotlinJs") {
                val mapFilePath = subProject("app").projectPath
                    .resolve("build/kotlin2js/app.js.map")
                assertSourceMapSourcesResolveTo(
                    mapFilePath,
                    projectPath.resolve("app/src/jsMain/kotlin/main.kt"),
                    // The IR BE generates correct paths for dependencies
                    projectPath.resolve("lib/src/jsMain/kotlin/foo.kt"),
                )
            }
        }
    }

    @TestMetadata("kotlin-multiplatform-browser-project")
    @MustRunOnChangesInJs
    @DisplayName("path in source maps are remapped for custom outputFile in library")
    @GradleTest
    fun testKotlinJsSourceMapCustomOutputFileInLibrary(gradleVersion: GradleVersion) {
        // With .js extension
        project("kotlin-multiplatform-browser-project", gradleVersion) {
            build(":lib:jsBrowserDevelopmentDistribution") {
                assertFileInProjectExists("lib/build/${Distribution.DIST}/js/developmentLibrary/kotlin-js-browser-lib.js")
                assertFileInProjectExists("lib/build/${Distribution.DIST}/js/developmentLibrary/kotlin-js-browser-lib.js.map")

                val mapFilePath = projectPath
                    .resolve("lib/build/${Distribution.DIST}/js/developmentLibrary/kotlin-js-browser-lib.js.map")

                assertSourceMapSourcesResolveTo(mapFilePath, projectPath.resolve("lib/src/jsMain/kotlin/Lib.kt"))
            }

            build(":lib:jsBrowserProductionDistribution") {
                assertFileInProjectExists("lib/build/${Distribution.DIST}/js/productionLibrary/kotlin-js-browser-lib.js")
                assertFileInProjectExists("lib/build/${Distribution.DIST}/js/productionLibrary/kotlin-js-browser-lib.js.map")

                val mapFilePath = projectPath
                    .resolve("lib/build/${Distribution.DIST}/js/productionLibrary/kotlin-js-browser-lib.js.map")

                assertSourceMapSourcesResolveTo(mapFilePath, projectPath.resolve("lib/src/jsMain/kotlin/Lib.kt"))
            }
        }
    }

    @TestMetadata("kotlin-multiplatform-browser-project")
    @MustRunOnChangesInJs
    @DisplayName("path in source maps are remapped for custom distribution outputDirectory in library")
    @GradleTest
    fun testKotlinJsSourceMapCustomDistributionDirectoryInLibrary(gradleVersion: GradleVersion) {
        project("kotlin-multiplatform-browser-project", gradleVersion) {
            subProject("lib").buildScriptInjection {
                kotlinMultiplatform.js {
                    binaries.withType(Library::class.java).configureEach {
                        it.distribution.outputDirectory.set(project.layout.buildDirectory.dir("custom/dist"))
                    }
                }
            }

            build(":lib:jsBrowserDevelopmentDistribution") {
                assertSourceMapSourcesResolveTo(
                    projectPath.resolve("lib/build/custom/dist/kotlin-js-browser-lib.js.map"),
                    projectPath.resolve("lib/src/jsMain/kotlin/Lib.kt"),
                )
            }
        }
    }

    @MustRunOnChangesInJs
    @DisplayName("path in source maps are remapped for default distribution in single-module library")
    @GradleTest
    fun testKotlinJsSourceMapDefaultDistributionInSingleModuleLibrary(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                kotlinMultiplatform.js {
                    nodejs()
                    binaries.library()
                }
            }
            val libSource = kotlinSourcesDir("jsMain").resolve("Lib.kt").apply {
                createParentDirectories()
                writeText("fun lib(): Int = 42\n")
            }

            build(":jsNodeDevelopmentLibraryDistribution") {
                assertSourceMapSourcesResolveTo(
                    projectPath.resolve("build/dist/js/developmentLibrary/empty.js.map"),
                    libSource,
                )
            }
        }
    }

    /**
     * Asserts that each of [expectedSources] is listed in the `sources` of [mapFile] as a relative path
     * that resolves, against the map's directory and its `sourceRoot`, to that file.
     */
    private fun assertSourceMapSourcesResolveTo(mapFile: Path, vararg expectedSources: Path) {
        assertFileExists(mapFile)
        val sourceMap = SourceMapSource.decode(mapFile.readText())
        val sourceRoot = Path(sourceMap.sourceRoot)
        assertAll(
            expectedSources.map { expectedSource ->
                {
                    assertFileExists(expectedSource)
                    val source = sourceMap.sources.singleOrNull { Path(it).name == expectedSource.name }
                    assertNotNull(source, "${expectedSource.name} is missing from the sources ${sourceMap.sources} of $mapFile")
                    assertFalse(Path(sourceMap.sourceRoot).resolve(source).isAbsolute, "Source path '$source' in $mapFile is not relative")
                    assertEquals(
                        expectedSource.normalize(),
                        mapFile.parent.resolve(sourceRoot).resolve(source).normalize(),
                        "Source path '$source' in $mapFile does not point to $expectedSource",
                    )
                }
            }
        )
    }

    @Serializable
    private data class SourceMapSource(
        val sourceRoot: String = "",
        val sources: List<String>,
    ) {
        companion object {
            private val json = Json {
                ignoreUnknownKeys = true
            }

            fun decode(content: String): SourceMapSource = json.decodeFromString(content)
        }
    }

    @TestMetadata("kotlin-multiplatform-browser-project")
    @MustRunOnChangesInJs
    @DisplayName("path in source maps are remapped for custom outputFile in library with ES modules")
    @GradleTest
    fun testKotlinJsSourceMapCustomOutputFileInLibraryWithESM(gradleVersion: GradleVersion) {
        project("kotlin-multiplatform-browser-project", gradleVersion) {
            subProject("lib").buildScriptInjection {
                kotlinMultiplatform.js {
                    useEsModules()
                }
            }

            build(":lib:jsBrowserDevelopmentDistribution") {
                assertFileInProjectExists("lib/build/${Distribution.DIST}/js/developmentLibrary/kotlin-js-browser-lib.mjs")
                assertFileInProjectExists("lib/build/${Distribution.DIST}/js/developmentLibrary/kotlin-js-browser-lib.mjs.map")

                val mapFilePath = projectPath
                    .resolve("lib/build/${Distribution.DIST}/js/developmentLibrary/kotlin-js-browser-lib.mjs.map")

                assertSourceMapSourcesResolveTo(mapFilePath, projectPath.resolve("lib/src/jsMain/kotlin/Lib.kt"))
            }

            build(":lib:jsBrowserProductionDistribution") {
                assertFileInProjectExists("lib/build/${Distribution.DIST}/js/productionLibrary/kotlin-js-browser-lib.mjs")
                assertFileInProjectExists("lib/build/${Distribution.DIST}/js/productionLibrary/kotlin-js-browser-lib.mjs.map")

                val mapFilePath = projectPath
                    .resolve("lib/build/${Distribution.DIST}/js/productionLibrary/kotlin-js-browser-lib.mjs.map")

                assertSourceMapSourcesResolveTo(mapFilePath, projectPath.resolve("lib/src/jsMain/kotlin/Lib.kt"))
            }
        }
    }

    @TestMetadata("kotlin2JsProjectWithSourceMap")
    @MustRunOnChangesInJs
    @DisplayName("source map is not generated when disabled")
    @GradleTest
    fun testKotlinJsSourceMapDisabled(gradleVersion: GradleVersion) {
        project("kotlin2JsProjectWithSourceMap", gradleVersion) {
            buildGradleKts.appendText(
                """
                |allprojects {
                |   tasks.withType<KotlinJsCompile> {
                |        compilerOptions.sourceMap.set(false)
                |   }
                |}
                |
                """.trimMargin()
            )
            build("jsDevelopmentExecutableCompileSync") {
                val jsFilePath = projectPath.resolve("build/js/packages/$projectName-app/kotlin/$projectName-app.js")
                assertFileExists(jsFilePath)
                assertFileNotExists(Path("$jsFilePath.map"))
            }
        }
    }

    @TestMetadata("kotlin2JsProjectWithSourceMap")
    @MustRunOnChangesInJs
    @DisplayName("sources of all modules are embedded into source map")
    @GradleTest
    fun testKotlinJsSourceMapEmbedSourcesAlways(gradleVersion: GradleVersion) {
        project("kotlin2JsProjectWithSourceMap", gradleVersion) {
            buildGradleKts.appendText(
                """
                |project("lib") {
                |   tasks.withType<KotlinJsCompile>() {
                |        compilerOptions {
                |            sourceMap.set(true)
                |            sourceMapEmbedSources.set(org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_ALWAYS)
                |        }
                |    }
                |}
                |project("app") {
                |    tasks.withType<KotlinJsCompile> {
                |        compilerOptions.sourceMapEmbedSources.set(
                |            org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_ALWAYS
                |        )
                |    }
                |}
                |
                """.trimMargin()
            )
            build("jsDevelopmentExecutableCompileSync") {
                val mapFilePath = projectPath.resolve("build/js/packages/$projectName-app/kotlin/$projectName-app.js.map")
                assertFileContains(
                    mapFilePath,
                    "fun main(args: Array<String>) {",
                    "inline fun foo(): String {",
                )
            }
        }
    }

    @TestMetadata("kotlin2JsProjectWithSourceMap")
    @MustRunOnChangesInJs
    @DisplayName("only sources of external modules are embedded into source map")
    @GradleTest
    fun testKotlinJsSourceMapEmbedSourcesInlining(gradleVersion: GradleVersion) {
        project("kotlin2JsProjectWithSourceMap", gradleVersion) {
            buildGradleKts.appendText(
                """
                |project("lib") {
                |   tasks.withType<KotlinJsCompile>() {
                |        compilerOptions {
                |            sourceMap.set(true)
                |            sourceMapEmbedSources.set(
                |                 org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_ALWAYS
                |            )
                |        }
                |    }
                |}
                |project("app") {
                |    tasks.withType<KotlinJsCompile> {
                |        compilerOptions.sourceMapEmbedSources.set(
                |            org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_INLINING
                |        )
                |    }
                |}
                |
                """.trimMargin()
            )
            build("jsDevelopmentExecutableCompileSync") {
                val mapFilePath = projectPath.resolve("build/js/packages/$projectName-app/kotlin/$projectName-app.js.map")
                assertFileDoesNotContain(
                    mapFilePath,
                    "fun main(args: Array<String>) {"
                )
                assertFileContains(
                    mapFilePath,
                    "inline fun foo(): String {",
                )
            }
        }
    }

    @DisplayName("browser distribution is generated")
    @GradleTest
    @TestMetadata("kotlin-js-browser-project")
    fun testBrowserDistribution(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {

            build("compileProductionExecutableKotlinJs") {
                assertTasksExecuted(":app:compileProductionExecutableKotlinJs")
                assert(task(":kotlinNpmInstall") == null) {
                    printBuildOutput()
                    "NPM install should not be run"
                }
            }

            build("assemble") {
                assertTasksExecuted(":app:jsBrowserProductionWebpack")

                assertDirectoryInProjectExists("build/js/packages/kotlin-js-browser-base")
                assertDirectoryInProjectExists("build/js/packages/kotlin-js-browser-lib")
                assertDirectoryInProjectExists("build/js/packages/kotlin-js-browser-app")

                assertFileInProjectExists("app/build/${Distribution.DIST}/js/productionExecutable/app.js")
            }

            build("clean", "jsBrowserDistribution") {
                assertTasksExecuted(
                    ":app:jsProcessResources",
                    ":app:jsBrowserDistribution"
                )

                assertFileInProjectExists("app/build/${Distribution.DIST}/js/productionExecutable/index.html")
            }
        }
    }

    @DisplayName("browser distribution is generated with KMP plugin")
    @GradleTest
    @TestMetadata("kotlin-multiplatform-browser-project")
    fun testMultiplatformBrowserDistribution(gradleVersion: GradleVersion) {
        project("kotlin-multiplatform-browser-project", gradleVersion) {

            build("compileProductionExecutableKotlinJs") {
                assertTasksExecuted(":app:compileProductionExecutableKotlinJs")
                assert(task(":kotlinNpmInstall") == null) {
                    printBuildOutput()
                    "NPM install should not be run"
                }
            }

            build("assemble") {
                assertTasksExecuted(":app:jsBrowserProductionWebpack")
            }
        }
    }

    @TestMetadata("js-dynamic-webpack-config-d")
    @DisplayName("webpack-config-d directory created during the build is not ignored")
    @GradleTest
    fun testDynamicWebpackConfigD(gradleVersion: GradleVersion) {
        project("js-dynamic-webpack-config-d", gradleVersion) {
            build("build") {
                assertDirectoryInProjectExists("build/js/packages/js-dynamic-webpack-config-d")
                assertFileInProjectContains("build/js/packages/js-dynamic-webpack-config-d/webpack.config.js", "// hello from patch.js")
            }
        }
    }

    @TestMetadata("kotlin-js-test-webpack-config")
    @DisplayName("webpack configuration is valid")
    @GradleTest
    fun testWebpackConfig(gradleVersion: GradleVersion) {
        project("kotlin-js-test-webpack-config", gradleVersion) {
            build("jsBrowserDevelopmentWebpack")

            build("jsCheckConfigDevelopmentWebpack")

            build("jsCheckConfigProductionWebpack")

            build("jsCheckConfigDevelopmentRun")

            build("jsCheckConfigProductionRun")
        }
    }

    @DisplayName("Check source map config of webpack")
    @GradleTest
    @TestMetadata("kotlin-js-browser-project")
    fun testWebpackSourceMapConfig(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            build("assemble") {
                assertTasksExecuted(":app:jsBrowserProductionWebpack")

                val sourceMapConfig = """
                |config.module.rules.push({
                    |test: /\.m?js${'$'}/,
                    |use: ["source-map-loader"],
                    |enforce: "pre"
                |});
                """.trimMargin()

                val webpackConfig = projectPath.resolve("build/js/packages/kotlin-js-browser-app/webpack.config.js")
                    .readLines()

                var startIndex = 0
                for ((index, line) in webpackConfig.withIndex()) {
                    if (line.contains("// source maps")) {
                        startIndex = index + 1
                        break
                    }
                }

                val expectedLinesCount = sourceMapConfig.lines().count()

                val actual = webpackConfig.subList(startIndex, startIndex + expectedLinesCount)
                    .joinToString("\n") { it.trim() }

                assertEquals(
                    sourceMapConfig,
                    actual
                )
            }
        }
    }

    @DisplayName("Check webpack update with per-file")
    @GradleTest
    @TestMetadata("js-per-file")
    fun testWebpackUpdatePerFile(gradleVersion: GradleVersion) {
        project("js-per-file", gradleVersion) {
            build("assemble") {
                assertTasksExecuted(":jsBrowserProductionWebpack")
            }

            projectPath.resolve("src/jsMain/kotlin/Main.kt").replaceText("CUSTOM TEXT", "CUSTOM TEXT 2")

            build("assemble") {
                assertTasksExecuted(":jsBrowserProductionWebpack")
            }
        }
    }
}
