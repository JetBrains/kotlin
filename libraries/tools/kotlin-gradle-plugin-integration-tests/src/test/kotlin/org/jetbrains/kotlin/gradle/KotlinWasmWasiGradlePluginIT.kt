/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.named
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ecosystem.KotlinEcosystemExtension
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.wasm.wasiadapter.toolchain.WasiAdapterToolchainMode
import org.jetbrains.kotlin.gradle.targets.wasm.component.WasmComponentizeTask
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.appendText
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@WasmWasiGradlePluginTests
class KotlinWasmWasiGradlePluginIT : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @DisplayName("Check wasi target with wasmtime")
    @GradleTest
    fun wasiWasmtimeTarget(gradleVersion: GradleVersion) {
        project("wasm-wasmtime-test", gradleVersion) {
            wasmWasiTest("Wasmtime")
        }
    }

    @DisplayName("Check wasi target with wasi adapter in disabled mode fails")
    @GradleTest
    fun wasiAdapterDisabledMode(gradleVersion: GradleVersion) {
        project("wasm-wasmtime-test", gradleVersion) {
            gradleProperties.appendText(
                """
                |
                |${PropertiesProvider.PropertyNames.KOTLIN_WASM_WASI_ADAPTER_TOOLCHAIN_MODE}=${WasiAdapterToolchainMode.DISABLE.name}
                """.trimMargin()
            )

            build(":compileProductionExecutableKotlinWasmWasi") {
                assertTasksExecuted(":compileProductionExecutableKotlinWasmWasi")
            }

            buildAndFail(":compileProductionExecutableKotlinWasmWasiComponentize") {
                assertOutputContains("WASI adapter toolchain is disabled")
                assertOutputContains("${PropertiesProvider.PropertyNames.KOTLIN_WASM_WASI_ADAPTER_TOOLCHAIN_MODE}=${WasiAdapterToolchainMode.DOWNLOAD.name}")
            }
        }
    }

    @DisplayName("Check wasi target with wasi adapter in preinstalled mode")
    @GradleTest
    fun wasiAdapterPreInstalledMode(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        val realTempDir = tempDir.toRealPath()
        val fakeAdapter = realTempDir.resolve("wasi_snapshot_preview1.command.wasm").toFile()
        copyMockWasiAdapter(fakeAdapter)

        project("wasm-wasmtime-test", gradleVersion) {
            gradleProperties.appendText(
                """
                |
                |${PropertiesProvider.PropertyNames.KOTLIN_WASM_WASI_ADAPTER_TOOLCHAIN_MODE}=${WasiAdapterToolchainMode.PREINSTALLED.name}
                |${PropertiesProvider.PropertyNames.KOTLIN_WASM_WASI_ADAPTER_TOOLCHAIN_PREINSTALLED_EXECUTABLE_PATH}=${fakeAdapter.absolutePath}
                """.trimMargin()
            )

            build(":compileProductionExecutableKotlinWasmWasiComponentize") {
                assertTasksExecuted(":compileProductionExecutableKotlinWasmWasiComponentize")
                assertOutputDoesNotContain("Downloading WASI preview1 adapter")
                val componentFile =
                    projectPath.resolve("build/compileSync/wasmWasi/main/productionExecutable/component/wasm-wasmtime-test.wasm")
                assertFileInProjectExists("build/compileSync/wasmWasi/main/productionExecutable/component/wasm-wasmtime-test.wasm")
                assertComponentNew(
                    inputModule = projectPath.resolve("build/compileSync/wasmWasi/main/productionExecutable/optimized/wasm-wasmtime-test.wasm")
                        .loggedPathString(),
                    component = componentFile,
                    adaptModule = fakeAdapter.toPath().loggedPathString(),
                )
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class, ExperimentalNodeJsToolchainDsl::class)
    @DisplayName("Check wasi target with reactor flavor requests reactor adapter")
    @GradleTest
    fun wasiAdapterReactorFlavor(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        val realTempDir = tempDir.toRealPath()
        val fakeRepoDir = realTempDir.resolve("mirror").toFile()
        val fakeInstallDir = realTempDir.resolve("install").toFile()
        val fakeReactorAdapterFile = fakeRepoDir.resolve("v49.0.1/wasi_snapshot_preview1.reactor.wasm")
        copyMockWasiAdapter(fakeReactorAdapterFile)

        val expectedInstalledAdapter = fakeInstallDir.resolve("v49.0.1/wasi_snapshot_preview1.reactor.wasm")

        project("wasm-wasmtime-test", gradleVersion) {
            addKgpToBuildScriptCompilationClasspath()
            addEcosystemPluginToBuildScriptCompilationClasspath()
            settingsBuildScriptInjection {
                settings.plugins.apply("org.jetbrains.kotlin.ecosystem")
                settings.extensions.getByType(KotlinEcosystemExtension::class.java).toolchainManagement {
                    wasiAdapter {
                        toolchainService {
                            installationDir.set(File(fakeInstallDir.invariantSeparatorsPath))
                            downloadBaseUrl.set(fakeRepoDir.toURI().toString().trimEnd('/'))
                        }
                    }
                }
            }

            buildScriptInjection {
                project.tasks.named<WasmComponentizeTask>("compileProductionExecutableKotlinWasmWasiComponentize") {
                    wasiAdapterRequest.get().flavor.set(org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter.WasiAdapterFlavor.REACTOR)
                }
            }

            build(":compileProductionExecutableKotlinWasmWasiComponentize") {
                assertTasksExecuted(":compileProductionExecutableKotlinWasmWasiComponentize")
                val downloadUrl = fakeRepoDir.toURI().toString().trimEnd('/') + "/v49.0.1/wasi_snapshot_preview1.reactor.wasm"
                assertOutputContains("Downloading WASI preview1 adapter REACTOR for Wasmtime 49.0.1 from $downloadUrl")
                assertTrue(expectedInstalledAdapter.exists(), "Expected installed adapter to exist at $expectedInstalledAdapter")

                val componentFile =
                    projectPath.resolve("build/compileSync/wasmWasi/main/productionExecutable/component/wasm-wasmtime-test.wasm")
                assertFileInProjectExists("build/compileSync/wasmWasi/main/productionExecutable/component/wasm-wasmtime-test.wasm")
                assertComponentNew(
                    inputModule = projectPath.resolve("build/compileSync/wasmWasi/main/productionExecutable/optimized/wasm-wasmtime-test.wasm")
                        .loggedPathString(),
                    component = componentFile,
                    adaptModule = expectedInstalledAdapter.toPath().loggedPathString(),
                )
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class, ExperimentalNodeJsToolchainDsl::class)
    @DisplayName("Check wasi target with wasi adapter custom download base url from settings")
    @GradleTest
    fun wasiAdapterSettingsCustomDownloadBaseUrl(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        val realTempDir = tempDir.toRealPath()
        val fakeRepoDir = realTempDir.resolve("mirror").toFile()
        val fakeInstallDir = realTempDir.resolve("install").toFile()
        val fakeAdapterFile = fakeRepoDir.resolve("v49.0.1/wasi_snapshot_preview1.command.wasm")
        copyMockWasiAdapter(fakeAdapterFile)

        val expectedInstalledAdapter = fakeInstallDir.resolve("v49.0.1/wasi_snapshot_preview1.command.wasm")

        project("wasm-wasmtime-test", gradleVersion) {
            addKgpToBuildScriptCompilationClasspath()
            addEcosystemPluginToBuildScriptCompilationClasspath()
            settingsBuildScriptInjection {
                settings.plugins.apply("org.jetbrains.kotlin.ecosystem")
                settings.extensions.getByType(KotlinEcosystemExtension::class.java).toolchainManagement {
                    wasiAdapter {
                        toolchainService {
                            installationDir.set(File(fakeInstallDir.invariantSeparatorsPath))
                            downloadBaseUrl.set(fakeRepoDir.toURI().toString().trimEnd('/'))
                        }
                    }
                }
            }

            build(":compileProductionExecutableKotlinWasmWasiComponentize") {
                assertTasksExecuted(":compileProductionExecutableKotlinWasmWasiComponentize")
                val downloadUrl = fakeRepoDir.toURI().toString().trimEnd('/') + "/v49.0.1/wasi_snapshot_preview1.command.wasm"
                assertOutputContains("Downloading WASI preview1 adapter COMMAND for Wasmtime 49.0.1 from $downloadUrl")
                assertTrue(expectedInstalledAdapter.exists(), "Expected installed adapter to exist at $expectedInstalledAdapter")

                val componentFile =
                    projectPath.resolve("build/compileSync/wasmWasi/main/productionExecutable/component/wasm-wasmtime-test.wasm")
                assertFileInProjectExists("build/compileSync/wasmWasi/main/productionExecutable/component/wasm-wasmtime-test.wasm")
                assertComponentNew(
                    inputModule = projectPath.resolve("build/compileSync/wasmWasi/main/productionExecutable/optimized/wasm-wasmtime-test.wasm")
                        .loggedPathString(),
                    component = componentFile,
                    adaptModule = expectedInstalledAdapter.toPath().loggedPathString(),
                )
            }
        }
    }

    private fun copyMockWasiAdapter(targetFile: File): File {
        targetFile.parentFile.mkdirs()
        val resourceStream =
            KotlinWasmWasiGradlePluginIT::class.java.classLoader.getResourceAsStream("wasm-adapter/wasi_snapshot_preview1.command.wasm")
                ?: error("Could not find wasm-adapter/wasi_snapshot_preview1.command.wasm test resource")
        resourceStream.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return targetFile
    }

    private fun TestProject.wasmWasiTest(engine: String, engineSetup: String = engine) {
        build(":wasmWasiTest") {
            assertTasksExecuted(":kotlinWasm${engineSetup}Setup")
            assertTasksExecuted(":compileKotlinWasmWasi")
            assertTasksExecuted(":wasmWasi${engine}Test")
        }

        build(":wasmWasiTest") {
            assertTasksUpToDate(":kotlinWasm${engineSetup}Setup")
            assertTasksUpToDate(":compileKotlinWasmWasi", ":wasmWasi${engine}Test")
        }

        projectPath.resolve("src/wasmWasiTest/kotlin/Test.kt").modify {
            it.replace(
                "fun test2() = assertEquals(foo(), 2)",
                """
                    |fun test2() = assertEquals(foo(), 2)
                    |
                    |@Test
                    |fun test3() = assertEquals(foo(), 3)
                    |""".trimMargin()
            )
        }

        buildAndFail(":wasmWasiTest") {
            assertTasksUpToDate(":compileKotlinWasmWasi")
            assertTasksFailed(":wasmWasi${engine}Test")
        }

        build(":wasmWasi${engine}ProductionRun") {
            assertTasksExecuted(":compileProductionExecutableKotlinWasmWasi")
            assertTasksExecuted(":compileProductionExecutableKotlinWasmWasiOptimize")

            assertTasksAreNotInTaskGraph(":kotlinWasmToolingSetup")

            assertNoBuildWarnings()
        }
    }

    @DisplayName("Check wasi target with wasmtime and component model basic hello world")
    @GradleTest
    fun wasiWasmtimeBasicComponentModel(gradleVersion: GradleVersion) {
        project("wasm-wasmtime-test", gradleVersion) {
            build(":wasmWasiWasmtimeProductionRun") {
                assertTasksExecuted(
                    ":compileProductionExecutableKotlinWasmWasi",
                    ":compileProductionExecutableKotlinWasmWasiOptimize",
                    ":compileProductionExecutableKotlinWasmWasiComponentize",
                    ":wasmWasiWasmtimeProductionRun"
                )
                assertOutputContains("Hello from Wasi")
                val componentFile = "build/compileSync/wasmWasi/main/productionExecutable/component/wasm-wasmtime-test.wasm"
                assertFileInProjectExists(componentFile)
                assertComponentNew(
                    inputModule = projectPath
                        .resolve("build/compileSync/wasmWasi/main/productionExecutable/optimized/wasm-wasmtime-test.wasm")
                        .loggedPathString(),
                    component = projectPath.resolve(componentFile)
                )
            }

            build(":wasmWasiWasmtimeDevelopmentRun") {
                assertTasksExecuted(
                    ":compileDevelopmentExecutableKotlinWasmWasi",
                    ":compileDevelopmentExecutableKotlinWasmWasiComponentize",
                    ":wasmWasiWasmtimeDevelopmentRun"
                )
                assertOutputContains("Hello from Wasi")
                val componentFile = "build/compileSync/wasmWasi/main/developmentExecutable/component/wasm-wasmtime-test.wasm"
                assertFileInProjectExists(componentFile)
                assertComponentNew(
                    inputModule = projectPath
                        .resolve("build/compileSync/wasmWasi/main/developmentExecutable/kotlin/wasm-wasmtime-test.wasm")
                        .loggedPathString(),
                    component = projectPath.resolve(componentFile)
                )
            }
        }
    }

    @DisplayName("Check wasi target with wasmtime and wit declarations in the project")
    @GradleTest
    fun wasiWasmtimeWithProjectWitDeclarations(gradleVersion: GradleVersion) {
        project("wasm-wasmtime-test", gradleVersion) {
            val witDir = projectPath.resolve("wit")
            witDir.createDirectories()
            witDir.resolve("world.wit").writeText(
                """
                package my:project;

                world my-world {
                    import print: func(msg: string);
                }
                """.trimIndent()
            )

            build(":wasmWasiWasmtimeProductionRun") {
                assertTasksExecuted(
                    ":compileProductionExecutableKotlinWasmWasiComponentize",
                    ":wasmWasiWasmtimeProductionRun"
                )
                assertOutputContains("Hello from Wasi")
                val componentFile = "build/compileSync/wasmWasi/main/productionExecutable/component/wasm-wasmtime-test.wasm"
                assertFileInProjectExists(componentFile)

                val embeddedModule = assertComponentEmbed(
                    witProject = witDir,
                    inputModule = projectPath
                        .resolve("build/compileSync/wasmWasi/main/productionExecutable/optimized/wasm-wasmtime-test.wasm")
                        .loggedPathString()
                )

                assertComponentNew(
                    inputModule = embeddedModule,
                    component = projectPath.resolve(componentFile)
                )
            }

            build(":wasmWasiWasmtimeProductionRun") {
                assertTasksUpToDate(
                    ":compileProductionExecutableKotlinWasmWasi",
                    ":compileProductionExecutableKotlinWasmWasiOptimize",
                    ":compileProductionExecutableKotlinWasmWasiComponentize"
                )
                assertTasksExecuted(":wasmWasiWasmtimeProductionRun")
            }

            witDir.resolve("world.wit").writeText(
                """
                package my:project;

                world my-world {
                    import print: func(msg: string);
                    import log: func(msg: string);
                }
                """.trimIndent()
            )

            build(":wasmWasiWasmtimeProductionRun") {
                assertTasksUpToDate(
                    ":compileProductionExecutableKotlinWasmWasi",
                    ":compileProductionExecutableKotlinWasmWasiOptimize"
                )
                assertTasksExecuted(
                    ":compileProductionExecutableKotlinWasmWasiComponentize",
                    ":wasmWasiWasmtimeProductionRun"
                )

                val componentFile = "build/compileSync/wasmWasi/main/productionExecutable/component/wasm-wasmtime-test.wasm"
                assertFileInProjectExists(componentFile)

                val embeddedModule = assertComponentEmbed(
                    witProject = witDir,
                    inputModule = projectPath
                        .resolve("build/compileSync/wasmWasi/main/productionExecutable/optimized/wasm-wasmtime-test.wasm")
                        .loggedPathString()
                )

                assertComponentNew(
                    inputModule = embeddedModule,
                    component = projectPath.resolve(componentFile)
                )
            }
        }
    }

    @DisplayName("Check wasi target with wasmtime and wit declarations extracted from klib")
    @GradleTest
    fun wasiWasmtimeWithKlibWitDeclarations(gradleVersion: GradleVersion) {
        val lib = project("empty", gradleVersion) {
            addKgpToBuildScriptCompilationClasspath()

            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                @OptIn(ExperimentalWasmDsl::class)
                project.applyMultiplatform {
                    wasmWasi()

                    sourceSets.wasmWasiMain.get().compileSource(
                        """
                            package com.example.wasmlib
        
                            fun getGreeting(): String = "Hello from Lib"
                        """.trimIndent()
                    )
                }
            }
        }

        val libPublished = lib.publish(
            publisherConfiguration = PublisherConfiguration(
                group = "wasmlib",
                version = "0.0.1",
            )
        )

        val klibFile = libPublished.wasmWasiComponent.klib

        FileSystems.newFileSystem(klibFile.toPath(), null as ClassLoader?).use { fs ->
            val witFolder = fs.getPath("wit")
            Files.createDirectories(witFolder)
            Files.write(
                witFolder.resolve("dep.wit"),
                """
                    package com:example;

                    world dep {
                        import log: func(param: string);
                    }
                    """.trimIndent().toByteArray()
            )
        }

        project("empty", gradleVersion) {

            addKgpToBuildScriptCompilationClasspath()

            plugins {
                kotlin("multiplatform")
            }

            addPublishedProjectToRepositories(libPublished)

            buildScriptInjection {
                project.applyMultiplatform {
                    @OptIn(ExperimentalWasmDsl::class)
                    wasmWasi {
                        wasmtime()
                        binaries.executable()
                    }
                    @OptIn(ExperimentalWasmDsl::class)
                    sourceSets.wasmWasiMain.get().apply {
                        dependencies {
                            implementation(libPublished.rootCoordinate)
                        }

                        compileSource(
                            """
                                package my.pack.name
                                import com.example.wasmlib.getGreeting
                                fun main() {
                                    println(getGreeting() + ", Wasi")
                                }
                            """.trimIndent()
                        )
                    }
                }
            }

            build(":wasmWasiWasmtimeProductionRun") {
                assertTasksExecuted(
                    ":compileProductionExecutableKotlinWasmWasiComponentize",
                    ":wasmWasiWasmtimeProductionRun"
                )
                assertOutputContains("Hello from Lib, Wasi")
                val componentFile = "build/compileSync/wasmWasi/main/productionExecutable/component/$projectName.wasm"
                assertFileInProjectExists(componentFile)

                val embeddedModule = assertComponentEmbed(
                    witProjectPattern = "[^\\r\\n]+${lib.projectName}-wasm-wasi[^\\r\\n]*-wit",
                    inputModule = projectPath
                        .resolve("build/compileSync/wasmWasi/main/productionExecutable/optimized/$projectName.wasm")
                        .loggedPathString()
                )

                assertComponentNew(
                    inputModule = embeddedModule,
                    component = projectPath.resolve(componentFile)
                )
            }
        }
    }

    @DisplayName("Check wasi target with wasmtime and wit declarations from project dependency")
    @GradleTest
    fun wasiWasmtimeWithProjectDependencyWitDeclarations(gradleVersion: GradleVersion) {
        val lib = project("empty", gradleVersion) {
            addKgpToBuildScriptCompilationClasspath()

            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                @OptIn(ExperimentalWasmDsl::class)
                project.applyMultiplatform {
                    wasmWasi()

                    sourceSets.wasmWasiMain.get().compileSource(
                        """
                        package com.example.wasmlib
    
                        fun getLibGreeting(): String = "Hello from Project Lib"
                        """.trimIndent()
                    )
                }
            }

            val libWit = projectPath.resolve("wit")
            libWit.createDirectories()
            libWit.resolve("lib.wit").writeText(
                """
                package my:lib;

                world lib-world {
                    import lib-log: func(msg: string);
                }
                """.trimIndent()
            )
        }

        project("empty", gradleVersion) {
            include(
                lib,
                "lib",
                // need to compare output with regex
                // in this case symlinks can confuse
                useSymlink = false,
            )

            addKgpToBuildScriptCompilationClasspath()

            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                @OptIn(ExperimentalWasmDsl::class)
                project.applyMultiplatform {
                    wasmWasi {
                        wasmtime()
                        binaries.executable()
                    }

                    sourceSets.wasmWasiMain.get().apply {
                        dependencies {
                            implementation(project(":lib"))
                        }

                        compileSource(
                            """
                        package my.pack.name
        
                        import com.example.wasmlib.getLibGreeting
        
                        fun main() {
                            println(getLibGreeting() + ", Wasi")
                        }
                        """.trimIndent()
                        )
                    }
                }
            }

            val appWit = projectPath.resolve("wit")
            appWit.createDirectories()
            appWit.resolve("app.wit").writeText(
                """
                package my:app;

                world app-world {
                    import app-log: func(msg: string);
                }
                """.trimIndent()
            )

            build(":wasmWasiWasmtimeProductionRun") {
                assertTasksExecuted(
                    ":lib:compileKotlinWasmWasi",
                    ":compileProductionExecutableKotlinWasmWasiComponentize",
                    ":wasmWasiWasmtimeProductionRun"
                )
                assertOutputContains("Hello from Project Lib, Wasi")
                val componentFile = "build/compileSync/wasmWasi/main/productionExecutable/component/${projectName}.wasm"
                assertFileInProjectExists(componentFile)

                val moduleWithLibWit = assertComponentEmbed(
                    witProject = subProject("lib").projectPath.resolve("wit"),
                    inputModule = projectPath
                        .resolve("build/compileSync/wasmWasi/main/productionExecutable/optimized/${projectName}.wasm")
                        .loggedPathString()
                )

                val moduleWithAllWits = assertComponentEmbed(
                    witProject = appWit,
                    inputModule = moduleWithLibWit
                )

                assertComponentNew(
                    inputModule = moduleWithAllWits,
                    component = projectPath.resolve(componentFile)
                )
            }
        }
    }

    @DisplayName("Explicitly imported WASI component model function")
    @GradleTest
    fun wasiWasmtimeWithExplicitlyImportedWasiFunction(gradleVersion: GradleVersion) {
        project("wasm-wasmtime-test", gradleVersion) {
            val appWit = projectPath.resolve("wit")
            appWit.createDirectories()
            appWit.resolve("app.wit").writeText(
                """
                package my:app;

                world app-world {
                    import wasi:clocks/monotonic-clock@$WASI_PACKAGES_VERSION;
                }
                """.trimIndent()
            )
            appWit.addMonotonicClockWitDependency()

            projectPath.resolve("src/wasmWasiMain/kotlin/foo.kt").writeText(
                """
                package my.pack.name

                import kotlin.wasm.ExperimentalWasmInterop
                import kotlin.wasm.WasmImport

                @OptIn(ExperimentalWasmInterop::class)
                @WasmImport("wasi:clocks/monotonic-clock@$WASI_PACKAGES_VERSION", "now")
                private external fun monotonicNow(): Long

                fun main() {
                    println("Monotonic clock is " + if (monotonicNow() <= monotonicNow()) "monotonic" else "broken")
                }
                """.trimIndent()
            )

            build(":wasmWasiWasmtimeProductionRun") {
                assertTasksExecuted(
                    ":compileProductionExecutableKotlinWasmWasiComponentize",
                    ":wasmWasiWasmtimeProductionRun"
                )
                assertOutputContains("Monotonic clock is monotonic")

                val componentFile = "build/compileSync/wasmWasi/main/productionExecutable/component/wasm-wasmtime-test.wasm"
                assertFileInProjectExists(componentFile)

                val embeddedModule = assertComponentEmbed(
                    witProject = appWit,
                    inputModule = projectPath
                        .resolve("build/compileSync/wasmWasi/main/productionExecutable/optimized/wasm-wasmtime-test.wasm")
                        .loggedPathString()
                )

                assertComponentNew(
                    inputModule = embeddedModule,
                    component = projectPath.resolve(componentFile)
                )
            }
        }
    }

    @DisplayName("WASI component model function explicitly imported by a dependency")
    @GradleTest
    fun wasiWasmtimeWithWasiFunctionImportedByDependency(gradleVersion: GradleVersion) {
        val lib = project("empty", gradleVersion) {
            addKgpToBuildScriptCompilationClasspath()

            plugins {
                kotlin("multiplatform")
            }

            buildScriptInjection {
                @OptIn(ExperimentalWasmDsl::class)
                project.applyMultiplatform {
                    wasmWasi()

                    sourceSets.wasmWasiMain.get().compileSource(
                        """
                        package com.example.wasmlib
    
                        import kotlin.wasm.ExperimentalWasmInterop
                        import kotlin.wasm.WasmImport
    
                        @OptIn(ExperimentalWasmInterop::class)
                        @WasmImport("wasi:clocks/monotonic-clock@$WASI_PACKAGES_VERSION", "now")
                        private external fun monotonicNow(): Long
    
                        fun isClockMonotonic(): Boolean = monotonicNow() <= monotonicNow()
                    """.trimIndent()
                    )
                }
            }
            val libWit = projectPath.resolve("wit")
            libWit.createDirectories()
            libWit.resolve("lib.wit").writeText(
                """
                package my:lib;

                world lib-world {
                    import wasi:clocks/monotonic-clock@$WASI_PACKAGES_VERSION;
                }
                """.trimIndent()
            )
            libWit.addMonotonicClockWitDependency()
        }

        project("empty", gradleVersion) {
            addKgpToBuildScriptCompilationClasspath()

            plugins {
                kotlin("multiplatform")
            }

            include(
                lib,
                "lib",
                // need to compare output with regex
                // in this case symlinks can confuse
                useSymlink = false,
            )

            buildScriptInjection {
                project.applyMultiplatform {
                    @OptIn(ExperimentalWasmDsl::class)
                    wasmWasi {
                        wasmtime()
                        binaries.executable()
                    }

                    @OptIn(ExperimentalWasmDsl::class)
                    sourceSets.wasmWasiMain.get().apply {
                        dependencies {
                            implementation(project(":lib"))
                        }

                        compileSource(
                            """
                            package my.pack.name
            
                            import com.example.wasmlib.isClockMonotonic
            
                            fun main() {
                                println("Monotonic clock is " + if (isClockMonotonic()) "monotonic" else "broken")
                            }
                        """.trimIndent()
                        )
                    }
                }
            }

            build(":wasmWasiWasmtimeProductionRun") {
                assertTasksExecuted(
                    ":lib:compileKotlinWasmWasi",
                    ":compileProductionExecutableKotlinWasmWasiComponentize",
                    ":wasmWasiWasmtimeProductionRun"
                )
                assertOutputContains("Monotonic clock is monotonic")

                val componentFile = "build/compileSync/wasmWasi/main/productionExecutable/component/$projectName.wasm"
                assertFileInProjectExists(componentFile)

                val embeddedModule = assertComponentEmbed(
                    witProject = subProject("lib").projectPath.resolve("wit"),
                    inputModule = projectPath
                        .resolve("build/compileSync/wasmWasi/main/productionExecutable/optimized/$projectName.wasm")
                        .loggedPathString()
                )

                assertComponentNew(
                    inputModule = embeddedModule,
                    component = projectPath.resolve(componentFile)
                )
            }
        }
    }

    /**
     * Declares the `wasi:clocks` package as a dependency of this WIT project,
     * because every WIT project has to be self-contained.
     *
     * Only the `now` function of the `monotonic-clock` interface is declared,
     * because a component may import a subset of the functions of an interface provided by the host.
     */
    private fun Path.addMonotonicClockWitDependency() {
        val clocksPackage = resolve("deps/clocks")
        clocksPackage.createDirectories()
        clocksPackage.resolve("monotonic-clock.wit").writeText(
            """
            package wasi:clocks@$WASI_PACKAGES_VERSION;

            interface monotonic-clock {
                type instant = u64;

                now: func() -> instant;
            }
            """.trimIndent()
        )
    }

    private fun Path.loggedPathString(): String =
        toRealPath().absolutePathString()

    /**
     * Asserts that `wasm-tools component embed` embedded [witProject] into [inputModule]
     * and returns the path of the module with the embedded WIT declarations.
     */
    private fun BuildResult.assertComponentEmbed(witProject: Path, inputModule: String): String =
        assertComponentEmbed(Regex.escape(witProject.loggedPathString()), inputModule)

    /**
     * Asserts that `wasm-tools component embed` embedded a WIT project
     * whose path matches the [witProjectPattern] regular expression into [inputModule]
     * and returns the path of the module with the embedded WIT declarations.
     *
     * WIT projects extracted from klibs are located in the Gradle artifact transforms cache,
     * so their paths can be matched only by a pattern.
     */
    private fun BuildResult.assertComponentEmbed(witProjectPattern: String, inputModule: String): String {
        val embedCommand = Regex(
            "component embed $witProjectPattern " + Regex.escape(inputModule) + Regex.escape(" -o ") + "([^\\r\\n]+)"
        )
        val embed = embedCommand.find(output)
        if (embed == null) printBuildOutput()
        assertNotNull(embed, "Build output does not contain any line matching '$embedCommand' regex.")
        return embed.groupValues[1]
    }

    /**
     * Asserts that `wasm-tools component new` turned [inputModule] into [component].
     */
    private fun BuildResult.assertComponentNew(
        inputModule: String,
        component: Path,
        adaptModule: String = "[^\\r\\n]+",
    ) {
        val adaptRegex = if (adaptModule == "[^\\r\\n]+") adaptModule else Regex.escape(adaptModule)
        assertOutputContains(
            Regex(
                Regex.escape("component new $inputModule") +
                        " --adapt wasi_snapshot_preview1=$adaptRegex -o " +
                        Regex.escape(component.loggedPathString())
            )
        )
    }

    @DisplayName("Check wasi target run")
    @GradleTest
    fun wasiRun(gradleVersion: GradleVersion) {
        project("new-mpp-wasm-wasi-test", gradleVersion) {
            build(":wasmWasiNodeDevelopmentRun") {
                assertOutputContains("Hello from Wasi")
            }
        }
    }

    @DisplayName("Check wasi target with binaryen")
    @GradleTest
    fun wasiTargetWithBinaryen(gradleVersion: GradleVersion) {
        project("new-mpp-wasm-wasi-test", gradleVersion) {
            buildGradleKts.modify {
                it.replace("wasmWasi {", "wasmWasi {\nbinaries.executable()")
            }

            build("assemble") {
                assertTasksExecuted(":compileProductionExecutableKotlinWasmWasi")
                assertTasksExecuted(":compileProductionExecutableKotlinWasmWasiOptimize")

                val original =
                    projectPath.resolve("build/compileSync/wasmWasi/main/productionExecutable/kotlin/new-mpp-wasm-wasi-test.wasm")
                val optimized =
                    projectPath.resolve("build/compileSync/wasmWasi/main/productionExecutable/optimized/new-mpp-wasm-wasi-test.wasm")
                assertTrue {
                    Files.size(original) > Files.size(optimized)
                }
            }
        }
    }

    @DisplayName("Wasi library")
    @GradleTest
    fun wasiLibrary(gradleVersion: GradleVersion) {
        project("wasm-wasi-library", gradleVersion) {

            build(":build") {
                assertTasksExecuted(":compileProductionLibraryKotlinWasmWasi")
                assertTasksExecuted(":compileKotlinWasmWasi")
                assertTasksExecuted(":wasmWasiNodeTest")
                assertTasksExecuted(":wasmWasiNodeProductionLibraryDistribution")

                val dist = "build/dist/wasmWasi/productionLibrary"
                assertFileExists(projectPath.resolve("$dist/foo.txt"))
                assertFileExists(projectPath.resolve("$dist/wasm-wasi-library.wasm"))
                assertFileExists(projectPath.resolve("$dist/wasm-wasi-library.wasm.map"))
                assertFileExists(projectPath.resolve("$dist/wasm-wasi-library.mjs"))
            }
        }
    }
}

private const val WASI_PACKAGES_VERSION = "0.2.12"
