/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.codeInsight.template.postfix.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.template.CustomLiveTemplate;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.postfix.templates.PostfixLiveTemplate;
import com.intellij.openapi.editor.Editor;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PostfixTemplateCompletionContributor extends CompletionContributor {
  public PostfixTemplateCompletionContributor() {
    extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new PostfixTemplatesCompletionProvider());
  }

  @Nullable
  public static PostfixLiveTemplate getPostfixLiveTemplate(@NotNull PsiFile file, @NotNull Editor editor) {
    PostfixLiveTemplate postfixLiveTemplate = CustomLiveTemplate.EP_NAME.findExtension(PostfixLiveTemplate.class);
    return postfixLiveTemplate != null && TemplateManagerImpl.isApplicable(postfixLiveTemplate, editor, file) ? postfixLiveTemplate : null;
  }
}
