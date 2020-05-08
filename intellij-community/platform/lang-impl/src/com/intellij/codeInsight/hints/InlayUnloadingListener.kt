// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.util.Disposer

class InlayUnloadingListener : DynamicPluginListener {
  override fun beforePluginUnload(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
    for (editor in EditorFactory.getInstance().allEditors) {
      val document = editor.document
      closeInlays(editor.inlayModel.getInlineElementsInRange(0, document.textLength - 1))
      closeInlays(editor.inlayModel.getBlockElementsInRange(0, document.textLength - 1))
      closeInlays(editor.inlayModel.getAfterLineEndElementsInRange(0, document.textLength - 1))
    }
  }

  private fun closeInlays(inlineElements: List<Inlay<*>>) {
    for (inlay in inlineElements) {
      if (inlay.renderer !is LinearOrderInlayRenderer<*>) return
      Disposer.dispose(inlay)
    }
  }
}