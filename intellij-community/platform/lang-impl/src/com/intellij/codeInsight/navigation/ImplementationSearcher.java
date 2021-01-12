// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.search.PsiElementProcessorAdapter;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.DefinitionsScopedSearch;
import com.intellij.util.ArrayUtil;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Query;
import org.jetbrains.annotations.Nullable;

public class ImplementationSearcher {
  public PsiElement @Nullable [] searchImplementations(Editor editor, PsiElement element, int offset) {
    TargetElementUtil targetElementUtil = TargetElementUtil.getInstance();
    boolean onRef = ReadAction.compute(() -> targetElementUtil.findTargetElement(editor, getFlags() & ~(TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED | TargetElementUtil.LOOKUP_ITEM_ACCEPTED), offset) == null);
    return searchImplementations(element, editor, onRef && ReadAction.compute(() -> element == null || targetElementUtil.includeSelfInGotoImplementation(element)), onRef);
  }

  public PsiElement @Nullable [] searchImplementations(PsiElement element,
                                                       Editor editor,
                                                       boolean includeSelfAlways,
                                                       boolean includeSelfIfNoOthers) {
    if (element == null) return PsiElement.EMPTY_ARRAY;
    PsiElement[] elements = searchDefinitions(element, editor);
    if (elements == null) return null; //the search has been cancelled
    if (elements.length > 0) return filterElements(element, includeSelfAlways ? ArrayUtil.prepend(element, elements) : elements);
    if (includeSelfAlways || includeSelfIfNoOthers) return new PsiElement[]{element};
    return PsiElement.EMPTY_ARRAY;
  }

  protected static SearchScope getSearchScope(PsiElement element, Editor editor) {
    return ReadAction.compute(() -> TargetElementUtil.getInstance().getSearchScope(editor, element));
  }

  protected PsiElement @Nullable("For the case the search has been cancelled") [] searchDefinitions(PsiElement element, Editor editor) {
    Ref<PsiElement[]> result = Ref.create();
    if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(
      () -> result.set(search(element, editor).toArray(PsiElement.EMPTY_ARRAY)),
      getSearchingForImplementations(), true, element.getProject())) {
      return null;
    }
    return result.get();
  }

  protected Query<PsiElement> search(PsiElement element, Editor editor) {
    return DefinitionsScopedSearch.search(element, getSearchScope(element, editor), isSearchDeep());
  }

  protected boolean isSearchDeep() {
    return true;
  }

  protected PsiElement[] filterElements(PsiElement element, PsiElement[] targetElements) {
    return targetElements;
  }

  public static int getFlags() {
    return TargetElementUtil.getInstance().getDefinitionSearchFlags();
  }

  public static class FirstImplementationsSearcher extends ImplementationSearcher {
    @Override
    protected PsiElement[] searchDefinitions(PsiElement element, Editor editor) {
      if (canShowPopupWithOneItem(element)) {
        return new PsiElement[]{element};
      }

      PsiElementProcessor.FindElement<PsiElement> collectProcessor = new PsiElementProcessor.FindElement<>();
      if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
        @Override
        public void run() {
          search(element, editor).forEach(new PsiElementProcessorAdapter<PsiElement>(collectProcessor){
            @Override
            public boolean processInReadAction(PsiElement element) {
              return !accept(element) || super.processInReadAction(element);
            }
          });
        }
      }, getSearchingForImplementations(), true, element.getProject())) {
        return null;
      }
      PsiElement foundElement = collectProcessor.getFoundElement();
      return foundElement != null ? new PsiElement[] {foundElement} : PsiElement.EMPTY_ARRAY;
    }

    protected boolean canShowPopupWithOneItem(PsiElement element) {
      return accept(element);
    }

    protected boolean accept(PsiElement element) {
      return true;
    }
  }

  public abstract static class BackgroundableImplementationSearcher extends ImplementationSearcher {
    @Override
    protected PsiElement[] searchDefinitions(PsiElement element, Editor editor) {
      CommonProcessors.CollectProcessor<PsiElement> processor = new CommonProcessors.CollectProcessor<PsiElement>() {
        @Override
        public boolean process(PsiElement element) {
          processElement(element);
          return super.process(element);
        }
      };
      search(element, editor).forEach(processor);
      return processor.toArray(PsiElement.EMPTY_ARRAY);
    }

    protected abstract void processElement(PsiElement element);
  }

  public static String getSearchingForImplementations() {
    return CodeInsightBundle.message("searching.for.implementations");
  }
}