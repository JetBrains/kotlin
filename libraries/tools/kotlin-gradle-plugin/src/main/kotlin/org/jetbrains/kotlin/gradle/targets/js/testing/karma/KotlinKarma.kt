/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.karma

import com.google.gson.GsonBuilder
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.process.ProcessForkOptions
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.plugin.HasKotlinDependencies
import org.jetbrains.kotlin.gradle.targets.js.NpmPackageVersion
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.karmaChromeLauncher
import org.jetbrains.kotlin.gradle.targets.js.karmaPhantomjsLauncher
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework

class KotlinKarma(val project: Project) : KotlinJsTestFramework {
    private val config: KarmaConfig = KarmaConfig()
    private val requiredDependencies = mutableSetOf<NpmPackageVersion>()

    fun useChrome() = useBrowser("Chrome", karmaChromeLauncher)

    fun useChromeCanary() = useBrowser("ChromeCanary", karmaChromeLauncher)

    fun useChromeHeadless() = useBrowser("ChromeHeadless", karmaChromeLauncher)

    fun usePhantomJS() = useBrowser("PhantomJS", karmaPhantomjsLauncher)

    fun useFirefox() = useBrowser("Firefox", TODO())

    fun useBrowser(id: String, dependency: NpmPackageVersion) {
        config.browsers.add(id)
        requiredDependencies.add(dependency)
    }

    fun useMocha() {

    }

    fun useWebpack() {

    }

    override fun configure(dependenciesHolder: HasKotlinDependencies) {
        dependenciesHolder.dependencies {
            runtimeOnly(npm("karma", "4.0.1"))
            runtimeOnly(npm("karma-chrome-launcher", "2.2.0"))
            runtimeOnly(npm("puppeteer", "1.14.0"))
            runtimeOnly(npm("karma-source-map-support", "1.4.0"))
            runtimeOnly(npm("karma-browserify", "6.0.0"))
            runtimeOnly(npm("browserify", "16.2.3"))
            runtimeOnly(npm("karma-mocha", "1.3.0"))
            runtimeOnly(npm("mocha", "6.1.2"))
            runtimeOnly(npm("karma-teamcity-reporter", "1.1.0"))
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

        val npmProjectLayout = NpmProject[task.project]

        val files = task.nodeModulesToLoad.map {
            npmProjectLayout.nodeModulesDir.resolve(it).also { file ->
                check(file.isFile) { "Cannot find ${file.canonicalPath}" }
            }.canonicalPath
        }.let {
            GsonBuilder().create().toJson(it)
        }

        val karmaConfJs = npmProjectLayout.nodeWorkDir.resolve("karma.conf.js")
        karmaConfJs.printWriter().use {
            //language=JavaScript
            it.println(
                """
                process.env.CHROME_BIN = require('puppeteer').executablePath();
                
                module.exports = (config) => {
                  config.set({
                    basePath: 'node_modules',
                    files: $files,
                    frameworks: ['browserify', 'source-map-support', 'mocha'],
                    browsers: ['ChromeHeadless'],
                    reporters: ['teamcity'],
                    singleRun: true,
                    preprocessors: {
                      '*.js': [ 'browserify' ]
                    },
                    browserify: {
                      debug: true
                    }
                  });
                }
            """.trimIndent()
            )
        }

        val nodeModules = listOf("karma/bin/karma")

        val args = nodeJsArgs +
                nodeModules.map {
                    npmProjectLayout.nodeModulesDir.resolve(it).also { file ->
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
}