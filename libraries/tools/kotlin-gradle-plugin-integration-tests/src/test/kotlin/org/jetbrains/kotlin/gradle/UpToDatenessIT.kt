package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Assert
import org.junit.Assume
import org.junit.Test
import java.io.File

class UpToDateIT : BaseGradleIT() {
    @Test
    fun testWithAllMutations() {
        val project = Project("simpleProject", "4.1")
        project.setupWorkingDir()
        mutations.forEach { it.initProject(project) }
        project.build("build") { assertSuccessful() }
        mutations.forEach {
            it.mutateProject(project)
            project.build("build") {
                assertSuccessful()
                it.checkAfterRebuild(this)
            }
        }
    }

    private val mutations
        get() = listOf(
                emptyMutation,
                optionMutation("compileKotlin.kotlinOptions.jvmTarget", "'1.6'", "'1.8'",
                               shouldOutdateCompileKotlin = true),
                optionMutation("compileKotlin.kotlinOptions.freeCompilerArgs", "[]", "['-Xallow-kotlin-package']",
                               shouldOutdateCompileKotlin = true),
                optionMutation("kotlin.experimental.coroutines", "'error'", "'enable'",
                               shouldOutdateCompileKotlin = true),
                optionMutation("archivesBaseName", "'someName'", "'otherName'",
                               shouldOutdateCompileKotlin = true),
                externalOutputMutation,
                compilerClasspathMutation)

    private val emptyMutation = object : ProjectMutation {
        override fun initProject(project: Project) = Unit
        override fun mutateProject(project: Project) = Unit

        override fun checkAfterRebuild(compiledProject: CompiledProject) = with(compiledProject) {
            assertTasksUpToDate(listOf(":compileKotlin"))
        }
    }

    private val compilerClasspathMutation = object : ProjectMutation {
        lateinit var originalCompilerCp: List<String>
        val originalPaths get() = originalCompilerCp.map { it.replace("\\", "/") }.joinToString(", ") { "'$it'" }

        override fun initProject(project: Project) = with(project) {
            buildGradle.appendText("\nprintln 'compiler_cp=' + compileKotlin.getComputedCompilerClasspath\$kotlin_gradle_plugin()")
            build("clean") { originalCompilerCp = "compiler_cp=\\[(.*)]".toRegex().find(output)!!.groupValues[1].split(", ") }
            buildGradle.appendText("\ncompileKotlin.compilerClasspath = files($originalPaths).toList()")
        }

        override fun mutateProject(project: Project) = with(project) {
            buildGradle.modify {
                val modifiedClasspath = originalCompilerCp.map {
                    val file = File(it)
                    val newFile = File(projectDir, file.nameWithoutExtension + "-1.jar")
                    file.copyTo(newFile)
                    newFile.absolutePath
                }.reversed()
                it.replace(originalPaths, modifiedClasspath.joinToString(", ") { "'${it.replace("\\", "/")}'" })
            }
        }

        override fun checkAfterRebuild(compiledProject: CompiledProject) = with(compiledProject) {
            assertTasksExecuted(listOf(":compileKotlin"))
        }
    }

    private val externalOutputMutation = object : ProjectMutation {
        override fun initProject(project: Project) = Unit

        lateinit var helloWorldKtClass: File

        override fun mutateProject(project: Project) = with(project) {
            helloWorldKtClass = File(projectDir, "build/classes/kotlin/main/demo/KotlinGreetingJoiner.class")
            Assume.assumeTrue(helloWorldKtClass.exists())
            helloWorldKtClass.delete(); Unit
        }

        override fun checkAfterRebuild(compiledProject: CompiledProject) = with(compiledProject) {
            assertTasksExecuted(listOf(":compileKotlin"))
            Assert.assertTrue(helloWorldKtClass.exists())
        }
    }

    private val BaseGradleIT.Project.buildGradle get() = File(projectDir, "build.gradle")

    private interface ProjectMutation {
        fun initProject(project: Project)
        fun mutateProject(project: Project)
        fun checkAfterRebuild(compiledProject: CompiledProject)
    }

    private fun optionMutation(
            path: String,
            oldValue: String,
            newValue: String,
            shouldOutdateCompileKotlin: Boolean
    ) = object : ProjectMutation {
        override fun initProject(project: Project) = with(project) {
            buildGradle.appendText("\n$path = $oldValue")
        }

        override fun mutateProject(project: Project) = with(project) {
            buildGradle.modify { it.replace("$path = $oldValue", "$path = $newValue") }
        }

        override fun checkAfterRebuild(compiledProject: CompiledProject) = with(compiledProject) {
            if (shouldOutdateCompileKotlin) {
                assertTasksExecuted(listOf(":compileKotlin"))
            }
            else {
                assertTasksUpToDate(listOf(":compileKotlin"))
            }
        }
    }
}
