/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS

@DisplayName("Compiler plugin incremental compilation")
@OtherGradlePluginTests
abstract class CompilerPluginsIncrementalIT : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(
            incremental = true
        )

    @DisabledOnOs(OS.WINDOWS, disabledReason = "Kotlin compiler holds an open file descriptor to plugin jar file")
    @DisplayName("KT-38570: After changing compiler plugin code, next incremental build picks it up")
    @GradleTest
    open fun afterChangeInPluginBuildDoesIncrementalProcessing(gradleVersion: GradleVersion) {
        project("incrementalChangeInPlugin".prefix, gradleVersion) {
            val classesDirectory = subProject("library").kotlinClassesDir("main")
            build("assemble") {
                assertClassDeclarationsContain(
                    classesDirectory, "library.SomeClass",
                    "public java.lang.String myMethod();"
                )
                assertClassDeclarationsContain(
                    classesDirectory, "library.SomeInterface",
                    "public abstract java.lang.String myMethod();"
                )
            }

            subProject("plugin")
                .kotlinSourcesDir()
                .resolve("test/compiler/plugin/MyMethodGenerator.kt")
                .modify {
                    it.replace("\"myMethod\"", "\"myNewMethod\"")
                }

            build("assemble") {
                assertClassDeclarationsContain(
                    classesDirectory, "library.SomeClass",
                    "public java.lang.String myNewMethod();"
                )
                assertClassDeclarationsContain(
                    classesDirectory, "library.SomeInterface",
                    "public abstract java.lang.String myNewMethod();"
                )
            }
        }
    }

    @DisabledOnOs(OS.WINDOWS, disabledReason = "Kotlin compiler holds an open file descriptor to plugin jar file")
    @DisplayName("KT-53644: Changes to lombok annotation parameters should be detected and used in incremental compilation")
    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.MAX_SUPPORTED) // Gradle version is irrelevant
    fun testLombokAnnotationParameterChange(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("jvm")
                kotlin("plugin.lombok")
                id("io.freefair.lombok")
            }
            javaSourcesDir().source("City.java") {
                """
                |import lombok.Builder;
                |import lombok.Getter;
                |import lombok.Singular;
                |import lombok.ToString;
                |
                |import java.util.SortedMap;
                |
                |@Builder
                |@Getter
                |@ToString
                |public class City {
                |   @Singular("record")
                |   private SortedMap<String, Integer> manual;
                |}    
                """.trimMargin()
            }
            kotlinSourcesDir().source("main.kt") {
                """
                |fun test() {
                |   val a = City.builder().record("Fontanka", 76).build()
                |}
                """.trimMargin()
            }
            build("compileKotlin")
            javaSourcesDir().source("City.java") {
                """
                |import lombok.Builder;
                |import lombok.Getter;
                |import lombok.Singular;
                |import lombok.ToString;
                |
                |import java.util.SortedMap;
                |
                |@Builder
                |@Getter
                |@ToString
                |public class City {
                |   @Singular("re")
                |   private SortedMap<String, Integer> manual;
                |}    
                """.trimMargin()
            }
            buildAndFail("compileKotlin") {
                assertOutputContains("main.kt:2:27 Unresolved reference 'record' on receiver of type 'City.CityBuilder'.")
            }
        }
    }

    private val String.prefix get() = "compilerPlugins/$this"
}

class CompilerPluginsK2IncrementalIT : CompilerPluginsIncrementalIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copyEnsuringK2()
}
