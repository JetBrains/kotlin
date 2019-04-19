// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.rename.inplace;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Result;
import com.intellij.codeInsight.template.TextResult;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.codeStyle.SuggestedNameInfo;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.refactoring.rename.NameSuggestionProvider;
import com.intellij.refactoring.rename.PreferrableNameSuggestionProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.LinkedHashSet;

public class MyLookupExpression extends Expression {
  protected final String myName;
  protected final LookupElement[] myLookupItems;
  private final String myAdvertisementText;

  public MyLookupExpression(final String name,
                            final LinkedHashSet<String> names,
                            PsiNamedElement elementToRename,
                            final PsiElement nameSuggestionContext,
                            final boolean shouldSelectAll,
                            final String advertisement) {
    myName = name;
    myAdvertisementText = advertisement;
    myLookupItems = initLookupItems(names, elementToRename, nameSuggestionContext, shouldSelectAll);
  }

  private static LookupElement[] initLookupItems(LinkedHashSet<String> names,
                                                 PsiNamedElement elementToRename,
                                                 PsiElement nameSuggestionContext,
                                                 final boolean shouldSelectAll) {
    if (names == null) {
      names = new LinkedHashSet<>();
      for (NameSuggestionProvider provider : NameSuggestionProvider.EP_NAME.getExtensionList()) {
        final SuggestedNameInfo suggestedNameInfo = provider.getSuggestedNames(elementToRename, nameSuggestionContext, names);
        if (suggestedNameInfo != null &&
            provider instanceof PreferrableNameSuggestionProvider &&
            !((PreferrableNameSuggestionProvider)provider).shouldCheckOthers()) {
          break;
        }
      }
    }
    final LookupElement[] lookupElements = new LookupElement[names.size()];
    final Iterator<String> iterator = names.iterator();
    for (int i = 0; i < lookupElements.length; i++) {
      final String suggestion = iterator.next();
      lookupElements[i] = LookupElementBuilder.create(suggestion).withInsertHandler(new InsertHandler<LookupElement>() {
        @Override
        public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
          if (shouldSelectAll) return;
          final Editor topLevelEditor = InjectedLanguageUtil.getTopLevelEditor(context.getEditor());
          final TemplateState templateState = TemplateManagerImpl.getTemplateState(topLevelEditor);
          if (templateState != null) {
            final TextRange range = templateState.getCurrentVariableRange();
            if (range != null) {
              topLevelEditor.getDocument().replaceString(range.getStartOffset(), range.getEndOffset(), suggestion);
            }
          }
        }
      });
    }
    return lookupElements;
  }

  @Override
  public LookupElement[] calculateLookupItems(ExpressionContext context) {
    return myLookupItems;
  }

  @Override
  public Result calculateQuickResult(ExpressionContext context) {
    return calculateResult(context);
  }

  @Override
  public Result calculateResult(ExpressionContext context) {
    TemplateState templateState = TemplateManagerImpl.getTemplateState(context.getEditor());
    final TextResult insertedValue = templateState != null ? templateState.getVariableValue(InplaceRefactoring.PRIMARY_VARIABLE_NAME) : null;
    if (insertedValue != null) {
      if (!insertedValue.getText().isEmpty()) return insertedValue;
    }
    return new TextResult(myName);
  }

  @Override
  public String getAdvertisingText() {
    return myAdvertisementText;
  }
}
