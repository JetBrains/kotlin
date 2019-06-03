// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:Experimental

package com.intellij.navigation

import com.intellij.ide.ui.createTargetPresentationRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.util.Consumer
import org.jetbrains.annotations.ApiStatus.Experimental

fun chooseTarget(editor: Editor, title: String, targets: List<NavigationTarget>, handler: (NavigationTarget) -> Unit) {
  chooseTarget(editor, title, targets, Consumer(handler))
}

fun chooseTarget(editor: Editor, title: String, targets: List<NavigationTarget>, consumer: Consumer<NavigationTarget>) {
  targets.singleOrNull()?.let {
    consumer.consume(it)
    return
  }
  val renderer = createTargetPresentationRenderer(NavigationTarget::getPresentationIfValid)
  return JBPopupFactory.getInstance()
    .createPopupChooserBuilder(targets)
    .setRenderer(renderer)
    .setNamerForFiltering(renderer::getItemSearchString)
    .setFont(EditorUtil.getEditorFont())
    .setTitle(title)
    .setItemChosenCallback(consumer)
    .withHintUpdateSupply()
    .createPopup()
    .showInBestPositionFor(editor)
}

private fun NavigationTarget.getPresentationIfValid(): TargetPresentation? {
  return if (isValid) targetPresentation else null
}
