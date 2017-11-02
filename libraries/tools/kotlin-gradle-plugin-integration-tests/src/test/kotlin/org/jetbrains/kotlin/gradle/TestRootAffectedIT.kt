package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test

class TestRootAffectedIT : BaseGradleIT() {
    @Test
    fun testSourceRootClassIsModifiedIC() {
        val project = Project("kotlinProject", GradleVersionAtLeast("4.1"))
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
        // todo: update Gradle after https://github.com/gradle/gradle/issues/3051 is resolved
        val project = Project("kotlinProject", SpecificGradleVersion("3.0"))
        val buildOptions = defaultBuildOptions().copy(incremental = true)

        project.build("build", options = buildOptions) {
            assertSuccessful()
        }

        val dummyFile = project.projectDir.getFileByName("Dummy.kt")
        dummyFile.delete()

        project.build("build", options = buildOptions) {
            assertSuccessful()
            assertCompiledKotlinSources(emptyList())
        }
    }

    @Test
    fun testTestRootClassIsRemovedIC() {
        val project = Project("kotlinProject", GradleVersionAtLeast("4.1"))
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