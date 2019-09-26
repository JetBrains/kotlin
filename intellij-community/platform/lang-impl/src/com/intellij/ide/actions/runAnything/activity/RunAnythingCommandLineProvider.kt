// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.activity

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.util.text.StringUtil

abstract class RunAnythingCommandLineProvider : RunAnythingProviderWithVisibleExecutionFail<String>() {

  abstract override fun getHelpCommand(): String

  protected abstract fun suggestCompletionVariants(dataContext: DataContext, commandLine: CommandLine): Sequence<String>

  protected abstract fun runAnything(dataContext: DataContext, commandLine: CommandLine): Boolean

  override fun getCommand(value: String) = value

  override fun findMatchingValue(dataContext: DataContext, pattern: String) =
    if (pattern.startsWith(helpCommand)) getCommand(pattern) else null

  private fun parseCommandLine(commandLine: String): CommandLine? {
    val command = when {
      commandLine.startsWith(helpCommand) -> StringUtil.trimStart(commandLine, helpCommand)
      helpCommand.startsWith(commandLine) -> ""
      else -> return null
    }
    val parameters = parseIncompleteCommandLine(command)
    val toComplete = parameters.last()
    val prefix = command.removeSuffix(toComplete).trim()
    val commands = parameters.filter { it.isNotEmpty() }
    val completedCommands = parameters.dropLast(1).filter { it.isNotEmpty() }
    return CommandLine(commands, completedCommands, command.trim(), prefix, toComplete)
  }

  private fun parseIncompleteCommandLine(commandLine: CharSequence): List<String> {
    val parameters = ArrayList<String>()
    val parameter = StringBuilder()
    var leadingQuote: Char? = null
    var leadingSlash = false
    for (ch in commandLine) {
      if (!leadingSlash) {
        when (ch) {
          leadingQuote -> leadingQuote = null
          '\'', '"' -> leadingQuote = ch
        }
      }
      when (ch) {
        '\\' -> leadingSlash = !leadingSlash
        else -> leadingSlash = false
      }
      if (leadingQuote == null && ch == ' ') {
        parameters.add(parameter.toString())
        parameter.clear()
      }
      else {
        parameter.append(ch)
      }
    }
    parameters.add(parameter.toString())
    return parameters
  }

  override fun getValues(dataContext: DataContext, pattern: String): List<String> {
    val commandLine = parseCommandLine(pattern) ?: return emptyList()
    val variants = suggestCompletionVariants(dataContext, commandLine)
    val prefix = commandLine.prefix.let { if (it.isEmpty()) helpCommand else "$helpCommand $it" }
    return variants.map { "$prefix $it" }.toList()
  }

  override fun runAnything(dataContext: DataContext, value: String): Boolean {
    val commandLine = parseCommandLine(value) ?: return false
    return runAnything(dataContext, commandLine)
  }

  data class CommandLine(
    val commands: List<String>,
    val completedCommands: List<String>,
    val command: String,
    val prefix: String,
    val toComplete: String
  ) {

    private val commandSet by lazy { completedCommands.toSet() }
    operator fun contains(command: String) = command in commandSet
  }
}