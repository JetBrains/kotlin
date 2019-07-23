/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.mocha

import org.gradle.api.Project
import org.gradle.process.ProcessForkOptions
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.KotlinGradleNpmPackage
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework

class KotlinMocha(override val compilation: KotlinJsCompilation) : KotlinJsTestFramework {
    private val project: Project = compilation.target.project
    private val nodeJs = NodeJsRootPlugin.apply(project.rootProject)
    private val versions = nodeJs.versions

    override val settingsState: String
        get() = "mocha"

    override val requiredNpmDependencies: Collection<RequiredKotlinJsDependency>
        get() = listOf(
            KotlinGradleNpmPackage("test-nodejs-runner"),
            versions.mocha,
            versions.mochaTeamCityReporter
        )

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

        val nodeModules = listOf(
            ".bin/mocha",
            task.nodeModulesToLoad.single()
        )

        val npmProject = compilation.npmProject

        val args = nodeJsArgs +
                nodeModules.map {
                    npmProject.nodeModulesDir.resolve(it).also { file ->
                        check(file.isFile) { "Cannot find ${file.canonicalPath}" }
                    }.canonicalPath
                } +
                listOf(
                    "-r", "kotlin-nodejs-source-map-support.js",
                    "--reporter", "mocha-teamcity-reporter"
                )

        return TCServiceMessagesTestExecutionSpec(
            forkOptions,
            args,
            false,
            clientSettings
        )
    }
}