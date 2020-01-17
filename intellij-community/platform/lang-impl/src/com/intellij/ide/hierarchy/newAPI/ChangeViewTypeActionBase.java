// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.hierarchy.newAPI;

import com.intellij.openapi.actionSystem.DataContext;

import javax.swing.*;

/**
 * @author cdr
 */
abstract class ChangeViewTypeActionBase extends ChangeHierarchyViewActionBase {
  ChangeViewTypeActionBase(final String shortDescription, final String longDescription, final Icon icon) {
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
