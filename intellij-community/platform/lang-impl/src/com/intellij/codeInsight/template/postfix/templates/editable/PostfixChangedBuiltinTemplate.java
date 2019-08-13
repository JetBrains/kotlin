// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.templates.editable;

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents the template that overrides the builtin one.
 * It's considered as builtin template: cannot be deleted via UI but can be restored to its initial state.
 */
public class PostfixChangedBuiltinTemplate extends PostfixTemplateWrapper {
  @NotNull
  private final PostfixTemplate myBuiltinTemplate;

  public PostfixChangedBuiltinTemplate(@NotNull PostfixTemplate template, @NotNull PostfixTemplate builtin) {
    super(template);
    myBuiltinTemplate = builtin;
  }

  @NotNull
  public PostfixTemplate getBuiltinTemplate() {
    return myBuiltinTemplate;
  }

  @Override
  public boolean isBuiltin() {
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PostfixChangedBuiltinTemplate)) return false;
    if (!super.equals(o)) return false;
    PostfixChangedBuiltinTemplate template = (PostfixChangedBuiltinTemplate)o;
    return Objects.equals(myBuiltinTemplate, template.myBuiltinTemplate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), myBuiltinTemplate);
  }
}
