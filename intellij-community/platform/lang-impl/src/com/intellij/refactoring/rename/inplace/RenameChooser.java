/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.refactoring.rename.inplace;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.ui.popup.JBPopupAdapter;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.refactoring.RefactoringSettings;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

abstract class RenameChooser {
  @NonNls private static final String CODE_OCCURRENCES = "Rename code occurrences";
  @NonNls private static final String ALL_OCCURRENCES = "Rename all occurrences";
  private final Set<RangeHighlighter> myRangeHighlighters = new HashSet<>();
  private final Editor myEditor;
  private final TextAttributes myAttributes;

  RenameChooser(Editor editor) {
    myEditor = editor;
    myAttributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
  }

  protected abstract void runRenameTemplate(Collection<Pair<PsiElement, TextRange>> stringUsages);

  public void showChooser(final Collection<? extends PsiReference> refs,
                          final Collection<Pair<PsiElement, TextRange>> stringUsages) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      runRenameTemplate(
        RefactoringSettings.getInstance().RENAME_SEARCH_IN_COMMENTS_FOR_FILE ? stringUsages : new ArrayList<>());
      return;
    }



    JBPopupFactory.getInstance().createPopupChooserBuilder(ContainerUtil.newArrayList(CODE_OCCURRENCES, ALL_OCCURRENCES))
      .setItemSelectedCallback(selectedValue -> {
        if (selectedValue == null) return;
        dropHighlighters();
        final MarkupModel markupModel = myEditor.getMarkupModel();

        if (selectedValue.equals(ALL_OCCURRENCES)) {
          for (Pair<PsiElement, TextRange> pair : stringUsages) {
            final TextRange textRange = pair.second.shiftRight(pair.first.getTextOffset());
            final RangeHighlighter rangeHighlighter = markupModel.addRangeHighlighter(
              textRange.getStartOffset(), textRange.getEndOffset(), HighlighterLayer.SELECTION - 1, myAttributes,
              HighlighterTargetArea.EXACT_RANGE);
            myRangeHighlighters.add(rangeHighlighter);
          }
        }

        for (PsiReference reference : refs) {
          final PsiElement element = reference.getElement();
          if (element == null) continue;
          final TextRange textRange = element.getTextRange();
          final RangeHighlighter rangeHighlighter = markupModel.addRangeHighlighter(
            textRange.getStartOffset(), textRange.getEndOffset(), HighlighterLayer.SELECTION - 1, myAttributes,
            HighlighterTargetArea.EXACT_RANGE);
          myRangeHighlighters.add(rangeHighlighter);
        }
      })
      .setTitle("String occurrences found")
      .setMovable(false)
      .setResizable(false)
      .setRequestFocus(true)
      .setItemChosenCallback((selectedValue) -> runRenameTemplate(ALL_OCCURRENCES.equals(selectedValue) ? stringUsages : new ArrayList<>()))
      .addListener(new JBPopupAdapter() {
        @Override
        public void onClosed(@NotNull LightweightWindowEvent event) {
          dropHighlighters();
        }
      })
      .createPopup().showInBestPositionFor(myEditor);
  }



  private void dropHighlighters() {
    for (RangeHighlighter highlight : myRangeHighlighters) {
      highlight.dispose();
    }
    myRangeHighlighters.clear();
  }
}
