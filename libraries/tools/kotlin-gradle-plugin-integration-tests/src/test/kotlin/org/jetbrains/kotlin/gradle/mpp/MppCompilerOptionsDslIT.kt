/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.gradle.mpp.MppCompilerOptionsDslIT.TestUtils.buildCompilerArguments
import org.jetbrains.kotlin.gradle.testbase.*
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.fail

@MppGradlePluginTests
@GradleTestVersions(maxVersion = TestVersions.Gradle.G_8_2)
class MppCompilerOptionsDslIT : KGPBaseTest() {


    /**
     * Will configure `apiVersion` on three different levels.
     * It is expected that target level configuration overwrites the top-level configuration
     * It is expected that compilation level configuration overwrites the target level configuration.
     */
    @GradleTest
    fun `test - top level - target level - compilation level`(gradleVersion: GradleVersion) {
        project("mppCompilerOptionsDsl", gradleVersion) {
            buildGradleKts.append(
                """
                kotlin {
                    jvm()
                    linuxArm64()
                    linuxX64()
                
                    compilerOptions {
                        apiVersion.set(KotlinVersion.KOTLIN_1_7)
                    }
                
                    linuxX64 {
                        compilerOptions {
                            apiVersion.set(KotlinVersion.KOTLIN_1_8)
                        }
                    }
                
                    linuxX64().compilations.getByName("test") {
                        compilerOptions {
                            apiVersion.set(KotlinVersion.KOTLIN_1_9)
                        }
                    }
                }
                """.trimIndent()
            )

            val arguments = buildCompilerArguments()
            val jvmMainArguments = arguments.get<K2JVMCompilerArguments>("jvm", "main")
            assertEquals("1.7", jvmMainArguments.apiVersion)

            val linuxX64MainArguments = arguments.get<K2NativeCompilerArguments>("linuxX64", "main")
            assertEquals("1.8", linuxX64MainArguments.apiVersion)

            val linuxX64TestArguments = arguments.get<K2NativeCompilerArguments>("linuxX64", "test")
            assertEquals("1.9", linuxX64TestArguments.apiVersion)
        }
    }

    /**
     * See puzzler posted: https://youtrack.jetbrains.com/issue/KT-61636/Wrong-scope-of-compiler-options-could-be-used-in-the-build-script#focus=Comments-27-8127182.0-0
     */
    @GradleTest
    fun `test - compilations all`(gradleVersion: GradleVersion) {
        project("mppCompilerOptionsDsl", gradleVersion) {
            buildGradleKts.append(
                """
                kotlin {
                    jvm()
                
                    compilerOptions {
                        apiVersion.set(KotlinVersion.KOTLIN_1_8)
                    }
                
                    targets.all {
                        compilations.all {
                            if (compilationName == "test") {
                                compilerOptions {
                                    apiVersion.set(KotlinVersion.KOTLIN_1_9)
                                }
                            }
                        }
                    }
                }
            """.trimIndent()
            )

            val arguments = buildCompilerArguments()
            val jvmMainArguments = arguments.get<K2JVMCompilerArguments>("jvm", "main")
            assertEquals("1.8", jvmMainArguments.apiVersion)

            val jvmTestArguments = arguments.get<K2JVMCompilerArguments>("jvm", "test")
            assertEquals("1.9", jvmTestArguments.apiVersion)
        }
    }

    @GradleTest
    fun `test - KT-61368 - Native compiler option 'module-name' isn't available within the compilerOptions`(gradleVersion: GradleVersion) {
        project("mppCompilerOptionsDsl", gradleVersion) {
            buildGradleKts.append(
                """
                    kotlin {
                        linuxArm64()
                        linuxX64()
                        
                        linuxX64().compilations.all {
                            compilerOptions {
                                moduleName.set("foo") 
                            }
                        }
                    }
                """.trimIndent()
            )

            val arguments = buildCompilerArguments()
            val linuxArm64MainArguments = arguments.get<K2NativeCompilerArguments>("linuxArm64", "main")
            assertNotEquals("foo", linuxArm64MainArguments.moduleName)

            val linuxX64MainArguments = arguments.get<K2NativeCompilerArguments>("linuxX64", "main")
            assertEquals("foo", linuxX64MainArguments.moduleName)

            val linuxX64TestArguments = arguments.get<K2JVMCompilerArguments>("linuxX64", "test")
            assertEquals("foo", linuxX64TestArguments.moduleName)
        }
    }

    @GradleTest
    fun `test - KT-61355 - freeCompilerArgs`(gradleVersion: GradleVersion) {
        project("mppCompilerOptionsDsl", gradleVersion) {
            buildGradleKts.append(
                """
                kotlin {
                    linuxX64()
                
                    jvm().compilations.all {
                        compilerOptions {
                            freeCompilerArgs.add("foo")
                        }
                    }
                }
                """.trimIndent()
            )

            val arguments = buildCompilerArguments()

            val linuxX64MainArguments = arguments.get<K2NativeCompilerArguments>("linuxX64", "main")
            assertEquals(emptyList(), linuxX64MainArguments.freeArgs)

            val jvmMainArguments = arguments.get<K2JVMCompilerArguments>("jvm", "main")
            assertEquals(listOf("foo"), jvmMainArguments.freeArgs)

            val jvmTestArguments = arguments.get<K2JVMCompilerArguments>("jvm", "test")
            assertEquals(listOf("foo"), jvmTestArguments.freeArgs)
        }
    }

    @Suppress("IdentifierGrammar")
    private object TestUtils {
        class CompilerArgumentsContainer(val project: TestProject) {
            inline fun <reified T : CommonCompilerArguments> get(target: String, compilation: String): T {
                val argumentsFile = project.projectPath.resolve("build/args/$target-$compilation.args")
                if (!argumentsFile.exists()) fail("Missing arguments for target $target and compilation $compilation")
                return parseCommandLineArguments<T>(argumentsFile.readText().lines())
            }
        }

        fun TestProject.buildCompilerArguments(): CompilerArgumentsContainer {
            build("buildCompilerArguments")
            return CompilerArgumentsContainer(this)
        }
    }
}
