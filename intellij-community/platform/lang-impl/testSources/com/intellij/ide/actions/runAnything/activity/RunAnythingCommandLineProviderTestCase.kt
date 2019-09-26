// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.activity

import com.intellij.ide.actions.runAnything.activity.RunAnythingCommandLineProvider.CommandLine
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.testFramework.UsefulTestCase

abstract class RunAnythingCommandLineProviderTestCase : UsefulTestCase() {
  private val HELP_COMMAND = "start"

  fun getValuesFor(command: String, vararg variants: String): List<String> {
    val provider = object : RunAnythingCommandLineProvider() {
      override fun getHelpCommand() = HELP_COMMAND

      override fun suggestCompletionVariants(dataContext: DataContext, commandLine: CommandLine) =
        variants.asSequence()

      override fun runAnything(dataContext: DataContext, commandLine: CommandLine) = true
    }
    val emptyDataContext = DataContext { }
    val values = provider.getValues(emptyDataContext, "$HELP_COMMAND $command")
    assertTrue(values.all { it.startsWith("$HELP_COMMAND ") })
    return values.map { it.removePrefix("$HELP_COMMAND ") }
  }

  fun withCommandLineFor(command: String, action: (CommandLine) -> Unit) {
    var isSuggestingTouched = false
    var isRunningTouched = false
    val provider = object : RunAnythingCommandLineProvider() {
      override fun getHelpCommand() = HELP_COMMAND

      override fun suggestCompletionVariants(dataContext: DataContext, commandLine: CommandLine): Sequence<String> {
        action(commandLine)
        isSuggestingTouched = true
        return emptySequence()
      }

      override fun runAnything(dataContext: DataContext, commandLine: CommandLine): Boolean {
        action(commandLine)
        isRunningTouched = true
        return true
      }
    }
    val emptyDataContext = DataContext { }
    provider.getValues(emptyDataContext, "$HELP_COMMAND $command")
    provider.execute(emptyDataContext, "$HELP_COMMAND $command")
    assertTrue(isSuggestingTouched)
    assertTrue(isRunningTouched)
  }
}