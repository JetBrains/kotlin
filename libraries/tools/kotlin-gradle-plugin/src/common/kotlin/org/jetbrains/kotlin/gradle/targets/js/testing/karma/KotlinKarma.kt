/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.karma

import com.google.gson.GsonBuilder
import jetbrains.buildServer.messages.serviceMessages.BaseTestSuiteMessage
import org.gradle.api.Project
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.internal.service.ServiceRegistry
import org.gradle.process.ProcessForkOptions
import org.gradle.process.internal.ExecHandle
import org.jetbrains.kotlin.gradle.internal.LogType
import org.jetbrains.kotlin.gradle.internal.TeamCityMessageStackTraceProcessor
import org.jetbrains.kotlin.gradle.internal.operation
import org.jetbrains.kotlin.gradle.internal.processLogMessage
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutor
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.internal.MppTestReportHelper
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.NpmPackageVersion
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.appendConfigsFromDir
import org.jetbrains.kotlin.gradle.targets.js.dsl.WebpackRulesDsl.Companion.webpackRulesContainer
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.jsQuoted
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.*
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.js.webpack.WebpackMajorVersion
import org.jetbrains.kotlin.gradle.targets.js.webpack.WebpackMajorVersion.Companion.choose
import org.jetbrains.kotlin.gradle.tasks.KotlinTest
import org.jetbrains.kotlin.gradle.testing.internal.reportsDir
import org.jetbrains.kotlin.gradle.utils.appendLine
import org.jetbrains.kotlin.gradle.utils.isParentOf
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.slf4j.Logger
import java.io.File

class KotlinKarma(
    @Transient override val compilation: KotlinJsCompilation,
    private val services: () -> ServiceRegistry,
    private val basePath: String
) : KotlinJsTestFramework {
    @Transient
    private val project: Project = compilation.target.project
    private val npmProject = compilation.npmProject

    @Transient
    private val nodeJs = NodeJsRootPlugin.apply(project.rootProject)
    private val nodeRootPackageDir by lazy { nodeJs.rootPackageDir }
    private val versions = nodeJs.versions
    private val nodeModulesCacheDir = nodeJs.nodeModulesGradleCacheDir

    private val config: KarmaConfig = KarmaConfig()
    private val requiredDependencies = mutableSetOf<RequiredKotlinJsDependency>()

    private val configurators = mutableListOf<(KotlinTest) -> Unit>()
    private val envJsCollector = mutableMapOf<String, String>()
    private val confJsWriters = mutableListOf<(Appendable) -> Unit>()
    private var sourceMaps = false
    private val defaultConfigDirectory = project.projectDir.resolve("karma.config.d")
    private var configDirectory: File by property {
        defaultConfigDirectory
    }
    private val isTeamCity = project.providers.gradleProperty(TCServiceMessagesTestExecutor.TC_PROJECT_PROPERTY)

    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency>
        get() = requiredDependencies + webpackConfig.getRequiredDependencies(versions)

    override fun getPath() = "$basePath:kotlinKarma"

    override val settingsState: String
        get() = "KotlinKarma($config)"

    private val webpackMajorVersion = PropertiesProvider(project).webpackMajorVersion

    val webpackConfig = KotlinWebpackConfig(
        configDirectory = project.projectDir.resolve("webpack.config.d"),
        optimization = KotlinWebpackConfig.Optimization(
            runtimeChunk = false,
            splitChunks = false
        ),
        sourceMaps = true,
        devtool = null,
        export = false,
        progressReporter = true,
        progressReporterPathFilter = nodeRootPackageDir,
        webpackMajorVersion = webpackMajorVersion,
        rules = project.objects.webpackRulesContainer(),
    )

    init {
        requiredDependencies.add(versions.kotlinJsTestRunner)
        requiredDependencies.add(versions.karma)

        useKotlinReporter()
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
                config.plugins.push('kotlin-test-js-runner/karma-kotlin-reporter.js');
                
                config.loggers = [
                    {
                        type: 'kotlin-test-js-runner/tc-log-appender.js',
                        //default layout
                        layout: { type: 'pattern', pattern: '%[%d{DATETIME}:%p [%c]: %]%m' }
                    }
                ]
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

    fun usePhantomJS() = useBrowser("PhantomJS", versions.karmaPhantomjsLauncher)

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
            webpackMajorVersion.choose(
                versions.webpack,
                versions.webpack4
            )
        )
        requiredDependencies.add(
            webpackMajorVersion.choose(
                versions.webpackCli,
                versions.webpackCli3
            )
        )
        requiredDependencies.add(versions.formatUtil)
        requiredDependencies.add(
            webpackMajorVersion.choose(
                versions.sourceMapLoader,
                versions.sourceMapLoader1
            )
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
                    if (webpackMajorVersion != WebpackMajorVersion.V4) {
                        """
                            // https://github.com/webpack/webpack/issues/12951
                            const PatchSourceMapSource = require('kotlin-test-js-runner/webpack-5-debug');
                            config.plugins.push(new PatchSourceMapSource())
                            """
                    } else ""
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

    fun useCoverage(
        html: Boolean = true,
        lcov: Boolean = true,
        cobertura: Boolean = false,
        teamcity: Boolean = true,
        text: Boolean = false,
        textSummary: Boolean = false,
        json: Boolean = false,
        jsonSummary: Boolean = false
    ) {
        if (listOf(
                html, lcov, cobertura,
                teamcity, text, textSummary,
                json, jsonSummary
            ).all { !it }
        ) return

        requiredDependencies.add(versions.karmaCoverage)
        config.reporters.add("coverage")
        addPreprocessor("coverage") { !it.endsWith("_test.js") }

        configurators.add {
            val reportDir = project.reportsDir.resolve("coverage/${it.name}")
            reportDir.mkdirs()

            config.coverageReporter = CoverageReporter(reportDir.canonicalPath).also { coverage ->
                if (html) coverage.reporters.add(Reporter("html"))
                if (lcov) coverage.reporters.add(Reporter("lcovonly"))
                if (cobertura) coverage.reporters.add(Reporter("cobertura"))
                if (teamcity) coverage.reporters.add(Reporter("teamcity"))
                if (text) coverage.reporters.add(Reporter("text"))
                if (textSummary) coverage.reporters.add(Reporter("text-summary"))
                if (json) coverage.reporters.add(Reporter("json"))
                if (jsonSummary) coverage.reporters.add(Reporter("json-summary"))
            }
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
                if (predicate(it)) {
                    config.preprocessors.getOrPut(it) { mutableListOf() }.add(name)
                }
            }
        }
    }

    override fun createTestExecutionSpec(
        task: KotlinJsTest,
        forkOptions: ProcessForkOptions,
        nodeJsArgs: MutableList<String>,
        debug: Boolean
    ): TCServiceMessagesTestExecutionSpec {
        val file = task.inputFileProperty.get().asFile.toString()

        config.files.add(npmProject.require("kotlin-test-js-runner/kotlin-test-karma-runner.js"))
        if (!debug) {
            config.files.add(file)
        } else {
            config.singleRun = false

            config.files.add(createDebuggerJs(file).normalize().absolutePath)

            confJsWriters.add {
                //language=ES6
                it.appendLine(
                    """
                        if (!config.plugins) {
                            config.plugins = config.plugins || [];
                            config.plugins.push('karma-*'); // default
                        }
                        
                        config.plugins.push('kotlin-test-js-runner/karma-kotlin-debug-plugin.js');
                    """.trimIndent()
                )
            }

            config.frameworks.add("karma-kotlin-debug")
        }

        if (config.browsers.isEmpty()) {
            error("No browsers configured for $task")
        }

        val clientSettings = TCServiceMessagesClientSettings(
            task.name,
            testNameSuffix = task.targetName,
            prependSuiteName = true,
            stackTraceParser = ::parseNodeJsStackTraceAsJvm,
            ignoreOutOfRootNodes = true,
            escapeTCMessagesInLog = isTeamCity.isPresent
        )

        config.basePath = npmProject.nodeModulesDir.absolutePath

        configurators.forEach {
            it(task)
        }

        val cliArgs = KotlinTestRunnerCliArgs(
            include = task.includePatterns,
            exclude = task.excludePatterns
        )

        config.client.args.addAll(cliArgs.toList())

        val karmaConfJs = npmProject.dir.resolve("karma.conf.js")
        karmaConfJs.printWriter().use { confWriter ->
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

            confWriter.appendFromConfigDir()

            confWriter.println()
            confWriter.println("}")
        }

        val nodeModules = listOf("karma/bin/karma")

        val karmaConfigAbsolutePath = karmaConfJs.absolutePath
        val args = if (debug) {
            nodeJsArgs + listOf(
                npmProject.require("kotlin-test-js-runner/karma-debug-runner.js"),
                karmaConfigAbsolutePath
            )
        } else {
            nodeJsArgs +
                    nodeModules.map { npmProject.require(it) } +
                    listOf("start", karmaConfigAbsolutePath)
        }

        return object : JSServiceMessagesTestExecutionSpec(
            forkOptions,
            args,
            true,
            clientSettings
        ) {
            lateinit var progressLogger: ProgressLogger

            override fun wrapExecute(body: () -> Unit) {
                services().operation("Running and building tests with karma and webpack") {
                    progressLogger = this
                    body()
                }
            }

            override fun createClient(testResultProcessor: TestResultProcessor, log: Logger, testReporter: MppTestReportHelper) =
                object : JSServiceMessagesClient(
                    testResultProcessor,
                    clientSettings,
                    log,
                    testReporter,
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
                            if (actualType?.isErrorLike() == true) {
                                processFailedBrowsers(text)
                            }
                            message
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

                    override fun testFailedMessage(execHandle: ExecHandle, exitValue: Int): String {
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

    private fun createDebuggerJs(
        file: String,
    ): File {
        val adapterJs = npmProject.dir.resolve("debugger.js")
        adapterJs.printWriter().use { writer ->
            // It is necessary for debugger attaching (--inspect-brk analogue)
            writer.println("debugger;")

            writer.println("module.exports = require(${file.jsQuoted()})")
        }

        return adapterJs
    }

    private fun Appendable.appendFromConfigDir() {
        if (!configDirectory.isDirectory) {
            return
        }

        appendLine()
        appendConfigsFromDir(configDirectory)
        appendLine()
    }
}

private val KARMA_MESSAGE = "^.*\\d{2} \\d{2} \\d{4,} \\d{2}:\\d{2}:\\d{2}.\\d{3}:(ERROR|WARN|INFO|DEBUG|LOG) \\[.*]: ([\\w\\W]*)\$"
    .toRegex()
