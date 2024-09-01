/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("CommonizerCLI")

package org.jetbrains.kotlin.commonizer.cli

import org.jetbrains.kotlin.cli.common.arguments.ArgumentParseErrors
import org.jetbrains.kotlin.cli.common.arguments.preprocessCommandLineArguments
import org.jetbrains.kotlin.commonizer.cli.Task.Category
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val tasks = parseTasksFromCommandLineArguments(args)
    executeTasks(tasks)
}

internal fun parseTasksFromCommandLineArguments(args: Array<String>): MutableList<Task> {
    if (args.isEmpty()) printUsageAndExit()

    val argumentsWithArgfilesExpanded = preprocessCommandLineArguments(args)

    val tokens: Iterator<String> = argumentsWithArgfilesExpanded.iterator()
    val tasks = mutableListOf<Task>()

    var taskAlias: String? = tokens.next()
    while (taskAlias != null) {
        taskAlias = parseTask(taskAlias, tokens, tasks)
    }
    return tasks
}

/**
 * Returns an original [args] with @argfile-arguments inlined.
 */
private fun preprocessCommandLineArguments(args: Array<String>): List<String> {
    val errors = lazy { ArgumentParseErrors() }
    val argumentsWithArgfilesExpanded = preprocessCommandLineArguments(args.asList(), errors)
    // We're using kotlinc infra for argfiles parsing, so general API of ArgumentParseErrors is a bit wider than what we need
    // In fact, 'errors' will contain only 'argfileErrors'
    if (errors.value.argfileErrors.isNotEmpty()) {
        printUsageAndExit("Errors while using @argfiles\n" + errors.value.argfileErrors.joinToString("\n"))
    }
    return argumentsWithArgfilesExpanded
}

// execute tasks in a specific order:
// - first, execute all informational tasks
// - then, all commonization tasks
private fun executeTasks(tasks: MutableList<Task>) {
    Category.values().forEach { category ->
        val sortedTasks = tasks.filter { it.category == category }.sorted()
        if (sortedTasks.isNotEmpty()) {
            category.prologue?.let(::println)

            sortedTasks.forEachIndexed { index, task ->
                val logPrefix = if (category.logEachStep && sortedTasks.size > 1) "[Step ${index + 1} of ${sortedTasks.size}] " else ""
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
        val missingMandatoryOptions = optionTypes.filterKeys { it !in options }.filterValues { it.mandatory }.keys
        if (missingMandatoryOptions.isNotEmpty())
            printUsageAndExit(
                "Mandatory options not specified in task $taskAlias: " + missingMandatoryOptions.joinToString { "-$it" } + "\n" +
                        "Specified options: ${options.keys.joinToString(", ")}"
            )

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
