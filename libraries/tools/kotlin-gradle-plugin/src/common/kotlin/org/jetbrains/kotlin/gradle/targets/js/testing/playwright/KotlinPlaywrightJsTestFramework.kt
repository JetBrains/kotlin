/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.playwright

import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.mapProperty
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.targets.js.NpmPackageVersion
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTestsLocation
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.npmToolingDir
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectModules
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinTestRunnerCliArgs
import org.jetbrains.kotlin.gradle.utils.asPathOrNull
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.listProperty
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions
import org.jetbrains.kotlin.gradle.utils.property
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URI
import java.nio.file.Path
import java.time.Duration
import javax.inject.Inject
import kotlin.time.toKotlinDuration


/**
 * Kotlin/JS browser test framework backed by [Playwright][com.microsoft.playwright.Playwright]
 */
internal class KotlinPlaywrightJsTestFramework(
    @Transient override val compilation: KotlinJsIrCompilation,
    override val frameworkTaskInputs: Inputs,
    private val objects: ObjectFactory,
) : KotlinJsTestFramework {

    abstract class Inputs @Inject constructor(objects: ObjectFactory) {
        @get:Nested
        val chromiumRunners: ListProperty<ChromiumRunnerInput> = objects.listProperty()

        @get:Nested
        val firefoxRunners: ListProperty<FirefoxRunnerInput> = objects.listProperty()

        @get:Nested
        val webkitRunners: ListProperty<WebkitRunnerInput> = objects.listProperty()

        @get:InputDirectory
        val playwrightBrowsersDirectory: DirectoryProperty = objects.directoryProperty()
    }

    /**
     * Common per-runner input properties. All concrete runner inputs (chromium, firefox,
     * webkit) extend this class to share the same set of Gradle-tracked properties.
     */
    abstract class BrowserRunnerInput @Inject constructor(objects: ObjectFactory) {
        @get:Nested
        abstract val testsLocation: Property<KotlinJsTestsLocation>

        @get:Input
        abstract val name: Property<String>

        @get:Input
        val timeout: Property<Duration> = objects.property<Duration>()

        @get:Input
        val headless: Property<Boolean> = objects.property<Boolean>()

        @get:Input
        val launchArgs: ListProperty<String> = objects.listProperty()

        @get:Input
        val launchEnvironmentVariables: MapProperty<String, String> = objects.mapProperty()

        @get:Optional
        @get:InputFile
        val customBrowserExecutable: RegularFileProperty = objects.fileProperty()

        @get:Input
        val finishMarker: Property<String> = objects.property<String>().convention("KOTLIN_TEST_FINISHED")
    }

    abstract class ChromiumRunnerInput @Inject constructor(objects: ObjectFactory) : BrowserRunnerInput(objects)
    abstract class FirefoxRunnerInput @Inject constructor(objects: ObjectFactory) : BrowserRunnerInput(objects)
    abstract class WebkitRunnerInput @Inject constructor(objects: ObjectFactory) : BrowserRunnerInput(objects)

    override val settingsState: String = "playwright"

    override val workingDir: DirectoryProperty = objects.directoryProperty()

    @Transient
    private val nodeJs = compilation.project.kotlinNodeJsEnvSpec

    override val executable: Property<String> = objects.property(nodeJs.executable)

    private val debugPort: Property<Int> = objects.property<Int>()

    @Transient
    private var debuggerReadySocket: ServerSocket? = null

    class DebugSessionInfo(
        val url: URI,
        val bundleDirectory: File,
        val debuggerReadyPort: Int,
    )

    /** Prepares everything IDEA needs before it creates the remote debug configuration. */
    @Suppress("unused")
    fun prepareDebugSession(task: KotlinJsTest): DebugSessionInfo {
        if (frameworkTaskInputs.chromiumRunners.get().isEmpty()) {
            task.logger.warn(
                "No Chromium runner is configured for Playwright debugging. " +
                        "Kotlin will launch Chromium using the first configured browser runner's test settings. " +
                        "Define a Chromium runner in the browser test DSL to customize the debug browser configuration."
            )
        }
        val runner = getDebugRunner()
        return DebugSessionInfo(
            url = buildDebugUrl(task, runner),
            bundleDirectory = runner.testsLocation.get().bundleLocation.get().asFile,
            debuggerReadyPort = prepareDebuggerReadyPort(),
        )
    }

    /** Completes the two-phase setup with the CDP port allocated by IDEA. */
    @Suppress("unused")
    fun configureRemoteDebuggingPort(remoteDebuggingPort: Int) {
        debugPort.set(remoteDebuggingPort)
    }

    private fun prepareDebuggerReadyPort(): Int {
        check(debuggerReadySocket == null) { "Playwright debugger readiness socket is already prepared" }
        return ServerSocket(0, 1, InetAddress.getLoopbackAddress()).also {
            it.soTimeout = DEBUGGER_READY_TIMEOUT_MILLIS
            debuggerReadySocket = it
        }.localPort
    }

    @get:Internal
    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency> = setOf(
        NpmPackageVersion("playwright-core", PLAYWRIGHT_VERSION)
    )

    @get:Internal
    internal val npmToolingEnvDir: DirectoryProperty = objects.directoryProperty().convention(compilation.npmToolingDir())

    override fun createTestExecuter(): TestExecuter<*> = PlaywrightTestExecutor()

    /**
     * Used by IntelliJ IDEA to determine the Playwright test page URL for browser test debug sessions.
     * Called from the Ultimate Gradle init script before the browser is launched, so source mappings can be prepared.
     */
    @Suppress("unused")
    fun buildDebugUrl(task: KotlinJsTest): URI = buildDebugUrl(task, getDebugRunner())

    private fun buildDebugUrl(task: KotlinJsTest, runner: BrowserRunnerInput): URI {
        val cliArgs = KotlinTestRunnerCliArgs(
            include = task.includePatterns,
            exclude = task.excludePatterns,
        ).toList()
        return runner.buildRunnerUrl(runner.testsLocation.get().url.get(), cliArgs)
    }

    private fun getDebugRunner(): ChromiumRunnerInput {
        frameworkTaskInputs.chromiumRunners.get().firstOrNull()?.let { return it }

        val configuredRunner = firstRunnerInput()
        return createChromiumInputs(objects).apply {
            name.convention("chromium")
            testsLocation.convention(configuredRunner.testsLocation)
            timeout.convention(configuredRunner.timeout)
            headless.convention(true)
            launchArgs.convention(emptyList())
            launchEnvironmentVariables.convention(emptyMap())
            finishMarker.convention(configuredRunner.finishMarker)
        }
    }

    override fun createTestExecutionSpec(
        task: KotlinJsTest,
        launchOpts: ProcessLaunchOptions,
        nodeJsArgs: MutableList<String>,
        debug: Boolean,
    ): PwExecutionSpec {
        val clientSettings = TCServiceMessagesClientSettings(
            rootNodeName = task.name,
            testNameSuffix = task.targetName,
            prependSuiteName = true,
            stackTraceParser = ::parseNodeJsStackTraceAsJvm,
            ignoreOutOfRootNodes = true,
        )

        val cliArgs = KotlinTestRunnerCliArgs(
            include = task.includePatterns,
            exclude = task.excludePatterns,
        ).toList()

        val browsersDirectory = frameworkTaskInputs.playwrightBrowsersDirectory.getFile().toPath()
        val debugOptions = if (debug) takeDebugOptions() else null

        val pwRunners = buildList {
            if (debugOptions != null) {
                val runner = getDebugRunner()
                add(
                    runner.createPwRunnerSpec(
                        PwBrowserKind.CHROMIUM,
                        browsersDirectory,
                        cliArgs,
                        debugOptions,
                    )
                )
            } else {
                frameworkTaskInputs.chromiumRunners.get().forEach {
                    add(it.createPwRunnerSpec(PwBrowserKind.CHROMIUM, browsersDirectory, cliArgs, debugOptions))
                }
                frameworkTaskInputs.firefoxRunners.get().forEach {
                    add(it.createPwRunnerSpec(PwBrowserKind.FIREFOX, browsersDirectory, cliArgs, debugOptions))
                }
                frameworkTaskInputs.webkitRunners.get().forEach {
                    add(it.createPwRunnerSpec(PwBrowserKind.WEBKIT, browsersDirectory, cliArgs, debugOptions))
                }
            }
        }

        val npmToolingEnv = npmToolingEnvDir.getFile()
        val modules = NpmProjectModules(npmToolingEnv)

        return PwExecutionSpec(
            createClient = { processor, logger -> PlaywrightTCServiceMessagesClient(processor, clientSettings, logger) },
            runners = pwRunners,
            nodeExecutable = executable.get(),
            playwrightCli = modules.require("playwright-core/cli.js"),
        )
    }

    private fun takeDebugOptions(): PwDebugOptions {
        if (!debugPort.isPresent) {
            throw GradleException("IDEA did not configure a remote debugging port for this Playwright debug run")
        }
        val remoteDebuggingPort = debugPort.get()

        // The execution spec owns and closes the socket after this point.
        val readySocket = debuggerReadySocket
            ?: throw GradleException("IDEA did not configure debugger attachment synchronization for this Playwright debug run")
        debuggerReadySocket = null
        return PwDebugOptions(
            remoteDebuggingPort = remoteDebuggingPort,
            debuggerReadySocket = readySocket,
        )
    }

    private fun BrowserRunnerInput.createPwRunnerSpec(
        kind: PwBrowserKind,
        browsersDirectory: Path,
        cliArgs: List<String>,
        debugOptions: PwDebugOptions?,
    ): PwRunnerSpec = PwRunnerSpec(
        name = name.get(),
        browserKind = kind,
        browsersDirectory = browsersDirectory,
        testsLocation = testsLocation.get(),
        buildTestsExecutionerUrl = { baseUrl -> buildRunnerUrl(baseUrl, cliArgs) },
        timeout = timeout.get().toKotlinDuration(),
        finishMarker = finishMarker.get(),
        headless = headless.get(),
        launchArgs = launchArgs.get(),
        launchEnvironmentVariables = launchEnvironmentVariables.get(),
        customBrowserExecutable = customBrowserExecutable.asPathOrNull,
        debugOptions = debugOptions,
    )

    private fun BrowserRunnerInput.buildRunnerUrl(baseUrl: URI, cliArgs: List<String>): URI {
        val runnerConfig = KotlinBrowserRunnerConfig(
            timeout = timeout.get(),
            testsFinishedMarker = finishMarker.get(),
            kotlinTestCliArguments = cliArgs
        )
        return runnerConfig.buildUrlWithConfigState(baseUrl)
    }

    private fun firstRunnerInput(): BrowserRunnerInput =
        frameworkTaskInputs.chromiumRunners.get().firstOrNull()
            ?: frameworkTaskInputs.firefoxRunners.get().firstOrNull()
            ?: frameworkTaskInputs.webkitRunners.get().firstOrNull()
            ?: error("No Playwright browser runners configured")

    companion object {
        private const val DEBUGGER_READY_TIMEOUT_MILLIS = 30_000

        fun createInputs(objects: ObjectFactory): Inputs =
            objects.newInstance(Inputs::class.java)

        fun createChromiumInputs(objects: ObjectFactory): ChromiumRunnerInput =
            objects.newInstance(ChromiumRunnerInput::class.java)

        fun createFirefoxInputs(objects: ObjectFactory): FirefoxRunnerInput =
            objects.newInstance(FirefoxRunnerInput::class.java)

        fun createWebkitInputs(objects: ObjectFactory): WebkitRunnerInput =
            objects.newInstance(WebkitRunnerInput::class.java)
    }
}
