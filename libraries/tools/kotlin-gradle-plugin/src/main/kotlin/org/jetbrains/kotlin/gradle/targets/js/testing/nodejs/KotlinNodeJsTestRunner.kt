/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.nodejs

import org.gradle.api.tasks.Input
import org.gradle.process.ProcessForkOptions
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.plugin.HasKotlinDependencies
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectLayout
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.testing.IgnoredTestSuites

class KotlinNodeJsTestRunner : KotlinJsTestFramework {
    @Input
    var ignoredTestSuites: IgnoredTestSuites = IgnoredTestSuites.showWithContents

    override fun configure(dependenciesHolder: HasKotlinDependencies) {
        dependenciesHolder.dependencies {
            runtimeOnly(kotlin("test-nodejs-runner"))
        }
    }

    override fun createTestExecutionSpec(
        task: KotlinJsTest,
        forkOptions: ProcessForkOptions,
        nodeJsArgs: MutableList<String>
    ): TCServiceMessagesTestExecutionSpec {
        val cliArgs = KotlinNodeJsTestRunnerCliArgs(
            task.nodeModulesToLoad.toList(),
            task.includePatterns,
            task.excludePatterns,
            ignoredTestSuites.cli
        )

        val clientSettings = TCServiceMessagesClientSettings(
            task.name,
            testNameSuffix = task.targetName,
            prepandSuiteName = true,
            stackTraceParser = ::parseNodeJsStackTraceAsJvm
        )

        val testRuntimeNodeModules = listOf(
            "kotlin-test-nodejs-runner.js",
            "kotlin-nodejs-source-map-support.js"
        )

        val npmProjectLayout = NpmProjectLayout[task.project]

        val args = nodeJsArgs +
                testRuntimeNodeModules.map {
                    npmProjectLayout.nodeModulesDir.resolve(it).also { file ->
                        check(file.isFile) {
                            "Cannot find ${file.canonicalPath}"
                        }
                    }.canonicalPath
                } +
                cliArgs.toList()

        return TCServiceMessagesTestExecutionSpec(
            forkOptions,
            args,
            true,
            clientSettings
        )
    }
}