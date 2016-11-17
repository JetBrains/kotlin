package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.getFileByName
import org.junit.Test

class ExecutionStrategyIT(): BaseGradleIT() {
    companion object {
        private const val GRADLE_VERSION = "2.10"
    }

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
        val project = Project("kotlinBuiltins", GRADLE_VERSION)
        setupProject(project)

        val strategyCLIArg = "-Dkotlin.compiler.execution.strategy=$executionStrategy"
        val finishMessage = "Finished executing kotlin compiler using $executionStrategy strategy"

        project.build("build", strategyCLIArg) {
            assertSuccessful()
            assertContains(finishMessage)
            checkOutput()
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
    }

    protected open fun CompiledProject.checkOutput() {
        assertFileExists("app/build/classes/main/foo/MainKt.class")
    }
}