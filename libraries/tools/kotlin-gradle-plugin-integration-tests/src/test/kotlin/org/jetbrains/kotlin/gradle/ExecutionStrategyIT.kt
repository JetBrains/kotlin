package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
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
        doTestExecutionStrategy(KotlinCompilerExecutionStrategy.DAEMON, addHeapDumpOptions = false)
    }

    @Test
    fun testDaemonViaSystemProperty() {
        doTestExecutionStrategy(KotlinCompilerExecutionStrategy.DAEMON, addHeapDumpOptions = false, viaSystemProperty = true)
    }

    @Test
    fun testInProcess() {
        doTestExecutionStrategy(KotlinCompilerExecutionStrategy.IN_PROCESS)
    }

    @Test
    fun testInProcessViaSystemProperty() {
        doTestExecutionStrategy(KotlinCompilerExecutionStrategy.IN_PROCESS, viaSystemProperty = true)
    }

    @Test
    fun testOutOfProcess() {
        doTestExecutionStrategy(KotlinCompilerExecutionStrategy.OUT_OF_PROCESS)
    }

    @Test
    fun testOutOfProcessViaSystemProperty() {
        doTestExecutionStrategy(KotlinCompilerExecutionStrategy.OUT_OF_PROCESS, viaSystemProperty = true)
    }

    private fun doTestExecutionStrategy(
        executionStrategy: KotlinCompilerExecutionStrategy,
        addHeapDumpOptions: Boolean = true,
        viaSystemProperty: Boolean = false
    ) {
        with(Project("kotlinBuiltins", addHeapDumpOptions = addHeapDumpOptions)) {
            setupProject(this)

            val cliArgPrefix = if (viaSystemProperty) "-D" else "-P"
            val strategyCLIArg = "${cliArgPrefix}kotlin.compiler.execution.strategy=${executionStrategy.propertyValue}"
            val finishMessage = "Finished executing kotlin compiler using $executionStrategy strategy"

            build("build", strategyCLIArg) {
                assertSuccessful()
                assertContains(finishMessage)
                checkOutput()
                assertNoWarnings()

                if (executionStrategy == KotlinCompilerExecutionStrategy.DAEMON) {
                    checkCompileDaemon()
                }
            }

            val classesKt = projectDir.getFileByName("classes.kt")
            classesKt.modify {
                it.checkedReplace("class B", "//class B")
            }
            build("build", strategyCLIArg) {
                assertSuccessful()
                assertContains(finishMessage)
                checkOutputAfterChange()
                assertNoWarnings()
            }
        }
    }

    private fun CompiledProject.checkCompileDaemon() {
        val m = "Kotlin compile daemon JVM options: \\[(.*?)\\]".toRegex().find(output)
            ?: error("Could not find Kotlin compile daemon JVM options in Gradle's output")
        val kotlinDaemonJvmArgs = m.groupValues[1].split(",").mapTo(LinkedHashSet()) { it.trim() }

        fun assertDaemonArgsContain(arg: String) {
            Assert.assertTrue(
                "Expected '$arg' in kotlin daemon JVM args, got: $kotlinDaemonJvmArgs",
                arg in kotlinDaemonJvmArgs
            )
        }

        // 256m is the default value for Gradle 5.0+
        assertDaemonArgsContain("-XX:MaxMetaspaceSize=256m")
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