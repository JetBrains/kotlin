// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.actions

import com.intellij.icons.AllIcons
import com.intellij.lang.LangBundle
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText
import com.intellij.openapi.application.Experiments
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.editor.markup.InspectionWidgetActionProvider
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.JBColor
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.NotNullProducer
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.JBUI
import javax.swing.JComponent
import javax.swing.plaf.FontUIResource

class ReaderModeActionProvider(override val separator: Separator? = Separator.create()) : InspectionWidgetActionProvider {
  override fun getAction(editor: Editor): AnAction {
    return object : DumbAwareAction(LangBundle.messagePointer("action.ReaderModeProvider.text"),
                                    LangBundle.messagePointer("action.ReaderModeProvider.description"), null), CustomComponentAction {
      override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        val actionButtonWithText = object : ActionButtonWithText(this, presentation, place, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE) {
          override fun iconTextSpace() = JBUI.scale(2)
        }

        actionButtonWithText.foreground = JBColor(NotNullProducer { editor.colorsScheme.getColor(FOREGROUND) ?: FOREGROUND.defaultColor })

        if (!SystemInfo.isWindows) {
          var font = actionButtonWithText.font
          font = FontUIResource(font.deriveFont(font.style, font.size - JBUIScale.scale(2).toFloat()))
          actionButtonWithText.font = (font)
        }

        return actionButtonWithText
      }

      override fun actionPerformed(e: AnActionEvent) {
        val project = e.project?: return

        ReaderModeSettings.instance(project).enabled = !ReaderModeSettings.instance(project).enabled
        project.messageBus.syncPublisher(READER_MODE_TOPIC).modeChanged(project)
      }

      override fun update(e: AnActionEvent) {
        if (!Experiments.getInstance().isFeatureEnabled("editor.reader.mode")) {
          e.presentation.isEnabledAndVisible = false
          return
        }

        val project = e.project?: return
        val file = PlatformDataKeys.VIRTUAL_FILE.getData(e.dataContext)
        e.presentation.isEnabledAndVisible = ReaderModeFileEditorListener.matchMode(project, file)

        val presentation = e.presentation
        if (!ReaderModeSettings.instance(project).enabled) {
          presentation.text = null
          presentation.icon = AllIcons.Gutter.JavadocRead
          presentation.hoveredIcon = null
        }
        else {
          presentation.text = LangBundle.message("action.ReaderModeProvider.text")
          presentation.icon = EmptyIcon.ICON_16
          presentation.hoveredIcon = AllIcons.Actions.Cancel
        }
      }
    }
  }

  companion object {
    val FOREGROUND = ColorKey.createColorKey("ActionButton.iconTextForeground",com.intellij.util.ui.UIUtil.getContextHelpForeground())
  }
}