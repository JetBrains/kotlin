/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText
import kotlin.io.path.deleteExisting
import kotlin.io.path.writeText
import kotlin.test.assertEquals

@DisplayName("KT-55389: Kotlin classes are compiled into the Java module of a JVM compilation")
@MustRunOnChangesInGradle
@JvmGradlePluginTests
class JpmsKotlinClassesIT : KGPBaseTest() {

    @DisplayName("Java sources of a named module can reference Kotlin classes of the same module")
    @GradleTest
    fun javaModuleUsesKotlinClasses(gradleVersion: GradleVersion) {
        project("jpmsKotlinClasses", gradleVersion) {
            build("run") {
                assertEquals("Hello from org.example.app", output.lines().single { it.startsWith("Hello from") })
            }
        }
    }

    @DisplayName("Java sources compile when compileKotlin is restored from the build cache")
    @GradleTest
    fun compileKotlinFromBuildCache(gradleVersion: GradleVersion) {
        project(
            "jpmsKotlinClasses",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true),
        ) {
            enableLocalBuildCache(workingDir.resolve("build-cache"))
            // Only compileKotlin is restored, so compileJava runs against the restored Kotlin classes
            buildGradleKts.appendText(
                """
                |
                |tasks.named<JavaCompile>("compileJava") { outputs.cacheIf { false } }
                """.trimMargin()
            )
            build("assemble") {
                assertTasksPackedToCache(":compileKotlin")
            }

            build("clean", "assemble") {
                assertTasksFromCache(":compileKotlin")
                assertTasksExecuted(":compileJava")
            }
        }
    }

    @DisplayName("compileJava is up to date in a second build and restored from the build cache after clean")
    @GradleTest
    fun compileJavaUpToDateAndFromBuildCache(gradleVersion: GradleVersion) {
        project(
            "jpmsKotlinClasses",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(buildCacheEnabled = true),
        ) {
            enableLocalBuildCache(workingDir.resolve("build-cache"))
            build("assemble") {
                assertTasksPackedToCache(":compileKotlin", ":compileJava")
            }

            build("assemble") {
                assertTasksUpToDate(":compileKotlin", ":compileJava")
            }

            build("clean", "run") {
                assertTasksFromCache(":compileKotlin", ":compileJava")
                assertEquals("Hello from org.example.app", output.lines().single { it.startsWith("Hello from") })
            }
        }
    }

    @DisplayName("A --patch-module for the same module in the build script is used instead")
    @GradleTest
    fun patchModuleInBuildScript(gradleVersion: GradleVersion) {
        project("jpmsKotlinClasses", gradleVersion) {
            buildGradleKts.appendText(
                """
                |
                |tasks.named<JavaCompile>("compileJava") {
                |    val kotlinClasses = tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileKotlin")
                |        .flatMap { it.destinationDirectory }
                |    options.compilerArgumentProviders.add(CommandLineArgumentProvider {
                |        listOf("--patch-module", "org.example.app=${'$'}{kotlinClasses.get().asFile.path}")
                |    })
                |}
                """.trimMargin()
            )

            build("run") {
                assertEquals("Hello from org.example.app", output.lines().single { it.startsWith("Hello from") })
            }
        }
    }

    @DisplayName("A --patch-module for the same module added in a doFirst action of the build script is used instead")
    @GradleTest
    fun patchModuleInDoFirst(gradleVersion: GradleVersion) {
        project("jpmsKotlinClasses", gradleVersion) {
            buildGradleKts.appendText(
                """
                |
                |tasks.named<JavaCompile>("compileJava") {
                |    val kotlinClasses = tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileKotlin")
                |        .flatMap { it.destinationDirectory }
                |    doFirst {
                |        options.compilerArgs = options.compilerArgs +
                |            listOf("--patch-module", "org.example.app=${'$'}{kotlinClasses.get().asFile.path}")
                |    }
                |}
                """.trimMargin()
            )

            build("run") {
                assertEquals("Hello from org.example.app", output.lines().single { it.startsWith("Hello from") })
            }
        }
    }

    @DisplayName("A Java-only module compiles with -Xlint:path -Werror")
    @GradleTest
    fun javaOnlyModuleWithPathLint(gradleVersion: GradleVersion) {
        project("jpmsKotlinClasses", gradleVersion) {
            kotlinSourcesDir().resolve("org/example/app/Greeter.kt").deleteExisting()
            javaSourcesDir().resolve("org/example/app/Main.java").writeText(
                """
                |package org.example.app;
                |
                |public class Main {
                |    public static void main(String[] args) {
                |        System.out.println("Hello from " + Main.class.getModule().getName());
                |    }
                |}
                """.trimMargin()
            )
            buildGradleKts.appendText(
                """
                |
                |tasks.named<JavaCompile>("compileJava") { options.compilerArgs.addAll(listOf("-Xlint:path", "-Werror")) }
                """.trimMargin()
            )

            build("run") {
                assertEquals("Hello from org.example.app", output.lines().single { it.startsWith("Hello from") })
            }
        }
    }
}
