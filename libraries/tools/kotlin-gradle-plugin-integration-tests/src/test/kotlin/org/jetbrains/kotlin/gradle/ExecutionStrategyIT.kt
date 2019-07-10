package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
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

        val isGradleAtLeast50 = project.testGradleVersionAtLeast("5.0")

        project.build("build", strategyCLIArg) {
            assertSuccessful()
            assertContains(finishMessage)
            checkOutput()
            assertNoWarnings()

            if (executionStrategy == "daemon" && isGradleAtLeast50) {
                val m = "Kotlin compile daemon JVM options: \\[(.*?)\\]".toRegex().find(output)
                    ?: error("Could not find Kotlin compile daemon JVM options in Gradle's output")
                val kotlinDaemonJvmArgs = m.groupValues[1].split(",").map { it.trim() }
                val maxMetaspaceArg = "-XX:MaxMetaspaceSize=256m"
                Assert.assertTrue(
                    "Kotlin daemon JVM args do not contain '$maxMetaspaceArg': $kotlinDaemonJvmArgs",
                    maxMetaspaceArg in kotlinDaemonJvmArgs
                )
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

    protected open fun setupProject(project: Project) {
        project.setupWorkingDir()
        File(project.projectDir, "app/build.gradle").appendText(
            "\ntasks.withType(org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile).all { kotlinOptions.allWarningsAsErrors = true }"
        )
    }

    protected abstract fun CompiledProject.checkOutput()
    protected abstract fun CompiledProject.checkOutputAfterChange()
}