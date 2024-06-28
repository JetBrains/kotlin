/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing

import org.gradle.api.Action
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.process.internal.DefaultProcessForkOptions
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma
import org.jetbrains.kotlin.gradle.targets.js.testing.mocha.KotlinMocha
import org.jetbrains.kotlin.gradle.tasks.KotlinTest
import org.jetbrains.kotlin.gradle.utils.domainObjectSet
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.getValue
import org.jetbrains.kotlin.gradle.utils.newFileProperty
import javax.inject.Inject

@DisableCachingByDefault
abstract class KotlinJsTest
@Inject
constructor(
    @Transient
    @Internal
    override var compilation: KotlinJsIrCompilation
) : KotlinTest(),
    RequiresNpmDependencies {
    @Transient
    private val nodeJs = project.kotlinNodeJsExtension

    private val nodeExecutable by project.provider { nodeJs.requireConfigured().executable }

    @Input
    var environment = mutableMapOf<String, String>()

    @get:Internal
    var testFramework: KotlinJsTestFramework? = null
        set(value) {
            field = value
            onTestFrameworkCallbacks.all { callback ->
                value?.let { callback.execute(it) }
            }
        }

    private var onTestFrameworkCallbacks = project.objects.domainObjectSet<Action<KotlinJsTestFramework>>()

    fun onTestFrameworkSet(action: Action<KotlinJsTestFramework>) {
        onTestFrameworkCallbacks.add(action)
    }

    @Suppress("unused")
    val testFrameworkSettings: String
        @Input get() = testFramework!!.settingsState

    @PathSensitive(PathSensitivity.ABSOLUTE)
    @InputFile
    @NormalizeLineEndings
    val inputFileProperty: RegularFileProperty = project.newFileProperty()

    @Input
    var debug: Boolean = false

    @Suppress("unused")
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:InputFiles
    val runtimeClasspath: FileCollection by lazy {
        compilation.runtimeDependencyFiles
    }

    @Suppress("unused")
    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    internal val compilationOutputs: FileCollection by lazy {
        compilation.output.allOutputs
    }

    @Suppress("unused")
    @get:Input
    val compilationId: String by lazy {
        compilation.let {
            val target = it.target
            target.project.path + "@" + target.name + ":" + it.compilationName
        }
    }

    @Input
    val nodeJsArgs: MutableList<String> =
        mutableListOf()

    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency>
        @Internal get() = testFramework!!.requiredNpmDependencies

    @Deprecated("Use useMocha instead", ReplaceWith("useMocha()"))
    fun useNodeJs() = useMocha()

    @Deprecated("Use useMocha instead", ReplaceWith("useMocha(body)"))
    fun useNodeJs(body: KotlinMocha.() -> Unit) = useMocha(body)

    @Deprecated("Use useMocha instead", ReplaceWith("useMocha(fn)"))
    fun useNodeJs(fn: Action<KotlinMocha>) {
        useMocha {
            fn.execute(this)
        }
    }

    fun useMocha() = useMocha {}
    fun useMocha(body: KotlinMocha.() -> Unit) = use(KotlinMocha(compilation, path), body)
    fun useMocha(fn: Action<KotlinMocha>) {
        useMocha {
            fn.execute(this)
        }
    }

    fun useKarma() = useKarma {}
    fun useKarma(body: KotlinKarma.() -> Unit) = use(
        KotlinKarma(compilation, { services }, path),
        body
    )

    fun useKarma(fn: Action<KotlinKarma>) {
        useKarma {
            fn.execute(this)
        }
    }

    fun environment(key: String, value: String) {
        this.environment[key] = value
    }

    private inline fun <T : KotlinJsTestFramework> use(runner: T, body: T.() -> Unit): T {
        check(testFramework == null) {
            "testFramework already configured for task ${this.path}"
        }

        val testFramework = runner.also(body)
        this.testFramework = testFramework

        return testFramework
    }

    override fun createTestExecutionSpec(): TCServiceMessagesTestExecutionSpec {
        val forkOptions = DefaultProcessForkOptions(fileResolver)
        forkOptions.workingDir = testFramework!!.workingDir.getFile()
        forkOptions.executable = nodeExecutable

        environment.forEach { (key, value) ->
            forkOptions.environment(key, value)
        }

        return testFramework!!.createTestExecutionSpec(
            task = this,
            forkOptions = forkOptions,
            nodeJsArgs = nodeJsArgs,
            debug = debug
        )
    }
}