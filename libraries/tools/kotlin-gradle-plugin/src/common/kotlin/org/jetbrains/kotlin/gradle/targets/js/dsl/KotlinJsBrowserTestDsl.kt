/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.file.Directory
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
     * Normally configured to use [KotlinJsBrowserTestDsl.defaultTestsLocation]
     *
     * Change it to a custom location to run tests from there.
     * The bundle location must be compatible with underlying browser test runners.
     */
    val testsLocation: Property<KotlinJsTestsLocation>

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

/**
 * Provides access to a prepared bundle of Kotlin JS tests.
 *
 * To maintain Gradle tasks' up-to-date correctness,
 * implementations of [KotlinJsTestsLocation] must support [org.gradle.api.tasks.Nested] annotation.
 */
@ExperimentalJsTestDsl
interface KotlinJsTestsLocation {
    /**
     * Access prepared JS tests via dev web server.
     */
    val devServer: Provider<out KotlinJsTestDevServerService>

    /**
     * Location of a prepared JS tests bundle on local filesystem.
     */
    val bundleLocation: Provider<Directory>
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
    fun browserDefaults(configure: Action<BrowserTestRunnerConfigDsl>): BrowserTestRunnerConfigDsl

    /**
     * Default bundle task that produces js bundle with the HTML file to run Kotlin tests in a browser.
     *
     * Use this task to post-process bundle output when needed.
     */
    val defaultBundleTask: TaskProvider<out BundleKotlinJsTestsTask>

    /**
     * Default location of bundled and ready to execute JS tests produced from Kotlin JS test compilation.
     * The output of this task is set to [BrowserTestRunnerConfigDsl.testsLocation].
     */
    val defaultTestsLocation: Provider<out KotlinJsTestsLocation>

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

    /** @see [webkit] */
    fun webkit(name: String = "webkit") = webkit(name, Action { })

    /**
     * Returns configuration entries of all enabled browser test runners.
     *
     * Use this API to collect browser test runner configuration for reporting, extra processing, or configure additional tasks.
     */
    val allBrowserRunners: Provider<Map<String, KotlinBrowserTestRunnerDsl>>
}
