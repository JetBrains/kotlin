// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.find.impl;

import com.intellij.find.FindModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class FindDialog  {
  /**
   * @deprecated
   * @see com.intellij.find.impl.FindInProjectUtil#initFileFilter(javax.swing.JComboBox, javax.swing.JCheckBox)
   */
  public static void initFileFilter(@NotNull final JComboBox<? super String> fileFilter, @NotNull final JCheckBox useFileFilter) {
    FindInProjectUtil.initFileFilter(fileFilter, useFileFilter);
  }

  /**
   * @deprecated
   * @see FindInProjectUtil#getPresentableName(com.intellij.find.FindModel.SearchContext)
   */
  public static String getPresentableName(@NotNull FindModel.SearchContext searchContext) {
    return FindInProjectUtil.getPresentableName(searchContext);
  }
}

