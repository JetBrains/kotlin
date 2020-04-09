// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.lookup.impl.AsyncRendering;
import com.intellij.codeInsight.template.impl.LiveTemplateLookupElement;
import com.intellij.ide.ui.UISettings;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class CompletionLookupArrangerImpl extends BaseCompletionLookupArranger {
  private static final UISettings ourUISettings = UISettings.getInstance();

  public CompletionLookupArrangerImpl(CompletionProcessEx process) {
    super(process);
  }

  @Override
  public synchronized void addElement(@NotNull LookupElement element, @NotNull LookupElementPresentation presentation) {
    StatisticsWeigher.clearBaseStatisticsInfo(element);
    super.addElement(element, presentation);
  }

  @Override
  protected boolean isAlphaSorted() {
    return ourUISettings.getSortLookupElementsLexicographically();
  }

  @NotNull
  @Override
  protected List<LookupElement> getExactMatches(List<? extends LookupElement> items) {
    String selectedText =
      InjectedLanguageUtil.getTopLevelEditor(myProcess.getParameters().getEditor()).getSelectionModel().getSelectedText();
    List<LookupElement> exactMatches = new SmartList<>();
    for (int i = 0; i < items.size(); i++) {
      LookupElement item = items.get(i);
      boolean isSuddenLiveTemplate = isSuddenLiveTemplate(item);
      if (isPrefixItem(item, true) && !isSuddenLiveTemplate || item.getLookupString().equals(selectedText)) {
        if (item instanceof LiveTemplateLookupElement) {
          // prefer most recent live template lookup item
          return Collections.singletonList(item);
        }
        exactMatches.add(item);
      }
      else if (i == 0 && isSuddenLiveTemplate && items.size() > 1 && !CompletionService.isStartMatch(items.get(1), this)) {
        return Collections.singletonList(item);
      }
    }
    return exactMatches;
  }

  @Override
  protected void removeItem(@NotNull LookupElement element, @NotNull ProcessingContext context) {
    super.removeItem(element, context);
    AsyncRendering.cancelRendering(element);
  }

  private static boolean isSuddenLiveTemplate(LookupElement element) {
    return element instanceof LiveTemplateLookupElement && ((LiveTemplateLookupElement)element).sudden;
  }
}
