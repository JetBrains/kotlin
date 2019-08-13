// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:ApiStatus.Experimental
package org.jetbrains.plugins.gradle.execution.test.runner

import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.plugins.gradle.execution.GradleRunnerUtil
import org.jetbrains.plugins.gradle.execution.test.runner.GradleTestRunConfigurationProducer.findTestsTaskToRun
import java.util.*
import kotlin.collections.LinkedHashSet

fun <E : PsiElement> ExternalSystemTaskExecutionSettings.applyTestConfiguration(
  module: Module,
  testTasksToRun: List<Map<String, List<String>>>,
  sourceElements: Iterable<E>,
  createFilter: (E) -> String
): Boolean {
  return applyTestConfiguration(module, testTasksToRun, sourceElements, ::getSourceFile, createFilter)
}

fun <E : PsiElement> ExternalSystemTaskExecutionSettings.applyTestConfiguration(
  module: Module,
  sourceElements: Iterable<E>,
  createFilter: (E) -> String
): Boolean {
  return applyTestConfiguration(module, sourceElements, ::getSourceFile, createFilter)
}

fun <E : PsiElement> ExternalSystemTaskExecutionSettings.applyTestConfiguration(
  module: Module,
  testTasksToRun: List<Map<String, List<String>>>,
  vararg sourceElements: E,
  createFilter: (E) -> String
): Boolean {
  return applyTestConfiguration(module, testTasksToRun, sourceElements.asIterable(), createFilter)
}

fun <E : PsiElement> ExternalSystemTaskExecutionSettings.applyTestConfiguration(
  module: Module,
  vararg sourceElements: E,
  createFilter: (E) -> String
): Boolean {
  return applyTestConfiguration(module, sourceElements.asIterable(), createFilter)
}

fun <T> ExternalSystemTaskExecutionSettings.applyTestConfiguration(
  module: Module,
  testTasksToRun: List<Map<String, List<String>>>,
  tests: Iterable<T>,
  findTestSource: (T) -> VirtualFile?,
  createFilter: (T) -> String): Boolean {
  return applyTestConfiguration(module, tests, findTestSource, createFilter) { source ->
    testTasksToRun.mapNotNull { it[source.path] }
  }
}

fun <T> ExternalSystemTaskExecutionSettings.applyTestConfiguration(
  module: Module,
  tests: Iterable<T>,
  findTestSource: (T) -> VirtualFile?,
  createFilter: (T) -> String): Boolean {
  return applyTestConfiguration(module, tests, findTestSource, createFilter) { source ->
    listOf(findTestsTaskToRun(source, module.project))
  }
}

fun <T> ExternalSystemTaskExecutionSettings.applyTestConfiguration(
  module: Module,
  tests: Iterable<T>,
  findTestSource: (T) -> VirtualFile?,
  createFilter: (T) -> String,
  getTestsTaskToRun: (VirtualFile) -> List<List<String>>
): Boolean {
  if (!GradleRunnerUtil.isGradleModule(module)) return false
  val projectPath = GradleRunnerUtil.resolveProjectPath(module) ?: return false
  return applyTestConfiguration(projectPath, tests, findTestSource, createFilter, getTestsTaskToRun)
}

fun <T> ExternalSystemTaskExecutionSettings.applyTestConfiguration(
  projectPath: String,
  tests: Iterable<T>,
  findTestSource: (T) -> VirtualFile?,
  createFilter: (T) -> String,
  getTestsTaskToRun: (VirtualFile) -> List<List<String>>
): Boolean {
  val testRunConfigurations = LinkedHashMap<String, Pair<List<String>, MutableSet<String>>>()
  for (test in tests) {
    val sourceFile = findTestSource(test) ?: return false
    for (tasks in getTestsTaskToRun(sourceFile)) {
      val commandLine = tasks.joinToString(" ") { it.escapeIfNeeded() }
      val (_, arguments) = testRunConfigurations.getOrPut(commandLine) { Pair(tasks, LinkedHashSet()) }
      arguments.add(createFilter(test).trim())
    }
  }
  val taskSettings = ArrayList<Pair<String, Set<String>>>()
  val unorderedParameters = ArrayList<String>()
  for ((tasks, arguments) in testRunConfigurations.values) {
    if (tasks.isEmpty()) continue
    for (task in tasks.dropLast(1)) {
      taskSettings.add(task to emptySet())
    }
    val last = tasks.last()
    taskSettings.add(last to arguments)
  }
  if (testRunConfigurations.size > 1) {
    unorderedParameters.add("--continue")
  }
  setFrom(projectPath, taskSettings, unorderedParameters)
  return true
}

private fun ExternalSystemTaskExecutionSettings.setFrom(
  projectPath: String,
  taskSettings: List<Pair<String, Set<String>>>,
  unorderedParameters: List<String>
) {
  val hasTasksAfterTaskWithArguments = taskSettings.dropWhile { it.second.isEmpty() }.size > 1
  if (hasTasksAfterTaskWithArguments) {
    val joiner = StringJoiner(" ")
    for ((task, arguments) in taskSettings) {
      joiner.add(task.escapeIfNeeded())
      joiner.addAll(arguments)
    }
    joiner.addAll(unorderedParameters)
    taskNames = emptyList()
    scriptParameters = joiner.toString()
  }
  else {
    val joiner = StringJoiner(" ")
    joiner.addAll(taskSettings.lastOrNull()?.second ?: emptyList())
    joiner.addAll(unorderedParameters)
    taskNames = taskSettings.map { it.first.escapeIfNeeded() }
    scriptParameters = joiner.toString()
  }
  externalProjectPath = projectPath
}

fun String.escapeIfNeeded() = when {
  contains(' ') -> "'$this'"
  else -> this
}

private fun StringJoiner.addAll(elements: Iterable<String>) = apply {
  for (element in elements) {
    add(element)
  }
}

fun getSourceFile(sourceElement: PsiElement?): VirtualFile? {
  if (sourceElement == null) return null
  if (sourceElement is PsiFileSystemItem) {
    return sourceElement.virtualFile
  }
  val containingFile = sourceElement.containingFile
  if (containingFile != null) {
    return containingFile.virtualFile
  }
  return null
}
