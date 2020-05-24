// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template;

import com.intellij.codeInsight.template.impl.TemplateImpl;
import com.intellij.codeInsight.template.impl.TemplateSubstitutionContext;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface TemplateSubstitutor {
  ExtensionPointName<TemplateSubstitutor> EP_NAME = ExtensionPointName.create("com.intellij.liveTemplateSubstitutor");

  /**
   * @deprecated use {@link #substituteTemplate(TemplateSubstitutionContext, TemplateImpl)} instead
   */
  @ApiStatus.ScheduledForRemoval(inVersion = "2020.3")
  @Deprecated
  @Nullable
  default TemplateImpl substituteTemplate(@NotNull PsiFile file, int caretOffset, @NotNull TemplateImpl template) {
    throw new RuntimeException(
      "Please, implement com.intellij.codeInsight.template.TemplateSubstitutor.substituteTemplate(Project, Editor, TemplateImpl) and don't invoke this method directly");
  }

  /**
   * @return template that should be used instead of {@code template} or null if we can't substitute a template
   */
  @Nullable
  default TemplateImpl substituteTemplate(@NotNull TemplateSubstitutionContext substitutionContext,
                                          @NotNull TemplateImpl template) {
    return substituteTemplate(substitutionContext.getPsiFile(), substitutionContext.getOffset(), template);
  }
}
