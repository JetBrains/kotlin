/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven

import org.jetbrains.kotlin.maven.test.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.Path
import kotlin.io.path.reader

@DisplayName("JDK compatibility")
class JdkCompatibilityIT : KotlinMavenTestBase() {

    @MavenTest
    @DisplayName("Compilation fails on JDK 1.8 when code uses JDK 9+ API")
    fun testCustomJdk8Failure(mavenVersion: TestVersions.Maven) {
        testProject("test-customJdk", mavenVersion) {
            build(
                "package",
                expectedToFail = true,
                buildOptions = buildOptions.copy(
                    javaVersion = TestVersions.Java.JDK_1_8,
                    extraMavenProperties = mapOf("kotlinCompilerJdk" to context.getJavaHomeString(TestVersions.Java.JDK_1_8))
                )
            ) {
                assertCompilationFailed()
                assertBuildLogContains(
                    "[INFO] Overriding JDK home path with",
                    "Unresolved reference 'StackWalker'"
                )
            }
        }
    }

    @MavenTest
    @DisplayName("Compilation succeeds on JDK 17 with JDK 9+ API")
    fun testCustomJdk17Success(mavenVersion: TestVersions.Maven) {
        testProject("test-customJdk", mavenVersion) {
            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = TestVersions.Java.JDK_17,
                    extraMavenProperties = mapOf("kotlinCompilerJdk" to context.getJavaHomeString(TestVersions.Java.JDK_17))
                )
            ) {
                assertJarExistsAndNotEmpty("target/test-customJdk-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    @DisplayName("Compilation with JDK 8 internal classpath dependencies succeeds")
    fun testJava8Classpath(mavenVersion: TestVersions.Maven) {
        testProject("java8/test-classpath", mavenVersion) {
            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = TestVersions.Java.JDK_1_8,
                    extraMavenProperties = mapOf("kotlinCompilerJdk" to context.getJavaHomeString(TestVersions.Java.JDK_1_8))
                )
            ) {
                assertTestsPassed(1)
                assertJarExistsAndNotEmpty("target/test-classpath-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    @DisplayName("Runtime image via jlink includes all required Kotlin and app modules")
    fun testJava9JlinkModularArtifacts(mavenVersion: TestVersions.Maven) {
        testProject("java9/test-jlink-modular-artifacts", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertFileExists("jlinked/target/maven-jlink/release")

                val releaseFile = Path(basedir).resolve("jlinked/target/maven-jlink/release")
                val props = java.util.Properties()
                releaseFile.reader().use { props.load(it) }

                var modules = props.getProperty("MODULES")
                    ?: fail("MODULES property missing from release file")

                if (modules.startsWith("\"") && modules.endsWith("\"")) {
                    modules = modules.substring(1, modules.length - 1)
                }

                val moduleSet = modules.split(" ").toSet()
                for (module in listOf(
                    "java.base",
                    "kotlin.stdlib",
                    "kotlin.stdlib.jdk7",
                    "kotlin.stdlib.jdk8",
                    "org.test.modularApp"
                )) {
                    assertTrue(module in moduleSet) {
                        "Expected to find $module in image modules: $modules"
                    }
                }
            }
        }
    }

    @MavenTest
    @DisplayName("Mixed Kotlin/Java source roots in Java 9 module compile correctly")
    fun testJava9SourceRootsRegisteredSeveralTimes(mavenVersion: TestVersions.Maven) {
        testProject("java9/test-sourceRootsRegisteredSeveralTimes", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertJarExistsAndNotEmpty("target/test-sourceRootsRegisteredSeveralTimes-1.0-SNAPSHOT.jar")
                assertFileExists("target/classes/foo/bar/Foo.class")
                assertFileExists("target/classes/koo/bar/Koo.class")
                assertFileExists("target/classes/module-info.class")
            }
        }
    }

    @MavenTest
    @DisplayName("KT-55709: Incremental compilation with many files does not fail on JDK 17")
    fun testJava17FileSnapshotMapOverflow(mavenVersion: TestVersions.Maven) {
        testProject("java17/test-fileSnapshotMap-overflow", mavenVersion) {
            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(javaVersion = TestVersions.Java.JDK_17)
            ) {
                assertJarExistsAndNotEmpty("app/target/test-fileSnapshotMap-overflow-app-1.0-SNAPSHOT.jar")
            }
        }
    }
}
