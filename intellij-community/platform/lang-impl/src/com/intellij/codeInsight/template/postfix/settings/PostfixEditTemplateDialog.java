// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.settings;

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate;
import com.intellij.codeInsight.template.postfix.templates.editable.PostfixTemplateEditor;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class PostfixEditTemplateDialog extends DialogWrapper {
  private final JBTextField myTemplateNameTextField;
  private final PostfixTemplateEditor myEditor;

  public PostfixEditTemplateDialog(@NotNull Component parentComponent,
                                   @NotNull PostfixTemplateEditor editor,
                                   @NotNull String templateType,
                                   @Nullable PostfixTemplate template) {
    super(null, parentComponent, true, IdeModalityType.IDE);
    myEditor = editor;
    Disposer.register(getDisposable(), editor);
    String initialName = template != null ? StringUtil.trimStart(template.getKey(), ".") : "";
    myTemplateNameTextField = new JBTextField(initialName);
    setTitle(template != null ? "Edit '" + initialName + "' template" : "Create new " + templateType + " template");
    init();
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myTemplateNameTextField;
  }

  @NotNull
  @Override
  protected List<ValidationInfo> doValidateAll() {
    String templateName = myTemplateNameTextField.getText();
    if (!StringUtil.isJavaIdentifier(templateName)) {
      return Collections.singletonList(new ValidationInfo("Template key must be an identifier", myTemplateNameTextField));
    }
    return super.doValidateAll();
  }

  @NotNull
  public String getTemplateName() {
    return myTemplateNameTextField.getText();
  }

  @Override
  protected JComponent createCenterPanel() {
    return FormBuilder.createFormBuilder()
      .addLabeledComponent("Key:", myTemplateNameTextField)
      .addComponentFillVertically(myEditor.getComponent(), UIUtil.DEFAULT_VGAP)
      .getPanel();
  }

  @Nullable
  @Override
  protected String getHelpId() {
    return myEditor.getHelpId();
  }
}
