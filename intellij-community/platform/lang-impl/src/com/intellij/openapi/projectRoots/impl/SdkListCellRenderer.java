// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author yole
*/
public class SdkListCellRenderer extends ColoredListCellRenderer<Sdk> {
  private final String myNullText;
  private final boolean myShowHomePath;

  public SdkListCellRenderer(@NotNull String nullText) {
    this(nullText, false);
  }

  public SdkListCellRenderer(@NotNull String nullText, boolean showHomePath) {
    myNullText = nullText;
    myShowHomePath = showHomePath;
  }

  @Override
  protected void customizeCellRenderer(@NotNull JList<? extends Sdk> list, Sdk sdk, int index, boolean selected, boolean hasFocus) {
    if (sdk != null) {
      setIcon(getSdkIcon(sdk));
      append(sdk.getName());
      if (myShowHomePath) {
        append(" (" + FileUtil.toSystemDependentName(StringUtil.notNullize(sdk.getHomePath())) + ")",
               selected ? SimpleTextAttributes.REGULAR_ATTRIBUTES : SimpleTextAttributes.GRAYED_ATTRIBUTES);
      }
    }
    else {
      append(myNullText);
    }
  }

  protected Icon getSdkIcon(Sdk sdk) {
    return ((SdkType) sdk.getSdkType()).getIcon();
  }
}