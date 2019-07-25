// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.editorActions.wordSelection;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiFileReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

public class InjectedReferenceSelectioner extends AbstractWordSelectioner {
  @Override
  public boolean canSelect(@NotNull PsiElement e) {
    return PsiTreeUtil.getParentOfType(e, PsiLanguageInjectionHost.class) != null;
  }

  @Override
  public List<TextRange> select(@NotNull PsiElement e, @NotNull CharSequence editorText, final int cursorOffset, @NotNull Editor editor) {
    PsiElement host = PsiTreeUtil.getParentOfType(e, PsiLanguageInjectionHost.class);
    if (host == null) return Collections.emptyList();

    ArrayList<TextRange> ranges = JBIterable.of(host.getReferences())
      .filter(PsiFileReference.class)
      .map(r -> r.getRangeInElement().shiftRight(r.getElement().getTextRange().getStartOffset()))
      .filter(r -> r.getStartOffset() <= cursorOffset)
      .addAllTo(new ArrayList<>());
    if (ranges.isEmpty()) return Collections.emptyList();

    TextRange smallest = null;
    for (TextRange r : ranges) {
      if (!r.containsOffset(cursorOffset)) continue;
      if (smallest == null || r.getLength() < smallest.getLength()) {
        smallest = r;
      }
    }
    if (smallest == null) return Collections.emptyList();
    int endOffset = smallest.getEndOffset();

    for (ListIterator<TextRange> it = ranges.listIterator(); it.hasNext(); ) {
      TextRange r = it.next();
      if (r.getEndOffset() > cursorOffset) continue;
      it.set(TextRange.create(r.getStartOffset(), endOffset));
    }

    return ranges;
  }

}
