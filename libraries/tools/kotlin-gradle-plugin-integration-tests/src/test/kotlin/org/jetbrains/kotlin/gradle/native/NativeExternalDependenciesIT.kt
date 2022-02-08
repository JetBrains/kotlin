/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.GradleVersionRequired
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Assume.assumeFalse
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NativeExternalDependenciesIT : BaseGradleIT() {
    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.FOR_MPP_SUPPORT

    @Test
    fun `no external dependencies`() = buildProjectWithDependencies { externalDependenciesText ->
        assertNull(externalDependenciesText)
    }

    @Test
    fun `ktor 1_5_4 and coroutines 1_4_3-native-mt`() {
        // These versions of Ktor and coroutines don't support macos-arm64
        assumeFalse(hostIsMacArm64())

        buildProjectWithDependencies(
            "io.ktor:ktor-io:1.5.4",
            "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3-native-mt"
        ) { externalDependenciesText ->
            assertNotNull(externalDependenciesText)

            assertEquals(
                """
                |0 native-external-dependencies
                |1 io.ktor:ktor-io,io.ktor:ktor-io-$MASKED_TARGET_NAME[1.5.4] #0[1.5.4]
                |${'\t'}/some/path/ktor-io.klib
                |${'\t'}/some/path/ktor-io-cinterop-bits.klib
                |${'\t'}/some/path/ktor-io-cinterop-sockets.klib
                |2 org.jetbrains.kotlinx:atomicfu,org.jetbrains.kotlinx:atomicfu-$MASKED_TARGET_NAME[0.15.1] #3[0.15.1] #1[0.15.1]
                |${'\t'}/some/path/atomicfu.klib
                |${'\t'}/some/path/atomicfu-cinterop-interop.klib
                |3 org.jetbrains.kotlinx:kotlinx-coroutines-core,org.jetbrains.kotlinx:kotlinx-coroutines-core-$MASKED_TARGET_NAME[1.4.3-native-mt] #0[1.4.3-native-mt] #1[1.4.3-native-mt]
                |${'\t'}/some/path/kotlinx-coroutines-core.klib
                |
                """.trimMargin(),
                externalDependenciesText
            )
        }
    }

    @Test
    fun `ktor 1_5_4 and coroutines 1_5_0-RC-native-mt`() {
        // These versions of Ktor and coroutines don't support macos-arm64
        assumeFalse(hostIsMacArm64())

        buildProjectWithDependencies(
            "io.ktor:ktor-io:1.5.4",
            "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0-RC-native-mt"
        ) { externalDependenciesText ->
            assertNotNull(externalDependenciesText)

            assertEquals(
                """
            |0 native-external-dependencies
            |1 io.ktor:ktor-io,io.ktor:ktor-io-$MASKED_TARGET_NAME[1.5.4] #0[1.5.4]
            |${'\t'}/some/path/ktor-io.klib
            |${'\t'}/some/path/ktor-io-cinterop-bits.klib
            |${'\t'}/some/path/ktor-io-cinterop-sockets.klib
            |2 org.jetbrains.kotlinx:atomicfu,org.jetbrains.kotlinx:atomicfu-$MASKED_TARGET_NAME[0.16.1] #3[0.16.1] #1[0.15.1]
            |${'\t'}/some/path/atomicfu.klib
            |${'\t'}/some/path/atomicfu-cinterop-interop.klib
            |3 org.jetbrains.kotlinx:kotlinx-coroutines-core,org.jetbrains.kotlinx:kotlinx-coroutines-core-$MASKED_TARGET_NAME[1.5.0-RC-native-mt] #0[1.5.0-RC-native-mt] #1[1.4.3-native-mt]
            |${'\t'}/some/path/kotlinx-coroutines-core.klib
            |
            """.trimMargin(),
                externalDependenciesText
            )
        }
    }

    @Test
    fun `ktor 1_6_5 and coroutines 1_5_2-native-mt`() = buildProjectWithDependencies(
        "io.ktor:ktor-io:1.6.5",
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt"
    ) { externalDependenciesText ->
        assertNotNull(externalDependenciesText)

        assertEquals(
            """
            |0 native-external-dependencies
            |1 io.ktor:ktor-io,io.ktor:ktor-io-$MASKED_TARGET_NAME[1.6.5] #0[1.6.5]
            |${'\t'}/some/path/ktor-io.klib
            |${'\t'}/some/path/ktor-io-cinterop-bits.klib
            |${'\t'}/some/path/ktor-io-cinterop-sockets.klib
            |2 org.jetbrains.kotlinx:atomicfu,org.jetbrains.kotlinx:atomicfu-$MASKED_TARGET_NAME[0.16.3] #3[0.16.3] #1[0.16.3]
            |${'\t'}/some/path/atomicfu.klib
            |${'\t'}/some/path/atomicfu-cinterop-interop.klib
            |3 org.jetbrains.kotlinx:kotlinx-coroutines-core,org.jetbrains.kotlinx:kotlinx-coroutines-core-$MASKED_TARGET_NAME[1.5.2-native-mt] #0[1.5.2-native-mt] #1[1.5.2-native-mt]
            |${'\t'}/some/path/kotlinx-coroutines-core.klib
            |
            """.trimMargin(),
            externalDependenciesText
        )
    }

    private fun buildProjectWithDependencies(
        vararg dependencies: String,
        externalDependenciesTextConsumer: (externalDependenciesText: String?) -> Unit
    ) {
        with(transformNativeTestProjectWithPluginDsl("native-external-dependencies")) {
            gradleBuildScript().appendText(
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

            build(
                "assemble",
                options = defaultBuildOptions().copy(dryRun = true)
            ) {
                assertSuccessful()

                val kotlinNativeTargetName = findKotlinNativeTargetName(output)
                assertNotNull(kotlinNativeTargetName)

                val externalDependenciesFile = findParameterInOutput("for_test_external_dependencies_file", output)?.let(::File)
                val externalDependenciesText = if (externalDependenciesFile?.exists() == true) {
                    externalDependenciesFile.readText()
                        .lineSequence()
                        .map { line ->
                            if (line.firstOrNull()?.isWhitespace() == true) {
                                // Transform artifact path.
                                val pureFileName = File(line.trimStart()).name
                                "\t/some/path/$pureFileName"
                            } else {
                                // Transform module name (mask target name).
                                line.replace(kotlinNativeTargetName, MASKED_TARGET_NAME)
                            }
                        }
                        .joinToString("\n")
                } else null

                externalDependenciesTextConsumer(externalDependenciesText)
            }
        }
    }

    internal companion object {
        val MASKED_TARGET_NAME = "testTarget" + HostManager.host.architecture.name

        fun hostIsMacArm64() = HostManager.host == KonanTarget.MACOS_ARM64

        fun findParameterInOutput(name: String, output: String): String? =
            output.lineSequence().mapNotNull { line ->
                val (key, value) = line.split('=', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
                if (key.endsWith(name)) value else null
            }.firstOrNull()

        fun findKotlinNativeTargetName(output: String): String? = findParameterInOutput("for_test_kotlin_native_target", output)
            ?.takeIf(String::isNotBlank)
            ?.toLowerCase()
    }
}
