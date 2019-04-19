/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.codeInsight.editorActions.wordSelection;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiFileReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
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
      .addAllTo(ContainerUtil.newArrayList());
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
