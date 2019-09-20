// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything

import com.intellij.ide.IdeBundle
import com.intellij.ide.actions.runAnything.RunAnythingContext.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.popup.ListSeparator
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.ErrorLabel
import com.intellij.ui.popup.ActionPopupStep
import com.intellij.ui.popup.PopupFactoryImpl
import com.intellij.ui.popup.list.PopupListElementRenderer
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.util.function.Supplier
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel

abstract class RunAnythingChooseContextAction(private val containingPanel: JPanel) : CustomComponentAction, DumbAware, ActionGroup() {
  override fun canBePerformed(context: DataContext): Boolean = true
  override fun isPopup(): Boolean = true
  override fun getChildren(e: AnActionEvent?): Array<AnAction> = EMPTY_ARRAY

  abstract var selectedContext: RunAnythingContext?
  abstract var availableContexts: List<RunAnythingContext>

  override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
    presentation.description = IdeBundle.message("run.anything.context.tooltip")
    return ActionButtonWithText(this, presentation, place, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE)
  }

  override fun update(e: AnActionEvent) {
    if (availableContexts.isEmpty()) {
      e.presentation.isEnabledAndVisible = false
      return
    }

    if (selectedContext != null && !availableContexts.contains(selectedContext!!)) {
      selectedContext = null
    }
    selectedContext = selectedContext ?: availableContexts[0]

    e.presentation.isEnabledAndVisible = true
    e.presentation.text = selectedContext!!.label
    e.presentation.icon = selectedContext!!.icon

    containingPanel.revalidate()
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project
    if (project == null) {
      return
    }

    val component = e.presentation.getClientProperty(CustomComponentAction.COMPONENT_KEY)
    if (component == null) {
      return
    }

    val updateToolbar = {
      val toolbar = UIUtil.uiParents(component, true).filter(ActionToolbar::class.java).first()
      toolbar!!.updateActionsImmediately()
    }

    val dataContext = e.dataContext
    val actionItems = ActionPopupStep.createActionItems(
      DefaultActionGroup(createItems()), dataContext,
      false,
      false, true,
      true, ActionPlaces.POPUP, null)

    ChooseContextPopup(ChooseContextPopupStep(actionItems, dataContext, updateToolbar), dataContext)
      .also { it.size = Dimension(500, 300) }
      .also { it.setRequestFocus(false) }
      .showUnderneathOf(component)
  }

  private fun createItems(): List<ContextItem> {
    return availableContexts.map {
      when (it) {
        is ProjectContext -> ProjectItem(it)
        is ModuleContext -> ModuleItem(it)
        is BrowseRecentDirectoryContext -> BrowseDirectoryItem(it)
        is RecentDirectoryContext -> RecentDirectoryItem(it)
        else -> throw UnsupportedOperationException()
      }
    }
  }

  inner class ProjectItem(context: ProjectContext) : ContextItem(context) {
    override fun update(e: AnActionEvent) {
      super.update(e)
      e.presentation.icon = EmptyIcon.ICON_16
    }
  }

  inner class ModuleItem(context: ModuleContext) : ContextItem(context)

  inner class BrowseDirectoryItem(context: BrowseRecentDirectoryContext) : ContextItem(context) {
    override fun actionPerformed(e: AnActionEvent) {
      ApplicationManager.getApplication().invokeLater {
        val project = e.project!!
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor().also { it.isForcedToUseIdeaFileChooser = true }
        FileChooserFactory.getInstance().createPathChooser(descriptor, project, e.dataContext.getData(PlatformDataKeys.CONTEXT_COMPONENT))
          .choose(project.guessProjectDir()) {
            val recentDirectories = RunAnythingContextRecentDirectoryCache.getInstance(project).state.paths
            val path = it.single().path
            if (recentDirectories.size >= Registry.intValue("run.anything.context.recent.directory.number")) {
              recentDirectories.removeAt(0)
            }
            recentDirectories.add(path)

            selectedContext = RecentDirectoryContext(path)
          }
      }
    }
  }

  inner class RecentDirectoryItem(context: RecentDirectoryContext) : ContextItem(context)

  abstract inner class ContextItem(val context: RunAnythingContext) : AnAction() {
    override fun update(e: AnActionEvent) {
      e.presentation.text = context.label
      e.presentation.description = context.description
      e.presentation.icon = context.icon
    }

    override fun actionPerformed(e: AnActionEvent) {
      selectedContext = context
    }
  }

  open inner class ChooseContextPopup(step: ActionPopupStep, dataContext: DataContext)
    : PopupFactoryImpl.ActionGroupPopup(null, step, null, dataContext, ActionPlaces.POPUP, -1) {
    override fun getListElementRenderer(): PopupListElementRenderer<PopupFactoryImpl.ActionItem> =
      object : PopupListElementRenderer<PopupFactoryImpl.ActionItem>(this) {
        private lateinit var myInfoLabel: JLabel

        override fun createItemComponent(): JComponent {
          myTextLabel = ErrorLabel()
          myInfoLabel = JLabel()
          myTextLabel.border = JBUI.Borders.emptyRight(10)

          val textPanel = JPanel(BorderLayout())
          textPanel.add(myTextLabel, BorderLayout.WEST)
          textPanel.add(myInfoLabel, BorderLayout.CENTER)
          return layoutComponent(textPanel)
        }

        override fun customizeComponent(list: JList<out PopupFactoryImpl.ActionItem>,
                                        actionItem: PopupFactoryImpl.ActionItem,
                                        isSelected: Boolean) {
          val event = ActionUtil.createEmptyEvent()
          ActionUtil.performDumbAwareUpdate(true, actionItem.action, event, false)

          val description = event.presentation.description
          if (description != null) {
            myInfoLabel.text = description
          }

          myTextLabel.text = event.presentation.text
          myInfoLabel.foreground = if (isSelected) UIUtil.getListSelectionForeground(true) else UIUtil.getInactiveTextColor()
        }
      }
  }

  class ChooseContextPopupStep(val actions: List<PopupFactoryImpl.ActionItem>, dataContext: DataContext, val updateToolbar: () -> Unit)
    : ActionPopupStep(actions, IdeBundle.message("run.anything.context.title.working.directory"), Supplier { dataContext }, null, true,
                      Condition { false }, false, true, null) {

    override fun getSeparatorAbove(value: PopupFactoryImpl.ActionItem?): ListSeparator? {
      val action = value?.action
      when {
        action is BrowseDirectoryItem -> return ListSeparator(IdeBundle.message("run.anything.context.separator.directories"))
        action is ModuleItem && action == actions.filter { it.action is ModuleItem }[0].action ->
          return ListSeparator(IdeBundle.message("run.anything.context.separator.modules"))
        else -> return super.getSeparatorAbove(value)
      }
    }

    override fun getFinalRunnable(): Runnable? {
      return super.getFinalRunnable().also { updateToolbar }
    }
  }

  companion object {
    fun allContexts(project: Project): List<RunAnythingContext> {
      return projectAndModulesContexts(project)
        .plus(BrowseRecentDirectoryContext)
        .plus(RunAnythingContextRecentDirectoryCache.getInstance(project).state.paths.map { RecentDirectoryContext(it) })
        .toList()
    }

    fun projectAndModulesContexts(project: Project): List<RunAnythingContext> {
      return sequenceOf<RunAnythingContext>()
        .plus(ProjectContext(project))
        .plus(ModuleManager.getInstance(project).modules.map {
          ModuleContext(it)
        }.let { if (it.size > 1) it else emptyList<RunAnythingContext>() })
        .toList()
    }
  }
}