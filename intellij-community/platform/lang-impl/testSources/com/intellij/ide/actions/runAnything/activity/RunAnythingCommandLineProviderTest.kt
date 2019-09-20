// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.activity

import org.junit.Test

class RunAnythingCommandLineProviderTest : RunAnythingCommandLineProviderTestCase() {
  @Test
  fun `test command line parsing`() {
    withCommandLineFor("") {
      assertOrderedEquals(it.parameters)
      assertOrderedEquals(it.completedParameters)
      assertEquals("", it.command)
      assertEquals("", it.prefix)
      assertEquals("", it.toComplete)
    }
    withCommandLineFor("task") {
      assertOrderedEquals(it.parameters, "task")
      assertOrderedEquals(it.completedParameters)
      assertEquals("task", it.command)
      assertEquals("", it.prefix)
      assertEquals("task", it.toComplete)
    }
    withCommandLineFor("task1 task2") {
      assertOrderedEquals(it.parameters, "task1", "task2")
      assertOrderedEquals(it.completedParameters, "task1")
      assertEquals("task1 task2", it.command)
      assertEquals("task1", it.prefix)
      assertEquals("task2", it.toComplete)
    }
    withCommandLineFor("task1 task2 ") {
      assertOrderedEquals(it.parameters, "task1", "task2")
      assertOrderedEquals(it.completedParameters, "task1", "task2")
      assertEquals("task1 task2", it.command)
      assertEquals("task1 task2", it.prefix)
      assertEquals("", it.toComplete)
    }
    val doubleQuotedTask = "\"task with spaces\""
    withCommandLineFor("task1 task2 $doubleQuotedTask") {
      assertOrderedEquals(it.parameters, "task1", "task2", doubleQuotedTask)
      assertOrderedEquals(it.completedParameters, "task1", "task2")
      assertEquals("task1 task2 $doubleQuotedTask", it.command)
      assertEquals("task1 task2", it.prefix)
      assertEquals(doubleQuotedTask, it.toComplete)
    }
    val incompleteDoubleQuotedTask = "\"incomplete 'task"
    withCommandLineFor("task1 task2 $incompleteDoubleQuotedTask") {
      assertOrderedEquals(it.parameters, "task1", "task2", incompleteDoubleQuotedTask)
      assertOrderedEquals(it.completedParameters, "task1", "task2")
      assertEquals("task1 task2 $incompleteDoubleQuotedTask", it.command)
      assertEquals("task1 task2", it.prefix)
      assertEquals(incompleteDoubleQuotedTask, it.toComplete)
    }
    val superDoubleQuotedTask = "super\"complex task 'with \\\" escaped\"quote"
    withCommandLineFor("task $superDoubleQuotedTask") {
      assertOrderedEquals(it.parameters, "task", superDoubleQuotedTask)
      assertOrderedEquals(it.completedParameters, "task")
      assertEquals("task $superDoubleQuotedTask", it.command)
      assertEquals("task", it.prefix)
      assertEquals(superDoubleQuotedTask, it.toComplete)
    }
    val singleQuotedTask = "'task with spaces'"
    withCommandLineFor("task1 task2 $singleQuotedTask") {
      assertOrderedEquals(it.parameters, "task1", "task2", singleQuotedTask)
      assertOrderedEquals(it.completedParameters, "task1", "task2")
      assertEquals("task1 task2 $singleQuotedTask", it.command)
      assertEquals("task1 task2", it.prefix)
      assertEquals(singleQuotedTask, it.toComplete)
    }
    val incompleteSingleQuotedTask = "'incomplete \"task"
    withCommandLineFor("task1 task2 $incompleteSingleQuotedTask") {
      assertOrderedEquals(it.parameters, "task1", "task2", incompleteSingleQuotedTask)
      assertOrderedEquals(it.completedParameters, "task1", "task2")
      assertEquals("task1 task2 $incompleteSingleQuotedTask", it.command)
      assertEquals("task1 task2", it.prefix)
      assertEquals(incompleteSingleQuotedTask, it.toComplete)
    }
    val superSingleQuotedTask = "super'complex task \"with \\' escaped'quote"
    withCommandLineFor("task $superSingleQuotedTask") {
      assertOrderedEquals(it.parameters, "task", superSingleQuotedTask)
      assertOrderedEquals(it.completedParameters, "task")
      assertEquals("task $superSingleQuotedTask", it.command)
      assertEquals("task", it.prefix)
      assertEquals(superSingleQuotedTask, it.toComplete)
    }
  }

  @Test
  fun `test variants completion`() {
    assertOrderedEquals(getValuesFor("task", "task1", "task2"), "task1", "task2")
    assertOrderedEquals(getValuesFor("task task", "task1", "task2"), "task task1", "task task2")
    assertOrderedEquals(getValuesFor("task 'incomplete task", "'task1'", "'task2'"), "task 'task1'", "task 'task2'")
  }

  @Test
  fun `test help command aliases`() {
    withHelpCommands("start", "starts", "run") {
      assertOrderedEquals(getValuesFor("task", "task1", "task2", prefix = "do"))
      assertOrderedEquals(getValuesFor("task", "task1", "task2", prefix = "run"), "task1", "task2")
      assertOrderedEquals(getValuesFor("task", "task1", "task2", prefix = "runs"))
      assertOrderedEquals(getValuesFor("task", "task1", "task2", prefix = "start"), "task1", "task2")
      assertOrderedEquals(getValuesFor("task", "task1", "task2", prefix = "starts"), "task1", "task2")
    }
    withHelpCommands("start", "starts", "run") {
      withCommandLineFor("task ", prefix = "run") {
        assertOrderedEquals(it.parameters, "task")
        assertOrderedEquals(it.completedParameters, "task")
        assertEquals("run", it.helpCommand)
        assertEquals("task", it.command)
        assertEquals("task", it.prefix)
        assertEquals("", it.toComplete)
      }
      withCommandLineFor("task ", prefix = "start") {
        assertOrderedEquals(it.parameters, "task")
        assertOrderedEquals(it.completedParameters, "task")
        assertEquals("start", it.helpCommand)
        assertEquals("task", it.command)
        assertEquals("task", it.prefix)
        assertEquals("", it.toComplete)
      }
      withCommandLineFor("task ", prefix = "starts") {
        assertOrderedEquals(it.parameters, "task")
        assertOrderedEquals(it.completedParameters, "task")
        assertEquals("starts", it.helpCommand)
        assertEquals("task", it.command)
        assertEquals("task", it.prefix)
        assertEquals("", it.toComplete)
      }
    }
  }
}