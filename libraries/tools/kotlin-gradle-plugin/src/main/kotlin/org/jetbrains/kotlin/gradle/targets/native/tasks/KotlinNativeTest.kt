/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks

import groovy.lang.Closure
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.process.ProcessForkOptions
import org.gradle.process.internal.DefaultProcessForkOptions
import org.jetbrains.kotlin.compilerRunner.konanVersion
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutor.Companion.TC_PROJECT_PROPERTY
import org.jetbrains.kotlin.gradle.targets.native.internal.parseKotlinNativeStackTraceAsJvm
import org.jetbrains.kotlin.gradle.tasks.KotlinTest
import org.jetbrains.kotlin.konan.CompilerVersion
import java.io.File
import java.util.concurrent.Callable

abstract class KotlinNativeTest : KotlinTest() {
    @Suppress("LeakingThis")
    private val processOptions: ProcessForkOptions = DefaultProcessForkOptions(fileResolver)

    @get:Internal
    val executableProperty: Property<FileCollection> = project.objects.property(FileCollection::class.java)

    @get:InputFiles // use FileCollection & @InputFiles rather than @InputFile to allow for task dependencies built-into this FileCollection
    @get:SkipWhenEmpty
    @Suppress("UNUSED") // Gradle input
    internal val executableFiles: FileCollection // Gradle < 5.0 seems to not work properly with @InputFiles Property<FileCollection>
        get() = executableProperty.get()

    private val executableFile
        get() = executableProperty.get().singleFile

    init {
        onlyIf { executableFile.exists() }
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
        get() = processOptions.workingDir.canonicalPath
        set(value) {
            processOptions.workingDir = File(value)
        }

    @get:Input
    var environment: Map<String, Any>
        get() = processOptions.environment
        set(value) {
            processOptions.environment = value
        }

    private fun <T> Property<T>.set(providerLambda: () -> T) = set(project.provider { providerLambda() })

    fun executable(file: File) {
        executableProperty.set(project.files(file))
    }

    fun executable(path: String) {
        executableProperty.set { project.files(path) }
    }

    fun executable(provider: () -> File) {
        executableProperty.set(project.files(Callable { provider() }))
    }

    fun executable(builtByTask: Task, provider: () -> File) {
        executableProperty.set(project.files(Callable { provider() }).builtBy(builtByTask))
    }

    fun executable(provider: Provider<File>) {
        executableProperty.set(provider.map { project.files(it) })
    }

    fun executable(provider: Closure<File>) {
        executableProperty.set(project.provider(provider).map { project.files(it) })
    }

    fun environment(name: String, value: Any) {
        processOptions.environment(name, value)
    }

    @get:Internal
    protected abstract val testCommand: TestCommand

    // KonanVersion doesn't provide an API to compare versions,
    // so we have to transform it to KotlinVersion first.
    // Note: this check doesn't take into account the meta version (release, eap, dev).
    private fun CompilerVersion.isAtLeast(major: Int, minor: Int, patch: Int): Boolean =
        KotlinVersion(this.major, this.minor, this.maintenance).isAtLeast(major, minor, patch)

    override fun createTestExecutionSpec(): TCServiceMessagesTestExecutionSpec {
        val extendedForkOptions = DefaultProcessForkOptions(fileResolver)
        processOptions.copyTo(extendedForkOptions)
        extendedForkOptions.executable = testCommand.executable

        val clientSettings = TCServiceMessagesClientSettings(
            name,
            testNameSuffix = targetName,
            prependSuiteName = targetName != null,
            treatFailedTestOutputAsStacktrace = false,
            stackTraceParser = ::parseKotlinNativeStackTraceAsJvm,
            escapeTCMessagesInLog = project.hasProperty(TC_PROJECT_PROPERTY)
        )

        // The KotlinTest expects that the exit code is zero even if some tests failed.
        // In this case it can check exit code and distinguish test failures from crashes.
        // But K/N allows forcing a zero exit code only since 1.3 (which was included in Kotlin 1.3.40).
        // Thus we check the exit code only for newer versions.
        val checkExitCode = project.konanVersion.isAtLeast(1, 3, 0)

        val cliArgs = testCommand.cliArgs("TEAMCITY", checkExitCode, includePatterns, excludePatterns, args)

        return TCServiceMessagesTestExecutionSpec(extendedForkOptions, cliArgs, checkExitCode, clientSettings)
    }

    protected abstract class TestCommand() {
        abstract val executable: String
        abstract fun cliArgs(
            testLogger: String?,
            checkExitCode: Boolean,
            testGradleFilter: Set<String>,
            testNegativeGradleFilter: Set<String>,
            userArgs: List<String>
        ): List<String>

        protected fun testArgs(
            testLogger: String?,
            checkExitCode: Boolean,
            testGradleFilter: Set<String>,
            testNegativeGradleFilter: Set<String>,
            userArgs: List<String>
        ): List<String> = mutableListOf<String>().also {

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

            it.addAll(userArgs)
        }
    }
}

/**
 * A task running Kotlin/Native tests on a host machine.
 */
open class KotlinNativeHostTest : KotlinNativeTest() {
    @get:Internal
    override val testCommand: TestCommand = object : TestCommand() {
        override val executable: String
            get() = this@KotlinNativeHostTest.executable.absolutePath

        override fun cliArgs(
            testLogger: String?,
            checkExitCode: Boolean,
            testGradleFilter: Set<String>,
            testNegativeGradleFilter: Set<String>,
            userArgs: List<String>
        ): List<String> = testArgs(testLogger, checkExitCode, testGradleFilter, testNegativeGradleFilter, userArgs)
    }
}

/**
 * A task running Kotlin/Native tests on a simulator (iOS/watchOS/tvOS).
 */
// TODO: Support debugging.
open class KotlinNativeSimulatorTest : KotlinNativeTest() {
    @Input
    @Option(option = "device", description = "Sets a simulated device used to execute tests.")
    lateinit var deviceId: String

    @get:Internal
    override val testCommand: TestCommand = object : TestCommand() {
        override val executable: String
            get() = "/usr/bin/xcrun"

        override fun cliArgs(
            testLogger: String?,
            checkExitCode: Boolean,
            testGradleFilter: Set<String>,
            testNegativeGradleFilter: Set<String>,
            userArgs: List<String>
        ): List<String> =
            listOf("simctl", "spawn", "--standalone", deviceId, this@KotlinNativeSimulatorTest.executable.absolutePath, "--") +
                    testArgs(testLogger, checkExitCode, testGradleFilter, testNegativeGradleFilter, userArgs)
    }
}