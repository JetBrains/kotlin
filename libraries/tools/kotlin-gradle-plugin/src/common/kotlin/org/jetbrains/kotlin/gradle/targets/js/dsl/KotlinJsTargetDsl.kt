/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetWithTests.Companion.DEFAULT_TEST_RUN_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.HasBinaries
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsPlatformTestRun
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsReportAggregatingTestRun
import org.jetbrains.kotlin.gradle.targets.js.ir.*
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.utils.withType

/**
 * Represents the Kotlin/JS target platform.
 *
 * Used for JS and Wasm compilation.
 *
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 */
interface KotlinJsSubTargetContainerDsl : KotlinTarget {

    /**
     * Returns the configuration options for Node.js execution environment
     * used for this [KotlinTarget].
     *
     * For more information about execution environments, see
     * https://kotl.in/kotlin-js-execution-environments
     * For more information about the Node.js execution environments, see
     * https://kotl.in/js-project-setup-node-js
     *
     * @see org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsNodeDsl
     */
    val nodejs: KotlinJsNodeDsl

    /**
     * Returns the configuration options for browser execution environment
     * used for this [KotlinTarget].
     *
     * For more information about execution environments, see
     * https://kotl.in/kotlin-js-execution-environments
     *
     * @see org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserDsl
     */
    val browser: KotlinJsBrowserDsl

    /**
     * Returns the configuration options for generic execution environments
     * used for this [KotlinTarget].
     *
     * For more information about execution environments, see
     * https://kotl.in/kotlin-js-execution-environments
     *
     * @see org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsGenericDsl
     */
    val generic: KotlinJsGenericDsl

    /**
     * Container for all execution environments enabled for this target.
     * Currently, the only supported environments are Node.js and browser.
     */
    @InternalKotlinGradlePluginApi
    val subTargets: NamedDomainObjectContainer<KotlinJsIrSubTargetWithBinary>

    /**
     * Internal property. It is not intended to be used by build script or plugin authors.
     *
     * Legacy method of detecting if a Node.js execution environment is enabled.
     */
    val isNodejsConfigured: Boolean
        get() = subTargets.withType<KotlinNodeJsIr>().isNotEmpty()

    /**
     * Internal property. It is not intended to be used by build script or plugin authors.
     *
     * Legacy method of detecting if a browser execution environment is enabled.
     */
    val isBrowserConfigured: Boolean
        get() = subTargets.withType<KotlinBrowserJsIr>().isNotEmpty()

    /**
     * Applies configuration to all Node.js execution environments used by this target.
     *
     * If Node.js is not enabled for this target, [body] will not be used.
     *
     * @see org.jetbrains.kotlin.gradle.targets.js.ir.KotlinNodeJsIr
     */
    // note: this is a legacy function from before KGP fully migrated to the Provider API.
    fun whenNodejsConfigured(body: KotlinJsNodeDsl.() -> Unit) {
        subTargets
            .withType<KotlinNodeJsIr>()
            .configureEach(body)
    }

    /**
     * Applies configuration to all browser execution environments used by this target.
     *
     * If browser is not enabled for this target, [body] will not be used.
     *
     * @see org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserDsl
     */
    // note: this is a legacy function from before KGP fully migrated to the Provider API.
    fun whenBrowserConfigured(body: KotlinJsBrowserDsl.() -> Unit) {
        subTargets
            .withType<KotlinBrowserJsIr>()
            .configureEach(body)
    }
}


/**
 * Base configuration options for the compilation of Kotlin JS and WasmJS targets.
 *
 * ```
 * kotlin {
 *     js { // Creates js target
 *         // Configure js target specifics here
 *     }
 *     wasmJs { // Creates WasmJS target
 *         // Configure WasmJS target specifics here
 *     }
 * }
 * ```
 *
 * To learn more see:
 * - [Set up a Kotlin/JS project](https://kotl.in/kotlin-js-setup).
 * - [Get started with Kotlin/Wasm and Compose Multiplatform](https://kotl.in/kotlin-wasm-js-setup).
 */
// NOTE: Consider splitting this so JS and WasmJS are configured separately KT-76473
interface KotlinJsTargetDsl :
    KotlinTarget,
    KotlinTargetWithNodeJsDsl,
    KotlinTargetWithGenericJsDsl,
    HasBinaries<KotlinJsBinaryContainer>,
    HasConfigurableKotlinCompilerOptions<KotlinJsCompilerOptions> {

    /**
     * Represents the name of the output module for a Kotlin/JS and Kotlin/Wasm target.
     * This property allows customization of output filenames
     */
    val outputModuleName: Property<String>

    /**
     * Enable 'browsers' as the execution environment for this target,
     * so the project can be used for client-side scripting in browsers.
     *
     * When enabled, Kotlin Gradle plugin will download and install
     * the required environment and dependencies for running and testing
     * in a browser.
     *
     * For more information, see https://kotl.in/kotlin-js-execution-environments
     *
     * @see KotlinJsBrowserDsl
     */
    fun browser() = browser { }

    /**
     * Enable 'browsers' as the execution environment for this target,
     * so the project can be used for client-side scripting in browsers.
     *
     * When enabled, Kotlin Gradle plugin will download and install
     * the required environment and dependencies for running and testing
     * in a browser.
     *
     * The target can be configured using [body].
     *
     * For more information, see https://kotl.in/kotlin-js-execution-environments
     *
     * @see KotlinJsBrowserDsl
     */
    fun browser(body: KotlinJsBrowserDsl.() -> Unit)

    /**
     * [Action] based version of [browser] above.
     */
    fun browser(fn: Action<KotlinJsBrowserDsl>) {
        browser {
            fn.execute(this)
        }
    }

    /**
     * _This option is only relevant for JS targets._
     * _Do not use in WasmJS targets._
     *
     * Use **CommonJS** as the JS module system used by the compiled JS code.
     *
     * This is a convenience method, to simplify configuring the Kotlin JS compiler options directly.
     *
     * Only one module system should be configured.
     * If no module system is configured it will default to
     * UMD (Universal Module Definition).
     * Do not configure two modules (e.g. [useEsModules]) in the same target, because the behaviour is undefined.
     */
    fun useCommonJs()

    /**
     * _This option is only relevant for JS targets._
     * _Do not use in WasmJS targets._
     *
     * Use **ES modules** as the JS module system used by the compiled JS code.
     *
     * This is a convenience method, to simplify configuring the Kotlin JS compiler options directly.
     *
     * Only one module system should be configured.
     * If no module system is configured it will default to
     * UMD (Universal Module Definition).
     * Do not configure two modules (e.g. [useCommonJs]) in the same target, because the behaviour is undefined.
     */
    fun useEsModules()

    /**
     * _This option is only relevant for JS targets._
     * _Do not use in WasmJS targets._
     *
     * > Note: Passing arguments to the main function is Experimental.
     * > It may be dropped or changed at any time.
     *
     * Specify a source of arguments for the `main()` function.
     *
     * [jsExpression] must be a JavaScript function that returns an array of Strings.
     * The array will be set as in the application's main argument, `args: Array<String>`, in place of main-function call.
     *
     * See https://kotl.in/kotlin-js-pass-arguments-to-main-function
     *
     * @see KotlinJsNodeDsl.passProcessArgvToMainFunction
     */
    @ExperimentalMainFunctionArgumentsDsl
    fun passAsArgumentToMainFunction(jsExpression: String)

    /**
     * > Note: Generating TypeScript declaration files is Experimental.
     * > It may be dropped or changed at any time.
     *
     * Enable generating TypeScript definitions from your Kotlin code.
     *
     * These definitions can be used by JavaScript tools and IDEs when working on hybrid apps to provide autocompletion,
     * support static analyzers, and make it easier to include Kotlin code in JavaScript and TypeScript projects.
     *
     * This is a convenience method, to simplify configuring the Kotlin JS compiler options directly.
     *
     * For more information about generating TypeScript definitions, see
     * https://kotl.in/kotlin-js-generate-typescript-defs
     */
    fun generateTypeScriptDefinitions()

    /**
     * The container that holds test run executions for the current target.
     *
     * A test run by the name [DEFAULT_TEST_RUN_NAME] is automatically created and configured.
     *
     * @see org.jetbrains.kotlin.gradle.plugin.KotlinTargetWithTests.testRuns
     */
    val testRuns: NamedDomainObjectContainer<KotlinJsReportAggregatingTestRun>

    // Need to compatibility when users use KotlinJsCompilation specific in build script
    override val compilations: NamedDomainObjectContainer<KotlinJsIrCompilation>

    override val binaries: KotlinJsBinaryContainer

    //region Deprecated Properties
    @Deprecated("Use outputModuleName with Provider API instead. Scheduled for removal in Kotlin 2.3.", level = DeprecationLevel.ERROR)
    var moduleName: String?

    @Deprecated(
        message = "produceExecutable() was changed on binaries.executable(). Scheduled for removal in Kotlin 2.3.",
        replaceWith = ReplaceWith("binaries.executable()"),
        level = DeprecationLevel.ERROR
    )
    fun produceExecutable() {
        throw GradleException("Please change produceExecutable() on binaries.executable()")
    }
    //endregion
}

/**
 * Base options for configuring Node.js for use in
 * Kotlin JS, WasmJS, and Wasi targets.
 *
 * To learn more see:
 * - [Set up a Kotlin/JS project](https://kotl.in/kotlin-js-setup).
 */
interface KotlinTargetWithNodeJsDsl {
    /**
     * Enables 'Node.js' as the execution environment for this target,
     * so the project can be used running JavaScript code outside a browser.
     *
     * When enabled, Kotlin Gradle plugin will download and install
     * the required environment and dependencies for running and testing
     * using Node.js.
     *
     * For more information, see https://kotl.in/kotlin-js-execution-environments
     *
     * @see KotlinJsNodeDsl
     */
    fun nodejs() = nodejs { }

    /**
     * Enables 'Node.js' as the execution environment for this target,
     * so the project can be used running JavaScript code outside a browser.
     *
     * When enabled, Kotlin Gradle plugin will download and install
     * the required environment and dependencies for running and testing
     * using Node.js.
     *
     * The target can be configured using [body].
     *
     * For more information, see https://kotl.in/kotlin-js-execution-environments
     *
     * @see KotlinJsNodeDsl
     */
    fun nodejs(body: KotlinJsNodeDsl.() -> Unit)

    /**
     * [Action] based version of [nodejs] above.
     */
    fun nodejs(fn: Action<KotlinJsNodeDsl>) {
        nodejs {
            fn.execute(this)
        }
    }
}

/**
 * Base options for configuring generic JS for use in
 * Kotlin JS targets.
 *
 * To learn more see:
 * - [Set up a Kotlin/JS project](https://kotl.in/kotlin-js-setup).
 */
interface KotlinTargetWithGenericJsDsl {
    /**
     * Enables 'generic' as the execution environment for this target,
     * so the project can be used running JavaScript code in generic environments.
     *
     * For more information, see https://kotl.in/kotlin-js-execution-environments
     *
     * @see KotlinJsGenericDsl
     */
    fun generic() = generic { }

    /**
     * Enables 'generic' as the execution environment for this target,
     * so the project can be used running JavaScript code in generic environments.
     *
     * The target can be configured using [body].
     *
     * For more information, see https://kotl.in/kotlin-js-execution-environments
     *
     * @see KotlinJsGenericDsl
     */
    fun generic(body: KotlinJsGenericDsl.() -> Unit)

    /**
     * [Action] based version of [generic] above.
     */
    fun generic(fn: Action<KotlinJsGenericDsl>) {
        generic {
            fn.execute(this)
        }
    }
}

/**
 * Common options for the configuring execution environments for Kotlin JS and Wasm targets.
 *
 * For more information about execution environments, see https://kotl.in/kotlin-js-execution-environments
 *
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 */
// note1: SubTarget == 'execution environment'
// note2: KGP only supports specific environments (Node.js, browser, D8).
//        See KT-73301 for supporting custom user-defined envs.
interface KotlinJsSubTargetDsl {

    /**
     * Configures the output of the bundle produced for Kotlin JS and Wasm targets.
     *
     * By default, the results of a Kotlin/JS project build reside in the
     * `build/dist/<targetName>/<binaryName>` directory within the project root.
     *
     * KGP will save the output bundle in the specified location.
     */
    @ExperimentalDistributionDsl
    fun distribution(body: Action<Distribution>)

    /**
     * Configures the default [KotlinJsTest] test task for the execution environment.
     *
     * This can be used to modify the configuration of the Kotlin JS test task.
     *
     * For more information about test tasks, see https://kotl.in/kotlin-js-test-configuration.
     */
    fun testTask(body: Action<KotlinJsTest>)

    /**
     * The container that holds all [KotlinJsPlatformTestRun] executions for the execution environments.
     *
     * This can be used to modify the configuration of the test runs.
     *
     * @see org.jetbrains.kotlin.gradle.plugin.KotlinTargetWithTests.testRuns
     * @see org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl.testRuns
     */
    val testRuns: NamedDomainObjectContainer<KotlinJsPlatformTestRun>
}

/**
 * Browser execution environment options for Kotlin JS and Wasm targets.
 *
 * For more information about execution environments, see https://kotl.in/kotlin-js-execution-environments
 *
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 */
interface KotlinJsBrowserDsl : KotlinJsSubTargetDsl {

    /**
     * Configures the default Webpack configuration for the browser execution environment.
     *
     * By default, Webpack is used by tasks used to [run][runTask],
     * [bundle][webpackTask], and [test][testTask] the Kotlin JS and Wasm targets.
     *
     * For more information about how Kotlin JS and Wasm use Webpack, see
     * https://kotl.in/js-project-setup/webpack-bundling
     *
     * @see org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
     */
    fun commonWebpackConfig(body: Action<KotlinWebpackConfig>)

    /**
     * Configures the default [KotlinWebpack] task that **runs** the Kotlin JS or Wasm target.
     *
     * This task will serve the compiled Kotlin JS or Wasm target webpack's local development server.
     * For more information about the run task, see
     * https://kotl.in/js-project-setup-run-task
     *
     * The common webpack configuration options for all [KotlinWebpack] tasks
     * can also be configured using [commonWebpackConfig].
     *
     * @see org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
     */
    fun runTask(body: Action<KotlinWebpack>)

    /**
     * Configures the default [KotlinWebpack] task that **bundles** the Kotlin JS or Wasm target.
     *
     * This task will bundle the compiled Kotlin JS or Wasm target.
     *
     * The common webpack configuration options for all [KotlinWebpack] tasks
     * can also be configured using [commonWebpackConfig].
     *
     * For more information about how Kotlin JS and Wasm use Webpack, see
     * https://kotl.in/js-project-setup/webpack-bundling
     *
     * @see org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
     */
    // https://kotlinlang.org/docs/js-project-setup.html#webpack-task
    fun webpackTask(body: Action<KotlinWebpack>)
}

/**
 * [Node.js](https://nodejs.org/) execution environment options for Kotlin JS and Wasm targets.
 *
 * For more information about execution environments, see
 * https://kotl.in/kotlin-js-execution-environments
 * For more information about the Node.js execution environments, see
 * https://kotl.in/js-project-setup-node-js
 *
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 */
interface KotlinJsNodeDsl : KotlinJsSubTargetDsl {

    /**
     * Configures the default [NodeJsExec] task that **runs** the Kotlin JS or Wasm target
     * using Node.js.
     *
     * For more information about the run task, see
     * https://kotl.in/js-project-setup-run-task
     *
     * @see org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
     */
    fun runTask(body: Action<NodeJsExec>)

    /**
     * _This option is only relevant for JS targets._
     * _Do not use in WasmJS targets._
     *
     * > Note: Passing arguments to the main function is Experimental.
     * > It may be dropped or changed at any time.
     *
     * Enable passing `process.argv` to the main function's `args` parameter.
     *
     * See https://kotl.in/kotlin-js-pass-arguments-to-main-function
     *
     * @see KotlinJsTargetDsl.passAsArgumentToMainFunction
     */
    @ExperimentalMainFunctionArgumentsDsl
    fun passProcessArgvToMainFunction()

    /**
     * _This option is only relevant for JS targets._
     * _Do not use in WasmJS targets._
     *
     * > Note: Passing arguments to the main function is Experimental.
     * > It may be dropped or changed at any time.
     *
     * Enable passing `process.argv.slice(2)` to the main function's `args` parameter.
     *
     * See https://kotl.in/kotlin-js-pass-arguments-to-main-function
     *
     * @see KotlinJsTargetDsl.passAsArgumentToMainFunction
     */
    @ExperimentalMainFunctionArgumentsDsl
    fun passCliArgumentsToMainFunction()
}

/**
 * Generic execution environment options for Kotlin JS targets.
 *
 * For more information about execution environments, see
 * https://kotl.in/kotlin-js-execution-environments
 *
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 */
interface KotlinJsGenericDsl : KotlinJsSubTargetDsl {

    /**
     * _This option is only relevant for JS targets._
     * _Do not use in WasmJS targets._
     *
     * > Note: Passing arguments to the main function is Experimental.
     * > It may be dropped or changed at any time.
     *
     * Enable passing `process.argv` to the main function's `args` parameter.
     *
     * See https://kotl.in/kotlin-js-pass-arguments-to-main-function
     *
     * @see KotlinJsTargetDsl.passAsArgumentToMainFunction
     */
    @ExperimentalMainFunctionArgumentsDsl
    fun passProcessArgvToMainFunction()

    /**
     * _This option is only relevant for JS targets._
     * _Do not use in WasmJS targets._
     *
     * > Note: Passing arguments to the main function is Experimental.
     * > It may be dropped or changed at any time.
     *
     * Enable passing `process.argv.slice(2)` to the main function's `args` parameter.
     *
     * See https://kotl.in/kotlin-js-pass-arguments-to-main-function
     *
     * @see KotlinJsTargetDsl.passAsArgumentToMainFunction
     */
    @ExperimentalMainFunctionArgumentsDsl
    fun passCliArgumentsToMainFunction()
}
