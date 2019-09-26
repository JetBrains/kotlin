/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.karma

import com.google.gson.GsonBuilder
import jetbrains.buildServer.messages.serviceMessages.BaseTestSuiteMessage
import org.gradle.api.Project
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.process.ProcessForkOptions
import org.jetbrains.kotlin.gradle.internal.operation
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.NpmPackageVersion
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.appendConfigsFromDir
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.*
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Devtool
import org.jetbrains.kotlin.gradle.testing.internal.reportsDir
import org.slf4j.Logger
import java.io.File

class KotlinKarma(override val compilation: KotlinJsCompilation) : KotlinJsTestFramework {
    private val project: Project = compilation.target.project
    private val nodeJs = NodeJsRootPlugin.apply(project.rootProject)
    private val versions = nodeJs.versions

    private val config: KarmaConfig = KarmaConfig()
    private val requiredDependencies = mutableSetOf<NpmPackageVersion>()

    private val configurators = mutableListOf<(KotlinJsTest) -> Unit>()
    private val envJsCollector = mutableMapOf<String, String>()
    private val confJsWriters = mutableListOf<(Appendable) -> Unit>()
    private var sourceMaps = false
    private var configDirectory: File? = project.projectDir.resolve("karma.config.d").takeIf { it.isDirectory }

    override val requiredNpmDependencies: Collection<RequiredKotlinJsDependency>
        get() = requiredDependencies.toList()

    override val settingsState: String
        get() = "KotlinKarma($config)"

    init {
        requiredDependencies.add(versions.karma)

        useLogReporter()
        useMocha()
        useWebpack()
        useSourceMapSupport()

        config.singleRun = true
        config.autoWatch = false
    }

    // This reporter extends karma-teamcity-reporter
    //  It is necessary, because karma-teamcity-reporter can't write browser's log
    //  And additionally it overrides flushLogs, because flushLogs adds redundant spaces after some messages
    private fun useLogReporter() {
        requiredDependencies.add(versions.karmaTeamcityReporter)
        config.reporters.add("karma-browser-log-reporter")

        confJsWriters.add {
            //language=ES6
            it.appendln(
                """
                // reporter for browser logs
                (function () {
                    const util = require('util');
                    const resolve = require('url').resolve;

                    const escapeMessage = function (message) {
                        if (message === null || message === undefined) {
                            return ''
                        }

                        return message.toString()
                            .replace(/\|/g, '||')
                            .replace(/'/g, "|'")
                            .replace(/\n/g, '|n')
                            .replace(/\r/g, '|r')
                            .replace(/\u0085/g, '|x')
                            .replace(/\u2028/g, '|l')
                            .replace(/\u2029/g, '|p')
                            .replace(/\[/g, '|[')
                            .replace(/\]/g, '|]')
                    };
            
                    var formatMessage = function () {
                        var args = Array.prototype.slice.call(arguments);
            
                        for (var i = args.length - 1; i > 0; i--) {
                            args[i] = escapeMessage(args[i])
                        }
            
                        return util.format.apply(null, args) + '\n'
                    };
                    
                    function createFormatError(config, emitter) {
                    const basePath = config.basePath;
                    const urlRoot = config.urlRoot === '/' ? '' : (config.urlRoot || '');
                    let lastServedFiles = [];
                    
                    emitter.on('file_list_modified', (files) => {
                      lastServedFiles = files.served
                    });
                     const URL_REGEXP = new RegExp('(?:https?:\\/\\/' +
                        config.hostname + '(?:\\:' + config.port + ')?' + ')?\\/?' +
                        urlRoot + '\\/?' +
                        '(base/|absolute)' + // prefix, including slash for base/ to create relative paths.
                        '((?:[A-z]\\:)?[^\\?\\s\\:]*)' + // path
                        '(\\?\\w*)?' + // sha
                        '(\\:(\\d+))?' + // line
                        '(\\:(\\d+))?' + // column
                        '', 'g');
                        
                     const SourceMapConsumer = require('source-map').SourceMapConsumer;
                     
                     const cache = new WeakMap();
                     function getSourceMapConsumer (sourceMap) {
                         if (!cache.has(sourceMap)) {
                             cache.set(sourceMap, new SourceMapConsumer(sourceMap))
                         }
                         return cache.get(sourceMap)
                     }
                     
                     const PathUtils = require('karma/lib/utils/path-utils.js');
                    
                    return function (input) {
                        let msg = input.replace(URL_REGEXP, function (_, prefix, path, __, ___, line, ____, column) {
                            const normalizedPath = prefix === 'base/' ? `${"$"}basePath}/${"$"}{path}` : path;
                            const file = lastServedFiles.find((file) => file.path === normalizedPath);
                    
                            if (file && file.sourceMap && line) {
                                line = +line;
                                column = +column;
                                const bias = column ? SourceMapConsumer.GREATEST_LOWER_BOUND : SourceMapConsumer.LEAST_UPPER_BOUND;
                    
                                try {
                                    const original = getSourceMapConsumer(file.sourceMap).originalPositionFor({ line, column: (column || 0), bias });
                    
                                    return `${"$"}{PathUtils.formatPathMapping(resolve(path, original.source), original.line, original.column)} <- ${"$"}{PathUtils.formatPathMapping(path, line, column)}`
                                } catch (e) {
                                }
                            }
                    
                            return PathUtils.formatPathMapping(path, line, column) || prefix
                        });
                    
                        return msg + '\n'
                      };
                      }
            
                    const LogReporter = function (baseReporterDecorator, config, emitter) {
                        const teamcityReporter = require("karma-teamcity-reporter")["reporter:teamcity"][1];
                        teamcityReporter.call(this, baseReporterDecorator);
                        
                        const formatError = createFormatError(config, emitter);

                        this.TEST_STD_OUT = "##teamcity[testStdOut name='%s' out='%s' flowId='']";
                        
                        const tcOnBrowserStart = this.onBrowserStart;
                        this.onBrowserStart = function (browser) {
                            tcOnBrowserStart.call(this, browser);
                            this.browserResults[browser.id].consoleCollector = [];
                        };
            
                        this.onBrowserLog = (browser, log, type) => {
                            var browserResult = this.browserResults[browser.id];
                            if (browserResult) {
                                browserResult.consoleCollector.push(`[${"$"}{type}] ${"$"}{log}\n`)
                            }
                        };
            
                        const tcSpecSuccess = this.specSuccess;
                        this.specSuccess = function (browser, result) {
                            tcSpecSuccess.call(this, browser, result);

                            var log = this.getLog(browser, result);
                            var testName = result.description;
                        
                            const endMessage = log.pop();
                            this.browserResults[browser.id].consoleCollector.forEach(item => {
                              log.push(
                              formatMessage(this.TEST_STD_OUT, testName, item)
                              )
                           });
                           log.push(endMessage);
                        
                           this.browserResults[browser.id].consoleCollector = []
                        };
            
                        this.specFailure = function (browser, result) {
                            var log = this.getLog(browser, result);
                            var testName = result.description;
                
                            log.push(formatMessage(this.TEST_START, testName));
                            this.browserResults[browser.id].consoleCollector.forEach(item => {
                                log.push(
                                    formatMessage(this.TEST_STD_OUT, testName, item)
                                )
                            });
                
                            log.push(formatMessage(this.TEST_FAILED, testName, 
                                result.log
                                    .map(log => formatError(log))
                                    .join('\n\n')
                            ));
                            log.push(formatMessage(this.TEST_END, testName, result.time));
                
                            this.browserResults[browser.id].consoleCollector = []
                        };
                        
                        this.flushLogs = function (browserResult) {
                            while (browserResult.log.length > 0) {
                                var line = browserResult.log.shift();
                                line = line.replace("flowId=''", "flowId='" + browserResult.flowId + "'");
                            
                                this.write(line);
                            }
                        }
                    };
                    
                    LogReporter.${"$"}inject = ['baseReporterDecorator', 'config', 'emitter'];
            
                    config.plugins = config.plugins || [];
                    config.plugins.push('karma-*'); // default
                    config.plugins.push({
                        'reporter:karma-browser-log-reporter': ['type', LogReporter]
                    });
                })();
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

    fun useChrome() = useChromeWithPuppeteer(
        id = "Chrome",
        envVar = CHROME_BIN
    )

    fun useChromeCanary() = useChromeWithPuppeteer(
        id = "ChromeCanary",
        envVar = CHROME_CANARY_BIN
    )

    fun useChromeHeadless() = useChromeWithPuppeteer(
        id = "ChromeHeadless",
        envVar = CHROME_BIN
    )

    fun usePhantomJS() = useBrowser("PhantomJS", versions.karmaPhantomJsLauncher)

    fun useFirefox() = useBrowser("Firefox", versions.karmaFirefoxLauncher)

    fun useOpera() = useBrowser("Opera", versions.karmaOperaLauncher)

    fun useSafari() = useBrowser("Safari", versions.karmaSafariLauncher)

    fun useIe() = useBrowser("IE", versions.karmaIeLauncher)

    private fun useBrowser(id: String, dependency: NpmPackageVersion) {
        config.browsers.add(id)
        requiredDependencies.add(dependency)
    }

    private fun useChromeWithPuppeteer(
        id: String,
        envVar: String
    ) {
        usePuppeteer(envVar)
        useBrowser(id, versions.karmaChromeLauncher)
    }

    private fun usePuppeteer(envVar: String) {
        requiredDependencies.add(versions.puppeteer)

        //language=JavaScript 1.8
        envJsCollector[envVar] = "require('puppeteer').executablePath()"
    }

    private fun useMocha() {
        requiredDependencies.add(versions.karmaMocha)
        requiredDependencies.add(versions.mocha)
        config.frameworks.add("mocha")
    }

    private fun useWebpack() {
        createAdapterJs()
        requiredDependencies.add(versions.browserProcessHrtime)

        requiredDependencies.add(versions.karmaWebpack)
        requiredDependencies.add(versions.webpack)

        val webpackConfigWriter = KotlinWebpackConfig(
            configDirectory = project.projectDir.resolve("webpack.config.d").takeIf { it.isDirectory },
            sourceMaps = true,
            devtool = Devtool.INLINE_SOURCE_MAP,
            export = false,
            progressReporter = true,
            progressReporterPathFilter = nodeJs.rootPackageDir.absolutePath
        )
        requiredDependencies.addAll(webpackConfigWriter.getRequiredDependencies(versions))

        addPreprocessor("webpack")
        confJsWriters.add {
            it.appendln()
            it.appendln("// webpack config")
            it.appendln("function createWebpackConfig() {")

            webpackConfigWriter.appendTo(it)

            it.appendln("   return config;")
            it.appendln("}")
            it.appendln()
            it.appendln("config.set({webpack: createWebpackConfig()});")
            it.appendln()
        }

        requiredDependencies.add(versions.webpack)
        requiredDependencies.add(versions.webpackCli)
        requiredDependencies.add(versions.sourceMapLoader)
        requiredDependencies.add(versions.sourceMapSupport)
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
        requiredDependencies.add(versions.karmaSourceMapLoader)
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

    private fun createAdapterJs() {
        configurators.add {
            val npmProject = compilation.npmProject
            val files = it.nodeModulesToLoad.map { npmProject.require(it) }

            val adapterJs = npmProject.dir.resolve("adapter.js")
            adapterJs.printWriter().use { writer ->
                val karmaRunner = npmProject.require("kotlin-test-js-runner/kotlin-test-karma-runner.js")
                writer.println("require('$karmaRunner')")

                files.forEach { file ->
                    writer.println("require('$file')")
                }
            }

            config.files.add(adapterJs.canonicalPath)
        }
    }

    override fun createTestExecutionSpec(
        task: KotlinJsTest,
        forkOptions: ProcessForkOptions,
        nodeJsArgs: MutableList<String>
    ): TCServiceMessagesTestExecutionSpec {
        if (config.browsers.isEmpty()) {
            error("No browsers configured for $task")
        }

        val clientSettings = TCServiceMessagesClientSettings(
            task.name,
            testNameSuffix = task.targetName,
            prependSuiteName = true,
            stackTraceParser = ::parseNodeJsStackTraceAsJvm,
            ignoreOutOfRootNodes = true
        )

        val npmProject = compilation.npmProject

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
            confWriter.println("// environment variables")
            envJsCollector.forEach { (envVar, value) ->
                //language=JavaScript 1.8
                confWriter.println("process.env.$envVar = $value")
            }

            confWriter.println()
            confWriter.println("module.exports = function(config) {")
            confWriter.println()

            confWriter.print("config.set(")
            GsonBuilder().setPrettyPrinting().create().toJson(config, confWriter)
            confWriter.println(");")

            confJsWriters.forEach { it(confWriter) }

            confWriter.appendFromConfigDir()

            confWriter.println()
            confWriter.println("}")
        }

        val nodeModules = listOf("karma/bin/karma")

        val args = nodeJsArgs +
                nodeModules.map { npmProject.require(it) } +
                listOf("start", karmaConfJs.absolutePath)

        return object : JSServiceMessagesTestExecutionSpec(
            forkOptions,
            args,
            false,
            clientSettings
        ) {
            lateinit var progressLogger: ProgressLogger

            var isLaunchFailed: Boolean = false

            override fun wrapExecute(body: () -> Unit) {
                project.operation("Running and building tests with karma and webpack") {
                    progressLogger = this
                    body()

                    if (isLaunchFailed) {
                        showSuppressedOutput()
                        throw IllegalStateException("Launch of some browsers was failed")
                    }
                }
            }

            override fun createClient(testResultProcessor: TestResultProcessor, log: Logger) =
                object : JSServiceMessagesClient(
                    testResultProcessor,
                    clientSettings,
                    log,
                    suppressedOutput
                ) {
                    val baseTestNameSuffix get() = settings.testNameSuffix
                    override var testNameSuffix: String? = baseTestNameSuffix

                    override fun printNonTestOutput(text: String) {
                        val value = text.trimEnd()
                        progressLogger.progress(value)

                        parseConsole(value)
                    }

                    override fun processStackTrace(stackTrace: String): String {
                        return stackTrace.lines()
                            .joinToString("\n") { line ->
                                val index = line.indexOf(KARMA_SOURCE_MAP_DELIMETER)
                                if (index == -1)
                                    line
                                else
                                    line
                                        .removeRange(index, line.length - 1)
                                        .replace(WEBPACK_PROTOCOL, "")
                            }

                    }

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

                    private fun parseConsole(text: String) {
                        if (KARMA_PROBLEM.matches(text)) {
                            log.error(text)
                            isLaunchFailed = true
                            return
                        }

                        super.printNonTestOutput(text)
                    }
                }
        }
    }

    private fun Appendable.appendFromConfigDir() {
        val configDirectory = configDirectory ?: return

        check(configDirectory.isDirectory) {
            "\"$configDirectory\" is not a directory"
        }

        appendln()
        appendConfigsFromDir(configDirectory)
        appendln()
    }

    companion object {
        const val CHROME_BIN = "CHROME_BIN"
        const val CHROME_CANARY_BIN = "CHROME_CANARY_BIN"

        const val WEBPACK_PROTOCOL = "webpack://"
        const val KARMA_SOURCE_MAP_DELIMETER = " <-"

        val KARMA_PROBLEM = "(?m)^.*\\d{2} \\d{2} \\d{4,} \\d{2}:\\d{2}:\\d{2}.\\d{3}:(ERROR|WARN) \\[.*]: (.*)\$".toRegex()
    }
}