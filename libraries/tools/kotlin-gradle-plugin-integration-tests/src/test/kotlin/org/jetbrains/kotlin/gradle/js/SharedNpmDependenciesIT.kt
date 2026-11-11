/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package org.jetbrains.kotlin.gradle.js

import org.gradle.api.attributes.Attribute
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.targets.js.npm.fromSrcPackageJson
import org.jetbrains.kotlin.gradle.targets.web.npm.KotlinSharedNpmProjectPlugin
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.NpmDependencyReport
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.readNpmDependencyReport
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.readPackageJsonDependencies
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.PublisherConfiguration
import org.jetbrains.kotlin.gradle.uklibs.addPublishedProjectToRepositories
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.publish
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertNull

@JsGradlePluginTests
@MustRunOnChangesInGradle
class SharedNpmDependenciesIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    private val versions = NpmVersions()

    private val karmaTools = mapOf(
        versions.karma.name to versions.karma.version,
        versions.webpack.name to versions.webpack.version,
    )

    private val mochaTools = mapOf(
        versions.mocha.name to versions.mocha.version,
    )

    @DisplayName("Shared npm projects contain a package.json per compilation of the declared JS and WasmJS projects")
    @GradleTest
    fun testSharedNpmProjectContent(gradleVersion: GradleVersion) {
        project("emptyKts", gradleVersion) {
            plugins {
                kotlin("multiplatform").apply(false)
            }
            buildScriptInjection {
                project.plugins.apply(KotlinSharedNpmProjectPlugin::class.java)
                project.dependencies.add("kotlinJsNpmSharedDependencies", project.dependencies.project(mapOf("path" to ":lib-a")))
                project.dependencies.add("kotlinJsNpmSharedDependencies", project.dependencies.project(mapOf("path" to ":lib-b")))
                project.dependencies.add("kotlinJsNpmSharedDependencies", project.dependencies.project(mapOf("path" to ":lib-web")))
                project.dependencies.add("kotlinWasmNpmSharedDependencies", project.dependencies.project(mapOf("path" to ":lib-b")))
            }

            includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = "lib-a") {
                buildScriptInjection {
                    project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                    kotlinMultiplatform.apply {
                        js { nodejs() }
                        sourceSets.getByName("jsMain").dependencies {
                            npm("is-even", "1.0.0")
                            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
                        }
                    }
                }
            }

            includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = "lib-b") {
                buildScriptInjection {
                    project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                    kotlinMultiplatform.apply {
                        js { browser() }
                        wasmJs { browser() }
                        sourceSets.getByName("commonMain").dependencies {
                            npm("cowsay", "9.9.9")
                        }
                        sourceSets.getByName("jsMain").dependencies {
                            implementation(project(":lib-a"))
                        }
                    }
                }
            }

            includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = "lib-web") {
                buildScriptInjection {
                    project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                    kotlinMultiplatform.apply {
                        js { browser() }
                        sourceSets.getByName("jsMain").dependencies {
                            implementation(project(":lib-b"))
                        }
                    }
                }
            }

            val libADependencies = mapOf(
                "is-even" to "1.0.0",
                "@js-joda/core" to "3.2.0",
                "format-util" to "^1.0.5",
            )
            val libBJsDependencies = mapOf("cowsay" to "9.9.9") + libADependencies
            val libBWasmDependencies = mapOf("cowsay" to "9.9.9")
            val libWebDependencies = libBJsDependencies

            build("kotlinJsSetupSharedNpmProject", "kotlinWasmSetupSharedNpmProject") {
                assertTasksExecuted(
                    ":lib-a:jsNpmDependenciesReport",
                    ":lib-a:jsTestNpmDependenciesReport",
                    ":lib-b:jsNpmDependenciesReport",
                    ":lib-b:jsTestNpmDependenciesReport",
                    ":lib-b:wasmJsNpmDependenciesReport",
                    ":lib-b:wasmJsTestNpmDependenciesReport",
                    ":lib-web:jsNpmDependenciesReport",
                    ":lib-web:jsTestNpmDependenciesReport",
                )

                val js = projectPath.resolve("build/js/shared-npm-project")
                val wasm = projectPath.resolve("build/wasm/shared-npm-project")

                assertRootPackageJson(
                    js.resolve("package.json"),
                    workspaces = listOf(
                        "packages/emptyKts-lib-a",
                        "packages/emptyKts-lib-a-test",
                        "packages/emptyKts-lib-b",
                        "packages/emptyKts-lib-b-test",
                        "packages/emptyKts-lib-web",
                        "packages/emptyKts-lib-web-test",
                    ),
                )
                assertWorkspacePackageJson(
                    js.resolve("packages/emptyKts-lib-a/package.json"),
                    name = "emptyKts-lib-a",
                    dependencies = libADependencies,
                )
                assertWorkspacePackageJson(
                    js.resolve("packages/emptyKts-lib-a-test/package.json"),
                    name = "emptyKts-lib-a-test",
                    dependencies = libADependencies,
                    requiredDevDependencies = mochaTools,
                )
                assertWorkspacePackageJson(
                    js.resolve("packages/emptyKts-lib-b/package.json"),
                    name = "emptyKts-lib-b",
                    dependencies = libBJsDependencies,
                )
                assertWorkspacePackageJson(
                    js.resolve("packages/emptyKts-lib-b-test/package.json"),
                    name = "emptyKts-lib-b-test",
                    dependencies = libBJsDependencies,
                    requiredDevDependencies = karmaTools,
                )

                assertWorkspacePackageJson(
                    js.resolve("packages/emptyKts-lib-web/package.json"),
                    name = "emptyKts-lib-web",
                    dependencies = libWebDependencies,
                )
                assertWorkspacePackageJson(
                    js.resolve("packages/emptyKts-lib-web-test/package.json"),
                    name = "emptyKts-lib-web-test",
                    dependencies = libWebDependencies,
                    requiredDevDependencies = karmaTools,
                )

                assertRootPackageJson(
                    wasm.resolve("package.json"),
                    workspaces = listOf("packages/emptyKts-lib-b", "packages/emptyKts-lib-b-test"),
                )
                assertWorkspacePackageJson(
                    wasm.resolve("packages/emptyKts-lib-b/package.json"),
                    name = "emptyKts-lib-b",
                    dependencies = libBWasmDependencies,
                )
                assertWorkspacePackageJson(
                    wasm.resolve("packages/emptyKts-lib-b-test/package.json"),
                    name = "emptyKts-lib-b-test",
                    dependencies = libBWasmDependencies,
                    requiredDevDependencies = karmaTools,
                )
            }

            build("kotlinJsSetupSharedNpmProject", "kotlinWasmSetupSharedNpmProject") {
                assertConfigurationCacheReused()
                assertTasksUpToDate(
                    ":lib-a:jsNpmDependenciesReport",
                    ":lib-a:jsTestNpmDependenciesReport",
                    ":lib-b:jsNpmDependenciesReport",
                    ":lib-b:jsTestNpmDependenciesReport",
                    ":lib-b:wasmJsNpmDependenciesReport",
                    ":lib-b:wasmJsTestNpmDependenciesReport",
                    ":lib-web:jsNpmDependenciesReport",
                    ":lib-web:jsTestNpmDependenciesReport",
                    ":kotlinJsSetupSharedNpmProject",
                    ":kotlinWasmSetupSharedNpmProject",
                )
            }
        }
    }

    @DisplayName("KT-90085 - npm dependency report of a dependent project picks up npm changes of its dependency project")
    @GradleTest
    fun testNpmDependenciesReportPicksUpDependencyNpmChanges(gradleVersion: GradleVersion) {
        project("emptyKts", gradleVersion) {
            plugins {
                kotlin("multiplatform").apply(false)
            }
            buildScriptInjection {
                project.plugins.apply(KotlinSharedNpmProjectPlugin::class.java)
                project.dependencies.add("kotlinJsNpmSharedDependencies", project.dependencies.project(mapOf("path" to ":lib-a")))
                project.dependencies.add("kotlinJsNpmSharedDependencies", project.dependencies.project(mapOf("path" to ":lib-b")))
            }

            includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = "lib-a") {
                buildScriptInjection {
                    project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                    kotlinMultiplatform.apply {
                        js { nodejs() }
                        sourceSets.getByName("jsMain").dependencies {
                            implementation(npm("is-even", "1.0.0"))
                            if (project.hasProperty("addLeftPad")) {
                                implementation(npm("left-pad", "1.3.0"))
                            }
                        }
                    }
                }
            }

            includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = "lib-b") {
                buildScriptInjection {
                    project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                    kotlinMultiplatform.apply {
                        js { nodejs() }
                        sourceSets.getByName("jsMain").dependencies {
                            implementation(project(":lib-a"))
                        }
                    }
                }
            }

            fun TestProject.libBNpmDependenciesReport(): NpmDependencyReport =
                projectPath.resolve("lib-b/build/kotlin-npm/js/npm-dep-reports/emptyKts-lib-b/npm-dep-report.json").toFile()
                    .let { readNpmDependencyReport(it) }

            build(":lib-b:jsNpmDependenciesReport") {
                assertTasksExecuted(":lib-a:jsNpmDependenciesReport")
                assertNull(libBNpmDependenciesReport().dependencies["left-pad"])
            }

            build(":lib-b:jsNpmDependenciesReport", "-PaddLeftPad=true") {
                assertTasksExecuted(":lib-a:jsNpmDependenciesReport")
                assertEquals("1.3.0", libBNpmDependenciesReport().dependencies["left-pad"])
            }

            build(":lib-b:jsNpmDependenciesReport", "-PaddLeftPad=true") {
                assertTasksUpToDate(":lib-a:jsNpmDependenciesReport", ":lib-b:jsNpmDependenciesReport")
            }
        }
    }

    @DisplayName("Test compilation depending on a project that depends on the main compilation has no circular npm dependency report")
    @GradleTest
    fun testNoCircularNpmDependenciesReportBetweenTestAndMainCompilations(gradleVersion: GradleVersion) {
        project("emptyKts", gradleVersion) {
            plugins {
                kotlin("multiplatform").apply(false)
            }
            buildScriptInjection {
                project.plugins.apply(KotlinSharedNpmProjectPlugin::class.java)
                project.dependencies.add("kotlinJsNpmSharedDependencies", project.dependencies.project(mapOf("path" to ":core")))
                project.dependencies.add("kotlinJsNpmSharedDependencies", project.dependencies.project(mapOf("path" to ":test-utils")))
            }

            includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = "core") {
                buildScriptInjection {
                    project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                    kotlinMultiplatform.apply {
                        js { nodejs() }
                        sourceSets.getByName("jsMain").dependencies {
                            implementation(npm("is-even", "1.0.0"))
                        }
                        sourceSets.getByName("jsTest").dependencies {
                            implementation(project(":test-utils"))
                        }
                    }
                }
            }

            includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = "test-utils") {
                buildScriptInjection {
                    project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                    kotlinMultiplatform.apply {
                        js { nodejs() }
                        sourceSets.getByName("jsMain").dependencies {
                            implementation(project(":core"))
                            implementation(npm("left-pad", "1.3.0"))
                        }
                    }
                }
            }

            val coreDependencies = mapOf("is-even" to "1.0.0")
            val testUtilsDependencies = mapOf("left-pad" to "1.3.0") + coreDependencies

            // core:jsTest needs test-utils main, which needs core main: a separate task, so no cycle.
            build("kotlinJsSetupSharedNpmProject") {
                assertTasksExecuted(
                    ":core:jsNpmDependenciesReport",
                    ":core:jsTestNpmDependenciesReport",
                    ":test-utils:jsNpmDependenciesReport",
                    ":test-utils:jsTestNpmDependenciesReport",
                )

                val js = projectPath.resolve("build/js/shared-npm-project")
                assertRootPackageJson(
                    js.resolve("package.json"),
                    workspaces = listOf(
                        "packages/emptyKts-core",
                        "packages/emptyKts-core-test",
                        "packages/emptyKts-test-utils",
                        "packages/emptyKts-test-utils-test",
                    ),
                )
                assertWorkspacePackageJson(
                    js.resolve("packages/emptyKts-core/package.json"),
                    name = "emptyKts-core",
                    dependencies = coreDependencies,
                )
                assertWorkspacePackageJson(
                    js.resolve("packages/emptyKts-core-test/package.json"),
                    name = "emptyKts-core-test",
                    dependencies = testUtilsDependencies,
                    requiredDevDependencies = mochaTools,
                )
                assertWorkspacePackageJson(
                    js.resolve("packages/emptyKts-test-utils/package.json"),
                    name = "emptyKts-test-utils",
                    dependencies = testUtilsDependencies,
                )
            }
        }
    }

    @DisplayName("npm dependencies of klibs, unpacked klibs and package.json files without a report reach the npm dependency report")
    @GradleTest
    fun testNpmDependenciesOfKlibsWithoutReport(gradleVersion: GradleVersion) {
        val published = project("empty", gradleVersion) {
            settingsBuildScriptInjection {
                settings.rootProject.name = "publishedKlib"
            }
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.applyMultiplatform {
                    js { nodejs() }
                    sourceSets.getByName("jsMain").dependencies {
                        implementation(npm("published-klib-dep", "3.0.1"))
                    }
                }
            }
        }.publish(publisherConfiguration = PublisherConfiguration(group = "producer", version = "1.0.0"))

        val publishedKlib = published.jsComponent.klib
        val publishedKlibPackageJson = publishedKlib.readPackageJsonDependencies() ?: error("No package.json in $publishedKlib")
        assertEquals("3.0.1", publishedKlibPackageJson.dependencies["published-klib-dep"])

        project("emptyKts", gradleVersion) {
            plugins {
                kotlin("multiplatform").apply(false)
            }
            addPublishedProjectToRepositories(published)

            includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = "lib") {
                buildScriptInjection {
                    project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                    kotlinMultiplatform.apply {
                        js { nodejs() }
                        sourceSets.getByName("jsMain").dependencies {
                            implementation(project.files("localKlib.klib"))
                            implementation(project.files("unpackedKlib"))
                            implementation(project.files("bare/package.json"))
                            implementation(published.rootCoordinate)
                        }
                    }
                }
            }

            ZipOutputStream(projectPath.resolve("lib/localKlib.klib").toFile().outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("package.json"))
                zip.write("""{"name": "localKlib", "dependencies": {"local-klib-dep": "1.3.0"}}""".toByteArray())
            }
            projectPath.resolve("lib/unpackedKlib").toFile().apply { mkdirs() }.resolve("package.json")
                .writeText("""{"name": "unpackedKlib", "dependencies": {"unpacked-klib-dep": "2.0.0"}}""")
            projectPath.resolve("lib/bare").toFile().apply { mkdirs() }.resolve("package.json")
                .writeText("""{"name": "bare", "dependencies": {"bare-package-json-dep": "4.0.0"}}""")

            build(":lib:jsNpmDependenciesReport") {
                val report = readNpmDependencyReport(
                    projectPath.resolve("lib/build/kotlin-npm/js/npm-dep-reports/emptyKts-lib/npm-dep-report.json").toFile()
                )
                assertEquals("emptyKts-lib", report.compilation)
                assertEquals("1.3.0", report.dependencies["local-klib-dep"])
                assertEquals("2.0.0", report.dependencies["unpacked-klib-dep"])
                assertEquals("4.0.0", report.dependencies["bare-package-json-dep"])
                assertEquals("3.0.1", report.dependencies["published-klib-dep"])
            }
        }
    }

    @DisplayName("Shared npm projects support several JS targets in one project")
    @GradleTest
    fun testSharedNpmProjectWithSeveralJsTargets(gradleVersion: GradleVersion) {
        project("emptyKts", gradleVersion) {
            plugins {
                kotlin("multiplatform").apply(false)
            }
            buildScriptInjection {
                project.plugins.apply(KotlinSharedNpmProjectPlugin::class.java)
                project.dependencies.add("kotlinJsNpmSharedDependencies", project.dependencies.project(mapOf("path" to ":lib-a")))
                project.dependencies.add("kotlinJsNpmSharedDependencies", project.dependencies.project(mapOf("path" to ":lib-b")))
            }

            includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = "lib-a") {
                buildScriptInjection {
                    project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                    kotlinMultiplatform.apply {
                        val targetAttr = Attribute.of("test.attribute", String::class.java)
                        js("first") {
                            attributes.attribute(targetAttr, "first")
                            nodejs()
                        }
                        js("second") {
                            attributes.attribute(targetAttr, "second")
                            browser()
                        }
                        sourceSets.getByName("firstMain").dependencies {
                            implementation(npm("is-even", "1.0.0"))
                        }
                        sourceSets.getByName("secondMain").dependencies {
                            implementation(npm("is-odd", "3.0.1"))
                        }
                    }
                }
            }

            includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = "lib-b") {
                buildScriptInjection {
                    project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                    kotlinMultiplatform.apply {
                        val targetAttr = Attribute.of("test.attribute", String::class.java)
                        js("first") {
                            attributes.attribute(targetAttr, "first")
                            nodejs()
                        }
                        js("second") {
                            attributes.attribute(targetAttr, "second")
                            browser()
                        }
                        sourceSets.getByName("commonMain").dependencies {
                            implementation(project(":lib-a"))
                        }
                    }
                }
            }

            val firstDependencies = mapOf("is-even" to "1.0.0")
            val secondDependencies = mapOf("is-odd" to "3.0.1")

            build("kotlinJsSetupSharedNpmProject") {
                assertTasksExecuted(
                    ":lib-a:firstNpmDependenciesReport",
                    ":lib-a:secondNpmDependenciesReport",
                    ":lib-b:firstNpmDependenciesReport",
                    ":lib-b:secondNpmDependenciesReport",
                )

                val js = projectPath.resolve("build/js/shared-npm-project")
                assertRootPackageJson(
                    js.resolve("package.json"),
                    workspaces = listOf(
                        "packages/emptyKts-lib-a-first",
                        "packages/emptyKts-lib-a-first-test",
                        "packages/emptyKts-lib-a-second",
                        "packages/emptyKts-lib-a-second-test",
                        "packages/emptyKts-lib-b-first",
                        "packages/emptyKts-lib-b-first-test",
                        "packages/emptyKts-lib-b-second",
                        "packages/emptyKts-lib-b-second-test",
                    ),
                )
                assertWorkspacePackageJson(
                    js.resolve("packages/emptyKts-lib-b-first/package.json"),
                    name = "emptyKts-lib-b-first",
                    dependencies = firstDependencies,
                )
                assertWorkspacePackageJson(
                    js.resolve("packages/emptyKts-lib-b-second/package.json"),
                    name = "emptyKts-lib-b-second",
                    dependencies = secondDependencies,
                )
            }
        }
    }

    private fun readPackageJson(file: Path): PackageJson {
        assertFileExists(file)
        return fromSrcPackageJson(file.toFile()) ?: error("Cannot parse $file")
    }

    private fun assertRootPackageJson(file: Path, workspaces: List<String>) {
        val packageJson = readPackageJson(file)
        assertEquals("emptyKts", packageJson.name, "name of $file")
        assertEquals("0.0.0-unspecified", packageJson.version, "version of $file")
        assertEquals(true, packageJson.private, "private of $file")
        assertEquals(workspaces, packageJson.workspaces?.toList(), "workspaces of $file")
    }

    private fun assertWorkspacePackageJson(
        file: Path,
        name: String,
        dependencies: Map<String, String>,
        requiredDevDependencies: Map<String, String> = emptyMap(),
    ) {
        val packageJson = readPackageJson(file)
        assertEquals(name, packageJson.name, "name of $file")
        assertEquals("0.0.0-unspecified", packageJson.version, "version of $file")
        assertEquals(dependencies.toSortedMap(), packageJson.dependencies.toSortedMap(), "dependencies of $file")
        assertEquals(
            requiredDevDependencies.toSortedMap(),
            packageJson.devDependencies.filterKeys { it in requiredDevDependencies }.toSortedMap(),
            "devDependencies of $file",
        )
    }
}
