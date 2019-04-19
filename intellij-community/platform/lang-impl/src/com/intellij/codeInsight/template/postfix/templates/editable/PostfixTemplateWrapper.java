// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.templates.editable;

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class PostfixTemplateWrapper extends PostfixTemplate {
  @NotNull private final PostfixTemplate myDelegate;

  public PostfixTemplateWrapper(@NotNull PostfixTemplate template) {
    this(template.getId(), template.getPresentableName(), template.getKey(), template, template.getProvider());
  }

  public PostfixTemplateWrapper(@NotNull String id,
                                @NotNull String name,
                                @NotNull String key,
                                @NotNull PostfixTemplate template,
                                @Nullable PostfixTemplateProvider provider) {
    super(id, name, key, template.getExample(), provider);
    myDelegate = template;
  }

  @NotNull
  public PostfixTemplate getDelegate() {
    return myDelegate;
  }

  @NotNull
  @Override
  public String getDescription() {
    return myDelegate.getDescription();
  }

  @Override
  public boolean startInWriteAction() {
    return myDelegate.startInWriteAction();
  }

  @Override
  public boolean isEnabled(PostfixTemplateProvider provider) {
    return myDelegate.isEnabled(provider);
  }

  @Override
  public boolean isApplicable(@NotNull PsiElement context, @NotNull Document copyDocument, int newOffset) {
    return myDelegate.isApplicable(context, copyDocument, newOffset);
  }

  @Override
  public void expand(@NotNull PsiElement context, @NotNull Editor editor) {
    myDelegate.expand(context, editor);
  }

  @Override
  public boolean isBuiltin() {
    return myDelegate.isBuiltin();
  }

  @Override
  public boolean isEditable() {
    return myDelegate.isEditable();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PostfixTemplateWrapper)) return false;
    if (!super.equals(o)) return false;
    PostfixTemplateWrapper wrapper = (PostfixTemplateWrapper)o;
    return Objects.equals(myDelegate, wrapper.myDelegate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myDelegate);
  }
}
