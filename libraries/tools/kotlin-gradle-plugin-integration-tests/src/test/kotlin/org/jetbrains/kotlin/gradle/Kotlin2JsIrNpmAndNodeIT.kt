/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle

import com.google.gson.Gson
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.js.ir.KLIB_TYPE
import org.jetbrains.kotlin.gradle.targets.js.nodejs.JsPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask.Companion.UPGRADE_PACKAGE_LOCK_BASE_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import org.junit.jupiter.api.DisplayName
import java.util.zip.ZipFile
import kotlin.io.path.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Kotlin/JS IR: npm/Yarn tooling, package.json, Node.js run and Mocha tests.
 */
@JsGradlePluginTests
@MustRunOnChangesInGradle
class Kotlin2JsIrNpmAndNodeIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @TestMetadata("kotlin-js-nodejs-project")
    @DisplayName("nodejs CLI arguments as main function arguments")
    @GradleTest
    fun testNodeJsMainArguments(gradleVersion: GradleVersion) {
        project("kotlin-js-nodejs-project", gradleVersion) {
            buildGradle.appendText(
                """
                |
                | kotlin {
                |    js {
                |        nodejs {
                |            passCliArgumentsToMainFunction()
                |        }
                |    }
                | }
                |
                |tasks.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec).configureEach {
                |    args += ["test", "'Hello, World'"]
                |}
               """.trimMargin()
            )
            build("jsNodeDevelopmentRun") {
                assertOutputContains("ACCEPTED: test;'Hello, World'")
            }
        }
    }

    @TestMetadata("kotlin-js-nodejs-project")
    @DisplayName("nodejs process.argv as main function arguments")
    @GradleTest
    fun testPassingNodeJsProcessArgvToTheMainFunction(gradleVersion: GradleVersion) {
        project("kotlin-js-nodejs-project", gradleVersion) {
            buildGradle.appendText(
                """
                |
                | kotlin {
                |    js {
                |        nodejs {
                |            passProcessArgvToMainFunction()
                |        }
                |    }
                | }
                |
                |tasks.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec).configureEach {
                |    args += ["test", "'Hello, World'"]
                |}
               """.trimMargin()
            )
            build("jsNodeDevelopmentRun") {
                assertOutputContains("kotlin-js-nodejs-project.js;test;'Hello, World'")
            }
        }
    }

    @DisplayName("package json contains correct extension for ES-modules")
    @GradleTest
    @TestMetadata("kotlin-js-browser-project")
    fun testPackageJsonWithEsModules(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            subProject("app").buildGradleKts.modify {
                it + """
                    |
                    |kotlin.js().useEsModules()
                    |
                """.trimMargin()
            }

            build(":app:jsPackageJson") {
                val packageJson = projectPath
                    .resolve("build/js/packages/kotlin-js-browser-app")
                    .resolve(NpmProject.PACKAGE_JSON)
                    .let {
                        Gson().fromJson(it.readText(), PackageJson::class.java)
                    }

                assertEquals("kotlin/kotlin-js-browser-app.mjs", packageJson.main)

            }
        }
    }

    @DisplayName("public package json contains correct extension for ES-modules")
    @GradleTest
    @TestMetadata("kotlin-js-browser-project")
    fun testPublicPackageJsonWithEsModules(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            subProject("app").buildGradleKts.modify {
                it + """
                    |
                    |kotlin.js().useEsModules()
                    |
                """.trimMargin()
            }

            build(":app:jsPublicPackageJson") {
                val packageJson = subProject("app").projectPath
                    .resolve("build/tmp/jsPublicPackageJson")
                    .resolve(NpmProject.PACKAGE_JSON)
                    .let {
                        Gson().fromJson(it.readText(), PackageJson::class.java)
                    }

                assertEquals(packageJson.main, "kotlin-js-browser-app.mjs")

            }
        }
    }

    @DisplayName("yarn is set up")
    @GradleTest
    @TestMetadata("yarn-setup")
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
                |the<YarnRootEnvSpec>().version.set("1.9.3")
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

    @TestMetadata("yarn-setup")
    @DisplayName("yarn is set up from local archive")
    @GradleTest
    fun testYarnSetupFromLocalArchive(gradleVersion: GradleVersion) {
        project("yarn-setup", gradleVersion) {
            build("yarnFolderRemove")

            buildGradleKts.appendText(
                """
                |
                |rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
                |   the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec>().version.set("1.22.22")
                |   the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec>().downloadBaseUrl.set(projectDir.toURI().toString())
                |}
                |
                """.trimMargin()
            )

            build("kotlinYarnSetup")
        }
    }

    @DisplayName("NPM dependencies are installed")
    @GradleTest
    @TestMetadata("npm-dependencies")
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
    @TestMetadata("npm-dependencies")
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

            val upgradePackageLock = JsPlatformDisambiguator.extensionName(UPGRADE_PACKAGE_LOCK_BASE_NAME)
            build(upgradePackageLock) {
                assertTasksExecuted(":$upgradePackageLock")
            }

            build("jsJar") {
                val klib = projectPath.resolve("build/libs").walk()
                    .single { it.extension == KLIB_TYPE }

                val packageJsonCandidates = klib.useAsZipFile { zipFile ->
                    zipFile.entries()
                        .asSequence()
                        .filter { it.name == NpmProject.PACKAGE_JSON }
                        .toList()
                }

                assertTrue("Expected absence of package.json in ${klib.name}. Actual:${packageJsonCandidates}") {
                    packageJsonCandidates.isEmpty()
                }
            }
        }
    }

    @DisplayName("package.json custom fields")
    @GradleTest
    @TestMetadata("kotlin-js-browser-project")
    fun testPackageJsonCustomField(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {

            build("rootPackageJson") {
                assertTasksExecuted(":app:jsPackageJson")

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

    @TestMetadata("kotlin-js-nodejs-custom-node-module")
    @DisplayName("NodeJs test with custom fork options")
    @GradleTest
    fun testNodeJsForkOptions(gradleVersion: GradleVersion) {
        project("kotlin-js-nodejs-custom-node-module", gradleVersion) {
            build("build") {
                assertOutputDoesNotContain("Execution optimizations have been disabled for task ':jsNodeTest'")

                assertTasksExecuted(":jsNodeTest")
            }
        }
    }

    @DisplayName("no dependencies from other modules are declared")
    @GradleTest
    @TestMetadata("kotlin-js-browser-project")
    fun testNoUnintendedDevDependencies(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {

            build("jsBrowserProductionWebpack") {
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

    @TestMetadata("kotlin-js-npm-overrides")
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
            build("jsPackageJson", "rootPackageJson", "kotlinNpmInstall") {
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

    @TestMetadata("kotlin-js-nodejs-project")
    @DisplayName("directories in dependencies doesn't break NPM resolution")
    @GradleTest
    fun testDirectoryDependencyNotFailProjectResolution(gradleVersion: GradleVersion) {
        project("kotlin-js-nodejs-project", gradleVersion) {
            buildScriptInjection {
                project.applyMultiplatform {
                    sourceSets.jsMain.dependencies {
                        implementation(project.files("${project.projectDir}/custom"))
                        implementation(project.files("${project.projectDir}/custom2"))
                    }
                }
            }

            build("jsPackageJson")
        }
    }

    @TestMetadata("kotlin-js-nodejs-project")
    @DisplayName("mocha fails on module not found")
    @GradleTest
    fun testMochaFailedModuleNotFound(gradleVersion: GradleVersion) {
        project("kotlin-js-nodejs-project", gradleVersion) {
            build("jsNodeTest") {
                assertOutputDoesNotContain("##teamcity[")
            }

            projectPath.resolve("src/jsTest/kotlin/Tests.kt").appendText(
                "\n" + """
                |class Tests3 {
                |   @Test
                |   fun testHello() {
                |       throw IllegalArgumentException("foo")
                |   }
                |}
                """.trimMargin()
            )
            buildAndFail("jsNodeTest") {
                assertTasksFailed(":jsNodeTest")

                assertTestResults(
                    projectPath.resolve(
                        if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_9_3)) {
                            "TEST-all.xml"
                        } else {
                            "Gradle93-TEST-all.xml"
                        }
                    ),
                    "jsNodeTest"
                )
            }

            projectPath.resolve("src/jsTest/kotlin/Tests.kt").appendText(
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
            buildAndFail("jsNodeTest") {
                assertTasksFailed(":jsNodeTest")
                assertOutputContains("Cannot find module 'foo'")
            }
        }
    }

    @TestMetadata("kotlin-js-nodejs-project")
    @DisplayName("mocha has no output during dry run")
    @GradleTest
    fun testMochaHasNoDryRunOutput(gradleVersion: GradleVersion) {
        project("kotlin-js-nodejs-project", gradleVersion) {
            build("jsNodeTest") {
                assertOutputDoesNotContain("0 passing")
            }
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
    @TestMetadata("kotlin-js-browser-project")
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
                        |    val npmResolutionManager = project.rootProject.kotlinNpmResolutionManager
                        |    tasks.named<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>("kotlinNpmInstall") {
                        |        doFirst {
                        |            npmResolutionManager.get().state = 
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

    @TestMetadata("nodeJsDownload")
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
                            the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().version.set("unspecified")
                            the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().download.set(false)
                        }
                        rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
                            the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec>().version.set("unspecified")
                            the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec>().download.set(false)
                        }
                        """
            }
            build("kotlinNodeJsSetup", "kotlinYarnSetup")
        }
    }

    @TestMetadata("kotlin-js-dependency-with-js-files")
    @DisplayName("js files from dependency are installed")
    @GradleTest
    fun testKotlinJsDependencyWithJsFiles(gradleVersion: GradleVersion) {
        project("kotlin-js-dependency-with-js-files", gradleVersion) {
            build("jsPackageJson") {
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

    @TestMetadata("kotlin-js-browser-library-project")
    @DisplayName("test package.json skipped with main package.json was up-to-date")
    @GradleTest
    fun testPackageJsonSkippedWithUpToDatePackageJsons(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-library-project", gradleVersion) {
            subProject("app").buildGradleKts.modify {
                it + """
                    
                    tasks.named("jsTestPackageJson") {
                        enabled = false
                    }

                    tasks.named("jsTestPublicPackageJson") {
                        enabled = false
                    }
                """.trimIndent()
            }

            build("assemble") {
                assertTasksExecuted(":app:jsPackageJson")
                assertTasksExecuted(":base:jsPackageJson")
                assertTasksExecuted(":lib:jsPackageJson")
            }

            build("check") {
                assertTasksUpToDate(":app:jsPackageJson")
                assertTasksUpToDate(":base:jsPackageJson")
                assertTasksUpToDate(":lib:jsPackageJson")

                assertTasksSkipped(":app:jsTestPackageJson")
                assertTasksExecuted(":lib:jsTestPackageJson")
                assertTasksExecuted(":base:jsTestPackageJson")
            }
        }
    }

    @TestMetadata("js-project-repos")
    @DisplayName("test FAIL_ON_PROJECT_REPOS using custom repository")
    @GradleTest
    fun testFailOnProjectReposUsingCustomRepo(gradleVersion: GradleVersion) {
        project("js-project-repos", gradleVersion) {
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

    @TestMetadata("js-project-repos")
    @DisplayName("test FAIL_ON_PROJECT_REPOS no download")
    @GradleTest
    fun testFailOnProjectReposNoDownload(gradleVersion: GradleVersion) {
        project("js-project-repos", gradleVersion) {
            buildGradleKts.modify {
                it + """
                    
                    rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
                        the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().download.set(false)
                    }

                    rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
                        the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec>().download.set(false)
                    }
                """.trimIndent()
            }

            build("kotlinNodeJsSetup", "kotlinYarnSetup") {
                assertTasksSkipped(":kotlinNodeJsSetup")
                assertTasksSkipped(":kotlinYarnSetup")
            }
        }
    }

    @TestMetadata("kotlin-js-project-failed-test")
    @DisplayName("Check that jsNodeTest run tests with commonjs module kind")
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
            buildAndFail("jsNodeTest") {
                assertTasksFailed(":jsNodeTest")

                assertTestResults(
                    projectPath.resolve(
                        if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_9_3)) {
                            "TEST-all.xml"
                        } else {
                            "Gradle93-TEST-all.xml"
                        }
                    ),
                    "jsNodeTest"
                )
            }
        }
    }

    @TestMetadata("kotlin-js-project-failed-test")
    @DisplayName("Check that jsNodeTest run tests with ESM module kind")
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
            buildAndFail("jsNodeTest") {
                assertTasksFailed(":jsNodeTest")

                assertTestResults(
                    if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_9_3)) {
                        projectPath.resolve("TEST-all.xml")
                    } else {
                        projectPath.resolve("Gradle93-TEST-all.xml")
                    },
                    "jsNodeTest"
                )
            }
        }
    }
}
