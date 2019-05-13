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
import org.jetbrains.kotlin.gradle.plugin.HasKotlinDependencies
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.nodejs.nodeJs
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmPackageVersion
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework

class KotlinKarma(val project: Project) : KotlinJsTestFramework {
    private val config: KarmaConfig = KarmaConfig()
    private val requiredDependencies = mutableSetOf<NpmPackageVersion>()

    private val versions = project.nodeJs.versions

    override val requiredNpmDependencies: Collection<RequiredKotlinJsDependency>
        get() = requiredDependencies.toList()

    init {
        requiredDependencies.add(versions.karma)

        requiredDependencies.add(versions.karmaTeamcityReporter)
        config.reporters.add("teamcity")

        config.singleRun = true
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
    }

    fun useWebpack() {
        requiredDependencies.add(versions.karmaWebpack)
    }

    fun useSourceMapSupport() {
        config.frameworks.add("source-map-support")
        requiredDependencies.add(versions.karmaSourceMapSupport)
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

        config.files.addAll(task.nodeModulesToLoad.map {
            npmProject.getModuleEntryPath(it)
        })

        config.basePath = npmProject.nodeModulesDir.absolutePath

        val karmaConfJs = npmProject.nodeWorkDir.resolve("karma.conf.js")
        karmaConfJs.printWriter().use {
            GsonBuilder().setPrettyPrinting().create().toJson(config, it)
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
}