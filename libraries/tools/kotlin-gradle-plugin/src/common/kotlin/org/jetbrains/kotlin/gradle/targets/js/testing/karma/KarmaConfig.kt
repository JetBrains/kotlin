/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.karma

/**
 * Serializable view of Karma config
 */
// https://karma-runner.github.io/4.0/config/configuration-file.html
data class KarmaConfig(
    var singleRun: Boolean = true,
    var autoWatch: Boolean = false,
    var basePath: String? = null,
    val files: MutableList<Any> = mutableListOf(),
    val frameworks: MutableList<String> = mutableListOf(),
    val client: KarmaClient = KarmaClient(),
    val browsers: MutableList<String> = mutableListOf(),
    val customLaunchers: MutableMap<String, CustomLauncher> = mutableMapOf(),
    var customContextFile: String? = null,
    var customDebugFile: String? = null,
    val failOnFailingTestSuite: Boolean = false,
    val failOnEmptyTestSuite: Boolean = false,
    val reporters: MutableList<String> = mutableListOf(),
    val preprocessors: MutableMap<String, MutableList<String>> = mutableMapOf(),
    val proxies: MutableMap<String, String> = mutableMapOf(),
    var port: Int? = null,

    /**
     * List of additional files which are necessary to be copied to the output directory
     *
     * It is not a Karma property, but a property of a kotlin-web-helpers plugin for Karma
     */
    internal val webpackCopy: MutableList<String> = mutableListOf(),
)

data class KarmaClient(
    val args: MutableList<String> = mutableListOf(),
)

data class CustomLauncher(var base: String) {
    val flags = mutableListOf<String>()
    var debug: Boolean? = null
}
