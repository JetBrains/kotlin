/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks

import groovy.lang.Closure
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.targets.native.internal.NativeAppleSimulatorTCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.targets.native.internal.parseKotlinNativeStackTraceAsJvm
import org.jetbrains.kotlin.gradle.tasks.KotlinTest
import org.jetbrains.kotlin.gradle.utils.SystemGetEnvSource.Companion.getAllEnvironmentVariables
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions.Companion.processLaunchOptions
import java.io.File
import javax.inject.Inject

/**
 * Base class for tasks running Kotlin/Native tests by launching a test
 * executable.
 *
 * Task inputs and outputs are tracked so that the task can participate in
 * up-to-date checks and (for cacheable subclasses) in the build cache. The
 * correctness contract for a restored-from-cache run is:
 *
 *  - The test executable is the primary input (its content, plus the task
 *    dependencies carried by the `FileCollection` it comes from).
 *  - Only environment variables explicitly registered with the
 *    `environment(name, value)` function (tracked by default) or
 *    [trackEnvironment] are part of the task inputs. The test process still
 *    inherits the whole environment of the Gradle process, so tests must be
 *    hermetic with respect to any variable they read but do not track.
 *  - The working directory is tracked relative to the project directory (see
 *    [workingDir]). Files *in* the working directory are not inputs: tests
 *    reading files relative to it must ensure those files are hermetic.
 *  - State of the machine running the tests (OS version, architecture of the
 *    host, Rosetta translation mode) is not part of the inputs. Reusing cache
 *    entries across machines assumes the machines are equivalent.
 */
@DisableCachingByDefault(because = "Abstract super-class, not to be instantiated directly")
abstract class KotlinNativeTest
@Inject
internal constructor(
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
    execOps: ExecOperations,
) : KotlinTest(execOps) {

    @get:Internal
    internal val processOptions: ProcessLaunchOptions = this.objects.processLaunchOptions {
        environment.putAll(this@KotlinNativeTest.providers.getAllEnvironmentVariables())
    }

    /**
     * Captured at configuration time: the deprecated `Task.project` must not
     * be used at execution time when Gradle snapshots task inputs.
     */
    private val projectDirectory: File = project.projectDir

    @get:Internal
    abstract val executableProperty: Property<FileCollection>

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    @get:InputFiles // use FileCollection & @InputFiles rather than @InputFile to allow for task dependencies built-into this FileCollection
    @get:SkipWhenEmpty
    @Suppress("UNUSED") // Gradle input
    internal val executableFiles: FileCollection // Gradle < 5.0 seems to not work properly with @InputFiles Property<FileCollection>
        get() = executableProperty.get()

    private val executableFile
        get() = executableProperty.get().singleFile

    init {
        super.onlyIf { executableFile.exists() }
    }

    @Input
    var args: List<String> = emptyList()

    // Already taken into account in the executableProperty.
    @get:Internal
    var executable: File
        get() = executableProperty.get().singleFile
        set(value) {
            executableProperty.set(project.files(value))
        }

    /**
     * The working directory for the test process.
     *
     * The absolute path is intentionally not a task input: what matters for
     * the correctness of up-to-date checks and the build cache is the
     * *content* of the working directory, not its absolute location.
     * [relativizedWorkingDir] is tracked instead, so that inputs do not leak
     * this machine's checkout path.
     */
    @get:Internal
    var workingDir: String
        get() = processOptions.workingDir.get().asFile.absolutePath
        set(value) {
            processOptions.workingDir.set(File(value))
        }

    /**
     * [workingDir] relativized against the project directory when located
     * inside it (as configured by default), or the absolute path otherwise.
     */
    @get:Input
    @Suppress("UNUSED") // Gradle input
    internal val relativizedWorkingDir: String
        get() = processOptions.workingDir.get().asFile.relativeToOrSelf(projectDirectory).path

    /**
     * Environment variables to pass to the test process, replacing the
     * default inheritance of the Gradle process' environment.
     *
     * Variables assigned through this property are NOT tracked as task
     * inputs. Use the `environment(name, value)` function (tracked by
     * default) or [trackEnvironment] to make a variable participate in
     * up-to-date checks and the build cache key.
     */
    @get:Internal
    var environment: Map<String, Any>
        get() = processOptions.environment.get()
        set(value) {
            processOptions.environment.set(providers.provider {
                value.mapValues { it.value.toString() }
            })
        }

    private val trackedEnvironmentVariablesKeys = mutableSetOf<String>()

    /**
     * The environment variables that participate in the up-to-date check and
     * the build cache key: the ones assigned with the `environment(name,
     * value)` function (tracked by default) or explicitly registered with
     * [trackEnvironment].
     */
    @Suppress("unused")
    @get:Input
    val trackedEnvironment: Map<String, Any>
        get() = environment.filterKeys(trackedEnvironmentVariablesKeys::contains)

    fun executable(file: File) {
        executableProperty.set(project.files(file))
    }

    fun executable(path: String) {
        executableProperty.set(providers.provider { project.files(path) })
    }

    fun executable(provider: () -> File) {
        executableProperty.set(project.files({ provider() }))
    }

    fun executable(builtByTask: Task, provider: () -> File) {
        executableProperty.set(project.files({ provider() }).builtBy(builtByTask))
    }

    fun executable(provider: Provider<File>) {
        executableProperty.set(project.files(provider))
    }

    fun executable(provider: Closure<File>) {
        executableProperty.set(project.provider(provider).map { project.files(it) })
    }

    /**
     * Adds an environment variable for the test process. Unless
     * `tracked = false` is passed, the variable participates in the
     * up-to-date check and the build cache key.
     */
    @JvmOverloads
    fun environment(name: String, value: Any, tracked: Boolean = true) {
        processOptions.environment.put(name, providers.provider { value.toString() })
        if (tracked) {
            trackedEnvironmentVariablesKeys.add(name)
        }
    }

    /**
     * Registers an environment variable (e.g. one inherited by the test
     * process from the Gradle process' environment) as a task input, so that
     * changing its value invalidates up-to-date checks and the build cache.
     *
     * Tests reading environment variables they do not track are not
     * hermetic: a run restored from cache may have been produced under a
     * different value of such a variable.
     */
    @JvmOverloads
    fun trackEnvironment(name: String, tracked: Boolean = true) {
        if (tracked) {
            trackedEnvironmentVariablesKeys.add(name)
        } else {
            trackedEnvironmentVariablesKeys.remove(name)
        }
    }

    @get:Internal
    protected abstract val testCommand: TestCommand

    override fun createTestExecutionSpec(): TCServiceMessagesTestExecutionSpec {
        processOptions.executable.set(testCommand.executable)

        val clientSettings = TCServiceMessagesClientSettings(
            name,
            testNameSuffix = targetName,
            prependSuiteName = targetName != null,
            treatFailedTestOutputAsStacktrace = false,
            stackTraceParser = ::parseKotlinNativeStackTraceAsJvm,
        )

        // The KotlinTest expects that the exit code is zero even if some tests failed.
        // In this case it can check exit code and distinguish test failures from crashes.
        val checkExitCode = true

        val cliArgs = testCommand.cliArgs("TEAMCITY", checkExitCode, includePatterns, excludePatterns, args)

        return TCServiceMessagesTestExecutionSpec(
            processLaunchOptions = processOptions,
            processArgs = cliArgs,
            checkExitCode = checkExitCode,
            clientSettings = clientSettings,
        )
    }

    protected abstract class TestCommand {
        abstract val executable: String

        abstract fun cliArgs(
            testLogger: String?,
            checkExitCode: Boolean,
            testGradleFilter: Set<String>,
            testNegativeGradleFilter: Set<String>,
            userArgs: List<String>,
        ): List<String>

        protected fun testArgs(
            testLogger: String?,
            checkExitCode: Boolean,
            testGradleFilter: Set<String>,
            testNegativeGradleFilter: Set<String>,
            userArgs: List<String>,
        ): List<String> = mutableListOf<String>().also {
            // during debug from IDE executable is switched and special arguments are added
            // via Gradle task manipulation; these arguments are expected to precede test settings
            it.addAll(userArgs)

            if (checkExitCode) {
                // Avoid returning a non-zero exit code in case of failed tests.
                it.add("--ktest_no_exit_code")
            }

            if (testLogger != null) {
                it.add("--ktest_logger=$testLogger")
            }

            if (testGradleFilter.isNotEmpty()) {
                it.add("--ktest_gradle_filter=${testGradleFilter.joinToString(",")}")
            }

            if (testNegativeGradleFilter.isNotEmpty()) {
                it.add("--ktest_negative_gradle_filter=${testNegativeGradleFilter.joinToString(",")}")
            }
        }
    }
}

/**
 * A task running Kotlin/Native tests on a host machine.
 *
 * Caching is enabled for this task type: with the build cache enabled, a
 * restored-from-cache run replays previously recorded test results instead
 * of executing the tests. See [KotlinNativeTest] for the correctness
 * contract of the task inputs, and [KotlinTest] for why runs with ignored
 * test failures are never stored.
 */
@CacheableTask
abstract class KotlinNativeHostTest
@Inject
internal constructor(
    objects: ObjectFactory,
    providers: ProviderFactory,
    execOps: ExecOperations,
) : KotlinNativeTest(
    objects = objects,
    providers = providers,
    execOps = execOps,
) {

    @get:Internal
    override val testCommand: TestCommand = object : TestCommand() {
        override val executable: String
            get() = this@KotlinNativeHostTest.executable.absolutePath

        override fun cliArgs(
            testLogger: String?,
            checkExitCode: Boolean,
            testGradleFilter: Set<String>,
            testNegativeGradleFilter: Set<String>,
            userArgs: List<String>,
        ): List<String> = testArgs(testLogger, checkExitCode, testGradleFilter, testNegativeGradleFilter, userArgs)
    }
}

/**
 * A task running Kotlin/Native tests on a simulator (iOS/watchOS/tvOS).
 */
@DisableCachingByDefault
abstract class KotlinNativeSimulatorTest
@Inject
internal constructor(
    objects: ObjectFactory,
    providers: ProviderFactory,
    execOps: ExecOperations,
) : KotlinNativeTest(
    objects = objects,
    providers = providers,
    execOps = execOps,
) {

    @Deprecated("Use the property 'device' instead. Scheduled for removal in Kotlin 2.3.", level = DeprecationLevel.ERROR)
    @get:Internal
    var deviceId: String
        get() = device.get()
        set(value) {
            device.set(value)
        }

    @get:Input
    @get:Option(option = "device", description = "Sets a simulated device used to execute tests.")
    abstract val device: Property<String>

    @Internal
    var debugMode = false

    @get:Input
    abstract val standalone: Property<Boolean> // disabled standalone means that xcode won't handle simulator boot/shutdown automatically

    @get:Internal
    override val testCommand: TestCommand = object : TestCommand() {
        override val executable: String
            get() = "/usr/bin/xcrun"

        override fun cliArgs(
            testLogger: String?,
            checkExitCode: Boolean,
            testGradleFilter: Set<String>,
            testNegativeGradleFilter: Set<String>,
            userArgs: List<String>,
        ): List<String> =
            listOfNotNull(
                "simctl",
                "spawn",
                "--wait-for-debugger".takeIf { debugMode },
                "--standalone".takeIf { standalone.get() },
                device.get(),
                this@KotlinNativeSimulatorTest.executable.absolutePath,
                "--"
            ) +
                    testArgs(testLogger, checkExitCode, testGradleFilter, testNegativeGradleFilter, userArgs)
    }

    override fun createTestExecutionSpec(): TCServiceMessagesTestExecutionSpec {
        val origin = super.createTestExecutionSpec()
        return NativeAppleSimulatorTCServiceMessagesTestExecutionSpec(
            processLaunchOpts = origin.processLaunchOptions,
            processArgs = origin.processArgs,
            checkExitCode = origin.checkExitCode,
            clientSettings = origin.clientSettings,
            dryRunArgs = origin.dryRunArgs,
            standaloneMode = standalone,
        )
    }
}
