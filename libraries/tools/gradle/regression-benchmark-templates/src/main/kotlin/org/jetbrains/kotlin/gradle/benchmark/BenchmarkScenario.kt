@file:Suppress("Unused", "Unused_Variable")
/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.benchmark

data class Scenario(
    var title: String? = null,
    var warmups: Int = 6,
    var iterations: Int = 10,
    val tasks: MutableList<String> = mutableListOf(),
    val gradleArgs: MutableList<String> = mutableListOf(),
    val cleanupTasks: MutableList<String> = mutableListOf(),
    val applyAbiChange: MutableList<String> = mutableListOf(),
    val applyNonAbiChange: MutableList<String> = mutableListOf(),
    val applyAndroidResourceValueChange: MutableList<String> = mutableListOf(),
    /**
     * The marker for the fake scenario runs for initialization before offline mode run.
     */
    val initRun: Boolean = false,
) {
    val configuredTitle: String
        get() = title ?: error("Scenario title is not set")

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

class ScenarioSuite(
    private val _scenarios: MutableList<Scenario> = mutableListOf<Scenario>()
) {
    val scenarios: List<Scenario> get() = _scenarios.toList()

    fun scenario(init: Scenario.() -> Unit) {
        val scenario = Scenario()
        scenario.init()
        _scenarios.add(scenario)
    }
}

fun ScenarioSuite.setupOfflineMode(): ScenarioSuite {
    val newScenarios = scenarios.flatMap { scenario ->
        val initScenario = scenario.copy(
            title = "${scenario.configuredTitle} (init)",
            warmups = 1,
            iterations = 0,
            initRun = true,
        )
        val offlineModeRun = scenario.copy(
            title = "${scenario.configuredTitle} (offline)",
            gradleArgs = scenario.gradleArgs.toMutableList().apply { add("--offline") },
        )
        listOf(initScenario, offlineModeRun)
    }
    return ScenarioSuite(newScenarios.toMutableList())
}

fun suite(init: ScenarioSuite.() -> Unit): ScenarioSuite {
    val suite = ScenarioSuite()
    suite.init()
    return suite
}
