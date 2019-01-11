package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.allKotlinFiles
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File

class TestRootAffectedIT : BaseGradleIT() {
    @Test
    fun testSourceRootClassIsModifiedIC() {
        val project = Project("kotlinProject")
        val buildOptions = defaultBuildOptions().copy(incremental = true)

        project.build("build", options = buildOptions) {
            assertSuccessful()
        }

        val kotlinGreetingJoinerFile = project.projectDir.getFileByName("KotlinGreetingJoiner.kt")
        kotlinGreetingJoinerFile.modify {
            val replacing = "fun addName(name: String?): Unit"
            val replacement = "fun addName(name: String): Unit"
            assert(it.contains(replacing)) { "API has changed!" }
            it.replace(replacing, replacement)
        }

        project.build("build", options = buildOptions) {
            assertSuccessful()
            val testKotlinGreetingJoinerFile = project.projectDir.getFileByName("TestKotlinGreetingJoiner.kt")
            assertCompiledKotlinSources(project.relativize(kotlinGreetingJoinerFile, testKotlinGreetingJoinerFile))
        }

        project.build("build", options = buildOptions) {
            assertSuccessful()
            assertCompiledKotlinSources(emptyList())
        }
    }

    @Test
    fun testSourceRootClassIsRemovedIC() {
        val project = Project("kotlinProject")
        val buildOptions = defaultBuildOptions().copy(incremental = true)

        project.build("build", options = buildOptions) {
            assertSuccessful()
        }

        val dummyFile = project.projectDir.getFileByName("Dummy.kt")
        dummyFile.delete()

        project.build("build", options = buildOptions) {
            assertSuccessful()
            // see KT-20541
            val kotlinTestFiles = File(project.projectDir, "src/test").allKotlinFiles()
            assertCompiledKotlinSources(project.relativize(kotlinTestFiles))
        }
    }

    @Test
    fun testTestRootClassIsRemovedIC() {
        val project = Project("kotlinProject")
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
    }
}