package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Assert
import org.junit.Assume
import org.junit.Test
import java.io.File

class UpToDateIT : BaseGradleIT() {
    @Test
    fun testNoMutation() = testWithMutation(object : ProjectMutation {
        override fun initProject(project: Project) = Unit
        override fun mutateProject(project: Project) = Unit

        override fun checkAfterRebuild(compiledProject: CompiledProject) = with(compiledProject) {
            assertTasksUpToDate(listOf(":compileKotlin"))
        }
    })

    @Test
    fun testJvmTargetMutation() = testWithMutation(optionMutation(
            "compileKotlin.kotlinOptions.jvmTarget", "'1.6'", "'1.8'",
            shouldOutdateCompileKotlin = true))

    @Test
    fun testFreeCompilerArgsMutation() = testWithMutation(optionMutation(
            "compileKotlin.kotlinOptions.freeCompilerArgs", "[]", "['-Xallow-kotlin-package']",
            shouldOutdateCompileKotlin = true))

    @Test
    fun testCoroutinesMutation() = testWithMutation(optionMutation(
            "kotlin.experimental.coroutines", "'error'", "'enable'",
            shouldOutdateCompileKotlin = true))

    @Test
    fun testArchivesBaseNameMutation() = testWithMutation(optionMutation(
            "archivesBaseName", "'someName'", "'otherName'",
            shouldOutdateCompileKotlin = true))

    @Test
    fun testSubpluginOptionsMutation() = testWithMutation(object : ProjectMutation {
        override fun initProject(project: Project) = with(project) {
            buildGradle.appendText("""${'\n'}
                buildscript { dependencies { classpath "org.jetbrains.kotlin:kotlin-allopen:${'$'}kotlin_version" } }
                apply plugin: "kotlin-allopen"
                allOpen { annotation("com.my.Annotation") }
                """.trimIndent())
        }

        override fun mutateProject(project: Project) = with(project) {
            buildGradle.modify { it.replace("com.my.Annotation", "com.my.AnotherAnnotation") }
        }

        override fun checkAfterRebuild(compiledProject: CompiledProject) = with(compiledProject) {
            assertTasksExecuted(listOf(":compileKotlin"))
        }
    })

    @Test
    fun testCompilerJarMutation() = testWithMutation(object : ProjectMutation {
        lateinit var originalCompilerJar: File
        val originalPath get() = originalCompilerJar.absolutePath.replace("\\", "/")

        override fun initProject(project: Project) = with(project) {
            buildGradle.appendText("\nprintln 'compiler_jar=' + compileKotlin.getCompilerJar\$kotlin_gradle_plugin()")
            build("clean") { originalCompilerJar = File("compiler_jar=(.*)".toRegex().find(output)!!.groupValues[1]) }
            buildGradle.appendText("\ncompileKotlin.compilerJarFile = file('$originalPath')")
        }

        override fun mutateProject(project: Project) = with(project) {
            val localCompilerJar = originalCompilerJar.copyTo(File(projectDir, "compiler.jar"))
            buildGradle.modify {
                it.replace("file('${originalPath}')", "file('${localCompilerJar.absolutePath.replace('\\', '/')}')")
            }
        }

        override fun checkAfterRebuild(compiledProject: CompiledProject) = with(compiledProject) {
            assertTasksExecuted(listOf(":compileKotlin"))
        }
    })

    @Test
    fun testExternalOutputMutation() = testWithMutation(object : ProjectMutation {
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
    })

    private fun testWithMutation(projectMutation: ProjectMutation) {
        val project = Project("simpleProject", "4.1")
        project.projectDir.run { if (exists()) deleteRecursively() }
        project.setupWorkingDir()
        projectMutation.initProject(project)
        project.build("build") { assertSuccessful() }
        projectMutation.mutateProject(project)
        project.build("build") {
            assertSuccessful()
            projectMutation.checkAfterRebuild(this)
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
