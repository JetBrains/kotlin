/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.karma

import com.google.gson.GsonBuilder
import org.gradle.api.Project
import org.gradle.process.ProcessForkOptions
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.targets.js.appendConfigsFromDir
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.nodejs.nodeJs
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmPackageVersion
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.KarmaConfig.CoverageReporter.Reporter
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfigWriter
import org.jetbrains.kotlin.gradle.testing.internal.reportsDir
import java.io.File

class KotlinKarma(val project: Project) : KotlinJsTestFramework {
    private val config: KarmaConfig = KarmaConfig()
    private val requiredDependencies = mutableSetOf<NpmPackageVersion>()

    private val versions = project.nodeJs.versions
    private val configurators = mutableListOf<(KotlinJsTest) -> Unit>()
    private val confJsWriters = mutableListOf<(Appendable) -> Unit>()
    private var sourceMaps = false
    private var configDirectory: File? = project.projectDir.resolve("karma.config.d").takeIf { it.isDirectory }

    override val requiredNpmDependencies: Collection<RequiredKotlinJsDependency>
        get() = requiredDependencies.toList()

    init {
        requiredDependencies.add(versions.karma)

        useTeamcityReporter()

        config.singleRun = true
    }

    private fun useTeamcityReporter() {
        requiredDependencies.add(versions.karmaTeamcityReporter)
        config.reporters.add("teamcity")
    }

    fun useConfigDirectory(dir: File) {
        configDirectory = dir
    }

    fun useChrome() = useBrowser("Chrome", versions.karmaChromeLauncher)

    fun useChromeCanary() = useBrowser("ChromeCanary", versions.karmaChromeLauncher)

    fun useChromeHeadless() = useBrowser("ChromeHeadless", versions.karmaChromeLauncher)

    fun usePhantomJS() = useBrowser("PhantomJS", versions.karmaPhantomJsLauncher)

    fun useFirefox() = useBrowser("Firefox", versions.karmaFirefoxLauncher)

    fun useOpera() = useBrowser("Opera", versions.karmaOperaLauncher)

    fun useSafari() = useBrowser("Safari", versions.karmaSafariLauncher)

    fun useIe() = useBrowser("Ie", versions.karmaIeLauncher)

    private fun useBrowser(id: String, dependency: NpmPackageVersion) {
        config.browsers.add(id)
        requiredDependencies.add(dependency)
    }

    fun useMocha() {
        requiredDependencies.add(versions.karmaMocha)
        requiredDependencies.add(versions.mocha)
        config.frameworks.add("mocha")
    }

    fun useWebpack() {
        requiredDependencies.add(versions.karmaWebpack)

        addPreprocessor("webpack")
        confJsWriters.add {
            it.appendln()
            it.appendln("// webpack config")
            it.appendln("function createWebpackConfig() {")

            KotlinWebpackConfigWriter(
                sourceMaps = true,
                export = false
            ).appendTo(it)

            it.appendln("   return config;")
            it.appendln("}")
            it.appendln()
            it.appendln("config.set({webpack: createWebpackConfig()});")
            it.appendln()
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

            config.coverageReporter = KarmaConfig.CoverageReporter(reportDir.canonicalPath).also { coverage ->
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
        config.frameworks.add("source-map-support")
        requiredDependencies.add(versions.karmaSourceMapSupport)

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
        nodeJsArgs: MutableList<String>
    ): TCServiceMessagesTestExecutionSpec {
        val clientSettings = TCServiceMessagesClientSettings(
            task.name,
            testNameSuffix = task.targetName,
            prependSuiteName = true,
            stackTraceParser = ::parseNodeJsStackTraceAsJvm,
            ignoreOutOfRootNodes = true
        )

        val npmProject = task.project.npmProject

        val files = task.nodeModulesToLoad.map { npmProject.require(it) }
        config.files.addAll(files)

        config.basePath = npmProject.nodeModulesDir.absolutePath

        configurators.forEach {
            it(task)
        }

        val karmaConfJs = npmProject.nodeWorkDir.resolve("karma.conf.js")
        karmaConfJs.printWriter().use { confWriter ->
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
                nodeModules.map {
                    npmProject.nodeModulesDir.resolve(it).also { file ->
                        check(file.isFile) { "Cannot find ${file.canonicalPath}" }
                    }.canonicalPath
                } +
                listOf("start", karmaConfJs.absolutePath)

        return TCServiceMessagesTestExecutionSpec(
            forkOptions,
            args,
            false,
            clientSettings
        )
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
}