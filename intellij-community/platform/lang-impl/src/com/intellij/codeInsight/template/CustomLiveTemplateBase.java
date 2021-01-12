/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.codeInsight.template;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.template.impl.CustomLiveTemplateLookupElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

abstract public class CustomLiveTemplateBase implements CustomLiveTemplate {
  /**
   * Implementation should returns {@code true} if it has own lookup item in completion autopopup
   * and it is supposed that template should be expanded while completion auto-popup is active.
   */
  public boolean hasCompletionItem(@NotNull CustomTemplateCallback callback, int offset) {
    return false;
  }

  /**
   * Return lookup elements for popup that appears on ListTemplateAction (Ctrl + J)
   */
  @NotNull
  public Collection<? extends CustomLiveTemplateLookupElement> getLookupElements(@NotNull PsiFile file, @NotNull Editor editor, int offset) {
    return Collections.emptyList();
  }
  
  /**
   * Populate completion result set. Used by LiveTemplateCompletionContributor
   */
  public void addCompletions(CompletionParameters parameters, CompletionResultSet result) {
    String prefix = computeTemplateKeyWithoutContextChecking(new CustomTemplateCallback(parameters.getEditor(), parameters.getOriginalFile()));
    if (prefix != null) {
      result.withPrefixMatcher(result.getPrefixMatcher().cloneWithPrefix(prefix)).addAllElements(
        getLookupElements(parameters.getOriginalFile(), parameters.getEditor(), parameters.getOffset()));
    }
  }

  @Nullable
  public String computeTemplateKeyWithoutContextChecking(@NotNull CustomTemplateCallback callback) {
    return computeTemplateKey(callback);
  }

  public boolean supportsMultiCaret() {
    return true;
  }
}
