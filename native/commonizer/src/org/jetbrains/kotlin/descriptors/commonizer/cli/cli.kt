/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("CommonizerCLI")

package org.jetbrains.kotlin.descriptors.commonizer.cli

import org.jetbrains.kotlin.descriptors.commonizer.cli.Task.Category
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty()) printUsageAndExit()

    val tokens = args.iterator()
    val tasks = mutableListOf<Task>()

    var taskAlias: String? = tokens.next()
    while (taskAlias != null) {
        taskAlias = parseTask(taskAlias, tokens, tasks)
    }

    // execute tasks in a specific order:
    // - first, execute all informational tasks
    // - then, all commonization tasks
    Category.values().forEach { category ->
        val sortedTasks = tasks.filter { it.category == category }.sorted()
        if (sortedTasks.isNotEmpty()) {
            category.prologue?.let(::println)

            sortedTasks.forEachIndexed { index, task ->
                val logPrefix = if (category.logEachStep) "[Step ${index + 1} of ${sortedTasks.size}] " else ""
                task.execute(logPrefix)
            }

            category.epilogue?.let(::println)
        }
    }
}

private fun parseTask(
    taskAlias: String,
    tokens: Iterator<String>,
    tasks: MutableList<Task>
): String? {
    val taskType = TaskType.getByAlias(taskAlias) ?: printUsageAndExit("Unknown task $taskAlias")
    val optionTypes = taskType.optionTypes.associateBy { it.alias }
    val options = mutableMapOf<String, Option<*>>()

    fun buildOngoingTask() {
        // check options completeness
        val missedMandatoryOptions = optionTypes.filterKeys { it !in options }.filterValues { it.mandatory }.keys
        if (missedMandatoryOptions.isNotEmpty())
            printUsageAndExit("Mandatory options not specified in task $taskAlias: " + missedMandatoryOptions.joinToString { "-$it" })

        tasks += taskType.taskConstructor(options.values)
    }

    while (tokens.hasNext()) {
        val optionAlias = tokens.next().let { token ->
            if (!token.startsWith('-')) {
                buildOngoingTask()

                // proceed to the next task
                return token
            }

            token.trimStart('-')
        }

        if (optionAlias in options) printUsageAndExit("Duplicated value for option -$optionAlias in task $taskAlias")

        val optionType = optionTypes[optionAlias] ?: printUsageAndExit("Unknown option -$optionAlias in task $taskAlias")

        val rawValue = if (tokens.hasNext()) tokens.next() else printUsageAndExit("No value for option -$optionAlias in task $taskAlias")
        val option = optionType.parse(rawValue) { reason ->
            printUsageAndExit("Failed to parse option -$optionAlias in task $taskAlias: $reason")
        }

        options[optionAlias] = option
    }

    buildOngoingTask()
    return null // no next task
}

private fun printUsageAndExit(errorMessage: String? = null): Nothing {
    if (errorMessage != null) {
        println("Error: $errorMessage")
        println()
    }

    fun formatLeft(indent: Int, left: String) = StringBuilder().apply {
        repeat(indent) { append("    ") }
        append(left)
    }

    fun StringBuilder.formatRight(right: String): String {
        val middleSpace = kotlin.math.max(38 - length, 1)
        repeat(middleSpace) { append(" ") }
        append(right)
        return this.toString()
    }

    fun formatBoth(indent: Int, left: String, right: String) = formatLeft(indent, left).formatRight(right)

    println("Usage: ${::printUsageAndExit.javaClass.`package`.name}.CommonizerCLI <task> <options> [<task> <options>...]")
    println()
    println("Tasks:")
    for (taskType in TaskType.values()) {
        println(formatBoth(1, taskType.alias, taskType.description))
        println(formatLeft(1, if (taskType.optionTypes.isNotEmpty()) "Options:" else "No options."))
        for (optionType in taskType.optionTypes) {
            val lines = optionType.description.split('\n')
            println(formatBoth(2, "-${optionType.alias}", lines.first()))
            lines.drop(1).forEach { println(StringBuilder().formatRight(it)) }
        }
        println()
    }

    exitProcess(if (errorMessage != null) 1 else 0)
}
