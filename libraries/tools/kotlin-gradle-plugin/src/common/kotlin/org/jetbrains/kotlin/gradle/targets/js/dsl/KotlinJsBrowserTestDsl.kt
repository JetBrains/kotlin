/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.DelicateKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import org.jetbrains.kotlin.gradle.DelicateKotlinGradlePluginApiKind
import java.net.URI
import kotlin.time.Duration

/**
 * Represents common configuration that is applicable to all Kotlin Browser Test Runners.
 *
 * This configuration DSL is available as part of [top-level block][KotlinJsBrowserTestDsl].
 * And can be overridden by a concrete browser [runner][KotlinBrowserTestRunnerDsl]
 * Sample:
 *
 *     kotlin {
 *       js {
 *         browser {
 *           test {
 *             // override default timeout on top-level
 *             timeout = 20.seconds
 *             chromium {
 *                 // override default timeout to 25 seconds
 *                 // specifically for this test runner
 *                 timeout = 25.seconds
 *             }
 *             firefox {
 *                // inherits timeout of 20 seconds from top-level block
 *             }
 *           }
 *         }
 *       }
 *     }
 */
@ExperimentalJsTestDsl
interface BrowserTestRunnerTopLevelConfigDsl {
    /**
     * Input location pointing to a prepared JS bundle with tests and HTML page that can be opened in a browser.
     *
     * Normally configured to use [KotlinJsBrowserTestDsl.defaultTestsLocationProvider]
     *
     * Change it to a custom location to run tests from there.
     * The bundle location must be compatible with underlying browser test runners.
     */
    @DelicateKotlinGradlePluginApi(kind = DelicateKotlinGradlePluginApiKind.REPLACES_DEFAULTS)
    val testsLocation: Property<KotlinJsTestsLocation>

    /**
     * Configure global timeout for how long tests allow to run.
     * On timeout browser will be closed, and the test run should finish with an error.
     *
     * Default is 30 seconds.
     */
    val timeout: Property<Duration>

    /**
     * Configure whether the browser should be launched in headless mode.
     *
     * Default is `true`
     */
    val headless: Property<Boolean>

    /**
     * Configure environment variables that will be passed to the browser instance.
     */
    val launchEnvironmentVariables: MapProperty<String, String>
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
     * Location of a prepared JS tests bundle on local filesystem.
     */
    val bundleLocation: Provider<Directory>

    /**
     * Name of the HTML file inside [bundleLocation], that runs Kotlin JS Tests when opened in browser.
     */
    val testHtmlFileName: Provider<String>

    /**
     * Access prepared JS tests via http web server.
     * Already points to [testHtmlFileName].
     *
     * *Delicate API*:
     * By default, URL points to the internal oversimplified web server launched by KGP as Shared Gradle Build Service.
     * This web server is suitable for serving static files only for tests execution in browsers.
     * The port is random and the host is localhost.
     * It is not recommended to use this property outside the Test task.
     * This URL is also used to attach the IDE debugger.
     */
    @DelicateKotlinGradlePluginApi(kind = DelicateKotlinGradlePluginApiKind.REQUIRES_KNOWLEDGE)
    val url: Provider<URI>
}

/**
 * Represents browser runner (e.g. [browser.test.chromium][KotlinJsBrowserTestDsl.ChromiumTestRunnerDsl]) DSL block.
 * Interface shared between all browser runners, and its members are not available at [top-level][BrowserTestRunnerTopLevelConfigDsl].
 *
 * Sample:
 *
 *     kotlin {
 *       js {
 *         browser {
 *           test {
 *             // no launchArgs at top-level
 *             chromium {
 *                launchArgs = listOf("--no-sandbox")
 *             }
 *             firefox {
 *                launchArgs = listOf("-devtools")
 *             }
 *           }
 *         }
 *       }
 *     }
 */
@ExperimentalJsTestDsl
interface KotlinBrowserTestRunnerDsl : BrowserTestRunnerTopLevelConfigDsl, Named {
    /**
     * Configure additional command line arguments to launch the browser.
     */
    val launchArgs: ListProperty<String>

    /**
     * Set to configure a custom path to the browser executable.
     *
     * Should not be used to get the default browser executable path.
     * Default is empty, meaning that the toolchain's default browser will be used.
     */
    @DelicateKotlinGradlePluginApi(kind = DelicateKotlinGradlePluginApiKind.REPLACES_DEFAULTS)
    val customBrowserExecutable: RegularFileProperty
}

/**
 * DSL Interface to configure multiple browser test runners for Kotlin/JS.
 */
@ExperimentalJsTestDsl
interface KotlinJsBrowserTestDsl : BrowserTestRunnerTopLevelConfigDsl {
    /**
     * Default location of bundled and ready to execute JS tests produced from Kotlin JS test compilation.
     * Note that this is read-only [Provider], that Kotlin Gradle Plugin offers as default.
     * To use different test location for tests use [KotlinBrowserTestRunnerDsl.testsLocation].
     *
     * The intended use case when this provider should be consumed by user code is to post-process default test bundle
     * or replace some parts of it in a different task and producing new instance of [KotlinJsTestsLocation].
     * The new location instance should be later set via [KotlinBrowserTestRunnerDsl.testsLocation].
     */
    val defaultTestsLocationProvider: Provider<out KotlinJsTestsLocation>

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
