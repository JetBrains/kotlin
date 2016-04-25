package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.allKotlinFiles
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File

class TestRootAffectedIT : BaseGradleIT() {
    @Test
    fun testSourceRootClassIsModifiedIC() {
        val project = Project("kotlinProject", "2.10")
        val buildOptions = defaultBuildOptions().copy(incremental = true)

        project.build("build", options = buildOptions) {
            assertSuccessful()
        }

        val kotlinGreetingJoinerFile = project.projectDir.getFileByName("KotlinGreetingJoiner.kt")
        kotlinGreetingJoinerFile.modify {
            val replacing   = "fun addName(name: String?): Unit"
            val replacement = "fun addName(name: String): Unit"
            assert(it.contains(replacing)) { "API has changed!" }
            it.replace(replacing, replacement)
        }

        project.build("build", options = buildOptions) {
            assertSuccessful()
            val expectedToCompile = project.relativize(listOf(kotlinGreetingJoinerFile) + project.allTestKotlinFiles())
            assertCompiledKotlinSources(expectedToCompile)
        }

        project.build("build", options = buildOptions) {
            assertSuccessful()
            assertCompiledKotlinSources(emptyList())
        }
    }

    @Test
    fun testSourceRootClassIsRemovedIC() {
        val project = Project("kotlinProject", "2.10")
        val buildOptions = defaultBuildOptions().copy(incremental = true)

        project.build("build", options = buildOptions) {
            assertSuccessful()
        }

        val dummyFile = project.projectDir.getFileByName("Dummy.kt")
        dummyFile.delete()

        project.build("build", options = buildOptions) {
            assertSuccessful()
            val expectedToCompile = project.relativize(project.allTestKotlinFiles())
            assertCompiledKotlinSources(expectedToCompile)
        }

        project.build("build", options = buildOptions) {
            assertSuccessful()
            assertCompiledKotlinSources(emptyList())
        }
    }

    @Test
    fun testTestRootClassIsRemovedIC() {
        val project = Project("kotlinProject", "2.10")
        val buildOptions = defaultBuildOptions().copy(incremental = true)

        project.build("build", options = buildOptions) {
            assertSuccessful()
        }

        val greeterTestFile = project.projectDir.getFileByName("TestGreeter.kt")
        greeterTestFile.delete()

        project.build("build", options = buildOptions) {
            assertSuccessful()
            assertCompiledKotlinSources(emptyList())
        }

        project.build("build", options = buildOptions) {
            assertSuccessful()
            assertCompiledKotlinSources(emptyList())
        }
    }

    private fun Project.allTestKotlinFiles(): Iterable<File> =
            File(projectDir, "src/test").allKotlinFiles()
}