/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven

import org.jetbrains.kotlin.maven.test.*
import org.junit.jupiter.api.DisplayName

@DisplayName("Smart defaults")
class SmartDefaultsIT : KotlinMavenTestBase() {

    @MavenTest
    @DisplayName("Extensions enabled activates smart defaults")
    fun testSmartDefaultsEnabled(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-enabled", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertSmartDefaultsEnabled()
                assertStdlibAutoAdded()
                assertFileExists("target/classes/org/jetbrains/HelloWorldKt.class")
            }
        }
    }

    @MavenTest
    @DisplayName("Extensions enabled configures mixed Kotlin/Java compilation automatically")
    fun testExtensionsEnableMixedKotlinJava(mavenVersion: TestVersions.Maven) {
        testProject("test-enable-extensions", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertSmartDefaultsEnabled()
                assertTestsPassed(4)
                assertJarExistsAndNotEmpty("target/test-enable-extensions-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    @DisplayName("KT-84163: Smart defaults respects Maven <sourceDirectory> and <testSourceDirectory> overrides")
    fun testSmartDefaultsBuildSourceDirOverrides(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-build-source-dirs", mavenVersion) {
            build("compile", "test-compile") {
                assertSmartDefaultsEnabled()
                // Custom source root must be compiled
                assertFileExists("target/classes/sample/CustomMain.class")
                // Default src/main/kotlin must NOT be compiled (user overrode <sourceDirectory>)
                assertFileDoesNotExist("target/classes/sample/DefaultMain.class") {
                    "Default main source root (src/main/kotlin) was compiled even though <sourceDirectory> was overridden"
                }

                // Custom test source root must be compiled
                assertFileExists("target/test-classes/sample/CustomTest.class")
                // Default src/test/kotlin must NOT be compiled (user overrode <testSourceDirectory>)
                assertFileDoesNotExist("target/test-classes/sample/DefaultTest.class") {
                    "Default test source root (src/test/kotlin) was compiled even though <testSourceDirectory> was overridden"
                }
            }
        }
    }

    @MavenTest
    @DisplayName("Smart defaults respects user-configured source directories")
    fun testSmartDefaultsCustomSourceDirs(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-custom-source-dirs", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertSmartDefaultsEnabled()
                assertFileExists("target/classes/test/MainKt.class")
            }
        }
    }

    @MavenTest
    @DisplayName("Smart defaults are off when extensions are not enabled")
    fun testSmartDefaultsDisabled(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-disabled", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertSmartDefaultsNotEnabled()
                assertStdlibNotAutoAdded()
                assertFileExists("target/classes/test/MainKt.class")
            }
        }
    }

    @MavenTest
    @DisplayName("Smart defaults can be disabled via Maven property even with extensions enabled")
    fun testSmartDefaultsDisabledViaProperty(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-disabled-via-property", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertSmartDefaultsNotEnabled()
                assertStdlibNotAutoAdded()
                assertFileExists("target/classes/test/MainKt.class")
            }
        }
    }

    @MavenTest
    @DisplayName("Smart defaults auto-configures src/main|test/kotlin source roots")
    fun testSmartDefaultsSourceRoots(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-source-roots", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertSmartDefaultsEnabled()
                assertStdlibAutoAdded()
                assertTestsPassed(2)
                assertFileExists("target/classes/test/Calculator.class")
                assertFileExists("target/test-classes/test/CalculatorTest.class")
            }
        }
    }

    @MavenTest
    @DisplayName("Smart defaults adds stdlib to dependency tree with plugin version")
    fun testSmartDefaultsStdlib(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-stdlib", mavenVersion) {
            build(
                "dependency:tree",
                "package",
                expectedToFail = false
            ) {
                assertSmartDefaultsEnabled()
                assertStdlibAutoAdded()
                assertDependencyTreeContains("org.jetbrains.kotlin", "kotlin-stdlib", context.kotlinVersion)
                assertFileExists("target/classes/test/MainKt.class")
            }
        }
    }

    @MavenTest
    @DisplayName("Smart defaults skips stdlib auto-add when already declared in pom")
    fun testSmartDefaultsStdlibExists(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-stdlib-exists", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertSmartDefaultsEnabled()
                assertStdlibNotAutoAdded()
                assertFileExists("target/classes/test/MainKt.class")
            }
        }
    }

    @MavenTest
    @DisplayName("KT-85146: Auto-added stdlib has correct resolved artifact scope")
    fun testSmartDefaultsStdlibResolvedScope(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-stdlib-scope", mavenVersion) {
            build(
                "dependency:tree",
                "package",
                expectedToFail = false
            ) {
                assertSmartDefaultsEnabled()
                assertStdlibAutoAdded()
                assertDependencyTreeContains("org.jetbrains.kotlin", "kotlin-stdlib", context.kotlinVersion)
                assertZipContains(
                    "target/test-smart-defaults-stdlib-scope-1.0-lambda.zip",
                    "lib/kotlin-stdlib-${context.kotlinVersion}.jar"
                )
            }
        }
    }

    @MavenTest
    @DisplayName("Check order of auto-bound goals in Kotlin+Java project")
    fun testSmartDefaultsAutoBindOrder(mavenVersion: TestVersions.Maven) {
        testProject("test-smart-defaults-auto-bind-order", mavenVersion) {
            build(
                "package", "-X",
                expectedToFail = false
            ) {
                assertSmartDefaultsEnabled()
                assertGoalOrderInBuildPlan(
                    executedFirst = "org.jetbrains.kotlin:kotlin-maven-plugin", goalFirst = ":compile",
                    executedSecond = "org.apache.maven.plugins:maven-compiler-plugin", goalSecond = ":compile",
                )
                assertGoalOrderInBuildPlan(
                    executedFirst = "org.jetbrains.kotlin:kotlin-maven-plugin", goalFirst = ":test-compile",
                    executedSecond = "org.apache.maven.plugins:maven-compiler-plugin", goalSecond = ":testCompile",
                )
                assertTestsPassed(2)
            }
        }
    }

    @MavenTest
    @DisplayName("KAPT applied properly for mixed Kotlin+Java project with smart defaults enabled")
    fun testSmartDefaultsInMixedProjectWithKapt(mavenVersion: TestVersions.Maven) {
        val buildOptions = if (isWindowsHost) buildOptions.copy(useKotlinDaemon = false) else buildOptions
        testProject("test-smart-defaults-kapt", mavenVersion, buildOptions) {
            build("verify") {
                assertSmartDefaultsEnabled()

                assertJarExistsAndNotEmpty("app/target/app-1.0-SNAPSHOT.jar")

                // KAPT ran and generated a Java source file from @Anno on KotlinService
                assertFileExists(
                    "app/target/generated-sources/kapt/compile/app/KotlinServiceGenerated.java"
                ) { "KAPT-generated Java source file was not found" }

                // KAPT ran and generated a Kotlin extension file from @Anno on KotlinService
                assertFileExists(
                    "app/target/generated-sources/kaptKotlin/compile/KotlinServiceExtensions.kt"
                ) { "KAPT-generated Kotlin extension file was not found" }

                // 2 tests in the `KotlinServiceTest`
                assertTestsPassed(2)
                // 3 tests in the `JavaConsumerTest`
                assertTestsPassed(3)
            }
        }
    }

    @MavenTest
    @DisplayName("Execution-level source dirs override smart defaults")
    fun testExecutionLevelSourceDirsOverrides(mavenVersion: TestVersions.Maven) {
        val buildOptions = if (isWindowsHost) buildOptions.copy(useKotlinDaemon = false) else buildOptions
        testProject("test-smart-defaults-execution-source-dirs", mavenVersion, buildOptions) {
            build("compile", "test-compile") {
                assertSmartDefaultsEnabled()

                // compile should use only explicitly specified execution-level sourceDirs if present
                assertFileExists("target/classes/sample/CustomMain.class")
                assertFileDoesNotExist("target/classes/sample/DefaultMain.class") {
                    "Default main source root was compiled unexpectedly"
                }

                // test-compile should use only explicitly specified execution-level sourceDirs if present
                assertFileExists("target/test-classes/sample/CustomTest.class")
                assertFileDoesNotExist("target/test-classes/sample/DefaultTest.class") {
                    "Default test source root was compiled unexpectedly"
                }
            }
        }
    }

    @MavenTest
    @DisplayName("Empty execution-level source dirs fallback to smart defaults")
    fun testEmptyExecutionLevelSourceDirsFallbacks(
        mavenVersion: TestVersions.Maven,
    ) {
        val buildOptions = if (isWindowsHost) buildOptions.copy(useKotlinDaemon = false) else buildOptions
        testProject("test-smart-defaults-empty-execution-source-dirs", mavenVersion, buildOptions) {
            build("compile", "test-compile") {
                assertSmartDefaultsEnabled()

                // empty explicit execution-level sourceDirs should be treated as absent
                assertFileExists("target/classes/sample/DefaultMain.class")
                assertFileExists("target/test-classes/sample/DefaultTest.class")
            }
        }
    }

    @MavenTest
    @DisplayName("Execution-level source dirs do not produce 'duplicate source root' warnings")
    fun testExecutionLevelSourceDirsWarnings(
        mavenVersion: TestVersions.Maven,
    ) {
        val buildOptions = if (isWindowsHost) buildOptions.copy(useKotlinDaemon = false) else buildOptions
        testProject("test-smart-defaults-execution-source-dirs-no-duplicates", mavenVersion, buildOptions) {
            build("compile", "test-compile") {
                assertSmartDefaultsEnabled()
                assertBuildLogDoesNotContain("Duplicate source root")

                // compile should include all explicitly configured source directories
                assertFileExists("target/classes/sample/CustomMain.class")
                assertFileExists("target/classes/sample/DefaultMain.class")

                // test-compile should include all explicitly configured test source directories
                assertFileExists("target/test-classes/sample/CustomTest.class")
                assertFileExists("target/test-classes/sample/DefaultTest.class")
            }
        }
    }

    @MavenTest
    @DisplayName("Execution-level source dirs still compile additional compile source roots")
    fun testExecutionLevelSourceDirsRespectCompileSourceRoots(
        mavenVersion: TestVersions.Maven,
    ) {
        val buildOptions = if (isWindowsHost) buildOptions.copy(useKotlinDaemon = false) else buildOptions
        testProject("test-smart-defaults-execution-source-dirs-compile-source-roots", mavenVersion, buildOptions) {
            build("package") {
                assertSmartDefaultsEnabled()

                assertFileExists("app/target/classes/sample/CustomMain.class")
                assertFileExists("app/target/classes/sample/GeneratedByPlugin.class")
                assertFileDoesNotExist("app/target/classes/sample/DefaultMain.class") {
                    "Default main source root was compiled unexpectedly"
                }
            }
        }
    }
}
