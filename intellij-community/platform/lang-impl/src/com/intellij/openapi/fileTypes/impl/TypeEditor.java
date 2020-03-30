// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.fileTypes.impl;

import com.intellij.openapi.fileTypes.UserFileType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

class TypeEditor<T extends UserFileType<T>> extends DialogWrapper {
  private final T myFileType;
  private final SettingsEditor<T> myEditor;

  TypeEditor(@NotNull Component parent, @NotNull T fileType, @NotNull @NlsContexts.DialogTitle String title) {
    super(parent, false);
    myFileType = fileType;
    myEditor = fileType.getEditor();
    setTitle(title);
    init();
    Disposer.register(myDisposable, myEditor);
  }

  @Override
  protected void init() {
    super.init();
    myEditor.resetFrom(myFileType);
  }

  @Override
  protected JComponent createCenterPanel() {
    return myEditor.getComponent();
  }

  @Override
  protected void doOKAction() {
    try {
      myEditor.applyTo(myFileType);
    }
    catch (ConfigurationException e) {
      Messages.showErrorDialog(getContentPane(), e.getMessage(), e.getTitle());
      return;
    }
    super.doOKAction();
  }

  @Override
  protected String getHelpId() {
    //noinspection SpellCheckingInspection
    return "reference.dialogs.newfiletype";
  }
}
