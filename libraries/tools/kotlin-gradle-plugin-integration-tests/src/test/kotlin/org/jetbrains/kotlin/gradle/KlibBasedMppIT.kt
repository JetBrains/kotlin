/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assume
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse

/** FIXME (sergey.igushkin): please enable these tests back as soon as the Kotlin/Native version that is bundled with the
 *        Kotlin distribution supports compilation to klib and targetless klibs.
 */
@Ignore
class KlibBasedMppIT : BaseGradleIT() {
    override val defaultGradleVersion = GradleVersionRequired.AtLeast("6.0")

    companion object {
        private const val MODULE_GROUP = "com.example"
    }

    @Test
    fun testBuildWithProjectDependency() = testBuildWithDependency {
        gradleBuildScript().appendText("\n" + """
            dependencies {
                commonMainImplementation(project("$dependencyModuleName"))
            }
        """.trimIndent())
    }

    @Test
    fun testBuildWithPublishedDependency() = testBuildWithDependency {
        build(":$dependencyModuleName:publish") {
            assertSuccessful()
        }

        gradleBuildScript().appendText("\n" + """
            repositories {
                maven("${'$'}rootDir/repo")
            }
            dependencies {
                commonMainImplementation("$MODULE_GROUP:$dependencyModuleName:1.0")
            }
        """.trimIndent())

        // prevent Gradle from linking the above dependency to the project:
        gradleBuildScript(dependencyModuleName).appendText("\ngroup = \"some.other.group\"")
    }

    private val dependencyModuleName = "project-dep"

    private fun testBuildWithDependency(configureDependency: Project.() -> Unit) = with(Project("common-klib-lib-and-app")) {
        Assume.assumeTrue(HostManager.hostIsMac)

        embedProject(Project("common-klib-lib-and-app"), renameTo = dependencyModuleName)
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

        projectDir.resolve(dependencyModuleName + "/src").walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            file.modify { it.replace("package com.h0tk3y.hmpp.klib.demo", "package com.projectdep") }
        }

        configureDependency()

        projectDir.resolve("src/commonMain/kotlin/LibUsage.kt").appendText("\n" + """
            package com.h0tk3y.hmpp.klib.demo.test
            
            import com.projectdep.LibCommonMainExpect as ProjectDepExpect
            
            private fun useProjectDep() {
                ProjectDepExpect()
            }
        """.trimIndent())

        projectDir.resolve("src/iosMain/kotlin/LibIosMainUsage.kt").appendText("\n" + """
            package com.h0tk3y.hmpp.klib.demo.test
            
            import com.projectdep.libIosMainFun as libFun
            
            private fun useProjectDep() {
                libFun()
            }
        """.trimIndent())

        val tasksToExecute = listOf(
            ":compileJvmAndJsMainKotlinMetadata",
            ":compileIosMainKotlinMetadata"
        )

        build("assemble") {
            assertSuccessful()

            assertTasksExecuted(*tasksToExecute.toTypedArray())

            assertFileExists("build/classes/kotlin/metadata/jvmAndJsMain/manifest")
            assertFileExists("build/classes/kotlin/metadata/iosMain/${projectName}_iosMain.klib")

            // Check that the common and JVM+JS source sets don't receive the Kotlin/Native stdlib in the classpath:
            run {
                fun getClasspath(taskPath: String): Iterable<String> {
                    val argsPrefix = " $taskPath Kotlin compiler args:"
                    return output.lines().single { argsPrefix in it }
                        .substringAfter("-classpath ").substringBefore(" -").split(File.pathSeparator)
                }

                fun classpathHasKNStdlib(classpath: Iterable<String>) = classpath.any { "klib/common/stdlib" in it.replace("\\", "/") }

                assertFalse(classpathHasKNStdlib(getClasspath(":compileCommonMainKotlinMetadata")))
                assertFalse(classpathHasKNStdlib(getClasspath(":compileJvmAndJsMainKotlinMetadata")))
            }
        }
    }

    private val transitiveDepModuleName = "transitive-dep"

    @Test
    fun testKotlinNativeImplPublishedDeps() =
        testKotlinNativeImplementationDependencies {
            build(":$transitiveDepModuleName:publish", ":$dependencyModuleName:publish") {
                assertSuccessful()
            }

            gradleBuildScript().appendText("\n" + """
                repositories {
                    maven("${'$'}rootDir/repo")
                }
                dependencies {
                    commonMainImplementation("$MODULE_GROUP:$dependencyModuleName:1.0")
                }
                """.trimIndent()
            )

            listOf(transitiveDepModuleName, dependencyModuleName).forEach {
                // prevent Gradle from linking the above dependency to the project:
                gradleBuildScript(it).appendText("\ngroup = \"com.some.other.group\"")
            }
        }

    @Test
    fun testKotlinNativeImplProjectDeps() =
        testKotlinNativeImplementationDependencies {
            gradleBuildScript().appendText("\ndependencies { \"commonMainImplementation\"(project(\":$dependencyModuleName\")) }")
        }

    private fun testKotlinNativeImplementationDependencies(
        setupDependencies: Project.() -> Unit
    ) = with(Project("common-klib-lib-and-app")) {
        embedProject(Project("common-klib-lib-and-app"), renameTo = transitiveDepModuleName)
        embedProject(Project("common-klib-lib-and-app"), renameTo = dependencyModuleName)
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
        gradleBuildScript(dependencyModuleName).appendText("\ndependencies { \"commonMainImplementation\"(project(\":$transitiveDepModuleName\")) }")

        setupDependencies(this@with)

        val compileNativeMetadataTaskName = "compileIosMainKotlinMetadata"
        build(":$compileNativeMetadataTaskName") {
            assertSuccessful()
        }
    }
}