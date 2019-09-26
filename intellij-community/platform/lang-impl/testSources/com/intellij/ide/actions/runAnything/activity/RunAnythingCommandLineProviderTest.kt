// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.activity

import org.junit.Test

class RunAnythingCommandLineProviderTest : RunAnythingCommandLineProviderTestCase() {
  @Test
  fun `test command line parsing`() {
    withCommandLineFor("") {
      assertSameElements(it.commands)
      assertSameElements(it.completedCommands)
      assertEquals("", it.command)
      assertEquals("", it.prefix)
      assertEquals("", it.toComplete)
    }
    withCommandLineFor("task") {
      assertSameElements(it.commands, "task")
      assertSameElements(it.completedCommands)
      assertEquals("task", it.command)
      assertEquals("", it.prefix)
      assertEquals("task", it.toComplete)
    }
    withCommandLineFor("task1 task2") {
      assertSameElements(it.commands, "task1", "task2")
      assertSameElements(it.completedCommands, "task1")
      assertEquals("task1 task2", it.command)
      assertEquals("task1", it.prefix)
      assertEquals("task2", it.toComplete)
    }
    withCommandLineFor("task1 task2 ") {
      assertSameElements(it.commands, "task1", "task2")
      assertSameElements(it.completedCommands, "task1", "task2")
      assertEquals("task1 task2", it.command)
      assertEquals("task1 task2", it.prefix)
      assertEquals("", it.toComplete)
    }
    val doubleQuotedTask = "\"task with spaces\""
    withCommandLineFor("task1 task2 $doubleQuotedTask") {
      assertSameElements(it.commands, "task1", "task2", doubleQuotedTask)
      assertSameElements(it.completedCommands, "task1", "task2")
      assertEquals("task1 task2 $doubleQuotedTask", it.command)
      assertEquals("task1 task2", it.prefix)
      assertEquals(doubleQuotedTask, it.toComplete)
    }
    val incompleteDoubleQuotedTask = "\"incomplete 'task"
    withCommandLineFor("task1 task2 $incompleteDoubleQuotedTask") {
      assertSameElements(it.commands, "task1", "task2", incompleteDoubleQuotedTask)
      assertSameElements(it.completedCommands, "task1", "task2")
      assertEquals("task1 task2 $incompleteDoubleQuotedTask", it.command)
      assertEquals("task1 task2", it.prefix)
      assertEquals(incompleteDoubleQuotedTask, it.toComplete)
    }
    val superDoubleQuotedTask = "super\"complex task 'with \\\" escaped\"quote"
    withCommandLineFor("task $superDoubleQuotedTask") {
      assertSameElements(it.commands, "task", superDoubleQuotedTask)
      assertSameElements(it.completedCommands, "task")
      assertEquals("task $superDoubleQuotedTask", it.command)
      assertEquals("task", it.prefix)
      assertEquals(superDoubleQuotedTask, it.toComplete)
    }
    val singleQuotedTask = "'task with spaces'"
    withCommandLineFor("task1 task2 $singleQuotedTask") {
      assertSameElements(it.commands, "task1", "task2", singleQuotedTask)
      assertSameElements(it.completedCommands, "task1", "task2")
      assertEquals("task1 task2 $singleQuotedTask", it.command)
      assertEquals("task1 task2", it.prefix)
      assertEquals(singleQuotedTask, it.toComplete)
    }
    val incompleteSingleQuotedTask = "'incomplete \"task"
    withCommandLineFor("task1 task2 $incompleteSingleQuotedTask") {
      assertSameElements(it.commands, "task1", "task2", incompleteSingleQuotedTask)
      assertSameElements(it.completedCommands, "task1", "task2")
      assertEquals("task1 task2 $incompleteSingleQuotedTask", it.command)
      assertEquals("task1 task2", it.prefix)
      assertEquals(incompleteSingleQuotedTask, it.toComplete)
    }
    val superSingleQuotedTask = "super'complex task \"with \\' escaped'quote"
    withCommandLineFor("task $superSingleQuotedTask") {
      assertSameElements(it.commands, "task", superSingleQuotedTask)
      assertSameElements(it.completedCommands, "task")
      assertEquals("task $superSingleQuotedTask", it.command)
      assertEquals("task", it.prefix)
      assertEquals(superSingleQuotedTask, it.toComplete)
    }
  }

  @Test
  fun `test variants completion`() {
    assertSameElements(getValuesFor("task", "task1", "task2"), "task1", "task2")
    assertSameElements(getValuesFor("task task", "task1", "task2"), "task task1", "task task2")
    assertSameElements(getValuesFor("task 'incomplete task", "'task1'", "'task2'"), "task 'task1'", "task 'task2'")
  }
}