// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.test.runner

import com.intellij.ide.IdeTooltipManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.scope.TestsScope
import com.intellij.ui.FileColorManager
import com.intellij.util.FunctionUtil
import com.intellij.util.getBestBalloonPosition
import com.intellij.util.getBestPopupPosition
import com.intellij.util.ui.JBUI
import icons.ExternalSystemIcons
import org.jetbrains.plugins.gradle.execution.test.runner.GradleTestRunConfigurationProducer.findAllTestsTaskToRun
import org.jetbrains.plugins.gradle.util.GradleBundle
import org.jetbrains.plugins.gradle.util.TasksToRun
import java.awt.Component
import java.util.function.Consumer
import javax.swing.DefaultListCellRenderer
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.border.EmptyBorder

typealias SourcePath = String
typealias TestName = String
typealias TestTasks = List<String>

open class TestTasksChooser {
  private val LOG = Logger.getInstance(TestTasksChooser::class.java)

  @Suppress("CAST_NEVER_SUCCEEDS")
  private fun error(message: String): Nothing = LOG.error(message) as Nothing

  fun chooseTestTasks(
    project: Project,
    context: DataContext,
    elements: Iterable<PsiElement>,
    consumer: Consumer<List<Map<SourcePath, TestTasks>>>
  ) {
    val sources = elements.map { getSourceFile(it) ?: error("Can not find source file for $it") }
    chooseTestTasks(project, context, sources, consumer)
  }

  fun chooseTestTasks(
    project: Project,
    context: DataContext,
    vararg elements: PsiElement,
    consumer: Consumer<List<Map<SourcePath, TestTasks>>>
  ) {
    chooseTestTasks(project, context, elements.asIterable(), consumer)
  }

  fun chooseTestTasks(
    project: Project,
    context: DataContext,
    sources: List<VirtualFile>,
    consumer: Consumer<List<Map<SourcePath, TestTasks>>>
  ) {
    val testTasks = findAllTestsTaskToRun(sources, project)
    when {
      testTasks.isEmpty() -> showTestsNotFoundWarning(project, context)
      testTasks.size == 1 -> consumer.accept(testTasks.values.toList())
      else -> chooseTestTasks(project, context, testTasks, consumer)
    }
  }

  private fun findAllTestsTaskToRun(
    sources: List<VirtualFile>,
    project: Project
  ): Map<TestName, Map<SourcePath, TasksToRun>> {
    val testTasks: Map<SourcePath, Map<TestName, TasksToRun>> =
      sources.map { source -> source.path to findAllTestsTaskToRun(source, project).map { it.testName to it }.toMap() }.toMap()
    val testTaskNames = testTasks.flatMap { it.value.keys }.toSet()
    return testTaskNames.map { name -> name to testTasks.mapNotNullValues { it.value[name] } }.toMap()
  }

  protected open fun chooseTestTasks(
    project: Project,
    context: DataContext,
    testTasks: Map<TestName, Map<SourcePath, TasksToRun>>,
    consumer: Consumer<List<Map<SourcePath, TestTasks>>>
  ) {
    assert(!ApplicationManager.getApplication().isCommandLine)
    val sortedTestTasksNames = testTasks.keys.toList().sortedByDescending { it == TEST_TASK_NAME }
    val testTaskRenderer = TestTaskListCellRenderer(project)
    JBPopupFactory.getInstance()
      .createPopupChooserBuilder(sortedTestTasksNames)
      .setRenderer(testTaskRenderer)
      .setTitle(suggestPopupTitle(context))
      .setAutoselectOnMouseMove(false)
      .setNamerForFiltering(FunctionUtil.id())
      .setMovable(true)
      .setAdText(GradleBundle.message("gradle.tests.tasks.choosing.popup.hint"))
      .setResizable(false)
      .setRequestFocus(true)
      .setMinSize(JBUI.size(270, 55))
      .setItemsChosenCallback {
        val choosesTestTasks = it.mapNotNull(testTasks::get)
        when {
          choosesTestTasks.isEmpty() -> showTestsNotFoundWarning(project, context)
          else -> consumer.accept(choosesTestTasks)
        }
      }
      .createPopup()
      .show(getBestPopupPosition(context))
  }

  protected open fun showTestsNotFoundWarning(project: Project, context: DataContext) {
    assert(!ApplicationManager.getApplication().isCommandLine)
    JBPopupFactory.getInstance()
      .createBalloonBuilder(JLabel(GradleBundle.message("gradle.tests.tasks.choosing.warning.text")))
      .setDisposable(ApplicationManager.getApplication())
      .setFillColor(IdeTooltipManager.getInstance().getTextBackground(false))
      .createBalloon()
      .show(getBestBalloonPosition(context), Balloon.Position.above)
  }

  private fun suggestPopupTitle(context: DataContext): String {
    val locationName = context.getData(LOCATION)
    return when (locationName) {
      null -> GradleBundle.message("gradle.tests.tasks.choosing.popup.title.common")
      else -> GradleBundle.message("gradle.tests.tasks.choosing.popup.title", locationName)
    }
  }

  private class TestTaskListCellRenderer(project: Project) : DefaultListCellRenderer() {
    private val cellInsets = JBUI.insets(1, 5)
    private val colorManager = FileColorManager.getInstance(project)

    override fun getListCellRendererComponent(
      list: JList<*>?,
      value: Any?,
      index: Int,
      isSelected: Boolean,
      cellHasFocus: Boolean
    ): Component {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
      text = value.toString()
      icon = ExternalSystemIcons.Task
      iconTextGap = cellInsets.left
      border = EmptyBorder(cellInsets)
      if (!isSelected) {
        background = colorManager.getScopeColor(TestsScope.NAME)
      }
      return this
    }
  }

  companion object {
    private const val TEST_TASK_NAME = "test"

    @JvmField
    val LOCATION = DataKey.create<String?>("org.jetbrains.plugins.gradle.execution.test.runner.TestTasksChooser.LOCATION")

    @JvmStatic
    fun contextWithLocationName(context: DataContext, locationName: String?): DataContext {
      if (locationName == null) return context
      return DataContext {
        when {
          LOCATION.`is`(it) -> locationName
          else -> context.getData(it)
        }
      }
    }

    private fun <K, V, R> Map<K, V>.mapNotNullValues(transform: (Map.Entry<K, V>) -> R?): Map<K, R> =
      mapNotNull { entry -> transform(entry)?.let { entry.key to it } }.toMap()
  }
}