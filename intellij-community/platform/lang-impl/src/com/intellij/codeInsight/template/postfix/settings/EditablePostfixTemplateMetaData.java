// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.settings;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.intention.impl.config.BeforeAfterMetaData;
import com.intellij.codeInsight.intention.impl.config.PlainTextDescriptor;
import com.intellij.codeInsight.intention.impl.config.TextDescriptor;
import com.intellij.codeInsight.template.impl.TemplateImpl;
import com.intellij.codeInsight.template.postfix.templates.editable.EditablePostfixTemplate;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

public class EditablePostfixTemplateMetaData implements BeforeAfterMetaData {

  @NotNull
  private final String myAfterText;
  private final String myBeforeText;

  public EditablePostfixTemplateMetaData(@NotNull EditablePostfixTemplate template) {
    TemplateImpl liveTemplate = template.getLiveTemplate();
    String text = liveTemplate.getString();

    myBeforeText = "<spot>$EXPR$</spot>" + template.getKey();
    myAfterText = StringUtil.replace(text, "$END$", "<spot></spot>", true);
  }

  @NotNull
  @Override
  public TextDescriptor[] getExampleUsagesBefore() {
    return new TextDescriptor[]{new PlainTextDescriptor(myBeforeText, "before.txt")};
  }

  @NotNull
  @Override
  public TextDescriptor[] getExampleUsagesAfter() {
    return new TextDescriptor[]{new PlainTextDescriptor(myAfterText, "after.txt")};
  }

  @NotNull
  @Override
  public TextDescriptor getDescription() {
    return new PlainTextDescriptor(CodeInsightBundle.message("templates.postfix.editable.description"), "description.txt");
  }
}
