/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.karma

import com.google.gson.GsonBuilder
import jetbrains.buildServer.messages.serviceMessages.BaseTestSuiteMessage
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.internal.service.ServiceRegistry
import org.gradle.process.ProcessForkOptions
import org.jetbrains.kotlin.gradle.internal.*
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.targets.js.NpmPackageVersion
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.dsl.WebpackRulesDsl.Companion.webpackRulesContainer
import org.jetbrains.kotlin.gradle.targets.js.internal.appendConfigsFromDir
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectModules
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.*
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework.Companion.CREATE_TEST_EXEC_SPEC_DEPRECATION_MSG
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework.Companion.createTestExecutionSpecDeprecated
import org.jetbrains.kotlin.gradle.targets.js.webTargetVariant
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.web.nodejs.BaseNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.web.nodejs.BaseNodeJsRootExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinTest
import org.jetbrains.kotlin.gradle.utils.appendLine
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.getValue
import org.jetbrains.kotlin.gradle.utils.processes.ExecAsyncHandle
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.slf4j.Logger
import java.io.File
import java.io.PrintWriter
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin.Companion.kotlinNodeJsEnvSpec as wasmKotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin.Companion.kotlinNodeJsRootExtension as wasmKotlinNodeJsRootExtension

class KotlinKarma internal constructor(
    @Transient
    override val compilation: KotlinJsIrCompilation,
    private val basePath: String,
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
) : KotlinJsTestFramework {

    @Deprecated("Manually creating instances of this class is deprecated. Scheduled for removal in Kotlin 2.4.")
    constructor(
        compilation: KotlinJsIrCompilation,
        @Suppress("UNUSED_PARAMETER")
        services: () -> ServiceRegistry,
        basePath: String,
    ) : this(
        compilation = compilation,
        basePath = basePath,
        objects = compilation.target.project.objects,
        providers = compilation.target.project.providers,
    )

    @Transient
    private val project: Project = compilation.target.project
    private val npmProject: NpmProject = compilation.npmProject

    private val platformType: KotlinPlatformType = compilation.platformType

    @Transient
    private val nodeJsRoot: BaseNodeJsRootExtension = compilation.webTargetVariant(
        { project.rootProject.kotlinNodeJsRootExtension },
        { project.rootProject.wasmKotlinNodeJsRootExtension },
    )

    private val versions: NpmVersions by lazy {
        nodeJsRoot.versions
    }

    @Transient
    private val nodeJsEnvSpec: BaseNodeJsEnvSpec = compilation.webTargetVariant(
        { project.kotlinNodeJsEnvSpec },
        { project.wasmKotlinNodeJsEnvSpec },
    )

    private val config: KarmaConfig = KarmaConfig()
    private val requiredDependencies = mutableSetOf<RequiredKotlinJsDependency>()

    private val configurators = mutableListOf<() -> Unit>()
    private val envJsCollector = mutableMapOf<String, String>()
    private val confJsWriters = mutableListOf<(Appendable) -> Unit>()
    private var sourceMaps = false
    private val defaultConfigDirectory = project.projectDir.resolve("karma.config.d")
    private var configDirectory: File by property {
        defaultConfigDirectory
    }
    private val npmProjectDir by project.provider { npmProject.dir }

    private val npmProjectNodeModulesDir by project.provider { npmProject.nodeModulesDir }

    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency>
        get() = requiredDependencies + webpackConfig.getRequiredDependencies(versions)

    override val workingDir: Provider<Directory>
        get() = npmProjectDir

    override val executable: Provider<String> = nodeJsEnvSpec.executable

    override fun getPath() = "$basePath:kotlinKarma"

    override val settingsState: String
        get() = "KotlinKarma($config)"

    internal val isWasm: Boolean = compilation.webTargetVariant(
        jsVariant = false,
        wasmVariant = true,
    )

    internal val npmToolingDir: DirectoryProperty = project.objects.directoryProperty().fileProvider(
        compilation.webTargetVariant(
            { npmProjectDir.map { it.asFile } },
            { (nodeJsRoot as WasmNodeJsRootExtension).npmTooling.map { it.dir } },
        )
    )

    val webpackConfig = KotlinWebpackConfig(
        npmProjectDir = npmProjectDir.map { it.asFile },
        configDirectory = project.projectDir.resolve("webpack.config.d"),
        optimization = KotlinWebpackConfig.Optimization(
            runtimeChunk = null,
            splitChunks = false
        ),
        sourceMaps = true,
        devtool = null,
        export = false,
        progressReporter = true,
        rules = project.objects.webpackRulesContainer(),
        experiments = mutableSetOf("topLevelAwait"),
        resolveLoadersFromKotlinToolingDir = isWasm
    )

    init {
        requiredDependencies.add(versions.karma)
        requiredDependencies.add(versions.kotlinWebHelpers)

        useKotlinReporter()
        useWebpackOutputPlugin()
        useMocha()
        useWebpack()
        useSourceMapSupport()
        usePropBrowsers()

        // necessary for debug as a fallback when no debuggable browsers found
        addChromeLauncher()
    }

    private fun usePropBrowsers() {
        val propValue = project.kotlinPropertiesProvider.jsKarmaBrowsers(compilation.target)
        val propBrowsers = propValue?.split(",")
        propBrowsers?.map(String::trim)?.forEach {
            @Suppress("DEPRECATION")
            when (it.toLowerCaseAsciiOnly()) {
                "chrome" -> useChrome()
                "chrome-canary" -> useChromeCanary()
                "chrome-canary-headless" -> useChromeCanaryHeadless()
                "chrome-headless" -> useChromeHeadless()
                "chrome-headless-no-sandbox" -> useChromeHeadlessNoSandbox()
                "chromium" -> useChromium()
                "chromium-headless" -> useChromiumHeadless()
                "firefox" -> useFirefox()
                "firefox-aurora" -> useFirefoxAurora()
                "firefox-aurora-headless" -> useFirefoxAuroraHeadless()
                "firefox-developer" -> useFirefoxDeveloper()
                "firefox-developer-headless" -> useFirefoxDeveloperHeadless()
                "firefox-headless" -> useFirefoxHeadless()
                "firefox-nightly" -> useFirefoxNightly()
                "firefox-nightly-headless" -> useFirefoxNightlyHeadless()
                "ie" -> useIe()
                "opera" -> useOpera()
                "phantom-js" -> usePhantomJS()
                "safari" -> useSafari()
                else -> project.logger.warn("Unrecognised `kotlin.js.browser.karma.browsers` value [$it]. Ignoring...")
            }
        }
    }

    private fun useKotlinReporter() {
        config.reporters.add("karma-kotlin-reporter")

        confJsWriters.add {
            // Not all log events goes through this appender
            // For example Error in config file
            //language=ES6
            it.appendLine(
                """
                config.plugins = config.plugins || [];
                config.plugins.push('kotlin-web-helpers/dist/karma-kotlin-reporter.js');
                
                config.loggers = [
                    {
                        type: 'kotlin-web-helpers/dist/tc-log-appender.js',
                        //default layout
                        layout: { type: 'pattern', pattern: '%[%d{DATETIME}:%p [%c]: %]%m' }
                    }
                ]
            """.trimIndent()
            )
        }
    }

    private fun useWebpackOutputPlugin() {
        config.frameworks.add("webpack-output")

        confJsWriters.add {
            // Not all log events goes through this appender
            // For example Error in config file
            //language=ES6
            it.appendLine(
                """
                config.plugins = config.plugins || [];
                config.plugins.push('kotlin-web-helpers/dist/karma-webpack-output.js');
            """.trimIndent()
            )
        }
    }

    internal fun watch() {
        config.singleRun = false
        config.autoWatch = true
    }

    fun useConfigDirectory(dir: String) = useConfigDirectory(File(dir))

    fun useConfigDirectory(dir: File) {
        configDirectory = dir
    }

    private fun useChromeLike(id: String) = useBrowser(id, versions.karmaChromeLauncher)

    fun useChrome() = useChromeLike("Chrome")

    fun useChromeHeadless() = useChromeLike("ChromeHeadless")

    fun useChromeHeadlessNoSandbox() {
        val chromeHeadlessNoSandbox = "ChromeHeadlessNoSandbox"

        config.customLaunchers[chromeHeadlessNoSandbox] = CustomLauncher("ChromeHeadless").apply {
            flags.add("--no-sandbox")
        }

        useChromeLike(chromeHeadlessNoSandbox)
    }

    fun useChromium() = useChromeLike("Chromium")

    fun useChromiumHeadless() = useChromeLike("ChromiumHeadless")

    fun useChromeCanary() = useChromeLike("ChromeCanary")

    fun useChromeCanaryHeadless() = useChromeLike("ChromeCanaryHeadless")

    fun useDebuggableChrome() {
        val debuggableChrome = "DebuggableChrome"

        config.customLaunchers[debuggableChrome] = CustomLauncher("Chrome").apply {
            flags.add("--remote-debugging-port=9222")
        }

        useChromeLike(debuggableChrome)
    }

    @Deprecated("It is not supported anymore. Scheduled for removal in Kotlin 2.4.")
    fun usePhantomJS() {
        project.logger.warn("PhantomJS is not supported anymore. Use other browsers instead.")
    }

    private fun useFirefoxLike(id: String) = useBrowser(id, versions.karmaFirefoxLauncher)

    fun useFirefox() = useFirefoxLike("Firefox")

    fun useFirefoxHeadless() = useFirefoxLike("FirefoxHeadless")

    fun useFirefoxDeveloper() = useFirefoxLike("FirefoxDeveloper")

    fun useFirefoxDeveloperHeadless() = useFirefoxLike("FirefoxDeveloperHeadless")

    fun useFirefoxAurora() = useFirefoxLike("FirefoxAurora")

    fun useFirefoxAuroraHeadless() = useFirefoxLike("FirefoxAuroraHeadless")

    fun useFirefoxNightly() = useFirefoxLike("FirefoxNightly")

    fun useFirefoxNightlyHeadless() = useFirefoxLike("FirefoxNightlyHeadless")

    fun useOpera() = useBrowser("Opera", versions.karmaOperaLauncher)

    fun useSafari() = useBrowser("Safari", versions.karmaSafariLauncher)

    fun useIe() = useBrowser("IE", versions.karmaIeLauncher)

    private fun useBrowser(id: String, dependency: NpmPackageVersion) {
        config.browsers.add(id)
        requiredDependencies.add(dependency)
    }

    private fun addChromeLauncher() {
        requiredDependencies.add(versions.karmaChromeLauncher)
    }

    private fun useMocha() {
        requiredDependencies.add(versions.karmaMocha)
        requiredDependencies.add(versions.mocha)
        config.frameworks.add("mocha")
    }

    private fun useWebpack() {
        config.frameworks.add("webpack")
        requiredDependencies.add(versions.karmaWebpack)
        requiredDependencies.add(
            versions.webpack
        )
        requiredDependencies.add(
            versions.webpackCli
        )
        requiredDependencies.add(
            versions.sourceMapLoader
        )

        addPreprocessor("webpack")
        confJsWriters.add {
            it.appendLine()
            it.appendLine("// webpack config")
            it.appendLine("function createWebpackConfig() {")

            webpackConfig.appendTo(it)
            //language=ES6
            it.appendLine(
                """
                // noinspection JSUnnecessarySemicolon
                ;(function(config) {
                    const webpack = require('webpack');
                ${
                    """
                    // https://github.com/webpack/webpack/issues/12951
                    const PatchSourceMapSource = require('kotlin-web-helpers/dist/webpack-5-debug');
                    config.plugins.push(new PatchSourceMapSource())
                    """
                }
                    config.plugins.push(new webpack.SourceMapDevToolPlugin({
                        moduleFilenameTemplate: "[absolute-resource-path]"
                    }))
                })(config);
            """.trimIndent()
            )

            it.appendLine("   return config;")
            it.appendLine("}")
            it.appendLine()
            it.appendLine("config.set({webpack: createWebpackConfig()});")
            it.appendLine()
        }
    }

    fun useSourceMapSupport() {
        requiredDependencies.add(versions.karmaSourcemapLoader)
        sourceMaps = true
        addPreprocessor("sourcemap")
    }

    private fun addPreprocessor(name: String, predicate: (String) -> Boolean = { true }) {
        configurators.add {
            config.files.forEach {
                if (it is String) {
                    if (predicate(it)) {
                        config.preprocessors.getOrPut(it) { mutableListOf() }.add(name)
                    }
                }
            }
        }
    }

    override fun createTestExecutionSpec(
        task: KotlinJsTest,
        launchOpts: ProcessLaunchOptions,
        nodeJsArgs: MutableList<String>,
        debug: Boolean,
    ): TCServiceMessagesTestExecutionSpec {
        val modules = NpmProjectModules(npmToolingDir.getFile())

        val cliArgs = KotlinTestRunnerCliArgs(
            include = task.includePatterns,
            exclude = task.excludePatterns
        )

        val karmaConfJs = npmProject.dir.getFile().resolve("karma.conf.js")

        writeConfig(
            config,
            task.inputFileProperty.getFile(),
            modules,
            task.name,
            cliArgs,
            karmaConfJs.printWriter(),
            configurators,
            confJsWriters,
            envJsCollector,
            platformType,
            npmProject.dir.getFile(),
            debug,
        ) { it.appendFromConfigDir() }

        val karmaConfigAbsolutePath = karmaConfJs.absolutePath
        val args = nodeJsArgs +
                modules.require("karma/bin/karma") +
                listOf("start", karmaConfigAbsolutePath)

        if (isWasm) {
            launchOpts.environment.put(
                "NODE_PATH",
                listOf(
                    npmProjectNodeModulesDir.getFile().normalize().absolutePath,
                    npmToolingDir.getFile().resolve("node_modules").normalize().absolutePath
                ).joinToString(File.pathSeparator)
            )

            launchOpts.environment.put(
                "KOTLIN_TOOLING_DIR",
                npmToolingDir.getFile().resolve("node_modules").normalize().absolutePath
            )
        }

        val clientSettings = TCServiceMessagesClientSettings(
            task.name,
            testNameSuffix = task.targetName,
            prependSuiteName = true,
            stackTraceParser = ::parseNodeJsStackTraceAsJvm,
            ignoreOutOfRootNodes = true,
        )

        return object : JSServiceMessagesTestExecutionSpec(
            processLaunchOpts = launchOpts,
            processArgs = args,
            checkExitCode = true,
            clientSettings = clientSettings
        ) {
            lateinit var progressLogger: ProgressLogger

            override fun wrapExecute(body: () -> Unit) {
                progressLogger = objects.newBuildOpLogger()
                progressLogger.operation("Running and building tests with karma and webpack") {
                    body()
                }
            }

            override fun createClient(testResultProcessor: TestResultProcessor, log: Logger) =
                object : JSServiceMessagesClient(
                    testResultProcessor,
                    clientSettings,
                    log,
                ) {
                    val baseTestNameSuffix get() = settings.testNameSuffix
                    override var testNameSuffix: String? = baseTestNameSuffix

                    private val failedBrowsers: MutableList<String> = mutableListOf()

                    private var stackTraceProcessor =
                        TeamCityMessageStackTraceProcessor()

                    override fun printNonTestOutput(text: String, type: LogType?) {
                        val value = text.trimEnd()
                        progressLogger.progress(value)

                        parseConsole(value, type)
                    }

                    private fun parseConsole(text: String, type: LogType?) {

                        var actualType = type
                        val inStackTrace = stackTraceProcessor.process(text) { line, logType ->
                            log.processLogMessage(line, logType)
                        }

                        if (inStackTrace) return

                        val launcherMessage = KARMA_MESSAGE.matchEntire(text)

                        val actualText = if (launcherMessage != null) {
                            val (logLevel, message) = launcherMessage.destructured
                            actualType = LogType.byValueOrNull(logLevel.toLowerCaseAsciiOnly())

                            val onlyErrorLike: (LogType?) -> Boolean = { processingType -> processingType?.isErrorLike() == true }

                            val failedBrowserProcessor = KarmaConsoleProcessor(
                                onlyErrorLike,
                                { _ -> true },
                                { processingText ->
                                    processFailedBrowsers(processingText)
                                    processingText
                                }
                            )

                            val proxyProcessor = KarmaConsoleRejector(
                                onlyErrorLike
                            ) { processingText ->
                                PROXY_FALSE_WARN.matchEntire(processingText) != null
                            }

                            val webpackOutputProcessor = KarmaConsoleRejector(
                                onlyErrorLike
                            ) { processingText ->
                                WEBPACK_OUTPUT_WARN.matchEntire(processingText) != null
                            }

                            listOf(
                                failedBrowserProcessor,
                                proxyProcessor,
                                webpackOutputProcessor
                            ).fold(message) { acc, processor ->
                                processor.process(actualType, acc)
                            }
                        } else {
                            text
                        }

                        actualType?.let { log.processLogMessage(actualText, it) }
                            ?: super.printNonTestOutput(text, type)

                    }

                    private fun processFailedBrowsers(text: String) {
                        config.browsers
                            .filter { it in text }
                            .filterNot { it in failedBrowsers }
                            .also {
                                failedBrowsers.addAll(it)
                            }
                    }

                    override fun testFailedMessage(execHandle: ExecAsyncHandle, exitValue: Int): String {
                        if (failedBrowsers.isEmpty()) {
                            return super.testFailedMessage(execHandle, exitValue)
                        }

                        val failedBrowsers = failedBrowsers
                            .joinToString("\n") {
                                "- $it"
                            }
                        return """
                            |Errors occurred during launch of browser for testing.
                            |$failedBrowsers
                            |Please make sure that you have installed browsers.
                            |Or change it via
                            |browser {
                            |    testTask {
                            |        useKarma {
                            |            useFirefox()
                            |            useChrome()
                            |            useSafari()
                            |        }
                            |    }
                            |}
                            """.trimMargin()
                    }

                    override fun processStackTrace(stackTrace: String): String =
                        processKarmaStackTrace(stackTrace)

                    override fun getSuiteName(message: BaseTestSuiteMessage): String {
                        val src = message.suiteName.trim()
                        // example: "sample.a DeepPackageTest Inner.HeadlessChrome 74.0.3729 (Mac OS X 10.14.4)"
                        // should be reported as "sample.a.DeepPackageTest.Inner[js,browser,HeadlessChrome74.0.3729,MacOSX10.14.4]"

                        // lets parse it from the end:
                        val os = src.substringAfterLast("(") // Mac OS X 10.14.4)
                            .removeSuffix(")") // Mac OS X 10.14.4
                            .replace(" ", "") // MacOSX10.14.4

                        val withoutOs = src.substringBeforeLast(" (") // sample.a DeepPackageTest Inner.HeadlessChrome 74.0.3729

                        val rawSuiteNameOnly = withoutOs
                            .substringBeforeLast(" ") // sample.a DeepPackageTest Inner.HeadlessChrome
                            .substringBeforeLast(".") // sample.a DeepPackageTest Inner

                        val browser = withoutOs.substring(rawSuiteNameOnly.length + 1) // HeadlessChrome 74.0.3729
                            .replace(" ", "") // HeadlessChrome74.0.3729

                        testNameSuffix = listOfNotNull(baseTestNameSuffix, browser, os)
                            .takeIf { it.isNotEmpty() }
                            ?.joinToString()

                        return rawSuiteNameOnly.replace(" ", ".") // sample.a.DeepPackageTest.Inner
                    }
                }
        }
    }

    private fun Appendable.appendFromConfigDir() {
        if (!configDirectory.isDirectory) {
            return
        }

        appendLine()
        appendConfigsFromDir(configDirectory)
        appendLine()
    }

    @Deprecated(message = CREATE_TEST_EXEC_SPEC_DEPRECATION_MSG)
    override fun createTestExecutionSpec(
        task: KotlinJsTest,
        forkOptions: ProcessForkOptions,
        nodeJsArgs: MutableList<String>,
        debug: Boolean,
    ): TCServiceMessagesTestExecutionSpec =
        createTestExecutionSpecDeprecated(
            task = task,
            forkOptions = forkOptions,
            nodeJsArgs = nodeJsArgs,
            debug = debug,
            objects = objects,
            providers = providers,
        )
}

// In Karma config it means relative path based on basePath which is configured inside Karma config
// It helps to get rid of windows backslashes in proxies config
// `base` is using because of how Karma works with proxies
// http://karma-runner.github.io/6.4/config/configuration-file.html#proxies
// http://karma-runner.github.io/6.4/config/files.html#loading-assets
internal fun basify(npmProjectDir: File, file: File): String {
    return "/base/${file.relativeTo(npmProjectDir).invariantSeparatorsPath}"
}

internal fun createLoadWasm(npmProjectDir: File, file: File): File {
    val static = npmProjectDir.resolve("static").also {
        it.mkdirs()
    }
    val loadJs = static.resolve("load.mjs")
    loadJs.printWriter().use { writer ->
        val relativePath = file.relativeTo(static).invariantSeparatorsPath
        writer.println(
            """
                import * as exports from "$relativePath"
                try {
                    const startUnitTests = "startUnitTests"
                    exports[startUnitTests]?.()
                    window.__karma__.loaded();
                } catch (e) {
                    window.__karma__.error("Problem with loading", void 0, void 0, void 0, e)
                }
            """.trimIndent()
        )
    }

    return loadJs
}

internal fun writeConfig(
    config: KarmaConfig,
    file: File,
    modules: NpmProjectModules,
    taskName: String,
    cliArgs: KotlinTestRunnerCliArgs,
    printWriter: PrintWriter,
    configurators: List<() -> Unit>,
    confJsWriters: List<(Appendable) -> Unit>,
    envJsCollector: Map<String, String>,
    platformType: KotlinPlatformType,
    npmProjectDir: File,
    debug: Boolean,
    writerAction: (Appendable) -> Unit,
) {
    val fileString = file.toString()

    config.files.add(modules.require("kotlin-web-helpers/dist/kotlin-test-karma-runner.js"))

    if (debug) {
        config.singleRun = false
        config.autoWatch = true

        config.browsers.clear()
    }

    if (platformType != KotlinPlatformType.wasm) {
        config.files.add(fileString)
    } else {

        config.files.add(
            createLoadWasm(npmProjectDir, file).normalize().absolutePath
        )

        config.customContextFile = modules.require("kotlin-web-helpers/dist/static/context.html")
        config.customDebugFile = modules.require("kotlin-web-helpers/dist/static/debug.html")

        if (debug) {
            config.webpackCopy.add(
                file.parentFile.resolve(file.nameWithoutExtension + ".wasm.map").absolutePath
            )
        }
    }

    if (config.browsers.isEmpty() && !debug) {
        error("No browsers configured for $taskName")
    }

    config.basePath = npmProjectDir.absolutePath

    configurators.forEach {
        it()
    }

    config.client.args.addAll(cliArgs.toList())

    printWriter.use { confWriter ->
        envJsCollector.forEach { (envVar, value) ->
            //language=JavaScript 1.8
            confWriter.println("process.env.$envVar = $value")
        }

        confWriter.println()
        confWriter.println("module.exports = function(config) {")
        confWriter.println()

        confWriter.print("config.set(")
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
            .toJson(config, confWriter)
        confWriter.println(");")

        confJsWriters.forEach { it(confWriter) }

        writerAction(confWriter)

        confWriter.println()
        confWriter.println("}")
    }
}

private val KARMA_MESSAGE = "^.*\\d{2} \\d{2} \\d{4,} \\d{2}:\\d{2}:\\d{2}.\\d{3}:(ERROR|WARN|INFO|DEBUG|LOG) \\[.*]: ([\\w\\W]*)\$"
    .toRegex()

private val PROXY_FALSE_WARN = "\"/\" is proxied, you should probably change urlRoot to avoid conflicts".toRegex()
private val WEBPACK_OUTPUT_WARN = "All files matched by \".+\" were excluded or matched by prior matchers\\.".toRegex()
