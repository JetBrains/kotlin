// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.rename.inplace

import com.intellij.codeInsight.hints.InlayPresentationFactory
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.codeInsight.hints.presentation.PresentationRenderer
import com.intellij.codeInsight.template.impl.TemplateState
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.popup.PopupFactoryImpl
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JComponent

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
                                       panel: JComponent): Inlay<PresentationRenderer>? {
    val editor = templateState.editor
    val inlay = createNavigatableButton(templateState, inEditorOffset, presentation) ?: return null
    fun showPopup() {
      try {
        editor.putUserData(PopupFactoryImpl.ANCHOR_POPUP_POSITION, inlay.visualPosition)
        val popup = JBPopupFactory.getInstance()
          .createComponentPopupBuilder(panel, null)
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

    val roundedCorners = InlayPresentationFactory.RoundedCorners(6, 6)
    val padding = InlayPresentationFactory.Padding(4, 4, 4, 4)

    val inactiveIcon = factory.container(
      presentation = factory.icon(AllIcons.Actions.InlayGear),
      padding = padding,
      roundedCorners = roundedCorners,
      background = JBColor.LIGHT_GRAY
    )
    val activeIcon = factory.container(
      presentation = factory.icon(AllIcons.Actions.InlayGear),
      padding = padding,
      roundedCorners = roundedCorners,
      background = JBColor.DARK_GRAY
    )
    val inactivePadded = factory.container(inactiveIcon, padding = InlayPresentationFactory.Padding(3, 6, 0, 0))
    val activePadded = factory.container(activeIcon, padding = InlayPresentationFactory.Padding(3, 6, 0, 0))

    return SelectableInlayButton(editor, inactivePadded, activePadded, activePadded, inlayToUpdate)
  }
}