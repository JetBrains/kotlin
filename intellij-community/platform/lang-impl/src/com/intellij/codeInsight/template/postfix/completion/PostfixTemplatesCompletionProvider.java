// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.codeInsight.template.CustomTemplateCallback;
import com.intellij.codeInsight.template.impl.LiveTemplateCompletionContributor;
import com.intellij.codeInsight.template.postfix.settings.PostfixTemplatesSettings;
import com.intellij.codeInsight.template.postfix.templates.PostfixLiveTemplate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import static com.intellij.codeInsight.template.postfix.completion.PostfixTemplateCompletionContributor.getPostfixLiveTemplate;

class PostfixTemplatesCompletionProvider extends CompletionProvider<CompletionParameters> {
  @Override
  protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
    Editor editor = parameters.getEditor();
    if (!isCompletionEnabled(parameters) || LiveTemplateCompletionContributor.shouldShowAllTemplates() ||
        editor.getCaretModel().getCaretCount() != 1) {
      /*
        disabled or covered with {@link com.intellij.codeInsight.template.impl.LiveTemplateCompletionContributor}
       */
      return;
    }

    PsiFile originalFile = parameters.getOriginalFile();
    PostfixLiveTemplate postfixLiveTemplate = getPostfixLiveTemplate(originalFile, editor);
    if (postfixLiveTemplate != null) {
      postfixLiveTemplate.addCompletions(parameters, result.withPrefixMatcher(new MyPrefixMatcher(result.getPrefixMatcher().getPrefix())));
      String possibleKey = postfixLiveTemplate.computeTemplateKeyWithoutContextChecking(new CustomTemplateCallback(editor, originalFile));
      if (possibleKey != null) {
        result = result.withPrefixMatcher(possibleKey);
        result.restartCompletionOnPrefixChange(
          StandardPatterns.string().oneOf(postfixLiveTemplate.getAllTemplateKeys(originalFile, parameters.getOffset())));
      }
    }
  }

  private static boolean isCompletionEnabled(@NotNull CompletionParameters parameters) {
    ProgressManager.checkCanceled();
    if (!parameters.isAutoPopup()) {
      return false;
    }

    PostfixTemplatesSettings settings = PostfixTemplatesSettings.getInstance();
    if (!settings.isPostfixTemplatesEnabled() || !settings.isTemplatesCompletionEnabled()) {
      return false;
    }

    return true;
  }

  private static class MyPrefixMatcher extends PrefixMatcher {
    protected MyPrefixMatcher(String prefix) {
      super(prefix);
    }

    @Override
    public boolean prefixMatches(@NotNull String name) {
      return name.equalsIgnoreCase(myPrefix);
    }

    @NotNull
    @Override
    public PrefixMatcher cloneWithPrefix(@NotNull String prefix) {
      return new MyPrefixMatcher(prefix);
    }
  }
}
