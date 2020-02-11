// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.hierarchy.newAPI;

import com.intellij.openapi.actionSystem.DataContext;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.function.Supplier;

/**
 * @author cdr
 */
abstract class ChangeViewTypeActionBase extends ChangeHierarchyViewActionBase {
  ChangeViewTypeActionBase(@NotNull Supplier<String> shortDescription, @NotNull Supplier<String> longDescription, final Icon icon) {
    super(shortDescription, longDescription, icon);
  }

  @Override
  protected TypeHierarchyBrowserBase getHierarchyBrowser(final DataContext context) {
    return getTypeHierarchyBrowser(context);
  }

  static TypeHierarchyBrowserBase getTypeHierarchyBrowser(final DataContext context) {
    return TypeHierarchyBrowserBase.DATA_KEY.getData(context);
  }
}
