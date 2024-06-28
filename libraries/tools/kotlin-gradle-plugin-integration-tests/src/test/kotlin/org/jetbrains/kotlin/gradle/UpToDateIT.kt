package org.jetbrains.kotlin.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import java.io.File
import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists
import kotlin.test.assertTrue

@DisplayName("Kotlin options change")
@JvmGradlePluginTests
class UpToDateIT : KGPBaseTest() {

    @DisplayName("Language version change")
    @GradleTest
    fun testLanguageVersionChange(gradleVersion: GradleVersion) {
        testMutations(
            gradleVersion,
            propertyMutationChain(
                "compileKotlin.kotlinOptions.languageVersion",
                "null", "'1.6'", "'1.5'", "'1.4'", "null"
            )
        )
    }

    @DisplayName("Api version change")
    @GradleTest
    fun testApiVersionChange(gradleVersion: GradleVersion) {
        testMutations(
            gradleVersion,
            propertyMutationChain(
                "compileKotlin.kotlinOptions.apiVersion",
                "null", "'1.6'", "'1.5'", "'1.4'", "null"
            )
        )
    }

    @DisplayName("Misc changes")
    @DisabledOnOs(OS.WINDOWS, disabledReason = "Failed to delete project directory")
    @GradleTest
    fun testOther(gradleVersion: GradleVersion) {
        testMutations(
            gradleVersion,
            setOf(
                emptyMutation,
                OptionMutation("compileKotlin.kotlinOptions.jvmTarget", "'1.8'", "'11'"),
                OptionMutation("compileKotlin.kotlinOptions.freeCompilerArgs", "[]", "['-Xallow-kotlin-package']"),
                archivesBaseNameOutputMutation("someName", "otherName"),
                subpluginOptionMutation,
                subpluginOptionMutationWithKapt,
                externalOutputMutation,
                compilerClasspathMutation
            )
        )
    }

    private fun testMutations(
        gradleVersion: GradleVersion,
        mutations: Set<ProjectMutation>
    ) {
        project("kotlinProject", gradleVersion) {
            //language=properties
            gradleProperties.append(
                """
                # suppress inspection "UnusedProperty"
                kotlin.jvm.target.validation.mode = warning
                """.trimIndent()
            )

            mutations.forEach { mutation ->
                mutation.initProject(this)
                build("classes")

                mutation.mutateProject(this)
                build("classes") {
                    try {
                        mutation.checkAfterRebuild(this)
                    } catch (e: Throwable) {
                        throw RuntimeException("Mutation '${mutation.name}' has failed", e)
                    }
                }
            }
        }
    }

    private val emptyMutation = object : ProjectMutation {
        override val name = "emptyMutation"

        override fun initProject(project: TestProject) = Unit
        override fun mutateProject(project: TestProject) = Unit

        override fun checkAfterRebuild(buildResult: BuildResult) = with(buildResult) {
            assertTasksUpToDate(":compileKotlin")
        }
    }

    private val compilerClasspathMutation = object : ProjectMutation {
        override val name = "compilerClasspathMutation"

        private val compilerClasspathRegex = "compiler_cp=\\[(.*)]".toRegex()
        lateinit var originalCompilerCp: List<String>
        val originalPaths get() = originalCompilerCp.map { it.replace("\\", "/") }.joinToString(", ") { "'$it'" }

        override fun initProject(project: TestProject) = with(project) {
            val pluginSuffix = "kotlin_gradle_plugin_common"
            buildGradle.appendText(
                "\nafterEvaluate { println 'compiler_cp=' + compileKotlin.getDefaultCompilerClasspath\$$pluginSuffix().toList() }"
            )
            build("clean") {
                originalCompilerCp = compilerClasspathRegex.find(output)!!.groupValues[1].split(", ")
            }

            buildGradle.appendText(
                """
                
                // Add Kapt to the project to test its input checks as well:
                apply plugin: 'kotlin-kapt'
                compileKotlin.getDefaultCompilerClasspath${'$'}$pluginSuffix().setFrom(files($originalPaths).toList())
                afterEvaluate {
                    kaptGenerateStubsKotlin.getDefaultCompilerClasspath${'$'}$pluginSuffix().setFrom(files($originalPaths).toList())
                }
                """.trimIndent()
            )
        }

        override fun mutateProject(project: TestProject) = with(project) {
            buildGradle.modify {
                val modifiedClasspath = originalCompilerCp.map {
                    val file = File(it)
                    val newFile = projectPath.resolve(file.nameWithoutExtension + "-1.jar").toFile()
                    file.copyTo(newFile)
                    newFile.absolutePath
                }.reversed()

                it.replace(
                    originalPaths,
                    modifiedClasspath.joinToString(", ") { "'${it.replace("\\", "/")}'" }
                )
            }
        }

        override fun checkAfterRebuild(buildResult: BuildResult) = with(buildResult) {
            assertTasksExecuted(":compileKotlin", ":kaptGenerateStubsKotlin")
            // KAPT with workers is not impacted by compiler classpath changes.
            assertTasksUpToDate(":kaptKotlin")
        }
    }

    private val subpluginOptionMutation = object : ProjectMutation {
        override val name: String get() = "subpluginOptionMutation"

        override fun initProject(project: TestProject) = with(project) {
            buildGradle.appendText(
                """
                
                plugins.apply("org.jetbrains.kotlin.plugin.allopen")
                allOpen { annotation("allopen.Foo"); annotation("allopen.Bar") }
                """.trimIndent()
            )
        }

        override fun mutateProject(project: TestProject) = with(project) {
            buildGradle.modify { it.replace("allopen.Foo", "allopen.Baz") }
        }

        override fun checkAfterRebuild(buildResult: BuildResult) = with(buildResult) {
            assertTasksExecuted(":compileKotlin")
        }
    }

    private val subpluginOptionMutationWithKapt = object : ProjectMutation {
        override val name: String get() = "subpluginOptionMutationWithKapt"

        override fun initProject(project: TestProject) = with(project) {
            buildGradle.appendText(
                """
                |
                |apply plugin: 'kotlin-kapt'
                |plugins.apply("org.jetbrains.kotlin.plugin.allopen")
                |allOpen { annotation("allopen.Foo"); annotation("allopen.Bar") }
                |
                |dependencies {
                |    kapt 'org.jetbrains.kotlin:annotation-processor-example'
                |}
                """.trimMargin()
            )
        }

        override fun mutateProject(project: TestProject) = with(project) {
            buildGradle.modify { it.replace("allopen.Foo", "allopen.Baz") }
        }

        override fun checkAfterRebuild(buildResult: BuildResult) = with(buildResult) {
            assertTasksExecuted(":compileKotlin", ":kaptGenerateStubsKotlin")
            assertTasksUpToDate(":kaptKotlin")
        }
    }

    private val externalOutputMutation = object : ProjectMutation {
        override val name = "externalOutputMutation"

        override fun initProject(project: TestProject) = Unit

        lateinit var helloWorldKtClass: Path

        override fun mutateProject(project: TestProject) = with(project) {
            val kotlinOutputPath = kotlinClassesDir()

            helloWorldKtClass = kotlinOutputPath.resolve("demo/KotlinGreetingJoiner.class")
            assertTrue(helloWorldKtClass.exists())
            helloWorldKtClass.deleteExisting()
        }

        override fun checkAfterRebuild(buildResult: BuildResult) = with(buildResult) {
            assertTasksExecuted(":compileKotlin")
            assertTrue(helloWorldKtClass.exists())
        }
    }

    private fun archivesBaseNameOutputMutation(
        oldName: String,
        newName: String,
    ) = object : ProjectMutation {
        override fun initProject(project: TestProject) {
            project.addArchivesBaseNameCompat(oldName)
        }

        override fun mutateProject(project: TestProject) {
            project.buildGradle.modify {
                if (project.gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_5)) {
                    it.replace("archivesBaseName = '$oldName'", "archivesBaseName = '$newName'")
                } else {
                    it.replace("archivesName = '$oldName'", "archivesName = '$newName'")
                }
            }
        }

        override fun checkAfterRebuild(buildResult: BuildResult) {
            buildResult.assertTasksExecuted(":compileKotlin")
        }

        override val name: String = "archiveBaseNameOutputMutation"
    }

    private interface ProjectMutation {
        fun initProject(project: TestProject)
        fun mutateProject(project: TestProject)
        fun checkAfterRebuild(buildResult: BuildResult)
        val name: String
    }

    private fun propertyMutationChain(
        path: String,
        vararg values: String
    ): Set<OptionMutation> = values
        .drop(1)
        .mapIndexed { index, value ->
            val actualIndex = index + 1
            OptionMutation(
                path,
                values[actualIndex - 1],
                value,
                index == 0
            )
        }
        .toSet()

    private inner class OptionMutation(
        private val path: String,
        private val oldValue: String,
        private val newValue: String,
        private val shouldInit: Boolean = true
    ) : ProjectMutation {
        override val name = "OptionMutation(path='$path', oldValue='$oldValue', newValue='$newValue')"

        override fun initProject(project: TestProject) = with(project) {
            if (shouldInit) {
                buildGradle.appendText("\n$path = $oldValue")
            }
        }

        override fun mutateProject(project: TestProject) = with(project) {
            buildGradle.modify { it.replace("$path = $oldValue", "$path = $newValue") }
        }

        override fun checkAfterRebuild(buildResult: BuildResult) = with(buildResult) {
            assertTasksExecuted(":compileKotlin")
        }
    }
}
