package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.kotlin.dsl.kotlin
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.internals.asFinishLogMessage
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnosticFactory
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.checkedReplace
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText
import kotlin.test.fail

@DisplayName("Kotlin JS compile execution strategy")
class ExecutionStrategyJsIT : ExecutionStrategyIT() {
    override val defaultBuildOptions: BuildOptions
        // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    override fun setupProject(project: TestProject) {
        super.setupProject(project)

        // transform the project into a multiplatform project with JS target
        val appSubProject = project.subProject("app")
        appSubProject.buildGradle.modify {
            it.replace(
                "id \"org.jetbrains.kotlin.jvm\"",
                "id \"org.jetbrains.kotlin.multiplatform\""
            ) +
                    """
                    |
                    |kotlin {
                    |    js {
                    |        nodejs()
                    |    }
                    |    
                    |    sourceSets {
                    |        jsMain {
                    |           kotlin.srcDir("src/main")
                    |        }
                    |    }
                    |}
                    |
                    |afterEvaluate {
                    |    tasks.named('compileKotlinJs') {
                    |        destinationDirectory = new File(project.projectDir, "web/js/")
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
    override val expectedOutOfProcessDiagnostics: Set<ToolingDiagnosticFactory> = setOf(
        KotlinToolingDiagnostics.UsingOutOfProcessDisablesBuildToolsApi,
        KotlinToolingDiagnostics.OutOfProcessExecutionStrategyUsage,
    )

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
    internal open val expectedOutOfProcessDiagnostics: Set<ToolingDiagnosticFactory>? = null

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
            addHeapDumpOptions = false,
            enableKotlinDaemonMemoryLimitInMb = null,
            buildOptions = defaultBuildOptions.copy(
                useDaemonFallbackStrategy = false,
                compilerExecutionStrategy = KotlinCompilerExecutionStrategy.DAEMON,
            )
        ) {
            setupProject(this)

            buildAndFail(
                "build",
                "-Pkotlin.daemon.jvmargs=-Xmxqwerty",
            ) {
                assertOutputContains("Invalid maximum heap size: -Xmxqwerty")
                assertOutputContains("Failed to compile with Kotlin daemon.")
                assertOutputContains("Fallback strategy (compiling without Kotlin daemon) is turned off.")
            }
        }
    }

    @DisplayName("Compilation via Kotlin daemon with disabled fallback strategy via task property")
    @GradleTest
    fun testDaemonFallbackStrategyDisabledTaskProperty(gradleVersion: GradleVersion) {
        project(
            projectName = "kotlinBuiltins",
            gradleVersion = gradleVersion,
            enableKotlinDaemonMemoryLimitInMb = null,
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
                assertOutputContains("Invalid maximum heap size: -Xmxqwerty")
                assertOutputContains("Failed to compile with Kotlin daemon.")
                assertOutputContains("Fallback strategy (compiling without Kotlin daemon) is turned off.")
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
            subProject("app").buildScriptInjection {
                project.tasks.withType<AbstractKotlinCompile<*>>().configureEach { task ->
                    task.compilerExecutionStrategy.set(KotlinCompilerExecutionStrategy.IN_PROCESS)
                }
            }
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

    @DisplayName("KT-75820: The legacy in-process mode properly reports compilation warnings/errors")
    @GradleTest
    fun testInProcessWithCompilationWarning(gradleVersion: GradleVersion) {
        project(
            projectName = "kotlinBuiltins",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                compilerExecutionStrategy = KotlinCompilerExecutionStrategy.IN_PROCESS,
                runViaBuildToolsApi = false,
            )
        ) {
            subProject("app").kotlinSourcesDir().resolve("classes.kt").modify {
                it.replace(
                    //language=kotlin
                    "class A",
                    //language=kotlin
                    "@Deprecated(\"deprecated for test\") class A",
                )
            }
            build("build") {
                val mainKt = subProject("app").kotlinSourcesDir().resolve("main.kt")
                // ensure warnings are properly parsed
                assertOutputContains("w: ${mainKt.toRealPath().toUri()}:9:5 'constructor(): A' is deprecated. deprecated for test")
            }
        }
    }

    @DisplayName("Compilation via separate compiler process")
    @GradleTest
    fun testOutOfProcess(gradleVersion: GradleVersion) {
        @Suppress("DEPRECATION")
        doTestExecutionStrategy(
            gradleVersion,
            KotlinCompilerExecutionStrategy.OUT_OF_PROCESS,
            expectDiagnostics = expectedOutOfProcessDiagnostics
        )
    }

    private fun doTestExecutionStrategy(
        gradleVersion: GradleVersion,
        executionStrategy: KotlinCompilerExecutionStrategy,
        addHeapDumpOptions: Boolean = true,
        testFallbackStrategy: Boolean = false,
        shouldConfigureStrategyViaGradleProperty: Boolean = true,
        expectDiagnostics: Set<ToolingDiagnosticFactory>? = null,
        additionalProjectConfiguration: TestProject.() -> Unit = {},
    ) {
        project(
            projectName = "kotlinBuiltins",
            gradleVersion = gradleVersion,
            addHeapDumpOptions = addHeapDumpOptions,
            enableKotlinDaemonMemoryLimitInMb = if (shouldConfigureStrategyViaGradleProperty) null else 1024,
            enableGradleDaemonMemoryLimitInMb = null, // We need to make an assertion based on default Gradle Daemon JDK configuration
            buildOptions = defaultBuildOptions.copy(
                useDaemonFallbackStrategy = testFallbackStrategy,
                compilerExecutionStrategy = if (shouldConfigureStrategyViaGradleProperty) {
                    executionStrategy
                } else {
                    null
                },
                logLevel = if (!testFallbackStrategy && executionStrategy == KotlinCompilerExecutionStrategy.DAEMON) {
                    LogLevel.DEBUG // used daemon JVM options are reported only to the DEBUG logs
                } else {
                    defaultBuildOptions.logLevel
                },
            )
        ) {
            setupProject(this)
            additionalProjectConfiguration()

            gradleProperties.appendText(
                """
                |
                |kotlin.jvm.target.validation.mode=ignore
                """.trimMargin()
            )

            val args = if (testFallbackStrategy) {
                arrayOf("-Pkotlin.daemon.jvmargs=-Xmxqwerty")
            } else {
                emptyArray()
            }
            @Suppress("DEPRECATION") val expectedFinishStrategy = when {
                testFallbackStrategy && this@ExecutionStrategyIT is ExecutionStrategyJvmIT -> KotlinCompilerExecutionStrategy.IN_PROCESS
                testFallbackStrategy && this@ExecutionStrategyIT !is ExecutionStrategyJvmIT -> KotlinCompilerExecutionStrategy.OUT_OF_PROCESS
                else -> executionStrategy
            }
            val finishMessage = expectedFinishStrategy.asFinishLogMessage

            build("build", *args) {
                assertOutputContains(expectedFinishStrategy.asFinishLogMessage)
                expectDiagnostics?.forEach { assertHasDiagnostic(it) }
                checkOutput(this@project)

                if (testFallbackStrategy) {
                    assertOutputContains("Invalid maximum heap size: -Xmxqwerty")
                    assertOutputContains("Using fallback strategy: Compile without Kotlin daemon")
                } else if (executionStrategy == KotlinCompilerExecutionStrategy.DAEMON) {
                    // 256m is the default value for Gradle 5.0+
                    val defaultJvmSettingsForGivenGradleVersion =
                        if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_0)) "256" else "384"
                    assertKotlinDaemonJvmOptions(
                        listOf("-XX:MaxMetaspaceSize=${defaultJvmSettingsForGivenGradleVersion}m", "-ea")
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

class NoActiveThreadsAfterCompilerInvocationIT : KGPDaemonsBaseTest() {
    @DisplayName("KT-84152: [BTA] In-process compilation should not leave active threads")
    @Disabled("Isn't fixed for BTA yet")
    @GradleTest
    fun testBta(gradleVersion: GradleVersion) = test(gradleVersion, buildOptions = defaultBuildOptions.copy(runViaBuildToolsApi = true))

    @DisplayName("KT-84152: In-process compilation should not leave active threads")
    @GradleTest
    fun testNonBta(gradleVersion: GradleVersion) = test(gradleVersion, buildOptions = defaultBuildOptions.copy(runViaBuildToolsApi = false))

    private fun test(
        gradleVersion: GradleVersion,
        buildOptions: BuildOptions
    ) {
        project(
            "empty",
            gradleVersion,
            buildOptions = buildOptions.copy(
                // model builder below breaks configuration cache in Gradle 9+,
                // making it configuration cache friendly isn't necessary for this test
                configurationCache = BuildOptions.ConfigurationCacheValue.DISABLED,
                compilerExecutionStrategy = KotlinCompilerExecutionStrategy.IN_PROCESS
            )
        ) {
            plugins {
                kotlin("jvm")
            }

            kotlinSourcesDir().resolve("Foo.kt")
                .createParentDirectories()
                .createFile()
                .writeText("class Foo")

            buildScriptInjection {
                fun makeThreadsSnapshot(): Set<String> = Thread
                    .getAllStackTraces()
                    .keys.groupBy { it.javaClass.name + ":" + it.name }
                    .map { (name, threads) -> "$name (total ${threads.size})" }.toSet()

                project.tasks.named("compileKotlin").configure {
                    it.doFirst {
                        project.extraProperties.set("threadsBeforeKotlinCompile", makeThreadsSnapshot())
                    }

                    it.doLast {
                        project.extraProperties.set("threadsAfterKotlinCompile", makeThreadsSnapshot())
                    }
                }
            }

            val newThreadsAfterExecution = buildModel("compileKotlin") { project ->
                @Suppress("UNCHECKED_CAST")
                val threadsBefore = project.extraProperties.get("threadsBeforeKotlinCompile") as Set<String>
                check(threadsBefore.isNotEmpty()) { "[threadsBefore] snapshot must not be empty" }

                @Suppress("UNCHECKED_CAST")
                val threadsAfter = project.extraProperties.get("threadsAfterKotlinCompile") as Set<String>
                check(threadsAfter.isNotEmpty()) { "[threadsAfter] snapshot must not be empty" }

                threadsAfter - threadsBefore
            }

            val expectedGradleWorkerThreads = listOf(
                """java\.lang\.Thread:pool-\d+-thread-\d+ \(total \d+\)""".toRegex(),
                """java\.lang\.Thread:WorkerExecutor Queue \(total 1\)""".toRegex(),
                """java\.lang\.Thread:Unconstrained build operations Thread \d+ \(total \d+\)""".toRegex(),
            )

            val newThreadsAfterExecutionFiltered = newThreadsAfterExecution.filter { threadInfo ->
                expectedGradleWorkerThreads.all { !threadInfo.matches(it) }
            }

            if (newThreadsAfterExecutionFiltered.isNotEmpty()) {
                fail("Threads were left active after compilation: $newThreadsAfterExecutionFiltered")
            }
        }
    }
}
