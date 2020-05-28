// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.rename.inplace

import com.intellij.codeInsight.hints.presentation.PresentationRenderer
import com.intellij.codeInsight.template.impl.TemplateState
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.Disposer
import com.intellij.ui.popup.PopupFactoryImpl
import javax.swing.JComponent

class TemplateInlayBuilder {

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

  fun createNavigatableButtonWithPopup(templateState: TemplateState,
                                       inEditorOffset: Int,
                                       presentation: SelectableInlayPresentation,
                                       panel: JComponent): Inlay<PresentationRenderer>? {
    val editor = templateState.editor
    val inlay = createNavigatableButton(templateState, inEditorOffset, presentation) ?: return null
    fun showPopup(){
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
      editor.putUserData(PopupFactoryImpl.ANCHOR_POPUP_POSITION, null)
    }
    presentation.addSelectionListener(object : SelectableInlayPresentation.SelectionListener {
      override fun selectionChanged(isSelected: Boolean) {
        if (isSelected) showPopup()
      }
    })
    return inlay
  }
}