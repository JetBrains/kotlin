// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle.excludedFiles;

import com.intellij.formatting.fileSet.PatternDescriptor;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.fields.valueEditors.TextFieldValueEditor;
import com.intellij.ui.components.fields.valueEditors.ValueEditor;
import com.intellij.ui.scale.JBUIScale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;

public abstract class ExcludedFilesPatternForm {
  private JPanel myTopPanel;
  private JBTextField myPatternField;

  public ExcludedFilesPatternForm() {
    int minWidth = JBUIScale.scale(400);
    int minHeight = myPatternField.getMinimumSize().height;
    myPatternField.setMinimumSize(new Dimension(minWidth, minHeight));
  }

  protected abstract void updateOnError();

  protected abstract void updateOnValue(@NotNull String newValue);

  public JPanel getTopPanel() {
    return myTopPanel;
  }

  public void setFileSpec(@NotNull String fileSpec) {
    ((PatternField)myPatternField).getValueEditor().setValue(fileSpec);
  }

  public String getFileSpec() {
    return ((PatternField)myPatternField).getValueEditor().getValue();
  }

  private void createUIComponents() {
    myPatternField = new PatternField();
  }

  private class PatternField extends JBTextField {
    private final ValueEditor<String> myValueEditor;

    private PatternField() {
      myValueEditor = new TextFieldValueEditor<String>(this, "Pattern", "") {
        @NotNull
        @Override
        public String parseValue(@Nullable String text) throws InvalidDataException {
          if (text != null && !PatternDescriptor.isValidPattern(text)) {
            throw new InvalidDataException("Invalid pattern");
          }
          return text != null ? text : "";
        }

        @Override
        public String valueToString(@NotNull String value) {
          return value;
        }

        @Override
        public boolean isValid(@NotNull String value) {
          return PatternDescriptor.isValidPattern(value);
        }

        @Override
        protected String validateTextOnChange(String text, DocumentEvent e) {
          String err = super.validateTextOnChange(text, e);
          if (!StringUtil.isEmpty(err)) {
            updateOnError();
          }
          return err;
        }
      };
      myValueEditor.addListener(new ValueEditor.Listener<String>() {
        @Override
        public void valueChanged(@NotNull String newValue) {
          updateOnValue(newValue);
        }
      });
    }

    public ValueEditor<String> getValueEditor() {
      return myValueEditor;
    }
  }
}