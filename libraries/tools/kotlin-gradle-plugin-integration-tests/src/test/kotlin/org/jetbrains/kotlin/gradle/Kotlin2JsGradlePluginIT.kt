/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.google.gson.Gson
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.js.dsl.Distribution
import org.jetbrains.kotlin.gradle.targets.js.ir.KLIB_TYPE
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.PACKAGE_LOCK_MISMATCH_MESSAGE
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.RESTORE_PACKAGE_LOCK_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.STORE_PACKAGE_LOCK_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.UPGRADE_PACKAGE_LOCK
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.targets.js.npm.fromSrcPackageJson
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testbase.TestVersions.Gradle.G_7_6
import org.jetbrains.kotlin.gradle.util.replaceText
import org.junit.jupiter.api.DisplayName
import java.nio.file.Files
import java.util.zip.ZipFile
import kotlin.io.path.*
import kotlin.streams.toList
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@JsGradlePluginTests
class Kotlin2JsIrGradlePluginIT : KGPBaseTest() {


    @DisplayName("TS type declarations are generated")
    @GradleTest
    fun generateDts(gradleVersion: GradleVersion) {
        project("kotlin2JsIrDtsGeneration", gradleVersion) {
            build("build") {
                assertFileInProjectExists("build/js/packages/kotlin2JsIrDtsGeneration/kotlin/kotlin2JsIrDtsGeneration.js")
                val dts = projectPath.resolve("build/js/packages/kotlin2JsIrDtsGeneration/kotlin/kotlin2JsIrDtsGeneration.d.ts")
                assertFileExists(dts)
                assertFileContains(dts, "function bar(): string")
            }
        }
    }

    @DisplayName("nodejs main function arguments")
    @GradleTest
    fun testNodeJsMainArguments(gradleVersion: GradleVersion) {
        project("kotlin-js-nodejs-project", gradleVersion) {
            buildGradle.appendText(
                """
                |
                |tasks.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec).configureEach {
                |   args += ["test", "'Hello, World'"]
                |}
               """.trimMargin()
            )
            build("nodeRun") {
                assertOutputContains("ACCEPTED: test;'Hello, World'")
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
    @GradleTestVersions(minVersion = G_7_6)
    fun testJsCompositeBuild(gradleVersion: GradleVersion) {
        project("js-composite-build", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)

            subProject("lib").apply {
                buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)
            }

            subProject("base").apply {
                buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)
            }

            fun BuildResult.moduleVersion(rootModulePath: String, moduleName: String): String =
                projectPath.resolve(rootModulePath).toFile()
                    .resolve(NpmProject.PACKAGE_JSON)
                    .also {
                        if (!it.exists()) {
                            it
                                .parentFile // lib2
                                .parentFile // node_modules
                                .parentFile // js
                                .resolve(NpmProject.PACKAGE_JSON)
                                .let {
                                    println("root package.json:")
                                    println(it.readText())
                                }

                            it
                                .parentFile // lib2
                                .parentFile // node_modules
                                .parentFile // js
                                .resolve("packages_imported")
                                .also {
                                    println("ALL IMPORTED: ")
                                    it.listFiles()
                                        ?.forEach {
                                            println(it.absolutePath)
                                        }
                                    it.resolve(".visited-gradle").let {
                                        println("visited-gradle content")
                                        println(it.readText())
                                    }
                                }
                                .resolve("lib2")
                                .resolve("0.0.0-unspecified")
                                .resolve(NpmProject.PACKAGE_JSON)
                                .let {
                                    println("lib2 package.json state:")
                                    if (it.exists()) {
                                        println(it.readText())
                                    } else {
                                        println("lib2 package.json does not exists")
                                    }
                                }

                            printBuildOutput()
                        }
                    }
                    .let { fromSrcPackageJson(it) }
                    .let { it?.dependencies }
                    ?.getValue(moduleName)
                    ?: error("Not found package $moduleName in $rootModulePath")

            build("build") {
                val libDecamelizeVersion = moduleVersion("build/js/node_modules/lib-lib-2", "decamelize")
                assertEquals("1.1.1", libDecamelizeVersion)

                val libAsyncVersion = moduleVersion("build/js/node_modules/lib-lib-2", "async")
                assertEquals("2.6.2", libAsyncVersion)

                val appNodeFetchVersion = moduleVersion("build/js/node_modules/js-composite-build", "node-fetch")
                assertEquals("3.2.8", appNodeFetchVersion)
            }
        }
    }

    @GradleTest
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

    @DisplayName("Only changed files synced during JS IR build")
    @GradleTest
    @GradleTestVersions(minVersion = G_7_6)
    fun testJsIrOnlyChangedFilesSynced(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)

            val filesModified: MutableMap<String, Long> = mutableMapOf()

            build("compileDevelopmentExecutableKotlinJs") {
                assertTasksExecuted(":app:developmentExecutableCompileSync")

                projectPath.resolve("build/js/packages/kotlin-js-browser-app")
                    .resolve("kotlin")
                    .toFile()
                    .walkTopDown()
                    .associateByTo(filesModified, { it.path }) {
                        it.lastModified()
                    }
            }

            projectPath.resolve("base/src/main/kotlin/Base.kt").modify {
                it.replace("73", "37")
            }

            val fooTxt = projectPath.resolve("app/src/main/resources/foo/foo.txt")
            fooTxt.parent.toFile().mkdirs()
            fooTxt.createFile().writeText("foo")

            build("compileDevelopmentExecutableKotlinJs") {
                assertTasksExecuted(":app:developmentExecutableCompileSync")

                val modified = projectPath.resolve("build/js/packages/kotlin-js-browser-app")
                    .resolve("kotlin")
                    .toFile()
                    .walkTopDown()
                    .filter { it.isFile }
                    .filterNot { filesModified[it.path] == it.lastModified() }
                    .toSet()

                assertEquals(
                    setOf(
                        projectPath.resolve("build/js/packages/kotlin-js-browser-app/kotlin/kotlin-js-browser-base.mjs").toFile(),
                        projectPath.resolve("build/js/packages/kotlin-js-browser-app/kotlin/foo/foo.txt").toFile(),
                    ),
                    modified.toSet()
                )
            }

            projectPath.resolve("build/js/packages/kotlin-js-browser-app")
                .resolve("kotlin")
                .toFile()
                .walkTopDown()
                .associateByTo(filesModified, { it.path }) {
                    it.lastModified()
                }

            fooTxt.writeText("bar")

            build("compileDevelopmentExecutableKotlinJs") {
                assertTasksExecuted(":app:developmentExecutableCompileSync")

                val modified = projectPath.resolve("build/js/packages/kotlin-js-browser-app")
                    .resolve("kotlin")
                    .toFile()
                    .walkTopDown()
                    .filter { it.isFile }
                    .filterNot { filesModified[it.path] == it.lastModified() }
                    .toSet()

                assertEquals(
                    setOf(
                        projectPath.resolve("build/js/packages/kotlin-js-browser-app/kotlin/foo/foo.txt").toFile(),
                    ),
                    modified.toSet()
                )
            }

            fooTxt.deleteExisting()


            build("compileDevelopmentExecutableKotlinJs") {
                assertTasksExecuted(":app:developmentExecutableCompileSync")

                assertFileInProjectNotExists("build/js/packages/kotlin-js-browser-app/kotlin/foo/foo.txt")
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

    @DisplayName("klib compilation with the declarations name clash")
    @GradleTest
    fun testProjectWithExportedNamesClash(gradleVersion: GradleVersion) {
        project("kotlin-js-invalid-project-with-exported-clash", gradleVersion) {
            build("compileKotlinJs") {
                assertOutputContains("Exporting name 'best' in ES modules may clash")
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
                assertFileInProjectExists("build/${Distribution.DIST}/js/productionExecutable/kotlin-js-package-module-name.js")
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

    @DisplayName("K1/JS IR implementation dependency")
    @GradleTest
    fun testK1JsIrImplementationDependency(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)

            build("assemble")

            projectPath.resolve("app/src/main/kotlin/App.kt").modify {
                it.replace("sheldon()", "best()")
            }

            buildAndFail("assemble")
        }
    }

    @DisplayName("K2/JS IR implementation dependency")
    @GradleTest
    fun testK2JsIrImplementationDependency(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)
            buildGradleKts.append(
                """
                    rootProject.subprojects.forEach {
                        it.tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile> {
                            kotlinOptions.languageVersion = "2.0"
                        }
                    }
                """.trimIndent()
            )

            build(":app:compileProductionExecutableKotlinJs")

            projectPath.resolve("app/src/main/kotlin/App.kt").modify {
                it.replace("sheldon()", "best()")
            }

            buildAndFail(":app:compileProductionExecutableKotlinJs")
        }
    }

    @DisplayName("JS IR compiled against automatically added dom-api-compat")
    @GradleTest
    fun testJsIrCompiledAgainstAutomaticallyAddedDomApiCompat(gradleVersion: GradleVersion) {
        project("kotlin-js-coroutines", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)

            build("assemble")
            build("compileDevelopmentExecutableKotlinJs")
        }
    }

    @DisplayName("Not existed friend dependencies does not affect compileKotlinJs UTD")
    @GradleTest
    fun testFriendDependenciesDoesNotExist(gradleVersion: GradleVersion) {
        project("kotlin-js-with-friend-paths", gradleVersion) {
            build("assemble")

            val oldFriendPaths = "friendPaths.from(project(\":lib\").buildDir.resolve(\"libs/lib.klib\"))"
            val appModuleGradleBuildKts = subProject("app").buildGradleKts
            assertFileInProjectContains(appModuleGradleBuildKts.absolutePathString(), oldFriendPaths)
            appModuleGradleBuildKts.replaceText(
                oldFriendPaths,
                "friendPaths.from(project(\":lib\").buildDir.resolve(\"libs/lib.klib\"), project(\":lib\").buildDir.resolve(\"libs/not_existed_lib.klib\"))"
            )

            build("assemble") {
                assertTasksUpToDate(":app:compileKotlinJs")
            }
        }
    }

    @DisplayName("klib fingerprints")
    @GradleTest
    fun testKlibFingerprints(gradleVersion: GradleVersion) {
        project("kotlin2JsIrICProject", gradleVersion) {
            val fingerprints = Array(2) {
                build("compileDevelopmentExecutableKotlinJs")

                val manifestLines = projectPath.resolve("build/classes/kotlin/main/default/manifest").readLines()
                val serializedKlibFingerprint = manifestLines.singleOrNull { it.startsWith("serializedKlibFingerprint=") }
                assertNotNull(serializedKlibFingerprint) { "can not find serializedKlibFingerprint" }
                assertTrue("bad serializedKlibFingerprint format '$serializedKlibFingerprint'") {
                    Regex("serializedKlibFingerprint=[a-z0-9]+\\.[a-z0-9]+").matches(serializedKlibFingerprint)
                }

                val serializedIrFileFingerprints = manifestLines.singleOrNull { it.startsWith("serializedIrFileFingerprints=") }
                assertNotNull(serializedIrFileFingerprints) { "can not find serializedIrFileFingerprints" }
                assertTrue("bad serializedIrFileFingerprints format '$serializedIrFileFingerprints'") {
                    // kotlin2JsIrICProject has 2 files
                    Regex("serializedIrFileFingerprints=[a-z0-9]+\\.[a-z0-9]+ [a-z0-9]+\\.[a-z0-9]+").matches(serializedIrFileFingerprints)
                }

                build("clean")

                serializedKlibFingerprint to serializedIrFileFingerprints
            }

            assertEquals(fingerprints[0], fingerprints[1], "fingerprints must be stable")
        }
    }

    @DisplayName("Klib runtime dependency on module which already depends on dependent")
    @GradleTest
    fun testKlibRuntimeDependency(gradleVersion: GradleVersion) {
        project("kotlin-js-ir-runtime-dependency", gradleVersion) {
            build("assemble") {
                assertTasksExecuted(":lib:otherKlib")
                assertTasksExecuted(":lib:compileOtherKotlinJs")
                assertTasksExecuted(":app:compileProductionExecutableKotlinJs")
            }
        }
    }

    @DisplayName("Webpack works with ES modules")
    @GradleTest
    fun testWebpackWorksWithEsModules(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)

            subProject("app").buildGradleKts.modify { originalScript ->
                buildString {
                    append(originalScript)
                    append(
                        """
                        |
                        |tasks.named<org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink>("compileDevelopmentExecutableKotlinJs") {
                        |   kotlinOptions {
                        |       moduleKind = "es"
                        |   }
                        |}
                        |
                        |tasks.named<org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink>("compileProductionExecutableKotlinJs") {
                        |   kotlinOptions {
                        |       moduleKind = "es"
                        |   }
                        |}
                        |
                        """.trimMargin()
                    )
                }
            }

            build("browserDistribution") {
                assertTasksExecuted(":app:browserProductionWebpack")
                assertFileExists(subProject("app").projectPath.resolve("build/${Distribution.DIST}/js/productionExecutable/app.js"))
            }
        }
    }


    @DisplayName("package json contains correct extension for ES-modules")
    @GradleTest
    fun testPackageJsonWithEsModules(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)
            subProject("app").buildGradleKts.modify {
                it + """
                    |
                    |kotlin.target.useEsModules()
                    |
                """.trimMargin()
            }

            build(":app:packageJson") {
                val packageJson = projectPath
                    .resolve("build/js/packages/kotlin-js-browser-app")
                    .resolve(NpmProject.PACKAGE_JSON)
                    .let {
                        Gson().fromJson(it.readText(), PackageJson::class.java)
                    }

                assertEquals(packageJson.main, "kotlin/kotlin-js-browser-app.mjs")

            }
        }
    }

    @DisplayName("public package json contains correct extension for ES-modules")
    @GradleTest
    fun testPublicPackageJsonWithEsModules(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)
            subProject("app").buildGradleKts.modify {
                it + """
                    |
                    |kotlin.target.useEsModules()
                    |
                """.trimMargin()
            }

            build(":app:publicPackageJson") {
                val packageJson = subProject("app").projectPath
                    .resolve("build/tmp/publicPackageJson")
                    .resolve(NpmProject.PACKAGE_JSON)
                    .let {
                        Gson().fromJson(it.readText(), PackageJson::class.java)
                    }

                assertEquals(packageJson.main, "kotlin-js-browser-app.mjs")

            }
        }
    }

    @DisplayName("Multiple targets works without clash")
    @GradleTest
    fun testMultipleJsTargets(gradleVersion: GradleVersion) {
        project("kotlin-js-multiple-targets", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)

            build("assemble") {
                assertTasksExecuted(":compileProductionExecutableKotlinServerSide")
                assertTasksExecuted(":compileProductionExecutableKotlinClientSide")
            }
        }
    }

    @DisplayName("Webpack config block works after task configured")
    @GradleTest
    fun testWebpackConfigWorksAfterTaskConfigured(gradleVersion: GradleVersion) {
        project("js-library-with-executable", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)

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

            build("browserDistribution") {
                assertTasksExecuted(":browserProductionWebpack")
                assertFileExists(projectPath.resolve("build/${Distribution.DIST}/js/productionExecutable/CORRECT_NAME.js"))
            }
        }
    }

    @DisplayName("Custom plugin applying Kotlin/JS plugin")
    @GradleTest
    fun customPluginApplyingKotlinJsPlugin(gradleVersion: GradleVersion) {
        project("js-custom-build-src-plugin", gradleVersion) {
            build("checkConfigurationsResolve") {
                assertTasksExecuted(":checkConfigurationsResolve")
            }
        }
    }

    @DisplayName("test compilation can access main compilation")
    @GradleTest
    fun testCompileTestCouldAccessProduction(gradleVersion: GradleVersion) {
        project("kotlin2JsProjectWithTests", gradleVersion) {
            build("build") {
                assertTasksExecuted(
                    ":compileKotlinJs",
                    ":compileTestKotlinJs"
                )
                assertFileInProjectExists("build/classes/kotlin/main/default/manifest")

                assertFileInProjectExists("build/js/packages/kotlin2JsProjectWithTests-test/kotlin/kotlin2JsProjectWithTests-test.js")
            }
        }
    }

    @DisplayName("test compilation can access internal symbols of main compilation")
    @GradleTest
    fun testCompilerTestAccessInternalProduction(gradleVersion: GradleVersion) {
        project("kotlin2JsInternalTest", gradleVersion) {
            build("compileTestDevelopmentExecutableKotlinJs")
        }
    }

    @DisplayName("source map is generated")
    @GradleTest
    fun testKotlinJsSourceMap(gradleVersion: GradleVersion) {
        project("kotlin2JsProjectWithSourceMap", gradleVersion) {
            build("compileDevelopmentExecutableKotlinJs") {
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

    @DisplayName("path in source maps are remapped for custom outputFile")
    @GradleTest
    fun testKotlinJsSourceMapCustomOutputFile(gradleVersion: GradleVersion) {
        project("kotlin2JsProjectWithSourceMap", gradleVersion) {
            val taskSelector = "named<KotlinJsIrLink>(\"compileDevelopmentExecutableKotlinJs\")"
            buildGradleKts.appendText(
                """
                |project("app") {
                |   tasks.$taskSelector {
                |       destinationDirectory.set(file("${'$'}{buildDir}/kotlin2js"))
                |       kotlinOptions.moduleName = "app"
                |   }
                |}
                |
                """.trimMargin()
            )
            build("compileDevelopmentExecutableKotlinJs") {
                val mapFilePath = subProject("app").projectPath
                    .resolve("build/kotlin2js/app.js.map")
                assertFileContains(mapFilePath, "\"../../src/main/kotlin/main.kt\"")
                // The IR BE generates correct paths for dependencies
                assertFileContains(mapFilePath, "\"../../../lib/src/main/kotlin/foo.kt\"")
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
            build("compileDevelopmentExecutableKotlinJs") {
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
            build("compileDevelopmentExecutableKotlinJs") {
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
            build("compileDevelopmentExecutableKotlinJs") {
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

    @DisplayName("smoke test of org.jetbrains.kotlin.js plugin")
    @GradleTest
    fun testNewKotlinJsPlugin(gradleVersion: GradleVersion) {
        project("kotlin-js-plugin-project", gradleVersion) {
            build("publish", "assemble", "test", "compileBenchmarkKotlinJs") {
                assertTasksExecuted(
                    ":compileKotlinJs", ":compileTestKotlinJs", ":compileBenchmarkKotlinJs"
                )

                val moduleDir = projectPath.resolve("build/repo/com/example/kotlin-js-plugin/1.0/")

                val publishedJar = moduleDir.resolve("kotlin-js-plugin-1.0.klib")
                ZipFile(publishedJar.toFile()).use { zip ->
                    val entries = zip.entries().asSequence().map { it.name }
                    assertTrue { "default/manifest" in entries }
                }

                val publishedPom = moduleDir.resolve("kotlin-js-plugin-1.0.pom")
                val kotlinVersion = defaultBuildOptions.kotlinVersion
                val pomText = publishedPom.readText().replace(Regex("\\s+"), "")
                assertTrue { "kotlinx-html-js</artifactId><version>0.7.5</version><scope>compile</scope>" in pomText }
                assertTrue { "kotlin-stdlib-js</artifactId><version>$kotlinVersion</version><scope>runtime</scope>" in pomText }

                assertFileExists(moduleDir.resolve("kotlin-js-plugin-1.0-sources.jar"))

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
        project(
            "npm-dependencies",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                jsOptions = defaultBuildOptions.jsOptions?.copy(
                    yarn = false
                )
            )
        ) {
            build("jsJar") {
                val archive = projectPath
                    .resolve("build/libs")
                    .allFilesWithExtension(KLIB_TYPE)
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

            build(UPGRADE_PACKAGE_LOCK) {
                assertTasksExecuted(":$UPGRADE_PACKAGE_LOCK")
            }

            build("jsJar") {
                val archive = Files.list(projectPath.resolve("build").resolve("libs")).use { files ->
                    files
                        .filter { it.extension == KLIB_TYPE }
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

            build("compileProductionExecutableKotlinJs") {
                assertTasksExecuted(":app:compileProductionExecutableKotlinJs")
                assert(task(":kotlinNpmInstall") == null) {
                    printBuildOutput()
                    "NPM install should not be run"
                }
            }

            build("assemble") {
                assertTasksExecuted(":app:browserProductionWebpack")

                assertDirectoryInProjectExists("build/js/packages/kotlin-js-browser-base")
                assertDirectoryInProjectExists("build/js/packages/kotlin-js-browser-lib")
                assertDirectoryInProjectExists("build/js/packages/kotlin-js-browser-app")

                assertFileInProjectExists("app/build/${Distribution.DIST}/js/productionExecutable/app.js")
            }

            build("clean", "browserDistribution") {
                assertTasksExecuted(
                    ":app:processResources",
                    ":app:browserDistribution"
                )

                assertFileInProjectExists("app/build/${Distribution.DIST}/js/productionExecutable/index.html")
            }
        }
    }

    @DisplayName("package.json custom fields")
    @GradleTest
    fun testPackageJsonCustomField(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)

            build("rootPackageJson") {
                assertTasksExecuted(":app:packageJson")

                val jso = projectPath.resolve("build/js/packages/kotlin-js-browser-app")
                    .resolve(NpmProject.PACKAGE_JSON)
                    .let {
                        Gson().fromJson(it.readText(), JsonObject::class.java)
                    }

                assertEquals(1, jso.get("customField1").asJsonObject.get("one").asInt)
                assertEquals(2, jso.get("customField1").asJsonObject.get("two").asInt)
                assertEquals(JsonNull.INSTANCE, jso.get("customField2").asJsonNull)
                assertEquals(JsonNull.INSTANCE, jso.get("customField3").asJsonNull)
                assertEquals(JsonNull.INSTANCE, jso.get("customField4").asJsonObject.get("foo").asJsonNull)
                assertEquals("@as/main", jso.get("customField5").asString)
            }
        }
    }

    @DisplayName("NodeJs test with custom fork options")
    @GradleTest
    fun testNodeJsForkOptions(gradleVersion: GradleVersion) {
        project("kotlin-js-nodejs-custom-node-module", gradleVersion) {
            build("build") {
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

    @DisplayName("npm overrides works")
    @GradleTest
    fun testNpmOverrides(gradleVersion: GradleVersion) {
        project(
            "kotlin-js-npm-overrides",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                jsOptions = defaultBuildOptions.jsOptions?.copy(
                    yarn = false
                )
            )
        ) {
            build("packageJson", "rootPackageJson", "kotlinNpmInstall") {
                fun getPackageJson() =
                    projectPath.resolve("build/js")
                        .resolve(NpmProject.PACKAGE_JSON)
                        .let {
                            Gson().fromJson(it.readText(), PackageJson::class.java)
                        }

                val name = "lodash"
                val version = getPackageJson().overrides?.get(name)
                val requiredVersion = ">=1.0.0 <1.2.1 || >1.4.0 <2.0.0"
                assertTrue("Root package.json must override $name with version $requiredVersion, but $version found") {
                    version == requiredVersion
                }

                val react = "react"
                val reactVersion = getPackageJson().overrides?.get(react)
                val requiredReactVersion = "16.0.0"
                assertTrue("Root package.json must override $react with version $requiredReactVersion, but $reactVersion found") {
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
                assertOutputContains("Cannot find module 'foo'")
            }
        }
    }

    @DisplayName("mocha has no output during dry run")
    @GradleTest
    fun testMochaHasNoDryRunOutput(gradleVersion: GradleVersion) {
        project("kotlin-js-nodejs-project", gradleVersion) {
            build("nodeTest") {
                assertOutputDoesNotContain("0 passing")
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

    @DisplayName("smoothly fail npm install")
    @GradleTest
    fun testFailNpmInstall(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            buildGradleKts.modify { originalScript ->
                buildString {
                    append("import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNpmResolutionManager")
                    append(originalScript)
                    append(
                        """
                        |
                        |plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
                        |    val nodejs = the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>()
                        |    tasks.named<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>("kotlinNpmInstall") {
                        |        doFirst {
                        |            project.rootProject.kotlinNpmResolutionManager.get().state = 
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

    @DisplayName("test deprecated kotlin-js gradle plugin message reported")
    @GradleTest
    fun testDeprecatedMessageReported(gradleVersion: GradleVersion) {
        project("kotlin2JsInternalTest", gradleVersion) {
            build("help") { // just to trigger plugin registration
                assertOutputContains("w: 'kotlin-js' Gradle plugin is deprecated and will be removed in the future.")
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
        project(
            "nodeJsDownload",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                jsOptions = defaultBuildOptions.jsOptions?.copy(
                    yarn = true
                )
            )
        ) {
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

    @GradleTest
    fun testJsIrWholeProgram(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            buildGradleKts.modify(::transformBuildScriptWithPluginsDsl)
            gradleProperties.appendText(
                """
                |
                |kotlin.js.ir.output.granularity=whole-program
                """.trimMargin()
            )

            build("assemble")
        }
    }

    @DisplayName("Cross modules work correctly with compose dependency ('KT60852')")
    @GradleTest
    fun crossModulesWorkCorrectlyWithComposeDependencyKT60852(gradleVersion: GradleVersion) {
        project("kotlin-js-compose-dependency", gradleVersion) {
            build("compileDevelopmentExecutableKotlinJs") {
                assertTasksExecuted(":compileDevelopmentExecutableKotlinJs")
            }
        }
    }

    @DisplayName("test package.json skipped with main package.json was up-to-date")
    @GradleTest
    fun testPackageJsonSkippedWithUpToDatePackageJsons(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-library-project", gradleVersion) {
            subProject("app").buildGradleKts.modify {
                it + """
                    
                    tasks.named("testPackageJson") {
                        enabled = false
                    }

                    tasks.named("testPublicPackageJson") {
                        enabled = false
                    }
                """.trimIndent()
            }

            build("assemble") {
                assertTasksExecuted(":app:packageJson")
                assertTasksExecuted(":base:packageJson")
                assertTasksExecuted(":lib:packageJson")
            }

            build("check") {
                assertTasksUpToDate(":app:packageJson")
                assertTasksUpToDate(":base:packageJson")
                assertTasksUpToDate(":lib:packageJson")

                assertTasksSkipped(":app:testPackageJson")
                assertTasksExecuted(":lib:testPackageJson")
                assertTasksExecuted(":base:testPackageJson")
            }
        }
    }

    @DisplayName("test FAIL_ON_PROJECT_REPOS using custom repository")
    @GradleTest
    fun testFailOnProjectReposUsingCustomRepo(gradleVersion: GradleVersion) {
        project(
            "js-project-repos",
            gradleVersion,
            // we can remove this line, when the min version of Gradle be at least 8.1
            dependencyManagement = DependencyManagement.DisabledDependencyManagement
        ) {
            settingsGradleKts.modify {
                it + """
                    
                    dependencyResolutionManagement {
                        repositories {
                            ivy {
                                name = "Node.JS dist"
                                url = URI("https://nodejs.org/dist")
                                patternLayout {
                                    artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
                                }
                                metadataSources {
                                    artifact()
                                }
                                content {
                                    includeModule("org.nodejs", "node")
                                }
                            }
                            ivy {
                                name = "Yarn dist"
                                url = URI("https://github.com/yarnpkg/yarn/releases/download")
                                patternLayout {
                                    artifact("v[revision]/[artifact](-v[revision]).[ext]")
                                }
                                metadataSources {
                                    artifact()
                                }
                                content {
                                    includeModule("com.yarnpkg", "yarn")
                                }
                            }
                        }
                    }
                """.trimIndent()
            }

            build("kotlinNodeJsSetup", "kotlinYarnSetup") {
                assertTasksExecuted(":kotlinNodeJsSetup")
                assertTasksExecuted(":kotlinYarnSetup")
            }
        }
    }

    @DisplayName("test FAIL_ON_PROJECT_REPOS no download")
    @GradleTest
    fun testFailOnProjectReposNoDownload(gradleVersion: GradleVersion) {
        project(
            "js-project-repos",
            gradleVersion,
            // we can remove this line, when the min version of Gradle be at least 8.1
            dependencyManagement = DependencyManagement.DisabledDependencyManagement
        ) {
            buildGradleKts.modify {
                it + """
                    
                    rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
                        rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().download = false
                    }

                    rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
                        rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().download = false
                    }
                """.trimIndent()
            }

            build("kotlinNodeJsSetup", "kotlinYarnSetup") {
                assertTasksSkipped(":kotlinNodeJsSetup")
                assertTasksSkipped(":kotlinYarnSetup")
            }
        }
    }

    @DisplayName("Check that nodeTest run tests with commonjs module kind")
    @GradleTest
    fun testFailedJsTestWithCommonJs(gradleVersion: GradleVersion) {
        project("kotlin-js-project-failed-test", gradleVersion) {
            buildGradle.appendText(
                """
                |
                |kotlin {
                |   js {
                |       useCommonJs()
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
        }
    }

    @DisplayName("Check that nodeTest run tests with ESM module kind")
    @GradleTest
    fun testFailedJsTestWithESM(gradleVersion: GradleVersion) {
        project("kotlin-js-project-failed-test", gradleVersion) {
            buildGradle.appendText(
                """
                |
                |kotlin {
                |   js {
                |       useEsModules()
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
        }
    }

    @DisplayName("Check source map config of webpack")
    @GradleTest
    fun testWebpackSourceMapConfig(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            build("assemble") {
                assertTasksExecuted(":app:browserProductionWebpack")

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
}
