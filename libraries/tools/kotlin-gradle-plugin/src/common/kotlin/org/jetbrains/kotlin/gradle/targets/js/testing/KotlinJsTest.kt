/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing

import org.gradle.api.Action
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependencies
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma
import org.jetbrains.kotlin.gradle.targets.js.testing.mocha.KotlinMocha
import org.jetbrains.kotlin.gradle.tasks.KotlinTest
import org.jetbrains.kotlin.gradle.utils.domainObjectSet
import org.jetbrains.kotlin.gradle.utils.getExecOperations
import org.jetbrains.kotlin.gradle.utils.newFileProperty
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions.Companion.processLaunchOptions
import javax.inject.Inject

@DisableCachingByDefault
abstract class KotlinJsTest
@Inject
internal constructor(
    @Transient
    @Internal
    override var compilation: KotlinJsIrCompilation,
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
    execOps: ExecOperations,
) : KotlinTest(execOps),
    RequiresNpmDependencies {

    @Deprecated("Extending this class is deprecated. Scheduled for removal in Kotlin 2.4.")
    @Suppress("DEPRECATION")
    constructor(
        compilation: KotlinJsIrCompilation,
    ) : this(
        compilation = compilation,
        objects = compilation.target.project.objects,
        providers = compilation.target.project.providers,
        execOps = compilation.target.project.getExecOperations(),
    )

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


    @Deprecated("Use useMocha instead. Scheduled for removal in Kotlin 2.3.", ReplaceWith("useMocha()"), level = DeprecationLevel.ERROR)
    fun useNodeJs() = useMocha()

    @Deprecated("Use useMocha instead. Scheduled for removal in Kotlin 2.3.", ReplaceWith("useMocha(body)"), level = DeprecationLevel.ERROR)
    fun useNodeJs(body: KotlinMocha.() -> Unit) = useMocha(body)

    @Deprecated("Use useMocha instead. Scheduled for removal in Kotlin 2.3.", ReplaceWith("useMocha(fn)"), level = DeprecationLevel.ERROR)
    fun useNodeJs(fn: Action<KotlinMocha>) {
        useMocha {
            fn.execute(this)
        }
    }

    fun useMocha() = useMocha {}
    fun useMocha(body: KotlinMocha.() -> Unit) =
        if (compilation.wasmTarget == null) {
            use(KotlinMocha(compilation, path, objects, providers), body)
        } else {
            logger.warn("Mocha test framework for Wasm target is not supported. For KotlinWasmNode used")
            testFramework
        }

    fun useMocha(fn: Action<KotlinMocha>) {
        useMocha {
            fn.execute(this)
        }
    }

    fun useKarma() = useKarma {}
    fun useKarma(body: KotlinKarma.() -> Unit): KotlinKarma =
        use(
            KotlinKarma(compilation, path, objects, providers),
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
        val testFramework = runner.also(body)
        this.testFramework = testFramework

        return testFramework
    }

    override fun createTestExecutionSpec(): TCServiceMessagesTestExecutionSpec {
        val launchOpts = objects.processLaunchOptions {
            workingDir.set(this@KotlinJsTest.testFramework!!.workingDir)
            executable.set(this@KotlinJsTest.testFramework!!.executable)
            environment.putAll(this@KotlinJsTest.environment)
        }

        return testFramework!!.createTestExecutionSpec(
            task = this,
            launchOpts = launchOpts,
            nodeJsArgs = nodeJsArgs,
            debug = debug
        )
    }
}
