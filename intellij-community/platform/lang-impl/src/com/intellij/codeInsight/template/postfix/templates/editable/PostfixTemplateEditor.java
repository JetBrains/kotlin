// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.templates.editable;

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * The editor that is able to show the UI settings for a particular template,
 * or to create a template from settings defined in UI form.
 */
@ApiStatus.Experimental
public interface PostfixTemplateEditor extends Disposable {
  @NotNull
  PostfixTemplate createTemplate(@NotNull String templateId, @NotNull String templateName);

  @NotNull
  JComponent getComponent();

  default String getHelpId() {
    return null;
  }
}
