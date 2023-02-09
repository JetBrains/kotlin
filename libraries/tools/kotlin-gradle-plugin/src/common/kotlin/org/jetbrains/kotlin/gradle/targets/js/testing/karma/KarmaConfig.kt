/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.karma

import java.io.File

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
    var coverageReporter: CoverageReporter? = null,
    val proxies: MutableMap<String, String> = mutableMapOf()
)

data class KarmaFile(
    val pattern: String,
    val included: Boolean,
    val served: Boolean,
    val watched: Boolean
)

data class KarmaClient(
    val args: MutableList<String> = mutableListOf()
)

class CustomLauncher(var base: String) {
    val flags = mutableListOf<String>()
    var debug: Boolean? = null
}

data class CoverageReporter(
    var dir: String,
    val reporters: MutableList<Reporter> = mutableListOf()
)

data class Reporter(
    val type: String,
    val subDir: String? = null,
    val file: String? = null
)