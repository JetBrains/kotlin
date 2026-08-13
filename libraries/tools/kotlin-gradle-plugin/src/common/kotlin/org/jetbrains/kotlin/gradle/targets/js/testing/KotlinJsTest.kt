/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.internal.tasks.testing.TestExecutionSpec
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependenciesTask
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma
import org.jetbrains.kotlin.gradle.targets.js.testing.mocha.KotlinMocha
import org.jetbrains.kotlin.gradle.tasks.KotlinTest
import org.jetbrains.kotlin.gradle.utils.domainObjectSet
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
    providers: ProviderFactory,
    execOps: ExecOperations,
) : KotlinTest(execOps),
    RequiresNpmDependenciesTask,
    UsesKotlinToolingDiagnostics {

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

    private var onTestFrameworkCallbacks = objects.domainObjectSet<Action<KotlinJsTestFramework>>()

    fun onTestFrameworkSet(action: Action<KotlinJsTestFramework>) {
        onTestFrameworkCallbacks.add(action)
    }

    @Suppress("unused")
    val testFrameworkSettings: String
        @Input get() = testFramework!!.settingsState

    @get:Nested
    @get:Optional
    @Suppress("unused")
    internal val testFrameworkInputs: Any?
        get() = testFramework?.frameworkTaskInputs

    @PathSensitive(PathSensitivity.ABSOLUTE)
    @InputFile
    @NormalizeLineEndings
    val inputFileProperty: RegularFileProperty = project.newFileProperty()

    @Input
    var debug: Boolean = false

    // Frameworks these options can't drive report a diagnostic instead. Karma is set up by the IDE init script.
    @get:Input
    @get:Option(
        option = "browser-debug",
        description = "Runs the browser tests of this task in debug mode.",
    )
    internal val browserDebug: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(false)

    // The options below only refine a debug run: --browser-debug is what turns one on. The IDE passes it on the
    // task it debugs and sends the ports it picked through the environment, which reaches every task of the build.
    @get:Input
    @get:Option(
        option = "browser-debug-port",
        description = "The port a CDP-compatible debugger can attach to. Defaults to $DEFAULT_DEBUG_PORT. " +
                "Only used with --browser-debug.",
    )
    internal val browserDebugPort: Property<String> = objects.property(String::class.java)
        .convention(providers.environmentVariable(BROWSER_DEBUG_PORT_ENV).orElse(DEFAULT_DEBUG_PORT.toString()))

    // Unset means the run doesn't wait: the port is one the debugger listens on, so there is nothing to default to.
    @get:Input
    @get:Optional
    @get:Option(
        option = "browser-debug-ready-port",
        description = "The loopback port to connect to before running the tests, to wait for a debugger to attach. " +
                "Only used with --browser-debug.",
    )
    internal val browserDebugReadyPort: Property<String> = objects.property(String::class.java)
        .convention(providers.environmentVariable(BROWSER_DEBUG_READY_PORT_ENV))

    @get:Input
    @get:Option(
        option = "browser-debug-ready-timeout",
        description = "How long to wait for a debugger to attach, in milliseconds. " +
                "Defaults to $DEFAULT_DEBUGGER_READY_TIMEOUT_MILLIS. Only used with --browser-debug.",
    )
    internal val browserDebugReadyTimeout: Property<String> = objects.property(String::class.java)
        .convention(
            providers.environmentVariable(BROWSER_DEBUG_READY_TIMEOUT_ENV)
                .orElse(DEFAULT_DEBUGGER_READY_TIMEOUT_MILLIS.toString())
        )

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
            use(KotlinMocha(compilation, path), body)
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
            KotlinKarma(compilation, path, objects),
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

    override fun createTestExecutionSpec(): TestExecutionSpec {
        configureBrowserDebug()

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

    override fun createTestExecuter(): TestExecuter<*> {
        val testExecuter = testFramework!!.createTestExecuter() ?: super.createTestExecuter()
        return testExecuter
    }

    internal fun configureBrowserDebug() {
        if (!browserDebug.get()) return

        val debuggableFramework = testFramework as? KotlinJsBrowserDebuggableFramework
        if (debuggableFramework == null) {
            // Karma is debugged through the IDE init script, which turns `debug` on before this runs.
            if (!debug) reportDiagnostic(KotlinToolingDiagnostics.BrowserDebugOptionsNotSupportedByTestFramework(path))
            return
        }

        debug = true
        debuggableFramework.kotlinJsBrowserDebugOptions.set(
            objects.newInstance(KotlinJsBrowserDebugOptions::class.java).apply {
                debugPort.set(browserDebugPort.map { parsePort(it, "--browser-debug-port") })
                debuggerReadyPort.set(browserDebugReadyPort.map { parsePort(it, "--browser-debug-ready-port") })
                debuggerReadyTimeoutMillis.set(browserDebugReadyTimeout.map { parseTimeoutMillis(it) })
            }
        )
    }

    private fun parsePort(value: String, option: String): Int =
        value.toIntOrNull()?.takeIf { it in 1..65535 }
            ?: throw GradleException("$option must be an integer between 1 and 65535, but was '$value'")

    private fun parseTimeoutMillis(value: String): Int =
        value.toIntOrNull()?.takeIf { it > 0 }
            ?: throw GradleException(
                "--browser-debug-ready-timeout must be a positive number of milliseconds, but was '$value'"
            )
}

// Set by the IDE with the ports it picked. The same names are in the IntelliJ plugin.
internal const val BROWSER_DEBUG_PORT_ENV = "KOTLIN_BROWSER_DEBUG_PORT"
internal const val BROWSER_DEBUG_READY_PORT_ENV = "KOTLIN_BROWSER_DEBUG_READY_PORT"
internal const val BROWSER_DEBUG_READY_TIMEOUT_ENV = "KOTLIN_BROWSER_DEBUG_READY_TIMEOUT"
