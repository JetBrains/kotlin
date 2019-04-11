/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.mocha

import org.gradle.process.ProcessForkOptions
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.plugin.HasKotlinDependencies
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectLayout
import org.jetbrains.kotlin.gradle.targets.js.tasks.KotlinNodeJsTestTask
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework

class KotlinMocha : KotlinJsTestFramework {
    override fun configure(dependenciesHolder: HasKotlinDependencies) {
        dependenciesHolder.dependencies {
            runtimeOnly(kotlin("test-nodejs-runner"))
            runtimeOnly(npm("mocha", "6.1.2"))
            runtimeOnly(npm("mocha-teamcity-reporter", ">=2.0.0"))
        }
    }

    override fun createTestExecutionSpec(
        task: KotlinNodeJsTestTask,
        forkOptions: ProcessForkOptions,
        nodeJsArgs: MutableList<String>
    ): TCServiceMessagesTestExecutionSpec {
        val clientSettings = TCServiceMessagesClientSettings(
            task.name,
            testNameSuffix = task.targetName,
            prepandSuiteName = true,
            stackTraceParser = ::parseNodeJsStackTraceAsJvm,
            ignoreOutOfRootNodes = true
        )

        val nodeModules = listOf(
            ".bin/mocha",
            task.nodeModulesToLoad.single()
        )

        val npmProjectLayout = NpmProjectLayout[task.project]

        val args = nodeJsArgs +
                nodeModules.map {
                    npmProjectLayout.nodeModulesDir.resolve(it).also { file ->
                        check(file.isFile) { "Cannot find ${file.canonicalPath}"}
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