package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.checkedReplace
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Assert
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

    override fun CompiledProject.checkOutputAfterChange() {
        assertFileExists("web/js/out.js")
    }
}

class ExecutionStrategyJvmIT : ExecutionStrategyIT() {
    override fun CompiledProject.checkOutput() {
        val classesDir = kotlinClassesDir(subproject = "app") + "foo/"
        assertFileExists("${classesDir}MainKt.class")
        assertFileExists("${classesDir}A.class")
        assertFileExists("${classesDir}B.class")
    }

    override fun CompiledProject.checkOutputAfterChange() {
        val classesDir = kotlinClassesDir(subproject = "app") + "foo/"
        assertFileExists("${classesDir}MainKt.class")
        assertFileExists("${classesDir}A.class")
        assertNoSuchFile("${classesDir}B.class")
    }
}

abstract class ExecutionStrategyIT : BaseGradleIT() {
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

            if (executionStrategy == "daemon") {
                checkCompileDaemon()
            }
        }

        val classesKt = project.projectDir.getFileByName("classes.kt")
        classesKt.modify {
            it.checkedReplace("class B", "//class B")
        }
        project.build("build", strategyCLIArg) {
            assertSuccessful()
            assertContains(finishMessage)
            checkOutputAfterChange()
            assertNoWarnings()
        }
    }

    private fun CompiledProject.checkCompileDaemon() {
        val isGradleAtLeast50 = project.testGradleVersionAtLeast("5.0")

        val m = "Kotlin compile daemon JVM options: \\[(.*?)\\]".toRegex().find(output)
            ?: error("Could not find Kotlin compile daemon JVM options in Gradle's output")
        val kotlinDaemonJvmArgs = m.groupValues[1].split(",").mapTo(LinkedHashSet()) { it.trim() }

        fun assertDaemonArgsContain(arg: String) {
            Assert.assertTrue(
                "Expected '$arg' in kotlin daemon JVM args, got: $kotlinDaemonJvmArgs",
                arg in kotlinDaemonJvmArgs
            )
        }

        if (isGradleAtLeast50) {
            // 256m is the default value for Gradle 5.0+
            assertDaemonArgsContain("-XX:MaxMetaspaceSize=256m")
        }

        assertDaemonArgsContain("-ea")
    }

    protected open fun setupProject(project: Project) {
        project.setupWorkingDir()
        File(project.projectDir, "app/build.gradle").appendText(
            "\ntasks.withType(org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile).all { kotlinOptions.allWarningsAsErrors = true }"
        )
    }

    protected abstract fun CompiledProject.checkOutput()
    protected abstract fun CompiledProject.checkOutputAfterChange()
}