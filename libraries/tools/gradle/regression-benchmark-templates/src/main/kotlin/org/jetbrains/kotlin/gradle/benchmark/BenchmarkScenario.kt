/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("Unused")

package org.jetbrains.kotlin.gradle.benchmark

class Scenario {
    lateinit var title: String
    var warmups: Int =
        System.getenv("kotlin_gradle_benchmark_default_warmups")?.toInt() ?: 6
    var iterations: Int =
        System.getenv("kotlin_gradle_benchmark_default_iterations")?.toInt() ?: 100
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
