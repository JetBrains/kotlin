/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven

import org.jetbrains.kotlin.maven.test.*
import org.junit.jupiter.api.DisplayName

@DisplayName("Basic compilation")
class BasicCompilationIT : KotlinMavenTestBase() {

    @MavenTest
    @DisplayName("Mixed Kotlin/Java project compiles and tests pass")
    fun testKotlinJavaCompilation(mavenVersion: TestVersions.Maven) {
        testProject("test-helloworld", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertTestsPassed(1)
                assertJarExistsAndNotEmpty("target/test-helloworld-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    @DisplayName("Kotlin script (.kts) source files compile")
    fun testKtsScriptCompilation(mavenVersion: TestVersions.Maven) {
        testProject("test-helloworld-kts", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertTestsPassed(1)
                assertJarExistsAndNotEmpty("target/test-helloworld-kts-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    @DisplayName("Test sources can access internal declarations from main")
    fun testAccessToInternal(mavenVersion: TestVersions.Maven) {
        testProject("test-accessToInternal", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertTestsPassed(2)
                assertJarExistsAndNotEmpty("target/test-accessToInternal-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    @DisplayName("Custom module name is set in compiled output")
    fun testChangeModuleName(mavenVersion: TestVersions.Maven) {
        testProject("test-moduleName", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertTestsPassed(1)
                assertJarExistsAndNotEmpty("target/test-moduleName-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    @DisplayName("Default module name is derived from Maven coordinates")
    fun testModuleNameDefault(mavenVersion: TestVersions.Maven) {
        testProject("test-moduleNameDefault", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertTestsPassed(1)
                assertJarExistsAndNotEmpty("target/test-moduleNameDefault-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    @DisplayName("Project with kotlin-reflect compiles and passes test")
    fun testReflection(mavenVersion: TestVersions.Maven) {
        testProject("test-reflection", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertTestsPassed(1)
                assertJarExistsAndNotEmpty("target/test-reflection-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    @DisplayName("Kotlin runtime JARs contain correct version and component attributes in manifest")
    fun testKotlinVersionInManifest(mavenVersion: TestVersions.Maven) {
        testProject("test-kotlin-version-in-manifest", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertTestsPassed(1)
                assertJarExistsAndNotEmpty("target/test-kotlin-version-in-manifest-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    @DisplayName("Additional compile source roots are included, detecting conflicting overloads")
    fun testRespectCompileSourceRoot(mavenVersion: TestVersions.Maven) {
        testProject("test-respect-compile-source-root", mavenVersion) {
            build(
                "package",
                expectedToFail = true
            ) {
                assertCompilationFailed()
                assertBuildLogContains("Conflicting overloads:")
            }
        }
    }

    @MavenTest
    @DisplayName("Kotlin-bom aligns all Kotlin dependency versions")
    fun testKotlinBom(mavenVersion: TestVersions.Maven) {
        testProject("test-bom", mavenVersion) {
            build(
                "dependency:tree",
                "package",
                expectedToFail = false
            ) {
                assertFileExists("target/test-kotlin-bom-1.0-SNAPSHOT.jar")
                val kotlinVersion = context.kotlinVersion
                assertDependencyTreeContains("org.jetbrains.kotlin", "kotlin-stdlib", kotlinVersion)
                assertDependencyTreeContains("org.jetbrains.kotlin", "kotlin-stdlib-jdk7", kotlinVersion)
                assertDependencyTreeContains("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", kotlinVersion)
                assertDependencyTreeContains("org.jetbrains.kotlin", "kotlin-reflect", kotlinVersion)
                assertDependencyTreeContains("org.jetbrains.kotlin", "kotlin-test", kotlinVersion, scope = "test")
                assertDependencyTreeContains(
                    "org.jetbrains.kotlin",
                    "kotlin-test-junit5",
                    kotlinVersion,
                    scope = "test"
                )
            }
        }
    }
}
