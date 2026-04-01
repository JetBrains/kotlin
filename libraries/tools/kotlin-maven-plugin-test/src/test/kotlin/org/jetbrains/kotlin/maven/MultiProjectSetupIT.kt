/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven

import org.jetbrains.kotlin.maven.test.*
import org.junit.jupiter.api.DisplayName

@DisplayName("Multi-module project setup")
class MultiProjectSetupIT : KotlinMavenTestBase() {

    @MavenTest
    @DisplayName("Single child module project compiles")
    fun testMultimodule(mavenVersion: TestVersions.Maven) {
        testProject("test-multimodule", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertFileExists("sub/target/classes/org/jetbrains/HelloWorldKt.class")
            }
        }
    }

    @MavenTest
    @DisplayName("In-process compilation reuses classloaders across modules")
    fun testMultimoduleInProcess(mavenVersion: TestVersions.Maven) {
        testProject("test-multimodule-in-process", mavenVersion) {
            build(
                "package", "-X",
                environmentVariables = mapOf("MAVEN_OPTS" to "-XX:MaxMetaspaceSize=300M"),
                expectedToFail = false
            ) {
                // 1 classloader for modules 1, 3, 4, 5 (same compiler config)
                // 1 classloader for module 2 (has all-open compiler plugin)
                assertBuildLogLineCount("[DEBUG] Creating classloader", expectedCount = 2)
            }
        }
    }

    @MavenTest
    @DisplayName("Multi-module with relative sourceDirs configuration compiles")
    fun testMultimoduleSrcDir(mavenVersion: TestVersions.Maven) {
        testProject("test-multimodule-srcdir", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertFileExists("sub/target/classes/org/jetbrains/HelloWorldKt.class")
            }
        }
    }

    @MavenTest
    @DisplayName("Multi-module with absolute sourceDirs configuration compiles")
    fun testMultimoduleSrcDirAbsolute(mavenVersion: TestVersions.Maven) {
        testProject("test-multimodule-srcdir-absolute", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertFileExists("sub/target/classes/org/jetbrains/HelloWorldKt.class")
            }
        }
    }
}
