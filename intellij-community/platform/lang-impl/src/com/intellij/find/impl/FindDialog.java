// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.find.impl;

import com.intellij.find.FindModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class FindDialog {
  /**
   * @deprecated use {@link FindInProjectUtil#initFileFilter(JComboBox, JCheckBox)}
   */
  @Deprecated
  public static void initFileFilter(@NotNull final JComboBox<? super String> fileFilter, @NotNull final JCheckBox useFileFilter) {
    FindInProjectUtil.initFileFilter(fileFilter, useFileFilter);
  }

  /**
   * @deprecated use {@link FindInProjectUtil#getPresentableName(FindModel.SearchContext)}
   */
  @Deprecated
  public static String getPresentableName(@NotNull FindModel.SearchContext searchContext) {
    return FindInProjectUtil.getPresentableName(searchContext);
  }
}

