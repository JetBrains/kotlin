package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Test
import java.io.File

class ExecutionStrategyJsIT : ExecutionStrategyIT() {
    override fun setupProject(project: Project) {
        super.setupProject(project)
        val buildGradle = File(project.projectDir, "app/build.gradle")
        buildGradle.modify {
            it.replace("apply plugin: \"kotlin\"", "apply plugin: \"kotlin2js\"") +
                    "\ncompileKotlin2Js.kotlinOptions.outputFile = \"web/js/out.js\""
        }
    }

    override fun CompiledProject.checkOutput() {
        assertFileExists("web/js/out.js")
    }
}

open class ExecutionStrategyIT : BaseGradleIT() {
    @Test
    fun testDaemon() {
        doTestExecutionStrategy("daemon")
    }

    @Test
    fun testInProcess() {
        doTestExecutionStrategy("in-process")
    }

    @Test
    fun testOutOfProcess() {
        doTestExecutionStrategy("out-of-process")
    }

    private fun doTestExecutionStrategy(executionStrategy: String) {
        val project = Project("kotlinBuiltins")
        setupProject(project)

        val strategyCLIArg = "-Dkotlin.compiler.execution.strategy=$executionStrategy"
        val finishMessage = "Finished executing kotlin compiler using $executionStrategy strategy"

        project.build("build", strategyCLIArg) {
            assertSuccessful()
            assertContains(finishMessage)
            checkOutput()
            assertNoWarnings()
        }

        val fKt = project.projectDir.getFileByName("f.kt")
        fKt.delete()
        project.build("build", strategyCLIArg) {
            assertFailed()
            assertContains(finishMessage)
            assert(output.contains("Unresolved reference: f", ignoreCase = true))
        }
    }

    protected open fun setupProject(project: Project) {
        project.setupWorkingDir()
        File(project.projectDir, "app/build.gradle").appendText(
            "\ntasks.withType(org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile).all { kotlinOptions.allWarningsAsErrors = true }"
        )
    }

    protected open fun CompiledProject.checkOutput() {
        assertFileExists(kotlinClassesDir(subproject = "app") + "foo/MainKt.class")
    }
}