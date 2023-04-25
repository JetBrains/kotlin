package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.checkedReplace
import org.junit.jupiter.api.DisplayName
import java.io.File

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
                    |        kotlinOptions.outputFile = "${'$'}{project.projectDir}/web/js/"
                    |    }
                    |}
                    |
                    """.trimMargin()
        }
    }

    override fun BuildResult.checkOutput(project: TestProject) {
        project.subProject("app").assertFileInProjectExists("web/js/default/manifest")
    }

    override fun BuildResult.checkOutputAfterChange(project: TestProject) {
        project.subProject("app").assertFileInProjectExists("web/js/default/manifest")
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

    @DisplayName("Compilation via Kotlin daemon with fallback strategy")
    @GradleTest
    fun testDaemonFallbackStrategy(gradleVersion: GradleVersion) {
        doTestExecutionStrategy(
            gradleVersion,
            KotlinCompilerExecutionStrategy.DAEMON,
            addHeapDumpOptions = false,
            testFallbackStrategy = true,
        )
    }

    @DisplayName("Compilation via Kotlin daemon with disabled fallback strategy")
    @GradleTest
    fun testDaemonFallbackStrategyDisabled(gradleVersion: GradleVersion) {
        project(
            projectName = "kotlinBuiltins",
            gradleVersion = gradleVersion,
            addHeapDumpOptions = false
        ) {
            setupProject(this)

            buildAndFail(
                "build",
                "-Pkotlin.compiler.execution.strategy=${KotlinCompilerExecutionStrategy.DAEMON}",
                "-Pkotlin.daemon.jvmargs=-Xmxqwerty",
                "-Pkotlin.daemon.useFallbackStrategy=false"
            ) {
                assertOutputContains("Failed to compile with Kotlin daemon. Fallback strategy (compiling without Kotlin daemon) is turned off.")
            }
        }
    }

    @DisplayName("Compilation via Kotlin daemon with disabled fallback strategy via task property")
    @GradleTest
    fun testDaemonFallbackStrategyDisabledTaskProperty(gradleVersion: GradleVersion) {
        project(
            projectName = "kotlinBuiltins",
            gradleVersion = gradleVersion,
            addHeapDumpOptions = false
        ) {
            setupProject(this)

            // This task configuration action is registered before all the KGP configuration actions,
            // so this test also checks if KGP doesn't override value that is set before KGP configuration actions
            //language=Gradle
            buildGradle.append(
                """
                subprojects {
                    tasks.withType(org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile).configureEach {
                        useDaemonFallbackStrategy = false
                    }
                }
                """.trimIndent()
            )

            buildAndFail(
                "build",
                "-Pkotlin.daemon.jvmargs=-Xmxqwerty",
            ) {
                assertOutputContains("Failed to compile with Kotlin daemon. Fallback strategy (compiling without Kotlin daemon) is turned off.")
            }
        }
    }

    @DisplayName("Compilation inside Gradle daemon configured via task property")
    @GradleTest
    fun testInProcessTaskProperty(gradleVersion: GradleVersion) {
        doTestExecutionStrategy(
            gradleVersion,
            KotlinCompilerExecutionStrategy.IN_PROCESS,
            shouldConfigureStrategyViaGradleProperty = false
        ) {
            // This task configuration action is registered before all the KGP configuration actions,
            // so this test also checks if KGP doesn't override value that is set before KGP configuration actions
            // KT-53617
            //language=Gradle
            buildGradle.append(
                """
                subprojects {
                    tasks.withType(org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile).configureEach {
                        compilerExecutionStrategy = org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy.IN_PROCESS
                    }
                }
                """.trimIndent()
            )
        }
    }

    @DisplayName("Compilation inside Gradle daemon")
    @GradleTest
    fun testInProcess(gradleVersion: GradleVersion) {
        doTestExecutionStrategy(
            gradleVersion,
            KotlinCompilerExecutionStrategy.IN_PROCESS
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

    private fun doTestExecutionStrategy(
        gradleVersion: GradleVersion,
        executionStrategy: KotlinCompilerExecutionStrategy,
        addHeapDumpOptions: Boolean = true,
        testFallbackStrategy: Boolean = false,
        shouldConfigureStrategyViaGradleProperty: Boolean = true,
        additionalProjectConfiguration: TestProject.() -> Unit = {},
    ) {
        project(
            projectName = "kotlinBuiltins",
            gradleVersion = gradleVersion,
            addHeapDumpOptions = addHeapDumpOptions,
            buildOptions = defaultBuildOptions.copy(useDaemonFallbackStrategy = testFallbackStrategy)
        ) {
            setupProject(this)
            additionalProjectConfiguration()

            @OptIn(ExperimentalStdlibApi::class)
            val args = buildList {
                if (shouldConfigureStrategyViaGradleProperty) {
                    add("-Pkotlin.compiler.execution.strategy=${executionStrategy.propertyValue}")
                }
                if (testFallbackStrategy) {
                    // add jvm option that JVM fails to parse
                    add("-Pkotlin.daemon.jvmargs=-Xmxqwerty")
                }
            }.toTypedArray()
            val expectedFinishStrategy = if (testFallbackStrategy) KotlinCompilerExecutionStrategy.OUT_OF_PROCESS else executionStrategy
            val finishMessage = "Finished executing kotlin compiler using $expectedFinishStrategy strategy"

            build("build", *args) {
                assertOutputContains(finishMessage)
                checkOutput(this@project)
                assertNoBuildWarnings()

                if (testFallbackStrategy) {
                    assertOutputContains("Using fallback strategy: Compile without Kotlin daemon")
                } else if (executionStrategy == KotlinCompilerExecutionStrategy.DAEMON) {
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
            build("build", *args) {
                assertOutputContains(finishMessage)
                checkOutputAfterChange(this@project)
                assertNoBuildWarnings()
            }
        }
    }

    protected open fun setupProject(project: TestProject) {
        project.subProject("app").apply {
            buildGradle.append(
                //language=Groovy
                """
                |tasks
                |    .withType(org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile)
                |    .configureEach { 
                |        kotlinOptions.allWarningsAsErrors = true 
                |    }
                |    
                |tasks
                |    .withType(org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile)
                |    .configureEach { 
                |        kotlinOptions.allWarningsAsErrors = false 
                |    }
                """.trimMargin()
            )
        }
    }

    protected abstract fun BuildResult.checkOutput(project: TestProject)
    protected abstract fun BuildResult.checkOutputAfterChange(project: TestProject)
}