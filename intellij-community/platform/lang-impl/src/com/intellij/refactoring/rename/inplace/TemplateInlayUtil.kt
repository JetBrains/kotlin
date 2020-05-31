// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.rename.inplace

import com.intellij.codeInsight.hints.InlayPresentationFactory
import com.intellij.codeInsight.hints.presentation.DynamicDelegatePresentation
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.codeInsight.hints.presentation.PresentationRenderer
import com.intellij.codeInsight.template.impl.TemplateState
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.refactoring.util.TextOccurrencesUtil
import com.intellij.ui.layout.*
import com.intellij.ui.popup.PopupFactoryImpl
import java.awt.Color
import java.awt.Point
import java.awt.event.MouseEvent
import java.util.concurrent.atomic.AtomicReference
import javax.swing.Icon
import javax.swing.JLabel

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
  fun createSettingsPanelPresentation(editor: EditorImpl,
                                      elementToRename : PsiElement,
                                      inlayToUpdate: AtomicReference<Inlay<PresentationRenderer>>): SelectableInlayPresentation {
    val processor = RenamePsiElementProcessor.forElement(elementToRename)
    val factory = PresentationFactory(editor)
    fun button(background: Color?, icon : Icon): InlayPresentation {
      val button = factory.container(
        presentation = factory.icon(icon),
        padding = InlayPresentationFactory.Padding(0, 0, 4, 4),
        roundedCorners = InlayPresentationFactory.RoundedCorners(6, 6),
        background = background
      )
      return factory.container(button, padding = InlayPresentationFactory.Padding(3, 0, 0, 0), background = background)
    }

    val colorsScheme = editor.colorsScheme
    val renameInComments = SelectableInlayButton(
      editor,
      default = button(colorsScheme.getColor(DefaultLanguageHighlighterColors.INLINE_REFACTORING_SETTINGS_DEFAULT),
                       AllIcons.Actions.InlayRenameInComments),
      active = button(colorsScheme.getColor(DefaultLanguageHighlighterColors.INLINE_REFACTORING_SETTINGS_FOCUSED),
                      AllIcons.Actions.InlayRenameInCommentsActive),
      hovered = button(colorsScheme.getColor(DefaultLanguageHighlighterColors.INLINE_REFACTORING_SETTINGS_HOVERED), AllIcons.Actions.InlayRenameInComments),
      inlayToUpdate = inlayToUpdate
    )
    renameInComments.isSelected = processor.isToSearchInComments(elementToRename)
    renameInComments.addSelectionListener(object : SelectableInlayPresentation.SelectionListener {
      override fun selectionChanged(isSelected: Boolean) {
        processor.setToSearchInComments(elementToRename, isSelected)
      }
    })

    var renameInNonCode : SelectableInlayButton? = null
    if (TextOccurrencesUtil.isSearchTextOccurrencesEnabled(elementToRename)) {

      renameInNonCode = SelectableInlayButton(
        editor,
        default = button(colorsScheme.getColor(DefaultLanguageHighlighterColors.INLINE_REFACTORING_SETTINGS_DEFAULT),
                         AllIcons.Actions.InlayRenameInNoCodeFiles),
        active = button(colorsScheme.getColor(DefaultLanguageHighlighterColors.INLINE_REFACTORING_SETTINGS_FOCUSED),
                        AllIcons.Actions.InlayRenameInNoCodeFilesActive),
        hovered = button(colorsScheme.getColor(DefaultLanguageHighlighterColors.INLINE_REFACTORING_SETTINGS_HOVERED),
                         AllIcons.Actions.InlayRenameInNoCodeFiles),
        inlayToUpdate = inlayToUpdate
      )

      renameInNonCode.isSelected = processor.isToSearchForTextOccurrences(elementToRename)
      renameInNonCode.addSelectionListener(object : SelectableInlayPresentation.SelectionListener {
        override fun selectionChanged(isSelected: Boolean) {
          processor.setToSearchForTextOccurrences(elementToRename, isSelected)
        }
      })
    }

    val renameInCommentsWithTooltipPresentation = factory.withTooltip("Rename also in comments and string literals", renameInComments)
    val presentation = if (renameInNonCode == null)
      renameInCommentsWithTooltipPresentation
    else factory.seq(renameInCommentsWithTooltipPresentation,
                     factory.withTooltip("Rename also in files that don’t contain explicit code references", renameInNonCode))

    class Selectable : DynamicDelegatePresentation(presentation), SelectableInlayPresentation {
      private val selectionListeners: MutableList<SelectableInlayPresentation.SelectionListener> = mutableListOf()
      override var isSelected = false
        set(value)  {
          field = value
          selectionListeners.forEach { it.selectionChanged(value) }
        }

      override fun addSelectionListener(listener: SelectableInlayPresentation.SelectionListener) {
        selectionListeners.add(listener)
      }

      override fun mouseClicked(event: MouseEvent, translated: Point) {
        if (translated.x < renameInComments.width) {
          renameInComments.isSelected = !renameInComments.isSelected
        }
        else if (renameInNonCode != null) {
          renameInNonCode.isSelected = !renameInNonCode.isSelected
        }
      }
    }
    return Selectable()
  }

  @JvmStatic
  fun renamePanel( elementToRename : PsiElement): DialogPanel {
    val processor = RenamePsiElementProcessor.forElement(elementToRename)
    return panel {
      row("Also rename in:") {
        row {
          cell {
            checkBox(RefactoringBundle.message("comments.and.strings"),
                     processor.isToSearchInComments(elementToRename),
                     actionListener = { _, cb ->  processor.setToSearchInComments(elementToRename, cb.isSelected)}
            ).focused()
            component(JLabel(AllIcons.Actions.InlayRenameInComments))
          }
        }
        if (TextOccurrencesUtil.isSearchTextOccurrencesEnabled(elementToRename)) {
          row {
            cell {
              checkBox(RefactoringBundle.message("text.occurrences"),
                       processor.isToSearchForTextOccurrences(elementToRename),
                       actionListener = { _, cb -> processor.setToSearchForTextOccurrences(elementToRename, cb.isSelected)})
              component(JLabel(AllIcons.Actions.InlayRenameInNoCodeFiles))
            }
          }
        }
      }
      row {
        link("More options", null) { }
        comment(KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_RENAME)))
      }
    }
  }
}