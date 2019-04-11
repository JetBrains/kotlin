/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.process.internal.DefaultProcessForkOptions
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.plugin.HasKotlinDependencies
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectLayout
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolver
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework
import org.jetbrains.kotlin.gradle.targets.js.testing.nodejs.KotlinNodeJsTestRunner
import org.jetbrains.kotlin.gradle.targets.js.testing.mocha.KotlinMocha
import org.jetbrains.kotlin.gradle.tasks.KotlinTestTask

open class KotlinNodeJsTestTask : KotlinTestTask() {
    @Internal
    private var testFramework: KotlinJsTestFramework = KotlinNodeJsTestRunner()

    @Input
    var debug: Boolean = false

    @Internal
    var runtimeDependencyHandler: HasKotlinDependencies? = null

    @Input
    var nodeModulesToLoad: MutableList<String> = mutableListOf()

    fun useNodeJs(body: KotlinNodeJsTestRunner.() -> Unit) = use(KotlinNodeJsTestRunner(), body)

    fun useMocha(body: KotlinMocha.() -> Unit) = use(KotlinMocha(), body)

    private inline fun <T : KotlinJsTestFramework> use(runner: T, body: T.() -> Unit) {
        testFramework = runner.also(body)

        val dependenciesHolder = runtimeDependencyHandler
        if (dependenciesHolder != null) {
            testFramework.configure(dependenciesHolder)
        }
    }

    override fun executeTests() {
        NpmResolver.resolve(project)
        super.executeTests()
    }

    override fun createTestExecutionSpec(): TCServiceMessagesTestExecutionSpec {
        val forkOptions = DefaultProcessForkOptions(fileResolver)

        NpmResolver.resolve(project)

        forkOptions.workingDir = NpmProjectLayout[project].nodeWorkDir
        forkOptions.executable = NodeJsPlugin[project].buildEnv().nodeExecutable

        val nodeJsArgs = mutableListOf<String>()

        if (debug) {
            nodeJsArgs.add("--inspect-brk")
        }

        return testFramework.createTestExecutionSpec(this, forkOptions, nodeJsArgs)
    }
}