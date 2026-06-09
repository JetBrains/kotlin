/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import java.time.Duration

@ExperimentalJsTestDsl
/**
 * Represents browser runner configuration.
 * This interface is extended by top-level [KotlinBrowserTestRunnerDsl]
 * as well as in browser-specific configurations (i.e. [KotlinJsBrowserTestDsl.ChromiumTestRunnerDsl])
 */
interface BrowserTestRunnerConfigDsl {
    /**
     * Input location pointing to a prepared JS bundle with tests and HTML page that can be opened in a browser.
     *
     * Normally configured from output of [KotlinJsBrowserTestDsl.bundleTask] ([WebpackBundleForKotlinJsTests.outputBundleDir])
     *
     * Change it to a custom bundle location to run tests from there.
     * The bundle location must be compatible with underlying browser test runners.
     *
     * Example of configuring a custom bundle location for a chromium test runner.
     * ```kotlin
     * // Step 1. take original task
     * val bundleTask = kotlin.js().browser.test.bundleTask
     *
     * // Step 2. create new task from output of the original bundling task
     * val postProcessingTask = tasks.register<Copy>("postProcessingForChromium") {
     *     from(bundleTask.outputBundleDir)
     *     into(layout.buildDirectory.dir("post-processed-bundle"))
     *
     *     doLast { doSomethingWithTheBundle() }
     * }
     *
     * // Step 3. configure test runner bundler directory from new task output
     * kotlin.js().browser.test.chromium().bundleDirectory(postProcessingTask.flatMap { it.destinationDirectory })
     * ```
     *
     * TODO: KT-86715 Add ability to load test.html via dev-server, so we need to allow configuring
     */
    val bundleDirectory: DirectoryProperty

    /**
     * Configure global timeout for how long tests allow to run.
     * On timeout browser will be closed, and the test run should finish with an error.
     *
     * Default is 2 minutes.
     */
    val timeout: Property<Duration>

    /**
     * Configure whether the browser should be launched in headless mode.
     *
     * Default is `true`
     */
    val headless: Property<Boolean>

    /**
     * Configure additional command line arguments to launch the browser.
     */
    val launchArgs: ListProperty<String>
}

@ExperimentalJsTestDsl
interface KotlinBrowserTestRunnerDsl : BrowserTestRunnerConfigDsl, Named

/**
 * DSL Interface to configure multiple browser test runners for Kotlin/JS.
 */
@ExperimentalJsTestDsl
interface KotlinJsBrowserTestDsl {
    /**
     * Represents a default configuration for all browser test runners.
     * Defaults can be overridden in the individual browser test runner configuration.
     */
    val browserDefaults: BrowserTestRunnerConfigDsl

    /**
     * Default bundle task that produces js bundle with the HTML file to run Kotlin tests in a browser.
     *
     * Use this task to post-process bundle output when needed.
     * The output of this task is set to [BrowserTestRunnerConfigDsl.bundleDirectory].
     * You may need to set a new bundleDirectory if you had applied any post-processing to the bundle.
     */
    val bundleTask: TaskProvider<out WebpackBundleForKotlinJsTests>

    /** Chromium-specific browser test runner config */
    interface ChromiumTestRunnerDsl : KotlinBrowserTestRunnerDsl

    /**
     * Enable & configure a Chromium browser test runner.
     *
     * Using different [name]s would configure different browser test runners.
     * Names must be unique among all runners.
     * Attempt to declare two browser runners of different types but with the same name will fail at configuration time.
     * Subsequent calls of [chromium] with the same name will configure the same runner.
     */
    fun chromium(name: String = "chromium", body: Action<ChromiumTestRunnerDsl>)

    /** @see [chromium] */
    fun chromium(name: String = "chromium") = chromium(name, Action { })

    /** Firefox-specific browser test runner config */
    interface FirefoxTestRunnerDsl : KotlinBrowserTestRunnerDsl

    /**
     * Enable & configure a Firefox browser test runner.
     *
     * Using different [name]s would configure different browser test runners.
     * Names must be unique among all runners.
     * Attempt to declare two browser runners of different types but with the same name will fail at configuration time.
     * Subsequent calls of [firefox] with the same name will configure the same runner.
     */
    fun firefox(name: String = "firefox", body: Action<FirefoxTestRunnerDsl>)

    /** @see [firefox] */
    fun firefox(name: String = "firefox") = firefox(name, Action { })

    /** Webkit-specific browser test runner config */
    interface WebkitTestRunnerDsl : KotlinBrowserTestRunnerDsl

    /**
     * Enable & configure a Webkit browser test runner.
     *
     * Using different [name]s would configure different browser test runners.
     * Names must be unique among all runners.
     * Attempt to declare two browser runners of different types but with the same name will fail at configuration time.
     * Subsequent calls of [webkit] with the same name will configure the same runner.
     */
    fun webkit(name: String = "webkit", body: Action<WebkitTestRunnerDsl>)

    /** @see [firefox] */
    fun webkit(name: String = "webkit") = webkit(name, Action { })

    /**
     * Returns configuration entries of all enabled browser test runners.
     *
     * Use this API to collect browser test runner configuration for reporting, extra processing, or configure additional tasks.
     */
    val allBrowserRunners: Provider<Map<String, KotlinBrowserTestRunnerDsl>>
}
