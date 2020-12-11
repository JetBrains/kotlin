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
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutor.Companion.TC_PROJECT_PROPERTY
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.NpmPackageVersion
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.appendConfigsFromDir
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.jsQuoted
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.*
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.tasks.KotlinTest
import org.jetbrains.kotlin.gradle.testing.internal.reportsDir
import org.jetbrains.kotlin.gradle.utils.property
import org.slf4j.Logger
import java.io.File

class KotlinKarma(override val compilation: KotlinJsCompilation, private val services: ServiceRegistry) :
    KotlinJsTestFramework {
    private val project: Project = compilation.target.project
    private val nodeJs = NodeJsRootPlugin.apply(project.rootProject)
    private val versions = nodeJs.versions

    private val config: KarmaConfig = KarmaConfig()
    private val requiredDependencies = mutableSetOf<RequiredKotlinJsDependency>()

    private val configurators = mutableListOf<(KotlinTest) -> Unit>()
    private val envJsCollector = mutableMapOf<String, String>()
    private val confJsWriters = mutableListOf<(Appendable) -> Unit>()
    private var sourceMaps = false
    private var configDirectory: File by property {
        project.projectDir.resolve("karma.config.d")
    }

    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency>
        get() = requiredDependencies + webpackConfig.getRequiredDependencies(versions)

    override val settingsState: String
        get() = "KotlinKarma($config)"

    val webpackConfig = KotlinWebpackConfig(
        configDirectory = project.projectDir.resolve("webpack.config.d"),
        sourceMaps = true,
        devtool = null,
        export = false,
        progressReporter = true,
        progressReporterPathFilter = nodeJs.rootPackageDir.absolutePath
    )

    init {
        requiredDependencies.add(versions.kotlinJsTestRunner)
        requiredDependencies.add(versions.karma)

        useKotlinReporter()
        useMocha()
        useWebpack()
        useSourceMapSupport()

        // necessary for debug as a fallback when no debuggable browsers found
        addChromeLauncher()
    }

    private fun useKotlinReporter() {
        config.reporters.add("karma-kotlin-reporter")

        confJsWriters.add {
            // Not all log events goes through this appender
            // For example Error in config file
            //language=ES6
            it.appendln(
                """
                config.plugins = config.plugins || [];
                config.plugins.push('kotlin-test-js-runner/karma-kotlin-reporter.js');
                
                config.loggers = [
                    {
                        type: 'kotlin-test-js-runner/tc-log-appender.js',
                        //default layout
                        layout: { type: 'pattern', pattern: '%[%d{DATE}:%p [%c]: %]%m' }
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
        requiredDependencies.add(versions.karmaWebpack)
        requiredDependencies.add(versions.webpack)

        addPreprocessor("webpack")
        confJsWriters.add {
            it.appendln()
            it.appendln("// webpack config")
            it.appendln("function createWebpackConfig() {")

            webpackConfig.appendTo(it)
            //language=ES6
            it.appendln(
                """
                // noinspection JSUnnecessarySemicolon
                ;(function(config) {
                    const webpack = require('webpack');
                    config.plugins.push(new webpack.SourceMapDevToolPlugin({
                        moduleFilenameTemplate: "[absolute-resource-path]"
                    }))
                })(config);
            """.trimIndent()
            )

            it.appendln("   return config;")
            it.appendln("}")
            it.appendln()
            it.appendln("config.set({webpack: createWebpackConfig()});")
            it.appendln()
        }

        requiredDependencies.add(versions.webpack)
        requiredDependencies.add(versions.webpackCli)
        requiredDependencies.add(versions.sourceMapLoader)
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

    private fun createAdapterJs(
        file: String,
        debug: Boolean
    ): File {
        val npmProject = compilation.npmProject

        val adapterJs = npmProject.dir.resolve("adapter-browser.js")
        adapterJs.printWriter().use { writer ->
            val karmaRunner = npmProject.require("kotlin-test-js-runner/kotlin-test-karma-runner.js")
            // It is necessary for debugger attaching (--inspect-brk analogue)
            if (debug) {
                writer.println("debugger;")
            }

            writer.println("require(${karmaRunner.jsQuoted()})")

            writer.println("module.exports = require(${file.jsQuoted()})")
        }

        return adapterJs
    }

    override fun createTestExecutionSpec(
        task: KotlinJsTest,
        forkOptions: ProcessForkOptions,
        nodeJsArgs: MutableList<String>,
        debug: Boolean
    ): TCServiceMessagesTestExecutionSpec {
        val npmProject = compilation.npmProject

        val file = task.nodeModulesToLoad
            .map { npmProject.require(it) }
            .single()

        val adapterJs = createAdapterJs(file, debug)

        config.files.add(adapterJs.canonicalPath)

        if (debug) {
            config.singleRun = false

            confJsWriters.add {
                //language=ES6
                it.appendln(
                    """
                        if (!config.plugins) {
                            config.plugins = config.plugins || [];
                            config.plugins.push('karma-*'); // default
                        }
                        
                        config.plugins.push('kotlin-test-js-runner/karma-debug-framework.js');
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
            escapeTCMessagesInLog = project.hasProperty(TC_PROJECT_PROPERTY)
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
            listOf(
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
                services.operation("Running and building tests with karma and webpack") {
                    progressLogger = this
                    body()
                }
            }

            override fun createClient(testResultProcessor: TestResultProcessor, log: Logger) =
                object : JSServiceMessagesClient(
                    testResultProcessor,
                    clientSettings,
                    log
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
                            actualType = LogType.byValueOrNull(logLevel.toLowerCase())
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

    private fun Appendable.appendFromConfigDir() {
        if (!configDirectory.isDirectory) {
            return
        }

        appendln()
        appendConfigsFromDir(configDirectory)
        appendln()
    }
}

private val KARMA_MESSAGE = "^.*\\d{2} \\d{2} \\d{4,} \\d{2}:\\d{2}:\\d{2}.\\d{3}:(ERROR|WARN|INFO|DEBUG|LOG) \\[.*]: ([\\w\\W]*)\$"
    .toRegex()