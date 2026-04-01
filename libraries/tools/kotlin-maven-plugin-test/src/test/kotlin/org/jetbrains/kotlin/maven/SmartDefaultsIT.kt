/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven

import org.jetbrains.kotlin.maven.test.*
import org.junit.jupiter.api.DisplayName

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
}