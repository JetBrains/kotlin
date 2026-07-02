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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.mapProperty
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClient
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.targets.js.NpmPackageVersion
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTestsLocation
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectModules
import org.jetbrains.kotlin.gradle.targets.js.npm.npmToolingDir
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinTestRunnerCliArgs
import org.jetbrains.kotlin.gradle.targets.web.nodejs.nodeJsRoot
import org.jetbrains.kotlin.gradle.utils.directoryProperty
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.asPathOrNull
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
        val timeout: Property<Duration> = objects.property<Duration>().convention(Duration.ofMinutes(1))

        @get:Input
        val headless: Property<Boolean> = objects.property<Boolean>().convention(true)

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

    @get:Internal
    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency> = setOf(
        NpmPackageVersion("playwright-core", "1.60.0")
    )

    @get:Internal
    internal val npmToolingEnvDir: DirectoryProperty = objects.directoryProperty(compilation.npmToolingDir)

    override fun createTestExecuter(): TestExecuter<*> = PlaywrightTestExecutor()

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

        val pwRunners = buildList {
            frameworkTaskInputs.chromiumRunners.get().forEach {
                add(it.createPwRunnerSpec(PwBrowserKind.CHROMIUM, browsersDirectory, cliArgs))
            }
            frameworkTaskInputs.firefoxRunners.get().forEach {
                add(it.createPwRunnerSpec(PwBrowserKind.FIREFOX, browsersDirectory, cliArgs))
            }
            frameworkTaskInputs.webkitRunners.get().forEach {
                add(it.createPwRunnerSpec(PwBrowserKind.WEBKIT, browsersDirectory, cliArgs))
            }
        }

        val npmToolingEnv = npmToolingEnvDir.getFile()
        val modules = NpmProjectModules(npmToolingEnv)

        return PwExecutionSpec(
            createClient = { processor, logger -> TCServiceMessagesClient(processor, clientSettings, logger) },
            runners = pwRunners,
            nodeExecutable = executable.get(),
            playwrightCli = modules.require("playwright-core/cli.js"),
        )
    }

    private fun BrowserRunnerInput.createPwRunnerSpec(
        kind: PwBrowserKind,
        browsersDirectory: Path,
        cliArgs: List<String>,
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
        customBrowserExecutable = customBrowserExecutable.asPathOrNull
    )

    private fun BrowserRunnerInput.buildRunnerUrl(baseUrl: String, cliArgs: List<String>): String {
        val runnerConfig = KotlinBrowserRunnerConfig(
            timeout = timeout.get(),
            testsFinishedMarker = finishMarker.get(),
            kotlinTestCliArguments = cliArgs
        )
        return runnerConfig.buildUrlWithConfigState(URI(baseUrl)).toString()
    }

    companion object {
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
