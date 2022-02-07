/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.google.gson.Gson
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.configuration.WarningMode
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.sources.METADATA_CONFIGURATION_NAME_SUFFIX
import org.jetbrains.kotlin.gradle.targets.js.ir.KLIB_TYPE
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectModules
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.targets.js.npm.fromSrcPackageJson
import org.jetbrains.kotlin.gradle.tasks.USING_JS_INCREMENTAL_COMPILATION_MESSAGE
import org.jetbrains.kotlin.gradle.tasks.USING_JS_IR_BACKEND_MESSAGE
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.jsCompilerType
import org.jetbrains.kotlin.gradle.util.normalizePath
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.DisabledIf
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipFile
import kotlin.io.path.*
import kotlin.streams.toList
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@JsGradlePluginTests
class Kotlin2JsIrGradlePluginIT : AbstractKotlin2JsGradlePluginIT(true) {

    @DisplayName("TS type declarations are generated")
    @GradleTest
    fun generateDts(gradleVersion: GradleVersion) {
        project("kotlin2JsIrDtsGeneration", gradleVersion) {
            build("build") {
                checkIrCompilationMessage()

                assertFileInProjectExists("build/kotlin2js/main/lib.js")
                val dts = projectPath.resolve("build/kotlin2js/main/lib.d.ts")
                assertFileExists(dts)
                assertFileContains(dts, "function bar(): string")
            }
        }
    }

    @DisplayName("stale output is cleaned")
    @GradleTest
    fun testCleanOutputWithEmptySources(gradleVersion: GradleVersion) {
        project("kotlin-js-nodejs-project", gradleVersion) {
            build("assemble") {
                assertTasksExecuted(":compileProductionExecutableKotlinJs")

                assertFileInProjectExists("build/js/packages/kotlin-js-nodejs-project/kotlin/kotlin-js-nodejs-project.js")
            }

            projectPath.resolve("src").toFile().deleteRecursively()

            buildGradle.appendText(
                """
                |
                |tasks.named("compileProductionExecutableKotlinJs").configure {
                |    mode = org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode.DEVELOPMENT
                |}
               """.trimMargin()
            )

            build("build") {
                assertFileInProjectNotExists("build/js/packages/kotlin-js-nodejs/kotlin/")
            }
        }
    }

    @DisplayName("js composite build works")
    @GradleTest
    fun testJsCompositeBuild(gradleVersion: GradleVersion) {
        project("js-composite-build", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)
            gradleProperties.appendText(jsCompilerType(KotlinJsCompilerType.IR))

            subProject("lib").apply {
                buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)
                gradleProperties.appendText(jsCompilerType(KotlinJsCompilerType.IR))
            }

            fun asyncVersion(rootModulePath: String, moduleName: String): String =
                NpmProjectModules(projectPath.resolve(rootModulePath).toFile())
                    .require(moduleName)
                    .let { Paths.get(it).parent.parent.resolve(NpmProject.PACKAGE_JSON) }
                    .let { fromSrcPackageJson(it.toFile()) }
                    .let { it!!.version }

            build("build") {
                val appAsyncVersion = asyncVersion("build/js/node_modules/js-composite-build", "async")
                assertEquals("3.2.0", appAsyncVersion)

                val libAsyncVersion = asyncVersion("build/js/node_modules/lib2", "async")
                assertEquals("2.6.2", libAsyncVersion)
            }
        }
    }

    @GradleTest
    @Disabled  // Persistent IR is no longer supported
    fun testJsIrIncrementalInParallel(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)
            gradleProperties.appendText(
                """
                |
                |kotlin.incremental.js.ir=true
                |org.gradle.parallel=true
                """.trimMargin()
            )

            build("assemble")
        }
    }

    @DisplayName("Check IR incremental cache invalidation by compiler args")
    @GradleTest
    fun testJsIrIncrementalCacheInvalidationByArgs(gradleVersion: GradleVersion) {
        project("kotlin2JsIrICProject", gradleVersion) {
            val buildConfig = buildGradleKts.readText()

            fun setLazyInitializationArg(value: Boolean) {
                buildGradleKts.writeText(buildConfig)
                buildGradleKts.appendText(
                    """
                    |
                    |tasks.named<org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink>("compileDevelopmentExecutableKotlinJs") {
                    |    kotlinOptions {
                    |        freeCompilerArgs += "-Xir-property-lazy-initialization=$value"
                    |   }
                    |}
                    """.trimMargin()
                )
            }

            fun String.testScriptOutLines() = this.lines().mapNotNull {
                val trimmed = it.removePrefix(">>> TEST OUT: ")
                if (trimmed == it) null else trimmed
            }

            // -Xir-property-lazy-initialization default is true
            build("nodeRun") {
                assertTasksExecuted(":compileDevelopmentExecutableKotlinJs")
                assertEquals(listOf("Hello, Gradle."), output.testScriptOutLines())
            }

            setLazyInitializationArg(false)
            build("nodeRun") {
                assertTasksExecuted(":compileDevelopmentExecutableKotlinJs")
                assertEquals(listOf("TOP LEVEL!", "Hello, Gradle."), output.testScriptOutLines())
            }

            setLazyInitializationArg(true)
            build("nodeRun") {
                assertTasksExecuted(":compileDevelopmentExecutableKotlinJs")
                assertEquals(listOf("Hello, Gradle."), output.testScriptOutLines())
            }
        }
    }

    @DisplayName("incremental compilation for JS IR  consider multiple artifacts in one project")
    @GradleTest
    fun testJsIrIncrementalMultipleArtifacts(gradleVersion: GradleVersion) {
        project("kotlin-js-ir-ic-multiple-artifacts", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)

            build("compileDevelopmentExecutableKotlinJs") {
                val cacheDir = projectPath.resolve("app/build/klib/cache/lib/")
                    .toFile()
                assertTrue("Lib cache size should be 2") {
                    cacheDir
                        .list()
                        ?.size == 2
                }

                var lib = false
                var libOther = false

                cacheDir.listFiles()!!
                    .forEach {
                        it.listFiles()!!
                            .filter { it.isFile }
                            .forEach {
                                val text = it.readText()
                                if (text.contains("<kotlin-js-ir-ic-multiple-artifacts:lib>")) {
                                    if (lib) {
                                        error("lib should be only once in cache")
                                    }
                                    lib = true
                                }

                                if (text.contains("<kotlin-js-ir-ic-multiple-artifacts:lib_other>")) {
                                    if (libOther) {
                                        error("libOther should be only once in cache")
                                    }
                                    libOther = true
                                }
                            }

                    }

                assertTrue("lib and libOther should be once in cache") {
                    lib && libOther
                }
                assertTasksExecuted(":app:compileDevelopmentExecutableKotlinJs")
            }
        }
    }
}

@JsGradlePluginTests
class Kotlin2JsGradlePluginIT : AbstractKotlin2JsGradlePluginIT(false) {

    @DisplayName("incremental compilation for js works")
    @GradleTest
    fun testIncrementalCompilation(gradleVersion: GradleVersion) {
        val buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        project("kotlin2JsICProject", gradleVersion, buildOptions = buildOptions) {
            val modules = listOf("app", "lib")
            val mainFiles = modules.flatMapTo(LinkedHashSet()) {
                projectPath.resolve("$it/src/main").allKotlinFiles
            }

            build("build") {
                checkIrCompilationMessage()
                assertOutputContains(USING_JS_INCREMENTAL_COMPILATION_MESSAGE)
                if (irBackend) {
                    assertCompiledKotlinSources(mainFiles.relativizeTo(projectPath), output)
                } else {
                    assertCompiledKotlinSources(projectPath.allKotlinFiles.relativizeTo(projectPath), output)
                }
            }

            build("build") {
                assertCompiledKotlinSources(emptyList(), output)
            }

            val modifiedFile = subProject("lib").kotlinSourcesDir().resolve("A.kt") ?: error("No A.kt file in test project")
            modifiedFile.modify {
                it.replace("val x = 0", "val x = \"a\"")
            }
            build("build") {
                checkIrCompilationMessage()
                assertOutputContains(USING_JS_INCREMENTAL_COMPILATION_MESSAGE)
                val affectedFiles = listOf("A.kt", "useAInLibMain.kt", "useAInAppMain.kt", "useAInAppTest.kt").mapNotNull {
                    projectPath.findInPath(it)
                }
                if (irBackend) {
                    // only klib ic is supported for now, so tests are generated non-incrementally with ir backend
                    assertCompiledKotlinSources(affectedFiles.filter { it in mainFiles }.relativizeTo(projectPath), output)
                } else {
                    assertCompiledKotlinSources(affectedFiles.relativizeTo(projectPath), output)
                }
            }
        }
    }

    @DisplayName("builtins are loaded")
    @GradleTest
    fun testKotlinJsBuiltins(gradleVersion: GradleVersion) {
        project("kotlinBuiltins", gradleVersion) {
            subProject("app").buildGradle.modify { originalScript ->
                buildString {
                    append(
                        originalScript.replace(
                            "id \"org.jetbrains.kotlin.jvm\"",
                            "id \"org.jetbrains.kotlin.js\""
                        )
                    )
                    append(
                        """
                        |
                        |afterEvaluate {
                        |    tasks.named('compileKotlinJs') {
                        |        kotlinOptions.outputFile = "${'$'}{project.projectDir}/out/out.js"
                        |    }
                        |}
                        |
                        """.trimMargin()
                    )
                }
            }
            build("build")
        }
    }

    @DisplayName("DCE minifies output file")
    @GradleTest
    fun testDce(gradleVersion: GradleVersion) {
        project("kotlin2JsDceProject", gradleVersion) {
            build("runRhino") {
                val pathPrefix = "mainProject/build/kotlin-js-min/main"
                assertFileInProjectExists("$pathPrefix/exampleapp.js.map")
                assertFileInProjectExists("$pathPrefix/examplelib.js.map")
                assertFileInProjectContains("$pathPrefix/exampleapp.js.map", "\"../../../src/main/kotlin/exampleapp/main.kt\"")

                val kotlinJs = projectPath.resolve("$pathPrefix/kotlin.js")
                assertFileExists(kotlinJs)
                assertTrue(
                    Files.size(kotlinJs) < 500 * 1000,
                    "Looks like kotlin.js file was not minified by DCE"
                )
            }
        }
    }

    @DisplayName("DCE output directory can be changed")
    @GradleTest
    fun testDceOutputPath(gradleVersion: GradleVersion) {
        project("kotlin2JsDceProject", gradleVersion) {
            subProject("mainProject").buildGradle.modify { originalScript ->
                buildString {
                    append(originalScript)
                    append(
                        """
                        |
                        |runDceKotlinJs.dceOptions.outputDirectory = "${'$'}{buildDir}/min"
                        |runRhino.args = ["-f", "min/kotlin.js", "-f", "min/examplelib.js", "-f", "min/exampleapp.js", "-f", "../check.js"]
                        """.trimMargin()
                    )
                }
            }

            build("runRhino") {
                val pathPrefix = "mainProject/build/min"
                assertFileInProjectExists("$pathPrefix/examplelib.js.map")
                assertFileInProjectContains("$pathPrefix/exampleapp.js.map", "\"../../src/main/kotlin/exampleapp/main.kt\"")

                val kotlinJs = projectPath.resolve("$pathPrefix/kotlin.js")
                assertFileExists(kotlinJs)
                assertTrue(Files.size(kotlinJs) < 500 * 1000, "Looks like kotlin.js file was not minified by DCE")
            }
        }
    }

    @DisplayName("DCE in dev mode doesn't minify output file")
    @GradleTest
    fun testDceDevMode(gradleVersion: GradleVersion) {
        project("kotlin2JsDceProject", gradleVersion) {
            subProject("mainProject").buildGradle.appendText(
                """
                |
                |runDceKotlinJs.dceOptions.devMode = true
                |    
                """.trimMargin()
            )

            build("runRhino") {
                val pathPrefix = "mainProject/build/kotlin-js-min/main"
                assertFileInProjectExists("$pathPrefix/examplelib.js.map")
                assertFileInProjectContains("$pathPrefix/exampleapp.js.map", "\"../../../src/main/kotlin/exampleapp/main.kt\"")

                val kotlinJs = projectPath.resolve("$pathPrefix/kotlin.js")
                assertFileExists(kotlinJs)
                assertTrue(Files.size(kotlinJs) > 1000 * 1000, "Looks like kotlin.js file was minified by DCE")
            }
        }
    }

    @DisplayName("DCE minifies FileCollection dependencies")
    @GradleTest
    fun testDceFileCollectionDependency(gradleVersion: GradleVersion) {
        project("kotlin2JsDceProject", gradleVersion) {
            subProject("mainProject").buildGradle.modify {
                it.replace("compile project(\":libraryProject\")", "compile project(\":libraryProject\").sourceSets.main.output")
            }

            build("runRhino") {
                val pathPrefix = "mainProject/build/kotlin-js-min/main"
                assertFileInProjectExists("$pathPrefix/examplelib.js.map")
                assertFileInProjectContains("$pathPrefix/exampleapp.js.map", "\"../../../src/main/kotlin/exampleapp/main.kt\"")

                val kotlinJs = projectPath.resolve("$pathPrefix/kotlin.js")
                assertFileExists(kotlinJs)
                assertTrue(Files.size(kotlinJs) < 500 * 1000, "Looks like kotlin.js file was not minified by DCE")
            }
        }
    }

    @DisplayName("js files from dependency are installed")
    @GradleTest
    fun testKotlinJsDependencyWithJsFiles(gradleVersion: GradleVersion) {
        project("kotlin-js-dependency-with-js-files", gradleVersion) {
            build("packageJson") {
                val dependency = "2p-parser-core"
                val version = "0.11.1"

                val dependencyDirectory = projectPath.resolve("build/js/packages_imported/$dependency/$version")
                assertDirectoryExists(dependencyDirectory)

                val packageJson = dependencyDirectory
                    .resolve(NpmProject.PACKAGE_JSON)
                    .let {
                        Gson().fromJson(it.readText(), PackageJson::class.java)
                    }

                assertEquals(dependency, packageJson.name)
                assertEquals(version, packageJson.version)
                assertEquals("$dependency.js", packageJson.main)
            }
        }
    }

    @DisplayName("DCE in dev mode replaces outdated dependencies on incremental build")
    @GradleTest
    fun testIncrementalDceDevModeOnExternalDependency(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)

            build(":base:jsLegacyJar")

            val baseSubproject = subProject("base")
            val libSubproject = subProject("lib")
            val baseJar = baseSubproject.projectPath.resolve("build/libs/base-legacy.jar")
            val originalBaseJar = libSubproject.projectPath.resolve("base.1.jar")
            val modifiedBaseJar = libSubproject.projectPath.resolve("base.2.jar")
            Files.copy(baseJar, originalBaseJar)

            baseSubproject.kotlinSourcesDir().resolve("Base.kt").appendText(
                """
                |
                |fun bestRandom() = 4
                """.trimMargin()
            )

            build(":base:jsLegacyJar")

            Files.copy(baseJar, modifiedBaseJar)

            val baseBuildscript = baseSubproject.buildGradleKts
            val libBuildscript = libSubproject.buildGradleKts
            baseBuildscript.modify {
                it.replace("js(\"both\")", "js(\"both\") { moduleName = \"base2\" }")
            }
            libBuildscript.modify {
                it.replace("implementation(project(\":base\"))", "implementation(files(\"${normalizePath(originalBaseJar.toString())}\"))")
            }
            libBuildscript.appendText(
                """
                kotlin.js().browser {
                    dceTask {
                        dceOptions.devMode = true
                    }
                }
                """.trimMargin()
            )

            val baseDceFile = projectPath.resolve("build/js/packages/kotlin-js-browser-lib/kotlin-dce/kotlin-js-browser-base-js-legacy.js")

            build(":lib:processDceKotlinJs") {
                assertFileDoesNotContain(baseDceFile, "bestRandom")
            }

            libBuildscript.modify {
                it.replace(normalizePath(originalBaseJar.toString()), normalizePath(modifiedBaseJar.toString()))
            }

            build(":lib:processDceKotlinJs") {
                assertFileContains(baseDceFile, "bestRandom")
            }
        }
    }
}

@JsGradlePluginTests
abstract class AbstractKotlin2JsGradlePluginIT(protected val irBackend: Boolean) : KGPBaseTest() {
    private val defaultJsOptions = BuildOptions.JsOptions(
        useIrBackend = irBackend,
        jsCompilerType = if (irBackend) KotlinJsCompilerType.IR else KotlinJsCompilerType.LEGACY,
    )

    final override val defaultBuildOptions =
        super.defaultBuildOptions.copy(
            jsOptions = defaultJsOptions,
            warningMode = WarningMode.Summary
        )

    protected fun BuildResult.checkIrCompilationMessage() {
        if (irBackend) {
            assertOutputContains(USING_JS_IR_BACKEND_MESSAGE)
        } else {
            assertOutputDoesNotContain(USING_JS_IR_BACKEND_MESSAGE)
        }
    }

    @DisplayName("js default output is included into jar")
    @GradleTest
    fun testJarIncludesJsDefaultOutput(gradleVersion: GradleVersion) {
        project("kotlin2JsNoOutputFileProject", gradleVersion) {
            build("jar") {
                checkIrCompilationMessage()

                assertTasksExecuted(":compileKotlin2Js")
                val jarPath = projectPath.resolve("build/libs/kotlin2JsNoOutputFileProject.jar")
                assertFileExists(jarPath)
                ZipFile(jarPath.toFile()).use { jar ->
                    if (!irBackend) {
                        assertEquals(
                            1, jar.entries().asSequence().count { it.name == "kotlin2JsNoOutputFileProject.js" },
                            "The jar should contain an entry `kotlin2JsNoOutputFileProject.js` with no duplicates"
                        )
                    }
                }
            }
        }
    }

    @DisplayName("js customized output is included into jar")
    @GradleTest
    fun testJarIncludesJsOutputSetExplicitly(gradleVersion: GradleVersion) {
        project("kotlin2JsModuleKind", gradleVersion) {
            build(":jar") {
                checkIrCompilationMessage()

                assertTasksExecuted(":compileKotlin2Js")
                val jarPath = projectPath.resolve("build/libs/kotlin2JsModuleKind.jar")
                assertFileExists(jarPath)
                ZipFile(jarPath.toFile()).use { jar ->
                    assertEquals(
                        1, jar.entries().asSequence().count { it.name == "app.js" },
                        "The jar should contain an entry `app.js` with no duplicates"
                    )
                }
            }
        }
    }

    @DisplayName("moduleKind option works")
    @GradleTest
    fun testModuleKind(gradleVersion: GradleVersion) {
        project("kotlin2JsModuleKind", gradleVersion) {
            build("runRhino") {
                checkIrCompilationMessage()
            }
        }
    }

    @DisplayName("default output file is generated")
    @GradleTest
    fun testDefaultOutputFile(gradleVersion: GradleVersion) {
        project("kotlin2JsNoOutputFileProject", gradleVersion) {
            build("build") {
                checkIrCompilationMessage()
                if (!irBackend) {
                    assertFileExists(kotlinClassesDir().resolve("kotlin2JsNoOutputFileProject.js"))
                }
                assertFileExists(kotlinClassesDir(sourceSet = "test").resolve("kotlin2JsNoOutputFileProject_test.js"))
            }
        }
    }

    @DisplayName("test compilation can access main compilation")
    @GradleTest
    fun testCompileTestCouldAccessProduction(gradleVersion: GradleVersion) {
        project("kotlin2JsProjectWithTests", gradleVersion) {
            build("build") {
                checkIrCompilationMessage()

                assertTasksExecuted(
                    ":compileKotlin2Js",
                    ":compileTestKotlin2Js"
                )
                if (irBackend) {
                    assertFileInProjectExists("build/kotlin2js/main/default/manifest")
                } else {
                    assertFileInProjectExists("build/kotlin2js/main/module.js")
                }
                assertFileInProjectExists("build/kotlin2js/test/module-tests.js")
            }
        }
    }

    @DisplayName("test compilation can access internal symbols of main compilation")
    @GradleTest
    fun testCompilerTestAccessInternalProduction(gradleVersion: GradleVersion) {
        project("kotlin2JsInternalTest", gradleVersion) {
            build("runRhino") {
                checkIrCompilationMessage()
            }
        }
    }

    @DisplayName("works with custom source sets")
    @GradleTest
    fun testJsCustomSourceSet(gradleVersion: GradleVersion) {
        project("kotlin2JsProjectWithCustomSourceset", gradleVersion) {
            build("build") {
                checkIrCompilationMessage()

                assertTasksExecuted(
                    ":compileKotlin2Js",
                    ":compileIntegrationTestKotlin2Js"
                )

                if (!irBackend) {
                    assertFileInProjectExists("build/kotlin2js/main/module.js")
                }
                assertFileInProjectExists("build/kotlin2js/integrationTest/module-inttests.js")

                val jarPath = projectPath.resolve("build/libs/kotlin2JsProjectWithCustomSourceset-inttests.jar")
                assertFileExists(jarPath)
                ZipFile(jarPath.toFile()).use { jar ->
                    assertEquals(
                        1, jar.entries().asSequence().count { it.name == "module-inttests.js" },
                        "The jar should contain an entry `module-inttests.js` with no duplicates"
                    )
                }
            }
        }
    }


    @DisplayName("source map is generated")
    @GradleTest
    @DisabledIf(
        "org.jetbrains.kotlin.gradle.AbstractKotlin2JsGradlePluginIT#getIrBackend",
        disabledReason = "Source maps are not supported in IR backend"
    )
    fun testKotlinJsSourceMap(gradleVersion: GradleVersion) {
        project("kotlin2JsNoOutputFileProject", gradleVersion) {
            buildGradle.appendText(
                """
                |
                |compileKotlin2Js.kotlinOptions.sourceMap = true
                |compileKotlin2Js.kotlinOptions.sourceMapPrefix = "prefixprefix/"
                |compileKotlin2Js.kotlinOptions.outputFile = "${'$'}{buildDir}/kotlin2js/main/app.js"
                """.trimMargin()
            )

            build("build") {
                val mapFilePath = projectPath.resolve("build/kotlin2js/main/app.js.map")
                assertFileExists(mapFilePath)
                val sourceFilePath = "prefixprefix/src/main/kotlin/example/Dummy.kt"
                assertFileContains(mapFilePath, "\"$sourceFilePath\"")
            }
        }
    }

    @DisplayName("sources can be embedded into source map")
    @GradleTest
    @DisabledIf(
        "org.jetbrains.kotlin.gradle.AbstractKotlin2JsGradlePluginIT#getIrBackend",
        disabledReason = "Source maps are not supported in IR backend"
    )
    fun testKotlinJsSourceMapInline(gradleVersion: GradleVersion) {
        project("kotlin2JsProjectWithSourceMapInline", gradleVersion) {
            build("build") {
                val mapFilePath = subProject("app").kotlinClassesDir().resolve("app.js.map")
                assertFileExists(mapFilePath)
                assertFileContains(
                    mapFilePath,
                    "\"./src/main/kotlin/main.kt\"",
                    "\"./src/main/kotlin/foo.kt\"",
                    "\"fun main(args: Array<String>) {",
                    "\"inline fun foo(): String {",
                )
            }
        }
    }

    @DisplayName("non-incremental compilation works")
    @GradleTest
    fun testIncrementalCompilationDisabled(gradleVersion: GradleVersion) {
        val jsOptions = defaultJsOptions.run {
            if (irBackend) copy(incrementalJsKlib = false) else copy(incrementalJs = false)
        }
        val options = defaultBuildOptions.copy(jsOptions = jsOptions)
        project("kotlin2JsICProject", gradleVersion, buildOptions = options) {
            build("build") {
                checkIrCompilationMessage()
                assertOutputDoesNotContain(USING_JS_INCREMENTAL_COMPILATION_MESSAGE)
            }
        }
    }

    @DisplayName("smoke test of org.jetbrains.kotlin.js plugin")
    @GradleTest
    @DisabledIf(
        "org.jetbrains.kotlin.gradle.AbstractKotlin2JsGradlePluginIT#getIrBackend",
        disabledReason = "kotlinx.html doesn't support IR"
    )
    fun testNewKotlinJsPlugin(gradleVersion: GradleVersion) {
        project("kotlin-js-plugin-project", gradleVersion) {
            build("publish", "runDceKotlin", "test", "runDceBenchmarkKotlin") {
                assertTasksExecuted(
                    ":compileKotlinJs", ":compileTestKotlinJs", ":compileBenchmarkKotlinJs",
                    ":runDceKotlin", ":runDceBenchmarkKotlin"
                )

                val moduleDir = projectPath.resolve("build/repo/com/example/kotlin-js-plugin/1.0/")

                val publishedJar = moduleDir.resolve("kotlin-js-plugin-1.0.jar")
                ZipFile(publishedJar.toFile()).use { zip ->
                    val entries = zip.entries().asSequence().map { it.name }
                    assertTrue { "kotlin-js-plugin.js" in entries }
                }

                val publishedPom = moduleDir.resolve("kotlin-js-plugin-1.0.pom")
                val kotlinVersion = defaultBuildOptions.kotlinVersion
                val pomText = publishedPom.readText().replace(Regex("\\s+"), "")
                assertTrue { "kotlinx-html-js</artifactId><version>0.6.12</version><scope>compile</scope>" in pomText }
                assertTrue { "kotlin-stdlib-js</artifactId><version>$kotlinVersion</version><scope>runtime</scope>" in pomText }

                assertFileExists(moduleDir.resolve("kotlin-js-plugin-1.0-sources.jar"))

                assertFileInProjectExists("build/js/node_modules/kotlin/kotlin.js")
                assertFileInProjectExists("build/js/node_modules/kotlin/kotlin.js.map")
                assertFileInProjectExists("build/js/node_modules/kotlin-test/kotlin-test.js")
                assertFileInProjectExists("build/js/node_modules/kotlin-test/kotlin-test.js.map")
                assertFileInProjectExists("build/js/node_modules/kotlin-test-js-runner/kotlin-test-nodejs-runner.js")
                assertFileInProjectExists("build/js/node_modules/kotlin-test-js-runner/kotlin-test-nodejs-runner.js.map")
                assertFileInProjectExists("build/js/node_modules/kotlin-js-plugin/kotlin/kotlin-js-plugin.js")
                assertFileInProjectExists("build/js/node_modules/kotlin-js-plugin/kotlin/kotlin-js-plugin.js.map")
                assertFileInProjectExists("build/js/node_modules/kotlin-js-plugin-test/kotlin/kotlin-js-plugin-test.js")
                assertFileInProjectExists("build/js/node_modules/kotlin-js-plugin-test/kotlin/kotlin-js-plugin-test.js.map")

                // Gradle 6.6+ slightly changed format of xml test results
                if (gradleVersion < GradleVersion.version("6.6")) {
                    assertTestResults(projectPath.resolve("tests_pre6.6.xml"), "nodeTest")
                } else {
                    assertTestResults(projectPath.resolve("tests.xml"), "nodeTest")
                }
            }
        }
    }

    @DisplayName("yarn is set up")
    @GradleTest
    fun testYarnSetup(gradleVersion: GradleVersion) {
        project("yarn-setup", gradleVersion) {
            build("yarnFolderRemove")

            build("kotlinYarnSetup", "yarnFolderCheck") {
                assertTasksExecuted(
                    ":kotlinYarnSetup",
                    ":yarnFolderCheck"
                )
            }

            buildGradleKts.appendText(
                """
                |
                |yarn.version = "1.9.3"
                """.trimMargin()
            )

            build("yarnConcreteVersionFolderChecker") {
                assertTasksExecuted(
                    ":kotlinYarnSetup",
                    ":yarnConcreteVersionFolderChecker"
                )
            }
        }
    }

    @DisplayName("NPM dependencies are installed")
    @GradleTest
    fun testNpmDependencies(gradleVersion: GradleVersion) {
        project("npm-dependencies", gradleVersion) {
            build("build") {
                assertDirectoryInProjectExists("build/js/node_modules/file-dependency")
                assertDirectoryInProjectExists("build/js/node_modules/file-dependency-2")
                assertFileInProjectExists("build/js/node_modules/file-dependency-3/index.js")
                assertFileInProjectExists("build/js/node_modules/42/package.json")
            }
        }
    }

    @DisplayName("public NPM dependencies are included into package.json")
    @GradleTest
    fun testPackageJsonWithPublicNpmDependencies(gradleVersion: GradleVersion) {
        project("npm-dependencies", gradleVersion) {
            build("jsJar") {
                val archive = projectPath
                    .resolve("build/libs")
                    .allFilesWithExtension(if (irBackend) KLIB_TYPE else "jar")
                    .single()

                ZipFile(archive.toFile()).use { zipFile ->
                    val packageJsonCandidates = zipFile.entries()
                        .asSequence()
                        .filter { it.name == NpmProject.PACKAGE_JSON }
                        .toList()

                    assertTrue("Expected existence of package.json in archive") {
                        packageJsonCandidates.size == 1
                    }

                    zipFile.getInputStream(packageJsonCandidates.single()).use {
                        it.reader().use { reader ->
                            val packageJson = Gson().fromJson(reader, PackageJson::class.java)
                            val devDep = "42"
                            val devDepVersion = "0.0.1"
                            assertTrue(
                                "Dev dependency \"$devDep\": \"$devDepVersion\" in package.json expected, but actual:\n" +
                                        "${packageJson.devDependencies}"
                            ) {
                                val devDependencies = packageJson.devDependencies
                                devDependencies
                                    .containsKey(devDep) &&
                                        devDependencies[devDep] == devDepVersion
                            }

                            val dep = "@yworks/optimizer"
                            val depVersion = "1.0.6"
                            assertTrue(
                                "Dependency \"$dep\": \"$depVersion\" in package.json expected, but actual:\n" +
                                        "${packageJson.dependencies}"
                            ) {
                                val dependencies = packageJson.dependencies
                                dependencies
                                    .containsKey(dep) &&
                                        dependencies[dep] == depVersion
                            }

                            val peerDep = "date-arithmetic"
                            val peerDepVersion = "4.1.0"
                            assertTrue(
                                "Peer dependency \"$peerDep\": \"$peerDepVersion\" in package.json expected, but actual:\n" +
                                        "${packageJson.peerDependencies}"
                            ) {
                                val peerDependencies = packageJson.peerDependencies
                                peerDependencies
                                    .containsKey(peerDep) &&
                                        peerDependencies[peerDep] == peerDepVersion
                            }
                        }
                    }
                }
            }

            buildGradleKts.modify { content ->
                content
                    .lines()
                    .joinToString(separator = "\n") {
                        if (it.contains("npm", ignoreCase = true)) "" else it
                    }
            }

            build("jsJar") {
                val archive = Files.list(projectPath.resolve("build").resolve("libs")).use { files ->
                    files
                        .filter { it.extension == if (irBackend) KLIB_TYPE else "jar" }
                        .toList()
                        .single()
                }

                ZipFile(archive.toFile()).use { zipFile ->
                    val packageJsonCandidates = zipFile.entries()
                        .asSequence()
                        .filter { it.name == NpmProject.PACKAGE_JSON }
                        .toList()

                    assertTrue("Expected absence of package.json in ${archive.name}") {
                        packageJsonCandidates.isEmpty()
                    }
                }
            }
        }
    }

    @DisplayName("browser distribution is generated")
    @GradleTest
    fun testBrowserDistribution(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)

            if (irBackend) {
                gradleProperties.appendText(jsCompilerType(KotlinJsCompilerType.IR))
            }

            build("assemble") {
                assertTasksExecuted(":app:browserProductionWebpack")

                assertDirectoryInProjectExists("build/js/packages/kotlin-js-browser-base-js-ir")
                assertDirectoryInProjectExists("build/js/packages/kotlin-js-browser-base-js-legacy")
                assertDirectoryInProjectExists("build/js/packages/kotlin-js-browser-lib")
                assertDirectoryInProjectExists("build/js/packages/kotlin-js-browser-app")

                assertFileInProjectExists("app/build/distributions/app.js")

                if (!irBackend) {
                    assertTasksExecuted(":app:processDceKotlinJs")

                    assertDirectoryInProjectExists("build/js/packages/kotlin-js-browser-app/kotlin-dce")

                    assertFileInProjectExists("build/js/packages/kotlin-js-browser-app/kotlin-dce/kotlin.js")
                    assertFileInProjectExists("build/js/packages/kotlin-js-browser-app/kotlin-dce/kotlin-js-browser-app.js")
                    assertFileInProjectExists("build/js/packages/kotlin-js-browser-app/kotlin-dce/kotlin-js-browser-lib.js")
                    assertFileInProjectExists("build/js/packages/kotlin-js-browser-app/kotlin-dce/kotlin-js-browser-base-js-legacy.js")

                    assertFileInProjectExists("app/build/distributions/app.js.map")
                }
            }

            build("clean", "browserDistribution") {
                assertTasksExecuted(
                    ":app:processResources",
                    if (irBackend) ":app:browserProductionExecutableDistributeResources" else ":app:browserDistributeResources"
                )

                assertFileInProjectExists("app/build/distributions/index.html")
            }
        }
    }

    @DisplayName("dependencies are resolved to metadata")
    @GradleTest
    fun testResolveJsProjectDependencyToMetadata(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)
            gradleProperties.appendText("kotlin.js.compiler=both")

            val compiler = if (irBackend) "IR" else "LEGACY"

            val pathPrefix = "metadataDependency: "

            val appBuild = projectPath.resolve("app/build.gradle.kts")
            appBuild.modify {
                it.replace("target {", "js($compiler) {")
            }
            appBuild.appendText(
                """
                |
                |kotlin.sourceSets {
                |    val main by getting {
                |        dependencies {
                |            // add these dependencies to check that they are resolved to metadata
                |                api(project(":base"))
                |                implementation(project(":base"))
                |                compileOnly(project(":base"))
                |                runtimeOnly(project(":base"))
                |            }
                |        }
                |    }
                |
                |task("printMetadataFiles") {
                |    doFirst {
                |        listOf("api", "implementation", "compileOnly", "runtimeOnly").forEach { kind ->
                |            val configuration = configurations.getByName(kind + "DependenciesMetadata")
                |            configuration.files.forEach { println("$pathPrefix" + configuration.name + "->" + it.name) }
                |        }
                |    }
                |}
                """.trimMargin()
            )

            val metadataDependencyRegex = "$pathPrefix(.*?)->(.*)".toRegex()

            build("printMetadataFiles") {
                val suffix = if (irBackend) "ir" else "legacy"
                val ext = if (irBackend) "klib" else "jar"

                val expectedFileName = "base-$suffix.$ext"

                val paths = metadataDependencyRegex
                    .findAll(output).map { it.groupValues[1] to it.groupValues[2] }
                    .filter { (_, f) -> "base" in f }
                    .toSet()

                assertEquals(
                    listOf("api", "implementation", "compileOnly", "runtimeOnly").map {
                        "$it$METADATA_CONFIGURATION_NAME_SUFFIX" to expectedFileName
                    }.toSet(),
                    paths
                )
            }
        }
    }

    @DisplayName("no dependencies from other modules are declared")
    @GradleTest
    fun testNoUnintendedDevDependencies(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)

            build("browserProductionWebpack") {
                val appPackageJson = getSubprojectPackageJson(projectName = "kotlin-js-browser", subProject = "app")
                val libPackageJson = getSubprojectPackageJson(projectName = "kotlin-js-browser", subProject = "lib")

                assertTrue("${appPackageJson.name} should contain css-loader") {
                    "css-loader" in appPackageJson.devDependencies
                }
                assertFalse("${libPackageJson.name} shouldn't contain css-loader") {
                    "css-loader" in libPackageJson.devDependencies
                }
            }
        }
    }

    @DisplayName("yarn resolutions works")
    @GradleTest
    fun testYarnResolution(gradleVersion: GradleVersion) {
        project("kotlin-js-yarn-resolutions", gradleVersion) {
            build("packageJson", "rootPackageJson", "kotlinNpmInstall") {
                fun getPackageJson() =
                    projectPath.resolve("build/js")
                        .resolve(NpmProject.PACKAGE_JSON)
                        .let {
                            Gson().fromJson(it.readText(), PackageJson::class.java)
                        }

                val name = "lodash"
                val version = getPackageJson().resolutions?.get(name)
                val requiredVersion = ">=1.0.0 <1.2.1 || >1.4.0 <2.0.0"
                assertTrue("Root package.json must have resolution $name with version $requiredVersion, but $version found") {
                    version == requiredVersion
                }

                val react = "react"
                val reactVersion = getPackageJson().resolutions?.get(react)
                val requiredReactVersion = "16.0.0"
                assertTrue("Root package.json must have resolution $react with version $requiredReactVersion, but $reactVersion found") {
                    reactVersion == requiredReactVersion
                }
            }
        }
    }

    @DisplayName("directories in dependencies doesn't break NPM resolution")
    @GradleTest
    fun testDirectoryDependencyNotFailProjectResolution(gradleVersion: GradleVersion) {
        project("kotlin-js-nodejs-project", gradleVersion) {
            buildGradle.appendText(
                """
                |
                |dependencies {
                |     implementation(files("${"$"}{projectDir}/custom"))
                |     implementation(files("${"$"}{projectDir}/custom2"))
                |}
                """.trimMargin()
            )

            build("packageJson")
        }
    }

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

    @DisplayName("task configuration avoidance on browser project when help is requested")
    @GradleTest
    fun testBrowserNoTasksConfigurationOnHelp(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)
            buildGradleKts.appendText(
                """
                |
                |allprojects {
                |    tasks.configureEach {
                |        if (this is org.gradle.configuration.Help) return@configureEach
                |        throw GradleException("Task ${'$'}{path} shouldn't be configured")
                |    }
                |}
                """.trimMargin()
            )
            build("help")
        }
    }

    @DisplayName("task configuration avoidance on nodejs project when help is requested")
    @GradleTest
    fun testNodeJsNoTasksConfigurationOnHelp(gradleVersion: GradleVersion) {
        project("kotlin-js-nodejs-project", gradleVersion) {
            buildGradle.appendText(
                """
                |
                |allprojects {
                |    tasks.configureEach {
                |        if (it instanceof org.gradle.configuration.Help) return
                |        throw new GradleException("Task ${'$'}{path} shouldn't be configured")
                |    }
                |}
                """.trimMargin()
            )
            build("help")
        }
    }

    @DisplayName("webpack configuration is valid")
    @GradleTest
    fun testWebpackConfig(gradleVersion: GradleVersion) {
        project("kotlin-js-test-webpack-config", gradleVersion) {
            build("browserDevelopmentWebpack")

            build("checkConfigDevelopmentWebpack")

            build("checkConfigProductionWebpack")

            build("checkConfigDevelopmentRun")

            build("checkConfigProductionRun")
        }
    }

    private fun TestProject.getSubprojectPackageJson(subProject: String, projectName: String? = null) =
        projectPath.resolve("build/js/packages/${projectName ?: projectName}-$subProject")
            .resolve(NpmProject.PACKAGE_JSON)
            .let {
                Gson().fromJson(it.readText(), PackageJson::class.java)
            }
}

@JsGradlePluginTests
class GeneralKotlin2JsGradlePluginIT : KGPBaseTest() {
    // TODO: This test fails with deprecation error on Gradle <7.0
    // Should be fixed via planned fixes in Kotlin/JS plugin: https://youtrack.jetbrains.com/issue/KFC-252
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0)
    @DisplayName("js with both backends mode builds successfully")
    @GradleTest
    fun testJsBothModeWithTests(gradleVersion: GradleVersion) {
        project("kotlin-js-both-mode-with-tests", gradleVersion) {
            build("build") {
                assertNoBuildWarnings()
            }
        }
    }

    @DisplayName("nodejs up-to-date check works")
    @GradleTest
    fun testNodeJsAndYarnDownload(gradleVersion: GradleVersion) {
        project("cleanTask", gradleVersion) {
            build("checkDownloadedFolder")

            build("checkIfLastModifiedNotNow", "--rerun-tasks")
        }
    }

    @DisplayName("Disable download should not download Node.JS and Yarn")
    @GradleTest
    fun testNodeJsAndYarnNotDownloaded(gradleVersion: GradleVersion) {
        project("nodeJsDownload", gradleVersion) {
            buildGradleKts.modify {
                it + "\n" +
                        """
                        rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
                            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().nodeVersion = "unspecified"
                            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().download = false
                        }
                        rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
                            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().version = "unspecified"
                            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().download = false
                        }
                        """
            }
            build("kotlinNodeJsSetup", "kotlinYarnSetup")
        }
    }

    @DisplayName("Yarn.lock persistence")
    @GradleTest
    fun testYarnLockStore(gradleVersion: GradleVersion) {
        project("nodeJsDownload", gradleVersion) {
            build("assemble") {
                assertFileExists(projectPath.resolve("kotlin-js-store").resolve("yarn.lock"))
                assert(
                    projectPath
                        .resolve("kotlin-js-store")
                        .resolve("yarn.lock")
                        .readText() == projectPath.resolve("build/js/yarn.lock").readText()
                )
            }
        }
    }

    @DisplayName("Yarn ignore scripts")
    @GradleTest
    fun testYarnIgnoreScripts(gradleVersion: GradleVersion) {
        project("nodeJsDownload", gradleVersion) {
            buildGradleKts.modify {
                it + "\n" +
                        """
                        dependencies {
                            implementation(npm("puppeteer", "11.0.0"))
                        }
                        """.trimIndent()
            }
            build("assemble") {
                assert(
                    projectPath
                        .resolve("build")
                        .resolve("js")
                        .resolve("node_modules")
                        .resolve("puppeteer")
                        .resolve(".local-chromium")
                        .notExists()
                ) {
                    "Chromium should not be installed with --ignore-scripts"
                }
            }
            buildGradleKts.modify {
                it + "\n" +
                        """
                        rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
                            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().ignoreScripts = false
                        }
                        """.trimIndent()
            }

            build("clean")

            build("assemble") {
                assertDirectoryExists(
                    projectPath
                        .resolve("build")
                        .resolve("js")
                        .resolve("node_modules")
                        .resolve("puppeteer")
                        .resolve(".local-chromium")
                )
            }
        }
    }
}