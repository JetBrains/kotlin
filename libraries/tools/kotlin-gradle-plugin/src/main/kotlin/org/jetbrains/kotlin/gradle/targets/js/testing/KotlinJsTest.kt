/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing

import groovy.lang.Closure
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.process.internal.DefaultProcessForkOptions
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.plugin.HasKotlinDependencies
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolver
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma
import org.jetbrains.kotlin.gradle.targets.js.testing.mocha.KotlinMocha
import org.jetbrains.kotlin.gradle.targets.js.testing.nodejs.KotlinNodeJsTestRunner
import org.jetbrains.kotlin.gradle.tasks.KotlinTest

open class KotlinJsTest : KotlinTest(), RequiresNpmDependencies {
    @Internal
    @SkipWhenEmpty
    internal var testFramework: KotlinJsTestFramework? = null

    val testFrameworkSettings: String
        @Input get() = testFramework!!.settingsState

    @Input
    var debug: Boolean = false

    @Input
    var nodeModulesToLoad: MutableList<String> = mutableListOf()

    override val requiredNpmDependencies: Collection<RequiredKotlinJsDependency>
        get() = testFramework!!.requiredNpmDependencies

    fun useNodeJs(body: KotlinNodeJsTestRunner.() -> Unit) = use(KotlinNodeJsTestRunner(project), body)
    fun useNodeJs(fn: Closure<*>) {
        useNodeJs {
            fn.delegate = this
            fn.call()
        }
    }

    fun useMocha(body: KotlinMocha.() -> Unit) = use(KotlinMocha(project), body)
    fun useMocha(fn: Closure<*>) {
        useMocha {
            fn.delegate = this
            fn.call()
        }
    }

    fun useKarma(body: KotlinKarma.() -> Unit) = use(KotlinKarma(project), body)
    fun useKarma(fn: Closure<*>) {
        useKarma {
            fn.delegate = this
            fn.call()
        }
    }

    private inline fun <T : KotlinJsTestFramework> use(runner: T, body: T.() -> Unit): T {
        check(testFramework == null) {
            "testFramework already configured for task ${this.path}"
        }

        val testFramework = runner.also(body)
        this.testFramework = testFramework

        return testFramework
    }

    override fun executeTests() {
        NpmResolver.resolve(project)
        NpmResolver.checkRequiredDependencies(project, this)
        super.executeTests()
    }

    override fun createTestExecutionSpec(): TCServiceMessagesTestExecutionSpec {
        val forkOptions = DefaultProcessForkOptions(fileResolver)

        NpmResolver.resolve(project)

        forkOptions.workingDir = NpmProject[project].nodeWorkDir
        forkOptions.executable = NodeJsPlugin.apply(project).root.environment.nodeExecutable

        val nodeJsArgs = mutableListOf<String>()

        if (debug) {
            nodeJsArgs.add("--inspect-brk")
        }

        return testFramework!!.createTestExecutionSpec(this, forkOptions, nodeJsArgs)
    }
}