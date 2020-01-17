// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.activity

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.util.execution.ParametersListUtil
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
abstract class RunAnythingCommandLineProvider : RunAnythingNotifiableProvider<String>() {

  open fun getHelpCommandAliases(): List<String> = emptyList()

  abstract override fun getHelpCommand(): String

  protected abstract fun suggestCompletionVariants(dataContext: DataContext, commandLine: CommandLine): Sequence<String>

  protected abstract fun run(dataContext: DataContext, commandLine: CommandLine): Boolean

  override fun getCommand(value: String) = value

  private fun getHelpCommands() = listOf(helpCommand) + getHelpCommandAliases()

  override fun findMatchingValue(dataContext: DataContext, pattern: String) =
    if (getHelpCommands().any { pattern.startsWith(it) }) getCommand(pattern) else null

  private fun extractLeadingHelpPrefix(commandLine: String): Pair<String, String>? {
    for (helpCommand in getHelpCommands()) {
      val prefix = "$helpCommand "
      when {
        commandLine.startsWith(prefix) -> return helpCommand to commandLine.removePrefix(prefix)
        prefix.startsWith(commandLine) -> return helpCommand to ""
      }
    }
    return null
  }

  private fun parseCommandLine(commandLine: String): CommandLine? {
    val (helpCommand, command) = extractLeadingHelpPrefix(commandLine) ?: return null
    val parameters = ParametersListUtil.parse(command, true, true, true)
    val toComplete = parameters.lastOrNull() ?: ""
    val prefix = command.removeSuffix(toComplete).trim()
    val nonEmptyParameters = parameters.filter { it.isNotEmpty() }
    val completedParameters = parameters.dropLast(1).filter { it.isNotEmpty() }
    return CommandLine(nonEmptyParameters, completedParameters, helpCommand, command.trim(), prefix, toComplete)
  }

  override fun getValues(dataContext: DataContext, pattern: String): List<String> {
    val commandLine = parseCommandLine(pattern) ?: return emptyList()
    val variants = suggestCompletionVariants(dataContext, commandLine)
    val helpCommand = commandLine.helpCommand
    val prefix = commandLine.prefix.let { if (it.isEmpty()) helpCommand else "$helpCommand $it" }
    return variants.map { "$prefix $it" }.toList()
  }

  override fun run(dataContext: DataContext, value: String): Boolean {
    val commandLine = parseCommandLine(value) ?: return false
    return run(dataContext, commandLine)
  }

  class CommandLine(
    val parameters: List<String>,
    val completedParameters: List<String>,
    val helpCommand: String,
    val command: String,
    val prefix: String,
    val toComplete: String
  ) {
    private val parameterSet by lazy { completedParameters.toSet() }
    operator fun contains(command: String) = command in parameterSet
  }
}