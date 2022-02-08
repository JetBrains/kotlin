@file:Suppress("Unused", "Unused_Variable")
/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.benchmark

class Scenario {
    lateinit var title: String
    var warmups = 6
    var iterations = 10
    val tasks = mutableListOf<String>()
    val gradleArgs = mutableListOf<String>()
    val cleanupTasks = mutableListOf<String>()
    val applyAbiChange = mutableListOf<String>()
    val applyNonAbiChange = mutableListOf<String>()
    val applyAndroidResourceValueChange = mutableListOf<String>()

    fun runTasks(vararg tasks: String) {
        this.tasks.addAll(tasks)
    }

    fun useGradleArgs(vararg args: String) {
        gradleArgs.addAll(args)
    }

    fun runCleanupTasks(vararg tasks: String) {
        cleanupTasks.addAll(tasks)
    }

    fun applyAbiChangeTo(pathToClassFile: String) {
        applyAbiChange.add(pathToClassFile)
    }

    /**
     * Currently not-working: https://github.com/gradle/gradle-profiler/issues/378
     */
    fun applyNonAbiChangeTo(pathToClassFile: String) {
        applyNonAbiChange.add(pathToClassFile)
    }

    fun applyAndroidResourceValueChange(pathToResourceFile: String) {
        applyAndroidResourceValueChange.add(pathToResourceFile)
    }
}

class ScenarioSuite {
    private val _scenarios = mutableListOf<Scenario>()
    val scenarios: List<Scenario> get() = _scenarios.toList()

    fun scenario(init: Scenario.() -> Unit) {
        val scenario = Scenario()
        scenario.init()
        _scenarios.add(scenario)
    }
}

fun suite(init: ScenarioSuite.() -> Unit): ScenarioSuite {
    val suite = ScenarioSuite()
    suite.init()
    return suite
}
