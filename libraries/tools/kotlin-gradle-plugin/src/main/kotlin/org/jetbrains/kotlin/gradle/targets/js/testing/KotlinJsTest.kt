/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing

import groovy.lang.Closure
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.gradle.process.internal.DefaultProcessForkOptions
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma
import org.jetbrains.kotlin.gradle.targets.js.testing.mocha.KotlinMocha
import org.jetbrains.kotlin.gradle.targets.js.testing.nodejs.KotlinNodeJsTestRunner
import org.jetbrains.kotlin.gradle.tasks.KotlinTest

open class KotlinJsTest : KotlinTest(), RequiresNpmDependencies {
    private val nodeJs get() = NodeJsRootPlugin.apply(project.rootProject)

    @get:Internal
    internal var testFramework: KotlinJsTestFramework? = null

    @Suppress("unused")
    val testFrameworkSettings: String
        @Input get() = testFramework!!.settingsState

    @Input
    var debug: Boolean = false

    @Internal
    override lateinit var compilation: KotlinJsCompilation

    @Suppress("unused")
    val runtimeClasspath: FileCollection
        @InputFiles get() = compilation.runtimeDependencyFiles

    @Suppress("unused")
    val compilationId: String
        @Input get() = compilation.let {
            val target = it.target
            target.project.path + "@" + target.name + ":" + it.compilationName
        }

    val nodeModulesToLoad: List<String>
        @Internal get() = listOf("./" + compilation.npmProject.main)

    override val nodeModulesRequired: Boolean
        @Internal get() = testFramework!!.nodeModulesRequired

    override val requiredNpmDependencies: Collection<RequiredKotlinJsDependency>
        @Internal get() = testFramework!!.requiredNpmDependencies

    fun useNodeJs() = useNodeJs {}
    fun useNodeJs(body: KotlinNodeJsTestRunner.() -> Unit) = use(KotlinNodeJsTestRunner(compilation), body)
    fun useNodeJs(fn: Closure<*>) {
        useNodeJs {
            ConfigureUtil.configure(fn, this)
        }
    }

    fun useMocha() = useMocha {}
    fun useMocha(body: KotlinMocha.() -> Unit) = use(KotlinMocha(compilation), body)
    fun useMocha(fn: Closure<*>) {
        useMocha {
            ConfigureUtil.configure(fn, this)
        }
    }

    fun useKarma() = useKarma {}
    fun useKarma(body: KotlinKarma.() -> Unit) = use(KotlinKarma(compilation), body)
    fun useKarma(fn: Closure<*>) {
        useKarma {
            ConfigureUtil.configure(fn, this)
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
        nodeJs.npmResolutionManager.checkRequiredDependencies(this)
        super.executeTests()
    }

    override fun createTestExecutionSpec(): TCServiceMessagesTestExecutionSpec {
        val forkOptions = DefaultProcessForkOptions(fileResolver)
        forkOptions.workingDir = compilation.npmProject.dir
        forkOptions.executable = nodeJs.environment.nodeExecutable

        val nodeJsArgs = mutableListOf<String>()

        if (debug) {
            nodeJsArgs.add("--inspect-brk")
        }

        return testFramework!!.createTestExecutionSpec(this, forkOptions, nodeJsArgs)
    }
}