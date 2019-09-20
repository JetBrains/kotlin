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

package com.intellij.codeInsight.highlighting;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
public abstract class HighlightUsagesHandlerBase<T extends PsiElement> {
  @NotNull protected final Editor myEditor;
  @NotNull protected final PsiFile myFile;

  protected List<TextRange> myReadUsages = new ArrayList<>();
  protected List<TextRange> myWriteUsages = new ArrayList<>();
  protected String myStatusText;
  protected String myHintText;

  protected HighlightUsagesHandlerBase(@NotNull Editor editor, @NotNull PsiFile file) {
    myEditor = editor;
    myFile = file;
  }

  public void highlightUsages() {
    List<T> targets = getTargets();
    if (targets == null) {
      return;
    }
    selectTargets(targets, targets1 -> {
      computeUsages(targets1);
      performHighlighting();
    });
  }

  private void performHighlighting() {
    boolean clearHighlights = HighlightUsagesHandler.isClearHighlights(myEditor);
    EditorColorsManager manager = EditorColorsManager.getInstance();
    TextAttributes attributes = manager.getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
    TextAttributes writeAttributes = manager.getGlobalScheme().getAttributes(EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES);
    HighlightUsagesHandler.highlightRanges(HighlightManager.getInstance(myEditor.getProject()),
                                           myEditor, attributes, clearHighlights, myReadUsages);
    HighlightUsagesHandler.highlightRanges(HighlightManager.getInstance(myEditor.getProject()),
                                           myEditor, writeAttributes, clearHighlights, myWriteUsages);
    if (!clearHighlights) {
      WindowManager.getInstance().getStatusBar(myFile.getProject()).setInfo(myStatusText);

      HighlightHandlerBase.setupFindModel(myFile.getProject()); // enable f3 navigation
    }
    if (myHintText != null) {
      HintManager.getInstance().showInformationHint(myEditor, myHintText);
    }
  }

  protected void buildStatusText(@Nullable String elementName, int refCount) {
    if (refCount > 0) {
      myStatusText = CodeInsightBundle.message(elementName != null ?
                                        "status.bar.highlighted.usages.message" :
                                        "status.bar.highlighted.usages.no.target.message", refCount, elementName,
                                               HighlightUsagesHandler.getShortcutText());
    }
    else {
      myHintText = CodeInsightBundle.message(elementName != null ?
                                          "status.bar.highlighted.usages.not.found.message" :
                                          "status.bar.highlighted.usages.not.found.no.target.message", elementName);
    }
  }

  public abstract List<T> getTargets();

  @Nullable
  public String getFeatureId() {
    return null;
  }

  protected abstract void selectTargets(List<T> targets, Consumer<List<T>> selectionConsumer);

  public abstract void computeUsages(List<T> targets);

  protected void addOccurrence(@NotNull PsiElement element) {
    TextRange range = element.getTextRange();
    if (range != null) {
      range = InjectedLanguageManager.getInstance(element.getProject()).injectedToHost(element, range);
      myReadUsages.add(range);
    }
  }

  public List<TextRange> getReadUsages() {
    return myReadUsages;
  }

  public List<TextRange> getWriteUsages() {
    return myWriteUsages;
  }

  /**
   * In case of egoistic handler (highlightReferences = false) IdentifierHighlighterPass applies information only from this particular handler.
   * Otherwise additional information would be collected from reference search as well. 
   */
  public boolean highlightReferences() {
    return false;
  }
}
