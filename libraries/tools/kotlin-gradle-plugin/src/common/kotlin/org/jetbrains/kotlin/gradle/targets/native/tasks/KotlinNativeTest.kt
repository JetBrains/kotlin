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
import org.gradle.work.NormalizeLineEndings
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

@DisableCachingByDefault(because = "Abstract super-class, not to be instantiated directly")
abstract class KotlinNativeTest
@Inject
internal constructor(
    objects: ObjectFactory?,
    providers: ProviderFactory?,
    execOps: ExecOperations?,
) : KotlinTest(execOps) {

    @Deprecated("Extending this class is deprecated. Scheduled for removal in Kotlin 2.4.")
    // Note to KGP developers: subtypes are still supported for KGP. We just want to prevent users from extending this task.
    constructor() : this(
        objects = null,
        providers = null,
        execOps = null,
    )

    // TODO KT-75294 When the deprecated secondary constructor is removed, move `objects` to a non-null constructor property.
    private val objects: ObjectFactory = objects ?: this.project.objects

    // TODO KT-75294 When the deprecated secondary constructor is removed, move `providers` to a non-null constructor property.
    private val providers: ProviderFactory = providers ?: this.project.providers

    private val processOptions: ProcessLaunchOptions = this.objects.processLaunchOptions {
        environment.putAll(this@KotlinNativeTest.providers.getAllEnvironmentVariables())
    }

    @get:Internal
    abstract val executableProperty: Property<FileCollection>

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
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

    @get:Input
    var workingDir: String
        get() = processOptions.workingDir.get().asFile.absolutePath
        set(value) {
            processOptions.workingDir.set(File(value))
        }

    @get:Internal
    var environment: Map<String, Any>
        get() = processOptions.environment.get()
        set(value) {
            processOptions.environment.set(providers.provider {
                value.mapValues { it.value.toString() }
            })
        }

    private val trackedEnvironmentVariablesKeys = mutableSetOf<String>()


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

    @JvmOverloads
    fun environment(name: String, value: Any, tracked: Boolean = true) {
        processOptions.environment.put(name, providers.provider { value.toString() })
        if (tracked) {
            trackedEnvironmentVariablesKeys.add(name)
        }
    }

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
 */
@DisableCachingByDefault
abstract class KotlinNativeHostTest
@Inject
internal constructor(
    objects: ObjectFactory?,
    providers: ProviderFactory?,
    execOps: ExecOperations?,
) : KotlinNativeTest(
    objects = objects,
    providers = providers,
    execOps = execOps,
) {

    @Deprecated("Extending this class is deprecated. Scheduled for removal in Kotlin 2.4.")
    // Note to KGP developers: subtypes are still supported for KGP. We just want to prevent users from extending this task.
    constructor() : this(
        objects = null,
        providers = null,
        execOps = null,
    )

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
    objects: ObjectFactory?,
    providers: ProviderFactory?,
    execOps: ExecOperations?,
) : KotlinNativeTest(
    objects = objects,
    providers = providers,
    execOps = execOps,
) {

    @Deprecated("Extending this class is deprecated. Scheduled for removal in Kotlin 2.4.")
    // Note to KGP developers: subtypes are still supported for KGP. We just want to prevent users from extending this task.
    constructor() : this(
        objects = null,
        providers = null,
        execOps = null,
    )

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
