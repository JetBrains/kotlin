/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.codeInsight.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.paths.PsiDynaReference;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ReferenceRange;
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference;
import com.intellij.util.PairConsumer;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author peter
 */
public class LegacyCompletionContributor extends CompletionContributor {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.completion.LegacyCompletionContributor");

  @Override
  public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet _result) {
    if (parameters.getCompletionType() != CompletionType.BASIC) {
      return;
    }
    CompletionData completionData = getCompletionData(parameters);
    if (completionData == null) return;

    final PsiElement insertedElement = parameters.getPosition();
    final CompletionResultSet result = _result.withPrefixMatcher(completionData.findPrefix(insertedElement, parameters.getOffset()));

    completeReference(parameters, result);

    final Set<LookupElement> lookupSet = new LinkedHashSet<>();
    final Set<CompletionVariant> keywordVariants = new HashSet<>();
    PsiFile file = parameters.getOriginalFile();
    completionData.addKeywordVariants(keywordVariants, insertedElement, file);
    completionData.completeKeywordsBySet(lookupSet, keywordVariants);
    result.addAllElements(lookupSet);
  }

  public static boolean completeReference(final CompletionParameters parameters, final CompletionResultSet result) {
    final CompletionData completionData = getCompletionData(parameters);
    if (completionData == null) {
      return false;
    }

    final Ref<Boolean> hasVariants = Ref.create(false);
    processReferences(parameters, result, (reference, resultSet) -> {
      final Set<LookupElement> lookupSet = new LinkedHashSet<>();
      completionData.completeReference(reference, lookupSet, parameters.getPosition(), parameters.getOriginalFile());
      for (final LookupElement item : lookupSet) {
        if (resultSet.getPrefixMatcher().prefixMatches(item)) {
          if (!item.isValid()) {
            LOG.error(completionData + " has returned an invalid lookup element " + item + " of " + item.getClass() +
                      " in " + parameters.getOriginalFile() + " of " + parameters.getOriginalFile().getClass() +
                      "; reference=" + reference + " of " + reference.getClass());
          }
          hasVariants.set(true);
          resultSet.addElement(item);
        }
      }
    });
    return hasVariants.get().booleanValue();
  }

  private static CompletionData getCompletionData(CompletionParameters parameters) {
    final PsiElement position = parameters.getPosition();
    return CompletionUtil.getCompletionDataByElement(position, parameters.getOriginalFile());
  }

  public static void processReferences(final CompletionParameters parameters,
                                       final CompletionResultSet result,
                                       final PairConsumer<PsiReference, CompletionResultSet> consumer) {
    final int startOffset = parameters.getOffset();
    final PsiReference ref = parameters.getPosition().getContainingFile().findReferenceAt(startOffset);
    if (ref instanceof PsiMultiReference) {
      for (final PsiReference reference : CompletionData.getReferences((PsiMultiReference)ref)) {
        processReference(result, startOffset, consumer, reference);
      }
    }
    else if (ref instanceof PsiDynaReference) {
      for (final PsiReference reference : ((PsiDynaReference<?>)ref).getReferences()) {
        processReference(result, startOffset, consumer, reference);
      }
    }
    else if (ref != null) {
      processReference(result, startOffset, consumer, ref);
    }
  }

  private static void processReference(final CompletionResultSet result,
                                       final int startOffset,
                                       final PairConsumer<PsiReference, CompletionResultSet> consumer,
                                       final PsiReference reference) {
    PsiElement element = reference.getElement();
    final int offsetInElement = startOffset - element.getTextRange().getStartOffset();
    if (!ReferenceRange.containsOffsetInElement(reference, offsetInElement)) {
      return;
    }

    TextRange range = reference.getRangeInElement();
    try {
      final String prefix = element.getText().substring(range.getStartOffset(), offsetInElement);
      consumer.consume(reference, result.withPrefixMatcher(prefix));
    }
    catch (StringIndexOutOfBoundsException e) {
      LOG.error("Reference=" + reference +
                "; element=" + element + " of " + element.getClass() +
                "; range=" + range +
                "; offset=" + offsetInElement,
                e);
    }
  }


}
