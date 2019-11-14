/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.karma

// https://karma-runner.github.io/4.0/config/configuration-file.html
data class KarmaConfig(
    var singleRun: Boolean = true,
    var autoWatch: Boolean = false,
    var basePath: String? = null,
    val files: MutableList<String> = mutableListOf(),
    val frameworks: MutableList<String> = mutableListOf(),
    val client: KarmaClient = KarmaClient(),
    val browsers: MutableList<String> = mutableListOf(),
    val customLaunchers: MutableMap<String, CustomLauncher> = mutableMapOf(),
    val failOnFailingTestSuite: Boolean = false,
    val reporters: MutableList<String> = mutableListOf(),
    val preprocessors: MutableMap<String, MutableList<String>> = mutableMapOf(),
    var coverageReporter: CoverageReporter? = null
)

data class KarmaClient(
    val args: MutableList<String> = mutableListOf(),
    val mocha: KarmaMocha = KarmaMocha()
)

data class KarmaMocha(
    var timeout: String = "2s"
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