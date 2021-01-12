// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.rename.inplace

import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.codeInsight.template.impl.TemplateState
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorAction
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Key
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
interface VirtualTemplateElement {

  companion object {
    private val VIRTUAL_TEMPLATE_ELEMENT: Key<VirtualTemplateElement> = Key.create("virtual_template_element")

    @JvmStatic
    fun installOnTemplate(templateState: TemplateState, element: VirtualTemplateElement){
      templateState.properties[VIRTUAL_TEMPLATE_ELEMENT] = element
    }

    @JvmStatic
    fun findInTemplate(templateState: TemplateState): VirtualTemplateElement? {
      return templateState.properties[VIRTUAL_TEMPLATE_ELEMENT] as? VirtualTemplateElement
    }
  }

  fun onSelect(templateState: TemplateState)
}

class SelectVirtualTemplateElement : EditorAction(Handler()) {
  private class Handler : EditorActionHandler() {
    override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
      val templateState = TemplateManagerImpl.getTemplateState(editor) ?: return
      VirtualTemplateElement.findInTemplate(templateState)?.onSelect(templateState)
    }

    override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext): Boolean {
      val templateState = TemplateManagerImpl.getTemplateState(editor) ?: return false
      return templateState.isLastVariable && VirtualTemplateElement.findInTemplate(templateState) != null
    }
  }
}