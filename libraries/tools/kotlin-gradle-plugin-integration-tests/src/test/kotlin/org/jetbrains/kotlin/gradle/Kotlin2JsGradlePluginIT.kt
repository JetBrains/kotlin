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
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockCopyTask.Companion.STORE_YARN_LOCK_NAME
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockCopyTask.Companion.UPGRADE_YARN_LOCK
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockCopyTask.Companion.YARN_LOCK_MISMATCH_MESSAGE
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
import kotlin.test.*

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

    @DisplayName("incremental compilation for JS IR consider multiple artifacts in one project")
    @GradleTest
    fun testJsIrIncrementalMultipleArtifacts(gradleVersion: GradleVersion) {
        project("kotlin-js-ir-ic-multiple-artifacts", gradleVersion) {
            build("compileDevelopmentExecutableKotlinJs") {
                val cacheDir = projectPath.resolve("app/build/klib/cache/").toFile()
                val cacheRootDirName = cacheDir.list()?.singleOrNull()
                assertTrue("Lib cache root dir should contain 1 element 'version.hash'") {
                    cacheRootDirName?.startsWith("version.") ?: false
                }
                val cacheRootDir = cacheDir.resolve(cacheRootDirName!!)
                val klibCacheDirs = cacheRootDir.list()
                // 2 for lib.klib + 1 for stdlib + 1 for dom-api + 1 for main
                assertEquals(5, klibCacheDirs?.size, "cache should contain 4 dirs")

                val libKlibCacheDirs = klibCacheDirs?.filter { dir -> dir.startsWith("lib.klib.") }
                assertEquals(2, libKlibCacheDirs?.size, "cache should contain 2 dirs for lib.klib")

                var lib = false
                var libOther = false

                cacheRootDir.listFiles()!!
                    .forEach {
                        it.listFiles()!!
                            .filter { it.isFile }
                            .forEach {
                                val text = it.readText()
                                // cache keeps the js code of compiled module, this substring from that js code
                                if (text.contains("root['kotlin-js-ir-ic-multiple-artifacts-lib']")) {
                                    if (lib) {
                                        error("lib should be only once in cache")
                                    }
                                    lib = true
                                }
                                // cache keeps the js code of compiled module, this substring from that js code
                                if (text.contains("root['kotlin-js-ir-ic-multiple-artifacts-lib-other']")) {
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

    @DisplayName("Remove unused dependency from klib")
    @GradleTest
    fun testJsIrIncrementalKlibRemoveUnusedDependency(gradleVersion: GradleVersion) {
        project("kotlin-js-ir-ic-remove-unused-dep", gradleVersion) {
            val appBuildGradleKts = subProject("app").buildGradleKts

            val buildGradleKtsWithoutDependency = appBuildGradleKts.readText()
            appBuildGradleKts.appendText(
                """
                |
                |dependencies {
                |    implementation(project(":lib"))
                |}
                |
                """.trimMargin()
            )

            build("compileDevelopmentExecutableKotlinJs") {
                assertTasksExecuted(":app:compileDevelopmentExecutableKotlinJs")
            }

            appBuildGradleKts.writeText(buildGradleKtsWithoutDependency)
            build("compileDevelopmentExecutableKotlinJs") {
                assertTasksExecuted(":app:compileDevelopmentExecutableKotlinJs")
            }
        }
    }

    @DisplayName("falsify kotlin js compiler args")
    @GradleTest
    fun testFalsifyKotlinJsCompilerArgs(gradleVersion: GradleVersion) {
        project("simple-js-executable", gradleVersion) {
            buildGradleKts.appendText(
                """
                |
                |tasks.named<org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink>("compileProductionExecutableKotlinJs").configure {
                |    kotlinOptions {
                |        freeCompilerArgs += "-Xir-dce=false"
                |        freeCompilerArgs += "-Xir-minimized-member-names=false"
                |    }
                |    
                |    doLast {
                |        kotlinOptions {
                |            if (freeCompilerArgs.single { it.startsWith("-Xir-dce") } != "-Xir-dce=false") throw GradleException("fail1")
                |            if (
                |            freeCompilerArgs
                |                .single { it.startsWith("-Xir-minimized-member-names") } != "-Xir-minimized-member-names=false"
                |            ) throw GradleException("fail2")
                |        }
                |    }
                |}
               """.trimMargin()
            )

            build("build") {
                assertFileInProjectNotExists("build/js/packages/kotlin-js-nodejs/kotlin/")
            }
        }
    }

    @DisplayName("generated typescript declarations validation")
    @GradleTest
    fun testGeneratedTypeScriptDeclarationsValidation(gradleVersion: GradleVersion) {
        project("js-ir-validate-ts", gradleVersion) {
            buildGradleKts.appendText(
                """
                |fun makeTypeScriptFileInvalid(mode: String) {
                |  val dts = projectDir.resolve("build/compileSync/js/main/" + mode + "Executable/kotlin/js-ir-validate-ts.d.ts")
                |  dts.appendText("\nlet invalidCode: unique symbol = Symbol()")
                |}
                |
                |tasks.named<org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink>("compileDevelopmentExecutableKotlinJs").configure {
                |   doLast { makeTypeScriptFileInvalid("development") }
                |}
                |
                |tasks.named<org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink>("compileProductionExecutableKotlinJs").configure {
                |   doLast { makeTypeScriptFileInvalid("production") }
                |}
               """.trimMargin()
            )

            buildAndFail("compileDevelopmentExecutableKotlinJs") {
                assertTasksFailed(":developmentExecutableValidateGeneratedByCompilerTypeScript")
                assertFileInProjectExists("build/js/packages/js-ir-validate-ts/kotlin/js-ir-validate-ts.js")
                assertFileInProjectExists("build/js/packages/js-ir-validate-ts/kotlin/js-ir-validate-ts.d.ts")
            }

            build("compileProductionExecutableKotlinJs") {
                assertTasksExecuted(":productionExecutableValidateGeneratedByCompilerTypeScript")
                assertFileInProjectExists("build/js/packages/js-ir-validate-ts/kotlin/js-ir-validate-ts.js")
                assertFileInProjectExists("build/js/packages/js-ir-validate-ts/kotlin/js-ir-validate-ts.d.ts")
            }
        }
    }

    @DisplayName("fully qualified names can be used in the sourcemap")
    @GradleTest
    fun testKotlinJsSourceMapGenerateFqNames(gradleVersion: GradleVersion) {
        project("kotlin2JsProjectWithSourceMap", gradleVersion) {
            buildGradleKts.appendText(
                """
                |allprojects {
                |   tasks.withType<KotlinJsCompile> {
                |        kotlinOptions.sourceMapNamesPolicy = "fully-qualified-names"
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

    @DisplayName("simple names can be used in the sourcemap")
    @GradleTest
    fun testKotlinJsSourceMapGenerateSimpleNames(gradleVersion: GradleVersion) {
        project("kotlin2JsProjectWithSourceMap", gradleVersion) {
            buildGradleKts.appendText(
                """
                |allprojects {
                |   tasks.withType<KotlinJsCompile> {
                |        kotlinOptions.sourceMapNamesPolicy = "simple-names"
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

    @DisplayName("don't generate names in the sourcemap")
    @GradleTest
    fun testKotlinJsSourceMapGenerateNoNames(gradleVersion: GradleVersion) {
        project("kotlin2JsProjectWithSourceMap", gradleVersion) {
            buildGradleKts.appendText(
                """
                |allprojects {
                |   tasks.withType<KotlinJsCompile> {
                |        kotlinOptions.sourceMapNamesPolicy = "no"
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

    @DisplayName("Kotlin/JS project with module name starting with package")
    @GradleTest
    fun testKotlinJsPackageModuleName(gradleVersion: GradleVersion) {
        project("kotlin-js-package-module-name", gradleVersion) {
            build("assemble") {
                assertFileInProjectExists("build/distributions/kotlin-js-package-module-name.js")
                assertFileInProjectExists("build/js/packages/@foo/bar/kotlin/@foo/bar.js")
            }
        }
    }

    @DisplayName("webpack must consider changes in dependencies in up-to-date")
    @GradleTest
    fun testWebpackConsiderChangesInDependencies(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)

            projectPath.resolve("app/src/main/kotlin/App.kt").modify {
                it.replace("require(\"css/main.css\")", "")
            }

            build("runWebpackResult") {
                assertOutputContains("Sheldon: 73")
            }

            projectPath.resolve("base/src/main/kotlin/Base.kt").modify {
                it.replace("73", "37")
            }

            build("runWebpackResult") {
                assertOutputContains("Sheldon: 37")
            }
        }
    }
}

@JsGradlePluginTests
class Kotlin2JsGradlePluginIT : AbstractKotlin2JsGradlePluginIT(false) {
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
                |
                |kotlin.js().browser {
                |    dceTask {
                |        dceOptions.devMode = true
                |    }
                |}
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
            build("build") {
                checkIrCompilationMessage()
            }
        }
    }

    @DisplayName("source map is generated")
    @GradleTest
    fun testKotlinJsSourceMap(gradleVersion: GradleVersion) {
        project("kotlin2JsProjectWithSourceMap", gradleVersion) {
            build(if (irBackend) "compileDevelopmentExecutableKotlinJs" else "compileKotlinJs") {
                if (irBackend) {
                    val appSourceMap = subProject("app").projectPath
                        .resolve("build/compileSync/js/main/developmentExecutable/kotlin/$projectName-app.js.map")
                    assertFileContains(
                        appSourceMap,
                        "\"../../../../../../src/main/kotlin/main.kt\"",
                        "\"../../../../../../../lib/src/main/kotlin/foo.kt\"",
                        "\"sourcesContent\":[null",
                    )

                    // The default should be generating simple names.
                    assertFileContains(appSourceMap, "somewhereOverTheRainbow")
                    assertFileDoesNotContain(appSourceMap, "\"names\":[]")
                    assertFileDoesNotContain(appSourceMap, "app.C.somewhereOverTheRainbow")

                    val libSourceMap = subProject("app").projectPath
                        .resolve("build/compileSync/js/main/developmentExecutable/kotlin/$projectName-lib.js.map")
                    assertFileContains(
                        libSourceMap,
                        "\"../../../../../../../lib/src/main/kotlin/foo.kt\"",
                        "\"sourcesContent\":[null",
                    )

                    // The default should be generating simple names.
                    assertFileDoesNotContain(libSourceMap, "\"names\":[]")

                    val libSourceMap2 = projectPath
                        .resolve("build/js/packages/$projectName-app/kotlin/$projectName-lib.js.map")
                    assertFileContains(
                        libSourceMap2,
                        "\"../../../../../lib/src/main/kotlin/foo.kt\"",
                        "\"sourcesContent\":[null",
                    )

                    // The default should be generating simple names.
                    assertFileDoesNotContain(libSourceMap2, "\"names\":[]")
                }
                assertFileContains(
                    projectPath
                        .resolve("build/js/packages/$projectName-app/kotlin/$projectName-app.js.map"),
                    "\"../../../../../app/src/main/kotlin/main.kt\"",
                    "\"../../../../../lib/src/main/kotlin/foo.kt\"",
                    "\"sourcesContent\":[null",
                )
            }
        }
    }

    @DisplayName("prefix is added to paths in source map")
    @DisabledIf(
        "org.jetbrains.kotlin.gradle.AbstractKotlin2JsGradlePluginIT#getIrBackend",
        disabledReason = "Source maps are not supported in IR backend"
    )
    @GradleTest
    fun testKotlinJsSourceMapCustomPrefix(gradleVersion: GradleVersion) {
        project("kotlin2JsProjectWithSourceMap", gradleVersion) {
            buildGradleKts.appendText(
                """
                |project("app") {
                |   tasks.withType<KotlinJsCompile> {
                |        kotlinOptions.sourceMapPrefix = "appPrefix/"
                |   }
                |}
                |
                |project("lib") {
                |   tasks.withType<KotlinJsCompile> {
                |        kotlinOptions.sourceMapPrefix = "libPrefix/"
                |   }
                |}
                |
                """.trimMargin()
            )
            build(if (irBackend) "compileDevelopmentExecutableKotlinJs" else "compileKotlinJs") {
                val mapFilePath = projectPath
                    .resolve("build/js/packages/$projectName-app/kotlin/$projectName-app.js.map")
                assertFileContains(
                    mapFilePath,
                    "\"appPrefix/src/main/kotlin/main.kt\"",
                    "\"appPrefix/libPrefix/src/main/kotlin/foo.kt\"",
                )
            }
        }
    }

    @DisplayName("path in source maps are remapped for custom outputFile")
    @GradleTest
    fun testKotlinJsSourceMapCustomOutputFile(gradleVersion: GradleVersion) {
        project("kotlin2JsProjectWithSourceMap", gradleVersion) {
            val taskSelector = if (irBackend)
                "named<KotlinJsIrLink>(\"compileDevelopmentExecutableKotlinJs\")"
            else
                "withType<KotlinJsCompile>"
            buildGradleKts.appendText(
                """
                |project("app") {
                |   tasks.$taskSelector {
                |        kotlinOptions.outputFile = "${'$'}{buildDir}/kotlin2js/app.js"
                |   }
                |}
                |
                """.trimMargin()
            )
            build(if (irBackend) "compileDevelopmentExecutableKotlinJs" else "compileKotlinJs") {
                val mapFilePath = subProject("app").projectPath
                    .resolve("build/kotlin2js/app.js.map")
                assertFileContains(mapFilePath, "\"../../src/main/kotlin/main.kt\"")
                if (irBackend) {
                    // The IR BE generates correct paths for dependencies
                    assertFileContains(mapFilePath, "\"../../../lib/src/main/kotlin/foo.kt\"")
                } else {
                    // The legacy BE doesn't.
                    assertFileContains(mapFilePath, "\"../../../../../lib/src/main/kotlin/foo.kt\"")
                }
            }
        }
    }

    @DisplayName("source map is not generated when disabled")
    @GradleTest
    fun testKotlinJsSourceMapDisabled(gradleVersion: GradleVersion) {
        project("kotlin2JsProjectWithSourceMap", gradleVersion) {
            buildGradleKts.appendText(
                """
                |allprojects {
                |   tasks.withType<KotlinJsCompile> {
                |        kotlinOptions.sourceMap = false
                |   }
                |}
                |
                """.trimMargin()
            )
            build(if (irBackend) "compileDevelopmentExecutableKotlinJs" else "compileKotlinJs") {
                val jsFilePath = projectPath.resolve("build/js/packages/$projectName-app/kotlin/$projectName-app.js")
                assertFileExists(jsFilePath)
                assertFileNotExists(Path("$jsFilePath.map"))
            }
        }
    }

    @DisplayName("sources of all modules are embedded into source map")
    @GradleTest
    fun testKotlinJsSourceMapEmbedSourcesAlways(gradleVersion: GradleVersion) {
        project("kotlin2JsProjectWithSourceMap", gradleVersion) {
            buildGradleKts.appendText(
                """
                |project("lib") {
                |   tasks.withType<KotlinJsCompile>() {
                |        kotlinOptions {
                |            sourceMap = true
                |            sourceMapEmbedSources = "always"
                |        }
                |    }
                |}
                |project("app") {
                |    tasks.withType<KotlinJsCompile> {
                |        kotlinOptions.sourceMapEmbedSources = "always"
                |    }
                |}
                |
                """.trimMargin()
            )
            build(if (irBackend) "compileDevelopmentExecutableKotlinJs" else "compileKotlinJs") {
                val mapFilePath = projectPath.resolve("build/js/packages/$projectName-app/kotlin/$projectName-app.js.map")
                assertFileContains(
                    mapFilePath,
                    "fun main(args: Array<String>) {",
                    "inline fun foo(): String {",
                )
            }
        }
    }

    @DisplayName("only sources of external modules are embedded into source map")
    @GradleTest
    fun testKotlinJsSourceMapEmbedSourcesInlining(gradleVersion: GradleVersion) {
        project("kotlin2JsProjectWithSourceMap", gradleVersion) {
            buildGradleKts.appendText(
                """
                |project("lib") {
                |   tasks.withType<KotlinJsCompile>() {
                |        kotlinOptions {
                |            sourceMap = true
                |            sourceMapEmbedSources = "always"
                |        }
                |    }
                |}
                |project("app") {
                |    tasks.withType<KotlinJsCompile> {
                |        kotlinOptions.sourceMapEmbedSources = "inlining"
                |    }
                |}
                |
                """.trimMargin()
            )
            build(if (irBackend) "compileDevelopmentExecutableKotlinJs" else "compileKotlinJs") {
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

    @DisplayName("incremental compilation for js works")
    @GradleTest
    fun testIncrementalCompilation(gradleVersion: GradleVersion) {
        val buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        project("kotlin2JsICProject", gradleVersion, buildOptions = buildOptions) {
            build("compileKotlinJs", "compileTestKotlinJs") {
                checkIrCompilationMessage()
                assertOutputContains(USING_JS_INCREMENTAL_COMPILATION_MESSAGE)
                assertCompiledKotlinSources(projectPath.allKotlinFiles.relativizeTo(projectPath), output)
            }

            build("compileKotlinJs", "compileTestKotlinJs") {
                assertCompiledKotlinSources(emptyList(), output)
            }

            val modifiedFile = subProject("lib").kotlinSourcesDir().resolve("A.kt") ?: error("No A.kt file in test project")
            modifiedFile.modify {
                it.replace("val x = 0", "val x = \"a\"")
            }
            build("compileKotlinJs", "compileTestKotlinJs") {
                checkIrCompilationMessage()
                assertOutputContains(USING_JS_INCREMENTAL_COMPILATION_MESSAGE)
                val affectedFiles = listOf("A.kt", "useAInLibMain.kt", "useAInAppMain.kt", "useAInAppTest.kt").mapNotNull {
                    projectPath.findInPath(it)
                }
                assertCompiledKotlinSources(affectedFiles.relativizeTo(projectPath), output)
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
            build("compileKotlinJs", "compileTestKotlinJs") {
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
            build("publish", "processDceKotlinJs", "test", "processDceBenchmarkKotlinJs") {
                assertTasksExecuted(
                    ":compileKotlinJs", ":compileTestKotlinJs", ":compileBenchmarkKotlinJs",
                    ":processDceKotlinJs", ":processDceBenchmarkKotlinJs"
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
                assertTrue { "kotlinx-html-js</artifactId><version>0.7.5</version><scope>compile</scope>" in pomText }
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

                assertTestResults(projectPath.resolve("tests.xml"), "nodeTest")
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

            build(UPGRADE_YARN_LOCK) {
                assertTasksExecuted(":$UPGRADE_YARN_LOCK")
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

            if (irBackend) {
                build("compileProductionExecutableKotlinJs") {
                    assertTasksExecuted(":app:compileProductionExecutableKotlinJs")
                    assert(task(":kotlinNpmInstall") == null) {
                        printBuildOutput()
                        "NPM install should not be run"
                    }
                }
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

    @DisplayName("kotlin/js compiler warning")
    @GradleTest
    fun testKotlinJsCompilerWarn(gradleVersion: GradleVersion) {
        project(
            "kotlin-js-compiler-warn",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(jsOptions = defaultJsOptions.copy(compileNoWarn = false, jsCompilerType = null))
        ) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)

            build("assemble") {
                assertOutputDoesNotContain("This project currently uses the Kotlin/JS Legacy")
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
                |            }
                |        }
                |    }
                |
                |task("printMetadataFiles") {
                |    doFirst {
                |        listOf("api", "implementation", "compileOnly").forEach { kind ->
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
                    listOf("api", "implementation", "compileOnly").map {
                        "$it$METADATA_CONFIGURATION_NAME_SUFFIX" to expectedFileName
                    }.toSet(),
                    paths
                )
            }
        }
    }

    @DisplayName("NodeJs test with custom fork options")
    @GradleTest
    fun testNodeJsForkOptions(gradleVersion: GradleVersion) {
        project("kotlin-js-nodejs-custom-node-module", gradleVersion) {
            build("build") {
                checkIrCompilationMessage()

                // It makes sense only since Tests will be run on Gradle 7.2
                assertOutputDoesNotContain("Execution optimizations have been disabled for task ':nodeTest'")

                assertTasksExecuted(":nodeTest")
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
                |        if (name == org.gradle.language.base.plugins.LifecycleBasePlugin.CLEAN_TASK_NAME ||
                |            this is org.gradle.configuration.Help) return@configureEach
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
                |        if (it.name == org.gradle.language.base.plugins.LifecycleBasePlugin.CLEAN_TASK_NAME ||
                |            it instanceof org.gradle.configuration.Help) return
                |        throw new GradleException("Task ${'$'}{path} shouldn't be configured")
                |    }
                |}
                """.trimMargin()
            )
            build("help")
        }
    }

    @DisplayName("mocha fails on module not found")
    @GradleTest
    fun testMochaFailedModuleNotFound(gradleVersion: GradleVersion) {
        project("kotlin-js-nodejs-project", gradleVersion) {
            build("nodeTest") {
                assertOutputDoesNotContain("##teamcity[")
            }

            projectPath.resolve("src/test/kotlin/Tests.kt").appendText(
                "\n" + """
                |class Tests3 {
                |   @Test
                |   fun testHello() {
                |       throw IllegalArgumentException("foo")
                |   }
                |}
                """.trimMargin()
            )
            buildAndFail("nodeTest") {
                assertTasksFailed(":nodeTest")

                assertTestResults(
                    projectPath.resolve("TEST-all.xml"),
                    "nodeTest"
                )
            }

            projectPath.resolve("src/test/kotlin/Tests.kt").appendText(
                "\n" + """
                |
                |@JsModule("foo")
                |@JsNonModule
                |external val foo: dynamic
                |
                |class Tests2 {
                |   @Test
                |   fun testHello() {
                |       foo
                |   }
                |}
                """.trimMargin()
            )
            buildAndFail("nodeTest") {
                assertTasksFailed(":nodeTest")
            }
        }
    }

    @DisplayName("Failing with yarn.lock update")
    @GradleTest
    fun testFailingWithYarnLockUpdate(gradleVersion: GradleVersion) {
        project("kotlin-js-yarn-lock-project", gradleVersion) {
            build(STORE_YARN_LOCK_NAME) {
                assertTasksExecuted(":$STORE_YARN_LOCK_NAME")
            }

            projectPath.resolve("kotlin-js-store").deleteRecursively()

            buildGradleKts.modify {
                it + "\n" +
                        """
                        rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin::class.java) {
                            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().reportNewYarnLock = true
                        }
                        """.trimIndent()
            }

            buildAndFail(STORE_YARN_LOCK_NAME) {
                assertTasksFailed(":$STORE_YARN_LOCK_NAME")
            }

            buildGradleKts.modify {
                it + "\n" +
                        """
                        dependencies {
                            implementation(npm("decamelize", "6.0.0"))
                        }
                            
                        rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin::class.java) {
                            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().yarnLockMismatchReport = 
                                org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport.FAIL
                        }
                        """.trimIndent()
            }

            buildAndFail(STORE_YARN_LOCK_NAME) {
                assertTasksFailed(":$STORE_YARN_LOCK_NAME")
            }

            // yarn.lock was not updated
            buildAndFail(STORE_YARN_LOCK_NAME) {
                assertTasksFailed(":$STORE_YARN_LOCK_NAME")
            }

            build(UPGRADE_YARN_LOCK) {
                assertTasksExecuted(":$UPGRADE_YARN_LOCK")
            }

            buildGradleKts.modify {
                val replaced = it.replace("implementation(npm(\"decamelize\", \"6.0.0\"))", "")
                replaced + "\n" +
                        """
                        rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin::class.java) {
                            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().yarnLockMismatchReport =
                                org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport.WARNING
                        }
                        """.trimIndent()
            }

            build(STORE_YARN_LOCK_NAME) {
                assertTasksExecuted(":$STORE_YARN_LOCK_NAME")

                assertOutputContains(YARN_LOCK_MISMATCH_MESSAGE)
            }

            build(UPGRADE_YARN_LOCK) {
                assertTasksExecuted(":$UPGRADE_YARN_LOCK")
            }

            buildGradleKts.modify {
                it + "\n" +
                        """
                        dependencies {
                            implementation(npm("decamelize", "6.0.0"))
                        }
                            
                        rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin::class.java) {
                            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().yarnLockMismatchReport =
                                org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport.NONE
                        }
                        """.trimIndent()
            }

            build(STORE_YARN_LOCK_NAME) {
                assertTasksExecuted(":$STORE_YARN_LOCK_NAME")

                assertOutputDoesNotContain(YARN_LOCK_MISMATCH_MESSAGE)
            }

            build(UPGRADE_YARN_LOCK) {
                assertTasksExecuted(":$UPGRADE_YARN_LOCK")
            }

            buildGradleKts.modify {
                it.replace("implementation(npm(\"decamelize\", \"6.0.0\"))", "")
            }

            projectPath.resolve("kotlin-js-store").deleteRecursively()

            buildGradleKts.modify {
                it + "\n" +
                        """
                        rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin::class.java) {
                            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().yarnLockMismatchReport =
                                org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport.NONE
                            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().reportNewYarnLock =
                                true
                        }
                        """.trimIndent()
            }

            build(STORE_YARN_LOCK_NAME) {
                assertTasksExecuted(":$STORE_YARN_LOCK_NAME")

                assertOutputDoesNotContain(YARN_LOCK_MISMATCH_MESSAGE)
            }

            buildGradleKts.modify {
                it + "\n" +
                        """
                        dependencies {
                            implementation(npm("decamelize", "6.0.0"))
                        }
                            
                        rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin::class.java) {
                            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().yarnLockMismatchReport =
                                org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport.FAIL
                            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().yarnLockAutoReplace = 
                                true
                        }
                        """.trimIndent()
            }

            buildAndFail(STORE_YARN_LOCK_NAME) {
                assertTasksFailed(":$STORE_YARN_LOCK_NAME")
            }

            //yarn.lock was updated
            build(STORE_YARN_LOCK_NAME) {
                assertTasksExecuted(":$STORE_YARN_LOCK_NAME")
            }

            buildGradleKts.modify {
                it.replace("implementation(npm(\"decamelize\", \"6.0.0\"))", "")
            }

            // check if everything ok without build/js/yarn.lock
            build("clean") {
                assertTasksExecuted(":clean")
            }

            build("clean") {
                assertTasksUpToDate(":clean")
            }

            buildAndFail(STORE_YARN_LOCK_NAME) {
                assertTasksFailed(":$STORE_YARN_LOCK_NAME")
            }

            projectPath.resolve("kotlin-js-store").deleteRecursively()

            //check if independent tasks can be executed
            build("help") {
                assertTasksExecuted(":help")
            }
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

    @DisplayName("incremental compilation with multiple js modules after compilation error works")
    @GradleTest
    fun testIncrementalCompilationWithMultipleModulesAfterCompilationError(gradleVersion: GradleVersion) {
        project("kotlin-js-ir-ic-multiple-artifacts", gradleVersion) {
            build("compileKotlinJs")

            val libKt = subProject("lib").kotlinSourcesDir().resolve("Lib.kt") ?: error("No Lib.kt file in test project")
            val appKt = subProject("app").kotlinSourcesDir().resolve("App.kt") ?: error("No App.kt file in test project")

            libKt.modify { it.replace("fun answer", "func answe") } // introduce compilation error
            buildAndFail("compileKotlinJs")
            libKt.modify { it.replace("func answe", "fun answer") } // revert compilation error
            appKt.modify { it.replace("Sheldon:", "Sheldon :") } // some change for incremental compilation
            build("compileKotlinJs", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertOutputContains(USING_JS_INCREMENTAL_COMPILATION_MESSAGE)
                assertTasksUpToDate(":lib:compileKotlinJs")
                assertTasksExecuted(":app:compileKotlinJs")
                assertCompiledKotlinSources(listOf(appKt).relativizeTo(projectPath), output)
            }
        }
    }

    @DisplayName("smoothly fail npm install")
    @GradleTest
    fun testFailNpmInstall(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            buildGradleKts.modify { originalScript ->
                buildString {
                    append(originalScript)
                    append(
                        """
                        |
                        |plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
                        |    val nodejs = the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>()
                        |    tasks.named<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>("kotlinNpmInstall") {
                        |        doFirst {
                        |            nodejs.npmResolutionManager.state = 
                        |            org.jetbrains.kotlin.gradle.targets.js.npm.KotlinNpmResolutionManager.ResolutionState.Error(GradleException("someSpecialException"))
                        |        }
                        |    }
                        |}
                        |
                        """.trimMargin()
                    )
                }
            }
            buildAndFail("build") {
                assertTasksFailed(":kotlinNpmInstall")
                assertOutputContains("someSpecialException")
            }
        }
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
        project(
            "cleanTask",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                // bug in Gradle: https://github.com/gradle/gradle/issues/15796
                warningMode = if (gradleVersion < GradleVersion.version("7.0")) WarningMode.None else defaultBuildOptions.warningMode
            )
        ) {
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
            build("assemble", "kotlinStoreYarnLock") {
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
            build("assemble", "kotlinNpmInstall") {
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

            build("assemble", "kotlinNpmInstall") {
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
