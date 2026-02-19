package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.util.JUnit4Assertions.assertTrue
import org.junit.jupiter.api.DisplayName

@DisplayName("Plugins DSL is working correctly")
@OtherGradlePluginTests
class PluginsDslIT : KGPBaseTest() {

    @DisplayName("Allopen plugin")
    @GradleTest
    fun testAllopenWithPluginsDsl(gradleVersion: GradleVersion) {
        project("allopenPluginsDsl".withPrefix, gradleVersion) {
            build("build") {
                assertTasksExecuted(":compileKotlin")
            }
        }
    }

    @DisplayName("Apply plugin to subproject from root project")
    @GradleTest
    fun testApplyToSubprojects(gradleVersion: GradleVersion) {
        project(
            "applyToSubprojects".withPrefix,
            gradleVersion,
            // applying plugins to subprojects is not compatible with isolated projects
            buildOptions = defaultBuildOptions.copy(isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED),
        ) {
            build("build") {
                assertTasksExecuted(":subproject:compileKotlin")
            }
        }
    }

    @DisplayName("All Kotlin plugins are applied to project")
    @GradleTest
    fun testApplyAllPlugins(gradleVersion: GradleVersion) {
        project("applyAllPlugins".withPrefix, gradleVersion) {

            val kotlinPluginClasses = setOf(
                "org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper",
                "org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin",
                "org.jetbrains.kotlin.allopen.gradle.AllOpenGradleSubplugin",
                "org.jetbrains.kotlin.allopen.gradle.SpringGradleSubplugin",
                "org.jetbrains.kotlin.noarg.gradle.NoArgGradleSubplugin",
                "org.jetbrains.kotlin.noarg.gradle.KotlinJpaSubplugin",
                "org.jetbrains.kotlinx.atomicfu.gradle.AtomicfuKotlinGradleSubplugin",
                "org.jetbrains.kotlin.lombok.gradle.LombokSubplugin",
                "org.jetbrains.kotlin.powerassert.gradle.PowerAssertGradlePlugin",
                "org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverGradleSubplugin",
                "org.jetbrains.kotlinx.serialization.gradle.SerializationGradleSubplugin"
            )

            build("build") {
                val appliedPlugins = "applied plugin class:(.*)".toRegex().findAll(output).map { it.groupValues[1] }.toSet()
                kotlinPluginClasses.forEach {
                    assertTrue(it in appliedPlugins) {
                        "Plugin class $it should be in applied plugins"
                    }
                }
            }
        }
    }

    private val String.withPrefix get() = "pluginsDsl/$this"
}
