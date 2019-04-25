/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.karma

// https://karma-runner.github.io/3.0/config/configuration-file.html
class KarmaConfig {
    var singleRun: Boolean = true
    var autoWatch: Boolean = false
    var basePath: String? = null
    val files: MutableList<String> = mutableListOf()
    val frameworks: MutableList<String> = mutableListOf()
    val browsers: MutableList<String> = mutableListOf()
    val reporters: MutableList<String> = mutableListOf()
    val preprocessors: MutableMap<String, List<String>> = mutableMapOf()
}