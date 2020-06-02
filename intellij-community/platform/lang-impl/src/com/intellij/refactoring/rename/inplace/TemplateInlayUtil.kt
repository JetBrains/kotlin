// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.rename.inplace

import com.intellij.codeInsight.hints.InlayPresentationFactory
import com.intellij.codeInsight.hints.presentation.IconPresentation
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.codeInsight.hints.presentation.PresentationRenderer
import com.intellij.codeInsight.template.impl.TemplateState
import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.refactoring.util.TextOccurrencesUtil
import com.intellij.ui.layout.*
import com.intellij.ui.popup.PopupFactoryImpl
import org.jetbrains.annotations.ApiStatus
import java.awt.Color
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JLabel

@ApiStatus.Experimental
object TemplateInlayUtil {

  @JvmStatic
  fun createNavigatableButton(templateState: TemplateState,
                              inEditorOffset: Int,
                              presentation: SelectableInlayPresentation): Inlay<PresentationRenderer>? {
    val renderer = PresentationRenderer(presentation)
    val inlay = templateState.editor.inlayModel.addInlineElement(inEditorOffset, true, renderer) ?: return null
    VirtualTemplateElement.installOnTemplate(templateState, object : VirtualTemplateElement {
      override fun onSelect(templateState: TemplateState) {
        presentation.isSelected = true
      }
    })
    Disposer.register(templateState, inlay)
    return inlay
  }

  @JvmStatic
  fun createNavigatableButtonWithPopup(templateState: TemplateState,
                                       inEditorOffset: Int,
                                       presentation: SelectableInlayPresentation,
                                       panel: DialogPanel): Inlay<PresentationRenderer>? {
    val editor = templateState.editor
    val inlay = createNavigatableButton(templateState, inEditorOffset, presentation) ?: return null
    fun showPopup() {
      try {
        editor.putUserData(PopupFactoryImpl.ANCHOR_POPUP_POSITION, inlay.visualPosition)
        panel.border = DialogWrapper.createDefaultBorder()
        val popup = JBPopupFactory.getInstance()
          .createComponentPopupBuilder(panel, panel.preferredFocusedComponent)
          .setRequestFocus(true)
          .addListener(object : JBPopupListener {
            override fun onClosed(event: LightweightWindowEvent) {
              presentation.isSelected = false
            }
          })
          .createPopup()
        popup.showInBestPositionFor(editor)
      }
      finally {
        editor.putUserData(PopupFactoryImpl.ANCHOR_POPUP_POSITION, null)
      }
    }
    presentation.addSelectionListener(object : SelectableInlayPresentation.SelectionListener {
      override fun selectionChanged(isSelected: Boolean) {
        if (isSelected) showPopup()
      }
    })
    return inlay
  }

  @JvmStatic
  fun createSettingsPresentation(editor: EditorImpl, inlayToUpdate: AtomicReference<Inlay<PresentationRenderer>>): SelectableInlayPresentation {
    val factory = PresentationFactory(editor)
    fun button(background: Color?): InlayPresentation {
      val button = factory.container(
        presentation = factory.icon(AllIcons.Actions.InlayGear),
        padding = InlayPresentationFactory.Padding(4, 4, 4, 4),
        roundedCorners = InlayPresentationFactory.RoundedCorners(6, 6),
        background = background
      )
      return factory.container(button, padding = InlayPresentationFactory.Padding(3, 6, 0, 0))
    }

    val colorsScheme = editor.colorsScheme
    return SelectableInlayButton(
      editor,
      default = button(colorsScheme.getColor(DefaultLanguageHighlighterColors.INLINE_REFACTORING_SETTINGS_DEFAULT)),
      active = button(colorsScheme.getColor(DefaultLanguageHighlighterColors.INLINE_REFACTORING_SETTINGS_FOCUSED)),
      hovered = button(colorsScheme.getColor(DefaultLanguageHighlighterColors.INLINE_REFACTORING_SETTINGS_HOVERED)),
      inlayToUpdate = inlayToUpdate
    )
  }

  @JvmStatic
  fun createRenameSettingsInlay(templateState: TemplateState, offset: Int, elementToRename : PsiNamedElement, inlayReference : AtomicReference<Inlay<PresentationRenderer>>): Inlay<PresentationRenderer>? {
    val editor = templateState.editor as EditorImpl
    val processor = RenamePsiElementProcessor.forElement(elementToRename)

    val factory = PresentationFactory(editor)
    val colorsScheme = editor.colorsScheme
    fun button(bgKey: ColorKey, iconPresentation: IconPresentation, second : Boolean = false) = factory.container(factory.container(
      presentation = iconPresentation,
      padding = InlayPresentationFactory.Padding(if (second) 0 else 4, 4, 4, 4),
      background = colorsScheme.getColor(bgKey)
    ), padding = InlayPresentationFactory.Padding(if (second) 0 else 3, if (second) 6 else 0, 0, 0))

    var tooltip = "Rename also in comments and string literals"
    val inCommentsIconPresentation = factory.icon(if (processor.isToSearchInComments(elementToRename)) AllIcons.Actions.InlayRenameInCommentsActive else AllIcons.Actions.InlayRenameInComments)
    var defaultPresentation = button(DefaultLanguageHighlighterColors.INLINE_REFACTORING_SETTINGS_DEFAULT, inCommentsIconPresentation)
    var active = button(DefaultLanguageHighlighterColors.INLINE_REFACTORING_SETTINGS_FOCUSED, inCommentsIconPresentation)
    var hovered = button(DefaultLanguageHighlighterColors.INLINE_REFACTORING_SETTINGS_HOVERED, inCommentsIconPresentation)

    var inTextOccurrencesIconPresentation: IconPresentation? = null
    if (TextOccurrencesUtil.isSearchTextOccurrencesEnabled(elementToRename)) {
      inTextOccurrencesIconPresentation = factory.icon(if (processor.isToSearchForTextOccurrences(elementToRename)) AllIcons.Actions.InlayRenameInNoCodeFilesActive else AllIcons.Actions.InlayRenameInNoCodeFiles)
      defaultPresentation = factory.seq(defaultPresentation, button(DefaultLanguageHighlighterColors.INLINE_REFACTORING_SETTINGS_DEFAULT, inTextOccurrencesIconPresentation, true))
      active = factory.seq(active, button(DefaultLanguageHighlighterColors.INLINE_REFACTORING_SETTINGS_FOCUSED, inTextOccurrencesIconPresentation, true))
      hovered = factory.seq(hovered, button(DefaultLanguageHighlighterColors.INLINE_REFACTORING_SETTINGS_HOVERED, inTextOccurrencesIconPresentation, true))
      tooltip += " and in files that don’t contain explicit code references"
    }

    val presentation = SelectableInlayButton(editor, defaultPresentation, active, hovered, inlayReference)
    val panel = renamePanel(elementToRename, editor, inCommentsIconPresentation, inTextOccurrencesIconPresentation, inlayReference)
    val inlay = createNavigatableButtonWithPopup(templateState, offset, presentation, panel) ?: return null
    inlayReference.set(inlay)
    return inlay
  }

  private fun renamePanel(elementToRename: PsiElement,
                          editor: Editor,
                          searchInCommentsPresentation: IconPresentation,
                          searchForTextOccurrencesPresentation: IconPresentation?,
                          inlayReference: AtomicReference<Inlay<PresentationRenderer>>): DialogPanel {
    val processor = RenamePsiElementProcessor.forElement(elementToRename)
    return panel {
      row("Also rename in:") {
        row {
          cell {
            checkBox(RefactoringBundle.message("comments.and.strings"),
                     processor.isToSearchInComments(elementToRename),
                     actionListener = { _, cb ->  processor.setToSearchInComments(elementToRename, cb.isSelected)
                                                  searchInCommentsPresentation.icon = if (cb.isSelected) AllIcons.Actions.InlayRenameInCommentsActive else AllIcons.Actions.InlayRenameInComments
                                                  inlayReference.get()?.repaint()}
            ).focused()
            component(JLabel(AllIcons.Actions.InlayRenameInComments))
          }
        }
        if (TextOccurrencesUtil.isSearchTextOccurrencesEnabled(elementToRename)) {
          row {
            cell {
              checkBox(RefactoringBundle.message("text.occurrences"),
                       processor.isToSearchForTextOccurrences(elementToRename),
                       actionListener = { _, cb -> processor.setToSearchForTextOccurrences(elementToRename, cb.isSelected)
                                                   searchForTextOccurrencesPresentation?.icon = if (cb.isSelected) AllIcons.Actions.InlayRenameInNoCodeFilesActive else AllIcons.Actions.InlayRenameInNoCodeFiles
                                                   inlayReference.get()?.repaint()})
              component(JLabel(AllIcons.Actions.InlayRenameInNoCodeFiles))
            }
          }
        }
      }
      row {
        cell {
          val renameAction = ActionManager.getInstance().getAction(IdeActions.ACTION_RENAME)
          link("More options", null) {
            val event = AnActionEvent(null,
                                      DataManager.getInstance().getDataContext(editor.component),
                                      ActionPlaces.UNKNOWN, renameAction.templatePresentation.clone(),
                                      ActionManager.getInstance(), 0)
            if (ActionUtil.lastUpdateAndCheckDumb(renameAction, event, true)) {
              ActionUtil.performActionDumbAware(renameAction, event)
            }
          }
          comment(KeymapUtil.getFirstKeyboardShortcutText(renameAction))
        }
      }
    }
  }
}