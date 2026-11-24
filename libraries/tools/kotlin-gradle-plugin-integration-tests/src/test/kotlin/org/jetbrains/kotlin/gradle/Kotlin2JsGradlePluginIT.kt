/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle

import org.gradle.kotlin.dsl.kotlin
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.js.dsl.Distribution
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.fromSrcPackageJson
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.gradle.utils.normalizedAbsolutePath
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInBuildToolsApi
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInCommonBackend
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInJs
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@JsGradlePluginTests
@MustRunOnChangesInGradle
class Kotlin2JsIrGradlePluginIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @TestMetadata("kotlin-js-nodejs-project")
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
                |    inputs.property("foo", 1)
                |}
               """.trimMargin()
            )

            build("build") {
                assertFileInProjectNotExists("build/js/packages/kotlin-js-nodejs/kotlin/")
            }
        }
    }

    @TestMetadata("js-composite-build")
    @DisplayName("js composite build works")
    @GradleTest
    fun testJsCompositeBuild(gradleVersion: GradleVersion) {
        project(
            "js-composite-build",
            gradleVersion,
        ) {
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
                                            println(it.normalizedAbsolutePath().toString())
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

    @MustRunOnChangesInJs
    @MustRunOnChangesInBuildToolsApi
    @GradleTest
    @TestMetadata("kotlin-js-browser-project")
    fun testJsIrIncrementalInParallel(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
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
    @TestMetadata("kotlin-js-browser-project")
    fun testJsIrOnlyChangedFilesSynced(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {

            val filesModified: MutableMap<String, Long> = mutableMapOf()

            build("jsDevelopmentExecutableCompileSync") {
                assertTasksExecuted(":app:jsDevelopmentExecutableCompileSync")

                projectPath.resolve("build/js/packages/kotlin-js-browser-app")
                    .resolve("kotlin")
                    .toFile()
                    .walkTopDown()
                    .associateByTo(filesModified, { it.path }) {
                        it.lastModified()
                    }
            }

            projectPath.resolve("base/src/jsMain/kotlin/Base.kt").modify {
                it.replace("73", "37")
            }

            val fooTxt = projectPath.resolve("app/src/jsMain/resources/foo/foo.txt")
            fooTxt.parent.toFile().mkdirs()
            fooTxt.createFile().writeText("foo")

            build("jsDevelopmentExecutableCompileSync") {
                assertTasksExecuted(":app:jsDevelopmentExecutableCompileSync")

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

            build("jsDevelopmentExecutableCompileSync") {
                assertTasksExecuted(":app:jsDevelopmentExecutableCompileSync")

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


            build("jsDevelopmentExecutableCompileSync") {
                assertTasksExecuted(":app:jsDevelopmentExecutableCompileSync")

                assertFileInProjectNotExists("build/js/packages/kotlin-js-browser-app/kotlin/foo/foo.txt")
            }
        }
    }

    @TestMetadata("simple-js-executable")
    @DisplayName("falsify kotlin js compiler args")
    @GradleTest
    fun testFalsifyKotlinJsCompilerArgs(gradleVersion: GradleVersion) {
        project("simple-js-executable", gradleVersion) {
            buildGradleKts.appendText(
                """
                |
                |tasks.named<org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink>("compileProductionExecutableKotlinJs").configure {
                |    compilerOptions {
                |        freeCompilerArgs.add("-Xir-dce=false")
                |        freeCompilerArgs.add("-Xir-minimized-member-names=false")
                |    }
                |    
                |    doLast {
                |        compilerOptions {
                |            if (freeCompilerArgs.get().single { it.startsWith("-Xir-dce") } != "-Xir-dce=false") throw GradleException("fail1")
                |            if (
                |            freeCompilerArgs.get()
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

    @TestMetadata("kotlin-js-package-module-name")
    @MustRunOnChangesInJs
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

    @MustRunOnChangesInJs
    @DisplayName("K1/JS IR implementation dependency")
    @GradleTest
    @TestMetadata("kotlin-js-browser-project")
    fun testK1JsIrImplementationDependency(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {

            build("assemble")

            projectPath.resolve("app/src/jsMain/kotlin/App.kt").modify {
                it.replace("sheldon()", "best()")
            }

            buildAndFail("assemble")
        }
    }

    @MustRunOnChangesInJs
    @DisplayName("K2/JS IR implementation dependency")
    @GradleTest
    @TestMetadata("kotlin-js-browser-project")
    fun testK2JsIrImplementationDependency(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            build(":app:compileProductionExecutableKotlinJs")

            projectPath.resolve("app/src/jsMain/kotlin/App.kt").modify {
                it.replace("sheldon()", "best()")
            }

            buildAndFail(":app:compileProductionExecutableKotlinJs")
        }
    }

    @TestMetadata("kotlin-js-coroutines")
    @DisplayName("JS IR compiled against automatically added dom-api-compat")
    @GradleTest
    fun testJsIrCompiledAgainstAutomaticallyAddedDomApiCompat(gradleVersion: GradleVersion) {
        project("kotlin-js-coroutines", gradleVersion) {
            build("assemble")
            build("compileDevelopmentExecutableKotlinJs")
        }
    }

    @TestMetadata("kotlin-js-with-friend-paths")
    @DisplayName("Not existed friend dependencies does not affect compileKotlinJs UTD")
    @GradleTest
    fun testFriendDependenciesDoesNotExist(gradleVersion: GradleVersion) {
        project("kotlin-js-with-friend-paths", gradleVersion) {
            build("assemble")

            val oldFriendPaths = "friendPaths.from(project(\":lib\").buildDir.resolve(\"classes/kotlin/js/main\"))"
            val appModuleGradleBuildKts = subProject("app").buildGradleKts
            assertFileInProjectContains(appModuleGradleBuildKts.absolutePathString(), oldFriendPaths)
            appModuleGradleBuildKts.replaceText(
                oldFriendPaths,
                "friendPaths.from(project(\":lib\").buildDir.resolve(\"classes/kotlin/js/main\"), project(\":lib\").buildDir.resolve(\"libs/not_existed_lib.klib\"))"
            )

            build("assemble") {
                assertTasksUpToDate(":app:compileKotlinJs")
            }
        }
    }

    @TestMetadata("kotlin2JsIrICProject")
    @MustRunOnChangesInJs
    @MustRunOnChangesInCommonBackend
    @DisplayName("klib fingerprints")
    @GradleTest
    fun testKlibFingerprints(gradleVersion: GradleVersion) {
        project("kotlin2JsIrICProject", gradleVersion) {
            val fingerprints = Array(2) {
                build("compileDevelopmentExecutableKotlinJs")

                val manifestLines = projectPath.resolve("build/classes/kotlin/js/main/default/manifest").readLines()
                val serializedKlibFingerprint = manifestLines.singleOrNull { it.startsWith("serializedKlibFingerprint=") }
                assertNotNull(serializedKlibFingerprint, "can not find serializedKlibFingerprint")
                assertTrue("bad serializedKlibFingerprint format '$serializedKlibFingerprint'") {
                    Regex("serializedKlibFingerprint=[a-z0-9]+\\.[a-z0-9]+").matches(serializedKlibFingerprint)
                }

                val serializedIrFileFingerprints = manifestLines.singleOrNull { it.startsWith("serializedIrFileFingerprints=") }
                assertNotNull(serializedIrFileFingerprints, "can not find serializedIrFileFingerprints")
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

    @TestMetadata("kotlin-js-ir-runtime-dependency")
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

    @TestMetadata("kotlin-js-multiple-targets")
    @DisplayName("Multiple targets works without clash")
    @GradleTest
    fun testMultipleJsTargets(gradleVersion: GradleVersion) {
        project("kotlin-js-multiple-targets", gradleVersion) {
            build("assemble") {
                assertTasksExecuted(":compileProductionExecutableKotlinServerSide")
                assertTasksExecuted(":compileProductionExecutableKotlinClientSide")
            }
        }
    }

    @TestMetadata("js-custom-build-src-plugin")
    @DisplayName("Custom plugin applying Kotlin/JS plugin")
    @GradleTest
    fun customPluginApplyingKotlinJsPlugin(gradleVersion: GradleVersion) {
        project("js-custom-build-src-plugin", gradleVersion) {
            build("checkConfigurationsResolve") {
                assertTasksExecuted(":checkConfigurationsResolve")
            }
        }
    }

    @TestMetadata("kotlin2JsProjectWithTests")
    @DisplayName("test compilation can access main compilation")
    @GradleTest
    fun testCompileTestCouldAccessProduction(gradleVersion: GradleVersion) {
        project("kotlin2JsProjectWithTests", gradleVersion) {
            build("build") {
                assertTasksExecuted(
                    ":compileKotlinJs",
                    ":compileTestKotlinJs"
                )
                assertFileInProjectExists("build/classes/kotlin/js/main/default/manifest")

                assertFileInProjectExists("build/js/packages/kotlin2JsProjectWithTests-test/kotlin/kotlin2JsProjectWithTests-test.js")
            }
        }
    }

    @TestMetadata("kotlin2JsInternalTest")
    @DisplayName("test compilation can access internal symbols of main compilation")
    @GradleTest
    fun testCompilerTestAccessInternalProduction(gradleVersion: GradleVersion) {
        project("kotlin2JsInternalTest", gradleVersion) {
            build("compileTestDevelopmentExecutableKotlinJs")
        }
    }

    @DisplayName("smoke test of org.jetbrains.kotlin.js plugin")
    @GradleTest
    @TestMetadata("kotlin-js-plugin-project")
    fun testNewKotlinJsPlugin(gradleVersion: GradleVersion) {
        project("kotlin-js-plugin-project", gradleVersion) {
            build("publish", "assemble", "jsTest", "compileBenchmarkKotlinJs") {
                assertTasksExecuted(
                    ":compileKotlinJs", ":compileTestKotlinJs", ":compileBenchmarkKotlinJs"
                )

                val moduleDir = projectPath.resolve("build/repo/com/example/kotlin-js-plugin-js/1.0/")

                val kjsManifest = moduleDir.resolve("kotlin-js-plugin-js-1.0.klib")
                    .useAsZipFile { zipFile ->
                        zipFile.readKLibManifest()
                    }

                assertTrue("expect klib contains a non-empty manifest (all entries: ${kjsManifest})") {
                    kjsManifest.isNotEmpty()
                }

                val publishedPom = moduleDir.resolve("kotlin-js-plugin-js-1.0.pom")
                val pomText = publishedPom.readText().replace(Regex("\\s+"), "")
                assertTrue { "kotlinx-html-js</artifactId><version>0.7.5</version><scope>compile</scope>" in pomText }
                assertTrue { "kotlin-stdlib-js</artifactId><scope>runtime</scope>" in pomText }

                assertFileExists(moduleDir.resolve("kotlin-js-plugin-js-1.0-sources.jar"))

                assertTestResults(
                    projectPath.resolve(if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_9_3)) "tests.xml" else "gradle93-tests.xml"),
                    "jsNodeTest"
                )
            }
        }
    }

    @DisplayName("task configuration avoidance on browser project when help is requested")
    @GradleTest
    @TestMetadata("kotlin-js-browser-project")
    fun testBrowserNoTasksConfigurationOnHelp(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
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
    @TestMetadata("kotlin-js-nodejs-project")
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

    @MustRunOnChangesInJs
    @GradleTest
    @TestMetadata("kotlin-js-browser-project")
    fun testJsIrWholeProgram(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            gradleProperties.appendText(
                """
                |
                |kotlin.js.ir.output.granularity=whole-program
                """.trimMargin()
            )

            build("assemble")
        }
    }

    @TestMetadata("kotlin-js-compose-dependency")
    @MustRunOnChangesInJs
    @DisplayName("Cross modules work correctly with compose dependency ('KT60852')")
    @GradleTest
    fun crossModulesWorkCorrectlyWithComposeDependencyKT60852(gradleVersion: GradleVersion) {
        project("kotlin-js-compose-dependency", gradleVersion) {
            build("compileDevelopmentExecutableKotlinJs") {
                assertTasksExecuted(":compileDevelopmentExecutableKotlinJs")
            }
        }
    }

    @MustRunOnChangesInJs
    @DisplayName("Changed output module name")
    @GradleTest
    @TestMetadata("kotlin-js-browser-project")
    fun testChangedOutputModuleName(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            val moduleName = "hello"
            subProject("app").buildGradleKts.modify {
                it.replace(
                    "js {",
                    """
                        js {
                            outputModuleName.set("$moduleName")
                    """.trimIndent()
                )
            }

            build("assemble") {
                assertFileExists(
                    subProject("app").projectPath.resolve("build/compileSync/js/main/productionExecutable/kotlin/$moduleName.mjs")
                )
            }
        }
    }

    @MustRunOnChangesInJs
    @DisplayName("KT-37105: Project is properly rebuilt when @JsModule name is changed")
    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.MAX_SUPPORTED) // Gradle version is irrelevant
    fun testKt37105(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    js {
                        browser()
                        binaries.executable()
                    }
                    sourceSets.jsMain.configure {
                        it.dependencies {
                            implementation(npm("decamelize", "6.0.0"))
                        }
                    }
                }
            }
            val npmKt = kotlinSourcesDir("jsMain").resolve("npm.kt").apply {
                createParentDirectories()
                writeText(
                    //language=kt
                    """
                    |@file:JsModule("decamelize")
                    |@file:JsNonModule
                    |external fun decamelize(input: String): String
                    """.trimMargin()
                )
            }
            kotlinSourcesDir("jsMain").resolve("main.kt").apply {
                createParentDirectories()
                writeText(
                    //language=kt
                    """
                    |fun main() {
                    |    decamelize("test")
                    |}
                    """.trimMargin()
                )
            }

            fun getResultingJsFile(): String {
                assertFileInProjectExists("build/js/packages/empty/kotlin/empty.js")
                return projectPath.resolve("build/js/packages/empty/kotlin/empty.js").readText()
            }

            build(":compileProductionExecutableKotlinJs") {
                val jsFileContents = getResultingJsFile()
                assert("'decamelize'" in jsFileContents && "'decamelize-fake'" !in jsFileContents) {
                    "Expected only 'decamelize' to be in the resulting JS file. File contents:\n$jsFileContents"
                }
            }
            npmKt.modify {
                it.replace("JsModule(\"decamelize\")", "JsModule(\"decamelize-fake\")")
            }
            build(":compileProductionExecutableKotlinJs") {
                val jsFileContents = getResultingJsFile()
                assert("'decamelize'" !in jsFileContents && "'decamelize-fake'" in jsFileContents) {
                    "Expected only 'decamelize-fake' to be in the resulting JS file. File contents:\n$jsFileContents"
                }
            }
        }
    }
}
