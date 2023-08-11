/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.KOTLIN_VERSION
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import java.io.File
import java.util.*
import kotlin.io.path.appendText
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@DisplayName("Tests for K/N builds with external dependencies")
@NativeGradlePluginTests
internal class NativeExternalDependenciesIT : KGPBaseTest() {

    @DisplayName("K/N shouldn't contain any external dependencies by default")
    @GradleTest
    fun shouldNotUseExternalDependencies(gradleVersion: GradleVersion) {
        buildProjectWithDependencies(gradleVersion) { externalDependenciesText ->
            assertEquals(
                """
                |0 native-external-dependencies
                |1 org.jetbrains.kotlin:kotlin-stdlib[$KOTLIN_VERSION] #0[$KOTLIN_VERSION]
                |
                """.trimMargin(),
                externalDependenciesText
            )
        }
    }

    @DisplayName("Should build with ktor 2.3.3 and coroutines 1.7.2")
    @GradleTest
    fun shouldUseOldKtorAndCoroutinesExternalDependencies(gradleVersion: GradleVersion) {

        buildProjectWithDependencies(
            gradleVersion,
            "io.ktor:ktor-io:2.3.3",
            "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2"
        ) { externalDependenciesText ->
            assertNotNull(externalDependenciesText)

            assertEquals("""
            0 native-external-dependencies
            1 io.ktor:ktor-io,io.ktor:ktor-io-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX[2.3.3] #0[2.3.3]
            	/some/path/ktor-io.klib
            2 org.jetbrains.kotlin:kotlin-stdlib[$KOTLIN_VERSION] #0[$KOTLIN_VERSION] #4[1.8.20] #3[1.8.20]
            3 org.jetbrains.kotlin:kotlin-stdlib-jdk7[1.8.20] #4[1.8.20]
            	/some/path/kotlin-stdlib-jdk7-1.8.20.jar
            4 org.jetbrains.kotlin:kotlin-stdlib-jdk8[1.8.20] #6[1.8.20]
            	/some/path/kotlin-stdlib-jdk8-1.8.20.jar
            5 org.jetbrains.kotlinx:atomicfu,org.jetbrains.kotlinx:atomicfu-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX[0.21.0] #6[0.21.0] #1[0.19.0]
            	/some/path/atomicfu.klib
            	/some/path/atomicfu-cinterop-interop.klib
            6 org.jetbrains.kotlinx:kotlinx-coroutines-core,org.jetbrains.kotlinx:kotlinx-coroutines-core-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX[1.7.2] #0[1.7.2] #1[1.7.1]
            	/some/path/kotlinx-coroutines-core.klib
            
            """.trimIndent(),
                externalDependenciesText
            )
        }
    }

    @DisplayName("Should build with ktor 1.6.5 and coroutines 1.5.2-native-mt")
    @GradleTest
    fun shouldUseKtorAndCoroutinesExternalDependencies(gradleVersion: GradleVersion) {
        buildProjectWithDependencies(
            gradleVersion,
            "io.ktor:ktor-io:1.6.5",
            "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt"
        ) { externalDependenciesText ->
            assertNotNull(externalDependenciesText)

            assertEquals(
                """
            |0 native-external-dependencies
            |1 io.ktor:ktor-io,io.ktor:ktor-io-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX[1.6.5] #0[1.6.5]
            |${'\t'}/some/path/ktor-io.klib
            |${'\t'}/some/path/ktor-io-cinterop-bits.klib
            |${'\t'}/some/path/ktor-io-cinterop-sockets.klib
            |2 org.jetbrains.kotlin:kotlin-stdlib[$KOTLIN_VERSION] #0[$KOTLIN_VERSION]
            |3 org.jetbrains.kotlinx:atomicfu,org.jetbrains.kotlinx:atomicfu-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX[0.16.3] #4[0.16.3] #1[0.16.3]
            |${'\t'}/some/path/atomicfu.klib
            |${'\t'}/some/path/atomicfu-cinterop-interop.klib
            |4 org.jetbrains.kotlinx:kotlinx-coroutines-core,org.jetbrains.kotlinx:kotlinx-coroutines-core-$DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX[1.5.2-native-mt] #0[1.5.2-native-mt] #1[1.5.2-native-mt]
            |${'\t'}/some/path/kotlinx-coroutines-core.klib
            |
            """.trimMargin(),
                externalDependenciesText
            )
        }
    }

    private fun buildProjectWithDependencies(
        gradleVersion: GradleVersion,
        vararg dependencies: String,
        externalDependenciesTextConsumer: (externalDependenciesText: String?) -> Unit,
    ) {
        nativeProject("native-external-dependencies", gradleVersion) {
            buildGradleKts.appendText(
                """
                |
                |kotlin {
                |    val commonMain by sourceSets.getting {
                |        dependencies {${dependencies.joinToString("") { "\n|            implementation(\"$it\")" }}
                |        }
                |    }
                |}
                """.trimMargin()
            )

            build("buildExternalDependenciesFile") {
                assertTasksExecuted(":buildExternalDependenciesFile")

                val externalDependenciesFile = findParameterInOutput("for_test_external_dependencies_file", output)?.let(::File)
                val externalDependenciesText = if (externalDependenciesFile?.exists() == true) {
                    externalDependenciesFile.readText()
                        .lineSequence()
                        .map { line ->
                            if (line.firstOrNull()?.isWhitespace() == true) "\t/some/path/${File(line.trimStart()).name}" else line
                        }
                        .joinToString("\n")
                } else null

                externalDependenciesTextConsumer(externalDependenciesText)
            }
        }
    }

}
