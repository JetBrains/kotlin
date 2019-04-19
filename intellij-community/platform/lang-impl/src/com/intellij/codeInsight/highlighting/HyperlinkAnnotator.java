// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.highlighting;

import com.intellij.ide.IdeBundle;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.paths.WebReference;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

public class HyperlinkAnnotator implements Annotator {

  private static final Key<String> messageKey = Key.create("hyperlink.message");

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (holder.isBatchMode()) return;
    for (PsiReference reference : element.getReferences()) {
      if (reference instanceof WebReference) {
        String message = holder.getCurrentAnnotationSession().getUserData(messageKey);
        if (message == null) {
          message = getMessage();
          holder.getCurrentAnnotationSession().putUserData(messageKey, message);
        }
        TextRange range = reference.getRangeInElement().shiftRight(element.getTextRange().getStartOffset());
        Annotation annotation = holder.createInfoAnnotation(range, message);
        annotation.setTextAttributes(CodeInsightColors.INACTIVE_HYPERLINK_ATTRIBUTES);
      }
    }
  }

  @NotNull
  private static String getMessage() {
    String message = IdeBundle.message("open.url.in.browser.tooltip");
    Shortcut[] shortcuts = KeymapManager.getInstance().getActiveKeymap().getShortcuts(IdeActions.ACTION_GOTO_DECLARATION);
    String shortcutText = "";
    Shortcut mouseShortcut = ContainerUtil.find(shortcuts, shortcut -> !shortcut.isKeyboard());
    if (mouseShortcut != null) {
      shortcutText += KeymapUtil.getShortcutText(mouseShortcut);
      shortcutText = shortcutText.replace("Button1 ", "");
    }
    Shortcut keyboardShortcut = ContainerUtil.find(shortcuts, shortcut -> shortcut.isKeyboard());
    if (keyboardShortcut != null) {
      if (!shortcutText.isEmpty()) shortcutText += ", ";
      shortcutText += KeymapUtil.getShortcutText(keyboardShortcut);
    }
    if (!shortcutText.isEmpty()) {
      message += " (" + shortcutText + ")";
    }
    return message;
  }
}
