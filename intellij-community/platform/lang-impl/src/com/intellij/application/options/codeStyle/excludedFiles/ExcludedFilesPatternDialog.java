// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle.excludedFiles;

import com.intellij.formatting.fileSet.FileSetDescriptor;
import com.intellij.formatting.fileSet.PatternDescriptor;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

class ExcludedFilesPatternDialog extends ExcludedFilesDialogBase {

  private ExcludedFilesPatternForm myForm;


  ExcludedFilesPatternDialog(@NotNull String initialSpec) {
    this();
    setTitle(ApplicationBundle.message("settings.code.style.general.file.pattern.edit"));
    myForm.setFileSpec(initialSpec);
  }

  ExcludedFilesPatternDialog() {
    super(true);
    setTitle(ApplicationBundle.message("settings.code.style.general.file.pattern.new"));
    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    myForm = new ExcludedFilesPatternForm() {
      @Override
      protected void updateOnError() {
        getOKAction().setEnabled(false);
      }

      @Override
      protected void updateOnValue(@NotNull String newValue) {
        getOKAction().setEnabled(true);
      }
    };
    return myForm.getTopPanel();
  }

  @Nullable
  @Override
  public FileSetDescriptor getDescriptor() {
    String fileSpec = StringUtil.trim(myForm.getFileSpec());
    return StringUtil.isEmpty(fileSpec) ? null : new PatternDescriptor(fileSpec);
  }
}
