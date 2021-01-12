// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.highlighting;

import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.ide.IdeBundle;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.model.psi.PsiSymbolReferenceService;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.paths.WebReference;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public class HyperlinkAnnotator implements Annotator {

  private static final Key<String> messageKey = Key.create("hyperlink.message");

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (holder.isBatchMode()) return;
    for (PsiHighlightedReference reference : PsiSymbolReferenceService.getService().getReferences(element, PsiHighlightedReference.class)) {
      TextRange range = reference.getAbsoluteRange();
      if (range.isEmpty()) {
        continue;
      }
      String message = reference.highlightMessage();
      AnnotationBuilder annotationBuilder = message == null ? holder.newSilentAnnotation(reference.highlightSeverity())
                                                            : holder.newAnnotation(reference.highlightSeverity(), message);
      reference.highlightReference(annotationBuilder.range(range)).create();
    }
    if (WebReference.isWebReferenceWorthy(element)) {
      for (PsiReference reference : element.getReferences()) {
        if (reference instanceof WebReference) {
          String message = holder.getCurrentAnnotationSession().getUserData(messageKey);
          if (message == null) {
            message = getMessage();
            holder.getCurrentAnnotationSession().putUserData(messageKey, message);
          }
          TextRange range = reference.getRangeInElement().shiftRight(element.getTextRange().getStartOffset());
          holder.newAnnotation(HighlightSeverity.INFORMATION, message)
            .range(range)
            .textAttributes(CodeInsightColors.INACTIVE_HYPERLINK_ATTRIBUTES)
            .create();
        }
        else if (reference instanceof HighlightedReference) {
          if (reference.isSoft()) continue;

          TextRange rangeInElement = reference.getRangeInElement();
          if (rangeInElement.isEmpty()) continue;

          TextRange range = rangeInElement.shiftRight(element.getTextRange().getStartOffset());
          holder.newSilentAnnotation(HighlightInfoType.HIGHLIGHTED_REFERENCE_SEVERITY)
            .range(range)
            .textAttributes(DefaultLanguageHighlighterColors.HIGHLIGHTED_REFERENCE)
            .create();
        }
      }
    }
  }

  @NotNull
  @ApiStatus.Internal
  public static String getMessage() {
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
