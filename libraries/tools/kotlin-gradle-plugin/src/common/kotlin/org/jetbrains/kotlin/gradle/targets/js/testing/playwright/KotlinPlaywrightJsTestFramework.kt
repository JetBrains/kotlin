/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.playwright

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.targets.js.NpmPackageVersion
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTestsLocation
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.npmToolingDir
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectModules
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsBrowserDebugOptions
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsBrowserDebuggableFramework
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinTestRunnerCliArgs
import org.jetbrains.kotlin.gradle.utils.asPathOrNull
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.listProperty
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions
import org.jetbrains.kotlin.gradle.utils.property
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
) : KotlinJsTestFramework, KotlinJsBrowserDebuggableFramework {

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
        val launchEnvironmentVariables: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java)

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

    private val debugPort: Property<Int> = objects.property<Int>().convention(DEFAULT_DEBUG_PORT)

    // null means we don't wait for a debugger before running the tests.
    private var debuggerReadyPort: Int? = null
    private var debuggerReadyTimeoutMillis: Int = DEFAULT_DEBUGGER_READY_TIMEOUT_MILLIS

    override fun configureDebug(options: KotlinJsBrowserDebugOptions) {
        options.debugPort?.let { debugPort.set(it) }
        options.debuggerReadyPort?.let { debuggerReadyPort = it }
        options.debuggerReadyTimeoutMillis?.let { debuggerReadyTimeoutMillis = it }
    }

    @get:Internal
    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency> = setOf(
        NpmPackageVersion("playwright-core", PLAYWRIGHT_VERSION)
    )

    @get:Internal
    internal val npmToolingEnvDir: DirectoryProperty = objects.directoryProperty().convention(compilation.npmToolingDir())

    override fun createTestExecuter(): TestExecuter<*> = PlaywrightTestExecutor()

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

        if (debug && frameworkTaskInputs.chromiumRunners.get().isEmpty()) {
            task.logger.warn(
                "No Chromium runner is configured for Playwright debugging. " +
                        "Kotlin will launch Chromium using the first configured browser runner's test settings. " +
                        "Define a Chromium runner in the browser test DSL to customize the debug browser configuration."
            )
        }

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
        val remoteDebuggingPort = debugPort.get()

        return PwDebugOptions(
            remoteDebuggingPort = remoteDebuggingPort,
            debuggerReadyPort = debuggerReadyPort,
            debuggerReadyTimeoutMillis = debuggerReadyTimeoutMillis,
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
        private const val DEFAULT_DEBUG_PORT = 9222
        private const val DEFAULT_DEBUGGER_READY_TIMEOUT_MILLIS = 30_000
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
