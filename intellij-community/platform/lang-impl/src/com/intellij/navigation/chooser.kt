// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:Experimental

package com.intellij.navigation

import com.intellij.ide.ui.createTargetPresentationRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import org.jetbrains.annotations.ApiStatus.Experimental
import java.util.function.Consumer

fun chooseTarget(editor: Editor, title: String, targets: List<NavigationTarget>, handler: (NavigationTarget) -> Unit) {
  chooseTarget(editor, title, targets, Consumer(handler))
}

fun chooseTarget(editor: Editor, title: String, targets: List<NavigationTarget>, consumer: Consumer<in NavigationTarget>) {
  targets.singleOrNull()?.let {
    consumer.accept(it)
    return
  }
  chooseTargetPopup(title, targets, NavigationTarget::getPresentationIfValid, consumer).showInBestPositionFor(editor)
}

fun <T> chooseTargetPopup(title: String,
                          targets: List<T>,
                          presentation: (T) -> TargetPopupPresentation?,
                          consumer: Consumer<in T>): JBPopup {
  val renderer = createTargetPresentationRenderer(presentation)
  return JBPopupFactory.getInstance()
    .createPopupChooserBuilder(targets)
    .setRenderer(renderer)
    .setNamerForFiltering(renderer::getItemSearchString)
    .setFont(EditorUtil.getEditorFont())
    .setTitle(title)
    .setItemChosenCallback(com.intellij.util.Consumer(consumer::accept))
    .withHintUpdateSupply()
    .createPopup()
}

private fun NavigationTarget.getPresentationIfValid(): TargetPopupPresentation? {
  return if (isValid) targetPresentation else null
}
