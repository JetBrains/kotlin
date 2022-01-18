package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.checkedReplace
import org.junit.jupiter.api.DisplayName

@DisplayName("Kotlin JS compile execution strategy")
class ExecutionStrategyJsIT : ExecutionStrategyIT() {
    override fun setupProject(project: TestProject) {
        super.setupProject(project)

        project.subProject("app").buildGradle.modify {
            it.replace(
                "id \"org.jetbrains.kotlin.jvm\"",
                "id \"org.jetbrains.kotlin.js\""
            ) +
                    """
                    |
                    |afterEvaluate {
                    |    tasks.named('compileKotlinJs') {
                    |        kotlinOptions.outputFile = "${'$'}{project.projectDir}/web/js/out.js"
                    |    }
                    |}
                    |
                    """.trimMargin()
        }
    }

    override fun BuildResult.checkOutput(project: TestProject) {
        project.subProject("app").assertFileInProjectExists("web/js/out.js")
    }

    override fun BuildResult.checkOutputAfterChange(project: TestProject) {
        project.subProject("app").assertFileInProjectExists("web/js/out.js")
    }
}

@DisplayName("Kotlin JVM compile execution strategy")
class ExecutionStrategyJvmIT : ExecutionStrategyIT() {
    override fun BuildResult.checkOutput(project: TestProject) {
        with(project) {
            val classesDir = subProject("app").kotlinClassesDir().resolve("foo")
            assertFileExists(classesDir.resolve("MainKt.class"))
            assertFileExists(classesDir.resolve("A.class"))
            assertFileExists(classesDir.resolve("B.class"))
        }
    }

    override fun BuildResult.checkOutputAfterChange(project: TestProject) {
        with(project) {
            val classesDir = subProject("app").kotlinClassesDir().resolve("foo")
            assertFileExists(classesDir.resolve("MainKt.class"))
            assertFileExists(classesDir.resolve("A.class"))
            assertFileNotExists(classesDir.resolve("B.class"))
        }
    }
}

abstract class ExecutionStrategyIT : KGPDaemonsBaseTest() {
    override val defaultBuildOptions: BuildOptions = super.defaultBuildOptions.copy(
        logLevel = LogLevel.DEBUG
    )

    @DisplayName("Compilation via Kotlin daemon")
    @GradleTest
    fun testDaemon(gradleVersion: GradleVersion) {
        doTestExecutionStrategy(
            gradleVersion,
            KotlinCompilerExecutionStrategy.DAEMON,
            addHeapDumpOptions = false
        )
    }

    @DisplayName("Compilation via Kotlin daemon enabled using system property")
    @GradleTest
    fun testDaemonViaSystemProperty(gradleVersion: GradleVersion) {
        doTestExecutionStrategy(
            gradleVersion,
            KotlinCompilerExecutionStrategy.DAEMON,
            addHeapDumpOptions = false,
            viaSystemProperty = true
        )
    }

    @DisplayName("Compilation inside Gradle daemon")
    @GradleTest
    fun testInProcess(gradleVersion: GradleVersion) {
        doTestExecutionStrategy(
            gradleVersion,
            KotlinCompilerExecutionStrategy.IN_PROCESS
        )
    }

    @DisplayName("Compilation inside Gradle daemon enabled using system property")
    @GradleTest
    fun testInProcessViaSystemProperty(gradleVersion: GradleVersion) {
        doTestExecutionStrategy(
            gradleVersion,
            KotlinCompilerExecutionStrategy.IN_PROCESS,
            viaSystemProperty = true
        )
    }

    @DisplayName("Compilation via separate compiler process")
    @GradleTest
    fun testOutOfProcess(gradleVersion: GradleVersion) {
        doTestExecutionStrategy(
            gradleVersion,
            KotlinCompilerExecutionStrategy.OUT_OF_PROCESS
        )
    }

    @DisplayName("Compilation via separate compiler process enabled via system property")
    @GradleTest
    fun testOutOfProcessViaSystemProperty(gradleVersion: GradleVersion) {
        doTestExecutionStrategy(
            gradleVersion,
            KotlinCompilerExecutionStrategy.OUT_OF_PROCESS,
            viaSystemProperty = true
        )
    }

    private fun doTestExecutionStrategy(
        gradleVersion: GradleVersion,
        executionStrategy: KotlinCompilerExecutionStrategy,
        addHeapDumpOptions: Boolean = true,
        viaSystemProperty: Boolean = false
    ) {
        project(
            projectName = "kotlinBuiltins",
            gradleVersion = gradleVersion,
            addHeapDumpOptions = addHeapDumpOptions
        ) {
            setupProject(this)

            val cliArgPrefix = if (viaSystemProperty) "-D" else "-P"
            val strategyCLIArg = "${cliArgPrefix}kotlin.compiler.execution.strategy=${executionStrategy.propertyValue}"
            val finishMessage = "Finished executing kotlin compiler using $executionStrategy strategy"

            build("build", strategyCLIArg) {
                assertOutputContains(finishMessage)
                checkOutput(this@project)
                assertNoBuildWarnings()

                if (executionStrategy == KotlinCompilerExecutionStrategy.DAEMON) {
                    // 256m is the default value for Gradle 5.0+
                    assertKotlinDaemonJvmOptions(
                        listOf("-XX:MaxMetaspaceSize=256m", "-ea")
                    )
                }
            }

            val classesKt = subProject("app").kotlinSourcesDir().resolve("classes.kt")
            classesKt.modify {
                it.checkedReplace("class B", "//class B")
            }
            build("build", strategyCLIArg) {
                assertOutputContains(finishMessage)
                checkOutputAfterChange(this@project)
                assertNoBuildWarnings()
            }
        }
    }

    protected open fun setupProject(project: TestProject) {
        project.subProject("app").buildGradle.append(
            //language=Groovy
            """
            |
            |tasks
            |    .withType(org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile)
            |    .configureEach { 
            |        kotlinOptions.allWarningsAsErrors = true 
            |    }
            """.trimMargin()
        )
    }

    protected abstract fun BuildResult.checkOutput(project: TestProject)
    protected abstract fun BuildResult.checkOutputAfterChange(project: TestProject)
}