package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Assert
import org.junit.Assume
import org.junit.Test
import java.io.File

class UpToDateIT : BaseGradleIT() {
    @Test
    fun testLanguageVersionChange() {
        testMutations(
            *propertyMutationChain(
                "compileKotlin.kotlinOptions.languageVersion",
                "null", "'1.1'", "'1.0'", "null"
            )
        )
    }

    @Test
    fun testApiVersionChange() {
        testMutations(
            *propertyMutationChain(
                "compileKotlin.kotlinOptions.apiVersion",
                "null", "'1.1'", "'1.0'", "null"
            )
        )
    }

    @Test
    fun testOther() {
        testMutations(
            emptyMutation,
            OptionMutation("compileKotlin.kotlinOptions.jvmTarget", "'1.6'", "'1.8'"),
            OptionMutation("compileKotlin.kotlinOptions.freeCompilerArgs", "[]", "['-Xallow-kotlin-package']"),
            OptionMutation("kotlin.experimental.coroutines", "'error'", "'enable'"),
            OptionMutation("archivesBaseName", "'someName'", "'otherName'"),
            subpluginOptionMutation,
            externalOutputMutation,
            compilerClasspathMutation
        )
    }

    private fun testMutations(vararg mutations: ProjectMutation) {
        val project = Project("kotlinProject")
        project.setupWorkingDir()
        mutations.forEach {
            it.initProject(project)
            project.build("classes") { assertSuccessful() }

            it.mutateProject(project)
            project.build("classes") {
                try {
                    assertSuccessful()
                    it.checkAfterRebuild(this)
                } catch (e: Throwable) {
                    throw RuntimeException("Mutation '${it.name}' has failed", e)
                }
            }
        }
    }

    private val emptyMutation = object : ProjectMutation {
        override val name = "emptyMutation"

        override fun initProject(project: Project) = Unit
        override fun mutateProject(project: Project) = Unit

        override fun checkAfterRebuild(compiledProject: CompiledProject) = with(compiledProject) {
            assertTasksUpToDate(listOf(":compileKotlin"))
        }
    }

    private val compilerClasspathMutation = object : ProjectMutation {
        override val name = "compilerClasspathMutation"

        lateinit var originalCompilerCp: List<String>
        val originalPaths get() = originalCompilerCp.map { it.replace("\\", "/") }.joinToString(", ") { "'$it'" }

        override fun initProject(project: Project) = with(project) {
            buildGradle.appendText("\nprintln 'compiler_cp=' + compileKotlin.getComputedCompilerClasspath\$kotlin_gradle_plugin()")
            build("clean") { originalCompilerCp = "compiler_cp=\\[(.*)]".toRegex().find(output)!!.groupValues[1].split(", ") }
            buildGradle.appendText("""${'\n'}
                // Add Kapt to the project to test its input checks as well:
                apply plugin: 'kotlin-kapt'
                compileKotlin.compilerClasspath = files($originalPaths).toList()
                afterEvaluate {
                    kaptGenerateStubsKotlin.compilerClasspath = files($originalPaths).toList()
                }
            """.trimIndent())
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
            assertTasksExecuted(":compileKotlin", ":kaptGenerateStubsKotlin", ":kaptKotlin")
        }
    }

    private val subpluginOptionMutation = object : ProjectMutation {
        override val name: String get() = "subpluginOptionMutation"

        override fun initProject(project: Project) = with(project) {
            buildGradle.appendText(
                "\n" + """
                buildscript { dependencies { classpath "org.jetbrains.kotlin:kotlin-allopen:${'$'}kotlin_version" } }
                apply plugin: "kotlin-allopen"
                allOpen { annotation("allopen.Foo"); annotation("allopen.Bar") }
            """.trimIndent()
            )
        }

        override fun mutateProject(project: Project) = with(project) {
            buildGradle.modify { it.replace("allopen.Foo", "allopen.Baz") }
        }

        override fun checkAfterRebuild(compiledProject: CompiledProject) = with(compiledProject) {
            assertTasksExecuted(":compileKotlin")
        }
    }

    private val externalOutputMutation = object : ProjectMutation {
        override val name = "externalOutputMutation"

        override fun initProject(project: Project) = Unit

        lateinit var helloWorldKtClass: File

        override fun mutateProject(project: Project) = with(project) {
            val kotlinOutputPath =
                if (testGradleVersionAtLeast("4.0"))
                    project.classesDir()
                else
                // Before 4.0, we should delete the classes from the temporary dir to make compileKotlin rerun:
                    "build/kotlin-classes/main/"

            helloWorldKtClass = File(projectDir, kotlinOutputPath + "demo/KotlinGreetingJoiner.class")
            Assume.assumeTrue(helloWorldKtClass.exists())
            helloWorldKtClass.delete(); Unit
        }

        override fun checkAfterRebuild(compiledProject: CompiledProject) = with(compiledProject) {
            assertTasksExecuted(":compileKotlin")
            Assert.assertTrue(helloWorldKtClass.exists())
        }
    }

    private val BaseGradleIT.Project.buildGradle get() = File(projectDir, "build.gradle")

    private interface ProjectMutation {
        fun initProject(project: Project)
        fun mutateProject(project: Project)
        fun checkAfterRebuild(compiledProject: CompiledProject)
        val name: String
    }

    private fun propertyMutationChain(path: String, vararg values: String): Array<ProjectMutation> =
        arrayListOf<ProjectMutation>().apply {
            for (i in 1..values.lastIndex) {
                add(OptionMutation(path, values[i - 1], values[i], shouldInit = i == 1))
            }

        }.toTypedArray()

    private inner class OptionMutation(
        private val path: String,
        private val oldValue: String,
        private val newValue: String,
        private val shouldInit: Boolean = true
    ) : ProjectMutation {
        override val name = "OptionMutation(path='$path', oldValue='$oldValue', newValue='$newValue')"

        override fun initProject(project: Project) = with(project) {
            if (shouldInit) {
                buildGradle.appendText("\n$path = $oldValue")
            }
        }

        override fun mutateProject(project: Project) = with(project) {
            buildGradle.modify { it.replace("$path = $oldValue", "$path = $newValue") }
        }

        override fun checkAfterRebuild(compiledProject: CompiledProject) = with(compiledProject) {
            assertTasksExecuted(":compileKotlin")
        }
    }
}
