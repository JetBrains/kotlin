// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.hierarchy.newAPI;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;

/**
 * @author cdr
 */
public final class ViewSubtypesHierarchyAction extends ChangeViewTypeActionBase {
  public ViewSubtypesHierarchyAction() {
    super(IdeBundle.message("action.view.subtypes.hierarchy"),
          IdeBundle.message("action.description.view.subtypes.hierarchy"), AllIcons.Hierarchy.Subtypes);
  }

  @Override
  protected final HierarchyScopeType getTypeName() {
    return TypeHierarchyBrowserBase.getSubtypesHierarchyType();
  }
}
