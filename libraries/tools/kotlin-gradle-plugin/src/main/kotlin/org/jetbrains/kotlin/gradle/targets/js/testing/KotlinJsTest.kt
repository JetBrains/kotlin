/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing

import groovy.lang.Closure
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.process.internal.DefaultProcessForkOptions
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma
import org.jetbrains.kotlin.gradle.targets.js.testing.mocha.KotlinMocha
import org.jetbrains.kotlin.gradle.tasks.KotlinTest
import org.jetbrains.kotlin.gradle.utils.newFileProperty
import javax.inject.Inject

open class KotlinJsTest
@Inject
constructor(
    @Internal override var compilation: KotlinJsCompilation
) :
    KotlinTest(),
    RequiresNpmDependencies {
    private val nodeJs get() = NodeJsRootPlugin.apply(project.rootProject)

    private val projectPath = project.path

    @get:Internal
    var testFramework: KotlinJsTestFramework? = null
        set(value) {
            field = value
            onTestFrameworkCallbacks.forEach { callback ->
                callback(value)
            }
        }

    private var onTestFrameworkCallbacks: MutableList<(KotlinJsTestFramework?) -> Unit> =
        mutableListOf()

    fun onTestFrameworkSet(action: (KotlinJsTestFramework?) -> Unit) {
        onTestFrameworkCallbacks.add(action)
        testFramework?.let { testFramework: KotlinJsTestFramework ->
            onTestFrameworkCallbacks.forEach { callback ->
                callback(testFramework)
            }
        }
    }

    @Suppress("unused")
    val testFrameworkSettings: String
        @Input get() = testFramework!!.settingsState

    @InputFile
    val inputFileProperty: RegularFileProperty = project.newFileProperty()

    @Input
    var debug: Boolean = false

    @Suppress("unused")
    val runtimeClasspath: FileCollection
        @InputFiles get() = compilation.runtimeDependencyFiles

    @Suppress("unused")
    internal val compilationOutputs: FileCollection
        @InputFiles get() = compilation.output.allOutputs

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

    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency>
        @Internal get() = testFramework!!.requiredNpmDependencies

    @Deprecated("Use useMocha instead", ReplaceWith("useMocha()"))
    fun useNodeJs() = useMocha()

    @Deprecated("Use useMocha instead", ReplaceWith("useMocha(body)"))
    fun useNodeJs(body: KotlinMocha.() -> Unit) = useMocha(body)

    @Deprecated("Use useMocha instead", ReplaceWith("useMocha(fn)"))
    fun useNodeJs(fn: Closure<*>) {
        useMocha {
            ConfigureUtil.configure(fn, this)
        }
    }

    fun useMocha() = useMocha {}
    fun useMocha(body: KotlinMocha.() -> Unit) = use(KotlinMocha(compilation, path), body)
    fun useMocha(fn: Closure<*>) {
        useMocha {
            ConfigureUtil.configure(fn, this)
        }
    }

    fun useKarma() = useKarma {}
    fun useKarma(body: KotlinKarma.() -> Unit) = use(
        KotlinKarma(compilation, services, path),
        body
    )
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
        nodeJs.npmResolutionManager.checkRequiredDependencies(task = this, services = services, logger = logger, projectPath = projectPath)
        super.executeTests()
    }

    override fun createTestExecutionSpec(): TCServiceMessagesTestExecutionSpec {
        val forkOptions = DefaultProcessForkOptions(fileResolver)
        forkOptions.workingDir = compilation.npmProject.dir
        forkOptions.executable = nodeJs.requireConfigured().nodeExecutable

        val nodeJsArgs = mutableListOf<String>()

        return testFramework!!.createTestExecutionSpec(
            task = this,
            forkOptions = forkOptions,
            nodeJsArgs = nodeJsArgs,
            debug = debug
        )
    }
}