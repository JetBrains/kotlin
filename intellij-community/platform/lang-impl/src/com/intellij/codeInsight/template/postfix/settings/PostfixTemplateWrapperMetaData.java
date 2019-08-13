// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.settings;

import com.intellij.codeInsight.intention.impl.config.BeforeAfterMetaData;
import com.intellij.codeInsight.intention.impl.config.TextDescriptor;
import com.intellij.codeInsight.template.postfix.templates.editable.PostfixTemplateWrapper;
import org.jetbrains.annotations.NotNull;

import static com.intellij.codeInsight.template.postfix.settings.PostfixTemplateMetaData.decorateTextDescriptorWithKey;

public class PostfixTemplateWrapperMetaData implements BeforeAfterMetaData {

  @NotNull
  private final BeforeAfterMetaData myDelegateMetaData;
  @NotNull
  private final String myKey;

  public PostfixTemplateWrapperMetaData(@NotNull PostfixTemplateWrapper wrapper) {
    myKey = wrapper.getKey();
    myDelegateMetaData = PostfixTemplateMetaData.createMetaData(wrapper.getDelegate());
  }

  @NotNull
  @Override
  public TextDescriptor[] getExampleUsagesBefore() {
    if (myDelegateMetaData instanceof PostfixTemplateMetaData) {
      return decorateTextDescriptorWithKey(((PostfixTemplateMetaData)myDelegateMetaData).getRawExampleUsagesBefore(), myKey);
    }

    return myDelegateMetaData.getExampleUsagesBefore();
  }

  @NotNull
  @Override
  public TextDescriptor[] getExampleUsagesAfter() {
    if (myDelegateMetaData instanceof PostfixTemplateMetaData) {
      return decorateTextDescriptorWithKey(((PostfixTemplateMetaData)myDelegateMetaData).getRawExampleUsagesAfter(), myKey);
    }
    return myDelegateMetaData.getExampleUsagesAfter();
  }

  @NotNull
  @Override
  public TextDescriptor getDescription() {
    return myDelegateMetaData.getDescription();
  }
}
