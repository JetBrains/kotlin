/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven

import org.jetbrains.kotlin.maven.test.*
import org.junit.jupiter.api.DisplayName

class CompilerOptionsIT : KotlinMavenTestBase() {

    @MavenTest
    @DisplayName("apiVersion restricts available API")
    fun testApiVersion(mavenVersion: TestVersions.Maven) {
        testProject("test-apiVersion", mavenVersion) {
            build(
                "package",
                expectedToFail = true
            ) {
                assertCompilationFailed()
                assertBuildLogContains("Unresolved reference 'new'")
            }
        }
    }

    @MavenTest
    @DisplayName("LanguageVersion restricts available language features")
    fun testLanguageVersion(mavenVersion: TestVersions.Maven) {
        testProject("test-languageVersion", mavenVersion) {
            build(
                "package",
                expectedToFail = true
            ) {
                assertCompilationFailed()
                assertBuildLogContains(
                    "The feature \"break continue in inline lambdas\" is only available since language version 2.2"
                )
            }
        }
    }

    @MavenTest
    @DisplayName("Invalid jvmTarget value is rejected and compilation fails")
    fun testJvmTarget(mavenVersion: TestVersions.Maven) {
        testProject("test-jvmTarget", mavenVersion) {
            build(
                "package",
                expectedToFail = true
            ) {
                assertBuildLogContains("Unknown -jvm-target value: 1.4")
            }
        }
    }

    @MavenTest
    @DisplayName("Extra compiler arguments in configuration block are passed through")
    fun testExtraArguments(mavenVersion: TestVersions.Maven) {
        testProject("test-extraArguments", mavenVersion) {
            build(
                "package", "-X",
                expectedToFail = false
            ) {
                assertCompilerArgsContain(
                    "-Xno-inline", "-Xno-optimize", "-Xno-call-assertions",
                    "-Xno-param-assertions",
                )
                assertTestsPassed(1)
                assertJarExistsAndNotEmpty("target/test-extraArguments-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    @DisplayName("nowarn suppresses compiler warnings")
    fun testSuppressWarnings(mavenVersion: TestVersions.Maven) {
        testProject("test-suppressWarnings", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertBuildLogDoesNotContain("Redundant '?'")
                assertJarExistsAndNotEmpty("target/test-helloworld-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    @DisplayName("Empty compiler argument in configuration is rejected and compilation fails")
    fun testEmptyArgument(mavenVersion: TestVersions.Maven) {
        testProject("test-empty-argument", mavenVersion) {
            build(
                "package",
                expectedToFail = true
            ) {
                assertBuildLogContains("Empty compiler argument passed in the <configuration> section")
            }
        }
    }
}
