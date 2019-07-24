/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.util.textCompletion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.CharFilter;
import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * @author sergey.evdokimov
 */
public class TextCompletionContributor extends CompletionContributor implements DumbAware {

  @Override
  public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
    PsiFile file = parameters.getOriginalFile();

    TextCompletionProvider provider = TextCompletionUtil.getProvider(file);
    if (provider == null) return;

    if (parameters.getInvocationCount() == 0 &&
        !Boolean.TRUE.equals(file.getUserData(TextCompletionUtil.AUTO_POPUP_KEY))) {
      return;
    }

    String advertisement = provider.getAdvertisement();
    if (advertisement != null) {
      result.addLookupAdvertisement(advertisement);
    }

    String text = file.getText();
    int offset = Math.min(text.length(), parameters.getOffset());
    String prefix = provider.getPrefix(text, offset);
    if (prefix == null) return;

    CompletionResultSet activeResult = provider.applyPrefixMatcher(result, prefix);

    provider.fillCompletionVariants(parameters, prefix, activeResult);
  }

  @Override
  public boolean invokeAutoPopup(@NotNull PsiElement position, char typeChar) {
    TextCompletionProvider provider = TextCompletionUtil.getProvider(position.getContainingFile());
    if (provider != null) {
      if (Boolean.TRUE.equals(position.getContainingFile().getUserData(TextCompletionUtil.AUTO_POPUP_KEY))) {
        return Objects.equals(CharFilter.Result.ADD_TO_PREFIX, provider.acceptChar(typeChar));
      }
    }
    return super.invokeAutoPopup(position, typeChar);
  }
}
