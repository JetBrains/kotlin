/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.karma

import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi

@ExternalKotlinTargetApi
/**
 * Serializable view of Karma config.
 *
 * The values are configured in
 * [org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma],
 * and serialised to JSON.
 *
 * The default version of Karma is set in [org.jetbrains.kotlin.gradle.targets.js.NpmVersions.karma].
 *
 * **Note:** This class is not intended for use by build script or plugin authors.
 *
 * @see org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma
 */
// https://karma-runner.github.io/6.4/config/configuration-file.html
data class KarmaConfig(
    /**
     * Continuous Integration mode
     *
     * See https://karma-runner.github.io/6.4/config/configuration-file.html#singlerun
     */
    var singleRun: Boolean = true,

    /**
     * Enable or disable watching files and executing the tests whenever one of these files changes.
     *
     * See https://karma-runner.github.io/6.4/config/configuration-file.html#autowatch
     */
    var autoWatch: Boolean = false,

    /**
     * The root path location that will be used to resolve all relative
     * paths defined in [files] and [exclude].
     *
     * If the value is a relative path it will be resolved to the `__dirname` of the configuration file.
     *
     * See https://karma-runner.github.io/6.4/config/configuration-file.html#basepath
     */
    var basePath: String? = null,

    /**
     * See https://karma-runner.github.io/6.4/config/configuration-file.html#files
     */
    val files: MutableList<Any> = mutableListOf(),

    /**
     * See https://karma-runner.github.io/6.4/config/configuration-file.html#frameworks
     */
    val frameworks: MutableList<String> = mutableListOf(),

    /**
     * Options which are accessible on browser side.
     *
     * When Karma launches the client (the browser being used for testing),
     * the options configured here will be available at runtime.
     */
    val client: KarmaClient = KarmaClient(),

    /**
     * See https://karma-runner.github.io/6.4/config/configuration-file.html#browsers
     */
    val browsers: MutableList<String> = mutableListOf(),

    /**
     * See https://karma-runner.github.io/6.4/config/browsers.html#configured-launchers
     *
     * Karma does not have detailed documentation on the available configuration options
     * for custom launchers.
     */
    val customLaunchers: MutableMap<String, CustomLauncher> = mutableMapOf(),

    /**
     * See https://karma-runner.github.io/6.4/config/configuration-file.html#customcontextfile
     */
    var customContextFile: String? = null,

    /**
     * See https://karma-runner.github.io/6.4/config/configuration-file.html#customdebugfile
     */
    var customDebugFile: String? = null,

    /**
     * See https://karma-runner.github.io/6.4/config/configuration-file.html#failonfailingtestsuite
     */
    val failOnFailingTestSuite: Boolean = false,

    /**
     * See https://karma-runner.github.io/6.4/config/configuration-file.html#failonemptytestsuite
     */
    val failOnEmptyTestSuite: Boolean = false,

    /**
     * See https://karma-runner.github.io/6.4/config/configuration-file.html#reporters
     */
    val reporters: MutableList<String> = mutableListOf(),

    /**
     * See https://karma-runner.github.io/6.4/config/configuration-file.html#preprocessors
     */
    val preprocessors: MutableMap<String, MutableList<String>> = mutableMapOf(),

    /**
     * See https://karma-runner.github.io/6.4/config/configuration-file.html#proxies
     */
    val proxies: MutableMap<String, String> = mutableMapOf(),

    /**
     * The port where the web server will be listening.
     *
     * If the defined port is already in use, karma will automatically increase its value in steps of 1 until a free port is found.
     *
     * The port should only be set by IntelliJ via an init script.
     */
    @property:ExternalKotlinTargetApi
    var port: Int? = null,

    /**
     * List of additional files which are necessary to be copied to the output directory
     *
     * It is not a Karma property, but a property of a kotlin-web-helpers plugin for Karma
     */
    internal val webpackCopy: MutableList<String> = mutableListOf(),
)

/**
 * Serializable view of Karma client config.
 *
 * **Note:** This class is not intended for use by build script or plugin authors.
 *
 * @see KarmaConfig
 */
data class KarmaClient(
    /**
     * See https://karma-runner.github.io/6.4/config/configuration-file.html#clientargs
     */
    val args: MutableList<String> = mutableListOf(),
)

/**
 * Serializable view of Karma custom launcher config.
 *
 * **Note:** This class is not intended for use by build script or plugin authors.
 *
 * Karma does not have detailed documentation on the available configuration options
 * for custom launchers.
 *
 * @see KarmaConfig.customLaunchers
 */
data class CustomLauncher(var base: String) {
    val flags = mutableListOf<String>()
    var debug: Boolean? = null
}
