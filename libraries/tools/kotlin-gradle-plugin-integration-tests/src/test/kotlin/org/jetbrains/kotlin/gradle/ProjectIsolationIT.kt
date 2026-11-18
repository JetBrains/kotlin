/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsSeverity
import org.jetbrains.kotlin.gradle.targets.js.npm.fromSrcPackageJson
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText
import kotlin.io.path.deleteRecursively
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.test.assertEquals

@MustRunOnChangesInGradle
class ProjectIsolationIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.enableIsolatedProjects()

    @TestMetadata("instantExecution")
    @DisplayName("JVM project should be compatible with project isolation")
    @JvmGradlePluginTests
    @GradleTest
    fun testProjectIsolationInJvmSimple(gradleVersion: GradleVersion) {
        project(
            projectName = "instantExecution",
            gradleVersion = gradleVersion,
        ) {
            build(":main-project:compileKotlin")
        }
    }

    @TestMetadata("kt-63990-buildSrcWithKotlinJvmPlugin")
    @DisplayName("project with buildSrc should be compatible with project isolation")
    @JvmGradlePluginTests
    @GradleTestVersions
    @GradleTest
    fun testProjectIsolationWithBuildSrc(gradleVersion: GradleVersion) {
        project(
            projectName = "kt-63990-buildSrcWithKotlinJvmPlugin",
            gradleVersion = gradleVersion,
        ) {
            build("tasks")
        }
    }

    @TestMetadata("AndroidIncrementalMultiModule")
    @DisplayName("Multi-module Android project")
    @GradleAndroidTest
    @AndroidGradlePluginTests
    fun testProjectIsolationAndroid(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            projectName = "AndroidIncrementalMultiModule",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleDebug")
        }
    }

    @DisplayName("Kapt multi-module project")
    @OtherGradlePluginTests
    @TestMetadata("kapt/javacIsLoadedOnce")
    @GradleTest
    fun testProjectIsolationKapt(
        gradleVersion: GradleVersion,
    ) {
        project("kapt/javacIsLoadedOnce", gradleVersion) {
            build("assemble")
        }
    }

    @DisplayName("Kapt with Android multi-module project")
    @AndroidGradlePluginTests
    @AndroidTestVersions(maxVersion = TestVersions.AGP.AGP_813)
    @TestMetadata("kapt/android-databinding")
    @GradleAndroidTest
    fun testProjectIsolationAndroidWithKapt(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            projectName = "kapt/android-databinding",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            gradleProperties.appendText(
                """
                |kotlin.jvm.target.validation.mode=warning
                """.trimMargin()
            )

            build("assembleDebug")
        }
    }

    // TODO KT-91654 Weak: the build runs the unqualified `kotlinJsSetupSharedNpmProject`, so it would still pass if the settings plugin applied the shared npm project plugin to every project. Assert `:lib-a` has no setup task. Also add a case with `Off` plus the settings plugin and no manual apply: the root has no setup task.
    @DisplayName("Shared JS and WasmJs npm projects should be compatible with project isolation")
    @JsGradlePluginTests
    @GradleTest
    fun testProjectIsolationSharedNpmProject(gradleVersion: GradleVersion) {
        project("emptyKts", gradleVersion) {
            enableWebIsolatedProjectsSupportPreview()
            applyKotlinEcosystemSettingsPlugin()
            plugins {
                kotlin("multiplatform").apply(false)
            }

            includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = "lib-a") {
                buildScriptInjection {
                    project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                    kotlinMultiplatform.apply {
                        js { nodejs() }
                        wasmJs { nodejs() }
                        sourceSets.getByName("commonMain").dependencies {
                            npm("is-even", "1.0.0")
                        }
                    }
                }
            }

            includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = "lib-b") {
                buildScriptInjection {
                    project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                    kotlinMultiplatform.apply {
                        js { nodejs() }
                        wasmJs { nodejs() }
                        sourceSets.getByName("commonMain").dependencies {
                            implementation(project(":lib-a"))
                        }
                    }
                }
            }

            build("kotlinJsSetupSharedNpmProject", "kotlinWasmJsSetupSharedNpmProject") {
                assertTasksExecuted(
                    ":lib-a:jsNpmDependenciesReport",
                    ":lib-a:jsTestNpmDependenciesReport",
                    ":lib-b:jsNpmDependenciesReport",
                    ":lib-b:jsTestNpmDependenciesReport",
                    ":lib-a:wasmJsNpmDependenciesReport",
                    ":lib-a:wasmJsTestNpmDependenciesReport",
                    ":lib-b:wasmJsNpmDependenciesReport",
                    ":lib-b:wasmJsTestNpmDependenciesReport",
                )
                for (platform in listOf("js", "wasmJs")) {
                    val libB = projectPath.resolve("build/$platform/shared-npm-project/packages/emptyKts-lib-b/package.json").toFile()
                    assertEquals("1.0.0", fromSrcPackageJson(libB)?.dependencies?.get("is-even"), "is-even from :lib-a in $libB")
                }
            }

            // When the configuration cache is reused, the registry is a fresh, empty instance.
            // Delete the outputs, so the tasks run again with the projects collected in the cached configuration.
            listOf("js", "wasmJs").forEach { platform ->
                projectPath.resolve("build/$platform/shared-npm-project").deleteRecursively()
            }
            build("kotlinJsSetupSharedNpmProject", "kotlinWasmJsSetupSharedNpmProject") {
                assertConfigurationCacheReused()
                assertTasksExecuted(":kotlinJsSetupSharedNpmProject", ":kotlinWasmJsSetupSharedNpmProject")
                listOf("js", "wasmJs").forEach { platform ->
                    val libB = projectPath.resolve("build/$platform/shared-npm-project/packages/emptyKts-lib-b/package.json")
                    assertEquals(
                        "1.0.0",
                        fromSrcPackageJson(libB.toFile())?.dependencies?.get("is-even"),
                        "is-even from :lib-a in $libB",
                    )
                }
            }
        }
    }

    @DisplayName("Shared npm projects fail with project isolation when the Kotlin ecosystem plugin is not applied")
    @JsGradlePluginTests
    @GradleTest
    fun testProjectIsolationSharedNpmProjectWithoutEcosystemPluginGetsError(gradleVersion: GradleVersion) {
        project("emptyKts", gradleVersion) {
            enableWebIsolatedProjectsSupportPreview()
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                kotlinMultiplatform.js { nodejs() }
            }

            // An error diagnostic only fails Kotlin tasks, so 'help' still runs and shows it.
            build("help") {
                assertHasDiagnostic(
                    KotlinToolingDiagnostics.SharedNpmProjectEcosystemPluginMissing,
                    expectedSeverity = KotlinToolingDiagnosticsSeverity.ERROR,
                )
            }
        }
    }

    // TODO KT-91654 Probably doesn't test what its name says: the settings plugin puts KGP on the settings classpath, and Gradle loads classes parent-first, so both subprojects likely share one copy of KGP's classes. Check class identity; then rework it so KGP really loads in different classloaders (covering `registryOrNull` returning null), or drop it.
    @DisplayName("Shared npm projects collect projects that load KGP in different classloaders")
    @JsGradlePluginTests
    @GradleTest
    fun testProjectIsolationSharedNpmProjectWithDifferentClassloaders(gradleVersion: GradleVersion) {
        project("emptyKts", gradleVersion) {
            enableWebIsolatedProjectsSupportPreview()
            applyKotlinEcosystemSettingsPlugin()

            // Each subproject puts KGP on its own build script classpath, and :lib-b also applies allopen.
            // The classpaths differ, so Gradle gives each subproject a build script classloader of its own.
            transferPluginRepositoriesIntoBuildScript()
            val kotlinVersion = buildOptions.kotlinVersion
            listOf(
                "lib-a" to listOf("multiplatform" to "kotlin-gradle-plugin"),
                "lib-b" to listOf("multiplatform" to "kotlin-gradle-plugin", "plugin.allopen" to "kotlin-allopen"),
            ).forEach { (name, plugins) ->
                includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = name) {
                    buildScriptBuildscriptBlockInjection {
                        plugins.forEach { (_, artifact) ->
                            buildscript.configurations.getByName("classpath").dependencies.add(
                                buildscript.dependencies.create("org.jetbrains.kotlin:$artifact:$kotlinVersion")
                            )
                        }
                    }
                    buildScriptInjection {
                        plugins.forEach { (pluginId, _) ->
                            project.plugins.apply("org.jetbrains.kotlin.$pluginId")
                        }
                        kotlinMultiplatform.js { nodejs() }
                        kotlinMultiplatform.sourceSets.getByName("commonMain").dependencies {
                            npm("is-even", "1.0.0")
                        }
                    }
                }
            }

            build("kotlinJsSetupSharedNpmProject") {
                val jsModuleDirs = projectPath.resolve("build/js/shared-npm-project/packages").listDirectoryEntries().sorted()

                assertEquals(
                    listOf("emptyKts-lib-a", "emptyKts-lib-a-test", "emptyKts-lib-b", "emptyKts-lib-b-test"),
                    jsModuleDirs.map { it.name },
                )

                jsModuleDirs.forEach { packageDir ->
                    val packageJson = packageDir.resolve("package.json")
                    assertEquals(
                        "1.0.0",
                        fromSrcPackageJson(packageJson.toFile())?.dependencies?.get("is-even"), "is-even in $packageJson"
                    )
                }
            }
        }
    }
}
