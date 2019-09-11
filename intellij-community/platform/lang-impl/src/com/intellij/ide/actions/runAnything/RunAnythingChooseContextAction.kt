// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything

import com.intellij.icons.AllIcons
import com.intellij.ide.IdeBundle
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore.getModuleDirPath
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.popup.ListSeparator
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.io.FileUtil
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

  abstract var executionContext: RunAnythingContext?
  abstract var preferableContext: RunAnythingContext?
  abstract var availableContexts: Array<out Class<RunAnythingContext>>

  override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
    return ActionButtonWithText(this, presentation, place, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE)
  }

  override fun update(e: AnActionEvent) {
    when {
      executionContext != null -> updateByContext(e, executionContext!!)
      preferableContext != null -> updateByContext(e, preferableContext!!)
      else -> e.presentation.isEnabledAndVisible = false
    }
  }

  private fun updateByContext(e: AnActionEvent, context: RunAnythingContext) {
    when {
      availableContexts.contains(context::class.java) -> {
        updatePresentation(e, context)
      }
      availableContexts.isNotEmpty() -> {
        executionContext = RunAnythingContext.ProjectContext
        updatePresentation(e, executionContext!!)
      }
      else -> e.presentation.isEnabledAndVisible = false
    }

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
    val actionItems = PopupFactoryImpl.ActionGroupPopup.getActionItems(DefaultActionGroup(createItems(project)), dataContext,
                                                                       false,
                                                                       false, true,
                                                                       true, ActionPlaces.POPUP)

    ChooseContextPopup(ChooseContextPopupStep(actionItems, dataContext, updateToolbar), dataContext)
      .also { it.size = Dimension(500, 300) }
      .also { it.setRequestFocus(false)}
      .showUnderneathOf(component)
  }

  private fun createItems(project: Project): List<ContextItem> {
    return sequenceOf<ContextItem>(ProjectItem(project))
      .plus(BrowseDirectoryItem())
      .plus(RunAnythingContextRecentDirectoryCache.getInstance(project).state.paths.reversed().map<String, ContextItem> {
        RecentDirectoryItem(it)
      })
      .plus(ModuleManager.getInstance(project).modules.map { ModuleItem(it) }.let { if (it.size > 1) it else emptyList<ContextItem>() })
      .filter { item -> availableContexts.any { it == item.contextType } }
      .toList()
  }

  inner class ModuleItem(val module: Module) : ContextItem(RunAnythingContext.ModuleContext::class.java) {
    override fun actionPerformed(e: AnActionEvent) {
      applyContext(RunAnythingContext.ModuleContext(module))
    }

    override fun update(e: AnActionEvent) {
      e.presentation.text = module.name
      e.presentation.icon = AllIcons.Nodes.Module

      val moduleDirectory =
        ModuleRootManager.getInstance(module).contentRoots.let { if (it.size == 1) it[0].path else getModuleDirPath(module) }

      e.presentation.description =
        module.project.guessProjectDir()?.let { FileUtil.getRelativePath(it.path, moduleDirectory, '/') } ?: "undefined"
    }
  }

  inner class ProjectItem(val project: Project) : ContextItem(RunAnythingContext.ProjectContext::class.java) {
    override fun actionPerformed(e: AnActionEvent) {
      applyContext(RunAnythingContext.ProjectContext)
    }

    override fun update(e: AnActionEvent) {
      e.presentation.text = IdeBundle.message("run.anything.context.project")
      e.presentation.description = project.basePath
      e.presentation.icon = EmptyIcon.ICON_16
    }
  }

  inner class BrowseDirectoryItem : ContextItem(RunAnythingContext.RecentDirectoryContext::class.java) {
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

            applyContext(RunAnythingContext.RecentDirectoryContext(path))
          }
      }
    }

    override fun update(e: AnActionEvent) {
      e.presentation.text = IdeBundle.message("run.anything.context.browse.directory")
      e.presentation.description = ""
      e.presentation.icon = AllIcons.Actions.Menu_open
    }
  }

  inner class RecentDirectoryItem(val path: String) : ContextItem(RunAnythingContext.RecentDirectoryContext::class.java) {
    override fun actionPerformed(e: AnActionEvent) {
      applyContext(RunAnythingContext.RecentDirectoryContext(path))
    }

    override fun update(e: AnActionEvent) {
      e.presentation.text = FileUtil.getLocationRelativeToUserHome(path)
      e.presentation.description = ""
      e.presentation.icon = AllIcons.Nodes.Folder
    }
  }

  abstract inner class ContextItem(val contextType: Class<out RunAnythingContext>) : AnAction() {
    fun applyContext(context: RunAnythingContext) {
      executionContext = context
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
                      Condition { false }, false, true) {

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
    fun allContexts(): Array<Class<out RunAnythingContext>> {
      return arrayOf(RunAnythingContext.ProjectContext::class.java,
                     RunAnythingContext.ModuleContext::class.java,
                     RunAnythingContext.RecentDirectoryContext::class.java)
    }

    fun noneContext(): Array<Class<out RunAnythingContext>> {
      return arrayOf()
    }

    private fun updatePresentation(e: AnActionEvent, context: RunAnythingContext) {
      e.presentation.isEnabledAndVisible = true
      e.presentation.text = context.presentation.label
      e.presentation.icon = context.presentation.icon
    }
  }
}